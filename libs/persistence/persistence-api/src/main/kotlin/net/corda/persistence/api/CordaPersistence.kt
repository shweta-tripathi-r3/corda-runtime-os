package net.corda.persistence.api

import java.io.Closeable
import javax.persistence.EntityManager

interface CordaPersistence : Closeable {
    fun createEntityManager(
        bundleClassloader: ClassLoader,
//        entities: List<String>,
        schemas: List<MappedSchema>,
        connectionUrl: String,
        login: String,
        password:String): EntityManager
}