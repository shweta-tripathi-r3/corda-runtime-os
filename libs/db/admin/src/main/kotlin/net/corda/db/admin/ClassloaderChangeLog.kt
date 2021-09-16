package net.corda.db.admin

import java.io.FileNotFoundException
import java.io.InputStream
import java.nio.file.Path

/**
 * Classloader implementation of [DbChange]
 * This will provide ChangeLog files that are present in the classloader as resource files.
 *
 * @property list of [ChangeLogResourceFiles] that will be processed in order. Associated classloaders will be de-duped.
 * @constructor Create empty Classloader change log
 */
class ClassloaderChangeLog(
    private val changelogFiles: LinkedHashSet<ChangeLogResourceFiles>,
) : DbChange {
    private val classLoaderMap by lazy {
        // de-dupe and put in map for quick retrieval
        changelogFiles.map {
            it.classLoader
        }.distinct().associateBy (
            // give the classloader a name if it doesn't have one already
            { createClassLoaderId(it) }, {it}
        )
    }

    override val masterChangeLogFiles by lazy {
        LinkedHashSet(changelogFiles.map { clf ->
            "${createClassLoaderId(clf.classLoader)}:${clf.rootPath}/${clf.masterFile}" })
    }

    override val changeLogFileList: Set<String> by lazy {
        changelogFiles.map { clf ->
            val clId = createClassLoaderId(clf.classLoader)
            val root = Path.of(classLoaderMap.getValue(clId).getResource(clf.rootPath).toURI())
            root.tree()
                .map { resourcePath ->
                    "$clId:${clf.rootPath}/${root.relativize(resourcePath)}"
                }
        }.flatten().toSet()
    }

    override fun fetch(path: String): InputStream {
        val splitPath = path.split(':', limit = 2)
        if(splitPath.size != 2)
            throw IllegalArgumentException("$path is not a valid resource path.")
        val cl = classLoaderMap[splitPath[0]] ?:
            throw IllegalArgumentException("Classloader ${splitPath[0]} from $path is not a valid.")
        return cl.getResourceAsStream(splitPath[1]) ?: throw FileNotFoundException("$path not found.")
    }

    private fun createClassLoaderId(classLoader: ClassLoader) =
        (classLoader.name ?: System.identityHashCode(classLoader).toString()).replace(':','|')
}

/**
 * Definition of a master change log file and associated classloader
 *
 * @property masterFile
 * @property rootPath defaulted to "migration"
 * @property classLoader defaulted to current classloader
 * @constructor
 */
data class ChangeLogResourceFiles(
    val masterFile: String,
    val rootPath: String = "migration",
    val classLoader: ClassLoader = ChangeLogResourceFiles::class.java.classLoader
)
