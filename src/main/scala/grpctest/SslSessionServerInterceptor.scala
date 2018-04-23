package grpctest

import java.util.Base64

import grpctest.hello.HandshakeRequest
import io.grpc._
import javax.net.ssl.SSLSession

class SslSessionServerInterceptor() extends ServerInterceptor {

  private val logger = Logger(this.getClass)

  def interceptCall[ReqT, RespT](
    call: ServerCall[ReqT, RespT],
    headers: Metadata,
    next: ServerCallHandler[ReqT, RespT]
  ): ServerCall.Listener[ReqT] = new InterceptionListener(next.startCall(call, headers), call)

  private class InterceptionListener[ReqT, RespT](next: ServerCall.Listener[ReqT], call: ServerCall[ReqT, RespT]) extends ServerCall.Listener[ReqT] {
    override def onHalfClose(): Unit = next.onHalfClose()
    override def onCancel(): Unit = next.onCancel()
    override def onComplete(): Unit = next.onComplete()
    override def onReady(): Unit = next.onReady()

    override def onMessage(message: ReqT): Unit = {
      message match {
        case handshake: HandshakeRequest =>
          val sslSession: Option[SSLSession] = Option(call.getAttributes.get(Grpc.TRANSPORT_ATTR_SSL_SESSION))
          if (sslSession.isEmpty) {
            logger.error("No SSL Session found in server call")
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
