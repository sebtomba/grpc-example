package grpctest

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}
import scala.util.{Failure, Success}

import io.grpc.StatusRuntimeException

object TestGrpc {

  private val logger = Logger(this.getClass)

  val clientCertChainFilePath = "client.certificate.pem"
  val clientPrivateKeyFilePath = "client.key.pem"
  val serverCertChainFilePath = "server.certificate.pem"
  val serverPrivateKeyFilePath = "server.key.pem"

  def main(args: Array[String]): Unit = {

    val publicKeyClient = CertificateHelper.publicKeyFrom(clientCertChainFilePath)
    val publicKeyServer = CertificateHelper.publicKeyFrom(serverCertChainFilePath)

    val server = new HelloWorldServer(
      publicKeyServer,
      ExecutionContext.global,
      HelloWorldServer.buildSslContext(serverCertChainFilePath, serverPrivateKeyFilePath)
    )
    server.start()

    Thread.sleep(1000)

    val client = HelloWorldClient(
      "localhost",
      50051,
      HelloWorldClient.buildSslContext(clientCertChainFilePath, clientPrivateKeyFilePath)
    )

    val result =
      (for {
        _ <- client.handshake(publicKeyClient)
        message <- client.greet("world")
      } yield message)
        .andThen {
          case Success(message) => logger.info("Greeting: " + message)
          case Failure(e: StatusRuntimeException) => logger.error(s"RPC failed: ${e.getStatus.getDescription}", e.getCause)
        }
        .recover { case _ => "" }

    try {
      Await.ready(result, Duration(5, SECONDS))
    } finally {
      client.shutdown()
      server.stop()
      server.blockUntilShutdown()
    }

  }

}
