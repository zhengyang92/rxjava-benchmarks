package co.smartreceipts.android.widget.tooltip.report.generate.data;

public interface GenerateInfoTooltipStorage {

    void tooltipWasDismissed();

    boolean wasTooltipDismissed();

    void reportWasGenerated();

    boolean wasReportEverGenerated();

}
