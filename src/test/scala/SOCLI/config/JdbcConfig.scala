package SOCLI.config

import scala.concurrent.duration.FiniteDuration

trait JdbcConfig{


  // Lecture des propriétés via System.getProperty
  val host: String = System.getProperty("service.host", "http://esbpprd1:1608/ws/socli/")
  val secure: Boolean = System.getProperty("service.secure", "true").toBoolean
  val nbTotalUser: Int = System.getProperty("nbUser", "100").toInt
  val rampDuration: FiniteDuration = FiniteDuration.apply(System.getProperty("rampDuration", "5").toInt, System.getProperty("rampUnit", "minutes"))
  val testDuration: FiniteDuration = FiniteDuration.apply(System.getProperty("testDuration", "15").toInt, System.getProperty("testUnit", "minutes"))


  // Paramètres de la base de données JDBC
  val jdbcUrl: String = System.getProperty("data.jdbc.url", "jdbc:oracle:thin:@//exappr-scan.galerieslafayette.ggl.inet:1530/pfroadh")
  val jdbcUsername: String = System.getProperty("data.jdbc.username", "SOCLI")
  val jdbcPassword: String = System.getProperty("data.jdbc.password", "bLFJmp!rctqERM8L")
  val nbRows: String = System.getProperty("data.nbRows", "80000") //
  val personStartIndex: String = System.getProperty("personStartIndex", "0")
}
