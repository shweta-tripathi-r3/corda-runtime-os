package net.corda.sample.goodbye

import net.corda.install.InstallService
import net.corda.sample.api.hello.HelloWorld
import org.osgi.framework.BundleActivator
import org.osgi.framework.BundleContext
import org.osgi.service.component.annotations.Activate
import org.osgi.service.component.annotations.Component
import org.osgi.service.component.annotations.Deactivate
import org.osgi.service.component.annotations.Reference

@Component(immediate = true)
class GoodbyeWorld: BundleActivator {

    @Reference
    var helloWorld: HelloWorld? = null

    @Reference
    var installService: InstallService? = null

    fun callHelloWorld() {
        helloWorld?.sayHello() ?: println("We couldn't find hello world!!!")

    }

    fun installCpk() {
        if (installService != null ){
            println("INSTALL...")
        } else {
            println("NO INSTALL!")
        }
    }

    @Activate
    override fun start(context: BundleContext?) {
        println("net.corda.sample.goodbye.GoodbyeWorld START")
        callHelloWorld()
        installCpk()
    }

    @Deactivate
    override fun stop(context: BundleContext?) {
        println("net.corda.sample.goodbye.GoodbyeWorld STOP")
    }

}

