package co.smartreceipts.android.persistence.database.controllers.impl;

import javax.inject.Inject;

import co.smartreceipts.android.analytics.Analytics;
import co.smartreceipts.android.di.scopes.ApplicationScope;
import co.smartreceipts.android.model.ColumnDefinitions;
import co.smartreceipts.android.model.Receipt;
import co.smartreceipts.android.persistence.DatabaseHelper;

@ApplicationScope
public class CSVTableController extends ColumnTableController {

    @Inject
    public CSVTableController(DatabaseHelper databaseHelper, Analytics analytics, ColumnDefinitions<Receipt> receiptColumnDefinitions) {
        super(databaseHelper.getCSVTable(), analytics, receiptColumnDefinitions);
    }
}
