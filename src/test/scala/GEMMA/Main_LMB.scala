package GEMMA

import scala.concurrent.duration._
import io.gatling.core.Predef._
import io.gatling.http.Predef._

import java.util.Calendar
import scala.language.postfixOps


class Main_LMB  extends Simulation {

private val  tpsAttente : Int = System.getProperty("tpsAttente", "0").toInt
//private val  nbCaissesNF : Int = System.getProperty("nbCaissesNF").toInt
private val  dureePlateau : Int =System.getProperty("dureePlateau", "1").toInt
private val  tpsMontee : Int =System.getProperty("tpsMontee", "1").toInt
private val  dureeTotale : Int = dureePlateau + tpsMontee + tpsAttente


val httpProtocol = http
 .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
 .acceptEncodingHeader("gzip, deflate")
 .acceptLanguageHeader("fr,fr-FR;q=0.8,en-US;q=0.5,en;q=0.3")
 .userAgentHeader("TESTS-DE-PERF")

val scnArchiveNF = scenario("ArchiveNF").exec(ArchiveNF.scn)

val now = Calendar.getInstance().getTime()

  setUp(
    scnArchiveNF.inject(atOnceUsers(1))
    .protocols(httpProtocol))
    .maxDuration(dureeTotale  minutes)
}
