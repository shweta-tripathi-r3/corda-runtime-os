package net.corda.cli.plugins.generator

import picocli.CommandLine

@CommandLine.Command(name = "view", description = ["View File"], mixinStandardHelpOptions = true)
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
        val content = readFile(specFile, useCase)
        println("Read file")
        println(content)
    }

    private fun readFile(specFile: String?, useCase: String?): String {

        if (specFile != null) {
            return when (specFile) {
                "json" -> this::class.java.classLoader.getResource("spec.json").readText()
                "yaml", "yml" -> this::class.java.classLoader.getResource("spec.yml").readText()
                else -> "Please input valid file type [json/yml/yaml]"
            }
        }
        if (useCase != null) {
            return when (useCase) {
                "json" -> this::class.java.classLoader.getResource("iou-cordapp-spec.json").readText()
                "yaml", "yml" -> this::class.java.classLoader.getResource("iou-cordapp-spec.yml").readText()
                else -> "Please input valid file type [json/yml/yaml]"
            }
        }
        return "Did not get any valid input"
    }
}