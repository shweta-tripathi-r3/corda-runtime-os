import net.corda.db.connection.manager.DbConnectionManager
import net.corda.db.connection.manager.DbConnectionOps
import net.corda.db.core.DataSourceFactory
import net.corda.db.core.DbPrivilege
import net.corda.libs.configuration.SmartConfig
import net.corda.v5.base.util.contextLogger
import org.osgi.service.component.annotations.Component
import org.osgi.service.component.propertytypes.ServiceRanking

@ServiceRanking(Int.MAX_VALUE)
@Component(service = [DbConnectionManager::class, DbConnectionManagerStub::class])
class DbConnectionManagerStub(
    val dataSourceFactory: DataSourceFactoryStub = DataSourceFactoryStub(),
    val dbConnectionOps: DbConnectionOpsStub = DbConnectionOpsStub(dataSourceFactory)
): DbConnectionManager, DbConnectionOps by dbConnectionOps, DataSourceFactory by dataSourceFactory {
    private companion object {
        private val logger = contextLogger()
    }

    private var smartConfig: SmartConfig? = null

    fun addClusterDb(entities: List<Class<*>>)
    {
        dbConnectionOps.addClusterDb(entities)
    }

    fun add(name: String,
            privilege: DbPrivilege,
            entities: List<Class<*>>)
    {
        dbConnectionOps.add(name, privilege, entities)
    }

    override fun initialise(config: SmartConfig) {
        smartConfig = config
        logger.info("Stub DbConnectionManager initialised with $config")
    }

    override val clusterConfig: SmartConfig
        get() = smartConfig!!

    override fun bootstrap(config: SmartConfig) {
        smartConfig = config
        logger.info("Stub DbConnectionManager bootstrapped with $config")
    }

    override val isRunning: Boolean
        get() = true

    override fun start() {
        logger.info("Stub DbConnectionManager started")
    }

    override fun stop() {
        logger.info("Stub DbConnectionManager stopped")
    }
}

