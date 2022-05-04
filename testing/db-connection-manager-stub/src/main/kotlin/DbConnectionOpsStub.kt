import net.corda.db.connection.manager.DBConfigurationException
import net.corda.db.connection.manager.DbConnectionOps
import net.corda.db.connection.manager.createFromConfig
import net.corda.db.core.CloseableDataSource
import net.corda.db.core.DbPrivilege
import net.corda.libs.configuration.SmartConfig
import net.corda.orm.DbEntityManagerConfiguration
import net.corda.orm.EntityManagerFactoryFactory
import net.corda.orm.JpaEntitiesSet
import net.corda.orm.impl.EntityManagerFactoryFactoryImpl
import java.util.UUID
import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory
import javax.sql.DataSource

class DbConnectionOpsStub(
    val dataSourceFactory: DataSourceFactoryStub,
    val connections: MutableMap<Pair<String, DbPrivilege>, DataSource> = mutableMapOf(),
    val emfs: MutableMap<Pair<String, DbPrivilege>, EntityManagerFactory> = mutableMapOf(),
    val emff: EntityManagerFactoryFactory = EntityManagerFactoryFactoryImpl(),
) : DbConnectionOps {
    override fun putConnection(
        name: String,
        privilege: DbPrivilege,
        config: SmartConfig,
        description: String?,
        updateActor: String
    ): UUID {
        connections[Pair(name, privilege)] = dataSourceFactory.createFromConfig(config)
        return UUID.randomUUID()
    }

    override fun putConnection(
        entityManager: EntityManager,
        name: String,
        privilege: DbPrivilege,
        config: SmartConfig,
        description: String?,
        updateActor: String
    ): UUID {
        return putConnection(name, privilege, config, description, updateActor)
    }

    override fun getClusterDataSource(): DataSource {
        return dataSourceFactory.create()
    }

    override fun getDataSource(name: String, privilege: DbPrivilege): DataSource? {
        return connections[Pair(name, privilege)]
    }

    override fun getDataSource(config: SmartConfig): CloseableDataSource {
        return dataSourceFactory.createFromConfig(config)
    }

    override fun getClusterEntityManagerFactory(): EntityManagerFactory = emfs[Pair("custer", DbPrivilege.DML)]
        ?: throw DBConfigurationException("'cluster' DB not configured")

    override fun getOrCreateEntityManagerFactory(
        db: net.corda.db.schema.CordaDb,
        privilege: DbPrivilege
    ): EntityManagerFactory = emfs[Pair(db.name, privilege)]
        ?: throw DBConfigurationException("'${db.name}' DB not configured")

    override fun getOrCreateEntityManagerFactory(
        name: String,
        privilege: DbPrivilege,
        entitiesSet: JpaEntitiesSet
    ): EntityManagerFactory {
        if(!emfs.containsKey(Pair(name, privilege)))
            emfs[Pair(name, privilege)] = emff.create(
                name,
                entitiesSet.classes.toList(),
                DbEntityManagerConfiguration(dataSourceFactory.create()),
            )
        return emfs[Pair(name, privilege)]!!
    }

    fun addClusterDb(entities: List<Class<*>>)
    {
        add("cluster", DbPrivilege.DML, entities)
    }

    fun add(name: String,
            privilege: DbPrivilege,
            entities: List<Class<*>>)
    {
        emfs[Pair(name, privilege)] = emff.create(
            name,
            entities,
            DbEntityManagerConfiguration(dataSourceFactory.create()),
        )
    }
}