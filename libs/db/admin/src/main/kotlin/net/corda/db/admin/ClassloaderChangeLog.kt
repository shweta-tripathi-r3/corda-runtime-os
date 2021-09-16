package net.corda.db.admin

import java.io.FileNotFoundException
import java.io.InputStream
import java.nio.file.Path

/**
 * Classloader implementation of [DbChange]
 * This will provide ChangeLog files that are present in the classloader as resource files.
 *
 * @property changelogFiles relative to the specified [rootPath]
 * @property classLoader defaulted to current classloader
 * @property rootPath defaulted to "migration"
 * @constructor Create empty Classloader change log
 */
class ClassloaderChangeLog(
    private val changelogFiles: LinkedHashSet<String>,
    private val classLoader: ClassLoader = ClassloaderChangeLog::class.java.classLoader,
    private val rootPath: String = "migration"
) : DbChange {
    override val masterChangeLogFiles by lazy {
        LinkedHashSet(changelogFiles.map { "$rootPath/$it" })
    }

    override val changeLogFileList by lazy {
        val root = Path.of(classLoader.getResource(rootPath).toURI())
        root.tree()
            .map {
                "$rootPath/${root.relativize(it)}"
            }.toSet()
    }

    override fun fetch(path: String): InputStream {
        return classLoader.getResourceAsStream(path) ?: throw FileNotFoundException("$path not found.")
    }
}
