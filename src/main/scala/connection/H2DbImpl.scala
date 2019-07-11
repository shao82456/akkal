//package connection
//
//import slick.jdbc.H2Profile
//
///**
//  * Created by Spencer on 2019-06-12
//  */
//
//class H2DbImpl extends DbComponent {
//  override val driver = H2Profile
//
//  val h2Url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"
//
//  override val db: driver.api.Database = Database.forURL(url = h2Url, driver = "org.h2.Driver")
//
//}
