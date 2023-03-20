package net.corda.ledger.persistence.query.impl

import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("VaultNamedQueryBuilderUtils")

fun logQueryRegistration(name: String, query: VaultNamedQuery.ParsedQuery) {
    when {
        log.isDebugEnabled -> log.debug("Registering vault named query with name: $name")
        log.isTraceEnabled -> log.trace(
            "Registering vault named query with name: $name, original query: ${query.originalQuery.replace(
                    "\n",
                    " "
                )
            }, parsed query: ${query.query}"
        )
    }
}