package GEMMA

import scala.concurrent.duration._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._

import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.security.MessageDigest

import scala.language.postfixOps
import sys.process._


object ArchiveNF {

  ///////////////////////////
  // Signature Helper Method
  def computeSignatureFull(user: String, passwordmd5: String, timestamp: String, body: String): (String, String) = {
    val bodymd5 = if (body.isEmpty) "" else {
      val md = MessageDigest.getInstance("MD5")
      md.digest(body.getBytes("UTF-8")).map("%02x".format(_)).mkString
    }

    val webservice = "rovercash/nf/archive"
    val request = s"POST:/$webservice?body=$bodymd5&timestamp=$timestamp&user=$user"

    val hmacSha256 = Mac.getInstance("HmacSHA256")
    val secretKey = new SecretKeySpec(passwordmd5.getBytes("UTF-8"), "HmacSHA256")
    hmacSha256.init(secretKey)

    val hmac = hmacSha256.doFinal(request.getBytes("UTF-8"))
    val signature = Base64.getEncoder.encodeToString(hmac)

    val retour = s"body=$bodymd5&timestamp=$timestamp&user=$user&signature=$signature"
    (signature, retour)
  }

  ///////////////////////////
  // Configuration Variables
  private val tempoDownload: Int = System.getProperty("tempoDownload", "500").toInt
  private val pasPacing: Int = 0




  private val FichierPath: String = System.getProperty("dataDir", "src/test/resources/GEMMA/")
  private val FichierUrl: String = "JDD_ARCHIVNF.csv"

  val urlFeeder = csv(FichierPath + FichierUrl).circular


  val theScript = ("ArchiveNF.sh")

  val scn = scenario("Gemma")
    .exec {session => session.set("pacing", "120")}
    .feed(urlFeeder)
    .exec { session =>
      println("DEBUG - Feeder values:")
      println("numCaisse: " + session("numCaisse").as[String])
      println("user: " + session("user").as[String])
      println("password: " + session("password").as[String])
      println("id_terminal: " + session("id_terminal").as[String])
      session
    }

    .forever {
      exec(flushSessionCookies)
        .exec(flushHttpCache)
        .exec(flushCookieJar)
        .pace(session => session("pacing").validate[String].map(i => i.toInt seconds))
        .exec {session => session.set("body", "" )}
        .group("LMB") {
          exec(flushSessionCookies)

            .exec { session =>
              val user = session("user").as[String]
              val passwordmd5 = session("password").as[String]
              val timestamp = System.currentTimeMillis().toString
              val body = ""  // ou le contenu que tu veux envoyer en body

              val bodymd5 = if (body.isEmpty) "" else {
                val md = java.security.MessageDigest.getInstance("MD5")
                md.digest(body.getBytes("UTF-8")).map("%02x".format(_)).mkString
              }

              val (signatureOnly, fullRetour) = computeSignatureFull(user, passwordmd5, timestamp, body)

              println(s"[DEBUG] Timestamp: $timestamp")
              println(s"[DEBUG] BodyMD5: $bodymd5")
              println(s"[DEBUG] Signature (for URL): $signatureOnly")
              println(s"[DEBUG] Retour complet (shell style): $fullRetour")

              session
                .set("timestamp", timestamp)
                .set("signature", signatureOnly)
                .set("bodymd5", bodymd5)
            }



            .exec(http("Message")
             // .post("ws/rovercash/nf/archive?body=${bodymd5}&timestamp=${timestamp}&user=${user}&signature=${signature} -H 'Content-Type: multipart/form-data;' -F id_terminal=${id_terminal} -F filedata=@src/test/resources/Archirve/ARCHIVE_001_20250505000000.zip")
             .post("ws/rovercash/nf/archive?body=${bodymd5}&timestamp=${timestamp}&user=${user}&signature=${signature}")
              .header("Content-Type", "multipart/form-data")
              .formParam("id_terminal", "${id_terminal}")
              .bodyPart(RawFileBodyPart("filedata", "src/test/resources/Archirve/ARCHIVE_001_20250505000000.zip"))
              .asMultipartForm
              .requestTimeout(60 seconds)
              .check(status.in(200,201))
              .check(jsonPath("$.messages[*].contenu.filename").findAll.optional.saveAs("directsURLS"))
            )
        }

/*        .doIfOrElse( session => session.attributes.contains("directsURLS"))
        {
          foreach("${directsURLS}", "Url") {
            exec(http("Download")
              .get("${Url}")
              .check(status.in(200,201))
            )
            pause (tempoDownload milliseconds)
          }
          exec(session => session.remove("directsURLS"))
            .exec {session => session.set("pacing", "120")}
        }
        {
          exec { session => session.set("pacing", ((session("pacing").as[String].toInt + pasPacing ).toString))}
            .doIfEquals(session => session("pacing").as[String], "210")
            {
              exec {session => session.set("pacing", "120")}
            }
        }

      {
        exec { session => session.set("pacing", ((session("pacing").as[String].toInt + pasPacing ).toString))}

      }*/

    }
}
