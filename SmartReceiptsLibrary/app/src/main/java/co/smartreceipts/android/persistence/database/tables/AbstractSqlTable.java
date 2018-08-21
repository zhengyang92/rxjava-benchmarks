package co.smartreceipts.android.persistence.database.tables;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;

import com.google.common.base.Preconditions;
import com.hadisatrio.optional.Optional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import co.smartreceipts.android.persistence.database.defaults.TableDefaultsCustomizer;
import co.smartreceipts.android.persistence.database.operations.DatabaseOperationMetadata;
import co.smartreceipts.android.persistence.database.operations.OperationFamilyType;
import co.smartreceipts.android.persistence.database.tables.adapters.DatabaseAdapter;
import co.smartreceipts.android.persistence.database.tables.adapters.SyncStateAdapter;
import co.smartreceipts.android.persistence.database.tables.keys.AutoIncrementIdPrimaryKey;
import co.smartreceipts.android.persistence.database.tables.keys.PrimaryKey;
import co.smartreceipts.android.persistence.database.tables.ordering.OrderByDatabaseDefault;
import co.smartreceipts.android.persistence.database.tables.ordering.OrderBy;
import co.smartreceipts.android.sync.model.Syncable;
import co.smartreceipts.android.sync.provider.SyncProvider;
import co.smartreceipts.android.utils.log.Logger;
import io.reactivex.Single;

/**
 * Abstracts out the core CRUD database operations in order to ensure that each of our core table instances
 * operate in a standard manner.
 *
 * @param <ModelType>      the model object that CRUD operations here should return
 * @param <PrimaryKeyType> the primary key type (e.g. Integer, String) that is used by the primary key column
 */
public abstract class AbstractSqlTable<ModelType, PrimaryKeyType> implements Table<ModelType, PrimaryKeyType> {

    public static final String COLUMN_DRIVE_SYNC_ID = "drive_sync_id";
    public static final String COLUMN_DRIVE_IS_SYNCED = "drive_is_synced";
    public static final String COLUMN_DRIVE_MARKED_FOR_DELETION = "drive_marked_for_deletion";
    public static final String COLUMN_LAST_LOCAL_MODIFICATION_TIME = "last_local_modification_time";
    public static final String COLUMN_CUSTOM_ORDER_ID = "custom_order_id";

    private final SQLiteOpenHelper sqLiteOpenHelper;
    private final String tableName;

    protected final DatabaseAdapter<ModelType, PrimaryKey<ModelType, PrimaryKeyType>> databaseAdapter;
    protected final PrimaryKey<ModelType, PrimaryKeyType> primaryKey;
    private final OrderBy orderBy;

    private SQLiteDatabase initialNonRecursivelyCalledDatabase;
    private List<ModelType> cachedResults;


    public AbstractSqlTable(@NonNull SQLiteOpenHelper sqLiteOpenHelper,
                            @NonNull String tableName,
                            @NonNull DatabaseAdapter<ModelType, PrimaryKey<ModelType, PrimaryKeyType>> databaseAdapter,
                            @NonNull PrimaryKey<ModelType, PrimaryKeyType> primaryKey) {
        this(sqLiteOpenHelper, tableName, databaseAdapter, primaryKey, new OrderByDatabaseDefault());
    }

    public AbstractSqlTable(@NonNull SQLiteOpenHelper sqLiteOpenHelper, @NonNull String tableName,
                            @NonNull DatabaseAdapter<ModelType, PrimaryKey<ModelType, PrimaryKeyType>> databaseAdapter,
                            @NonNull PrimaryKey<ModelType, PrimaryKeyType> primaryKey, @NonNull OrderBy orderBy) {
        this.sqLiteOpenHelper = Preconditions.checkNotNull(sqLiteOpenHelper);
        this.tableName = Preconditions.checkNotNull(tableName);
        this.databaseAdapter = Preconditions.checkNotNull(databaseAdapter);
        this.primaryKey = Preconditions.checkNotNull(primaryKey);
        this.orderBy = Preconditions.checkNotNull(orderBy);
    }

    public final SQLiteDatabase getReadableDatabase() {
        if (initialNonRecursivelyCalledDatabase == null) {
            return sqLiteOpenHelper.getReadableDatabase();
        } else {
            return initialNonRecursivelyCalledDatabase;
        }
    }

    public final SQLiteDatabase getWritableDatabase() {
        if (initialNonRecursivelyCalledDatabase == null) {
            return sqLiteOpenHelper.getWritableDatabase();
        } else {
            return initialNonRecursivelyCalledDatabase;
        }
    }

    @Override
    @NonNull
    public final String getTableName() {
        return tableName;
    }

