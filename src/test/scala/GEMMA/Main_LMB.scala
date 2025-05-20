package GEMMA

import scala.concurrent.duration._
import io.gatling.core.Predef._
import io.gatling.http.Predef._

import java.util.Calendar
import scala.language.postfixOps


class Main_LMB  extends Simulation {


  private val urlGemma: String = System.getProperty("host", "https://gemma-perf.galerieslafayette.store/")
  private val  tpsAttente : Int = System.getProperty("tpsAttente", "0").toInt
  private val  dureePlateau : Int =System.getProperty("dureePlateau", "1").toInt
  private val  tpsMontee : Int =System.getProperty("tpsMontee", "1").toInt
  private val  dureeTotale : Int = dureePlateau + tpsMontee + tpsAttente


  val dateDebut: Long = System.currentTimeMillis()

val httpProtocol = http
  .baseUrl(urlGemma)
 .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
 .acceptEncodingHeader("gzip, deflate")
 .acceptLanguageHeader("fr,fr-FR;q=0.8,en-US;q=0.5,en;q=0.3")
 .userAgentHeader("TESTS-DE-PERF")


  before {
    println(s"Début du test à ${new java.util.Date(dateDebut)}")
  }

val scnArchiveNF = scenario("ArchiveNF").exec(ArchiveNF.scn)

val now = Calendar.getInstance().getTime()

  setUp(
    scnArchiveNF.inject(atOnceUsers(1))
    .protocols(httpProtocol))
    .maxDuration(dureeTotale  minutes)


  after {
    val dateFin: Long = System.currentTimeMillis()
    println(s"Fin du test à ${new java.util.Date(dateFin)}")
  }
}
