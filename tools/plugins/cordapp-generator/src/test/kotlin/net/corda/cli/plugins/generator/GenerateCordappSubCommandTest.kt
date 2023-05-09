package net.corda.cli.plugins.generator

import com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemErrAndOutNormalized
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
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

    @Test
    fun `if file is absent at the path, exception is thrown`() {
        val app = GenerateCordappSubCommand()
        val filePath = tempDir.resolve("src.json")
        tapSystemErrAndOutNormalized {
            CommandLine(app).execute("--file=$filePath")
        }.apply {
            assertTrue(this.contains("No such file or directory:"))
        }
    }

    @Test
    fun `if file is present at the path, file is read`() {
        val app = GenerateCordappSubCommand()
        val resource = "cordapp-spec.json"
        val file = File(javaClass.classLoader.getResource(resource).file)
        val filePath = Paths.get(file.absolutePath)
        tapSystemErrAndOutNormalized {
            CommandLine(app).execute("--file=$filePath")
        }.apply {
            assertTrue(this.contains("Your CorDapp is downloaded at"))
        }
    }
}
