package co.smartreceipts.android.trips.navigation

import co.smartreceipts.android.model.Trip

/**
 * Defines how we can route to view the receipts contained in a specific trip
 */
interface ViewReceiptsInTripRouter {

    fun routeToViewReceipts(trip: Trip)

}