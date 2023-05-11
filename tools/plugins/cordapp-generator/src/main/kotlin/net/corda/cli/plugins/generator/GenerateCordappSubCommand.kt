package net.corda.cli.plugins.generator

import com.r3.generator.ApplicationArtifactEngine
import com.r3.generator.ApplicationFamily
import com.r3.generator.core.FileType
import java.io.File
import java.nio.file.Path
import picocli.CommandLine

//import com.r3.generator.

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
            val file = readAndValidateFile()
            val appScaffoldEngine =
                ApplicationArtifactEngine.newInstance(ApplicationFamily.Cordapp, file, FileType.JSON)
                    .ignite();
            println(appScaffoldEngine)
            var outputMessage = CommandLine.Help.Ansi.AUTO.string("@|bold,green, Your CorDapp is downloaded at: |@")
            System.out.format(outputMessage);

            outputMessage = CommandLine.Help.Ansi.AUTO.string("@|bold,blue,underline $outputPath |@")
            System.out.format(outputMessage);

        } catch (e: java.lang.Exception) {
            System.out.format(CommandLine.Help.Ansi.AUTO.string("@|bold,red Failed to read the file due to : ${e.message}|@"));
        }
    }

    /**
     * Reads the cordapp spec from a JSON or YAML formatted file.
     *
     * @return JSON/YAML file present at specified path, or null if no file was provided.
     * @throws IllegalArgumentException If the input file format is not supported
     */
    @Suppress("ComplexMethod", "ThrowsCount")
    private fun readAndValidateFile(): File? {
        return filePath?.toString()?.run {
            val file = filePath!!.toFile()
            if (!file.exists()) {
                throw IllegalArgumentException("No such file or directory: $this.")
            }
            when {
                endsWith(".json") || endsWith(".yaml") || endsWith(".yml") -> return file
                else -> throw IllegalArgumentException("Input file format not supported.")
            }
        }
    }
}