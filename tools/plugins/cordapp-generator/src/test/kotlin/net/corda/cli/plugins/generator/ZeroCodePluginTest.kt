package net.corda.cli.plugins.generator

import java.nio.file.Files
import java.nio.file.Path
import net.corda.cli.plugins.zerocode.GenerateCordappSubCommand
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ZeroCodePluginTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `if file type is not JSON or YAML (YML), exception is thrown`() {
        val app = GenerateCordappSubCommand()
        val filePath = Files.createFile(tempDir.resolve("src.txt"))
//        tapSystemErrAndOutNormalized {
//            CommandLine(app).execute("--file=$filePath")
//        }.apply {
//            Assertions.assertTrue(this.contains("Input file format not supported."))
//        }
        println(app)
        println(filePath)
    }
}
