package co.smartreceipts.android.permissions;

import android.support.annotation.NonNull;

import javax.inject.Inject;

import co.smartreceipts.android.activities.SmartReceiptsActivity;
import co.smartreceipts.android.di.scopes.ActivityScope;
import co.smartreceipts.android.permissions.exceptions.PermissionsNotGrantedException;
import io.reactivex.Completable;

@ActivityScope
public class PermissionsDelegate {

    @Inject
    PermissionStatusChecker permissionStatusChecker;
    @Inject
    ActivityPermissionsRequester<SmartReceiptsActivity> permissionRequester;

    @Inject
    public PermissionsDelegate() {
    }

    public Completable checkPermissionAndMaybeAsk(@NonNull String manifestPermission) {
        return permissionStatusChecker.isPermissionGranted(manifestPermission)
                .flatMapCompletable(isGranted -> {
                    if (isGranted) {
                        return Completable.complete();
                    } else {
                        return permissionRequester.request(manifestPermission)
                                .flatMapCompletable(permissionResponse -> {
                                    if (permissionResponse.wasGranted()) {
                                        return Completable.complete();
                                    } else {
                                        return Completable.error(new PermissionsNotGrantedException("User failed to grant permission", manifestPermission));
                                    }
                                });
                    }
                });
    }

    public void markRequestConsumed(@NonNull String manifestPermission) {
        permissionRequester.markRequestConsumed(manifestPermission);
    }
}
