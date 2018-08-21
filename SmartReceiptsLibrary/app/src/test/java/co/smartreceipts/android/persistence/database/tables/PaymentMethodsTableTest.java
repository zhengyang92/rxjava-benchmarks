package co.smartreceipts.android.persistence.database.tables;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import co.smartreceipts.android.model.PaymentMethod;
import co.smartreceipts.android.model.factory.PaymentMethodBuilderFactory;
import co.smartreceipts.android.persistence.DatabaseHelper;
import co.smartreceipts.android.persistence.database.defaults.TableDefaultsCustomizer;
import co.smartreceipts.android.persistence.database.operations.DatabaseOperationMetadata;
import co.smartreceipts.android.persistence.database.tables.ordering.OrderingPreferencesManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class PaymentMethodsTableTest {

    private static final String METHOD1 = "name1";
    private static final String METHOD2 = "name2";
    private static final String METHOD3 = "name3";
    private static final int ORDER_ID1 = 1;
    private static final int ORDER_ID2 = 2;

    // Class under test
    PaymentMethodsTable mPaymentMethodsTable;

    @Mock
    SQLiteDatabase mSQLiteDatabase;

    @Mock
    TableDefaultsCustomizer mTableDefaultsCustomizer;

    @Mock
    OrderingPreferencesManager orderingPreferencesManager;

    @Captor
    ArgumentCaptor<String> mSqlCaptor;

    SQLiteOpenHelper mSQLiteOpenHelper;

    PaymentMethod mPaymentMethod1;

    PaymentMethod mPaymentMethod2;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        mSQLiteOpenHelper = new TestSQLiteOpenHelper(RuntimeEnvironment.application);
        mPaymentMethodsTable = new PaymentMethodsTable(mSQLiteOpenHelper, orderingPreferencesManager);

        // Now create the table and insert some defaults
        mPaymentMethodsTable.onCreate(mSQLiteOpenHelper.getWritableDatabase(), mTableDefaultsCustomizer);
        mPaymentMethod1 = mPaymentMethodsTable.insert(new PaymentMethodBuilderFactory().setMethod(METHOD1).setCustomOrderId(ORDER_ID1).build(), new DatabaseOperationMetadata()).blockingGet();
        mPaymentMethod2 = mPaymentMethodsTable.insert(new PaymentMethodBuilderFactory().setMethod(METHOD2).setCustomOrderId(ORDER_ID2).build(), new DatabaseOperationMetadata()).blockingGet();
    }

    @After
    public void tearDown() {
        mSQLiteOpenHelper.getWritableDatabase().execSQL("DROP TABLE IF EXISTS " + mPaymentMethodsTable.getTableName());
    }

    @Test
    public void getTableName() {
        assertEquals("paymentmethods", mPaymentMethodsTable.getTableName());
    }

    @Test
    public void onCreate() {
        final TableDefaultsCustomizer customizer = mock(TableDefaultsCustomizer.class);
        mPaymentMethodsTable.onCreate(mSQLiteDatabase, customizer);
        verify(mSQLiteDatabase, atLeastOnce()).execSQL(mSqlCaptor.capture());
        verify(customizer).insertPaymentMethodDefaults(mPaymentMethodsTable);

        assertTrue(mSqlCaptor.getValue().contains("CREATE TABLE paymentmethods"));
        assertTrue(mSqlCaptor.getValue().contains("id INTEGER PRIMARY KEY AUTOINCREMENT"));
        assertTrue(mSqlCaptor.getValue().contains("method TEXT"));
        assertTrue(mSqlCaptor.getValue().contains("drive_sync_id TEXT"));
        assertTrue(mSqlCaptor.getValue().contains("drive_is_synced BOOLEAN DEFAULT 0"));
        assertTrue(mSqlCaptor.getValue().contains("drive_marked_for_deletion BOOLEAN DEFAULT 0"));
        assertTrue(mSqlCaptor.getValue().contains("last_local_modification_time DATE"));
        assertTrue(mSqlCaptor.getValue().contains("custom_order_id INTEGER DEFAULT 0"));
    }

    @Test
    public void onUpgradeFromV11() {
        final int oldVersion = 11;
        final int newVersion = DatabaseHelper.DATABASE_VERSION;

        final TableDefaultsCustomizer customizer = mock(TableDefaultsCustomizer.class);
        mPaymentMethodsTable.onUpgrade(mSQLiteDatabase, oldVersion, newVersion, customizer);
        verify(mSQLiteDatabase, atLeastOnce()).execSQL(mSqlCaptor.capture());
        verify(customizer).insertPaymentMethodDefaults(mPaymentMethodsTable);

        assertTrue(mSqlCaptor.getAllValues().get(0).contains(PaymentMethodsTable.TABLE_NAME));
        assertTrue(mSqlCaptor.getAllValues().get(0).contains(PaymentMethodsTable.COLUMN_ID));
        assertTrue(mSqlCaptor.getAllValues().get(0).contains(PaymentMethodsTable.COLUMN_METHOD));
        assertEquals(mSqlCaptor.getAllValues().get(0), "CREATE TABLE paymentmethods (id INTEGER PRIMARY KEY AUTOINCREMENT, method TEXT);");
        assertEquals(mSqlCaptor.getAllValues().get(1), "ALTER TABLE " + mPaymentMethodsTable.getTableName() + " ADD drive_sync_id TEXT");
        assertEquals(mSqlCaptor.getAllValues().get(2), "ALTER TABLE " + mPaymentMethodsTable.getTableName() + " ADD drive_is_synced BOOLEAN DEFAULT 0");
        assertEquals(mSqlCaptor.getAllValues().get(3), "ALTER TABLE " + mPaymentMethodsTable.getTableName() + " ADD drive_marked_for_deletion BOOLEAN DEFAULT 0");
        assertEquals(mSqlCaptor.getAllValues().get(4), "ALTER TABLE " + mPaymentMethodsTable.getTableName() + " ADD last_local_modification_time DATE");
        assertEquals(mSqlCaptor.getAllValues().get(5), "ALTER TABLE " + mPaymentMethodsTable.getTableName() + " ADD COLUMN custom_order_id INTEGER DEFAULT 0");
    }

    @Test
    public void onUpgradeFromV14() {
        final int oldVersion = 14;
        final int newVersion = DatabaseHelper.DATABASE_VERSION;

        final TableDefaultsCustomizer customizer = mock(TableDefaultsCustomizer.class);
        mPaymentMethodsTable.onUpgrade(mSQLiteDatabase, oldVersion, newVersion, customizer);
        verify(mSQLiteDatabase, atLeastOnce()).execSQL(mSqlCaptor.capture());
        verify(customizer, never()).insertPaymentMethodDefaults(mPaymentMethodsTable);

        assertEquals(mSqlCaptor.getAllValues().get(0), "ALTER TABLE " + mPaymentMethodsTable.getTableName() + " ADD drive_sync_id TEXT");
        assertEquals(mSqlCaptor.getAllValues().get(1), "ALTER TABLE " + mPaymentMethodsTable.getTableName() + " ADD drive_is_synced BOOLEAN DEFAULT 0");
        assertEquals(mSqlCaptor.getAllValues().get(2), "ALTER TABLE " + mPaymentMethodsTable.getTableName() + " ADD drive_marked_for_deletion BOOLEAN DEFAULT 0");
        assertEquals(mSqlCaptor.getAllValues().get(3), "ALTER TABLE " + mPaymentMethodsTable.getTableName() + " ADD last_local_modification_time DATE");
        assertEquals(mSqlCaptor.getAllValues().get(4), "ALTER TABLE " + mPaymentMethodsTable.getTableName() + " ADD COLUMN custom_order_id INTEGER DEFAULT 0");
    }

    @Test
    public void onUpgradeFromV15() {
        final int oldVersion = 15;
        final int newVersion = DatabaseHelper.DATABASE_VERSION;

        final TableDefaultsCustomizer customizer = mock(TableDefaultsCustomizer.class);
        mPaymentMethodsTable.onUpgrade(mSQLiteDatabase, oldVersion, newVersion, customizer);
        verify(mSQLiteDatabase, atLeastOnce()).execSQL(mSqlCaptor.capture());
        verify(customizer, never()).insertPaymentMethodDefaults(mPaymentMethodsTable);

        assertEquals(mSqlCaptor.getAllValues().get(0), "ALTER TABLE " + mPaymentMethodsTable.getTableName() + " ADD COLUMN custom_order_id INTEGER DEFAULT 0");
    }

    @Test
    public void onUpgradeFromV16WhenCustomOrderIdColumnIsPresent() {
        final int oldVersion = 16;
        final int newVersion = DatabaseHelper.DATABASE_VERSION;

        final TableDefaultsCustomizer customizer = mock(TableDefaultsCustomizer.class);
        final Cursor cursor = mock(Cursor.class);
        final String pragmaTableInfo = "PRAGMA table_info(" + mPaymentMethodsTable.getTableName() + ")";
        final int columnNameIndex = 0;
        when(mSQLiteDatabase.rawQuery(pragmaTableInfo, null)).thenReturn(cursor);
        when(cursor.moveToFirst()).thenReturn(true);
        when(cursor.getColumnIndex("name")).thenReturn(columnNameIndex);
        when(cursor.getString(columnNameIndex)).thenReturn("id", "method", "drive_sync_id", "drive_is_synced", "drive_marked_for_deletion", "last_local_modification_time", "custom_order_id");
        when(cursor.moveToNext()).thenReturn(true, true, true, true, true, true, false);

        mPaymentMethodsTable.onUpgrade(mSQLiteDatabase, oldVersion, newVersion, customizer);
        verify(mSQLiteDatabase, never()).execSQL(mSqlCaptor.capture());
        verify(customizer, never()).insertPaymentMethodDefaults(mPaymentMethodsTable);
    }

    @Test
    public void onUpgradeFromV16WhenCustomOrderIdColumnIsMissing() {
        final int oldVersion = 16;
        final int newVersion = DatabaseHelper.DATABASE_VERSION;

        final TableDefaultsCustomizer customizer = mock(TableDefaultsCustomizer.class);
        final Cursor cursor = mock(Cursor.class);
        final String pragmaTableInfo = "PRAGMA table_info(" + mPaymentMethodsTable.getTableName() + ")";
        final int columnNameIndex = 0;
        when(mSQLiteDatabase.rawQuery(pragmaTableInfo, null)).thenReturn(cursor);
        when(cursor.moveToFirst()).thenReturn(true);
        when(cursor.getColumnIndex("name")).thenReturn(columnNameIndex);
        when(cursor.getString(columnNameIndex)).thenReturn("id", "method", "drive_sync_id", "drive_is_synced", "drive_marked_for_deletion", "last_local_modification_time");
        when(cursor.moveToNext()).thenReturn(true, true, true, true, true, false);

        mPaymentMethodsTable.onUpgrade(mSQLiteDatabase, oldVersion, newVersion, customizer);
        verify(mSQLiteDatabase, atLeastOnce()).execSQL(mSqlCaptor.capture());
        verify(customizer, never()).insertPaymentMethodDefaults(mPaymentMethodsTable);

        assertEquals(mSqlCaptor.getAllValues().get(0), "ALTER TABLE " + mPaymentMethodsTable.getTableName() + " ADD COLUMN custom_order_id INTEGER DEFAULT 0");
        assertEquals(mSqlCaptor.getAllValues().get(1), "UPDATE " + mPaymentMethodsTable.getTableName() + " SET custom_order_id = ROWID");
    }

    @Test
    public void onUpgradeAlreadyOccurred() {
        final int oldVersion = DatabaseHelper.DATABASE_VERSION;
        final int newVersion = DatabaseHelper.DATABASE_VERSION;

        final TableDefaultsCustomizer customizer = mock(TableDefaultsCustomizer.class);
        mPaymentMethodsTable.onUpgrade(mSQLiteDatabase, oldVersion, newVersion, customizer);
        verify(mSQLiteDatabase, never()).execSQL(mSqlCaptor.capture());
        verify(customizer, never()).insertPaymentMethodDefaults(mPaymentMethodsTable);
    }

    @Test
    public void get() {
        final List<PaymentMethod> paymentMethods = mPaymentMethodsTable.get().blockingGet();
        assertEquals(paymentMethods, Arrays.asList(mPaymentMethod1, mPaymentMethod2));
    }

    @Test
    public void insert() {
        final PaymentMethod paymentMethod = mPaymentMethodsTable.insert(new PaymentMethodBuilderFactory().setMethod(METHOD3).build(), new DatabaseOperationMetadata()).blockingGet();
        assertNotNull(paymentMethod);
        assertEquals(METHOD3, paymentMethod.getMethod());

        final List<PaymentMethod> paymentMethods = mPaymentMethodsTable.get().blockingGet();
        assertEquals(paymentMethods, Arrays.asList(mPaymentMethod1, mPaymentMethod2, paymentMethod));

        assertEquals(0, paymentMethod.getCustomOrderId());
    }

    @Test
    public void findByPrimaryKey() {
        mPaymentMethodsTable.findByPrimaryKey(mPaymentMethod1.getId())
                .test()
                .assertNoErrors()
                .assertResult(mPaymentMethod1);
    }

    @Test
    public void findByPrimaryMissingKey() {
        mPaymentMethodsTable.findByPrimaryKey(-1)
                .test()
                .assertError(Exception.class);
    }

    @Test
    public void update() {
        final PaymentMethod updatedPaymentMethod = mPaymentMethodsTable.update(mPaymentMethod1, new PaymentMethodBuilderFactory().setMethod(METHOD3).build(), new DatabaseOperationMetadata()).blockingGet();
        assertNotNull(updatedPaymentMethod);
        assertEquals(METHOD3, updatedPaymentMethod.getMethod());
        assertFalse(mPaymentMethod1.equals(updatedPaymentMethod));

        final List<PaymentMethod> paymentMethods = mPaymentMethodsTable.get().blockingGet();
        assertEquals(paymentMethods, Arrays.asList(updatedPaymentMethod, mPaymentMethod2));
    }

    @Test
    public void delete() {
        assertEquals(mPaymentMethod1, mPaymentMethodsTable.delete(mPaymentMethod1, new DatabaseOperationMetadata()).blockingGet());

        final List<PaymentMethod> paymentMethods = mPaymentMethodsTable.get().blockingGet();
        assertEquals(paymentMethods, Collections.singletonList(mPaymentMethod2));
    }

}