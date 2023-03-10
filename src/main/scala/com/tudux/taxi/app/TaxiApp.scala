package com.tudux.taxi.app

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.AskPattern._
import akka.cluster.typed.{Cluster, Join}
import akka.http.scaladsl.Http
import akka.util.Timeout
import com.tudux.taxi.actors.TaxiTripParent

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}
import com.tudux.taxi.actors.TaxiTripParent.CommandParent
import com.tudux.taxi.routes.TaxiRouter

object TaxiApp {

  def startHttpServer(taxi: ActorRef[CommandParent])(implicit system: ActorSystem[_]): Unit = {
    implicit val ec: ExecutionContext = system.executionContext
    val router = new TaxiRouter(taxi)
    val routes = router.routes

    val httpBindingFuture = Http().newServerAt("localhost", 10001).bind(routes)
    httpBindingFuture.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info(s"Server online at http://${address.getHostString}:${address.getPort}")
      case Failure(ex) =>
        system.log.error(s"Failed to bind HTTP server, because: $ex")
        system.terminate()
    }
  }


  def main(args: Array[String]): Unit = {
    trait RootCommand
    case class RetrieveTaxiActor(reply_to: ActorRef[ActorRef[CommandParent]]) extends RootCommand

    val rootBehavior: Behavior[RootCommand] = Behaviors.setup { context =>
      val taxi_parent_actor = context.spawn(TaxiTripParent(), "taxiActorParent")
      Behaviors.receiveMessage {
        case RetrieveTaxiActor(reply_to) =>
          reply_to ! taxi_parent_actor
          Behaviors.same
      }
    }

    implicit val system: ActorSystem[RootCommand] = ActorSystem(rootBehavior, "TaxiActorSystemTyped")
    //required to join, seems like dev mode does not perform automatic join
    //https://discuss.lightbend.com/t/running-services-failed-akka-no-coordinator-found-to-register-probably-no-seed-nodes-configured-and-manual-cluster-join-not-performed/3334/3
    val cluster  = Cluster(system)
    cluster.manager.tell(Join(cluster.selfMember.address))

    implicit val timeout: Timeout = Timeout(5.seconds)
    implicit val ec: ExecutionContext = system.executionContext

    val taxi_actor_future: Future[ActorRef[CommandParent]] = system.ask(reply_to => RetrieveTaxiActor(reply_to))
    taxi_actor_future.foreach(startHttpServer)
  }

}
