package com.ant.liao;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;

public class GifDrawable extends Drawable implements GifAction, GifReDraw {

	/** gif解码器 */
	private GifDecoder gifDecoder = null;

	private GifImageType animationType = GifImageType.SYNC_DECODER;
	/** 动画显示的次数 */
	private int loopNum = -1;

	private int currentLoop = 0;

	private GifAnimation animation = null;

	private boolean animationRun = false;

	private boolean isLoop = false;

	private boolean singleFrame = false;

	/** 当前要画的帧的图 */
	private Bitmap currentImage = null;

	private GifListener listener = null;
	
	private int iListenerType = 0;
	
	private int currentFrame = 0;

	public GifDrawable() {
		mBitmapState = new BitmapState((Bitmap) null);
        mBitmapState.mTargetDensity = mTargetDensity;
        animation = new GifAnimation();
		animation.setRedraw(this);
	}
	
	private void init() {
		stopDrawThread();
		if (currentImage != null) {
			currentImage = null;
		}
		if (gifDecoder != null) {
			stopDecodeThread();
			gifDecoder.destroy();
			gifDecoder = null;
		}
		currentLoop = 0;
		gifDecoder = new GifDecoder(this);
		if (isLoop) {
			gifDecoder.setLoopAnimation();
		}
	}

	/**
	 * 以字节形式设置动画数据<br>
	 * 如果图片太大，建议<font color=red>不要</font>采用本方法来设置
	 * @param gif
	 */
	public void setGifImage(byte[] gif) {
		init();
		gifDecoder.setGifImage(gif);
		gifDecoder.start();
	}

	/**
	 * 以资源的形式设置动画数据
	 * @param rs
	 * @param resId
	 */
	public void setGifImage(Resources rs, int resId) {
		init();
		gifDecoder.setGifImage(rs, resId);
		gifDecoder.start();
	}

	/**
	 * 以文件形式设置动画数据
	 * @param strFileName
	 */
	public void setGifImage(String strFileName) {
		init();
		gifDecoder.setGifImage(strFileName);
		gifDecoder.start();
	}

	/**
	 * 资源清理<br>
	 * 在不再使用Gif或退出时，务必调用本方法
	 */
	public void destroy() {
		stopDrawThread();
		stopDecodeThread();
		animation.destroy();
		gifDecoder.destroy();
		gifDecoder = null;
		animation = null;
	}

	/**
	 * 设置动画事件回调<br>gif动画有二个事件会回调：1.播放次数，2当前播放的帧数
	 * @param listener
	 * @param iType 见GifListner中的定义
	 */
	public void setListener(GifListener listener,int iType) {
		this.listener = listener;
		if(iType >= GifListener.LOOP_ONLY && iType <= GifListener.LOOP_AND_FRAME_COUNT){
			iListenerType = iType;
		}
	}

	/**
	 * 设置动画播放的次数，必须大于1<br>
	 * 本方法优先级大于setLoopAnimation
	 * @param num
	 */
	public void setLoopNumber(int num) {
		if (num > 1) {
			loopNum = num;
			setLoopAnimation();
		}
	}

	/**
	 * 设置动画循环播放<br>
	 * 默认动画只播放一次
	 */
	public void setLoopAnimation() {
		isLoop = true;
		if (gifDecoder != null) {
			gifDecoder.setLoopAnimation();
		}
	}

	/**
	 * 继续显示动画<br>
	 * 在暂停动画后，通过本方法来让动画继续
	 */
	public void restartGifAnimation() {
		if (singleFrame)
			return;
		if (animationRun) {
			animation.restartAnimation();
		}
	}

	/**
	 * 暂停动画<br>
	 * 在onPause中，最好调用本方法来让动画暂停
	 */
	public void pauseGifAnimation() {
		if (singleFrame)
			return;
		animation.pauseAnimation();
	}

	private void stopDecodeThread() {
		if (gifDecoder != null && gifDecoder.getState() != Thread.State.TERMINATED) {
			//gifDecoder.free();
			gifDecoder.interrupt();
		}
	}

