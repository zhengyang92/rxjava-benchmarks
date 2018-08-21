package co.smartreceipts.android.model.impl.columns.receipts

import android.content.Context
import co.smartreceipts.android.model.Price
import co.smartreceipts.android.model.Receipt
import co.smartreceipts.android.model.factory.PriceBuilderFactory
import co.smartreceipts.android.settings.UserPreferenceManager
import co.smartreceipts.android.settings.catalog.UserPreference
import co.smartreceipts.android.sync.model.SyncState
import java.util.*

/**
 * Provides a column that returns the total of the price and tax fields based on user settings
 */
class ReceiptNetExchangedPricePlusTaxColumn(
    id: Int, syncState: SyncState,
    localizedContext: Context,
    private val preferences: UserPreferenceManager,
    customOrderId: Long
) : AbstractExchangedPriceColumn(
    id,
    ReceiptColumnDefinitions.ActualDefinition.PRICE_PLUS_TAX_EXCHANGED,
    syncState,
    localizedContext,
    customOrderId
) {

    override fun getPrice(receipt: Receipt): Price {
        return if (preferences.get(UserPreference.Receipts.UsePreTaxPrice)) {
            val factory = PriceBuilderFactory()
            factory.setPrices(Arrays.asList(receipt.price, receipt.tax), receipt.trip.tripCurrency)
            factory.build()
        } else {
            receipt.price
        }
    }
}
