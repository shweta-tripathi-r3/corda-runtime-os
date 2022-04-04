package net.corda.orm.impl.test.entities

import java.util.UUID
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinColumns
import jakarta.persistence.ManyToOne
import jakarta.persistence.NamedQuery

@NamedQuery(
    name = "Cat.findByOwner",
    query = "select c.name from Cat as c join c.owner as o where o.name = :owner"
)
@Entity
data class Cat(
    @Id
    @Column
    val id: UUID,
    @Column
    val name: String,
    @Column
    val colour: String,

    @ManyToOne
    @JoinColumns(
        JoinColumn(name = "owner_id", referencedColumnName = "id")
    )
    val owner: Owner?
) {
    constructor() : this(id = UUID.randomUUID(), name = "", colour = "sort-of-spotty", owner = null)
}
