import java.io.File
import java.util.logging.Logger

import scala.concurrent.{ExecutionContext, Future}

import com.example.protos.hello.{GreeterGrpc, HelloReply, HelloRequest}
import io.grpc._
import io.grpc.netty.{GrpcSslContexts, NettyServerBuilder}
import io.netty.handler.ssl.{ClientAuth, SslContext}
import javax.net.ssl.SSLSession

object HelloWorldServer {

  private val port = 50051

  def buildSslContext(certChainFile: String, privateKeyFile: String, clientCertChainFile: String): SslContext = {
    GrpcSslContexts.forServer(new File(certChainFile), new File(privateKeyFile))
      .trustManager(new File(clientCertChainFile))
      .clientAuth(ClientAuth.OPTIONAL)
      .build()
  }

}

class HelloWorldServer(executionContext: ExecutionContext, sslContext: SslContext) {
  self =>

  private[this] val logger = Logger.getLogger(classOf[HelloWorldServer].getName)
  private[this] var server: Server = _

  def start(): Unit = {

    server = NettyServerBuilder.forPort(HelloWorldServer.port)
      .sslContext(sslContext)
      .addService(ServerInterceptors.intercept(
        GreeterGrpc.bindService(new GreeterImpl, executionContext),
        new HelloWorldServerInterceptor()))
      .build
      .start

    logger.info("Server started, listening on " + HelloWorldServer.port)
    sys.addShutdownHook {
      System.err.println("*** shutting down gRPC server since JVM is shutting down")
      self.stop()
      System.err.println("*** server shut down")
    }
  }

  def stop(): Unit = {
    if (server != null) {
      server.shutdown()
    }
  }

  def blockUntilShutdown(): Unit = {
    if (server != null) {
      server.awaitTermination()
    }
  }

  private class GreeterImpl extends GreeterGrpc.Greeter {

    private[this] val logger = Logger.getLogger(classOf[GreeterImpl].getName)

    override def sayHello(req: HelloRequest): Future[HelloReply] = {
      logger.info(s"Got $req")
      val reply = HelloReply(message = "Hello " + req.name)
      Future.successful(reply)
    }
  }

}

class HelloWorldServerInterceptor() extends ServerInterceptor {

  private[this] val logger = Logger.getLogger(classOf[HelloWorldServerInterceptor].getName)

  def interceptCall[ReqT, RespT](
    call: ServerCall[ReqT, RespT],
    headers: Metadata,
    next: ServerCallHandler[ReqT, RespT]): ServerCall.Listener[ReqT] = {

    val sslSession: Option[SSLSession] = Option(call.getAttributes.get(Grpc.TRANSPORT_ATTR_SSL_SESSION))
    if (sslSession.isEmpty)
      logger.severe("No SSL Session found in server call")

    sslSession.foreach(logPublicKey)
    next.startCall(call, headers)
  }

  private def logPublicKey(sslSession: SSLSession): Unit =
    sslSession
      .getPeerCertificates
      .foreach(c => logger.info(c.getPublicKey.getEncoded.map("%02x".format(_)).mkString(":")))
}
