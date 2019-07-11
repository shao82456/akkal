import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.Done
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import dao.ActorRepositoryImpl
import model.Actor
import spray.json.DefaultJsonProtocol._

import scala.concurrent.Future

object WebServer2 {

  // domain model
  final case class Item(name: String, id: Long)
  final case class Order(items: List[Item])

  // formats for unmarshalling and marshalling
  implicit val itemFormat = jsonFormat2(Item)
  implicit val orderFormat = jsonFormat1(Order)
  implicit val actorFormat = jsonFormat4(Actor)

  // (fake) async database query api
  def fetchItem(itemId: Long): Future[Option[Item]] = ???
  def saveOrder(order: Order): Future[Done] = ???

  def main(args: Array[String]) {

    // needed to run the route
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()

    val actorDAO=new ActorRepositoryImpl()
    val route: Route =
      pathPrefix("actor") {
        get {
          pathPrefix("get" / IntNumber) { id =>
            // there might be no item for a given id
            //          val maybeItem: Future[Option[Item]] = fetchItem(id)
            //
            //          onSuccess(maybeItem) {
            //            case Some(item) => complete(item)
            //            case None       => complete(StatusCodes.NotFound)
            //          }
            val actorFuture = actorDAO.getactorById(id)
            onSuccess(actorFuture) {
              complete(_)
            }
          }
        } ~
          post {
            path("add") {
              entity(as[Order]) { order =>
                val saved: Future[Done] = saveOrder(order)
                onComplete(saved) { done =>
                  complete("actor created")
                }
              }
            }
          }
      }
    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)
    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")

  }
}