package connection

import slick.jdbc.JdbcProfile

/**
  * Created by Spencer on 2019-06-12
  */

trait DbComponent {

  val driver: JdbcProfile

  import driver.api.Database

  val db: Database
}
