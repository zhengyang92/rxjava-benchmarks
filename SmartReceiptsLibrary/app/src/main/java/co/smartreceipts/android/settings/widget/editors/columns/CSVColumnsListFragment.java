package co.smartreceipts.android.settings.widget.editors.columns;

import android.content.Context;
import android.support.v7.app.ActionBar;

import javax.inject.Inject;

import co.smartreceipts.android.R;
import co.smartreceipts.android.model.Column;
import co.smartreceipts.android.model.Receipt;
import co.smartreceipts.android.model.impl.columns.receipts.ReceiptColumnDefinitions;
import co.smartreceipts.android.persistence.database.controllers.TableController;
import co.smartreceipts.android.persistence.database.controllers.impl.CSVTableController;
import co.smartreceipts.android.persistence.database.tables.ordering.OrderingPreferencesManager;
import co.smartreceipts.android.workers.reports.ReportResourcesManager;
import dagger.android.support.AndroidSupportInjection;

public class CSVColumnsListFragment extends ColumnsListFragment {

    public static String TAG = "CSVColumnsListFragment";

    @Inject
    ReceiptColumnDefinitions receiptColumnDefinitions;
    @Inject
    CSVTableController csvTableController;
    @Inject
    OrderingPreferencesManager orderingPreferencesManager;
    @Inject
    ReportResourcesManager reportResourcesManager;

    public static CSVColumnsListFragment newInstance() {
        return new CSVColumnsListFragment();
    }

    @Override
    public void onAttach(Context context) {
        AndroidSupportInjection.inject(this);
        super.onAttach(context);
    }

    @Override
    public void onResume() {
        super.onResume();
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.menu_main_csv);
        }
    }

    @Override
    protected TableController<Column<Receipt>> getTableController() {
        return csvTableController;
    }

    @Override
    protected ReceiptColumnDefinitions getReceiptColumnDefinitions() {
        return receiptColumnDefinitions;
    }

    @Override
    protected void saveTableOrdering() {
        super.saveTableOrdering();
        orderingPreferencesManager.saveCsvColumnsTableOrdering();
    }

    @Override
    protected ReportResourcesManager getReportResourcesManager() {
        return reportResourcesManager;
    }
}
