package net.corda.flow.fiber

import co.paralleluniverse.fibers.Fiber
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import net.corda.cache.caffeine.CacheFactoryImpl
import org.osgi.service.component.annotations.Activate
import org.osgi.service.component.annotations.Component
import java.time.Duration

@Suppress("unused")
@Component(service = [FlowFiberCache::class])
class FlowFiberCacheImpl @Activate constructor() : FlowFiberCache {

    private val cache: Cache<String, Fiber<Any>> = CacheFactoryImpl().build(
        "flow-fiber-cache",
        Caffeine.newBuilder()
            .maximumSize(10)
            .expireAfterWrite(Duration.ofMinutes(5))
    )

    override fun put(flowId: String, fiber: Fiber<Any>) {
        cache.put(flowId, fiber)
    }

    override fun get(flowId: String): Fiber<Any>? {
        return cache.getIfPresent(flowId)
    }

    override fun remove(flowId: String) {
        cache.invalidate(flowId)
    }
}