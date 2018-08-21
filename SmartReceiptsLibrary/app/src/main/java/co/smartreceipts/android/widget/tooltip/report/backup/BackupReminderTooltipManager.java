package co.smartreceipts.android.widget.tooltip.report.backup;

import android.support.annotation.NonNull;

import com.google.common.base.Preconditions;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import co.smartreceipts.android.di.scopes.ApplicationScope;
import co.smartreceipts.android.sync.BackupProvidersManager;
import co.smartreceipts.android.sync.provider.SyncProvider;
import co.smartreceipts.android.widget.tooltip.TooltipManager;
import co.smartreceipts.android.widget.tooltip.report.backup.data.BackupReminderTooltipStorage;
import io.reactivex.Maybe;
import io.reactivex.schedulers.Schedulers;

@ApplicationScope
public class BackupReminderTooltipManager implements TooltipManager {

    private static final int DAYS_WITHOUT_BACKUP_LIMIT = 10;
    private static final int NEW_RECEIPTS_LIMIT = 15;
    private static final int NO_PREVIOUS_BACKUPS_DAY = -1;

    private final BackupProvidersManager backupProvidersManager;
    private final BackupReminderTooltipStorage backupReminderTooltipStorage;

    @Inject
    public BackupReminderTooltipManager(@NonNull BackupProvidersManager backupProvidersManager,
                                        @NonNull BackupReminderTooltipStorage backupReminderTooltipStorage) {
        this.backupProvidersManager = Preconditions.checkNotNull(backupProvidersManager);
        this.backupReminderTooltipStorage = Preconditions.checkNotNull(backupReminderTooltipStorage);
    }

    public Maybe<Integer> needToShowBackupReminder() {
        return Maybe.fromCallable(() -> {
                    int prolongationsCount = backupReminderTooltipStorage.getProlongationsCount();
                    int receiptsLimit = NEW_RECEIPTS_LIMIT + NEW_RECEIPTS_LIMIT * prolongationsCount;
                    int daysLimit = DAYS_WITHOUT_BACKUP_LIMIT + DAYS_WITHOUT_BACKUP_LIMIT * prolongationsCount;

                    if (backupProvidersManager.getSyncProvider() == SyncProvider.None && // disabled auto backups
                            backupReminderTooltipStorage.getReceiptsCountWithoutBackup() >= receiptsLimit) { // and user has a lot of new receipts since last backup

                        long lastManualBackupTime = backupReminderTooltipStorage.getLastManualBackupDate().getTime();

                        if (lastManualBackupTime == 0) { // if we didn't track any manual backup yet
                            return NO_PREVIOUS_BACKUPS_DAY;
                        } else {
                            int daysSinceLastManualBackup = (int) TimeUnit.MILLISECONDS.toDays(Math.abs(lastManualBackupTime - System.currentTimeMillis()));
                            return daysSinceLastManualBackup >= daysLimit ? daysSinceLastManualBackup : null;
                        }
                    } else {
                        return null;
                    }
                })
                .subscribeOn(Schedulers.io());
    }

    @Override
    public void tooltipWasDismissed() {
        backupReminderTooltipStorage.prolongReminder();
    }
}
