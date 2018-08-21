package co.smartreceipts.android.workers.reports.pdf.renderer.grid;

import android.support.annotation.NonNull;

import com.google.common.base.Preconditions;

import org.apache.commons.io.IOUtils;

import java.io.IOException;

import co.smartreceipts.android.workers.reports.pdf.pdfbox.PdfBoxWriter;
import co.smartreceipts.android.workers.reports.pdf.renderer.constraints.HeightConstraint;
import co.smartreceipts.android.workers.reports.pdf.renderer.constraints.WidthConstraint;
import co.smartreceipts.android.workers.reports.pdf.renderer.imagex.PdfPDImageXFactory;

public class PdfGridRenderer extends GridRenderer {

    private final PdfPDImageXFactory factory;

    public PdfGridRenderer(@NonNull PdfPDImageXFactory factory, float width, float height) {
        this(factory, new WidthConstraint(width), new HeightConstraint(height));
    }

    public PdfGridRenderer(@NonNull PdfPDImageXFactory factory,
                           @NonNull WidthConstraint widthConstraint, @NonNull HeightConstraint heightConstraint) {
        super(widthConstraint, heightConstraint);
        this.factory = Preconditions.checkNotNull(factory);
    }

    @Override
    public void render(@NonNull PdfBoxWriter writer) throws IOException {
        try {
            factory.open();
            while (factory.nextPage()) {
                writer.newPage();
                super.render(writer);
            }
        } finally {
            IOUtils.closeQuietly(factory);
        }
    }
}
