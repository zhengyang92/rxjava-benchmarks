package co.smartreceipts.android.fragments;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.media.ExifInterface;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.squareup.picasso.Callback;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;

import java.io.File;

import javax.inject.Inject;

import co.smartreceipts.android.R;
import co.smartreceipts.android.activities.NavigationHandler;
import co.smartreceipts.android.analytics.Analytics;
import co.smartreceipts.android.analytics.events.Events;
import co.smartreceipts.android.imports.CameraInteractionController;
import co.smartreceipts.android.imports.importer.ActivityFileResultImporter;
import co.smartreceipts.android.imports.locator.ActivityFileResultLocator;
import co.smartreceipts.android.model.Receipt;
import co.smartreceipts.android.model.factory.ReceiptBuilderFactory;
import co.smartreceipts.android.ocr.OcrManager;
import co.smartreceipts.android.persistence.PersistenceManager;
import co.smartreceipts.android.persistence.database.controllers.impl.ReceiptTableController;
import co.smartreceipts.android.persistence.database.controllers.impl.StubTableEventsListener;
import co.smartreceipts.android.persistence.database.operations.DatabaseOperationMetadata;
import co.smartreceipts.android.persistence.database.operations.OperationFamilyType;
import co.smartreceipts.android.utils.IntentUtils;
import co.smartreceipts.android.utils.log.Logger;
import dagger.android.support.AndroidSupportInjection;
import io.reactivex.disposables.CompositeDisposable;
import wb.android.flex.Flex;
import wb.android.image.ImageUtils;
import wb.android.storage.StorageManager;
import wb.android.ui.PinchToZoomImageView;

public class ReceiptImageFragment extends WBFragment {

    // Save state
    private static final String KEY_OUT_RECEIPT = "key_out_receipt";
    private static final String KEY_OUT_URI = "key_out_uri";

    @Inject
    Flex flex;
    @Inject
    PersistenceManager persistenceManager;
    @Inject
    Analytics analytics;
    @Inject
    ReceiptTableController receiptTableController;
    @Inject
    OcrManager ocrManager;
    @Inject
    NavigationHandler navigationHandler;

    @Inject
    ActivityFileResultLocator activityFileResultLocator;
    @Inject
    ActivityFileResultImporter activityFileResultImporter;

    private PinchToZoomImageView imageView;
    private LinearLayout footer;
    private ProgressBar progress;
    private Toolbar toolbar;

    private Receipt receipt;
    private ImageUpdatedListener imageUpdatedListener;
    private CompositeDisposable compositeDisposable;
    private boolean isRotateOngoing;
    private Uri imageUri;

    public static ReceiptImageFragment newInstance() {
        return new ReceiptImageFragment();
    }

    @Override
    public void onAttach(Context context) {
        AndroidSupportInjection.inject(this);
        super.onAttach(context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            receipt = getArguments().getParcelable(Receipt.PARCEL_KEY);
        } else {
            receipt = savedInstanceState.getParcelable(KEY_OUT_RECEIPT);
            imageUri = savedInstanceState.getParcelable(KEY_OUT_URI);
        }
        isRotateOngoing = false;
        imageUpdatedListener = new ImageUpdatedListener();
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.receipt_image_view, container, false);
        imageView = rootView.findViewById(R.id.receiptimagefragment_imageview);
        footer = rootView.findViewById(R.id.footer);
        progress = rootView.findViewById(R.id.progress);

        final LinearLayout rotateCCW = rootView.findViewById(R.id.rotate_ccw);
        final LinearLayout retakePhoto = rootView.findViewById(R.id.retake_photo);
        final LinearLayout rotateCW = rootView.findViewById(R.id.rotate_cw);

        rotateCCW.setOnClickListener(view -> {
            analytics.record(Events.Receipts.ReceiptImageViewRotateCcw);
            rotate(ExifInterface.ORIENTATION_ROTATE_270);
        });
        retakePhoto.setOnClickListener(view -> {
            analytics.record(Events.Receipts.ReceiptImageViewRetakePhoto);
            imageUri = new CameraInteractionController(ReceiptImageFragment.this).retakePhoto(receipt);
        });
        rotateCW.setOnClickListener(view -> {
            analytics.record(Events.Receipts.ReceiptImageViewRotateCw);
            rotate(ExifInterface.ORIENTATION_ROTATE_90);
        });
        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadImage();
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        toolbar = getActivity().findViewById(R.id.toolbar);
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        Logger.debug(this, "Result Code: " + resultCode);
        if (receipt == null) {
            receipt = getArguments().getParcelable(Receipt.PARCEL_KEY);
        }

        // Show the progress bar
        progress.setVisibility(View.VISIBLE);

        // Null out the last request
        final Uri cachedImageSaveLocation = imageUri;
        imageUri = null;

