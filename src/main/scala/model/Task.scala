package model

/**
  * Created by Spencer on 2019-06-12
  */

object TaskStatus extends Enumeration {
  type TaskStatus = Value
  val SUBMIT  = Value("submit")
  val PENDING = Value("pending")
  val RUNNING = Value("running")
  val STOPPED = Value("stopped")
  val FAILED  = Value("failed")
  val SUCCESS = Value("success")
  val KILLED  = Value("killed")
  val DISCARDED = Value("discarded")
}

case class Task(
    id: Int,
    status: TaskStatus.TaskStatus = TaskStatus.SUBMIT,
    confId: Int,
    startTime: String,
    endTime: String,
    progress: Double,
    taskLink: String
)

import slick.jdbc.MySQLProfile.api._

class TaskTable(tag: Tag) extends Table[(Int, String, Int, String, String, Double, String)](tag, "task") {

  def id        = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def status    = column[String]("status", O.SqlType("VARCHAR(200)"))
  def confId    = column[Int]("conf_id")
  def startTime = column[String]("start_time", O.SqlType("VARCHAR(50)"))
  def endTime   = column[String]("end_time", O.SqlType("VARCHAR(50)"))
  def progress  = column[Double]("progress")
  def taskLink  = column[String]("taskLink", O.SqlType("VARCHAR(200)"))

  def * = (id, status, confId, startTime, endTime, progress, taskLink)
}
