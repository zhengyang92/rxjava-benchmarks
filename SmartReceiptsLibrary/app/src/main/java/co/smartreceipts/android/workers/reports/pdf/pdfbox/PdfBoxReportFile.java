package co.smartreceipts.android.workers.reports.pdf.pdfbox;

import android.support.annotation.NonNull;

import com.google.common.base.Preconditions;
import com.tom_roush.pdfbox.pdmodel.PDDocument;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import co.smartreceipts.android.model.Column;
import co.smartreceipts.android.model.Distance;
import co.smartreceipts.android.model.Receipt;
import co.smartreceipts.android.model.Trip;
import co.smartreceipts.android.persistence.database.controllers.grouping.results.CategoryGroupingResult;
import co.smartreceipts.android.persistence.database.controllers.grouping.results.SumCategoryGroupingResult;
import co.smartreceipts.android.purchases.wallet.PurchaseWallet;
import co.smartreceipts.android.settings.UserPreferenceManager;
import co.smartreceipts.android.utils.log.Logger;
import co.smartreceipts.android.workers.reports.ReportResourcesManager;
import co.smartreceipts.android.workers.reports.pdf.PdfReportFile;
import co.smartreceipts.android.workers.reports.pdf.colors.PdfColorManager;
import co.smartreceipts.android.workers.reports.pdf.fonts.PdfFontManager;

public class PdfBoxReportFile implements PdfReportFile, PdfBoxSectionFactory {

    private final DefaultPdfBoxContext pdfBoxContext;
    private final ReportResourcesManager reportResourcesManager;
    private final PDDocument pdDocument;
    private final List<PdfBoxSection> sections;


    public PdfBoxReportFile(@NonNull ReportResourcesManager reportResourcesManager,
                            @NonNull UserPreferenceManager preferences) throws IOException {

        this.reportResourcesManager = Preconditions.checkNotNull(reportResourcesManager);

        pdDocument = new PDDocument();
        sections = new ArrayList<>();

        final PdfColorManager colorManager = new PdfColorManager();
        final PdfFontManager fontManager = new PdfFontManager(reportResourcesManager.getLocalizedContext(), pdDocument);
        fontManager.initialize();

        pdfBoxContext = new DefaultPdfBoxContext(reportResourcesManager.getLocalizedContext(),
                fontManager, colorManager, preferences);
    }


    @Override
    public void writeFile(@NonNull OutputStream outStream, @NonNull Trip trip) throws IOException {
        try {
            final PdfBoxWriter writer = new PdfBoxWriter(pdDocument, pdfBoxContext, new DefaultPdfBoxPageDecorations(pdfBoxContext, trip));
            for (PdfBoxSection section : sections) {
                section.writeSection(pdDocument, writer);
            }
            writer.writeAndClose();

            pdDocument.save(outStream);
        } finally {
            try {
                pdDocument.close();
            } catch (IOException e) {
                Logger.error(this, e);
            }
        }
    }

    public void addSection(PdfBoxSection section) {
        sections.add(section);
    }

    @NonNull
    @Override
    public PdfBoxReceiptsTablePdfSection createReceiptsTableSection(
            @NonNull Trip trip, @NonNull List<Receipt> receipts, @NonNull List<Column<Receipt>> columns,
            @NonNull List<Distance> distances, @NonNull List<Column<Distance>> distanceColumns,
            @NonNull List<SumCategoryGroupingResult> categpries,
            @NonNull List<Column<SumCategoryGroupingResult>> categoryColumns,
            @NonNull List<CategoryGroupingResult> groupingResults,
            @NonNull PurchaseWallet purchaseWallet) {

        return new PdfBoxReceiptsTablePdfSection(pdfBoxContext, reportResourcesManager, trip,
                receipts, columns, distances, distanceColumns, categpries, categoryColumns,
                groupingResults, purchaseWallet);
    }


    @NonNull
    @Override
    public PdfBoxReceiptsImagesPdfSection createReceiptsImagesSection(@NonNull Trip trip, @NonNull List<Receipt> receipts) {
        return new PdfBoxReceiptsImagesPdfSection(pdfBoxContext, pdDocument, trip, receipts);
    }
}