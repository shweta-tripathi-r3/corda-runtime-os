package net.corda.introspiciere.cli.topics

import net.corda.introspiciere.cli.internalMain
import net.corda.testdoubles.http.StartFakeHttpServer
import net.corda.testdoubles.http.fakeHttpServer
import net.corda.testdoubles.streams.StandardStreams
import net.corda.testdoubles.streams.inMemoryStdout
import net.corda.testdoubles.util.parse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@StartFakeHttpServer
@StandardStreams
class DeleteTopicCommandTest {
    @Test
    fun `delete topic successfully`() {
        fakeHttpServer.handle("delete", "/topics/topic1") { }

        internalMain(
            *"topics delete --endpoint ${fakeHttpServer.endpoint} --topic topic1".parse(),
            overrideStdout = inMemoryStdout.outputStream
        )

        assertEquals(
            String.format(DeleteTopicCommand.successMessage, "topic1"),
            inMemoryStdout.readText().trim(),
            "expeced output"
        )
    }
}

