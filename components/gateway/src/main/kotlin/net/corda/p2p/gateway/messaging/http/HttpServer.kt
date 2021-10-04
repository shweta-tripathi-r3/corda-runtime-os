package net.corda.p2p.gateway.messaging.http

import com.typesafe.config.Config
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.timeout.IdleStateHandler
import net.corda.configuration.read.ConfigurationHandler
import net.corda.configuration.read.ConfigurationReadService
import net.corda.lifecycle.LifecycleCoordinatorFactory
import net.corda.p2p.gateway.domino.DominoLifecycle.State
import net.corda.p2p.gateway.domino.LeafDominoLifecycle
import net.corda.p2p.gateway.messaging.GatewayConfiguration
import net.corda.p2p.gateway.messaging.RevocationConfig
import net.corda.p2p.gateway.messaging.RevocationConfigMode
import net.corda.p2p.gateway.messaging.SslConfiguration
import net.corda.v5.base.util.contextLogger
import java.net.SocketAddress
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import javax.net.ssl.KeyManagerFactory

/**
 * The [HttpServer] is responsible for opening a socket listener on the configured port in order to receive and handle
 * multiple HTTP(S) connections.
 *
 * The server hosts only one endpoint which can be used for POST requests. In the future, it may host versioned endpoints.
 *
 * The body of the POST requests should contain serialised (and possibly encrypted) Corda P2P messages. The server is not
 * responsible for validating these messages, only the request headers. Once a request is checks out, its body is sent upstream
 * and a response is sent back to the client. The response body is empty unless it follows a session handshake request,
 * in which case the body will contain additional information.
 */
class HttpServer(
    private val eventListener: HttpEventListener,
    private val configurationService: ConfigurationReadService,
    private val coordinatorFactory: LifecycleCoordinatorFactory
) : LeafDominoLifecycle(coordinatorFactory),
    ConfigurationHandler,
    HttpEventListener {

    companion object {
        private val logger = contextLogger()

        /**
         * Default number of thread to use for the worker group
         */
        private const val NUM_SERVER_THREADS = 4

        /**
         * The channel will be closed if neither read nor write was performed for the specified period of time.
         */
        private const val SERVER_IDLE_TIME_SECONDS = 5
    }

    private val clientChannels = ConcurrentHashMap<SocketAddress, Channel>()

    private var bossGroup: NioEventLoopGroup? = null
    private var workerGroup: NioEventLoopGroup? = null
    private var serverChannel: Channel? = null

    private var configRegistration: AutoCloseable? = null
    private var gatewayConfiguration: GatewayConfiguration? = null

    override fun startSequence() {
        if (gatewayConfiguration != null) {
            if (state != State.Started) {
                startResources()
            }
        } else {
            if (configRegistration == null) {
                configurationService.registerForUpdates(this)
            }
        }
    }

    override fun stopSequence() {
        bossGroup?.shutdownGracefully()
        bossGroup?.terminationFuture()?.sync()
        workerGroup?.shutdownGracefully()
        workerGroup?.terminationFuture()?.sync()
        serverChannel?.let {
            if(it.isOpen) {
                it.close().sync()
            }
        }
        clientChannels.clear()
    }

    private fun startResources() {
        bossGroup = NioEventLoopGroup(1)
        workerGroup = NioEventLoopGroup(NUM_SERVER_THREADS)
        val server = ServerBootstrap()
        server.group(bossGroup, workerGroup).channel(NioServerSocketChannel::class.java)
            .childHandler(ServerChannelInitializer(gatewayConfiguration!!.sslConfig))
        val host = gatewayConfiguration!!.hostAddress
        val port = gatewayConfiguration!!.hostPort
        logger.info("Trying to bind to $host:$port")
        val channelFuture = server.bind(host, port).sync()
        if (channelFuture.isSuccess) {
            serverChannel = channelFuture.channel()
            logger.info("Listening on port $port")
            hasStarted()
        } else {
            logger.warn("Failed to bind to $host:$port")
            gotError(channelFuture.cause())
        }
    }

    private fun restartServer() {
        stopSequence()
        startResources()
    }

    /**
     * Writes the given message to the channel corresponding to the recipient address. This method should be called
     * by upstream services in order to send an HTTP response
     * @param message
     * @param destination
     * @throws IllegalStateException if the connection to the peer is not active. This can happen because either the peer
     * has closed it or the server is stopped
     */
    @Throws(IllegalStateException::class)
    fun write(statusCode: HttpResponseStatus, message: ByteArray, destination: SocketAddress) {
        // YIFT: This method (together with the clientChannels) should move to the InboundMessageHandler
        val channel = clientChannels[destination]
        if (channel == null) {
            throw IllegalStateException("Connection to $destination not active")
        } else {
            logger.debug("Writing HTTP response to channel $channel")
            val response = HttpHelper.createResponse(message, statusCode)
            channel.writeAndFlush(response)
            logger.debug("Done writing HTTP response to channel $channel")
        }
    }

    override fun onOpen(event: HttpConnectionEvent) {
        clientChannels[event.channel.remoteAddress()] = event.channel
        eventListener.onOpen(event)
    }

    override fun onClose(event: HttpConnectionEvent) {
        clientChannels.remove(event.channel.remoteAddress())
        eventListener.onClose(event)
    }

    override fun onMessage(message: HttpMessage) {
        eventListener.onMessage(message)
    }

    private inner class ServerChannelInitializer(private val sslConfiguration: SslConfiguration) : ChannelInitializer<SocketChannel>() {

        private val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())

        init {
            sslConfiguration.run {
                keyManagerFactory.init(this.keyStore, this.keyStorePassword.toCharArray())
            }
        }

        override fun initChannel(ch: SocketChannel) {
            val pipeline = ch.pipeline()
            pipeline.addLast("sslHandler", createServerSslHandler(sslConfiguration.keyStore, keyManagerFactory))
            pipeline.addLast("idleStateHandler", IdleStateHandler(0, 0, SERVER_IDLE_TIME_SECONDS))
            pipeline.addLast(HttpServerCodec())
            pipeline.addLast(HttpChannelHandler(this@HttpServer, logger))
        }
    }

    override fun onNewConfiguration(changedKeys: Set<String>, config: Map<String, Config>) {
        // TODO: create proper structure for config and reuse shared logic
        if (config.containsKey("p2p.gateway") && changedKeys.contains("p2p.gateway")) {
            val configPath = config["p2p.gateway"]!!
            val keyStore = Base64.getDecoder().decode(configPath.getString("sslConfig.keyStore"))
            val keyStorePassword = configPath.getString("sslConfig.keyStorePassword")
            val trustStore = Base64.getDecoder().decode(configPath.getString("sslConfig.trustStore"))
            val truststorePassword = configPath.getString("sslConfig.trustStorePassword")
            val revocationConfigMode = configPath.getEnum(RevocationConfigMode::class.java, "sslConfig.revocationCheck.mode")
            val revocationConfig = RevocationConfig(revocationConfigMode)
            val sslConfiguration = SslConfiguration(keyStore, keyStorePassword, trustStore, truststorePassword, revocationConfig)
            gatewayConfiguration = GatewayConfiguration(
                configPath.getString("hostAddress"),
                configPath.getInt("hostPort"),
                sslConfiguration
            )

            when(state) {
                State.Created -> startResources()
                State.Started -> restartServer()
                State.StoppedByParent -> {}
                State.StoppedDueToError -> restartServer()
            }
        }
    }

}
