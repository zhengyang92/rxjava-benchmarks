package co.smartreceipts.android.settings.widget;

import co.smartreceipts.android.R;

public class PreferenceHeaderHelpFragment extends
        AbstractPreferenceHeaderFragment {

    @Override
    public int getPreferencesResourceId() {
        return R.xml.preferences_help;
    }

    @Override
    public void configurePreferences() {
        mSettingsActivity.configurePreferencesHelp(this);
    }
}
