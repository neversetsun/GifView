package com.ant.liao;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

/**
 * GifView，一个显示gif图片的view。<br>
 * gif图片可以是字节，资源或者文件的形式，可以设置播放次数，也可以设置循环播放。在播放过程中可以进行暂停<br>
 * 本类进行了各种优化设计，并且能支持帧数超过100以上的大gif图片的播放。 请注意在适当的时候要调用destroy方法来释放资源<br>
 * 对gifview的其它使用（如设置大小等），和ImageView一样
 * 
 * @author smartliao
 * 
 */
public class GifView extends ImageView implements GifAction, GifReDraw {

	/** gif解码器 */
	private GifDecoder gifDecoder = null;
	/** 当前要画的帧的图 */
	private Bitmap currentImage = null;

	private GifAnimation animation = null;

	private boolean animationRun = false;

	/** 动画显示的次数 */
	private int loopNum = -1;

	private boolean isLoop = false;

	private int currentLoop = 0;

	private int currentFrame = 0;

	private GifListener listener = null;

	private boolean singleFrame = false;

	/** 1返回播放次数事件，2返回播放帧数事件，3返回次数和帧数事件 */
	private int iListenerType = 0;

	private GifImageType animationType = GifImageType.SYNC_DECODER;

	public GifView(Context context) {
		super(context);
		setScaleType(ImageView.ScaleType.FIT_XY);
		animation = new GifAnimation();
		animation.setRedraw(this);
	}

	public GifView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public GifView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setScaleType(ImageView.ScaleType.FIT_XY);
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
	 * 设置图片，并开始解码
	 * 
	 * @param gif
	 *            要设置的图片
	 */
	private void setGifDecoderImage(byte[] gif) {
		init();
		gifDecoder.setGifImage(gif);
		gifDecoder.start();
	}

	private void setGifDecoderImage(Resources rs, int resId) {
		init();
		gifDecoder.setGifImage(rs, resId);
		gifDecoder.start();
	}

	/**
	 * 设置动画事件回调<br>
	 * 如果你想得到动画的事件，通过本方法来设置事件回调。 有二种事件：1.播放次数，2当前播放第几帧
	 * 
	 * @param listener
	 * @param iType
	 *            见GifListner中的定义
	 */
	public void setListener(GifListener listener, int iType) {
		this.listener = listener;
		if (iType >= GifListener.LOOP_ONLY && iType <= GifListener.LOOP_AND_FRAME_COUNT) {
			iListenerType = iType;
		}
	}

	/**
	 * 设置动画播放的次数，必须大于1<br>
	 * 本方法的优先级高于setLoopAnimation，当设置了本方法后，不会管setLoopAnimation的结果
	 * 
	 * @param num
	 *            播放次数
	 */
	public void setLoopNumber(int num) {
		if (num > 1) {
			loopNum = num;
			setLoopAnimation();
		}
	}

	/**
	 * 设置动画是否循环播放<br>
	 * 默认动画只播放一次就结束
	 */
	public void setLoopAnimation() {
		isLoop = true;
		if (gifDecoder != null) {
			gifDecoder.setLoopAnimation();
		}
	}

	/**
	 * 以字节数据形式设置gif图片<br>
	 * 如果图片太大，请不要采用本方法，而应该采用setGifImage(String strFileName)或setGifImage(int
	 * resId)方法
	 * 
	 * @param gif
	 *            图片
	 */
	public void setGifImage(byte[] gif) {
		setGifDecoderImage(gif);
	}

	/**
	 * 以文件形式设置gif图片
	 * 
	 * @param strFileName
	 *            gif图片路径，此图片必须有访问权限
	 */
	public void setGifImage(String strFileName) {
		init();
		gifDecoder.setGifImage(strFileName);
		gifDecoder.start();
	}

	/**
	 * 以资源形式设置gif图片
	 * 
	 * @param resId
	 *            gif图片的资源ID
	 */
	public void setGifImage(int resId) {
		setGifDecoderImage(getResources(), resId);
	}

	/**
	 * 清理，不使用的时候，调用本方法来释放资源<br>
	 * <strong>强烈建议在退出或者不需要gif动画时，调用本方法</strong>
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
	 * 继续显示动画。当动画暂停后，通过本方法来使动画继续
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
	 * 建议在onpause时，调用本方法
	 */
	public void pauseGifAnimation() {
		if (singleFrame)
			return;
		animation.pauseAnimation();
	}

	/**
	 * 设置gif在解码过程中的显示方式<br>
	 * <strong>本方法只能在setGifImage方法之前设置，否则设置无效</strong>
	 * 
	 * @param type
	 *            显示方式
	 */
	public void setGifImageType(GifImageType type) {
		if (gifDecoder == null)
			animationType = type;
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

	/**
	 * 中断解码线程
	 */
	private void stopDecodeThread() {
		if (gifDecoder != null && gifDecoder.getState() != Thread.State.TERMINATED) {
			gifDecoder.interrupt();
			gifDecoder.destroy();
		}
	}

	@Override
	public void setVisibility(int visibility) {
		super.setVisibility(visibility);
		if (visibility == GONE || visibility == INVISIBLE) {
			stopDrawThread();
		} else if (visibility == VISIBLE) {
			reAnimation();
		}
	}

	@Override
	public void dispatchWindowVisibilityChanged(int visibility) {
		if (visibility == GONE || visibility == INVISIBLE) {
			pauseGifAnimation();
		} else if (visibility == VISIBLE) {
			restartGifAnimation();
		}
		super.dispatchWindowVisibilityChanged(visibility);
	}

	protected void onWindowVisibilityChanged(int visibility)
	{
		//Log.d("-------------------------",String.valueOf(visibility));
	}
	
	
	private void reAnimation() {
		if (singleFrame)
			return;
		stopDrawThread();
		currentLoop = 0;
		animation.runAnimation();
	}

	/**
	 * @hide
	 */
	public void parseReturn(int iResult) {
		if (getVisibility() == GONE || getVisibility() == INVISIBLE) {
			return;
		}
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
				getCurrentFrame();
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
			Log.e("parseReturn", "ERROR");
			break;
		}
	}

	public void loopEnd() {
		currentLoop += 1;
		if (loopNum > 0) {
			if (currentLoop >= loopNum) {
				stopDrawThread();
				stopDecodeThread();
			}
		}
		if (listener != null) {
			if (iListenerType == GifListener.LOOP_ONLY || iListenerType == GifListener.LOOP_AND_FRAME_COUNT)
				listener.gifEnd(currentLoop);
			currentFrame = 0;
		}
	}

	private int getCurrentFrame() {
		if (gifDecoder != null) {
			GifFrame frame = gifDecoder.next();
			if (frame == null) {
				return -1;
			}
			if (frame.image != null) {
				currentImage = frame.image;
			}
			return frame.delay;
		} else {
			return -1;
		}
	}

	public int reDraw() {
		int delay = getCurrentFrame();
		drawImage();
		return delay;
	}

	private void invalidateImage() {
		if (redrawHandler != null) {
			Message msg = redrawHandler.obtainMessage();
			redrawHandler.sendMessage(msg);
		}
	}

	private void drawImage() {
		if (currentImage == null || (currentImage != null && currentImage.isRecycled() == false)) {
			setImageBitmap(currentImage);
			invalidate();
			if (listener != null
					&& (iListenerType == GifListener.FRAME_COUNT_ONLY || iListenerType == GifListener.LOOP_AND_FRAME_COUNT)) {
				currentFrame++;
				listener.frameCount(currentFrame);
			}
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

}
