package net.cordapp.bundle

import com.example.serialization.PrivateBundleItem
import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.serialization.SerializationCustomSerializer

@CordaSerializable
data class MainBundleItem(val privateBundleItem: PrivateBundleItem) {
    companion object {
        @JvmStatic
        fun newInstance(): MainBundleItem = MainBundleItem(PrivateBundleItem(5))
    }
}

class PrivateBundleItemSerializer : SerializationCustomSerializer<PrivateBundleItem, PrivateBundleItemProxy> {
    override fun toProxy(obj: PrivateBundleItem): PrivateBundleItemProxy =
        PrivateBundleItemProxy(obj.i)

    override fun fromProxy(proxy: PrivateBundleItemProxy): PrivateBundleItem =
        PrivateBundleItem(proxy.i)
}

class PrivateBundleItemProxy(val i: Int)