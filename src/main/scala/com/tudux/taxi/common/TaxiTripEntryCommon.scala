package com.tudux.taxi.common

object TaxiTripEntryCommon {

  case class TaxiTripEntry(trip_id: String , vendor_id: Int, tpep_pickup_datetime: String, tpep_dropoff_datetime: String, passenger_count: Int,
                      trip_distance: Double, pickup_longitude: Double, pickup_latitude: Double, rate_codeID: Int,
                      store_and_fwd_flag: String, dropoff_longitude: Double, dropoff_latitude: Double,
                      payment_type: Int, fare_amount: Double, extra: Double, mta_tax: Double,
                      tip_amount: Double, tolls_amount: Double, improvement_surcharge: Double, total_amount: Double)

}
