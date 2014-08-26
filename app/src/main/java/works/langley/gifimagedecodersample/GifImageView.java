package works.langley.gifimagedecodersample;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class GifImageView extends ImageView {
    private static final String TAG = GifImageView.class.getSimpleName();
    private final GifImageView self = this;

    public static final int IMAGE_TYPE_UNKNOWN = 0;
    public static final int IMAGE_TYPE_STATIC = 1;
    public static final int IMAGE_TYPE_DYNAMIC = 2;

    public static final int DECODE_STATUS_UNDECODE = 0;
    public static final int DECODE_STATUS_DECODING = 1;
    public static final int DECODE_STATUS_DECODED = 2;

    private GifImageDecoder mDecoder;
    private Bitmap mBitmap;
    private InputStream mInputStream;

    public int mImageType = IMAGE_TYPE_UNKNOWN;
    public int mDecodeStatus = DECODE_STATUS_UNDECODE;

    private long mTime;
    private int mIndex;

    private int mResId;
    private String mFilePath;

    private boolean isPlaying = false;

    private float mScale = -1;
    private int mOverriddenDensity = -1;
    private static int mOverriddenClassDensity = -1;
    private ScaleType mScaleType;

    public GifImageView(Context context) {
        super(context);
    }

    public GifImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private InputStream getInputStream() {
        if (mInputStream != null)
            return mInputStream;
        if (mFilePath != null)
            try {
                return new FileInputStream(mFilePath);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        if (mResId > 0)
            return getContext().getResources().openRawResource(mResId);
        return null;
    }

    /**
     * set gif file path
     *
     * @param filePath
     */
    public void setGif(String filePath) {
        Bitmap bitmap = BitmapFactory.decodeFile(filePath);
        setGif(filePath, bitmap);
    }

    /**
     * set gif file path and cache mCurrentImage
     *
     * @param filePath
     * @param cacheImage
     */
    public void setGif(String filePath, Bitmap cacheImage) {
        this.mResId = 0;
        this.mFilePath = filePath;
        this.mInputStream = null;
        mImageType = IMAGE_TYPE_UNKNOWN;
        mDecodeStatus = DECODE_STATUS_UNDECODE;
        isPlaying = false;
        mBitmap = cacheImage;
        requestLayout();
    }

    /**
     * set gif resource id
     *
     * @param resId
     */
    public void setGif(int resId) {
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), resId);
        setGif(resId, bitmap);
    }

    /**
     * set gif resource id and cache mCurrentImage
     *
     * @param resId
     * @param cacheImage
     */
    public void setGif(int resId, Bitmap cacheImage) {
        this.mFilePath = null;
        this.mResId = resId;
        this.mInputStream = null;
        mImageType = IMAGE_TYPE_UNKNOWN;
        mDecodeStatus = DECODE_STATUS_UNDECODE;
        isPlaying = false;
        mBitmap = cacheImage;
        requestLayout();
    }

    /**
     * set gif input stream
     *
     * @param inputStream
     */
    public void setGif(InputStream inputStream) {
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
        setGif(inputStream, bitmap);
    }

    /**
     * set gif input stream
     *
     * @param inputStream
     * @param cacheImage
     */
    public void setGif(InputStream inputStream, Bitmap cacheImage) {
        this.mFilePath = null;
        this.mResId = 0;
        this.mInputStream = inputStream;
        mImageType = IMAGE_TYPE_UNKNOWN;
        mDecodeStatus = DECODE_STATUS_UNDECODE;
        isPlaying = false;
        mBitmap = cacheImage;
        requestLayout();
    }

    private void decode() {
        release();
        mIndex = 0;

        mDecodeStatus = DECODE_STATUS_DECODING;

        new Thread() {
            @Override
            public void run() {
                try {
                    mDecoder = new GifImageDecoder();
                    mDecoder.read(getInputStream());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (mDecoder.mWidth == 0 || mDecoder.mHeight == 0) {
                    mImageType = IMAGE_TYPE_STATIC;
                } else {
                    mImageType = IMAGE_TYPE_DYNAMIC;
                }
                postInvalidate();
                mTime = System.currentTimeMillis();
                mDecodeStatus = DECODE_STATUS_DECODED;
            }
        }.start();
    }

    public void release() {
        mDecoder = null;
    }

    @Override
    public void setScaleType(ScaleType scaleType) {
        super.setScaleType(scaleType);
    }

    public float getScale() {
        float targetDensity = getContext().getResources().getDisplayMetrics().densityDpi;
        float displayThisDensity = getDensity();
        mScale = targetDensity / displayThisDensity;
        if (mScale < 0.1f) mScale = 0.1f;
        if (mScale > 5.0f) mScale = 5.0f;
        return mScale;
    }

    public int getDensity() {
        int density;

        // If a custom instance density was set, set the mCurrentImage to this density
        if (mOverriddenDensity > 0) {
            density = mOverriddenDensity;
        } else if (isClassLevelDensitySet()) {
            // If a class level density has been set, set every mCurrentImage to that density
            density = getClassLevelDensity();
        } else {
            // If the instance density was not overridden, get the one from the display
            DisplayMetrics metrics = new DisplayMetrics();

            if (!(getContext() instanceof Activity)) {
                density = DisplayMetrics.DENSITY_HIGH;
            } else {
                Activity activity = (Activity) getContext();
                activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
                density = metrics.densityDpi;
            }
        }

        return density;
    }

    public static boolean isClassLevelDensitySet() {
        return mOverriddenClassDensity != -1;
    }

    public static int getClassLevelDensity() {
        return mOverriddenClassDensity;
    }

    public static void setClassLevelDensity(int classLevelDensity) {
        mOverriddenClassDensity = classLevelDensity;
    }

    public void setDensity(int fixedDensity) {
        mOverriddenDensity = fixedDensity;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mScale = getScale();
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        /**
         * if both mWidth and mHeight are set scale mWidth first. modify in future
         * if necessary
         */
        boolean scaleToWidth = false;
        if (widthMode == MeasureSpec.EXACTLY
                || widthMode == MeasureSpec.AT_MOST) {
            scaleToWidth = true;
        } else if (heightMode == MeasureSpec.EXACTLY
                || heightMode == MeasureSpec.AT_MOST) {
            scaleToWidth = false;
        }

        int iw;
        int ih;

        final Drawable drawable = getDrawable();

        if (drawable != null) {
            iw = drawable.getIntrinsicWidth();
            ih = drawable.getIntrinsicHeight();
            if (iw <= 0) iw = 1;
            if (ih <= 0) ih = 1;
        } else if (mBitmap != null) {
            iw = mBitmap.getWidth();
            ih = mBitmap.getHeight();
            if (iw <= 0) iw = 1;
            if (ih <= 0) ih = 1;
        } else {
            return;
        }

        if (scaleToWidth) {
            int heightC = width * ih / iw;
            if (height > 0)
                if (heightC > height) {
                    // dont let mHeight be greater then set max
                    heightC = height;
                    width = heightC * iw / ih;
                }
            this.setScaleType(ScaleType.CENTER_CROP);
            setMeasuredDimension(width, heightC);

        } else {
            // need to scale to mHeight instead
            int marg = 0;
            if (getParent() != null) {
                if (getParent().getParent() != null) {
                    marg += ((RelativeLayout) getParent().getParent())
                            .getPaddingTop();
                    marg += ((RelativeLayout) getParent().getParent())
                            .getPaddingBottom();
                }
            }
            width = height * iw / ih;
            height -= marg;
            setMeasuredDimension(width, height);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mBitmap != null) {
            float[] gifDrawParams = applyScaleType(canvas);
            if (mDecodeStatus == DECODE_STATUS_UNDECODE) {
                canvas.drawBitmap(mBitmap, gifDrawParams[0], gifDrawParams[1], null);
                if (isPlaying) {
                    decode();
                    invalidate();
                }
            } else if (mDecodeStatus == DECODE_STATUS_DECODING) {
                canvas.drawBitmap(mBitmap, gifDrawParams[0], gifDrawParams[1], null);
                invalidate();
            } else if (mDecodeStatus == DECODE_STATUS_DECODED) {
                if (mImageType == IMAGE_TYPE_STATIC) {
                    canvas.drawBitmap(mBitmap, gifDrawParams[0], gifDrawParams[1], null);
                } else if (mImageType == IMAGE_TYPE_DYNAMIC) {
                    if (isPlaying) {
                        long now = System.currentTimeMillis();

                        if (mTime + mDecoder.getDelay(mIndex) < now) {
                            mTime += mDecoder.getDelay(mIndex);
                            incrementFrameIndex();
                        }
                        Bitmap bitmap = mDecoder.getFrame(mIndex);
                        if (bitmap != null) {
                            canvas.drawBitmap(bitmap, gifDrawParams[0], gifDrawParams[1], null);
                        }
                        invalidate();
                    } else {
                        Bitmap bitmap = mDecoder.getFrame(mIndex);
                        canvas.drawBitmap(bitmap, gifDrawParams[0], gifDrawParams[1], null);
                    }
                } else {
                    canvas.drawBitmap(mBitmap, gifDrawParams[0], gifDrawParams[1], null);
                }
            }
        }
    }

    private void incrementFrameIndex() {
        mIndex++;
        if (mIndex >= mDecoder.getFrameCount()) {
            mIndex = 0;
        }
    }

    private void decrementFrameIndex() {
        mIndex--;
        if (mIndex < 0) {
            mIndex = mDecoder.getFrameCount() - 1;
        }
    }

    public void play() {
        mTime = System.currentTimeMillis();
        isPlaying = true;
        invalidate();
    }

    public void pause() {
        isPlaying = false;
        invalidate();
    }

    public void stop() {
        isPlaying = false;
        mIndex = 0;
        invalidate();
    }

    public void nextFrame() {
        if (mDecodeStatus == DECODE_STATUS_DECODED) {
            incrementFrameIndex();
            invalidate();
        }
    }

    public void prevFrame() {
        if (mDecodeStatus == DECODE_STATUS_DECODED) {
            decrementFrameIndex();
            invalidate();
        }
    }


    /**
     * Applies the scale type of the ImageViewEx to the GIF.
     * Use the returned value to draw the GIF and calculate
     * the right y-offset, if any has to be set.
     *
     * @param canvas The {@link android.graphics.Canvas} to apply the {@link android.widget.ImageView.ScaleType} to.
     * @return A float array containing, for each position:
     * - 0 The x position of the gif
     * - 1 The y position of the gif
     * - 2 The scaling applied to the y-axis
     */
    private float[] applyScaleType(Canvas canvas) {
        // Get the current dimensions of the view and the gif
        float vWidth = getWidth();
        float vHeight = getHeight();
        float gWidth = mBitmap.getWidth() * mScale;
        float gHeight = mBitmap.getHeight() * mScale;

        // Disable the default scaling, it can mess things up
        if (mScaleType == null) {
            mScaleType = getScaleType();
            setScaleType(ScaleType.MATRIX);
        }

        float x = 0;
        float y = 0;
        float s = 1;

        switch (mScaleType) {
            case CENTER:
                /* Center the currentImage in the view, but perform no scaling. */
                x = (vWidth - gWidth) / 2 / mScale;
                y = (vHeight - gHeight) / 2 / mScale;
                break;

            case CENTER_CROP:
                /*
                 * Scale the currentImage uniformly (maintain the currentImage's aspect ratio)
                 * so that both dimensions (mWidth and mHeight) of the currentImage will
                 * be equal to or larger than the corresponding dimension of the
                 * view (minus padding). The currentImage is then centered in the view.
                 */
                float minDimensionCenterCrop = Math.min(gWidth, gHeight);
                if (minDimensionCenterCrop == gWidth) {
                    s = vWidth / gWidth;
                } else {
                    s = vHeight / gHeight;
                }
                x = (vWidth - gWidth * s) / 2 / (s * mScale);
                y = (vHeight - gHeight * s) / 2 / (s * mScale);
                canvas.scale(s, s);
                break;

            case CENTER_INSIDE:
                /*
                 * Scale the currentImage uniformly (maintain the currentImage's aspect ratio)
                 * so that both dimensions (mWidth and mHeight) of the currentImage will
                 * be equal to or less than the corresponding dimension of the
                 * view (minus padding). The currentImage is then centered in the view.
                 */
                // Scaling only applies if the gif is larger than the container!
                if (gWidth > vWidth || gHeight > vHeight) {
                    float maxDimensionCenterInside = Math.max(gWidth, gHeight);
                    if (maxDimensionCenterInside == gWidth) {
                        s = vWidth / gWidth;
                    } else {
                        s = vHeight / gHeight;
                    }
                }
                x = (vWidth - gWidth * s) / 2 / (s * mScale);
                y = (vHeight - gHeight * s) / 2 / (s * mScale);
                canvas.scale(s, s);
                break;

            case FIT_CENTER:
                /*
                 * Compute a scale that will maintain the original src aspect ratio,
                 * but will also ensure that src fits entirely inside dst.
                 * At least one axis (X or Y) will fit exactly.
                 * The result is centered inside dst.
                 */
                // This scale type always scales the gif to the exact dimension of the View
                float maxDimensionFitCenter = Math.max(gWidth, gHeight);
                if (maxDimensionFitCenter == gWidth) {
                    s = vWidth / gWidth;
                } else {
                    s = vHeight / gHeight;
                }
                x = (vWidth - gWidth * s) / 2 / (s * mScale);
                y = (vHeight - gHeight * s) / 2 / (s * mScale);
                canvas.scale(s, s);
                break;

            case FIT_START:
                /*
                 * Compute a scale that will maintain the original src aspect ratio,
                 * but will also ensure that src fits entirely inside dst.
                 * At least one axis (X or Y) will fit exactly.
                 * The result is centered inside dst.
                 */
                // This scale type always scales the gif to the exact dimension of the View
                float maxDimensionFitStart = Math.max(gWidth, gHeight);
                if (maxDimensionFitStart == gWidth) {
                    s = vWidth / gWidth;
                } else {
                    s = vHeight / gHeight;
                }
                x = 0;
                y = 0;
                canvas.scale(s, s);
                break;

            case FIT_END:
                /*
                 * Compute a scale that will maintain the original src aspect ratio,
                 * but will also ensure that src fits entirely inside dst.
                 * At least one axis (X or Y) will fit exactly.
                 * END aligns the result to the right and bottom edges of dst.
                 */
                // This scale type always scales the gif to the exact dimension of the View
                float maxDimensionFitEnd = Math.max(gWidth, gHeight);
                if (maxDimensionFitEnd == gWidth) {
                    s = vWidth / gWidth;
                } else {
                    s = vHeight / gHeight;
                }
                x = (vWidth - gWidth * s) / mScale / s;
                y = (vHeight - gHeight * s) / mScale / s;
                canvas.scale(s, s);
                break;

            case FIT_XY:
                /*
                 * Scale in X and Y independently, so that src matches dst exactly.
                 * This may change the aspect ratio of the src.
                 */
                float sFitX = vWidth / gWidth;
                s = vHeight / gHeight;
                x = 0;
                y = 0;
                canvas.scale(sFitX, s);
                break;
            default:
                break;
        }

        return new float[]{x, y, s};
    }
}