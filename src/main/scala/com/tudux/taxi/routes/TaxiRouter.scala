package com.tudux.taxi.routes

import akka.http.scaladsl.server.Directives._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.model.headers.Location
import io.circe.generic.auto._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import akka.http.scaladsl.server.Route
import cats.data.Validated.{Invalid, Valid}
import cats.implicits._
import com.tudux.taxi.actors.PersistentTaxiTripEntry.Response
import com.tudux.taxi.actors.PersistentTaxiTripEntry.Response.CreatedTaxiTripEntryResponse
import com.tudux.taxi.actors.TaxiTripParent.{CommandParent, CreateTaxiTripEntryParent}
import com.tudux.taxi.common.TaxiTripEntryCommon.TaxiTripEntry

import java.util.UUID

case class RequestTaxiTripEntryRequest(vendor_id: Int, tpep_pickup_datetime: String, tpep_dropoff_datetime: String, passenger_count: Int,
                                       trip_distance: Double, pickup_longitude: Double, pickup_latitude: Double, rate_codeID: Int,
                                       store_and_fwd_flag: String, dropoff_longitude: Double, dropoff_latitude: Double,
                                       payment_type: Int, fare_amount: Double, extra: Double, mta_tax: Double,
                                       tip_amount: Double, tolls_amount: Double, improvement_surcharge: Double, total_amount: Double) {


  val trip_id = UUID.randomUUID().toString
  val taxi_trip_entry = TaxiTripEntry(trip_id,vendor_id, tpep_pickup_datetime, tpep_dropoff_datetime, passenger_count, trip_distance, pickup_longitude, pickup_latitude, rate_codeID, store_and_fwd_flag, dropoff_longitude, dropoff_latitude, payment_type, fare_amount, extra, mta_tax, tip_amount, tolls_amount, improvement_surcharge, total_amount)
  def toCommand(replyTo: ActorRef[Response]): CommandParent = CreateTaxiTripEntryParent(taxi_trip_entry, replyTo)
}

class TaxiRouter(taxi_actor: ActorRef[CommandParent])(implicit system: ActorSystem[_]) {
  implicit val timeout: Timeout = Timeout(5.seconds)

  def createTaxiEntry(request: RequestTaxiTripEntryRequest): Future[Response] =
    taxi_actor.ask(replyTo => request.toCommand(replyTo))

  def toHttpEntity(payload: String): HttpEntity.Strict = HttpEntity(ContentTypes.`application/json`, payload)
  case class GenericResponse(taxi_trip_id: String)

  val routes =
    pathPrefix("taxi") {
      pathEndOrSingleSlash {
        post {
          entity(as[RequestTaxiTripEntryRequest]) { request =>
            onSuccess(createTaxiEntry(request)) {
              case CreatedTaxiTripEntryResponse(taxi_trip_id) =>
                complete(
                  HttpResponse(
                    StatusCodes.Created,
                    entity = HttpEntity(
                      ContentTypes.`text/plain(UTF-8)`,
                      s"Taxi Trip Entry Created with Id: ${taxi_trip_id}"
                    )
                  )
                )
            }
          }
        }
      }
    }

}
