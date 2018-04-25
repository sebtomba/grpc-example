package grpctest

import java.io.File

import scala.concurrent.{ExecutionContext, Future}

import grpctest.hello._
import io.grpc._
import io.grpc.netty.{GrpcSslContexts, NettyServerBuilder}
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import io.netty.handler.ssl.{ClientAuth, SslContext}

object HelloWorldServer {

  private val port = 50051

  def buildSslContext(certChainFile: String, privateKeyFile: String): SslContext = {

    GrpcSslContexts.forServer(new File(certChainFile), new File(privateKeyFile))
      .trustManager(InsecureTrustManagerFactory.INSTANCE)
      .clientAuth(ClientAuth.OPTIONAL)
      .build()
  }

}

class HelloWorldServer(publicKey: String, executionContext: ExecutionContext, sslContext: SslContext) {
  self =>

  private val logger = Logger(this.getClass)
  private[this] var server: Server = _

  def start(): Unit = {

    server = NettyServerBuilder.forPort(HelloWorldServer.port)
      .sslContext(sslContext)
      .addService(GreeterGrpc.bindService(new GreeterImpl, executionContext))
      .intercept(new SslSessionServerInterceptor())
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

    private val logger = Logger(this.getClass)

    def handshake(request: HandshakeRequest): Future[HandshakeReply] = {
      logger.info(s"Got $request")
      val reply = HandshakeReply(publicKey)
      Future.successful(reply)
    }

    override def sayHello(request: HelloRequest): Future[HelloReply] = {
      logger.info(s"Got $request")
      val reply = HelloReply(message = "Hello " + request.name)
      Future.successful(reply)
    }
  }

}
