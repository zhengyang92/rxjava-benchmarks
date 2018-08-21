package co.smartreceipts.android.persistence.database.tables.adapters;

import android.content.ContentValues;
import android.database.Cursor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import co.smartreceipts.android.model.Column;
import co.smartreceipts.android.model.Receipt;
import co.smartreceipts.android.model.impl.columns.receipts.ReceiptColumnDefinitions;
import co.smartreceipts.android.model.impl.columns.receipts.ReceiptNameColumn;
import co.smartreceipts.android.persistence.database.operations.DatabaseOperationMetadata;
import co.smartreceipts.android.persistence.database.operations.OperationFamilyType;
import co.smartreceipts.android.persistence.database.tables.AbstractColumnTable;
import co.smartreceipts.android.persistence.database.tables.AbstractSqlTable;
import co.smartreceipts.android.persistence.database.tables.keys.PrimaryKey;
import co.smartreceipts.android.settings.UserPreferenceManager;
import co.smartreceipts.android.sync.model.SyncState;
import co.smartreceipts.android.workers.reports.ReportResourcesManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class ColumnDatabaseAdapterTest {

    private static final int ID = 5;
    private static final int TYPE = ReceiptColumnDefinitions.ActualDefinition.NAME.getColumnType();
	private static final long CUSTOM_ORDER_ID = 10;
    
    // Class under test
    ColumnDatabaseAdapter columnDatabaseAdapter;

    @Mock
    Cursor cursor;

    @Mock
    Column<Receipt> column;

    @Mock
    PrimaryKey<Column<Receipt>, Integer> primaryKey;

    @Mock
    ReportResourcesManager reportResourcesManager;

    @Mock
    UserPreferenceManager preferences;

    @Mock
    SyncStateAdapter syncStateAdapter;

    @Mock
    SyncState syncState, getSyncState;

    Column<Receipt> receiptNameColumn;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        final int idIndex = 1;
        final int typeIndex = 2;
        final int customOrderIdIndex = 3;

        receiptNameColumn = new ReceiptNameColumn(ID, getSyncState, CUSTOM_ORDER_ID);
        ReceiptColumnDefinitions receiptColumnDefinitions = new ReceiptColumnDefinitions(reportResourcesManager, preferences);

        when(reportResourcesManager.getLocalizedContext()).thenReturn(RuntimeEnvironment.systemContext);

        when(cursor.getColumnIndex(AbstractColumnTable.COLUMN_ID)).thenReturn(idIndex);
        when(cursor.getColumnIndex(AbstractColumnTable.COLUMN_TYPE)).thenReturn(typeIndex);
        when(cursor.getColumnIndex(AbstractSqlTable.COLUMN_CUSTOM_ORDER_ID)).thenReturn(customOrderIdIndex);
        when(cursor.getInt(idIndex)).thenReturn(ID);
        when(cursor.getInt(typeIndex)).thenReturn(TYPE);
        when(cursor.getLong(customOrderIdIndex)).thenReturn(CUSTOM_ORDER_ID);

        when(column.getId()).thenReturn(ID);
        when(column.getType()).thenReturn(TYPE);
        when(column.getSyncState()).thenReturn(syncState);
        when(column.getCustomOrderId()).thenReturn(CUSTOM_ORDER_ID);

        when(primaryKey.getPrimaryKeyValue(column)).thenReturn(ID);

        when(syncStateAdapter.read(cursor)).thenReturn(syncState);
        when(syncStateAdapter.get(any(SyncState.class), any(DatabaseOperationMetadata.class))).thenReturn(getSyncState);

        columnDatabaseAdapter = new ColumnDatabaseAdapter(receiptColumnDefinitions, syncStateAdapter);
    }

    @Test
    public void read() throws Exception {
        assertEquals(receiptNameColumn, columnDatabaseAdapter.read(cursor));
    }

    @Test
    public void write() throws Exception {
        final String sync = "sync";
        final ContentValues syncValues = new ContentValues();
        syncValues.put(sync, sync);
        when(syncStateAdapter.writeUnsynced(syncState)).thenReturn(syncValues);

        final ContentValues contentValues = columnDatabaseAdapter.write(column, new DatabaseOperationMetadata());
        assertEquals(Integer.valueOf(TYPE), contentValues.getAsInteger(AbstractColumnTable.COLUMN_TYPE));
        assertEquals(sync, contentValues.getAsString(sync));
        assertFalse(contentValues.containsKey(AbstractColumnTable.COLUMN_ID));
        assertEquals(CUSTOM_ORDER_ID, (long) contentValues.getAsLong(AbstractSqlTable.COLUMN_CUSTOM_ORDER_ID));
}

    @Test
    public void writeUnsycned() throws Exception {
        final String sync = "sync";
        final ContentValues syncValues = new ContentValues();
        syncValues.put(sync, sync);
        when(syncStateAdapter.write(syncState)).thenReturn(syncValues);

        final ContentValues contentValues = columnDatabaseAdapter.write(column, new DatabaseOperationMetadata(OperationFamilyType.Sync));
        assertEquals(Integer.valueOf(TYPE), contentValues.getAsInteger(AbstractColumnTable.COLUMN_TYPE));
        assertEquals(sync, contentValues.getAsString(sync));
        assertFalse(contentValues.containsKey(AbstractColumnTable.COLUMN_ID));
        assertEquals(CUSTOM_ORDER_ID, (long) contentValues.getAsLong(AbstractSqlTable.COLUMN_CUSTOM_ORDER_ID));
    }

    @Test
    public void build() throws Exception {
        assertEquals(receiptNameColumn, columnDatabaseAdapter.build(column, primaryKey, mock(DatabaseOperationMetadata.class)));
        assertEquals(receiptNameColumn.getSyncState(), columnDatabaseAdapter.build(column, primaryKey, mock(DatabaseOperationMetadata.class)).getSyncState());
    }
}