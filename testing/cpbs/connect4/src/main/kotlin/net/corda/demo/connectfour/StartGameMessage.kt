package net.corda.demo.connectfour

class StartGameMessage {
    var opponentX500Name: String? = null
    var column: Int? = null
    var row: Int? = null
    val boardState: Array<IntArray>? = null
}

