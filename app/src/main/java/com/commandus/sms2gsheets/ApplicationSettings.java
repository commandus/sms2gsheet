package com.commandus.sms2gsheets;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * StickyChat application settings singleton class
 */
public class ApplicationSettings {
	static final String PREFS_NAME = "sms2gsheets";
	private static final String PREF_SPREADSHEETID = "ss";
	private static final String PREF_SHEET = "s";
	private static final String PREF_FIRST_TIME = "firsttime";

	private String mSpreadsheetId;
	private String mSheet;
	private static boolean mFirstTime;

	private static ApplicationSettings mInstance = null;

	synchronized static ApplicationSettings getInstance(Context context) {
		if (mInstance == null) {
			mInstance = new ApplicationSettings(context);
		}
		return mInstance;
	}

	private ApplicationSettings(Context context) {
		load(context);
	}

	public String getSpreadsheetId() {
		return mSpreadsheetId;
	}

    public void setSpreadsheetId(String value) {
        mSpreadsheetId = value;
    }

    public String getSheet() {
		return mSheet;
	}


    public void setSheet(String value) {
        mSheet = value;
    }

    public void save(Context context) {
		SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PREF_SPREADSHEETID, mSpreadsheetId);
		editor.putString(PREF_SHEET, mSheet);
		editor.putBoolean(PREF_FIRST_TIME, mSpreadsheetId.isEmpty());
		editor.apply();
	}

    private void load(Context context) {
		SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
		mSpreadsheetId = settings.getString(PREF_SPREADSHEETID, "");
		mSheet = settings.getString(PREF_SHEET, "");
		mFirstTime = settings.getBoolean(PREF_FIRST_TIME, true);
	}

	public boolean isFirstTime() {
		return mFirstTime;
	}

	public void setFirstTime(Context context, boolean value) {
		mFirstTime = value;
        save(context);
	}

}
