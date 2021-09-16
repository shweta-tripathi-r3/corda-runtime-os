package net.corda.db.admin

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Files

class PathUtilsTest {
    val fs = Jimfs.newFileSystem(Configuration.unix())

    @Test
    fun `tree returns empty list when not directory`() {
        val file = Files.createFile(fs.getPath("file.foo"))
        assertThat(file.tree()).isEmpty()
    }

    @Test
    fun `tree finds files recursively`() {
        val root = Files.createDirectory(fs.getPath("hello"))
        val one = Files.createFile(root.resolve("file.foo"))
        val dir = Files.createDirectory(root.resolve("dir"))
        val two = Files.createFile(dir.resolve("file.foo"))
        val dir2 = Files.createDirectory(dir.resolve("another"))
        val three = Files.createFile(dir2.resolve("file.foo"))
        val four = Files.createFile(dir2.resolve("file2.foo"))
        assertThat(root.tree()).containsExactlyInAnyOrder(one, two, three, four)
    }
}
