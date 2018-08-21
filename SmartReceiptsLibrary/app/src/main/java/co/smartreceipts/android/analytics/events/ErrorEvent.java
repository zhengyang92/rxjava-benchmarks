package co.smartreceipts.android.analytics.events;

import android.support.annotation.NonNull;

import com.google.common.base.Preconditions;

import java.util.Collections;

import co.smartreceipts.android.utils.ExceptionUtils;

public class ErrorEvent extends DefaultEvent {

    private static final String DEFAULT_NAME = "Exception";
    private static final String TRACE_DATAPOINT_NAME = "trace";
    private final Throwable throwable;



    private enum Category implements Event.Category {
        OnError
    }
    public ErrorEvent(@NonNull Throwable throwable) {
        this(DEFAULT_NAME, throwable);
    }

    public ErrorEvent(@NonNull Object object, @NonNull Throwable throwable) {
        this(object.getClass(), throwable);
    }

    public ErrorEvent(@NonNull Class<?> klass, @NonNull Throwable throwable) {
        this(klass.getSimpleName(), throwable);
    }

    public ErrorEvent(@NonNull String name, @NonNull Throwable throwable) {
        super(Category.OnError, new ImmutableName(name), Collections.singletonList(new DataPoint(TRACE_DATAPOINT_NAME, ExceptionUtils.getStackTrace(throwable))));
        this.throwable = Preconditions.checkNotNull(throwable);
    }

    @NonNull
    public Throwable getThrowable() {
        return throwable;
    }

}
