/**
 * Created with IntelliJ IDEA.
 * User: ladlestein
 * Date: 4/10/13
 * Time: 1:10 AM
 * To change this template use File | Settings | File Templates.
 */

package com.nowanswers.spider

import scala.concurrent.ExecutionContext
import akka.util.Timeout
import scala.concurrent.duration._
import akka.actor.ActorSystem

package object implicits {

  val system = ActorSystem("spider")

  val dispatcher = system.dispatcher

  implicit val ec = ExecutionContext.fromExecutor(dispatcher)

  implicit val timeout = Timeout(5 seconds)


}
