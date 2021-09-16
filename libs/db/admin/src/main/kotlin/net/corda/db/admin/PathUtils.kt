package net.corda.db.admin

import java.io.File
import java.nio.file.Files
import java.nio.file.Path

/**
 * Return all children of a [File]
 *
 * @return List of all child File objects.
 * Returns an empty list if [File] is not a directory
 */
fun Path.tree(): List<Path> {
    if (!Files.isDirectory(this))
        return emptyList()

    var list = mutableListOf<Path>()
    Files.list(this).forEach {
        if (Files.isDirectory(it)) {
            list.addAll(it.tree())
        } else {
            list.add(it)
        }
    }
    return list
}