	/**
	 * 中断动画线程
	 */
	private void stopDrawThread() {
		if (singleFrame)
			return;
		animation.stopAnimation();
		animationRun = false;
	}

	private void reAnimation() {
		if (singleFrame)
			return;
		stopDrawThread();
		currentLoop = 0;
		animation.runAnimation();
	}

	private int getCurrentFrame() {
		if (gifDecoder != null) {
			GifFrame frame = gifDecoder.next();
			if (frame == null) {
				return -1;
			}
			if (frame.image != null) {
				currentImage = frame.image;
				BitmapState mb = new BitmapState(currentImage);
				mTargetDensity = mb.mTargetDensity;
		        mBitmapState.mTargetDensity = mTargetDensity;
			}
			return frame.delay;
		} else {
			return -1;
		}
	}

	/*
	 * @hide
	 */
	public int reDraw() {
		int delay = getCurrentFrame();
		drawImage();
		return delay;

	}

	private void drawImage() {
		if (currentImage == null || (currentImage != null && currentImage.isRecycled() == false)) {
			setBitmap(currentImage);
			if(listener != null && (iListenerType == GifListener.FRAME_COUNT_ONLY || iListenerType == GifListener.LOOP_AND_FRAME_COUNT )){
				currentFrame++;
				listener.frameCount(currentFrame);
			}
			invalidateSelf();
		}
	}

	private Handler redrawHandler = new Handler() {
		public void handleMessage(Message msg) {
			try {
				drawImage();
			} catch (Exception ex) {
				Log.e("GifView", ex.toString());
			}
		}
	};

	/**
	 * @hide
	 */
	public void parseReturn(int iResult) {
		switch (iResult) {
		case RETURN_FIRST:
			Log.d("parseReturn", "FIRST");
			if (animationType == GifImageType.COVER || animationType == GifImageType.SYNC_DECODER) {
				currentImage = gifDecoder.getFrameImage();
				invalidateImage();
			}

			break;
		case RETURN_FINISH:
			Log.d("parseReturn", "FINISH");
			if (gifDecoder.getFrameCount() == 1) {
				// 如果是单张图，停止所有的线程
				currentImage = gifDecoder.getFrameImage();
				invalidateImage();
				stopDrawThread();
				stopDecodeThread();
				singleFrame = true;
			} else {
				if (animationRun == false) {
					reAnimation();
					animationRun = true;
				}
			}
			break;
		case RETURN_CACHE_FINISH:
			Log.d("parseReturn", "CACHE_FINISH");
			if (animationRun == false) {
				reAnimation();
				animationRun = true;
			}
			break;
		case RETURN_ERROR:
			Log.d("parseReturn", "ERROR");
			break;
		}
	}

	private void invalidateImage() {
		//int delay = getCurrentFrame();
		if (redrawHandler != null) {
			Message msg = redrawHandler.obtainMessage();
			redrawHandler.sendMessage(msg);
		}
		//return delay;
	}

	@Override
	/**
	 * @hide
	 */
	public void loopEnd() {
		currentLoop += 1;
		if (loopNum > 0) {
			if (currentLoop >= loopNum) {
				stopDrawThread();
				stopDecodeThread();
			}
		}
		if (listener != null) {
			if( iListenerType == GifListener.LOOP_ONLY || iListenerType == GifListener.LOOP_AND_FRAME_COUNT )
				listener.gifEnd(currentLoop);
			currentFrame = 0;
		}
	}

	private static final int DEFAULT_PAINT_FLAGS =
            Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG;
    private BitmapState mBitmapState;
    private Bitmap mBitmap;
    private int mTargetDensity;

    private final Rect mDstRect = new Rect();   // Gravity.apply() sets this

    private boolean mApplyGravity;
    private boolean mMutated;
    
     // These are scaled to match the target density.
    private int mBitmapWidth;
    private int mBitmapHeight;
    
  

