package GEMMA

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.util.Base64

class ArchiveSimulation extends Simulation {

  val httpProtocol = http
    .baseUrl("https://gemma-perf.galerieslafayette.store")
    .disableWarmUp

  // Paramètres fixes
  val user = "edi17406689565"
  val passwordMd5 = "$2y$10$fhnNan2dzUC0KstCLk0DB.pcmiL.cGm8IMdkueDubmSIl/DFk0OK2"
  val body = ""

  // Compteur pour les numéros de caisse
  val numCaisseIterator = Iterator.from(1)

  val scn = scenario("Archive Scenario")
    .exec(session => {
      // Récupère le numéro de caisse et le formate sur 3 chiffres
      val currentNumber = numCaisseIterator.next()
      val numCaisse = f"$currentNumber%03d" // Format "001", "002", etc.

      // Met à jour la session
      session.set("numCaisse", numCaisse)
    })
    .exec(session => {
      // Récupère le numéro de caisse depuis la session
      val numCaisse = session("numCaisse").as[String]
      val timestamp = System.currentTimeMillis / 1000

      // Calcul du MD5 du body
      val bodyMd5 = if (body.isEmpty) "" else {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(body.getBytes(StandardCharsets.UTF_8))
        digest.map("%02x".format(_)).mkString
      }

      // Construction de la chaîne à signer
      val requestString = s"POST:/rovercash/nf/archive?body=$bodyMd5&timestamp=$timestamp&user=$user"

      // Calcul du HMAC-SHA256
      val mac = Mac.getInstance("HmacSHA256")
      val secretKey = new SecretKeySpec(passwordMd5.getBytes(StandardCharsets.UTF_8), "HmacSHA256")
      mac.init(secretKey)
      val hmacBytes = mac.doFinal(requestString.getBytes(StandardCharsets.UTF_8))

      // Conversion en hexadécimal
      val hmacHex = hmacBytes.map("%02x".format(_)).mkString

      // Encodage Base64 sans padding
      val signature = Base64.getEncoder.withoutPadding().encodeToString(hmacHex.getBytes(StandardCharsets.UTF_8))

      // Construction de l'URL finale
      val endpointWs = s"/ws/rovercash/nf/archive?body=&timestamp=$timestamp&user=$user&signature=$signature"

      // Chemin du fichier dynamique utilisant numCaisse
     // val filePath = "ARCHIVE_${numCaisse}_20250505000000.zip"
      val filePath = s"./Archirve/ARCHIVE_${numCaisse}_20250505000000.zip"

      // Debug logging
      println(s"[DEBUG] Numéro de Caisse: $numCaisse")
      println(s"[DEBUG] Chemin du Fichier: $filePath")
      println(s"[DEBUG] Chaîne de Requête: $requestString")
      println(s"[DEBUG] Signature: $signature")
      println(s"[DEBUG] URL Finale: $endpointWs")

      // Mise à jour de la session
      session
        .set("timestamp", timestamp.toString)
        .set("endpointWs", endpointWs)
        .set("filePath", filePath)
    })
    .exec(
      http("Post Archive")
        .post("${endpointWs}")
        .header("Content-Type", "multipart/form-data")
        .formParam("id_terminal", "10001")
        .formUpload("filedata", "${filePath}")
    )

  setUp(
    scn.inject(atOnceUsers(1))
  ).protocols(httpProtocol)
}