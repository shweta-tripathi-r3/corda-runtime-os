package net.corda.membership.certificates.datamodel

import net.corda.db.schema.DbSchema
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

/**
 * An entity representing a single certificate.
 */
@Entity
@Table(name = DbSchema.VNODE_CERTIFICATE_DB_TABLE)
data class Certificate(
    @Id
    @Column(name = "alias", nullable = false, updatable = false)
    override val alias: String,

    @Column(name = "usage", nullable = false, updatable = false)
    override val usage: String,

    @Column(name = "raw_certificate", nullable = false, updatable = true)
    override val rawCertificate: String,
) : CertificateEntity
