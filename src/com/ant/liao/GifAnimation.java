package com.ant.liao;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

public class GifAnimation {
	
	private GifReDraw draw = null;
	private boolean pause = false;

	private Handler handler = new Handler(Looper.getMainLooper());
	private AnimationRunAble animation = new AnimationRunAble();

	
	public GifAnimation() {
	}

	public void setRedraw(GifReDraw v) {
		draw = v;
	}

	public void pauseAnimation() {
		synchronized (animation) {
			handler.removeCallbacks(animation);
			pause = true;
		}
	}

	public void restartAnimation() {
		synchronized (animation) {
			pause = false;
			handler.post(animation);
		}
	}

	public void stopAnimation() {
		pauseAnimation();
	}

	public void runAnimation() {
		pause = false;
		handler.post(animation);
	}
	
	public void destroy(){
		stopAnimation();
		draw = null;
	}

	private class AnimationRunAble implements Runnable {
		public void run() {
			int delay = draw.reDraw();
			if (pause == false) {
				if (delay > 0)
					SystemClock.sleep(delay);
				synchronized (animation) {
					if (pause == false)
						handler.post(animation);
				}
			}
		}
	}

}
