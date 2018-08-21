package co.smartreceipts.android.sync.drive.services;

import android.support.annotation.NonNull;

import com.google.android.gms.drive.DriveId;

public interface DriveIdUploadCompleteCallback {

    void onSuccess(@NonNull DriveId driveId);

    void onFailure(@NonNull DriveId driveId);
}
