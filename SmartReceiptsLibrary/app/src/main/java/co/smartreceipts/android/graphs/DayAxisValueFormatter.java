package co.smartreceipts.android.graphs;

import android.text.format.DateFormat;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DayAxisValueFormatter implements IAxisValueFormatter {

    private final SimpleDateFormat dateFormat;
    private final Calendar calendar;

    public DayAxisValueFormatter() {
        final String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), "MMdd");
        this.dateFormat = new SimpleDateFormat(pattern, Locale.getDefault());
        this.calendar = Calendar.getInstance();
    }

    @Override
    public String getFormattedValue(float value, AxisBase axis) {

        try { // Hack for sometimes appearing IndexOutOfBoundsException from MPAndroidCharts lib
            final int days = (int) value;
            calendar.set(Calendar.DAY_OF_YEAR, days);
            return dateFormat.format(calendar.getTime());
        } catch (IndexOutOfBoundsException e) {
            return "";
        }
    }

}
