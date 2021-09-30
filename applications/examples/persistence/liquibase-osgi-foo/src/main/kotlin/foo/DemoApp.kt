package foo

import net.corda.db.admin.LiquibaseSchemaMigrator
import net.corda.db.admin.impl.ClassloaderChangeLog
import net.corda.db.core.InMemoryDataSourceFactory
import net.corda.db.core.PostgresDataSourceFactory
import net.corda.osgi.api.Application
import net.corda.v5.base.util.contextLogger
import org.osgi.service.component.annotations.Activate
import org.osgi.service.component.annotations.Component
import org.osgi.service.component.annotations.Reference
import org.slf4j.Logger

@Component
class DemoApp @Activate constructor(
    @Reference(service = LiquibaseSchemaMigrator::class)
    private val schemaMigrator: LiquibaseSchemaMigrator,
) : Application {

    companion object {
        val log: Logger = contextLogger()
    }


    override fun startup(args: Array<String>) {
        log.info("Starting app")

        val dbChange = ClassloaderChangeLog(linkedSetOf(
            ClassloaderChangeLog.ChangeLogResourceFiles(
                this::class.java.packageName,
                listOf("migration/db.changelog-master.xml"),
                classLoader = this::class.java.classLoader)
        ))
        schemaMigrator.updateDb(
            InMemoryDataSourceFactory().create("foo").connection,
                dbChange)
    }

    override fun shutdown() {
        log.info("Stopping app")
    }
}
