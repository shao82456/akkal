package connection

import slick.jdbc.MySQLProfile

/**
  * Created by Spencer on 2019-06-11
  */

trait MySqlDbImpl extends DbComponent {

  override val driver = MySQLProfile

  override val db: driver.api.Database = MySqlDb.db
}

object MySqlDb {
  import slick.jdbc.MySQLProfile.api._
  val db: Database = Database.forConfig("database.mysql-test")
}
