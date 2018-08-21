package wb.android.storage;

import android.content.Context;
import android.support.annotation.NonNull;

import java.io.File;

public class InternalDirectoryRoot implements DirectoryRoot {

    private final Context context;
    private File file;

    public InternalDirectoryRoot(@NonNull Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public File get() {
        if (file == null) { // No need to worry about double inits as we'll always get the same result
            file = context.getFilesDir();
        }
        return this.file;
    }
}
