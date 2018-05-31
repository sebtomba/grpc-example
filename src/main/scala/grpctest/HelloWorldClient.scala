package grpctest

import java.io.File
import java.util.concurrent.TimeUnit

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success

import grpctest.hello._
import grpctest.hello.GreeterGrpc.GreeterStub
import io.grpc._
import io.grpc.netty._
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.util.InsecureTrustManagerFactory

object HelloWorldClient {
  def apply(host: String, port: Int, sslContext: SslContext): HelloWorldClient = {
    val channel = NettyChannelBuilder.forAddress(host, port)
      .negotiationType(NegotiationType.TLS)
      .sslContext(sslContext)
      .intercept(new SslSessionClientInterceptor())
      .build()
    val stub = GreeterGrpc.stub(channel)
    new HelloWorldClient(channel, stub)
  }

  def buildSslContext(clientCertChainFilePath: String, clientPrivateKeyFilePath: String): SslContext = {
    val builder = GrpcSslContexts.forClient
    builder.trustManager(InsecureTrustManagerFactory.INSTANCE)
    builder.keyManager(new File(clientCertChainFilePath), new File(clientPrivateKeyFilePath))
    builder.build
  }
}

class HelloWorldClient private(channel: ManagedChannel, stub: GreeterStub) {

  private val logger = Logger(this.getClass)

  def shutdown(): Unit = {
    channel.shutdown.awaitTermination(5, TimeUnit.SECONDS)
  }

  def handshake(key: String): Future[String] = {
    val request = HandshakeRequest(key)
    stub.handshake(request)
      .andThen {
        case Success(reply) => logger.info(s"Got $reply")
      }.map(_.key)
  }

  /** Say hello to server. */
  def greet(name: String): Future[String] = {
    val request = HelloRequest(name = name)
    stub.sayHello(request).map(_.message)
  }
}
