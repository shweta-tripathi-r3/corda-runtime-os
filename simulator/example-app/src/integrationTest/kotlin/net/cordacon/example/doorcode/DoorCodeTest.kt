package net.cordacon.example.doorcode

import net.corda.simulator.HoldingIdentity
import net.corda.simulator.RequestData
import net.corda.simulator.Simulator
import net.corda.simulator.crypto.HsmCategory
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Test

class DoorCodeTest {

    @Test
    fun `should change the door code if no other signature is required`() {
        // Given only Alice lives in the house
        val simulator = Simulator()
        val aliceNode = simulator.createVirtualNode(HoldingIdentity.create("Alice"), DoorCodeChangeFlow::class.java)

        // When Alice requests a change to the door code and signs it herself
        aliceNode.generateKey("door-code-change-key", HsmCategory.LEDGER, "any-scheme")
        val requestData = RequestData.create(
            "r1",
            DoorCodeChangeFlow::class.java,
            DoorCodeChangeRequest(DoorCode("1234"), listOf())
        )
        val result = aliceNode.callFlow(requestData)

        // Then the door code should be changed
        assertThat(result, `is`("Door code changed to 1234"))
    }
}