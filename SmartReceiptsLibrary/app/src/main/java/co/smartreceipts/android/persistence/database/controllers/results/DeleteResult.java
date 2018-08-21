package co.smartreceipts.android.persistence.database.controllers.results;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.base.Preconditions;

import co.smartreceipts.android.persistence.database.operations.DatabaseOperationMetadata;

public class DeleteResult<ModelType> {

    private final ModelType model;
    private final Throwable throwable;
    private final DatabaseOperationMetadata databaseOperationMetadata;

    public DeleteResult(@NonNull ModelType model, @NonNull DatabaseOperationMetadata databaseOperationMetadata) {
        this(model, null, databaseOperationMetadata);
    }

    public DeleteResult(@NonNull ModelType model, @Nullable Throwable throwable, @NonNull DatabaseOperationMetadata databaseOperationMetadata) {
        this.model = Preconditions.checkNotNull(model);
        this.throwable = throwable;
        this.databaseOperationMetadata = Preconditions.checkNotNull(databaseOperationMetadata);
    }

    @NonNull
    public ModelType get() {
        return model;
    }

    @Nullable
    public Throwable getThrowable() {
        return throwable;
    }

    @NonNull
    public DatabaseOperationMetadata getDatabaseOperationMetadata() {
        return databaseOperationMetadata;
    }
}
