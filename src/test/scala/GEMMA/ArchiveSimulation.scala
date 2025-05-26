package GEMMA

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.security.MessageDigest

class ArchiveSimulation extends Simulation {

  val httpProtocol = http
    .baseUrl("https://gemma-perf.galerieslafayette.store/ws")

  val user = "edi17406689565"
  val passwordMd5 = "$2y$10$fhnNan2dzUC0KstCLk0DB.pcmiL.cGm8IMdkueDubmSIl/DFk0OK2"
  val timestamp = System.currentTimeMillis() / 1000
  val body = ""

  // Debug: Print timestamp
  println(s"[DEBUG] Timestamp: $timestamp")

  val bodyMd5 = if (body.isEmpty) "" else {
    val md = MessageDigest.getInstance("MD5")
    md.digest(body.getBytes("UTF-8")).map("%02x".format(_)).mkString
  }

  val request = s"POST:/rovercash/nf/archive?body=$bodyMd5&timestamp=$timestamp&user=$user"

  val secretKey = new SecretKeySpec(passwordMd5.getBytes("UTF-8"), "HmacSHA256")
  val mac = Mac.getInstance("HmacSHA256")
  mac.init(secretKey)
  val requestHmac = mac.doFinal(request.getBytes("UTF-8"))
  val signature = Base64.getEncoder.encodeToString(requestHmac)

  // Debug: Print signature
  println(s"[DEBUG] Signature: $signature")

  val endpointWs = s"/rovercash/nf/archive?body=&timestamp=$timestamp&user=$user&signature=$signature"

  val scn = scenario("Archive Scenario")
    .exec(
      http("Post Archive")
       .post(s"$endpointWs -H 'Content-Type: multipart/form-data;' -F id_terminal=10001 -F filedata=src/test/resources/Archirve/ARCHIVE_001_20250505000000.zip")

      /*        .post(endpointWs)
              .header("Content-Type", "multipart/form-data")
              .formParam("id_terminal", "10001")
              .formUpload("filedata", "src/test/resources/Archirve/ARCHIVE_001_20250505000000.zip")*/
    )

  setUp(scn.inject(atOnceUsers(1))).protocols(httpProtocol)
}

