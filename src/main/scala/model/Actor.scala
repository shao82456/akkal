package model
import java.util.Date

case class Actor(
                 actorId: Int,
                 firstName: String,
                 lastName: String,
                 lastUpdate: String
               )


import slick.jdbc.MySQLProfile.api._

class ActorTable(tag: Tag) extends Table[(Int, String, String, String)](tag, "actor") {

  def actorId        = column[Int]("actor_id", O.PrimaryKey, O.AutoInc)
  def firstName    = column[String]("first_name", O.SqlType("VARCHAR(45)"))
  def lastName    = column[String]("last_name",O.SqlType("VARCHAR(45)"))
  def lastUpdate = column[String]("last_update", O.SqlType("TIMESTAMP"))

  def * = (actorId, firstName, lastName, lastUpdate)
}
