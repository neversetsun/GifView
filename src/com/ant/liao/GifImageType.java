package com.ant.liao;

/**
 * 解码过程中，Gif动画显示的方式<br>
 * 如果图片较大，那么解码过程会比较长，这个解码过程中，gif如何显示
 * 
 * @author liao
 * 
 */
public enum GifImageType {

	/**
	 * 在解码过程中，不显示图片，直到解码全部成功后，再显示
	 */
	WAIT_FINISH(0),
	/**
	 * 和解码过程同步，解码进行到哪里，图片显示到哪里
	 */
	SYNC_DECODER(1),
	/**
	 * 在解码过程中，只显示第一帧图片
	 */
	COVER(2);

	GifImageType(int i) {
		nativeInt = i;
	}

	final int nativeInt;
}