    @Override
    public synchronized void onCreate(@NonNull SQLiteDatabase db, @NonNull TableDefaultsCustomizer customizer) {
        initialNonRecursivelyCalledDatabase = db;
    }

    @Override
    public synchronized void onUpgrade(@NonNull SQLiteDatabase db, int oldVersion, int newVersion, @NonNull TableDefaultsCustomizer customizer) {
        initialNonRecursivelyCalledDatabase = db;
    }

    protected synchronized void onUpgradeToAddSyncInformation(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion <= 14) { // Add syncing state information
            final String alter1 = "ALTER TABLE " + getTableName() + " ADD " + COLUMN_DRIVE_SYNC_ID + " TEXT";
            final String alter2 = "ALTER TABLE " + getTableName() + " ADD " + COLUMN_DRIVE_IS_SYNCED + " BOOLEAN DEFAULT 0";
            final String alter3 = "ALTER TABLE " + getTableName() + " ADD " + COLUMN_DRIVE_MARKED_FOR_DELETION + " BOOLEAN DEFAULT 0";
            final String alter4 = "ALTER TABLE " + getTableName() + " ADD " + COLUMN_LAST_LOCAL_MODIFICATION_TIME + " DATE";

            db.execSQL(alter1);
            db.execSQL(alter2);
            db.execSQL(alter3);
            db.execSQL(alter4);
        }
    }

    @Override
    public synchronized final void onPostCreateUpgrade() {
        // We no longer need to worry about recursive database calls
        initialNonRecursivelyCalledDatabase = null;
    }

    @NonNull
    @Override
    public Single<List<ModelType>> get() {
        return Single.fromCallable(this::getBlocking);
    }

    /**
     * This method aims to fetch all entities that have been marked for deletion but are not yet delete
     *
     * @param syncProvider the provided {@link SyncProvider} to check for
     * @return a {@link Single} containing a {@link List} all items assigned for deletion
     */
    @NonNull
    public synchronized Single<List<ModelType>> getAllMarkedForDeletionItems(@NonNull final SyncProvider syncProvider) {
        Preconditions.checkArgument(syncProvider == SyncProvider.GoogleDrive, "Google Drive is the only supported provider at the moment");

        return Single.fromCallable(() -> {
                    Cursor cursor = null;
                    try {
                        final List<ModelType> results = new ArrayList<>();
                        cursor = getReadableDatabase().query(getTableName(), null, COLUMN_DRIVE_MARKED_FOR_DELETION + " = ?", new String[]{Integer.toString(1)}, null, null, null);
                        if (cursor != null && cursor.moveToFirst()) {
                            do {
                                results.add(databaseAdapter.read(cursor));
                            }
                            while (cursor.moveToNext());
                        }
                        return results;
                    } finally {
                        if (cursor != null) {
                            cursor.close();
                        }
                    }
                });
    }

    @NonNull
    @Override
    public Single<ModelType> findByPrimaryKey(@NonNull final PrimaryKeyType primaryKeyType) {
        return Single.fromCallable(() -> AbstractSqlTable.this.findByPrimaryKeyBlocking(primaryKeyType))
                .map(modelTypeOptional -> {
                    if (modelTypeOptional.isPresent()) {
                        return modelTypeOptional.get();
                    } else {
                        throw new Exception("Find by primary key failed. No such key");
                    }
                });
    }

    @NonNull
    @Override
    public final Single<ModelType> insert(@NonNull final ModelType modelType, @NonNull final DatabaseOperationMetadata databaseOperationMetadata) {
        return Single.fromCallable(() -> AbstractSqlTable.this.insertBlocking(modelType, databaseOperationMetadata))
                .map(modelTypeOptional -> {
                    if (modelTypeOptional.isPresent()) {
                        return modelTypeOptional.get();
                    } else {
                        throw new Exception("Insert failed.");
                    }
                });
    }

    @NonNull
    @Override
    public final Single<ModelType> update(@NonNull final ModelType oldModelType, @NonNull final ModelType newModelType, @NonNull final DatabaseOperationMetadata databaseOperationMetadata) {
        return Single.fromCallable(() -> AbstractSqlTable.this.updateBlocking(oldModelType, newModelType, databaseOperationMetadata))
                .map(modelTypeOptional -> {
                    if (modelTypeOptional.isPresent()) {
                        return modelTypeOptional.get();
                    } else {
                        throw new Exception("Update failed.");
                    }
                });
    }

    @NonNull
    @Override
    public final Single<ModelType> delete(@NonNull final ModelType modelType, @NonNull final DatabaseOperationMetadata databaseOperationMetadata) {
        return Single.fromCallable(() -> AbstractSqlTable.this.deleteBlocking(modelType, databaseOperationMetadata))
                .map(modelTypeOptional -> {
                    if (modelTypeOptional.isPresent()) {
                        return modelTypeOptional.get();
                    } else {
                        throw new Exception("Delete failed.");
                    }
                });
    }

    @NonNull
    public Single<Boolean> deleteSyncData(@NonNull final SyncProvider syncProvider) {
        return Single.fromCallable(() -> AbstractSqlTable.this.deleteSyncDataBlocking(syncProvider));
    }

    @NonNull
    public synchronized List<ModelType> getBlocking() {
        if (cachedResults != null) {
            return cachedResults;
        }

        Cursor cursor = null;
        try {
            cachedResults = new ArrayList<>();
            cursor = getReadableDatabase().query(getTableName(), null, COLUMN_DRIVE_MARKED_FOR_DELETION + " = ?", new String[]{Integer.toString(0)}, null, null, orderBy.getOrderByPredicate());
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    cachedResults.add(databaseAdapter.read(cursor));
                }
                while (cursor.moveToNext());
            }
            return new ArrayList<>(cachedResults);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @SuppressWarnings("unchecked")
    @NonNull
    public synchronized Optional<ModelType> insertBlocking(@NonNull ModelType modelType, @NonNull DatabaseOperationMetadata databaseOperationMetadata) {
        final ContentValues values = databaseAdapter.write(modelType, databaseOperationMetadata);
        if (getWritableDatabase().insertOrThrow(getTableName(), null, values) != -1) {
            if (Integer.class.equals(primaryKey.getPrimaryKeyClass())) {
                Cursor cursor = null;
                try {
                    cursor = getReadableDatabase().rawQuery("SELECT last_insert_rowid()", null);

                    final Integer id;
                    if (cursor != null && cursor.moveToFirst() && cursor.getColumnCount() > 0) {
                        id = cursor.getInt(0);
                    } else {
                        id = -1;
                    }

                    // Note: We do some quick hacks around generics here to ensure the types are consistent
                    final PrimaryKey<ModelType, PrimaryKeyType> autoIncrementPrimaryKey = (PrimaryKey<ModelType, PrimaryKeyType>) new AutoIncrementIdPrimaryKey<>((PrimaryKey<ModelType, Integer>) primaryKey, id);

                    final ModelType insertedItem = databaseAdapter.build(modelType, autoIncrementPrimaryKey, databaseOperationMetadata);
                    if (cachedResults != null) {
                        cachedResults.add(insertedItem);
                        if (insertedItem instanceof Comparable<?>) {
                            Collections.sort((List<? extends Comparable>) cachedResults);
                        }
                    }
                    return Optional.of(insertedItem);
                } finally { // Close the cursor and db to avoid memory leaks
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            } else {
                // If it's not an auto-increment id, just grab whatever the definition is...
                final ModelType insertedItem = databaseAdapter.build(modelType, primaryKey, databaseOperationMetadata);
                if (cachedResults != null) {
                    cachedResults.add(insertedItem);
                    if (insertedItem instanceof Comparable<?>) {
                        Collections.sort((List<? extends Comparable>) cachedResults);
                    }
                }
                return Optional.of(insertedItem);
            }
        } else {
            return Optional.absent();
        }
    }

    @SuppressWarnings("unchecked")
    public synchronized Optional<ModelType> updateBlocking(@NonNull ModelType oldModelType, @NonNull ModelType newModelType, @NonNull DatabaseOperationMetadata databaseOperationMetadata) {
        final ContentValues values = databaseAdapter.write(newModelType, databaseOperationMetadata);
        final String oldPrimaryKeyValue = primaryKey.getPrimaryKeyValue(oldModelType).toString();

        final boolean updateSuccess;
        if (databaseOperationMetadata.getOperationFamilyType() == OperationFamilyType.Sync && oldModelType instanceof Syncable) {
            // For sync operations, ensure that this only succeeds if we haven't already updated this item more recently
            final Syncable syncableOldModel = (Syncable) oldModelType;
            updateSuccess = getWritableDatabase().update(getTableName(), values, primaryKey.getPrimaryKeyColumn() + " = ? AND " + AbstractSqlTable.COLUMN_LAST_LOCAL_MODIFICATION_TIME + " >= ?", new String[]{ oldPrimaryKeyValue, Long.toString(syncableOldModel.getSyncState().getLastLocalModificationTime().getTime()) }) > 0;
        } else {
            updateSuccess = getWritableDatabase().update(getTableName(), values, primaryKey.getPrimaryKeyColumn() + " = ?", new String[]{ oldPrimaryKeyValue }) > 0;
        }

        if (updateSuccess) {
            final ModelType updatedItem;
            if (Integer.class.equals(primaryKey.getPrimaryKeyClass())) {
                // If it's an auto-increment key, ensure we're re-using the same id as the old key
                final PrimaryKey<ModelType, PrimaryKeyType> autoIncrementPrimaryKey = (PrimaryKey<ModelType, PrimaryKeyType>) new AutoIncrementIdPrimaryKey<>((PrimaryKey<ModelType, Integer>) primaryKey, (Integer) primaryKey.getPrimaryKeyValue(oldModelType));
                updatedItem = databaseAdapter.build(newModelType, autoIncrementPrimaryKey, databaseOperationMetadata);
            } else {
                // Otherwise, we'll use whatever the user defined...
                updatedItem = databaseAdapter.build(newModelType, primaryKey, databaseOperationMetadata);
            }
            if (cachedResults != null) {
                boolean wasCachedResultRemoved = cachedResults.remove(oldModelType);
                if (!wasCachedResultRemoved) {
                    // If our cache is wrong, let's use the actual primary key to see if we can find it
                    final PrimaryKeyType primaryKeyValue = primaryKey.getPrimaryKeyValue(newModelType);
                    Logger.debug(this, "Failed to remove {} with primary key {} from our cache. Searching through to manually remove...", newModelType.getClass(), primaryKeyValue);
                    for (final ModelType cachedResult : cachedResults) {
                        if (primaryKeyValue.equals(primaryKey.getPrimaryKeyValue(cachedResult))) {
                            wasCachedResultRemoved = cachedResults.remove(cachedResult);
                            if (wasCachedResultRemoved) {
                                break;
                            }
                        }
                    }
                    if (!wasCachedResultRemoved) {
                        Logger.warn(this, "Primary key {} was never found in our cache.", primaryKeyValue);
                    }
                }
                if (newModelType instanceof Syncable) {
                    final Syncable syncable = (Syncable) newModelType;
                    if (!syncable.getSyncState().isMarkedForDeletion(SyncProvider.GoogleDrive)) {
                        cachedResults.add(updatedItem);
                    }
                } else {
                    cachedResults.add(updatedItem);
                }
                if (updatedItem instanceof Comparable<?>) {
                    Collections.sort((List<? extends Comparable>) cachedResults);
                }
            }
            return Optional.of(updatedItem);
        } else {
            return Optional.absent();
        }

    }

    public synchronized Optional<ModelType> deleteBlocking(@NonNull ModelType modelType, @NonNull DatabaseOperationMetadata databaseOperationMetadata) {
        final String primaryKeyValue = primaryKey.getPrimaryKeyValue(modelType).toString();
        if (getWritableDatabase().delete(getTableName(), primaryKey.getPrimaryKeyColumn() + " = ?", new String[]{primaryKeyValue}) > 0) {
            if (cachedResults != null) {
                cachedResults.remove(modelType);
            }
            return Optional.of(modelType);
        } else {
            return Optional.absent();
        }
    }

    public synchronized boolean deleteSyncDataBlocking(@NonNull SyncProvider syncProvider) {
        Preconditions.checkArgument(syncProvider == SyncProvider.GoogleDrive, "Google Drive is the only supported provider at the moment");

        // First - remove all that are marked for deletion but haven't been actually deleted
        getWritableDatabase().delete(getTableName(), COLUMN_DRIVE_MARKED_FOR_DELETION + " = ?", new String[]{Integer.toString(1)});

        // Next - update all items that currently contain sync data (to remove it)
        final ContentValues contentValues = new SyncStateAdapter().deleteSyncData(syncProvider);
        getWritableDatabase().update(getTableName(), contentValues, null, null);

        // Lastly - let's clear out all cached data
        if (cachedResults != null) {
            cachedResults.clear();
        }

        return true;
    }

    @NonNull
    public synchronized Optional<ModelType> findByPrimaryKeyBlocking(@NonNull PrimaryKeyType primaryKeyType) {
        if (cachedResults != null) {
            for (final ModelType cachedResult : cachedResults) {
                if (primaryKey.getPrimaryKeyValue(cachedResult).equals(primaryKeyType)) {
                    return Optional.of(cachedResult);
                }
            }
            return Optional.absent();
        } else {
            final List<ModelType> entries = new ArrayList<>(getBlocking());
            final int size = entries.size();
            for (int i = 0; i < size; i++) {
                final ModelType modelType = entries.get(i);
                if (primaryKey.getPrimaryKeyValue(modelType).equals(primaryKeyType)) {
                    return Optional.of(modelType);
                }
            }
            return Optional.absent();
        }
    }

    @Override
    public synchronized void deleteAllTableRowsBlockiing() {
        getWritableDatabase().execSQL("DELETE FROM " + getTableName());
        clearCache();
    }

    @Override
    public synchronized void clearCache() {
        if (cachedResults != null) {
            cachedResults.clear();
            cachedResults = null;
        }
    }

}
