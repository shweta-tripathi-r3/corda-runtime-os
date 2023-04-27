package net.cordapp.testing.calculator

/**
 * Class used to marshall input parameters for ChatOutgoingFlow from JSON.
 */
data class ChatOutgoingFlowParameter(
    val recipientX500Name: String? = null,
    val message: String? = null
)