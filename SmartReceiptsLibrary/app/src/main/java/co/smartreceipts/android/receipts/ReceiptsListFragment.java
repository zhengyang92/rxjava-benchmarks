package co.smartreceipts.android.receipts;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionMenu;
import com.google.common.base.Preconditions;
import com.hadisatrio.optional.Optional;
import com.jakewharton.rxbinding2.view.RxView;
import com.squareup.picasso.Picasso;
import com.tapadoo.alerter.Alert;
import com.tapadoo.alerter.Alerter;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import co.smartreceipts.android.R;
import co.smartreceipts.android.activities.NavigationHandler;
import co.smartreceipts.android.analytics.Analytics;
import co.smartreceipts.android.analytics.events.Events;
import co.smartreceipts.android.config.ConfigurationManager;
import co.smartreceipts.android.fragments.ImportPhotoPdfDialogFragment;
import co.smartreceipts.android.fragments.ReceiptMoveCopyDialogFragment;
import co.smartreceipts.android.fragments.ReportInfoFragment;
import co.smartreceipts.android.imports.AttachmentSendFileImporter;
import co.smartreceipts.android.imports.CameraInteractionController;
import co.smartreceipts.android.imports.RequestCodes;
import co.smartreceipts.android.imports.importer.ActivityFileResultImporter;
import co.smartreceipts.android.imports.intents.IntentImportProcessor;
import co.smartreceipts.android.imports.intents.model.FileType;
import co.smartreceipts.android.imports.locator.ActivityFileResultLocator;
import co.smartreceipts.android.imports.locator.ActivityFileResultLocatorResponse;
import co.smartreceipts.android.model.Receipt;
import co.smartreceipts.android.model.Trip;
import co.smartreceipts.android.model.factory.ReceiptBuilderFactory;
import co.smartreceipts.android.ocr.OcrManager;
import co.smartreceipts.android.ocr.widget.alert.OcrStatusAlerterPresenter;
import co.smartreceipts.android.ocr.widget.alert.OcrStatusAlerterView;
import co.smartreceipts.android.permissions.PermissionsDelegate;
import co.smartreceipts.android.permissions.exceptions.PermissionsNotGrantedException;
import co.smartreceipts.android.persistence.PersistenceManager;
import co.smartreceipts.android.persistence.database.controllers.ReceiptTableEventsListener;
import co.smartreceipts.android.persistence.database.controllers.TableController;
import co.smartreceipts.android.persistence.database.controllers.impl.ReceiptTableController;
import co.smartreceipts.android.persistence.database.controllers.impl.StubTableEventsListener;
import co.smartreceipts.android.persistence.database.controllers.impl.TripTableController;
import co.smartreceipts.android.persistence.database.operations.DatabaseOperationMetadata;
import co.smartreceipts.android.persistence.database.operations.OperationFamilyType;
import co.smartreceipts.android.persistence.database.tables.ordering.OrderingPreferencesManager;
import co.smartreceipts.android.receipts.attacher.ReceiptAttachmentDialogFragment;
import co.smartreceipts.android.receipts.attacher.ReceiptAttachmentManager;
import co.smartreceipts.android.receipts.attacher.ReceiptRemoveAttachmentDialogFragment;
import co.smartreceipts.android.receipts.creator.ReceiptCreateActionPresenter;
import co.smartreceipts.android.receipts.creator.ReceiptCreateActionView;
import co.smartreceipts.android.receipts.delete.DeleteReceiptDialogFragment;
import co.smartreceipts.android.receipts.ordering.ReceiptsOrderer;
import co.smartreceipts.android.settings.UserPreferenceManager;
import co.smartreceipts.android.settings.catalog.UserPreference;
import co.smartreceipts.android.sync.BackupProvidersManager;
import co.smartreceipts.android.utils.ConfigurableResourceFeature;
import co.smartreceipts.android.utils.log.Logger;
import co.smartreceipts.android.widget.model.UiIndicator;
import co.smartreceipts.android.widget.rxbinding2.RxFloatingActionMenu;
import dagger.android.support.AndroidSupportInjection;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import wb.android.dialog.BetterDialogBuilder;
import wb.android.flex.Flex;

