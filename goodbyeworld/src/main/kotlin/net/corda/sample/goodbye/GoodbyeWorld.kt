package net.corda.sample.goodbye

import net.corda.install.Cpi
import net.corda.install.InstallService
import net.corda.sample.api.hello.HelloWorld
import net.corda.sandbox.SandboxService
import org.osgi.framework.BundleActivator
import org.osgi.framework.BundleContext
import org.osgi.service.component.annotations.Activate
import org.osgi.service.component.annotations.Component
import org.osgi.service.component.annotations.Deactivate
import org.osgi.service.component.annotations.Reference
import java.nio.file.Path
import java.nio.file.Paths

@Component(immediate = true)
class GoodbyeWorld : BundleActivator {

    @Reference
    var helloWorld: HelloWorld? = null

    @Reference
    var installService: InstallService? = null

    @Reference
    var sandboxService: SandboxService? = null

    val path = Paths.get("/home/ldebiasi/IdeaProjects/flow-worker/cpk-test/build/libs/")

    fun callHelloWorld() {
        helloWorld?.sayHello() ?: println("We couldn't find hello world!!!")
    }

    fun installCpk(path: Path) {
        if (installService != null) {
            println("INSTALL...")
            val cpiIdentifier = "unique_cpi_identifier"
            val cpkUris = installService!!.scanForCpks(setOf(path.toUri()))
            val cpis = if (cpkUris.isNotEmpty()) {
                val cpkUriList = cpkUris.toList()
                listOf(Cpi(cpiIdentifier, cpkUriList[0], cpkUriList.drop(1).toSet(), emptyMap()))
            } else {
                emptyList()
            }
            cpis.forEach { cpi ->
                installService!!.installCpi(cpi)
                val loadedCpi = installService!!.getCpi(cpiIdentifier)
                    ?: throw IllegalArgumentException("CPI $cpiIdentifier has not been installed.")
                val sandbox = sandboxService!!.createSandbox(cpiIdentifier)
                println(sandbox)
            }
            println("INSTALL $cpis")
        } else {
            println("NO INSTALL!")
        }
    }

    @Activate
    override fun start(context: BundleContext?) {
        println("net.corda.sample.goodbye.GoodbyeWorld START")
//        Thread {
            println("net.corda.sample.goodbye.GoodbyeWorld RUN")
            installCpk(path)
//        }.start()
//        Thread.sleep(5000)
    }

    @Deactivate
    override fun stop(context: BundleContext?) {
        println("net.corda.sample.goodbye.GoodbyeWorld STOP")
    }

}

