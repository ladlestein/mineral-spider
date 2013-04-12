package com.nowanswers.spider

import akka.routing.RoundRobinRouter
import java.io.File
import com.nowanswers.spider.mineral.MindatMineralPageScraperComponent
import akka.actor.{ActorRef, Props, Actor}
import akka.pattern._
import org.ccil.cowan.tagsoup.jaxp.SAXFactoryImpl
import java.net.URL
import com.nowanswers.chemistry.BasicFormulaParserComponent
import com.mongodb.casbah.Imports._
import akka.actor.Status.Success
import scala.concurrent.Future
import com.nowanswers.spider.implicits._
import spray.io.IOExtension
import spray.can.client.HttpClient
import spray.client.HttpConduit
import spray.http.{HttpMethods, HttpRequest}
import akka.event.Logging

/**
 * Created with IntelliJ IDEA.
 * User: ladlestein
 * Date: 6/11/12
 * Time: 6:34 PM
 *
 * The spidering code is here.
 */

object Spider {

  val hostname = "www.mindat.org"
  val letterPage = s"http://$hostname/index-%s.html"
  val cacheDir = "/Users/ladlestein/cache"
  new File(cacheDir).mkdir

  val nLetterFetchers = 2
  val nMineralVisitors = 5

  val parserFactory = new SAXFactoryImpl

  val master = system.actorOf(Props[Master], name = "Master")

  val ioBridge = IOExtension(system).ioBridge()
  val httpClient = system.actorOf(Props(new HttpClient(ioBridge)))

  val conduit = system.actorOf(
    props = Props(new HttpConduit(httpClient, hostname, 80)),
    name = "http-conduit"
  )
  val pipeline = HttpConduit.sendReceive(conduit)

  val mineralFetchers = system.actorOf(Props[MineralVisitor].withRouter(RoundRobinRouter(nMineralVisitors)), name = "mineralVisitor")

  def run = {
    master ? Start
  }

  class Master extends Actor {

    val log = Logging(context.system, this)

    val topLevelFetchers = context.actorOf(Props[PageVisitor].withRouter(RoundRobinRouter(nLetterFetchers)), name = "letterRouter")

    def receive = {
      case Start => {
        val cs = sender
        val tasks = Future.sequence(
          for (letter <- 'A' to 'B')
            yield {
              val url = letterPage format letter
              topLevelFetchers ? VisitPage(url)
            }
        )
        tasks onComplete { _ =>
          {
            log info "all letter-pages fetched"
            cs ! Success
          }
        }
      }
    }
  }

  class PageVisitor extends Actor {

    val log = Logging(context.system, this)

    val parser = parserFactory.newSAXParser

    def receive = {
      case VisitPage(url) => {
        val cs = sender
        val resP = pipeline(HttpRequest(method = HttpMethods.GET, uri = url))

        resP map {
          res => {
            val body = res.entity.asString
            log info s"have body for $url"

            try {
              val loader = xml.XML.withSAXParser(parser)
              val doc = loader.loadString(body)
              val seq = doc \\ "a"

              seq filter { elem =>
                ! ((elem \ "b") isEmpty)
              } map { elem =>
                elem \ "@href"
              } foreach { href =>
                val mineralPageUrl = new URL(new URL(url), href.text)
                mineralFetchers ? VisitMineral(mineralPageUrl.toString)
              }

              log info s"# of links in $url is ${seq.length}"
            }
            catch {
              case x: Exception => log error(x, s"Exception thrown processing letter page $url")
              throw x
            }
            cs ! Success
          }
        }
      }
    }
  }

  class MineralVisitor extends Actor

  with RealMineralBuilderComponent
  with BasicFormulaParserComponent
  with MindatMineralPageScraperComponent
  with MongoMineralStoreComponent {

    val log = Logging(context.system, this)

    val saxParser = parserFactory.newSAXParser

    val collection = {
      val connection = MongoConnection()
      val database = connection("spider")
      database ("minerals")
    }


    def receive = {
      case VisitMineral(url) => {
        val cs = sender
        val resP = pipeline(HttpRequest(method = HttpMethods.GET, uri = url))
        resP map {
          res => {
            val body = res.entity.asString
            log info s"have body for $url"

            try {
              val loader = xml.XML.withSAXParser(saxParser)
              val doc = loader.loadString(body)
              (builder buildMineral doc) foreach { mineral =>
                log info s"Found mineral ${mineral.name} with formula ${mineral.formula}"
                store storeMineral mineral
              }
            }

            catch {
              case x: Exception => log error(x, s"Exception thrown processing mineral page $url")
              throw x
            }
            cs ! Success

          }
        }
      }
    }

  }
}

sealed trait SpiderMessage

case class Start(listener: ActorRef) extends SpiderMessage
case object Flow extends SpiderMessage

case object Finished extends SpiderMessage
case class VisitPage(url: String) extends SpiderMessage
case class VisitMineral(url: String) extends SpiderMessage
case class CachePage(letter: Char, body: String, listener: ActorRef) extends SpiderMessage


