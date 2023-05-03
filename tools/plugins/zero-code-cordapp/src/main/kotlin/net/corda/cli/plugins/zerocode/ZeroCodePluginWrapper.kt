package net.corda.cli.plugins.zerocode

import net.corda.cli.api.CordaCliPlugin
import org.pf4j.Extension
import org.pf4j.Plugin
import org.pf4j.PluginWrapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import picocli.CommandLine

class ZeroCodePluginWrapper(wrapper: PluginWrapper) : Plugin(wrapper) {
    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }

    override fun start() {
        logger.info("Zero Code Plugin Started!")
    }

    override fun stop() {
        logger.info("Zero Code Plugin Stopped!")
    }

    @Extension
    @CommandLine.Command(
        name = "zero-code",
        mixinStandardHelpOptions = true,
        subcommands = [ZeroCodeSubCommand::class],
        description = ["Plugin for creating zero-code CorDapp"]
    )
    class ZeroCodePlugin : CordaCliPlugin
}