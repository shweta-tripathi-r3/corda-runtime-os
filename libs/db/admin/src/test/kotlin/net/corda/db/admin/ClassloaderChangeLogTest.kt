package net.corda.db.admin

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.FileNotFoundException

class ClassloaderChangeLogTest {
    @Test
    fun `when changeLogList return all resources from given folder`() {
        val cl = ClassloaderChangeLog(linkedSetOf("fred"), this::class.java.classLoader, "test")

        assertThat(cl.changeLogFileList).containsExactlyInAnyOrder(
            "test/foo.txt"
        )
    }

    @Test
    fun `when changeLogList return all resources from default folder`() {
        val cl = ClassloaderChangeLog(linkedSetOf("fred"), this::class.java.classLoader)

        assertThat(cl.changeLogFileList).containsExactlyInAnyOrder(
            "migration/bar.txt", "migration/test/fred.txt"
        )
    }

    @Test
    fun `when fetch return resources as stream`() {
        val cl = ClassloaderChangeLog(linkedSetOf("fred"), this::class.java.classLoader)

        assertThat(cl.fetch("migration/test/fred.txt").bufferedReader().use { it.readText() })
            .isEqualTo("freddy")
    }

    @Test
    fun `when fetch invalid throw not found`() {
        val cl = ClassloaderChangeLog(linkedSetOf("fred"), this::class.java.classLoader)
        assertThrows<FileNotFoundException> {
            cl.fetch("does-not-exist/fred.txt")
        }
    }
}
