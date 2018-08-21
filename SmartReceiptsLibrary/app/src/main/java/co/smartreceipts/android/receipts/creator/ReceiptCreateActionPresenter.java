package co.smartreceipts.android.receipts.creator;

import android.support.annotation.NonNull;

import com.google.common.base.Preconditions;

import javax.inject.Inject;

import co.smartreceipts.android.analytics.Analytics;
import co.smartreceipts.android.analytics.events.Events;
import co.smartreceipts.android.di.scopes.FragmentScope;
import co.smartreceipts.android.widget.mvp.BasePresenter;
import co.smartreceipts.android.widget.mvp.Presenter;

/**
 * Provides an implementation of the {@link Presenter} contract for our {@link ReceiptCreateActionView},
 * so we can manage the process by which the user can create a new receipt via the camera, gallery
 * import, or plain text.
 */
@FragmentScope
public class ReceiptCreateActionPresenter extends BasePresenter<ReceiptCreateActionView> {

    private final Analytics analytics;

    @Inject
    public ReceiptCreateActionPresenter(@NonNull ReceiptCreateActionView view,
                                        @NonNull Analytics analytics) {
        super(view);
        this.analytics = Preconditions.checkNotNull(analytics);
    }

    @Override
    public void subscribe() {
        compositeDisposable.add(view.getCreateNewReceiptMenuButtonToggles()
                .subscribe(isOpen -> {
                    if (isOpen) {
                        view.displayReceiptCreationMenuOptions();
                    } else {
                        view.hideReceiptCreationMenuOptions();
                    }
                }));

        compositeDisposable.add(view.getCreateNewReceiptFromCameraButtonClicks()
                .doOnNext(ignored -> analytics.record(Events.Receipts.AddPictureReceipt))
                .subscribe(ignored -> view.createNewReceiptViaCamera()));

        compositeDisposable.add(view.getCreateNewReceiptFromPlainTextButtonClicks()
                .doOnNext(ignored -> analytics.record(Events.Receipts.AddTextReceipt))
                .subscribe(ignored -> view.createNewReceiptViaPlainText()));

        compositeDisposable.add(view.getCreateNewReceiptFromImportedFileButtonClicks()
                .doOnNext(ignored -> analytics.record(Events.Receipts.ImportPictureReceipt))
                .subscribe(ignored -> view.createNewReceiptViaFileImport()));
    }

}
