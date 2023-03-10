package com.tudux.taxi

import akka.NotUsed
import akka.actor.typed.{ActorSystem, Behavior, Scheduler}
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.typed.Cluster
import akka.util.Timeout
import com.tudux.taxi.actors.PersistentTaxiTripEntry.Response
import com.tudux.taxi.actors.PersistentTaxiTripEntry.Response.CreatedTaxiTripEntryResponse
import com.tudux.taxi.actors.TaxiTripParent
import com.tudux.taxi.actors.TaxiTripParent.CreateTaxiTripEntryParent
import com.tudux.taxi.common.TaxiTripEntryCommon.TaxiTripEntry

import java.util.UUID
import scala.concurrent.ExecutionContext

object App extends App {

  val rootBehavior: Behavior[NotUsed] = Behaviors.setup { context =>

    val taxi_parent = context.spawn(TaxiTripParent(),"TaxiParent")
    val logger = context.log

    val responseHandler = context.spawn(Behaviors.receiveMessage[Response] {
      case CreatedTaxiTripEntryResponse(taxi_trip_id) =>
        logger.info(s"Succesfully created taxi trip $taxi_trip_id")
        Behaviors.same

    },"replyHandler")

    import akka.actor.typed.scaladsl.AskPattern._
    import scala.concurrent.duration._
    implicit val timeout: Timeout = Timeout(2.seconds)
    implicit val scheduler: Scheduler = context.system.scheduler
    implicit val ec: ExecutionContext = context.executionContext

    val trip_id = UUID.randomUUID().toString
    val taxiTripEntry: TaxiTripEntry = TaxiTripEntry(trip_id,2,"2016-01-01 00:00:00","2016-01-01 00:00:00",2,1.10,-73.990371704101563,40.734695434570313,1,"N",-73.981842041015625,40.732406616210937,2,7.5,0.5,0.5,0,0,0.3,8.8)
    taxi_parent ! CreateTaxiTripEntryParent(taxiTripEntry, responseHandler)

    Behaviors.empty
  }

  //verify docker exec -it taxi-typed cqlsh
  val system = ActorSystem(rootBehavior, "TaxiActorSystemTyped")
}
