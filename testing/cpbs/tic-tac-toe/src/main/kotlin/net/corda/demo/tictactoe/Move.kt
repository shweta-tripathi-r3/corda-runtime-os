package net.corda.demo.tictactoe

data class Move(
    val playerX500Name: String,
    val colPlayed: Int,
    val rowPlayed: Int
)