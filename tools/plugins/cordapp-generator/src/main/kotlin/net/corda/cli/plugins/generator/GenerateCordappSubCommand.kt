package net.corda.cli.plugins.generator

import com.r3.generator.ApplicationFamily
import com.r3.generator.ApplicationGenerateArtifactEngine
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

    @CommandLine.Option(
        names = ["--workspace", "-w"],
        description = ["Path to the intermediate directory where CorDapp files will be downloaded"]
    )
    var workspacePath: Path? = null

    private val fileBeingRead = "     File is being read"
    private val cordappGeneration = "     CorDapp will be generated soon"
    private val cordappDownload = "     Your CorDapp is downloaded at:"
    override fun run() {
        //read contents from file
        var inputMessage = CommandLine.Help.Ansi.AUTO.string("@|bold,yellow, $fileBeingRead |@")
        System.out.format(inputMessage)
        inputMessage = CommandLine.Help.Ansi.AUTO.string("@|bold,yellow, $cordappGeneration |@")
        System.out.format(inputMessage)
        try {
            val file = readAndValidateFile()
            val hashMap = HashMap<String, String>()
            hashMap["out-dir"] = outputPath.toString()
            hashMap["workspace-path"] = workspacePath.toString()
            val artifactInfo = ApplicationGenerateArtifactEngine.newInstance(
                ApplicationFamily.Cordapp,
                file,
                FileType.JSON,
                hashMap
            ).ignite();

            println(artifactInfo.generatedApplicationSourcePath)
            var outputMessage = CommandLine.Help.Ansi.AUTO.string("@|bold,green  $cordappDownload |@")
            System.out.format(outputMessage);

            outputMessage = CommandLine.Help.Ansi.AUTO.string("@|bold,blue,underline $outputPath |@")
            System.out.format(outputMessage);

        } catch (e: java.lang.Exception) {
            System.out.format(CommandLine.Help.Ansi.AUTO.string("@|bold,red Failed to read the file due to : ${e.message} :|@"));
            for(error in e.stackTrace){
                println(error)
            }
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
                else -> throw IllegalArgumentException("Input file format not supported. We just support JSON/YAML at the moment")
            }
        }
    }
}