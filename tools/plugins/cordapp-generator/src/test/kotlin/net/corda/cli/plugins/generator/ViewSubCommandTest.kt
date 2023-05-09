package net.corda.cli.plugins.generator

import com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemErrAndOutNormalized
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import picocli.CommandLine

class ViewSubCommandTest {

    @Test
    fun `if file type is not JSON or YAML (YML), exception is thrown`() {
        val app = ViewSubCommand()
        tapSystemErrAndOutNormalized {
            CommandLine(app).execute("-s=txt")
        }.apply {
            Assertions.assertTrue(this.contains("Please input valid file type [json/yml/yaml]"))
        }
    }

    @Test
    fun `if file type is JSON or YAML (YML), file is read`() {
        val app = ViewSubCommand()
        tapSystemErrAndOutNormalized {
            CommandLine(app).execute("-s=json")
        }.apply {
            Assertions.assertTrue(this.contains("File is read"))
        }
    }
}