package SOCLI.simulation

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._
import SOCLI.objects.ObjectSearchCustomer

import scala.concurrent.duration._
import scala.language.postfixOps


class ClientSimulation extends Simulation {


  // Lecture des propriétés via System.getProperty
  private val host: String = System.getProperty("service.host", "http://esbpprd1:1608/ws/socli")
  private val secure: Boolean = System.getProperty("service.secure", "true").toBoolean
  private val nbTotalUser: Int = System.getProperty("nbUser", "100").toInt
  private val rampDuration: FiniteDuration = FiniteDuration.apply(System.getProperty("rampDuration", "5").toInt, System.getProperty("rampUnit", "minutes"))
  private val testDuration: FiniteDuration = FiniteDuration.apply(System.getProperty("testDuration", "15").toInt, System.getProperty("testUnit", "minutes"))

  var httpConf = http
    .baseUrl(host)
    .inferHtmlResources(
      BlackList(
        """.*\.js""",
        """.*\.css""",
        """.*\.gif""",
        """.*\.jpeg""",
        """.*\.jpg""",
        """.*\.ico""",
        """.*\.woff""",
        """.*\.(t|o)tf""",
        """.*\.png"""
      )
    )
    .header("Accept-Version", "2")

  if(secure){
    val login: String = System.getProperty("service.login", "zebaz")
    val password: String = System.getProperty("service.password", "076a0d9a46")
    httpConf = httpConf.basicAuth(login, password)
  }

  val scnSearchCustomer = scenario("SearchCustomer").exec(ObjectSearchCustomer.scnSearchCustomer)

  setUp(
    scnSearchCustomer.inject(atOnceUsers(1)
    )
  ).protocols(httpConf)


}

