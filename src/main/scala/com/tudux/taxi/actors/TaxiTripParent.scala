package com.tudux.taxi.actors

import akka.actor.PoisonPill
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.HashCodeNoEnvelopeMessageExtractor
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import com.tudux.taxi.actors.PersistentTaxiTripEntry.Command.{CreateTaxiTripEntry, GetTaxiTripEntry}
import com.tudux.taxi.actors.PersistentTaxiTripEntry.{Command, Response}
import com.tudux.taxi.common.TaxiTripEntryCommon.TaxiTripEntry

import java.util.UUID


object TaxiTripParent {

  sealed trait CommandParent
  case class CreateTaxiTripEntryParent(taxiTripEntry: TaxiTripEntry, replyTo: ActorRef[Response]) extends CommandParent
  case class GetTaxiTripEntryParent(trip_id: String, replyTo: ActorRef[Response]) extends CommandParent

  def apply(): Behavior[CommandParent] =Behaviors.setup{ context =>

    //Persistent actor setup form1
    //val trip_id = UUID.randomUUID().toString
    //val persistent_taxi_entry_actor = context.spawn(PersistentTaxiTripEntry(trip_id),"persistent_taxi_entry_actor")

    val sharding = ClusterSharding(context.system)
    val TaxiEntryHandlerTypeKey = EntityTypeKey[Command]("TaxiEntryRequestHandler")

    val messageExtractor =
      new HashCodeNoEnvelopeMessageExtractor[Command](numberOfShards = 30) {
        override def entityId(message: Command): String = message.trip_id
      }

    val shardRegion: ActorRef[Command] =
      sharding.init(
        Entity(TaxiEntryHandlerTypeKey) { context =>
          PersistentTaxiTripEntry(context.entityId)
        }.withMessageExtractor(messageExtractor)
          //.withStopMessage(PoisonPill) Pending create graceful shutdown case object as default is PoisonPill
      )

    Behaviors.receiveMessage { message =>
      message match {
        case CreateTaxiTripEntryParent(taxiTripEntry, replyTo) =>
          //persistent_taxi_entry_actor.tell(CreateTaxiTripEntry(trip_id,taxiTripEntry,replyTo))
          context.log.info(s"[Taxi Parent] inserting entry $taxiTripEntry")
          shardRegion ! CreateTaxiTripEntry(taxiTripEntry.trip_id,taxiTripEntry,replyTo)
          Behaviors.same
        case GetTaxiTripEntryParent(trip_id,replyTo) =>
          context.log.info(s"[Taxi Parent] retrieving entry $trip_id")
          shardRegion ! GetTaxiTripEntry(trip_id, replyTo)
          Behaviors.same
      }
    }

  }

}
