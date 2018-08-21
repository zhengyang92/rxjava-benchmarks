package co.smartreceipts.android.model.impl.columns.categories

import co.smartreceipts.android.model.impl.columns.AbstractColumnImpl
import co.smartreceipts.android.persistence.database.controllers.grouping.results.SumCategoryGroupingResult
import co.smartreceipts.android.sync.model.SyncState

class CategoryNameColumn(id: Int, syncState: SyncState) :
    AbstractColumnImpl<SumCategoryGroupingResult>(
        id,
        CategoryColumnDefinitions.ActualDefinition.NAME,
        syncState
    ) {

    override fun getValue(sumCategoryGroupingResult: SumCategoryGroupingResult): String =
        sumCategoryGroupingResult.category.name + " [" + sumCategoryGroupingResult.receiptsCount + "]"
}
