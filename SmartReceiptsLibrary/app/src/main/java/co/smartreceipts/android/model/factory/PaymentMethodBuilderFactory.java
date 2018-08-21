package co.smartreceipts.android.model.factory;

import android.support.annotation.NonNull;

import com.google.common.base.Preconditions;

import co.smartreceipts.android.model.PaymentMethod;
import co.smartreceipts.android.model.impl.ImmutablePaymentMethodImpl;
import co.smartreceipts.android.sync.model.SyncState;
import co.smartreceipts.android.sync.model.impl.DefaultSyncState;

/**
 * A {@link co.smartreceipts.android.model.Receipt} {@link BuilderFactory}
 * implementation, which will be used to generate instances of {@link co.smartreceipts.android.model.PaymentMethod} objects
 */
public class PaymentMethodBuilderFactory implements BuilderFactory<PaymentMethod> {

    private int _id;
    private String _method;
    private SyncState _syncState;
    private long _customOrderId;

    /**
     * Default constructor for this class
     */
    public PaymentMethodBuilderFactory() {
        _id = MISSING_ID;
        _method = "";
        _syncState = new DefaultSyncState();
        _customOrderId = 0;
    }

    /**
     * Defines the primary key id for this object
     *
     * @param id - the id
     * @return this {@link PaymentMethodBuilderFactory} for method chaining
     */
    public PaymentMethodBuilderFactory setId(int id) {
        _id = id;
        return this;
    }

    /**
     * Defines the payment method type for this object
     *
     * @param method - the payment method
     * @return this {@link PaymentMethodBuilderFactory} for method chaining
     */
    public PaymentMethodBuilderFactory setMethod(@NonNull String method) {
        _method = Preconditions.checkNotNull(method);
        return this;
    }

    public PaymentMethodBuilderFactory setSyncState(@NonNull SyncState syncState) {
        _syncState = Preconditions.checkNotNull(syncState);
        return this;
    }


    /**
     * Defines the "custom_order_id" for this payment method
     *
     * @param orderId - the payment method custom order id
     * @return this {@link PaymentMethodBuilderFactory} for method chaining
     */
    public PaymentMethodBuilderFactory setCustomOrderId(long orderId) {
        _customOrderId = orderId;
        return this;
    }

    /**
     * @return - the {@link PaymentMethodBuilderFactory} object as set by the setter methods in this class
     */
    @NonNull
    public PaymentMethod build() {
        return new ImmutablePaymentMethodImpl(_id, _method, _syncState, _customOrderId);
    }
}
