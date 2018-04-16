import scala.concurrent.ExecutionContext

object TestGrpc {

  val clientCertChainFilePath = "client.certificate.pem"
  val clientPrivateKeyFilePath = "client.key.pem"
  val serverCertChainFilePath = "server.certificate.pem"
  val serverPrivateKeyFilePath = "server.key.pem"

  def main(args: Array[String]): Unit = {
    val server = new HelloWorldServer(
      ExecutionContext.global,
      HelloWorldServer.buildSslContext(
        serverCertChainFilePath,
        serverPrivateKeyFilePath,
        clientCertChainFilePath
      )
    )
    server.start()

    Thread.sleep(3000)

    val client = HelloWorldClient(
      "localhost",
      50051,
      HelloWorldClient.buildSslContext(
        clientCertChainFilePath,
        clientPrivateKeyFilePath,
        serverCertChainFilePath
      )
    )
    try {
      client.greet("world")
    } finally {
      client.shutdown()
    }

    server.stop()
  }

}
