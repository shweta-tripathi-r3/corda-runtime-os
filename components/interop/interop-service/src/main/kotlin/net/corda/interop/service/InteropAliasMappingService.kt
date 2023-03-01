package net.corda.interop.service

import net.corda.virtualnode.HoldingIdentity
import net.corda.virtualnode.VirtualNodeInfo
import net.corda.virtualnode.read.VirtualNodeInfoReadService
import org.osgi.service.component.annotations.Component

@Component(service = [InteropAliasMappingService::class])
class InteropAliasMappingService {

    //returns the holding identity for the name of an actual identity (not alias)
    fun getMapping(vNodeInfoList: List<VirtualNodeInfo>, name: String): HoldingIdentity? {
        var realHoldingIdentity: HoldingIdentity? = null
        vNodeInfoList.forEach {
            if (it.holdingIdentity.x500Name.toString().contains(name))
                realHoldingIdentity = it.holdingIdentity
        }
        return realHoldingIdentity
    }
}