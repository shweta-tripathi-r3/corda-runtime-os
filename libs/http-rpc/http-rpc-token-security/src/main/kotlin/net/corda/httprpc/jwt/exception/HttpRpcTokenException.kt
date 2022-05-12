package net.corda.httprpc.jwt.exception

class JwtTokenException(override val message: String?) : Exception(message) {
}