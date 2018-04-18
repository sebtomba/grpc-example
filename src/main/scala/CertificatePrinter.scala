import java.security.cert.Certificate
import java.util.Base64

import scala.annotation.tailrec

object CertificatePrinter {
  def print(certificate: Certificate): String = {
    val str = Base64.getEncoder.encodeToString(certificate.getEncoded)
    split(str).mkString("\n-----BEGIN CERTIFICATE-----\n", "\n", "\n-----END CERTIFICATE-----\n")
  }

  @tailrec
  private def split(s: String, acc: List[String] = Nil): List[String] =
    if (s.length == 0) acc.reverse
    else {
      val (a, b) = s.splitAt(64)
      split(b, a :: acc)
    }

}
