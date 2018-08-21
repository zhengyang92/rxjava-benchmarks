package co.smartreceipts.android.persistence.database.controllers.grouping.results;

import android.support.annotation.NonNull;

import com.google.common.base.Preconditions;

import co.smartreceipts.android.model.Price;

public class SumReimbursementGroupingResult {

    private final boolean isReimbursable;

    private final Price price;

    public SumReimbursementGroupingResult(boolean isReimbursable, @NonNull Price price) {
        this.isReimbursable = isReimbursable;
        this.price = Preconditions.checkNotNull(price);
    }

    public boolean isReimbursable() {
        return isReimbursable;
    }

    @NonNull
    public Price getPrice() {
        return price;
    }
}
