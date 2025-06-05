package GEMMA

import io.gatling.core.Predef._
import io.gatling.http.Predef._

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.util.Base64
import scala.concurrent.duration.DurationInt

class ArchiveSimulation extends Simulation {

  val httpProtocol = http
    .baseUrl("https://gemma-perf.galerieslafayette.store")
    .disableWarmUp

  private val FichierPath: String = System.getProperty("dataDir", "./src/test/resources/GEMMA/")
  private val FichierUrl: String = "JDD_CAISSES.csv"

  // CSV feeder avec données utilisateurs
  val jdd_caisse = csv(FichierPath + FichierUrl).circular

  val body = ""

  val scn = scenario("Archive Scenario")
    .feed(jdd_caisse)
    .exec(session => {
      val timestamp = System.currentTimeMillis / 1000

      val user = session("identifiant").as[String]
      val passwordMd5 = session("password").as[String]
      val idTerminal = session("id_terminal_ext").as[String]
      val numCaisse = session("numCaisse").as[String]

      // Calcul du MD5 du body si non vide
      val bodyMd5 = if (body.isEmpty) "" else {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(body.getBytes(StandardCharsets.UTF_8))
        digest.map("%02x".format(_)).mkString
      }

      // Signature HMAC-SHA256
      val requestString = s"POST:/rovercash/nf/archive?body=$bodyMd5&timestamp=$timestamp&user=$user"
      val mac = Mac.getInstance("HmacSHA256")
      val secretKey = new SecretKeySpec(passwordMd5.getBytes(StandardCharsets.UTF_8), "HmacSHA256")
      mac.init(secretKey)
      val hmacBytes = mac.doFinal(requestString.getBytes(StandardCharsets.UTF_8))
      val hmacHex = hmacBytes.map("%02x".format(_)).mkString
      val signature = Base64.getEncoder.withoutPadding().encodeToString(hmacHex.getBytes(StandardCharsets.UTF_8))

      // Construction de l'endpoint signé
      val endpointWs = s"/ws/rovercash/nf/archive?body=&timestamp=$timestamp&user=$user&signature=$signature"


      val caisseIndex = session.userId % 10 match {
        case 0 => 10
        case n => n
      }
      val fileNumber = f"$caisseIndex%03d"
      val filePath = s"src/test/resources/Archirve/ARCHIVE_${fileNumber}_20250505000000.zip"

      // Logs de debug
      println(s"[DEBUG] user: $user")
      println(s"[DEBUG] endpointWs: $endpointWs")
      println(s"[DEBUG] filePath: $filePath")

      session
        .set("timestamp", timestamp.toString)
        .set("endpointWs", endpointWs)
        .set("filePath", filePath)
        .set("idTerminal", idTerminal)
        .set("callCount", 0) // Initialisation du compteur
    })
    .exec { session =>
      val currentCount = session("callCount").asOption[Int].getOrElse(0)
      session.set("callCount", currentCount + 1)
    }

    .exec(
      http("Post Archive")
        .post("${endpointWs}")
        .header("Content-Type", "multipart/form-data")
        .formParam("id_terminal", "${idTerminal}")
        .formUpload("filedata", "${filePath}")
    )
    .exec { session =>
      println(s"[DEBUG] Requête #${session("callCount").as[Int]} effectuée pour l'utilisateur ${session("identifiant").as[String]}")
      session
    }

  setUp(
    scn.inject(
        atOnceUsers(1)
      )
  )
    .protocols(httpProtocol)
}
