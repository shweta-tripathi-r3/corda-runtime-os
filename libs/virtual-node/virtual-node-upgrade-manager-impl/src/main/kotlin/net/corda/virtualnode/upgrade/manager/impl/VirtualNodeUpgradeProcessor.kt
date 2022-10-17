package net.corda.virtualnode.upgrade.manager.impl

import java.time.Instant
import java.util.concurrent.CompletableFuture
import net.corda.data.ExceptionEnvelope
import net.corda.data.virtualnode.VirtualNodeCpiUpgradeRequest
import net.corda.messaging.api.processor.DurableProcessor
import net.corda.messaging.api.publisher.Publisher
import net.corda.messaging.api.publisher.RPCSender
import net.corda.messaging.api.records.Record
import net.corda.schema.Schemas.VirtualNode.Companion.VIRTUAL_NODE_UPGRADE_STATUS_TOPIC

typealias VirtualNodeShortHash = String

class VirtualNodeUpgradeProcessor(
    private val statusPublisher: Publisher,
    private val membershipReRegistrationRpcSender: RPCSender<String, String>,

    private val virtualNodeEntityRepository: VirtualNodeEntityRepository
    ) : DurableProcessor<VirtualNodeShortHash, VirtualNodeCpiUpgradeRequest> {


    override val keyClass = String::class.java
    override val valueClass = VirtualNodeCpiUpgradeRequest::class.java

    override fun onNext(events: List<Record<VirtualNodeShortHash, VirtualNodeCpiUpgradeRequest>>): List<Record<*, *>> {
        events.map {
            val vNodeShortHash = it.key
            val request = it.value
            checkNotNull(request) {
                "Virtual node upgrade request with null record is not supported."
            }
            upgradeVirtualNodeCpi(vNodeShortHash, request.cpiFileChecksum, request.requestId)
        }
    }

    //todo wrap this
    private fun publishStatus(requestId: String, status: String) {
        statusPublisher.publish(listOf(Record(VIRTUAL_NODE_UPGRADE_STATUS_TOPIC, requestId, status)))
    }

    private fun upgradeVirtualNodeCpi(
        virtualNodeShortHash: String,
        cpiFileChecksum: String,
        requestId: String,
    ) {
        try {
            publishStatus(requestId, "Validating virtual node upgrade request.")

            // todo work out which of these operations can be condensed into one transaction

            val currentVirtualNode = findCurrentVirtualNode(request.virtualNodeShortId)

            publishStatus(requestId, "Validating virtual node state")
            validateCurrentVirtualNodeMaintenance(currentVirtualNode)

            publishStatus(requestId, "Validating upgrade CPI")
            val upgradeCpiMetadata = findUpgradeCpi(request.cpiFileChecksum)
            val (holdingId, connections) = findHoldingIdentityAndConnections(request.virtualNodeShortId)

            val originalCpiMetadata = findCurrentCpiMetadata(currentVirtualNode.cpiName, currentVirtualNode.cpiVersion)

            publishStatus(requestId, "Validating CPI group")
            validateCpiInSameGroup(originalCpiMetadata, upgradeCpiMetadata)

            publishStatus(requestId, "Getting vault schema connections")
            val (vaultDDLConnectionConfig, vaultDMLConnectionConfig) =
                getVaultSchemaConnectionConfigs(connections)

            publishStatus(requestId, "Running CPI migrations")
            runCpiMigrations(holdingId, upgradeCpiMetadata, vaultDDLConnectionConfig?.config, vaultDMLConnectionConfig?.config)

            publishStatus(requestId, "Associating new CPI with virtual node")
            updateVirtualNodeCpi(holdingId, upgradeCpiMetadata.id)

            publishStatus(requestId, "Setting virtual node state to active")
            updateVirtualNodeToActive(holdingId.shortHash.value)

            publishStatus(requestId, "Publishing upgraded virtual node info")
            publishNewlyUpgradedVirtualNodeInfo(holdingId, upgradeCpiMetadata, connections)

            publishStatus(requestId, "Requesting re-registration from MGM")
            publishMgmReRegistration(
                currentVirtualNode.holdingIdentityShortHash,
                upgradeCpiMetadata.id.name,
                upgradeCpiMetadata.id.version,
                upgradeCpiMetadata.id.signerSummaryHash
            )

        } catch (e: Exception) {
            handleException(respFuture, e)
        }
    }

    private fun publishMgmReRegistration(
        holdingIdentityShortHash: String,
        name: String,
        version: String,
        signerSummaryHash: SecureHash?
    ) {
        val future = mgmReRegistrationSender.sendRequest(holdingIdentityShortHash)
        future.whenComplete { t, u ->
            reRegistrationCompletionLogic()
        }
    }

    private fun reRegistrationCompletionLogic() {
        statusPublisher.completed()
    }

    private fun updateVirtualNodeToActive(holdingIdShortHash: String) {
        virtualNodeEntityRepository.setVirtualNodeState(holdingIdShortHash, "ACTIVE")
    }

    private fun rollBackVirtualNodeToOriginalCpi(holdingId: HoldingIdentity, currentCpiMetadata: CpiMetadataLite) {
        updateVirtualNodeCpi(holdingId, currentCpiMetadata.id)
    }

    private fun validateCurrentVirtualNodeMaintenance(currentVirtualNode: VirtualNodeEntityRepository.VirtualNodeLite) {
        require(currentVirtualNode.virtualNodeState == "IN_MAINTENANCE") {
            "Virtual nodes must be in maintenance mode to upgrade a CPI. " +
                    "Virtual node '${currentVirtualNode.holdingIdentityShortHash}' state was '${currentVirtualNode.virtualNodeState}'"
        }
    }

    private fun findCurrentCpiMetadata(cpiName: String, cpiVersion: String): CpiMetadataLite {
        return requireNotNull(virtualNodeEntityRepository.getCPIMetadataByNameAndVersion(cpiName, cpiVersion)) {
            "CPI with name $cpiName, version $cpiVersion was not found."
        }
    }

    private fun validateCpiInSameGroup(
        currentCpiMetadata: CpiMetadataLite,
        upgradeCpiMetadata: CpiMetadataLite
    ) {
        if (currentCpiMetadata.mgmGroupId != upgradeCpiMetadata.mgmGroupId) {
            throw MgmGroupMismatchException(currentCpiMetadata.mgmGroupId, upgradeCpiMetadata.mgmGroupId)
        }
    }

    private fun findCurrentVirtualNode(holdingIdentityShortHash: String): VirtualNodeEntityRepository.VirtualNodeLite {
        return virtualNodeEntityRepository.findByHoldingIdentity(holdingIdentityShortHash)
            ?: throw VirtualNodeNotFoundException(holdingIdentityShortHash)
    }

    private fun publishNewlyUpgradedVirtualNodeInfo(
        holdingIdentity: HoldingIdentity,
        cpiMetadata: CpiMetadataLite,
        dbConnections: VirtualNodeDbConnections
    ) {
        virtualNodeInfoPublisher.publishVNodeInfo(holdingIdentity, cpiMetadata, dbConnections)
    }

    private fun getVaultSchemaConnectionConfigs(
        vnodeDbConnections: VirtualNodeDbConnections
    ): Pair<DbConnectionLite?, DbConnectionLite?> {
        val vaultDDLConnection = vnodeDbConnections.vaultDdlConnectionId?.let {
            dbConnectionsRepository.get(it)
        }
        val vaultDMLConnection = vnodeDbConnections.vaultDmlConnectionId.let {
            dbConnectionsRepository.get(it)
        }
        return Pair(vaultDDLConnection, vaultDMLConnection)
    }

    private fun findHoldingIdentityAndConnections(holdingIdentityShortHash: String): VirtualNodeEntityRepository.HoldingIdentityAndConnections {
        return virtualNodeEntityRepository.getHoldingIdentityAndConnections(holdingIdentityShortHash)
            ?: throw HoldingIdentityNotFoundException(holdingIdentityShortHash)
    }

    private fun runCpiMigrations(
        holdingId: HoldingIdentity,
        cpiMetadata: CpiMetadataLite,
        vaultDDLConnectionConfig: String?,
        vaultDMLConnectionConfig: String?
    ) {
        val vaultDb = vnodeDbFactory.createVaultDbs(
            holdingId.shortHash,
            vaultDDLConnectionConfig,
            vaultDMLConnectionConfig
        )
        migrationUtility.runCpiMigrations(cpiMetadata, vaultDb)
    }

    private fun updateVirtualNodeCpi(holdingIdentity: HoldingIdentity, cpiId: CpiIdentifier) {
        virtualNodeEntityRepository.updateVirtualNodeCpi(holdingIdentity, cpiId)
    }

    private fun findUpgradeCpi(cpiFileChecksum: String): CpiMetadataLite {
        return virtualNodeEntityRepository.getCpiMetadataByChecksum(cpiFileChecksum)
            ?: throw CpiNotFoundException("CPI with file checksum $cpiFileChecksum was not found.")
    }

}