package co.smartreceipts.android.analytics.events;

import android.support.annotation.NonNull;

/**
 * Extends the {@link Event} contract to allow us to "mark" events with dynamic information (e.g. data points)
 */
public interface DataPointEvent extends Event {

    /**
     * Adds a datapoint to this event
     *
     * @param dataPoint the {@link DataPoint} to add
     * @return this {@link DataPointEvent} for method chaining
     */
    DataPointEvent addDataPoint(@NonNull DataPoint dataPoint);
}
