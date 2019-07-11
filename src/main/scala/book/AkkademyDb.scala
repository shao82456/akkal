package book

import akka.actor.Actor
import akka.event.Logging

import scala.collection.mutable

object AkkademyDb{
  case class SetRequest(key:String,value:Object)
}
class AkkademyDb extends Actor{
  import AkkademyDb._

  val map=new mutable.HashMap[String,Object]()
  val log=Logging(context.system,this)
  override def receive: Receive = {
    case SetRequest(key,value)=>{
      log.info("received SetReq of key:{} and value:{}",key,value)
      map.put(key,value)
    }
    case o =>{
      log.info("received unknown message:{}",o)
    }

  }
}