    /**
     * Returns the paint used to render this drawable.
     */
    public final Paint getPaint() {
        return mBitmapState.mPaint;
    }

    /**
     * Returns the bitmap used by this drawable to render. May be null.
     */
    public final Bitmap getBitmap() {
        return mBitmap;
    }

    private void computeBitmapSize() {
        mBitmapWidth = mBitmap.getScaledWidth(mTargetDensity);
        mBitmapHeight = mBitmap.getScaledHeight(mTargetDensity);
    }
    
    private void setBitmap(Bitmap bitmap) {
        if (bitmap != mBitmap) {
            mBitmap = bitmap;
            if (bitmap != null) {
            	if(mTargetDensity == 0){
            		mTargetDensity = mBitmap.getDensity();
            	}
                computeBitmapSize();
            } else {
                mBitmapWidth = mBitmapHeight = -1;
            }
            mApplyGravity = true;
            invalidateSelf();
        }
    }

    /**
     * Set the density scale at which this drawable will be rendered. This
     * method assumes the drawable will be rendered at the same density as the
     * specified canvas.
     *
     * @param canvas The Canvas from which the density scale must be obtained.
     *
     * @see android.graphics.Bitmap#setDensity(int)
     * @see android.graphics.Bitmap#getDensity()
     */
    public void setTargetDensity(Canvas canvas) {
        setTargetDensity(canvas.getDensity());
    }

    /**
     * Set the density scale at which this drawable will be rendered.
     *
     * @param metrics The DisplayMetrics indicating the density scale for this drawable.
     *
     * @see android.graphics.Bitmap#setDensity(int)
     * @see android.graphics.Bitmap#getDensity()
     */
    public void setTargetDensity(DisplayMetrics metrics) {
        setTargetDensity(metrics.densityDpi);
    }

    /**
     * Set the density at which this drawable will be rendered.
     *
     * @param density The density scale for this drawable.
     *
     * @see android.graphics.Bitmap#setDensity(int)
     * @see android.graphics.Bitmap#getDensity()
     */
    public void setTargetDensity(int density) {
        if (mTargetDensity != density) {
            mTargetDensity = density == 0 ? DisplayMetrics.DENSITY_DEFAULT : density;
            if (mBitmap != null) {
                computeBitmapSize();
            }
            invalidateSelf();
        }
    }

    /** Get the gravity used to position/stretch the bitmap within its bounds.
     * See android.view.Gravity
     * @return the gravity applied to the bitmap
     */
    public int getGravity() {
        return mBitmapState.mGravity;
    }
    
    /** Set the gravity used to position/stretch the bitmap within its bounds.
        See android.view.Gravity
     * @param gravity the gravity
     */
    public void setGravity(int gravity) {
        if (mBitmapState.mGravity != gravity) {
            mBitmapState.mGravity = gravity;
            mApplyGravity = true;
            invalidateSelf();
        }
    }

    /**
     * Enables or disables anti-aliasing for this drawable. Anti-aliasing affects
     * the edges of the bitmap only so it applies only when the drawable is rotated.
     * 
     * @param aa True if the bitmap should be anti-aliased, false otherwise.
     */
    public void setAntiAlias(boolean aa) {
        mBitmapState.mPaint.setAntiAlias(aa);
        invalidateSelf();
    }
    
    @Override
    public void setFilterBitmap(boolean filter) {
        mBitmapState.mPaint.setFilterBitmap(filter);
        invalidateSelf();
    }

    @Override
    public void setDither(boolean dither) {
        mBitmapState.mPaint.setDither(dither);
        invalidateSelf();
    }

    /**
     * Indicates the repeat behavior of this drawable on the X axis.
     * 
     * @return {@link Shader.TileMode#CLAMP} if the bitmap does not repeat,
     *         {@link Shader.TileMode#REPEAT} or {@link Shader.TileMode#MIRROR} otherwise.
     */
    public Shader.TileMode getTileModeX() {
        return mBitmapState.mTileModeX;
    }

