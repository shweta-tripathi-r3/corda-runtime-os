package net.corda.membership.impl.registration.dummy

import net.corda.data.membership.PersistentMemberInfo
import net.corda.data.membership.StaticNetworkInfo
import net.corda.data.membership.common.ApprovalRuleDetails
import net.corda.data.membership.common.ApprovalRuleType
import net.corda.data.membership.common.RegistrationStatus
import net.corda.data.membership.preauth.PreAuthToken
import net.corda.membership.lib.InternalGroupParameters
import net.corda.membership.lib.SignedMemberInfo
import net.corda.membership.lib.approval.ApprovalRuleParams
import net.corda.membership.lib.registration.RegistrationRequest
import net.corda.membership.persistence.client.MembershipPersistenceClient
import net.corda.membership.persistence.client.MembershipPersistenceOperation
import net.corda.membership.persistence.client.MembershipPersistenceResult
import net.corda.messaging.api.records.Record
import net.corda.v5.base.types.LayeredPropertyMap
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.membership.MemberInfo
import net.corda.virtualnode.HoldingIdentity
import org.osgi.service.component.annotations.Activate
import org.osgi.service.component.annotations.Component
import org.osgi.service.component.propertytypes.ServiceRanking
import java.time.Instant
import java.util.UUID

@ServiceRanking(Int.MAX_VALUE)
@Component(service = [MembershipPersistenceClient::class])
class TestMembershipPersistenceClientImpl @Activate constructor() : MembershipPersistenceClient {
    override fun persistMemberInfo(
        viewOwningIdentity: HoldingIdentity,
        memberInfos: Collection<SignedMemberInfo>,
    ): MembershipPersistenceOperation<Unit> = MembershipPersistenceOperationImpl(MembershipPersistenceResult.success())

    override fun persistGroupPolicy(
        viewOwningIdentity: HoldingIdentity,
        groupPolicy: LayeredPropertyMap,
        version: Long,
    ): MembershipPersistenceOperation<Unit> = MembershipPersistenceOperationImpl(MembershipPersistenceResult.success())

    override fun persistGroupParametersInitialSnapshot(
        viewOwningIdentity: HoldingIdentity
    ): MembershipPersistenceOperation<InternalGroupParameters> = MembershipPersistenceOperationImpl(MembershipPersistenceResult.Failure("Unsupported"))

    override fun persistGroupParameters(
        viewOwningIdentity: HoldingIdentity,
        groupParameters: InternalGroupParameters,
    ): MembershipPersistenceOperation<InternalGroupParameters> = MembershipPersistenceOperationImpl(MembershipPersistenceResult.Success(groupParameters))

    override fun addNotaryToGroupParameters(
        viewOwningIdentity: HoldingIdentity,
        notary: MemberInfo,
    ): MembershipPersistenceOperation<InternalGroupParameters> = MembershipPersistenceOperationImpl(MembershipPersistenceResult.Failure("Unsupported"))

    override fun persistRegistrationRequest(
        viewOwningIdentity: HoldingIdentity,
        registrationRequest: RegistrationRequest,
    ): MembershipPersistenceOperation<Unit> = MembershipPersistenceOperationImpl(MembershipPersistenceResult.success())

    override fun setMemberAndRegistrationRequestAsApproved(
        viewOwningIdentity: HoldingIdentity,
        approvedMember: HoldingIdentity,
        registrationRequestId: String,
    ): MembershipPersistenceOperation<MemberInfo> = MembershipPersistenceOperationImpl(MembershipPersistenceResult.Failure("Unsupported"))

    override fun setRegistrationRequestStatus(
        viewOwningIdentity: HoldingIdentity,
        registrationId: String,
        registrationRequestStatus: RegistrationStatus,
        reason: String?,
    ): MembershipPersistenceOperation<Unit> = MembershipPersistenceOperationImpl(MembershipPersistenceResult.success())

    override fun mutualTlsAddCertificateToAllowedList(
        mgmHoldingIdentity: HoldingIdentity,
        subject: String,
    ): MembershipPersistenceOperation<Unit> = MembershipPersistenceOperationImpl(MembershipPersistenceResult.success())

    override fun mutualTlsRemoveCertificateFromAllowedList(
        mgmHoldingIdentity: HoldingIdentity,
        subject: String,
    ): MembershipPersistenceOperation<Unit> = MembershipPersistenceOperationImpl(MembershipPersistenceResult.success())

    override fun generatePreAuthToken(
        mgmHoldingIdentity: HoldingIdentity,
        preAuthTokenId: UUID,
        ownerX500Name: MemberX500Name,
        ttl: Instant?,
        remarks: String?,
    ): MembershipPersistenceOperation<Unit> = MembershipPersistenceOperationImpl(MembershipPersistenceResult.success())

    override fun consumePreAuthToken(
        mgmHoldingIdentity: HoldingIdentity,
        ownerX500Name: MemberX500Name,
        preAuthTokenId: UUID
    ): MembershipPersistenceOperation<Unit> = MembershipPersistenceOperationImpl(MembershipPersistenceResult.success())

    override fun revokePreAuthToken(
        mgmHoldingIdentity: HoldingIdentity,
        preAuthTokenId: UUID,
        remarks: String?,
    ): MembershipPersistenceOperation<PreAuthToken> = MembershipPersistenceOperationImpl(MembershipPersistenceResult.Failure("Unsupported"))

    override fun addApprovalRule(
        viewOwningIdentity: HoldingIdentity,
        ruleParams: ApprovalRuleParams,
    ): MembershipPersistenceOperation<ApprovalRuleDetails> = MembershipPersistenceOperationImpl(MembershipPersistenceResult.Failure("Unsupported"))

    override fun deleteApprovalRule(
        viewOwningIdentity: HoldingIdentity,
        ruleId: String,
        ruleType: ApprovalRuleType,
    ): MembershipPersistenceOperation<Unit> = MembershipPersistenceOperationImpl(MembershipPersistenceResult.success())

    override fun suspendMember(
        viewOwningIdentity: HoldingIdentity, memberX500Name: MemberX500Name, serialNumber: Long?, reason: String?
    ): MembershipPersistenceOperation<PersistentMemberInfo> = MembershipPersistenceOperationImpl(MembershipPersistenceResult.Failure("Unsupported"))

    override fun activateMember(
        viewOwningIdentity: HoldingIdentity, memberX500Name: MemberX500Name, serialNumber: Long?, reason: String?
    ): MembershipPersistenceOperation<PersistentMemberInfo> = MembershipPersistenceOperationImpl(MembershipPersistenceResult.Failure("Unsupported"))

    override fun updateStaticNetworkInfo(
        info: StaticNetworkInfo
    ): MembershipPersistenceOperation<StaticNetworkInfo> = MembershipPersistenceOperationImpl(MembershipPersistenceResult.Failure("Unsupported"))

    override val isRunning = true

    override fun start() {}

    override fun stop() {}


    private class MembershipPersistenceOperationImpl<T>(
        private val results: MembershipPersistenceResult<T>
    ) : MembershipPersistenceOperation<T> {
        override fun execute() = results

        override fun createAsyncCommands(): Collection<Record<*, *>> = emptyList()
    }
}
