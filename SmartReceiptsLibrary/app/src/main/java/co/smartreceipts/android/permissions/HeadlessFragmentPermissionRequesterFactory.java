package co.smartreceipts.android.permissions;

import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

import com.google.common.base.Preconditions;

public class HeadlessFragmentPermissionRequesterFactory {

    private final FragmentManager fragmentManager;

    public HeadlessFragmentPermissionRequesterFactory(@NonNull FragmentActivity activity) {
        this.fragmentManager = Preconditions.checkNotNull(activity.getSupportFragmentManager());
    }

    @NonNull
    public PermissionRequesterHeadlessFragment get() {
        final String tag = PermissionRequesterHeadlessFragment.class.getName();
        PermissionRequesterHeadlessFragment fragment = (PermissionRequesterHeadlessFragment) this.fragmentManager.findFragmentByTag(tag);
        if (fragment == null) {
            fragment = new PermissionRequesterHeadlessFragment();
            this.fragmentManager.beginTransaction().add(fragment, tag).commitNow();
        }
        return fragment;
    }
}
