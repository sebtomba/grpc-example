import java.io.File
import java.util.concurrent.TimeUnit
import java.util.logging.{Level, Logger}

import com.example.protos.hello.GreeterGrpc.GreeterBlockingStub
import com.example.protos.hello.{GreeterGrpc, HelloRequest}
import io.grpc._
import io.grpc.netty.{GrpcSslContexts, NegotiationType, NettyChannelBuilder}
import io.netty.handler.ssl.SslContext

object HelloWorldClient {
  def apply(host: String, port: Int, sslContext: SslContext): HelloWorldClient = {
    val channel = NettyChannelBuilder.forAddress(host, port)
      .negotiationType(NegotiationType.TLS)
      .sslContext(sslContext)
      .intercept(new HelloWorldClientInterceptor())
      .build()
    val blockingStub = GreeterGrpc.blockingStub(channel)
    new HelloWorldClient(channel, blockingStub)
  }

  def buildSslContext(
    clientCertChainFilePath: String,
    clientPrivateKeyFilePath: String,
    serverCertChainFilePath: String): SslContext = {
    val builder = GrpcSslContexts.forClient
    builder.trustManager(new File(serverCertChainFilePath))
    builder.keyManager(new File(clientCertChainFilePath), new File(clientPrivateKeyFilePath))
    builder.build
  }
}

class HelloWorldClient private(
  private val channel: ManagedChannel,
  private val blockingStub: GreeterBlockingStub
) {
  private[this] val logger = Logger.getLogger(classOf[HelloWorldClient].getName)

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

class HelloWorldClientInterceptor() extends ClientInterceptor {
  def interceptCall[ReqT, RespT](
    method: MethodDescriptor[ReqT, RespT],
    callOptions: CallOptions,
    next: Channel): ClientCall[ReqT, RespT] = {
    // This is obviously before TLS negotiation, so of not much value to us
    next.newCall(method, callOptions)
  }
}
