package co.smartreceipts.android.settings.widget;

import co.smartreceipts.android.R;

public class PreferenceHeaderDistanceFragment extends
        AbstractPreferenceHeaderFragment {

    @Override
    public int getPreferencesResourceId() {
        return R.xml.preferences_distance;
    }

    @Override
    public void configurePreferences() {
        mSettingsActivity.configurePreferencesDistance(this);
    }
}