        activityFileResultLocator.onActivityResult(requestCode, resultCode, data, cachedImageSaveLocation);
    }

    private void subscribe() {
        compositeDisposable = new CompositeDisposable();

        compositeDisposable.add(activityFileResultLocator.getUriStream()
                // uri always has SCHEME_CONTENT -> we don't need to check permissions
                .subscribe(locatorResponse -> {
                    if (!locatorResponse.getThrowable().isPresent()) {
                        progress.setVisibility(View.VISIBLE);
                        activityFileResultImporter.importFile(locatorResponse.getRequestCode(),
                                locatorResponse.getResultCode(), locatorResponse.getUri(), receipt.getTrip());
                    } else {
                        Toast.makeText(getActivity(), getFlexString(R.string.FILE_SAVE_ERROR), Toast.LENGTH_SHORT).show();
                        progress.setVisibility(View.GONE);
                        activityFileResultLocator.markThatResultsWereConsumed();
                    }
                }));

        compositeDisposable.add(activityFileResultImporter.getResultStream()
                .subscribe(response -> {
                    if (!response.getThrowable().isPresent()) {
                        final Receipt retakeReceipt = new ReceiptBuilderFactory(receipt).setFile(response.getFile()).build();
                        receiptTableController.update(receipt, retakeReceipt, new DatabaseOperationMetadata());
                    } else {
                        Toast.makeText(getActivity(), getFlexString(R.string.IMG_SAVE_ERROR), Toast.LENGTH_SHORT).show();
                    }
                    progress.setVisibility(View.GONE);
                    activityFileResultLocator.markThatResultsWereConsumed();
                    activityFileResultImporter.markThatResultsWereConsumed();
                }));
    }

    @Override
    public void onResume() {
        super.onResume();
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(receipt.getName());
        }
        receiptTableController.subscribe(imageUpdatedListener);

        subscribe();

    }

    @Override
    public void onPause() {
        receiptTableController.unsubscribe(imageUpdatedListener);

        compositeDisposable.clear();

        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Logger.debug(this, "onSaveInstanceState");
        outState.putParcelable(KEY_OUT_RECEIPT, receipt);
        outState.putParcelable(KEY_OUT_URI, imageUri);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_share, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                navigationHandler.navigateBack();
                return true;
            case R.id.action_share:
                if (receipt.getFile() != null) {
                    final Intent sendIntent = IntentUtils.getSendIntent(getActivity(), receipt.getFile());
                    startActivity(Intent.createChooser(sendIntent, getResources().getString(R.string.send_email)));
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void loadImage() {
        if (receipt.getImage() != null) {
            Picasso.get().load(receipt.getImage()).memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE).fit().centerInside().into(imageView, new Callback() {
                @Override
                public void onSuccess() {
                    progress.setVisibility(View.GONE);
                    imageView.setVisibility(View.VISIBLE);
                    footer.setVisibility(View.VISIBLE);
                }

                @Override
                public void onError(Exception e) {
                    progress.setVisibility(View.GONE);
                    if (getActivity() != null) {
                        Toast.makeText(getActivity(), getFlexString(R.string.IMG_OPEN_ERROR), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void rotate(int orientation) {
        if (isRotateOngoing) {
            return;
        }
        isRotateOngoing = true;
        progress.setVisibility(View.VISIBLE);
        (new ImageRotater(orientation, receipt.getImage())).execute();
    }

    private void onRotateComplete(boolean success) {
        if (!success) {
            Toast.makeText(getActivity(), "Image Rotate Failed", Toast.LENGTH_SHORT).show();
        }
        isRotateOngoing = false;
        progress.setVisibility(View.GONE);
    }

    private class ImageUpdatedListener extends StubTableEventsListener<Receipt> {

        @Override
        public void onUpdateSuccess(@NonNull Receipt oldReceipt, @NonNull Receipt newReceipt, @NonNull DatabaseOperationMetadata databaseOperationMetadata) {
            if (databaseOperationMetadata.getOperationFamilyType() != OperationFamilyType.Sync) {
                if (oldReceipt.equals(receipt)) {
                    receipt = newReceipt;
                    loadImage();
                }
            }
        }

        @Override
        public void onUpdateFailure(@NonNull Receipt oldReceipt, @Nullable Throwable e, @NonNull DatabaseOperationMetadata databaseOperationMetadata) {
            if (databaseOperationMetadata.getOperationFamilyType() != OperationFamilyType.Sync) {
                progress.setVisibility(View.GONE);
                Toast.makeText(getActivity(), getFlexString(R.string.database_error), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class ImageRotater extends AsyncTask<Void, Void, Bitmap> {

        private final int mOrientation;
        private final File mImg;

        public ImageRotater(int orientation, File img) {
            mOrientation = orientation;
            mImg = img;
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            try {
                StorageManager storage = persistenceManager.getStorageManager();
                File root = mImg.getParentFile();
                String filename = mImg.getName();
                Bitmap bitmap = storage.getBitmap(root, filename);
                bitmap = ImageUtils.rotateBitmap(bitmap, mOrientation);
                storage.writeBitmap(root, bitmap, filename, CompressFormat.JPEG, 85);
                return bitmap;
            } catch (Exception e) {
                Logger.error(this, e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (result == null) {
                onRotateComplete(false);
            } else {
                imageView.setImageBitmap(result);
                onRotateComplete(true);
            }
        }
    }

    private String getFlexString(int id) {
        return getFlexString(flex, id);
    }

}