package co.smartreceipts.android.imports;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;

import com.google.common.base.Preconditions;

import java.io.File;
import java.lang.ref.WeakReference;

import co.smartreceipts.android.model.Receipt;
import co.smartreceipts.android.utils.IntentUtils;
import co.smartreceipts.android.utils.cache.SmartReceiptsTemporaryFileCache;
import co.smartreceipts.android.utils.log.Logger;

public class CameraInteractionController {

    private final Context context;
    private final WeakReference<Fragment> fragmentReference;

    public CameraInteractionController(@NonNull Fragment fragment) {
        context = Preconditions.checkNotNull(fragment.getContext()).getApplicationContext();
        fragmentReference = new WeakReference<>(Preconditions.checkNotNull(fragment));
    }

    /**
     * Takes a photo for a given trip directory
     *
     * @return the Uri result of the photo
     */
    @NonNull
    public Uri takePhoto() {
        return startPhotoIntent(new SmartReceiptsTemporaryFileCache(context).getInternalCacheFile(System.currentTimeMillis() + "x.jpg"), RequestCodes.NATIVE_NEW_RECEIPT_CAMERA_REQUEST);
    }

    /**
     * Takes a photo for a given receipt
     *
     * @return the Uri result of the photo
     */
    @NonNull
    public Uri addPhoto() {
        return startPhotoIntent(new SmartReceiptsTemporaryFileCache(context).getInternalCacheFile(System.currentTimeMillis() + "x.jpg"), RequestCodes.NATIVE_ADD_PHOTO_CAMERA_REQUEST);
    }

    /**
     * Retakes a photo for a given receipt
     *
     * @param receipt the desired {@link Receipt}
     * @return the Uri result of the photo
     */
    @NonNull
    public Uri retakePhoto(@NonNull Receipt receipt) {
        Preconditions.checkNotNull(receipt.getFile());
        return startPhotoIntent(receipt.getFile(), RequestCodes.NATIVE_RETAKE_PHOTO_CAMERA_REQUEST);
    }

    @NonNull
    private Uri startPhotoIntent(@NonNull File saveLocation, int nativeCameraRequestCode) {
        final Fragment fragment = fragmentReference.get();
        if (fragment == null || !fragment.isResumed()) {
            Logger.warn(this, "Returning empty URI as save location");
            return Uri.EMPTY;
        }

        final Intent intent = IntentUtils.getImageCaptureIntent(fragment.getActivity(), saveLocation);
        fragment.startActivityForResult(intent, nativeCameraRequestCode);
        final Uri uri = intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT);
        Logger.debug(this, "Returning {} as save location", uri);
        return uri;
    }
}
