package dao

import com.mysql.jdbc.exceptions.jdbc4.MySQLSyntaxErrorException
import connection.{DbComponent, MySqlDbImpl}
import model.{Actor,ActorTable}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import spray.json._
//import routes._
/**
  * Created by Spencer on 2019-06-12
  */
trait ActorRepository { this: DbComponent =>


  import driver.api._

  protected val actorTable = TableQuery[ActorTable]

  def createTableIfNotExists(): Future[Unit] = db.run {
    actorTable.schema.create
  }.recover {
    case _: MySQLSyntaxErrorException => ()  // if table already exist, simply ignore
  }

  def create(actor: Actor): Future[Int] =  {
    val id =
      (actorTable returning actorTable.map(_.actorId)) +=
        (actor.actorId, actor.firstName, actor.lastName, actor.lastUpdate)
    db.run(id)
  }

//  def updateactorStatusById(id: Int, status: actorStatus.actorStatus): Future[Int] = db.run {
//    actorTable.filter(_.id === id).map(_.status).update(status.toString)
//  }
//
//  def updateactorEndTimeById(id: Int, endTime: String): Future[Int] = db.run {
//    actorTable.filter(_.id === id).map(_.endTime).update(endTime)
//  }
//
//  def updateactorProgressById(id: Int, progress: Double): Future[Int] = db.run {
//    actorTable.filter(_.id === id).map(_.progress).update(progress)
//  }
//
//  def updateactorLinkById(id: Int, actorLink: String): Future[Int] = db.run {
//    actorTable.filter(_.id === id).map(_.actorLink).update(actorLink)
//  }

  def getactorById(id: Int): Future[Actor] = db.run {
    actorTable.filter(_.actorId === id).take(1).result.head.map(transfer)
  }


  private def transfer(tuple: (Int, String, String, String)): Actor = {
//    val status = actorStatusFormat.read(tuple._2.toJson)
    Actor(tuple._1, tuple._2, tuple._3, tuple._4)
  }
}

class ActorRepositoryImpl extends ActorRepository with MySqlDbImpl
