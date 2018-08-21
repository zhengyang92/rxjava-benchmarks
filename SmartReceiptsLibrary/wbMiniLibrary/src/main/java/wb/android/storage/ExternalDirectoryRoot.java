package wb.android.storage;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.util.Preconditions;

import java.io.File;

public class ExternalDirectoryRoot implements DirectoryRoot {

    private final Context context;
    private File file;

    public ExternalDirectoryRoot(@NonNull Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public File get() {
        if (file == null) { // No need to worry about double inits as we'll always get the same result
            file = context.getExternalFilesDir(null);
        }
        if (file == null) {
            throw new IllegalStateException("Cannot operate with a null file");
        }
        //noinspection ConstantConditions
        return this.file;
    }
}
