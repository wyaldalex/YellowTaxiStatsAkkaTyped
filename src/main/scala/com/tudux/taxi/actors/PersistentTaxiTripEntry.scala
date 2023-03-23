package com.tudux.taxi.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import com.tudux.taxi.common.TaxiTripEntryCommon.TaxiTripEntry

object PersistentTaxiTripEntry {

  sealed trait Command {
    val trip_id: String
  }
  object Command {
    case class CreateTaxiTripEntry(trip_id: String,taxi_trip_entry: TaxiTripEntry, replyTo: ActorRef[Response]) extends Command
    case class GetTaxiTripEntry(trip_id: String, replyTo: ActorRef[Response]) extends Command
  }

  sealed trait Event
  case class CreatedTaxiTripEntry(taxiTripEntry: TaxiTripEntry) extends Event

  sealed trait Response
  object Response {
    case class CreatedTaxiTripEntryResponse(taxi_trip_id: String) extends  Response
    case class GetTaxiTripEntryResponse(taxi_trip: TaxiTripEntry) extends  Response
  }

  import Command._
  import Response._

  val commandHandler: (TaxiTripEntry, Command) => Effect[Event, TaxiTripEntry] =
    (state, command) =>
    command match {
      case CreateTaxiTripEntry(trip_id,taxiTripEntry, replyTo) =>
        Effect
          .persist(CreatedTaxiTripEntry(taxiTripEntry)) //this thing requires an explicit actor ref to reply to in the http layer...
          .thenReply(replyTo)(_ => CreatedTaxiTripEntryResponse(trip_id))

      case GetTaxiTripEntry(trip_id, replyTo) =>
        Effect.reply(replyTo)(GetTaxiTripEntryResponse(state))
    }

  val eventHandler: (TaxiTripEntry, Event) => TaxiTripEntry = (state,event) =>
    event match {
      case CreatedTaxiTripEntry(taxiTripEntry) =>
        taxiTripEntry
    }

  def apply(trip_id: String): Behavior[Command] =
    EventSourcedBehavior[Command, Event, TaxiTripEntry](
      persistenceId = PersistenceId.ofUniqueId(trip_id),
      emptyState = TaxiTripEntry("",0,"","",0,0,0,0,0,"",0,0,0,0,0,0,0,0,0,0),
      commandHandler = commandHandler,
      eventHandler = eventHandler
    )
}
