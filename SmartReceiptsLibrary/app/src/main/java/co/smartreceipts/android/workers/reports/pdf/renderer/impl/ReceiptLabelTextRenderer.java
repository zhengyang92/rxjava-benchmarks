package co.smartreceipts.android.workers.reports.pdf.renderer.impl;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import com.google.common.base.Preconditions;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.util.awt.AWTColor;

import co.smartreceipts.android.model.Receipt;
import co.smartreceipts.android.settings.UserPreferenceManager;
import co.smartreceipts.android.settings.catalog.UserPreference;
import co.smartreceipts.android.workers.reports.pdf.fonts.PdfFontSpec;
import co.smartreceipts.android.workers.reports.pdf.renderer.text.TextRenderer;

public class ReceiptLabelTextRenderer extends TextRenderer {


    public ReceiptLabelTextRenderer(@NonNull Receipt receipt, @NonNull Context context, @NonNull PDDocument pdDocument,
                                    @NonNull UserPreferenceManager userPreferenceManager, @NonNull AWTColor color, @NonNull PdfFontSpec fontSpec) {
        super(context, pdDocument, new TextFormatter(context, userPreferenceManager).buildLegendForImage(receipt), color, fontSpec);
    }

    @VisibleForTesting
    static class TextFormatter {

        private static final String SEP = " \u2022 ";

        private final Context context;
        private final UserPreferenceManager userPreferenceManager;

        public TextFormatter(@NonNull Context context, @NonNull UserPreferenceManager userPreferenceManager) {
            this.context = Preconditions.checkNotNull(context.getApplicationContext());
            this.userPreferenceManager = Preconditions.checkNotNull(userPreferenceManager);
        }

        @NonNull
        private String buildLegendForImage(@NonNull Receipt receipt) {
            final int num = (userPreferenceManager.get(UserPreference.ReportOutput.PrintUserIdByPdfPhoto)) ?
                    receipt.getId() : receipt.getIndex();

            final String extra = (userPreferenceManager.get(UserPreference.ReportOutput.PrintReceiptCommentByPdfPhoto)
                    && !TextUtils.isEmpty(receipt.getComment()))
                    ? SEP + receipt.getComment()
                    : "";

            return num + SEP + receipt.getName() + SEP
                    + receipt.getFormattedDate(context,
                    userPreferenceManager.get(UserPreference.General.DateSeparator)) + extra;
        }
    }
}