public class ReceiptsListFragment extends ReceiptsFragment implements ReceiptTableEventsListener, ReceiptCreateActionView,
        OcrStatusAlerterView, ReceiptAttachmentDialogFragment.Listener {

    public static final String READ_PERMISSION = Manifest.permission.READ_EXTERNAL_STORAGE;

    // Outstate
    private static final String OUT_HIGHLIGHTED_RECEIPT = "out_highlighted_receipt";
    private static final String OUT_IMAGE_URI = "out_image_uri";

    @Inject
    Flex flex;

    @Inject
    PersistenceManager persistenceManager;

    @Inject
    ConfigurationManager configurationManager;

    @Inject
    Analytics analytics;

    @Inject
    TripTableController tripTableController;

    @Inject
    ReceiptTableController receiptTableController;

    @Inject
    BackupProvidersManager backupProvidersManager;

    @Inject
    OcrManager ocrManager;

    @Inject
    NavigationHandler navigationHandler;

    @Inject
    ActivityFileResultImporter activityFileResultImporter;

    @Inject
    ActivityFileResultLocator activityFileResultLocator;

    @Inject
    PermissionsDelegate permissionsDelegate;

    @Inject
    IntentImportProcessor intentImportProcessor;

    @Inject
    ReceiptCreateActionPresenter receiptCreateActionPresenter;

    @Inject
    OcrStatusAlerterPresenter ocrStatusAlerterPresenter;

    @Inject
    UserPreferenceManager preferenceManager;

    @Inject
    OrderingPreferencesManager orderingPreferencesManager;

    @Inject
    ReceiptsOrderer receiptsOrderer;

    @Inject
    ReceiptAttachmentManager receiptAttachmentManager;

    @Inject
    Picasso picasso;

    @BindView(R.id.progress)
    ProgressBar loadingProgress;

    @BindView(R.id.no_data)
    TextView noDataAlert;

    @BindView(R.id.receipt_action_camera)
    View receiptActionCameraButton;

    @BindView(R.id.receipt_action_text)
    View receiptActionTextButton;

    @BindView(R.id.receipt_action_import)
    View receiptActionImportButton;

    @BindView(R.id.fab_menu)
    FloatingActionMenu floatingActionMenu;

    @BindView(R.id.fab_active_mask)
    View floatingActionMenuActiveMaskView;

    // Non Butter Knife Views
    private Alerter alerter;
    private Alert alert;

    private Unbinder unbinder;

    private Receipt highlightedReceipt;
    private Uri imageUri;

    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    private ActionBarSubtitleUpdatesListener actionBarSubtitleUpdatesListener = new ActionBarSubtitleUpdatesListener();

    private boolean showDateHeaders;
    private ReceiptsHeaderItemDecoration headerItemDecoration;

    private boolean importIntentMode;

    @Override
    public void onAttach(Context context) {
        AndroidSupportInjection.inject(this);
        super.onAttach(context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Logger.debug(this, "onCreate");
        super.onCreate(savedInstanceState);
        adapter = new ReceiptsAdapter(getContext(), preferenceManager, backupProvidersManager, navigationHandler, receiptsOrderer, picasso);
        if (savedInstanceState != null) {
            imageUri = savedInstanceState.getParcelable(OUT_IMAGE_URI);
            highlightedReceipt = savedInstanceState.getParcelable(OUT_HIGHLIGHTED_RECEIPT);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Logger.debug(this, "onCreateView");

        // Create our OCR drop-down alerter
        this.alerter = Alerter.create(getActivity())
                .setTitle(R.string.ocr_status_title)
                .setBackgroundColor(R.color.smart_receipts_colorAccent)
                .setIcon(R.drawable.ic_receipt_white_24dp);

        // And inflate the root view
        return inflater.inflate(R.layout.receipt_fragment_layout, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Logger.debug(this, "onViewCreated");

        this.unbinder = ButterKnife.bind(this, view);

        receiptActionTextButton.setVisibility(configurationManager.isEnabled(ConfigurableResourceFeature.TextOnlyReceipts) ? View.VISIBLE : View.GONE);
        floatingActionMenuActiveMaskView.setOnClickListener(v -> {
            // Intentional stub to block click events when this view is active
        });

        showDateHeaders = preferenceManager.get(UserPreference.Layout.IncludeReceiptDateInLayout);
        headerItemDecoration = new ReceiptsHeaderItemDecoration(adapter, ReceiptsListItem.TYPE_HEADER);
        if (showDateHeaders) {
            recyclerView.addItemDecoration(headerItemDecoration);
        }

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Logger.debug(this, "onActivityCreated");

        trip = ((ReportInfoFragment) getParentFragment()).getTrip();
        Preconditions.checkNotNull(trip, "A valid trip is required");
    }

    @Override
    public void onStart() {
        super.onStart();
        Logger.debug(this, "onStart");
        receiptTableController.subscribe(this);
        receiptTableController.get(trip);
    }

    @Override
    public void onResume() {
        super.onResume();
        Logger.debug(this, "onResume");

        if (showDateHeaders != preferenceManager.get(UserPreference.Layout.IncludeReceiptDateInLayout)) {
            showDateHeaders = preferenceManager.get(UserPreference.Layout.IncludeReceiptDateInLayout);
            if (showDateHeaders) {
                recyclerView.addItemDecoration(headerItemDecoration);
            } else {
                recyclerView.removeItemDecoration(headerItemDecoration);
            }
        }

        tripTableController.subscribe(actionBarSubtitleUpdatesListener);

        ocrStatusAlerterPresenter.subscribe();
        receiptCreateActionPresenter.subscribe();

        compositeDisposable.add(activityFileResultLocator.getUriStream()
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapSingle(response -> {
                    Logger.debug(this, "getting response from activityFileResultLocator.getUriStream() uri {}", response.getUri().toString());
                    if (response.getUri().getScheme() != null && response.getUri().getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
                        return Single.just(response);
                    } else { // we need to check read external storage permission
                        Logger.debug(this, "need to check permission");
                        return permissionsDelegate.checkPermissionAndMaybeAsk(READ_PERMISSION)
                                .toSingleDefault(response)
                                .onErrorReturn(ActivityFileResultLocatorResponse::LocatorError);
                    }
                })
                .subscribe(locatorResponse -> {
                    permissionsDelegate.markRequestConsumed(READ_PERMISSION);
                    if (!locatorResponse.getThrowable().isPresent()) {
                        if (loadingProgress != null) {
                            loadingProgress.setVisibility(View.VISIBLE);
                        }
                        activityFileResultImporter.importFile(locatorResponse.getRequestCode(),
                                locatorResponse.getResultCode(), locatorResponse.getUri(), trip);
                    } else {
                        Logger.debug(this, "Error with permissions");
                        if (locatorResponse.getThrowable().get() instanceof PermissionsNotGrantedException) {
                            Toast.makeText(getActivity(), getString(R.string.toast_no_storage_permissions), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getActivity(), getFlexString(R.string.FILE_SAVE_ERROR), Toast.LENGTH_SHORT).show();
                        }
                        highlightedReceipt = null;
                        if (loadingProgress != null) {
                            loadingProgress.setVisibility(View.GONE);
                        }
                        Logger.debug(this, "marking that locator result were consumed");
                        activityFileResultLocator.markThatResultsWereConsumed();
                        Logger.debug(this, "marked that locator result were consumed");
                    }
                }));

        compositeDisposable.add(activityFileResultImporter.getResultStream()
                .subscribe(response -> {
                    Logger.info(ReceiptsListFragment.this, "Handled the import of {}", response);
                    if (!response.getThrowable().isPresent()) {
                        switch (response.getRequestCode()) {
                            case RequestCodes.IMPORT_GALLERY_IMAGE:
                            case RequestCodes.IMPORT_GALLERY_PDF:
                            case RequestCodes.NATIVE_NEW_RECEIPT_CAMERA_REQUEST:
                                navigationHandler.navigateToCreateNewReceiptFragment(trip, response.getFile(), response.getOcrResponse());
                                break;
                            case RequestCodes.ATTACH_GALLERY_IMAGE:
                            case RequestCodes.NATIVE_ADD_PHOTO_CAMERA_REQUEST:
                                if (highlightedReceipt != null) {
                                    final Receipt updatedReceipt = new ReceiptBuilderFactory(highlightedReceipt)
                                            .setFile(response.getFile())
                                            .build();
                                    receiptTableController.update(highlightedReceipt, updatedReceipt, new DatabaseOperationMetadata());
                                }
                                break;
                            case RequestCodes.ATTACH_GALLERY_PDF:
                                if (highlightedReceipt != null) {
                                    final Receipt updatedReceiptWithFile = new ReceiptBuilderFactory(highlightedReceipt)
                                            .setFile(response.getFile())
                                            .build();
                                    receiptTableController.update(highlightedReceipt, updatedReceiptWithFile, new DatabaseOperationMetadata());
                                }
                                break;
                        }
                    } else {
                        Toast.makeText(getActivity(), getFlexString(R.string.FILE_SAVE_ERROR), Toast.LENGTH_SHORT).show();
                    }
                    highlightedReceipt = null;

                    if (loadingProgress != null) {
                        loadingProgress.setVisibility(View.GONE);
                    }
                    // Indicate that we consumed these results to avoid using this same stream on the next event
                    activityFileResultLocator.markThatResultsWereConsumed();
                    activityFileResultImporter.markThatResultsWereConsumed();
                }));

        compositeDisposable.add(adapter.getItemClicks()
                .subscribe(receipt -> {
                    if (!importIntentMode) {
                        analytics.record(Events.Receipts.ReceiptMenuEdit);
                        navigationHandler.navigateToEditReceiptFragment(trip, receipt);
                    } else {
                        attachImportIntent(receipt);
                    }
                }));

        compositeDisposable.add(adapter.getMenuClicks()
                .subscribe(receipt -> {
                    if (!importIntentMode) {
                        showReceiptMenu(receipt);
                    }
                }));

        compositeDisposable.add(adapter.getImageClicks()
                .subscribe(receipt -> {
                    if (!importIntentMode) {
                        if (receipt.hasImage()) {
                            analytics.record(Events.Receipts.ReceiptMenuViewImage);
                            navigationHandler.navigateToViewReceiptImage(receipt);
                        } else if (receipt.hasPDF()) {
                            analytics.record(Events.Receipts.ReceiptMenuViewPdf);
                            navigationHandler.navigateToViewReceiptPdf(receipt);
                        } else {
                            showAttachmentDialog(receipt);
                        }
                    } else {
                        attachImportIntent(receipt);
                    }
                }));

        compositeDisposable.add(intentImportProcessor.getLastResult()
                .map(intentImportResultOptional -> intentImportResultOptional.isPresent() &&
                        (intentImportResultOptional.get().getFileType() == FileType.Image || intentImportResultOptional.get().getFileType() == FileType.Pdf))
                .subscribe(importIntentPresent -> importIntentMode = importIntentPresent));
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        updateActionBarTitle(true);
    }

    @Override
    public void onPause() {
        compositeDisposable.clear();
        receiptCreateActionPresenter.unsubscribe();
        ocrStatusAlerterPresenter.unsubscribe();
        floatingActionMenu.close(false);
        tripTableController.unsubscribe(actionBarSubtitleUpdatesListener);
        super.onPause();
    }

    @Override
    public void onStop() {
        receiptTableController.unsubscribe(this);
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(OUT_IMAGE_URI, imageUri);
        outState.putParcelable(OUT_HIGHLIGHTED_RECEIPT, highlightedReceipt);
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        Logger.debug(this, "onActivityResult");
        Logger.debug(this, "Result Code: {}", resultCode);
        Logger.debug(this, "Request Code: {}", requestCode);

        // Need to make this call here, since users with "Don't keep activities" will hit this call
        // before any of onCreate/onStart/onResume is called. This should restore our current trip (what
        // onResume() would normally do to prevent a variety of crashes that we might encounter
        if (trip == null) {
            trip = ((ReportInfoFragment) getParentFragment()).getTrip();
        }

        // Null out the last request
        final Uri cachedImageSaveLocation = imageUri;
        imageUri = null;

        loadingProgress.setVisibility(View.VISIBLE);

        activityFileResultLocator.onActivityResult(requestCode, resultCode, data, cachedImageSaveLocation);

        if (resultCode != Activity.RESULT_OK) {
            loadingProgress.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        Logger.debug(this, "onDestroyView");
        this.alert = null;
        this.alerter = null;
        this.unbinder.unbind();
        super.onDestroyView();
    }

    @Override
    protected PersistenceManager getPersistenceManager() {
        return persistenceManager;
    }

    private void showAttachmentDialog(final Receipt receipt) {
        highlightedReceipt = receipt;

        ReceiptAttachmentDialogFragment.newInstance(receipt).show(getChildFragmentManager(), ReceiptAttachmentDialogFragment.class.getSimpleName());
    }

    public final void showReceiptMenu(final Receipt receipt) {
        highlightedReceipt = receipt;
        final BetterDialogBuilder builder = new BetterDialogBuilder(getActivity());
        builder.setTitle(receipt.getName())
                .setCancelable(true)
                .setNegativeButton(android.R.string.cancel, (dialog, id) -> dialog.cancel());

        final String receiptActionDelete = getString(R.string.receipt_dialog_action_delete);
        final String receiptActionMoveCopy = getString(R.string.receipt_dialog_action_move_copy);
        final String receiptActionRemoveAttachment = getString(R.string.receipt_dialog_action_remove_attachment);
        final String[] receiptActions;
        if (receipt.getFile() != null) {
            receiptActions = new String[]{receiptActionDelete, receiptActionMoveCopy};
        } else {
            receiptActions = new String[]{receiptActionDelete, receiptActionMoveCopy, receiptActionRemoveAttachment};
        }
        builder.setItems(receiptActions, (dialog, item) -> {
            final String selection = receiptActions[item];
            if (selection != null) {
                if (selection.equals(receiptActionDelete)) { // Delete Receipt
                    analytics.record(Events.Receipts.ReceiptMenuDelete);
                    final DeleteReceiptDialogFragment deleteReceiptDialogFragment = DeleteReceiptDialogFragment.newInstance(receipt);
                    navigationHandler.showDialog(deleteReceiptDialogFragment);

                } else if (selection.equals(receiptActionMoveCopy)) {// Move-Copy
                    analytics.record(Events.Receipts.ReceiptMenuMoveCopy);
                    ReceiptMoveCopyDialogFragment.newInstance(receipt).show(getFragmentManager(), ReceiptMoveCopyDialogFragment.TAG);

                } else if (selection.equals(receiptActionRemoveAttachment)) { // Remove Attachment
                    analytics.record(Events.Receipts.ReceiptMenuRemoveAttachment);
                    navigationHandler.showDialog(ReceiptRemoveAttachmentDialogFragment.newInstance(receipt));
                }
            }
            dialog.cancel();
        });
        builder.show();
    }

    @Override
    protected TableController<Receipt> getTableController() {
        return receiptTableController;
    }

    @Override
    public void onGetSuccess(@NonNull List<Receipt> receipts, @NonNull Trip trip) {
        if (isAdded()) {
            super.onGetSuccess(receipts);

            loadingProgress.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            if (receipts.isEmpty()) {
                noDataAlert.setVisibility(View.VISIBLE);
            } else {
                noDataAlert.setVisibility(View.INVISIBLE);
            }
            updateActionBarTitle(getUserVisibleHint());

        }
    }

    @Override
    public void onGetFailure(@Nullable Throwable e, @NonNull Trip trip) {
        Toast.makeText(getActivity(), R.string.database_get_error, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onGetSuccess(@NonNull List<Receipt> list) {
        // TODO: Respond?
    }

    @Override
    public void onGetFailure(@Nullable Throwable e) {
        Toast.makeText(getActivity(), R.string.database_get_error, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onInsertSuccess(@NonNull Receipt receipt, @NonNull DatabaseOperationMetadata databaseOperationMetadata) {
        if (isAdded()) {
            receiptTableController.get(trip);
        }
    }

    @Override
    public void onInsertFailure(@NonNull Receipt receipt, @Nullable Throwable e, @NonNull DatabaseOperationMetadata databaseOperationMetadata) {
        if (isAdded() && databaseOperationMetadata.getOperationFamilyType() != OperationFamilyType.Sync) {
            Toast.makeText(getActivity(), getFlexString(R.string.database_error), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onUpdateSuccess(@NonNull Receipt oldReceipt, @NonNull Receipt newReceipt, @NonNull DatabaseOperationMetadata databaseOperationMetadata) {
        if (isAdded()) {
            if (databaseOperationMetadata.getOperationFamilyType() != OperationFamilyType.Sync) {
                if (newReceipt.getFile() != null && newReceipt.getFileLastModifiedTime() != oldReceipt.getFileLastModifiedTime()) {
                    final int stringId;
                    if (oldReceipt.getFile() != null) {
                        if (newReceipt.hasImage()) {
                            stringId = R.string.toast_receipt_image_replaced;
                        } else {
                            stringId = R.string.toast_receipt_pdf_replaced;
                        }
                    } else {
                        if (newReceipt.hasImage()) {
                            stringId = R.string.toast_receipt_image_added;
                        } else {
                            stringId = R.string.toast_receipt_pdf_added;
                        }
                    }
                    Toast.makeText(getActivity(), getString(stringId, newReceipt.getName()), Toast.LENGTH_SHORT).show();

                    intentImportProcessor.getLastResult()
                            .filter(intentImportResultOptional -> intentImportResultOptional.isPresent() &&
                                    (intentImportResultOptional.get().getFileType() == FileType.Image || intentImportResultOptional.get().getFileType() == FileType.Pdf))
                            .subscribe(ignored -> {
                                if (getActivity() != null) {
                                    intentImportProcessor.markIntentAsSuccessfullyProcessed(getActivity().getIntent());
                                }
                            });
                }
            }
            // But still refresh for sync operations
            receiptTableController.get(trip);
        }
    }

    @Override
    public void onUpdateFailure(@NonNull Receipt oldReceipt, @Nullable Throwable e, @NonNull DatabaseOperationMetadata databaseOperationMetadata) {
        if (isAdded() && databaseOperationMetadata.getOperationFamilyType() != OperationFamilyType.Sync) {
            Toast.makeText(getActivity(), getFlexString(R.string.database_error), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDeleteSuccess(@NonNull Receipt receipt, @NonNull DatabaseOperationMetadata databaseOperationMetadata) {
        if (isAdded()) {
            receiptTableController.get(trip);
        }
    }

    @Override
    public void onDeleteFailure(@NonNull Receipt receipt, @Nullable Throwable e, @NonNull DatabaseOperationMetadata databaseOperationMetadata) {
        if (isAdded() && databaseOperationMetadata.getOperationFamilyType() != OperationFamilyType.Sync) {
            Toast.makeText(getActivity(), getFlexString(R.string.database_error), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCopySuccess(@NonNull Receipt oldReceipt, @NonNull Receipt newReceipt) {
        if (isAdded()) {
            receiptTableController.get(trip);
            Toast.makeText(getActivity(), getFlexString(R.string.toast_receipt_copy), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCopyFailure(@NonNull Receipt oldReceipt, @Nullable Throwable e) {
        if (isAdded()) {
            Toast.makeText(getActivity(), getFlexString(R.string.COPY_ERROR), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMoveSuccess(@NonNull Receipt oldReceipt, @NonNull Receipt newReceipt) {
        if (isAdded()) {
            receiptTableController.get(trip);
            Toast.makeText(getActivity(), getFlexString(R.string.toast_receipt_move), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMoveFailure(@NonNull Receipt oldReceipt, @Nullable Throwable e) {
        if (isAdded()) {
            Toast.makeText(getActivity(), getFlexString(R.string.MOVE_ERROR), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void displayReceiptCreationMenuOptions() {
        if (floatingActionMenuActiveMaskView.getVisibility() != View.VISIBLE) { // avoid duplicate animations
            floatingActionMenuActiveMaskView.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.out_from_bottom_right));
            floatingActionMenuActiveMaskView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void hideReceiptCreationMenuOptions() {
        if (floatingActionMenuActiveMaskView.getVisibility() != View.GONE) { // avoid duplicate animations
            floatingActionMenuActiveMaskView.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.in_to_bottom_right));
            floatingActionMenuActiveMaskView.setVisibility(View.GONE);
        }
    }

    @Override
    public void createNewReceiptViaCamera() {
        imageUri = new CameraInteractionController(this).takePhoto();
    }

    @Override
    public void createNewReceiptViaPlainText() {
        scrollToStart();
        navigationHandler.navigateToCreateNewReceiptFragment(trip, null, null);
    }

    @Override
    public void createNewReceiptViaFileImport() {
        final ImportPhotoPdfDialogFragment fragment = new ImportPhotoPdfDialogFragment();
        fragment.show(getChildFragmentManager(), ImportPhotoPdfDialogFragment.TAG);
    }

    @NonNull
    @Override
    public Observable<Boolean> getCreateNewReceiptMenuButtonToggles() {
        return RxFloatingActionMenu.toggleChanges(floatingActionMenu);
    }

    @NonNull
    @Override
    public Observable<Object> getCreateNewReceiptFromCameraButtonClicks() {
        return RxView.clicks(receiptActionCameraButton);
    }

    @NonNull
    @Override
    public Observable<Object> getCreateNewReceiptFromImportedFileButtonClicks() {
        return RxView.clicks(receiptActionImportButton);
    }

    @NonNull
    @Override
    public Observable<Object> getCreateNewReceiptFromPlainTextButtonClicks() {
        return RxView.clicks(receiptActionTextButton);
    }

    @Override
    public void displayOcrStatus(@NonNull UiIndicator<String> ocrStatusIndicator) {
        if (ocrStatusIndicator.getState() == UiIndicator.State.Loading) {
            if (alert == null) {
                alerter.setText(ocrStatusIndicator.getData().get());
                alert = alerter.show();
                alert.setEnableInfiniteDuration(true);
            } else {
                alert.setText(ocrStatusIndicator.getData().get());
            }
        } else if (alert != null) {
            alert.hide();
            alert = null;
        }
    }

    @Override
    public void setImageUri(@NonNull Uri uri) {
        imageUri = uri;
    }

    private void attachImportIntent(Receipt receipt) {
        compositeDisposable.add(intentImportProcessor.getLastResult()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .flatMapSingle(intentImportResult -> {
                    final AttachmentSendFileImporter importer = new AttachmentSendFileImporter(requireActivity(), trip, persistenceManager, receiptTableController, analytics);
                    return importer.importAttachment(intentImportResult, receipt);
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(file -> {

                }, throwable -> Toast.makeText(getActivity(), R.string.database_error, Toast.LENGTH_SHORT).show()));
    }

    private class ActionBarSubtitleUpdatesListener extends StubTableEventsListener<Trip> {

        @Override
        public void onGetSuccess(@NonNull List<Trip> list) {
            if (isAdded()) {
                updateActionBarTitle(getUserVisibleHint());
            }
        }
    }

    private String getFlexString(int id) {
        return getFlexString(flex, id);
    }

}
