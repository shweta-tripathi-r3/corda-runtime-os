package net.corda.httprpc.jwt

interface HttpRpcTokenProcessor {
    fun buildAndSignToken(subject: String): String
    fun getSubject(jwt: String): String
    fun verify(jwt: String): Boolean
}