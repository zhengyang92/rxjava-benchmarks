package co.smartreceipts.android;

import android.support.annotation.NonNull;

import java.io.File;
import java.math.BigDecimal;
import java.sql.Date;
import java.util.Collections;
import java.util.TimeZone;

import co.smartreceipts.android.model.Category;
import co.smartreceipts.android.model.PaymentMethod;
import co.smartreceipts.android.model.Price;
import co.smartreceipts.android.currency.PriceCurrency;
import co.smartreceipts.android.model.Source;
import co.smartreceipts.android.model.Trip;
import co.smartreceipts.android.model.factory.CategoryBuilderFactory;
import co.smartreceipts.android.model.gson.ExchangeRate;
import co.smartreceipts.android.model.impl.DefaultTripImpl;
import co.smartreceipts.android.model.impl.ImmutablePaymentMethodImpl;
import co.smartreceipts.android.model.impl.ImmutablePriceImpl;
import co.smartreceipts.android.sync.model.SyncState;
import co.smartreceipts.android.sync.model.impl.DefaultSyncState;
import co.smartreceipts.android.sync.model.impl.Identifier;
import co.smartreceipts.android.sync.model.impl.IdentifierMap;
import co.smartreceipts.android.sync.model.impl.MarkedForDeletionMap;
import co.smartreceipts.android.sync.model.impl.SyncStatusMap;
import co.smartreceipts.android.sync.provider.SyncProvider;

public class DefaultObjects {

    private DefaultObjects() { }

    @NonNull
    public static Trip newDefaultTrip() {
        return new DefaultTripImpl(new File(new File("").getAbsolutePath()),
                                  new Date(System.currentTimeMillis()),
                                  TimeZone.getDefault(),
                                  new Date(System.currentTimeMillis()),
                                  TimeZone.getDefault(),
                                  PriceCurrency.getDefaultCurrency(),
                                  "comment",
                                  "costCenter",
                                  Source.Undefined,
                                  newDefaultSyncState());
    }

    @NonNull
    public static SyncState newDefaultSyncState() {
        return new DefaultSyncState(new IdentifierMap(Collections.singletonMap(SyncProvider.GoogleDrive, new Identifier("abc"))),
                                    new SyncStatusMap(Collections.singletonMap(SyncProvider.GoogleDrive, true)),
                                    new MarkedForDeletionMap(Collections.singletonMap(SyncProvider.GoogleDrive, true)),
                                    new Date(System.currentTimeMillis()));
    }

    @NonNull
    public static PaymentMethod newDefaultPaymentMethod() {
        return new ImmutablePaymentMethodImpl(23, "method");
    }

    @NonNull
    public static Category newDefaultCategory() {
        return new CategoryBuilderFactory().setName("name").setCode("code").build();
    }

    @NonNull
    public static Price newDefaultPrice() {
        return new ImmutablePriceImpl(new BigDecimal(5), PriceCurrency.getInstance("USD"), new ExchangeRate("USD", Collections.singletonMap("USD", 1.00d)));
    }

    @NonNull
    public static Price newDefaultTax() {
        return new ImmutablePriceImpl(new BigDecimal(2), PriceCurrency.getInstance("USD"), new ExchangeRate("USD", Collections.singletonMap("USD", 1.00d)));
    }
}
