package GEMMA

import scala.concurrent.duration._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._

import scala.language.postfixOps
import sys.process._


object ArchiveNF {
  private val tempoDownload : Int = System.getProperty("tempoDownload", "500").toInt
  private val pasPacing : Int = 0
 // private val urlGemma : String = "https://localhost:8080/"
  private val urlGemma: String = System.getProperty("host", "https://localhost:8080/")


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

            .exec {  session =>
              val theShell = (theScript + session("user").as[String] + " " + session("password").as[String])
              val shellReturn = (theShell).!!

              session.set("signature", shellReturn)
            }
            .exec(http("Message")
              .post(urlGemma+"ws/rovercash/nf/archive?body=&timestamp=${timestamp}&user=${user}&signature=${signature}' -H 'Content-Type: multipart/form-data;' -F id_terminal=${id_terminal} -F filedata=@/apps/lmb/tmp/PACK_NF/ARCHIVE_${numCaisse}_20250505000000.zip")
              .requestTimeout(60 seconds)
              .check(status.in(200,201))
              .check(jsonPath("$.messages[*].contenu.filename").findAll.optional.saveAs("directsURLS"))
            )
        }

        .doIfOrElse( session => session.attributes.contains("directsURLS"))
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

      }

    }
}
