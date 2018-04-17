import java.util.logging.Logger

import io.grpc._
import javax.net.ssl.SSLSession

class SslSessionServerInterceptor() extends ServerInterceptor {

  private[this] val logger = Logger.getLogger("SslSessionServerInterceptor")

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
      .foreach(c => logger.info(CertificatePrinter.print(c)))
}
