package net.corda.cli.plugins.generator

import picocli.CommandLine

@CommandLine.Command(name = "view", description = ["View File"])
class ViewSubCommand : Runnable {

    @CommandLine.Option(
        names = ["--spec", "-s"],
        description = ["Type of spec file to be viewed [json/yaml]"]
    )
    var specFile: String? = null

    @CommandLine.Option(
        names = ["--example-spec", "-es"],
        description = ["Type of IOU spec file [json/yaml]"]
    )
    var useCase: String? = null

    override fun run() {
        try {
            val content = readFile(specFile, useCase)
            var inputMessage = CommandLine.Help.Ansi.AUTO.string("@|bold,green, File is read|@")
            System.out.format(inputMessage)
            println(content)
        } catch (e: java.lang.Exception) {
            val errorMessage = CommandLine.Help.Ansi.AUTO.string("@|bold,red, File is read|@")
            System.out.format(errorMessage)
        }
    }

    private fun readFile(specFile: String?, useCase: String?): String {

        if (specFile != null) {
            return when (specFile) {
                "json" -> this::class.java.classLoader.getResource("cordapp-spec.json").readText()
                "yaml", "yml" -> this::class.java.classLoader.getResource("cordapp-spec.yaml").readText()
                else -> "Please input valid file type [json/yml/yaml]"
            }
        }
        if (useCase != null) {
            return when (useCase) {
                "json" -> this::class.java.classLoader.getResource("iou-cordapp-spec.json").readText()
                "yaml", "yml" -> this::class.java.classLoader.getResource("iou-cordapp-spec.yaml").readText()
                else -> "Please input valid file type [json/yml/yaml]"
            }
        }
        return "Did not get any valid input"
    }
}