package com.ant.liao;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DemoActivity extends Activity implements GifListener {

	private GifView gifView = null;
	
	private GifDrawable drawableGif = null;
	
	private LinearLayout layout = null;
	
	private int currentType = 1;
	
	private TextView desc = null;
	
	private TextView frame_desc = null;
	
	private ImageView img = null;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.gif);

		Button btn_static = (Button) findViewById(R.id.static_btn);
		btn_static.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				if(currentType != 1){
					setGifView(1);
				}
			}
		});

		Button btn_one = (Button)findViewById(R.id.one_btn);
		btn_one.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				if(currentType != 2){
					setGifView(2);
				}
			}
		});
		
		Button btn_loop = (Button)findViewById(R.id.loop_btn);
		btn_loop.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				if(currentType != 3){
					setGifView(3);
				}
			}
		});
		
		Button btn_drawable = (Button)findViewById(R.id.drawable_btn);
		btn_drawable.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				if(currentType != 4){
					setDrawable();
				}
			}
		});
		
		layout = (LinearLayout)findViewById(R.id.gif_show);
		layout.setBackgroundColor(0xFFFF00);
		desc = (TextView)findViewById(R.id.desc);
		
		frame_desc = (TextView)findViewById(R.id.frame_desc);
		
		setGifView(1);
	}

	private void destroyGif(){
		if(gifView != null){
			gifView.destroy();
		}
		gifView = null;
	}
	
	private void destroyDrawable(){
		if(drawableGif != null){
			drawableGif.destroy();
		}
		drawableGif = null;
	}
	
	private void newGifView(){
		destroyGif();
		destroyDrawable();
		gifView = new GifView(this);
		LayoutParams l = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		gifView.setLayoutParams(l);
		gifView.setScaleType(ScaleType.FIT_CENTER);
	}
	
	private void setGifView(int type){
		if(type >=1 && type <=3){
		newGifView();
		switch(type){
		case 1:
			gifView.setGifImage(R.drawable.static_gif);
			desc.setText("下面显示的是单帧静态gif");
			frame_desc.setText("");
			break;
		case 2:
			gifView.setGifImage(R.drawable.doa);
			gifView.setBackgroundColor(0xFFFF00);
			gifView.setLoopNumber(2);
			gifView.setListener(this,1);
			desc.setText("下面显示的是只播放2次的Gif动画，通过setLoopNumber方法可以设置播放次数");
			frame_desc.setText("");
			break;
		case 3:
			gifView.setGifImage(R.drawable.m);
			gifView.setLoopAnimation();
			gifView.setListener(this,2);
			desc.setText("下面显示的是循环播放的gif动画，通过setLoopAnimation可以设置动画循环播放，默认只播放一次");
			frame_desc.setText("");
			break;
		}
		currentType = type;
		layout.removeAllViews();
		layout.addView(gifView);
		}
	}
	
	private void setDrawable(){
		destroyGif();
		destroyDrawable();
		drawableGif = new GifDrawable();
		drawableGif.setGifImage(this.getResources(),R.drawable.b);
		drawableGif.setLoopAnimation();
		drawableGif.setListener(this, 4);
		img = new ImageView(this);
		LayoutParams p = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		img.setLayoutParams(p);
		img.setBackgroundDrawable(drawableGif);
		desc.setText("下面显示的是GifDrawable");
		frame_desc.setText("");
		layout.removeAllViews();
		layout.addView(img);
		currentType = 4;
	}
	
	public void onResume() {
		super.onResume();
		
	}

	public void onPause() {
		super.onPause();
		if(gifView != null){
			gifView.pauseGifAnimation();
		}
		if(drawableGif != null){
			drawableGif.pauseGifAnimation();
		}
	}

	public void onDestroy() {
		super.onDestroy();
		destroyGif();
		destroyDrawable();
	}

	@Override
	public void gifEnd(int num) {
		frame_desc.setText("第" + num + "帧播放完");
	}

	@Override
	public void frameCount(int frame) {
		frame_desc.setText("frame:" + frame);
	}


}