    /**
     * Indicates the repeat behavior of this drawable on the Y axis.
     * 
     * @return {@link Shader.TileMode#CLAMP} if the bitmap does not repeat,
     *         {@link Shader.TileMode#REPEAT} or {@link Shader.TileMode#MIRROR} otherwise.
     */    
    public Shader.TileMode getTileModeY() {
        return mBitmapState.mTileModeY;
    }

    /**
     * Sets the repeat behavior of this drawable on the X axis. By default, the drawable
     * does not repeat its bitmap. Using {@link Shader.TileMode#REPEAT} or
     * {@link Shader.TileMode#MIRROR} the bitmap can be repeated (or tiled) if the bitmap
     * is smaller than this drawable.
     * 
     * @param mode The repeat mode for this drawable.
     * 
     * @see #setTileModeY(android.graphics.Shader.TileMode) 
     * @see #setTileModeXY(android.graphics.Shader.TileMode, android.graphics.Shader.TileMode) 
     */
    public void setTileModeX(Shader.TileMode mode) {
        setTileModeXY(mode, mBitmapState.mTileModeY);
    }

    /**
     * Sets the repeat behavior of this drawable on the Y axis. By default, the drawable
     * does not repeat its bitmap. Using {@link Shader.TileMode#REPEAT} or
     * {@link Shader.TileMode#MIRROR} the bitmap can be repeated (or tiled) if the bitmap
     * is smaller than this drawable.
     * 
     * @param mode The repeat mode for this drawable.
     * 
     * @see #setTileModeX(android.graphics.Shader.TileMode) 
     * @see #setTileModeXY(android.graphics.Shader.TileMode, android.graphics.Shader.TileMode) 
     */    
    public final void setTileModeY(Shader.TileMode mode) {
        setTileModeXY(mBitmapState.mTileModeX, mode);
    }

    /**
     * Sets the repeat behavior of this drawable on both axis. By default, the drawable
     * does not repeat its bitmap. Using {@link Shader.TileMode#REPEAT} or
     * {@link Shader.TileMode#MIRROR} the bitmap can be repeated (or tiled) if the bitmap
     * is smaller than this drawable.
     * 
     * @param xmode The X repeat mode for this drawable.
     * @param ymode The Y repeat mode for this drawable.
     * 
     * @see #setTileModeX(android.graphics.Shader.TileMode)
     * @see #setTileModeY(android.graphics.Shader.TileMode) 
     */
    public void setTileModeXY(Shader.TileMode xmode, Shader.TileMode ymode) {
        final BitmapState state = mBitmapState;
        if (state.mTileModeX != xmode || state.mTileModeY != ymode) {
            state.mTileModeX = xmode;
            state.mTileModeY = ymode;
            state.mRebuildShader = true;
            invalidateSelf();
        }
    }

