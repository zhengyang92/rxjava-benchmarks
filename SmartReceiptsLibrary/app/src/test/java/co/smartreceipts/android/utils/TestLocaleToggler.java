package co.smartreceipts.android.utils;

import android.support.annotation.NonNull;

import java.util.Locale;

import co.smartreceipts.android.currency.PriceCurrency;
import co.smartreceipts.android.model.utils.ModelUtils;

public class TestLocaleToggler {

    private static Locale originalLocale;

    public static void setDefaultLocale(@NonNull Locale locale) {
        PriceCurrency.clearStaticCachesForTesting();
        ModelUtils.clearStaticCachesForTesting();
        originalLocale = Locale.getDefault();
        Locale.setDefault(locale);
    }

    public static void resetDefaultLocale() {
        if (originalLocale == null) {
            throw new IllegalArgumentException("Cannot reset the default Locale without calling the setter method.");
        }
        Locale.setDefault(originalLocale);
        originalLocale = null;
    }
}
