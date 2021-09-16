package net.corda.db.admin.impl

import liquibase.resource.AbstractResourceAccessor
import liquibase.resource.ClassLoaderResourceAccessor
import liquibase.resource.InputStreamList
import liquibase.resource.ResourceAccessor
import net.corda.db.admin.DbChange
import net.corda.v5.base.util.contextLogger
import java.io.ByteArrayInputStream
import java.io.StringWriter
import java.lang.UnsupportedOperationException
import java.net.URI
import java.util.SortedSet
import javax.xml.bind.JAXBContext
import javax.xml.bind.Marshaller
import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAttribute
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlRootElement

/**
 * Implementation of [liquibase.resource.ResourceAccessor] that handles with
 * Stream resources directly and supports aggregating master changelogs.
 * Getting the streams is delegated to [dbChange].
 *
 * @property masterChangeLogFileName is the name of the (generated) master log file
 * @property dbChange
 * @constructor Create empty Stream resource accessor
 */
class StreamResourceAccessor(
    val masterChangeLogFileName: String,
    val dbChange: DbChange,
    private val classLoaderResourceAccessor: ResourceAccessor =
        ClassLoaderResourceAccessor(ClassLoaderResourceAccessor::class.java.classLoader)
) : AbstractResourceAccessor() {
    companion object {
        private val log = contextLogger()
    }

    val xmlMarshaller by lazy {
        val marshaller = JAXBContext.newInstance(CompositeDatabaseChangeLog::class.java).createMarshaller()
        marshaller.setProperty(
            Marshaller.JAXB_SCHEMA_LOCATION,
            "http://www.liquibase.org/xml/ns/dbchangelog http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.3.xsd"
        )
        marshaller
    }

    /**
     * Return the streams for each resource mapped by the given path.
     * The path is often a URL but does not have to be.
     * Should accept both / and \ chars for file paths to be platform-independent.
     * If path points to a compressed resource, return a stream of the uncompressed contents of the file.
     * Returns [InputStreamList] since multiple resources can map to the same path, such as "META-INF/MAINFEST.MF".
     * Remember to close streams when finished with them.
     *
     * @param relativeTo Location that streamPath should be found relative to. If null, streamPath is an absolute path
     * @return Empty list if the resource does not exist.
     * @throws IOException if there is an error reading an existing path.
     */
    override fun openStreams(relativeTo: String?, streamPath: String?): InputStreamList {
        if (masterChangeLogFileName == streamPath) {
            log.info("Creating composite master changelog file $masterChangeLogFileName with: ${dbChange.masterChangeLogFiles}")
            // dynamically create the master file by combining the specified.
            val master = CompositeDatabaseChangeLog(
                dbChange.masterChangeLogFiles.map {
                    Include(it)
                }
            )
            // using string writer for ease of debugging.
            StringWriter().use {
                xmlMarshaller.marshal(master, it)
                log.debug("Composite master file: $it")
                return InputStreamList(URI(masterChangeLogFileName), ByteArrayInputStream(it.toString().toByteArray()))
            }
        }
        if (null != streamPath && !dbChange.changeLogFileList.contains(streamPath)) {
            // NOTE: this is needed for fetching the XML schemas
            log.debug("'$streamPath' not a known change log ... delegating to ClassLoaderResourceAccessor")
            return classLoaderResourceAccessor.openStreams(relativeTo, streamPath)
        }
        if (null == relativeTo && null != streamPath) {
            log.debug("Fetching change log from: $streamPath")
            return InputStreamList(URI(streamPath), dbChange.fetch(streamPath))
        }

        throw UnsupportedOperationException(
            "openStreams with arguments '$relativeTo' and '$streamPath' not supported"
        )
    }

    /**
     * Returns the path to all resources contained in the given path.
     * The passed path is not included in the returned set.
     * Returned strings should use "/" for file path separators, regardless of the OS and should accept both / and \
     * chars for file paths to be platform-independent.
     * Returned set is sorted, normally alphabetically but subclasses can use different comparators.
     * The values returned should be able to be passed into [.openStreams] and return the contents.
     * Returned paths should normally be root-relative and therefore not be an absolute path, unless there is a good
     * reason to be absolute.
     *
     *
     * @param relativeTo Location that streamPath should be found relative to. If null, path is an absolute path
     * @param path The path to lookup resources in.
     * @param recursive Set to true and will return paths to contents in sub directories as well.
     * @param includeFiles Set to true and will return paths to files.
     * @param includeDirectories Set to true and will return paths to directories.
     * @return empty set if nothing was found
     * @throws IOException if there is an error reading an existing root.
     */
    override fun list(
        relativeTo: String?,
        path: String?,
        recursive: Boolean,
        includeFiles: Boolean,
        includeDirectories: Boolean
    ): SortedSet<String> {
        // NOTE: this method doesn't seem to be used by Liquibase internally, hence not implemented.
        TODO("Not yet implemented")
    }

    /**
     * Returns a description of the places this classloader will look for paths.
     * Used in error messages and other troubleshooting cases.
     */
    override fun describeLocations(): SortedSet<String> {
        return (dbChange.changeLogFileList + masterChangeLogFileName)
            .map { "[${dbChange.javaClass.simpleName}]$it" }.toSortedSet()
    }

    @XmlRootElement(
        name = "databaseChangeLog",
        namespace = "http://www.liquibase.org/xml/ns/dbchangelog"
    )
    @XmlAccessorType(XmlAccessType.PROPERTY)
    class CompositeDatabaseChangeLog(
        @field:XmlElement(
            name = "include",
            namespace = "http://www.liquibase.org/xml/ns/dbchangelog"
        )
        val includes: List<Include>
    ) {
        constructor() : this(emptyList())
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    class Include(@field:XmlAttribute(name = "file") val file: String) {
        constructor() : this("")
    }
}
