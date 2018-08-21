package co.smartreceipts.android.persistence.database.defaults;

import android.support.annotation.NonNull;

import com.google.common.base.Preconditions;

import javax.inject.Inject;

import co.smartreceipts.android.di.scopes.ApplicationScope;
import co.smartreceipts.android.persistence.database.tables.CSVTable;
import co.smartreceipts.android.persistence.database.tables.CategoriesTable;
import co.smartreceipts.android.persistence.database.tables.PDFTable;
import co.smartreceipts.android.persistence.database.tables.PaymentMethodsTable;
import dagger.Module;

public class WhiteLabelFriendlyTableDefaultsCustomizer implements TableDefaultsCustomizer {

    DefaultTableDefaultCustomizerImpl tableDefaultsCustomizer;

    public WhiteLabelFriendlyTableDefaultsCustomizer(@NonNull DefaultTableDefaultCustomizerImpl tableDefaultCustomizer) {
        this.tableDefaultsCustomizer = Preconditions.checkNotNull(tableDefaultCustomizer);
    }

    @Override
    public void insertCSVDefaults(@NonNull CSVTable table) {
        tableDefaultsCustomizer.insertCSVDefaults(table);
    }

    @Override
    public void insertPDFDefaults(@NonNull PDFTable table) {
        tableDefaultsCustomizer.insertPDFDefaults(table);
    }

    @Override
    public void insertCategoryDefaults(@NonNull CategoriesTable table) {
        tableDefaultsCustomizer.insertCategoryDefaults(table);
    }

    @Override
    public void insertPaymentMethodDefaults(@NonNull PaymentMethodsTable table) {
        tableDefaultsCustomizer.insertPaymentMethodDefaults(table);
    }
}
