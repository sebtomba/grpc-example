import java.util.logging.Logger

import io.grpc._
import javax.net.ssl.SSLSession


class SslSessionClientInterceptor() extends ClientInterceptor {
  def interceptCall[ReqT, RespT](
    method: MethodDescriptor[ReqT, RespT],
    callOptions: CallOptions,
    next: Channel): ClientCall[ReqT, RespT] = {
    new SslSessionClientCallInterceptor(next.newCall(method, callOptions))
  }
}

class SslSessionClientCallInterceptor[ReqT, RespT](next: ClientCall[ReqT, RespT]) extends ClientCall[ReqT, RespT] {
  self =>

  private[this] val logger = Logger.getLogger("SslSessionClientCallInterceptor")

  def cancel(message: String, cause: Throwable): Unit = next.cancel(message, cause)
  def request(numMessages: Int): Unit = next.request(numMessages)
  def sendMessage(message: ReqT): Unit = next.sendMessage(message)
  def halfClose(): Unit = next.halfClose()

  override def isReady: Boolean = next.isReady
  override def setMessageCompression(enabled: Boolean): Unit = next.setMessageCompression(enabled)
  override def getAttributes: Attributes = next.getAttributes

  def start(responseListener: ClientCall.Listener[RespT], headers: Metadata): Unit =
    next.start(new InterceptionListener(responseListener), headers)

  private class InterceptionListener(next: ClientCall.Listener[RespT]) extends ClientCall.Listener[RespT] {
    override def onMessage(message: RespT): Unit = next.onMessage(message)
    override def onClose(status: Status, trailers: Metadata): Unit = next.onClose(status, trailers)
    override def onReady(): Unit = next.onReady()

    override def onHeaders(headers: Metadata): Unit = {
      val sslSession: Option[SSLSession] = Option(self.getAttributes.get(Grpc.TRANSPORT_ATTR_SSL_SESSION))
      if (sslSession.isEmpty)
        logger.severe("No SSL Session found in server call")

      sslSession.foreach(logCertificate)
      next.onHeaders(headers)
    }


    private def logCertificate(sslSession: SSLSession): Unit =
      sslSession
        .getPeerCertificates
        .foreach(c => logger.info(CertificatePrinter.print(c)))
  }
}
