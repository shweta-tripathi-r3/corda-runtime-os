package net.corda.orm.impl.test.entities

import java.util.UUID
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id

@Entity
data class MutableEntity(
    @Id
    @Column
    val id: UUID,
    @Column
    var tag: String,
)