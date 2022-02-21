package net.corda.introspiciere.cli

import picocli.CommandLine

fun main(vararg args: String) {
    internalMain(*args)
}

internal fun internalMain(vararg args: String) {
    CommandLine(Subcommands()).execute(*args)
}