package co.smartreceipts.android.widget.rxbinding2;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.widget.TextView;

import com.google.common.base.Preconditions;
import com.hadisatrio.optional.Optional;

import co.smartreceipts.android.model.Price;
import io.reactivex.Observable;
import io.reactivex.functions.Consumer;

/**
 * Static factory methods for creating {@linkplain Observable observables} and {@linkplain Consumer
 * actions} for {@link TextView}.
 */
public class RxTextViewExtensions {

    /**
     * An action which sets the text property of {@code view} with a {@link Price}
     * <p>
     * <em>Warning:</em> The created observable keeps a strong reference to {@code view}. Unsubscribe
     * to free this reference.
     */
    @CheckResult
    @NonNull
    public static Consumer<? super Price> price(@NonNull final TextView view) {
        //noinspection ResultOfMethodCallIgnored
        Preconditions.checkNotNull(view, "view == null");
        return (Consumer<Price>) price -> {
            final String currentPrice = view.getText().toString();
            final String proposedPrice = price.getDecimalFormattedPrice();
            if (!proposedPrice.equals(currentPrice)) {
                view.setText(proposedPrice);
            }
        };
    }

    /**
     * An action which sets the text property of {@code view} with an {@link com.hadisatrio.optional.Optional} of
     * {@link Price}
     * <p>
     * <em>Warning:</em> The created observable keeps a strong reference to {@code view}. Unsubscribe
     * to free this reference.
     */
    @CheckResult
    @NonNull
    public static Consumer<? super Optional<Price>> priceOptional(@NonNull final TextView view) {
        //noinspection ResultOfMethodCallIgnored
        Preconditions.checkNotNull(view, "view == null");
        return (Consumer<Optional<Price>>) price -> {
            final String currentPrice = view.getText().toString();
            final String proposedPrice = price.isPresent() ? price.get().getDecimalFormattedPrice() : "";
            if (!proposedPrice.equals(currentPrice)) {
                view.setText(proposedPrice);
            }
        };
    }
}
