package net.corda.cli.plugins.zerocode

import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.nio.file.Path
import picocli.CommandLine

@CommandLine.Command(name = "corDapp", description = ["Generates CorDapp structure"])
class ZeroCodeSubCommand : Runnable {

    @CommandLine.Option(
        names = ["--file", "-f"],
        arity = "0..1",
        description = ["Path to a JSON or YAML file that contains static network information"]
    )
    var filePath: Path? = null

    override fun run() {
//        val objectMapper = jacksonObjectMapper()
        // validate input file first
        //read contents from file
        val content = readAndValidateFile()

        //print
        println("Read file")
        println(content.toString())
    }

    /**
     * Reads and validates static network information from a JSON or YAML formatted file.
     *
     * @return Static network information as [Map], or null if no file was provided.
     * @throws IllegalArgumentException If the input file format is not supported, the file is malformed, or the file
     * contains an invalid combination of blocks e.g. both 'memberNames' and 'members' blocks are present.
     */
    @Suppress("ComplexMethod", "ThrowsCount")
    private fun readAndValidateFile(): Map<String, Any>? {
        return filePath?.toString()?.run {
            val file = filePath!!.toFile()
            if (!file.exists()) {
                throw IllegalArgumentException("No such file or directory: $this.")
            }
            when {
                endsWith(".json") -> {
                    try {
                        jacksonObjectMapper().readValue<Map<String, Any>>(file)
                    } catch (e: MismatchedInputException) {
                        throw IllegalArgumentException("Could not read static network information from $this.")
                    }
                }
//                endsWith(".yaml") || endsWith(".yml") -> {
//                    Yaml().load(file.readText())
//                        ?: throw IllegalArgumentException("Could not read static network information from $this.")
//                }
                else -> throw IllegalArgumentException("Input file format not supported.")
            }.also { parsed ->
                val hasMemberNames = parsed["memberNames"] != null
                val hasMembers = parsed["members"] != null
                if (hasMemberNames && hasMembers) {
                    throw IllegalArgumentException("Only one of 'memberNames' and 'members' blocks may be specified.")
                }
                if (hasMemberNames) {
                    require(parsed["endpoint"] != null) { "Endpoint must be specified." }
                    require(parsed["endpointProtocol"] != null) { "Endpoint protocol must be specified." }
                }
            }
        }
    }
}