package grpctest

import java.util.Base64

import grpctest.hello.HandshakeReply
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

  private val logger = Logger(this.getClass)

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
    override def onClose(status: Status, trailers: Metadata): Unit = next.onClose(status, trailers)
    override def onReady(): Unit = next.onReady()
    override def onHeaders(headers: Metadata): Unit = next.onHeaders(headers)

    override def onMessage(message: RespT): Unit = {
      message match {
        case handshake: HandshakeReply =>
          val sslSession: Option[SSLSession] = Option(self.getAttributes.get(Grpc.TRANSPORT_ATTR_SSL_SESSION))
          if (sslSession.isEmpty) {
            logger.error("No SSL Session found in client call")
            close()
          } else {
            sslSession.foreach { session =>
              val pubKey = Base64.getEncoder.encodeToString(session.getPeerCertificates.head.getPublicKey.getEncoded)
              if (pubKey == handshake.key) {
                next.onMessage(message)
              } else {
                logger.error("Wrong public key")
                close()
              }
            }
          }

        case _ => next.onMessage(message)
      }
    }

    private def close(): Unit =
      throw Status.UNAUTHENTICATED.withDescription("Wrong public key").asRuntimeException()

  }
}
