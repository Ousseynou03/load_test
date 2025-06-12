package SOCLI.objects

import SOCLI.config.JdbcConfig
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef.jdbcFeeder

object ObjectSearchCustomer extends JdbcConfig {

  val scnSearchCustomer = scenario("SearchCustomer")
    .feed(jdbcFeeder(
      jdbcUrl,
      jdbcUsername,
      jdbcPassword,
      "SELECT NOM Nom, ID id, ID_WITH_POSTALE, ID_WITH_OPTIN, ID_WITH_MAIL FROM(" +
        "SELECT DISTINCT(p.NOM) NOM, p.ID, p.ID_WITH_POSTALE, p.ID_WITH_OPTIN, p.ID_WITH_MAIL " +
        "FROM PERSONNE p, PERSONNE_ENSEIGNE pe " +
        "WHERE p.NOM IS NOT NULL " +
        "AND NOT REGEXP_LIKE(p.nom, '.*[[:digit:]]+.*') " +
        "AND p.id_personne = pe.id_personne AND p.id_personne > " + personStartIndex + ")" +
        "WHERE ROWNUM <= " + nbRows  // Tu peux ajuster ce nombre dans config/JdbcCustomer
    ).random)
    .exec { session =>
      // Vérifie les données de la session ici
      println("Nom: " + session("Nom").as[String])
      println("ID: " + session("Id").as[String])
      println("ID_WITH_POSTALE: " + session("ID_WITH_POSTALE").as[String])
      println("ID_WITH_OPTIN: " + session("ID_WITH_OPTIN").as[String])
      println("ID_WITH_MAIL: " + session("ID_WITH_MAIL").as[String])
      session
    }
    .exec(
      http("Search Customer")
        .put("/v2/customer")
        .header("Content-Type", "application/json")
        .body(StringBody(session =>
          s"""{
             |  "nom": "${session("Nom").as[String]}",
             |  "id": "${session("Id").as[String]}",
             |  "idPostale": "${session("ID_WITH_POSTALE").as[String]}",
             |  "idOptin": "${session("ID_WITH_OPTIN").as[String]}",
             |  "idMail": "${session("ID_WITH_MAIL").as[String]}"
             |}""".stripMargin
        )).asJson
        .check(status.is(200))
    )




}
