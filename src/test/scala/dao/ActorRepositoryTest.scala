package dao

import model.Actor
import org.scalatest.FunSuite

import scala.concurrent.Await

class ActorRepositoryTest extends FunSuite {
  import scala.concurrent.duration._

  test("testGetactorById") {
    val taskRepo = new ActorRepositoryImpl()
    val actorFuture=taskRepo.getactorById(300)
    val actor = Await.result(actorFuture.mapTo[Actor], 1 second)
    println(actor)
  }

}
