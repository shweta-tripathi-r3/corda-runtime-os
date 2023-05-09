package net.corda.cli.plugins.generator

import com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemErrAndOutNormalized
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import picocli.CommandLine

class GenerateCordappSubCommandTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `if file type is not JSON or YAML (YML), exception is thrown`() {
        val app = GenerateCordappSubCommand()
        val filePath = Files.createFile(tempDir.resolve("src.txt"))
        tapSystemErrAndOutNormalized {
            CommandLine(app).execute("--file=$filePath")
        }.apply {
            assertTrue(this.contains("Input file format not supported."))
        }
    }
}
