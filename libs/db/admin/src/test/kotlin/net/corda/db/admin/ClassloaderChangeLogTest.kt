package net.corda.db.admin

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.FileNotFoundException
import java.lang.IllegalArgumentException

class ClassloaderChangeLogTest {
    val classLoader = this::class.java.classLoader
    val changelogFiles = linkedSetOf(
        ChangeLogResourceFiles("fred", classLoader = classLoader)
    )

    @Test
    fun `when changeLogList return all resources from given folder`() {
        val cl = ClassloaderChangeLog(linkedSetOf(
            ChangeLogResourceFiles("fred", "test", classLoader)
        ))

        assertThat(cl.changeLogFileList).containsExactlyInAnyOrder(
            "${classLoader.name}:test/foo.txt"
        )
    }

    @Test
    fun `when changeLogList return all resources from default folder`() {
        val cl = ClassloaderChangeLog(changelogFiles)

        assertThat(cl.changeLogFileList).containsExactlyInAnyOrder(
            "${classLoader.name}:migration/bar.txt", "${classLoader.name}:migration/test/fred.txt"
        )
    }

    @Test
    fun `when fetch return resources as stream`() {
        val cl = ClassloaderChangeLog(changelogFiles)

        assertThat(cl.fetch("${classLoader.name}:migration/test/fred.txt").bufferedReader().use { it.readText() })
            .isEqualTo("freddy")
    }

    @Test
    fun `when fetch invalid throw not found`() {
        val cl = ClassloaderChangeLog(changelogFiles)
        assertThrows<FileNotFoundException> {
            cl.fetch("${classLoader.name}:does-not-exist/fred.txt")
        }
    }

    @Test
    fun `when fetch invalid arg throw`() {
        val cl = ClassloaderChangeLog(changelogFiles)
        assertThrows<IllegalArgumentException> {
            cl.fetch("fred.txt")
        }
    }

    @Test
    fun `when fetch invalid classloader throw`() {
        val cl = ClassloaderChangeLog(changelogFiles)
        assertThrows<IllegalArgumentException> {
            cl.fetch("does-not-exist:fred.txt")
        }
    }
}
