package net.corda.cli.plugins.generator

import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.nio.file.Path
import org.yaml.snakeyaml.Yaml
import picocli.CommandLine

@CommandLine.Command(name = "generate", description = ["Generates CorDapp structure"])
class GenerateCordappSubCommand : Runnable {

    @CommandLine.Option(
        names = ["--file", "-f"],
        arity = "0..1",
        description = ["Path to a JSON or YAML file that contains the cordapp spec file"]
    )
    var filePath: Path? = null

    @CommandLine.Option(
        names = ["--output", "-o"],
        description = ["Path to the output directory where CorDapp zip needs to be downloaded"]
    )
    var outputPath: Path? = null
    override fun run() {
        //read contents from file
        var inputMessage = CommandLine.Help.Ansi.AUTO.string("@|bold,yellow, File is being read|@")
        System.out.format(inputMessage)
        inputMessage = CommandLine.Help.Ansi.AUTO.string("@|bold,yellow, CorDapp will be generated soon|@")
        System.out.format(inputMessage)
        try {
            readAndValidateFile()
            var outputMessage = CommandLine.Help.Ansi.AUTO.string("@|bold,green, Your CorDapp is downloaded at: |@")
            System.out.format(outputMessage);

            outputMessage = CommandLine.Help.Ansi.AUTO.string("@|bold,blue,underline $outputPath |@")
            System.out.format(outputMessage);

        } catch (e: IllegalArgumentException) {
            System.out.format(CommandLine.Help.Ansi.AUTO.string("@|bold,red Failed to read the file due to : ${e.message}|@"));
        }
        // val appScaffoldEngine = DefaultApplicationScaffoldEngine(ApplicationFamily.Cordapp,)
    }

    /**
     * Reads the cordapp spec from a JSON or YAML formatted file.
     *
     * @return Cordapp specification as [Map], or null if no file was provided.
     * @throws IllegalArgumentException If the input file format is not supported
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
                        throw IllegalArgumentException("Could not read cordapp specification from $this.")
                    }
                }
                endsWith(".yaml") || endsWith(".yml") -> {
                    Yaml().load(file.readText())
                        ?: throw IllegalArgumentException("Could not read cordapp specification from $this.")
                }
                else -> throw IllegalArgumentException("Input file format not supported.")
            }
        }
    }
}