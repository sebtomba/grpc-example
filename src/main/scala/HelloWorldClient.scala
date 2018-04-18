import java.io.File
import java.util.concurrent.TimeUnit
import java.util.logging.{Level, Logger}

import com.example.protos.hello.GreeterGrpc.GreeterBlockingStub
import com.example.protos.hello.{GreeterGrpc, HelloRequest}
import io.grpc._
import io.grpc.netty.{GrpcSslContexts, NegotiationType, NettyChannelBuilder}
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.util.InsecureTrustManagerFactory

object HelloWorldClient {
  def apply(host: String, port: Int, sslContext: SslContext): HelloWorldClient = {
    val channel = NettyChannelBuilder.forAddress(host, port)
      .negotiationType(NegotiationType.TLS)
      .sslContext(sslContext)
      .intercept(new SslSessionClientInterceptor())
      .build()
    val blockingStub = GreeterGrpc.blockingStub(channel)
    new HelloWorldClient(channel, blockingStub)
  }

  def buildSslContext(clientCertChainFilePath: String, clientPrivateKeyFilePath: String): SslContext = {
    val builder = GrpcSslContexts.forClient
    builder.trustManager(InsecureTrustManagerFactory.INSTANCE)
    builder.keyManager(new File(clientCertChainFilePath), new File(clientPrivateKeyFilePath))
    builder.build
  }
}

class HelloWorldClient private(
  private val channel: ManagedChannel,
  private val blockingStub: GreeterBlockingStub
) {
  private[this] val logger = Logger.getLogger(this.getClass.getName)

  def shutdown(): Unit = {
    channel.shutdown.awaitTermination(5, TimeUnit.SECONDS)
  }

  /** Say hello to server. */
  def greet(name: String): Unit = {
    logger.info("Will try to greet " + name + " ...")
    val request = HelloRequest(name = name)
    try {
      val response = blockingStub.sayHello(request)
      logger.info("Greeting: " + response.message)
    }
    catch {
      case e: StatusRuntimeException =>
        logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus)
    }
  }
}
