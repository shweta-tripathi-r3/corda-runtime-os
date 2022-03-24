package net.corda.demo.connectfour

data class Move(
    val playerX500Name: String,
    val colPlayed: Int,
    val rowPlayed: Int
)