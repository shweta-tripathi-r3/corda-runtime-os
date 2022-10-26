package net.corda.libs.virtualnode.datamodel

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table
import javax.persistence.Version
import net.corda.db.schema.DbSchema.CONFIG

@Entity
@Table(name = "virtual_node_operation", schema = CONFIG)
class VirtualNodeOperation(
    @Id
    @Column(name = "id", nullable = false)
    val id: String,

    @Version
    @Column(name = "entity_version", nullable = false)
    val version: Int
) {

}