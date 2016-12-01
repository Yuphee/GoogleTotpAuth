package com.zhangyf.auth.utils;


import com.zhangyf.auth.config.MTokenConstants;

public class CountUtils {
	
	protected final static long mStartTime = 0;
	public final static long mTimeStep = 30;
	public static final long millisToSeconds(long timeMillis) {
		return timeMillis / 1000;
	}

	/**
	 * Gets the number of milliseconds since epoch.
	 */
	public static long currentTimeMillis(PrefsUtil prefsUtil) {
		return System.currentTimeMillis() + prefsUtil.getLong(MTokenConstants.PREFS_SERVICE_TIME_REDUCE, 0l);
	}

	  public static long getValueAtTime(long time) {
		    long timeSinceStartTime = time - mStartTime;
		    if (timeSinceStartTime >= 0) {
		      return timeSinceStartTime / mTimeStep;
		    } else {
		      return (timeSinceStartTime - (mTimeStep - 1)) / mTimeStep;
		    }
		  }
	  
	  public static long getValueStartTime(long value) {
		    return mStartTime + (value * mTimeStep);
		  }
}
