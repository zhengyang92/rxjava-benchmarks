package co.smartreceipts.android.workers.reports.pdf.renderer.imagex.compat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * <p>
 * This class enables rendering a PDF document. This class is not thread safe.
 * </p>
 * <p>
 * If you want to render a PDF, you create a renderer and for every page you want
 * to render, you open the page, render it, and close the page. After you are done
 * with rendering, you close the renderer. After the renderer is closed it should not
 * be used anymore. Note that the pages are rendered one by one, i.e. you can have
 * only a single page opened at any given time.
 * </p>
 * <p>
 * A typical use of the APIs to render a PDF looks like this:
 * </p>
 * <pre>
 * // create a new renderer
 * PdfRenderer renderer = new PdfRenderer(getSeekableFileDescriptor());
 *
 * // let us just render all pages
 * final int pageCount = renderer.getPageCount();
 * for (int i = 0; i < pageCount; i++) {
 *     Page page = renderer.openPage(i);
 *
 *     // say we render for showing on the screen
 *     page.render(mBitmap, null, null, Page.RENDER_MODE_FOR_DISPLAY);
 *
 *     // do stuff with the bitmap
 *
 *     // close the page
 *     page.close();
 * }
 *
 * // close the renderer
 * renderer.close();
 * </pre>
 *
 * <h3>Print preview and print output</h3>
 * <p>
 * If you are using this class to rasterize a PDF for printing or show a print
 * preview, it is recommended that you respect the following contract in order
 * to provide a consistent user experience when seeing a preview and printing,
 * i.e. the user sees a preview that is the same as the printout.
 * </p>
 * <ul>
 * <li>
 * Respect the property whether the document would like to be scaled for printing
 * as per {@link #shouldScaleForPrinting()}.
 * </li>
 * <li>
 * When scaling a document for printing the aspect ratio should be preserved.
 * </li>
 * <li>
 * Do not inset the content with any margins from the {@link android.print.PrintAttributes}
 * as the application is responsible to render it such that the margins are respected.
 * </li>
 * <li>
 * If document page size is greater than the printed media size the content should
 * be anchored to the upper left corner of the page for left-to-right locales and
 * top right corner for right-to-left locales.
 * </li>
 * </ul>
 *
 * @see #close()
 */
public final class PdfRendererCompat implements AutoCloseable {

    private final Point mTempPoint = new Point();

    private final int pageCount;

    /**
     * Pdfium core for loading and rendering PDFs
     */
    private final PdfiumCore pdfiumCore;

    private PdfDocument pdfDocument;

    private ParcelFileDescriptor input;

    private Page mCurrentPage;

    /** @hide */
    @IntDef({
            Page.RENDER_MODE_FOR_DISPLAY,
            Page.RENDER_MODE_FOR_PRINT
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface RenderMode {}

    /**
     * Creates a new instance.
     * <p>
     * <strong>Note:</strong> The provided file descriptor must be <strong>seekable</strong>,
     * i.e. its data being randomly accessed, e.g. pointing to a file.
     * </p>
     * <p>
     * <strong>Note:</strong> This class takes ownership of the passed in file descriptor
     * and is responsible for closing it when the renderer is closed.
     * </p>
     * <p>
     * If the file is from an untrusted source it is recommended to run the renderer in a separate,
     * isolated process with minimal permissions to limit the impact of security exploits.
     * </p>
     *
     * @param context the context
     * @param input Seekable file descriptor to read from.
     *
     * @throws java.io.IOException If an error occurs while reading the file.
     * @throws java.lang.SecurityException If the file requires a password or
     *         the security scheme is not supported.
     */
    public PdfRendererCompat(@NonNull Context context, @NonNull ParcelFileDescriptor input) throws IOException {
        if (context == null) {
            throw new NullPointerException("context cannot be null");
        }
        if (input == null) {
            throw new NullPointerException("input cannot be null");
        }

        this.input = input;
        this.pdfiumCore = new PdfiumCore(context);
        this.pdfDocument = pdfiumCore.newDocument(input);
        this.pageCount = pdfiumCore.getPageCount(pdfDocument);
    }

    /**
     * Closes this renderer. You should not use this instance
     * after this method is called.
     */
    public void close() {
        throwIfClosed();
        throwIfPageOpened();
        doClose();
    }

    /**
     * Gets the number of pages in the document.
     *
     * @return The page count.
     */
    public int getPageCount() {
        throwIfClosed();
        return pageCount;
    }

    /**
     * Gets whether the document prefers to be scaled for printing.
     * You should take this info account if the document is rendered
     * for printing and the target media size differs from the page
     * size.
     *
     * @return If to scale the document.
     */
    public boolean shouldScaleForPrinting() {
        throwIfClosed();
        return false;
    }

    /**
     * Opens a page for rendering.
     *
     * @param index The page index.
     * @return A page that can be rendered.
     *
     * @see android.graphics.pdf.PdfRenderer.Page#close() PdfRenderer.Page.close()
     */
    public Page openPage(int index) {
        throwIfClosed();
        throwIfPageOpened();
        throwIfPageNotInDocument(index);
        mCurrentPage = new Page(index);
        return mCurrentPage;
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            if (input != null) {
                doClose();
            }
        } finally {
            super.finalize();
        }
    }

    private void doClose() {
        if (mCurrentPage != null) {
            mCurrentPage.close();
        }
        pdfiumCore.closeDocument(pdfDocument);
        try {
            input.close();
        } catch (IOException ioe) {
            /* ignore - best effort */
        }
        input = null;
    }

    private void throwIfClosed() {
        if (input == null) {
            throw new IllegalStateException("Already closed");
        }
    }

    private void throwIfPageOpened() {
        if (mCurrentPage != null) {
            throw new IllegalStateException("Current page not closed");
        }
    }

    private void throwIfPageNotInDocument(int pageIndex) {
        if (pageIndex < 0 || pageIndex >= pageCount) {
            throw new IllegalArgumentException("Invalid page index");
        }
    }

    /**
     * This class represents a PDF document page for rendering.
     */
    public final class Page implements AutoCloseable {

        /**
         * Mode to render the content for display on a screen.
         */
        public static final int RENDER_MODE_FOR_DISPLAY = 1;

        /**
         * Mode to render the content for printing.
         */
        public static final int RENDER_MODE_FOR_PRINT = 2;

        private final int mIndex;
        private final int mWidth;
        private final int mHeight;

        private long mNativePage;

        private Page(int index) {
            mNativePage = pdfiumCore.openPage(pdfDocument, index);
            mIndex = index;
            mWidth = pdfiumCore.getPageWidthPoint(pdfDocument, index);
            mHeight = pdfiumCore.getPageHeightPoint(pdfDocument, index);
        }

        /**
         * Gets the page index.
         *
         * @return The index.
         */
        public int getIndex() {
            return  mIndex;
        }

        /**
         * Gets the page width in points (1/72").
         *
         * @return The width in points.
         */
        public int getWidth() {
            return mWidth;
        }

        /**
         * Gets the page height in points (1/72").
         *
         * @return The height in points.
         */
        public int getHeight() {
            return mHeight;
        }

        /**
         * Renders a page to a bitmap.
         * <p>
         * You may optionally specify a rectangular clip in the bitmap bounds. No rendering
         * outside the clip will be performed, hence it is your responsibility to initialize
         * the bitmap outside the clip.
         * </p>
         * <p>
         * You may optionally specify a matrix to transform the content from page coordinates
         * which are in points (1/72") to bitmap coordinates which are in pixels. If this
         * matrix is not provided this method will apply a transformation that will fit the
         * whole page to the destination clip if provided or the destination bitmap if no
         * clip is provided.
         * </p>
         * <p>
         * The clip and transformation are useful for implementing tile rendering where the
         * destination bitmap contains a portion of the image, for example when zooming.
         * Another useful application is for printing where the size of the bitmap holding
         * the page is too large and a client can render the page in stripes.
         * </p>
         * <p>
         * <strong>Note: </strong> The destination bitmap format must be
         * {@link Bitmap.Config#ARGB_8888 ARGB}.
         * </p>
         * <p>
         * <strong>Note: </strong> The optional transformation matrix must be affine as per
         * {@link Matrix#isAffine() Matrix.isAffine()}. Hence, you can specify
         * rotation, scaling, translation but not a perspective transformation.
         * </p>
         *
         * @param destination Destination bitmap to which to render.
         * @param destClip Optional clip in the bitmap bounds.
         * @param transform Optional transformation to apply when rendering.
         * @param renderMode The render mode.
         *
         * @see #RENDER_MODE_FOR_DISPLAY
         * @see #RENDER_MODE_FOR_PRINT
         */
        public void render(@NonNull Bitmap destination, @Nullable Rect destClip,
                           @Nullable Matrix transform, @RenderMode int renderMode) {
            if (destination.getConfig() != Bitmap.Config.ARGB_8888) {
                throw new IllegalArgumentException("Unsupported pixel format");
            }

            if (destClip != null) {
                if (destClip.left < 0 || destClip.top < 0
                        || destClip.right > destination.getWidth()
                        || destClip.bottom > destination.getHeight()) {
                    throw new IllegalArgumentException("destBounds not in destination");
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (transform != null && !transform.isAffine()) {
                    throw new IllegalArgumentException("transform not affine");
                }
            }

            if (renderMode != RENDER_MODE_FOR_PRINT && renderMode != RENDER_MODE_FOR_DISPLAY) {
                throw new IllegalArgumentException("Unsupported render mode");
            }

            if (renderMode == RENDER_MODE_FOR_PRINT && renderMode == RENDER_MODE_FOR_DISPLAY) {
                throw new IllegalArgumentException("Only single render mode supported");
            }

            final int contentLeft = (destClip != null) ? destClip.left : 0;
            final int contentTop = (destClip != null) ? destClip.top : 0;
            final int contentRight = (destClip != null) ? destClip.right
                    : destination.getWidth();
            final int contentBottom = (destClip != null) ? destClip.bottom
                    : destination.getHeight();

            pdfiumCore.renderPageBitmap(pdfDocument, destination, mIndex, contentLeft, contentTop, contentRight, contentBottom);
        }

        /**
         * Closes this page.
         *
         * @see android.graphics.pdf.PdfRenderer#openPage(int)
         */
        @Override
        public void close() {
            throwIfClosed();
            doClose();
        }

        @Override
        protected void finalize() throws Throwable {
            try {
                if (mNativePage != 0) {
                    doClose();
                }
            } finally {
                super.finalize();
            }
        }

        private void doClose() {
            // TODO: Alter PdfiumCore support lib to allow page by page closing (currently only whole doc at once supported)
            mNativePage = 0;
            mCurrentPage = null;
        }

        private void throwIfClosed() {
            if (mNativePage == 0) {
                throw new IllegalStateException("Already closed");
            }
        }
    }
}

