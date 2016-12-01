package com.zhangyf.auth.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.zhangyf.auth.config.MTokenConstants;

/**
 * SharedPreferences工具
 * 
 * @author zhangyf
 * @version 创建时间：2015-11-24
 */
public class PrefsUtil {

	private static String TAG = PrefsUtil.class.getSimpleName();
	private SharedPreferences prefs;
	private Editor editor;

	public PrefsUtil(Context context) {
		prefs = context.getSharedPreferences(MTokenConstants.PREFS_NAME,
				Context.MODE_MULTI_PROCESS);
		editor = prefs.edit();
	}

	public SharedPreferences getPrefs() {
		return this.prefs;
	}

	public Editor getEditor() {
		return this.editor;
	}

	public boolean getBoolean(String key, boolean defValue) {
		return prefs.getBoolean(key, defValue);
	}

	public float getFloat(String key, float defValue) {
		return prefs.getFloat(key, defValue);
	}

	public int getInt(String key, int defValue) {
		return prefs.getInt(key, defValue);
	}

	public long getLong(String key, long defValue) {
		return prefs.getLong(key, defValue);
	}

	public String getString(String key, String defValue) {
		return prefs.getString(key, defValue);
	}

	public void putBoolean(String key, boolean value) {
		editor.putBoolean(key, value);
		editor.commit();
	}

	public void putFloat(String key, float value) {
		editor.putFloat(key, value);
		editor.commit();
	}

	public void putInt(String key, int value) {
		editor.putInt(key, value);
		editor.commit();
	}

	public boolean putLong(String key, long value) {
		editor.putLong(key, value);
		return editor.commit();
	}

	public void putString(String key, String value) {
		editor.putString(key, value);
		editor.commit();
	}

}
