package grpctest

import java.io.{File, FileInputStream}
import java.security.cert.{CertificateFactory, X509Certificate}
import java.util.Base64

object CertificateHelper {

  def publicKeyFrom(certFilePath: String): String =
    publicKey(fromFile(new File(certFilePath)))

  def fromFile(certFile: File): X509Certificate = {
    val cf = CertificateFactory.getInstance("X.509", "BC")
    val is = new FileInputStream(certFile)
    cf.generateCertificate(is).asInstanceOf[X509Certificate]
  }

  def publicKey(certificate: X509Certificate): String =
    Base64.getEncoder.encodeToString(certificate.getPublicKey.getEncoded)
}
