import net.corda.db.core.CloseableDataSource
import net.corda.db.core.DataSourceFactory
import net.corda.db.core.InMemoryDataSourceFactory
import net.corda.db.testkit.DbUtils

class DataSourceFactoryStub: DataSourceFactory {
    override fun create(
        driverClass: String,
        jdbcUrl: String,
        username: String,
        password: String,
        isAutoCommit: Boolean,
        maximumPoolSize: Int
    ): CloseableDataSource {
        return create()
    }

    fun create(): CloseableDataSource {
        if(DbUtils.isInMemory) return InMemoryDataSourceFactory().create("test-db")
        return DbUtils.createPostgresDataSource()
    }
}