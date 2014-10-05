package com.ant.liao;

public interface GifAction {
	
	/**第一贴解码成功*/
	public static final int RETURN_FIRST = 1;
	/**所有解码成功*/
	public static final int RETURN_FINISH = 2;
	/**缴存解码成功*/
	public static final int RETURN_CACHE_FINISH = 3;
	/**解码失败*/
	public static final int RETURN_ERROR = 4;
	
	/**
	 * 动画解码结果	
	 * @param iResult 结果
	 */
	public void parseReturn(int iResult);
	
	/**
	 * @hide
	 * gif动画是否结束了一轮的显示，每一轮结束，都会有本事件触发
	 */
	public void loopEnd();
}
