package co.smartreceipts.android.persistence.database.tables.adapters;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;

import com.google.common.base.Preconditions;

import co.smartreceipts.android.model.Category;
import co.smartreceipts.android.model.factory.CategoryBuilderFactory;
import co.smartreceipts.android.persistence.database.operations.DatabaseOperationMetadata;
import co.smartreceipts.android.persistence.database.operations.OperationFamilyType;
import co.smartreceipts.android.persistence.database.tables.CategoriesTable;
import co.smartreceipts.android.persistence.database.tables.keys.PrimaryKey;
import co.smartreceipts.android.sync.model.SyncState;

import static co.smartreceipts.android.persistence.database.tables.AbstractSqlTable.COLUMN_CUSTOM_ORDER_ID;

/**
 * Implements the {@link DatabaseAdapter} contract for the {@link co.smartreceipts.android.persistence.database.tables.CategoriesTable}
 */
public final class CategoryDatabaseAdapter implements DatabaseAdapter<Category, PrimaryKey<Category, Integer>> {

    private final SyncStateAdapter mSyncStateAdapter;

    public CategoryDatabaseAdapter() {
        this(new SyncStateAdapter());
    }

    public CategoryDatabaseAdapter(@NonNull SyncStateAdapter syncStateAdapter) {
        mSyncStateAdapter = Preconditions.checkNotNull(syncStateAdapter);
    }

    @Override
    @NonNull
    public Category read(@NonNull Cursor cursor) {
        final int idIndex = cursor.getColumnIndex(CategoriesTable.COLUMN_ID);
        final int nameIndex = cursor.getColumnIndex(CategoriesTable.COLUMN_NAME);
        final int codeIndex = cursor.getColumnIndex(CategoriesTable.COLUMN_CODE);
        final int customOrderIdIndex = cursor.getColumnIndex(COLUMN_CUSTOM_ORDER_ID);

        final int id = cursor.getInt(idIndex);
        final String name = cursor.getString(nameIndex);
        final String code = cursor.getString(codeIndex);
        final SyncState syncState = mSyncStateAdapter.read(cursor);
        final long orderId = cursor.getLong(customOrderIdIndex);
        return new CategoryBuilderFactory().setId(id).setName(name).setCode(code).setSyncState(syncState).setCustomOrderId(orderId).build();
    }

    @Override
    @NonNull
    public ContentValues write(@NonNull Category category, @NonNull DatabaseOperationMetadata databaseOperationMetadata) {
        final ContentValues values = new ContentValues();
        values.put(CategoriesTable.COLUMN_NAME, category.getName());
        values.put(CategoriesTable.COLUMN_CODE, category.getCode());
        values.put(CategoriesTable.COLUMN_CUSTOM_ORDER_ID, category.getCustomOrderId());
        if (databaseOperationMetadata.getOperationFamilyType() == OperationFamilyType.Sync) {
            values.putAll(mSyncStateAdapter.write(category.getSyncState()));
        } else {
            values.putAll(mSyncStateAdapter.writeUnsynced(category.getSyncState()));
        }
        return values;
    }

    @Override
    @NonNull
    public Category build(@NonNull Category category, @NonNull PrimaryKey<Category, Integer> primaryKey, @NonNull DatabaseOperationMetadata databaseOperationMetadata) {
        Integer id = primaryKey.getPrimaryKeyValue(category);
        return new CategoryBuilderFactory()
                .setId(id)
                .setName(category.getName())
                .setCode(category.getCode())
                .setSyncState(mSyncStateAdapter.get(category.getSyncState(), databaseOperationMetadata))
                .setCustomOrderId(category.getCustomOrderId())
                .build();
    }

}
