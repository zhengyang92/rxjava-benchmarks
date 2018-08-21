package co.smartreceipts.android.sync.widget.backups;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.CheckBox;

import com.google.common.base.Preconditions;

import javax.inject.Inject;

import co.smartreceipts.android.R;
import co.smartreceipts.android.activities.NavigationHandler;
import dagger.android.support.AndroidSupportInjection;

public class ImportLocalBackupDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {

    private static final String ARG_SMR_URI = "arg_smr_uri";

    @Inject
    NavigationHandler navigationHandler;

    private Uri mUri;
    private CheckBox mOverwriteCheckBox;

    public static ImportLocalBackupDialogFragment newInstance(@NonNull Uri uri) {
        final ImportLocalBackupDialogFragment fragment = new ImportLocalBackupDialogFragment();
        final Bundle args = new Bundle();
        args.putParcelable(ARG_SMR_URI, uri);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        AndroidSupportInjection.inject(this);
        super.onAttach(context);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUri = getArguments().getParcelable(ARG_SMR_URI);
        Preconditions.checkNotNull(mUri, "ImportBackupDialogFragment requires a valid SMR Uri");
    }

    @SuppressLint("InflateParams")
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View dialogView = getActivity().getLayoutInflater().inflate(R.layout.dialog_import_backup, null);
        mOverwriteCheckBox = (CheckBox) dialogView.findViewById(R.id.dialog_import_overwrite);

        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(dialogView);
        builder.setTitle(R.string.import_string);
        builder.setCancelable(true);
        builder.setPositiveButton(R.string.dialog_import_positive, this);
        builder.setNegativeButton(android.R.string.cancel, this);
        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            navigationHandler.showDialog(ImportLocalBackupWorkerProgressDialogFragment.newInstance(mUri, mOverwriteCheckBox.isChecked()));
        }
        dismiss();
    }
}