    @Override
    public int getChangingConfigurations() {
        return super.getChangingConfigurations() | mBitmapState.mChangingConfigurations;
    }
    
    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        mApplyGravity = true;
    }

    @Override
    public void draw(Canvas canvas) {
        Bitmap bitmap = mBitmap;
        if (bitmap != null) {
            final BitmapState state = mBitmapState;
            if (state.mRebuildShader) {
                Shader.TileMode tmx = state.mTileModeX;
                Shader.TileMode tmy = state.mTileModeY;

                if (tmx == null && tmy == null) {
                    state.mPaint.setShader(null);
                } else {
                    state.mPaint.setShader(new BitmapShader(bitmap,
                            tmx == null ? Shader.TileMode.CLAMP : tmx,
                            tmy == null ? Shader.TileMode.CLAMP : tmy));
                }
                state.mRebuildShader = false;
                copyBounds(mDstRect);
            }

            Shader shader = state.mPaint.getShader();
            if (shader == null) {
                if (mApplyGravity) {
                	Gravity.apply(state.mGravity, mBitmapWidth, mBitmapHeight,
                            getBounds(), mDstRect);
//                	Rect container = getBounds();
//                	mDstRect.left = container.left + ((container.right - container.left - mBitmapWidth)/2);
//                	mDstRect.right = mDstRect.left + mBitmapWidth;
//                	mDstRect.top = container.top + ((container.bottom - container.top - mBitmapHeight)/2);
//                	mDstRect.bottom = mDstRect.top + mBitmapHeight;
                    mApplyGravity = false;
                }
                canvas.drawBitmap(bitmap, null, mDstRect, state.mPaint);
            } else {
                if (mApplyGravity) {
                    copyBounds(mDstRect);
                    mApplyGravity = false;
                }
                canvas.drawRect(mDstRect, state.mPaint);
            }
        }
    }

    @Override
    public void setAlpha(int alpha) {
        int oldAlpha = mBitmapState.mPaint.getAlpha();
        if (alpha != oldAlpha) {
            mBitmapState.mPaint.setAlpha(alpha);
            invalidateSelf();
        }
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        mBitmapState.mPaint.setColorFilter(cf);
        invalidateSelf();
    }

    /**
     * A mutable BitmapDrawable still shares its Bitmap with any other Drawable
     * that comes from the same resource.
     *
     * @return This drawable.
     */
    @Override
    public Drawable mutate() {
        if (!mMutated && super.mutate() == this) {
            mBitmapState = new BitmapState(mBitmapState);
            mMutated = true;
        }
        return this;
    }

    @Override
    public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs)
            throws XmlPullParserException, IOException {
        super.inflate(r, parser, attrs);
    }

    @Override
    public int getIntrinsicWidth() {
        return mBitmapWidth;
    }

    @Override
    public int getIntrinsicHeight() {
        return mBitmapHeight;
    }

    @Override
    public int getOpacity() {
        if (mBitmapState.mGravity != Gravity.FILL) {
            return PixelFormat.TRANSLUCENT;
        }
        Bitmap bm = mBitmap;
        return (bm == null || bm.hasAlpha() || mBitmapState.mPaint.getAlpha() < 255) ?
                PixelFormat.TRANSLUCENT : PixelFormat.OPAQUE;
    }

    @Override
    public final ConstantState getConstantState() {
        mBitmapState.mChangingConfigurations = getChangingConfigurations();
        return mBitmapState;
    }

    final static class BitmapState extends ConstantState {
        Bitmap mBitmap;
        int mChangingConfigurations;
        int mGravity = Gravity.FILL;
        Paint mPaint = new Paint(DEFAULT_PAINT_FLAGS);
        Shader.TileMode mTileModeX = null;
        Shader.TileMode mTileModeY = null;
        int mTargetDensity = DisplayMetrics.DENSITY_DEFAULT;
        boolean mRebuildShader;

        BitmapState(Bitmap bitmap) {
            mBitmap = bitmap;
        }

        BitmapState(BitmapState bitmapState) {
            this(bitmapState.mBitmap);
            mChangingConfigurations = bitmapState.mChangingConfigurations;
            mGravity = bitmapState.mGravity;
            mTileModeX = bitmapState.mTileModeX;
            mTileModeY = bitmapState.mTileModeY;
            mTargetDensity = bitmapState.mTargetDensity;
            mPaint = new Paint(bitmapState.mPaint);
            mRebuildShader = bitmapState.mRebuildShader;
        }

        @Override
        public Drawable newDrawable() {
            return new GifDrawable(this, null);
        }

        @Override
        public Drawable newDrawable(Resources res) {
            return new GifDrawable(this, res);
        }

        @Override
        public int getChangingConfigurations() {
            return mChangingConfigurations;
        }
    }

    private GifDrawable(BitmapState state, Resources res) {
        mBitmapState = state;
        if (res != null) {
            mTargetDensity = res.getDisplayMetrics().densityDpi;
        } else {
            mTargetDensity = state.mTargetDensity;
        }
        setBitmap(state != null ? state.mBitmap : null);
    }

}
