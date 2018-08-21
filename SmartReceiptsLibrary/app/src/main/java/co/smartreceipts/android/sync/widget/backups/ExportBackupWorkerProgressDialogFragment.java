package co.smartreceipts.android.sync.widget.backups;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.widget.Toast;

import javax.inject.Inject;

import co.smartreceipts.android.R;
import co.smartreceipts.android.analytics.Analytics;
import co.smartreceipts.android.analytics.events.ErrorEvent;
import co.smartreceipts.android.persistence.PersistenceManager;
import co.smartreceipts.android.sync.manual.ManualBackupTask;
import co.smartreceipts.android.utils.IntentUtils;
import co.smartreceipts.android.widget.tooltip.report.backup.data.BackupReminderTooltipStorage;
import dagger.android.support.AndroidSupportInjection;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;


public class ExportBackupWorkerProgressDialogFragment extends DialogFragment {

    @Inject
    PersistenceManager persistenceManager;

    @Inject
    Analytics analytics;

    @Inject
    BackupReminderTooltipStorage backupReminderTooltipStorage;

    @Inject
    ManualBackupTask manualBackupTask;

    private Disposable disposable;

    @Override
    public void onAttach(Context context) {
        AndroidSupportInjection.inject(this);
        super.onAttach(context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        ProgressDialog dialog = new ProgressDialog(getActivity(), getTheme());
        dialog.setMessage(getString(R.string.dialog_export_working));
        dialog.setIndeterminate(true);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        return dialog;
    }

    @Override
    public void onResume() {
        super.onResume();
        disposable = manualBackupTask.backupData().observeOn(AndroidSchedulers.mainThread())
                .subscribe(file -> {
                    final Intent intent = IntentUtils.getSendIntent(getContext(), file);
                    getActivity().startActivity(Intent.createChooser(intent, getString(R.string.export)));
                    backupReminderTooltipStorage.setLastManualBackupDate();
                    manualBackupTask.markBackupAsComplete();
                }, throwable -> {
                    analytics.record(new ErrorEvent(ExportBackupWorkerProgressDialogFragment.this, throwable));
                    Toast.makeText(getContext(), getString(R.string.EXPORT_ERROR), Toast.LENGTH_LONG).show();
                    dismiss();
                }, this::dismiss);
    }

    @Override
    public void onPause() {
        disposable.dispose();
        super.onPause();
    }
}
