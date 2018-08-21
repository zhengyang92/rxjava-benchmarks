package co.smartreceipts.android.model.converters;

import android.support.annotation.NonNull;

import java.util.List;

/**
 * Supports the conversion between two types of model objects (i.e. a list {@link co.smartreceipts.android.model.Distance}
 * of distance objects might need to be converted into a list of {@link co.smartreceipts.android.model.Receipt} objects).
 *
 * @author williambaumann
 */
public interface ModelConverter<F, T> {

    /**
     * Converts a {@link List} of type {@link F} into a {@link List} object of type {@link T}
     *
     * @param fs - the {@link List} of {@link F} to convert.
     * @return a {@link List} of {@link T}
     */
    @NonNull
    List<T> convert(@NonNull List<F> fs);

}
