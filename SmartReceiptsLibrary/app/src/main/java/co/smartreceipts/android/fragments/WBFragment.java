package co.smartreceipts.android.fragments;

import android.app.Application;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import co.smartreceipts.android.SmartReceiptsApplication;
import wb.android.flex.Flex;

public class WBFragment extends Fragment {

	private SmartReceiptsApplication mApplication;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mApplication = getSmartReceiptsApplication();
	}

    @Override
	public void onStart() {
		super.onStart();
	}

    @Override
	public void onDetach() {
		super.onDetach();
		mApplication = null;
	}

	protected String getFlexString(Flex flex, int id) {
		if (isAdded()) {
			return flex.getString(getActivity(), id);
		}
		else {
			return "";
		}
	}

    public final void setSupportActionBar(@Nullable Toolbar toolbar) {
        ((AppCompatActivity)getActivity()).setSupportActionBar(toolbar);
    }

    @Nullable
	public final ActionBar getSupportActionBar() {
        if (getActivity() != null) {
            return ((AppCompatActivity) getActivity()).getSupportActionBar();
        } else {
            return null;
        }
	}

	public SmartReceiptsApplication getSmartReceiptsApplication() {
		if (mApplication == null) {
			if (getActivity() != null) {
				final Application application = getActivity().getApplication();
				if (application instanceof SmartReceiptsApplication) {
					mApplication = (SmartReceiptsApplication) application;
				}
				else {
					throw new RuntimeException("The Application must be an instance a SmartReceiptsApplication");
				}
			}
		}
		return mApplication;
	}

}