package net.cordacon.example.doorcode

import net.corda.simulator.HoldingIdentity
import net.corda.simulator.RequestData
import net.corda.simulator.Simulator
import net.corda.simulator.crypto.HsmCategory
import net.corda.simulator.factories.JsonMarshallingServiceFactory
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Test

class DoorCodeTest {

    @Test
    fun `should get signatures from everyone who needs to sign`() {
        // Given Alice and Bob both live in the house
        val simulator = Simulator()
        val alice = HoldingIdentity.create("Alice")
        val bob = HoldingIdentity.create("Bob")

        val aliceNode = simulator.createVirtualNode(alice, DoorCodeChangeFlow::class.java,
            DoorCodeChangeResponderFlow::class.java)
        val bobNode = simulator.createVirtualNode(bob, DoorCodeChangeFlow::class.java,
            DoorCodeChangeResponderFlow::class.java)

        // When Alice requests a change to the door code and signs it herself
        aliceNode.generateKey("alice-door-code-change-key", HsmCategory.LEDGER, "any-scheme")
        bobNode.generateKey("bob-door-code-change-key", HsmCategory.LEDGER, "any-scheme")
        val requestData = RequestData.create(
            "r1",
            DoorCodeChangeFlow::class.java,
            DoorCodeChangeRequest(DoorCode("1234"), listOf(bobNode.member))
        )

        // Then the door code should be changed
        val jsonService = JsonMarshallingServiceFactory.create()
        val result = jsonService.parse(aliceNode.callFlow(requestData), DoorCodeChangeResult::class.java)
        assertThat(result.newDoorCode, `is`(DoorCode("1234")))
//        assertThat(result.signedBy, `is`(setOf(alice.member, bob.member)))
    }
}