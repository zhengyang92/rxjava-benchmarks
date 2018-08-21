package co.smartreceipts.android.workers.reports.pdf;

import android.support.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import co.smartreceipts.android.model.Trip;
import co.smartreceipts.android.persistence.DatabaseHelper;
import co.smartreceipts.android.persistence.PersistenceManager;
import co.smartreceipts.android.settings.UserPreferenceManager;
import co.smartreceipts.android.utils.log.Logger;
import co.smartreceipts.android.workers.reports.AbstractReport;
import co.smartreceipts.android.workers.reports.ReportGenerationException;
import co.smartreceipts.android.workers.reports.ReportResourcesManager;
import co.smartreceipts.android.workers.reports.pdf.pdfbox.PdfBoxReportFile;
import wb.android.storage.StorageManager;

public abstract class PdfBoxAbstractReport extends AbstractReport {

    public PdfBoxAbstractReport(@NonNull ReportResourcesManager reportResourcesManager,
                                @NonNull PersistenceManager persistenceManager) {
        super(reportResourcesManager, persistenceManager);
    }

    public PdfBoxAbstractReport(@NonNull ReportResourcesManager reportResourcesManager,
                                @NonNull DatabaseHelper db,
                                @NonNull UserPreferenceManager preferences,
                                @NonNull StorageManager storageManager) {
        super(reportResourcesManager, db, preferences, storageManager);
    }

    @NonNull
    @Override
    public File generate(@NonNull Trip trip) throws ReportGenerationException {
        final String outputFileName = getFileName(trip);
        FileOutputStream pdfStream = null;

        try {
            getStorageManager().delete(trip.getDirectory(), outputFileName);

            pdfStream = getStorageManager().getFOS(trip.getDirectory(), outputFileName);

            PdfBoxReportFile pdfBoxReportFile = new PdfBoxReportFile(getReportResourcesManager(), getPreferences());

            createSections(trip, pdfBoxReportFile);


            pdfBoxReportFile.writeFile(pdfStream, trip);

            return getStorageManager().getFile(trip.getDirectory(), outputFileName);

        } catch (IOException e) {
            Logger.error(this, e);
            throw new ReportGenerationException(e);
        } finally {
            if (pdfStream != null) {
                StorageManager.closeQuietly(pdfStream);
            }
        }

    }

    public abstract void createSections(@NonNull Trip trip, PdfBoxReportFile pdfBoxReportFile);

    protected String getFileName(Trip trip) {
        return trip.getDirectory().getName() + ".pdf";
    }
}
