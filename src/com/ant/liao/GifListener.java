package com.ant.liao;

/**
 * gif动画事件回调接口，如果需要得到Gif动画播放过程中的相关事件，必须实现本接口<br>
 * 
 * @author smartliao
 *
 */
public interface GifListener {

	/**只需要播放次数的事件回调*/
	public final static int  LOOP_ONLY = 1;
	/**只需要当前帧数的事件回调*/
	public final static int FRAME_COUNT_ONLY = 2;
	/**播放次数和当前帧数的事件回调都要*/
	public final static int LOOP_AND_FRAME_COUNT = 3;

	
	/**
	 * gif动画播放结束事件，每播放完一次都会触发本事件
	 * @param num 播放次数
	 */
	public void gifEnd(int num);
	
	/**
	 * gif动画当前帧数事件
	 * @param frame
	 */
	public void frameCount(int frame);
	
	
}
