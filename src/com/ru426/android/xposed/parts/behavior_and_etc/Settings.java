package com.ru426.android.xposed.parts.behavior_and_etc;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.MenuItem;
import android.widget.Toast;

import com.ru426.android.xposed.parts.behavior_and_etc.util.XUtil;

public class Settings extends PreferenceActivity {
	private static Context mContext;
	private static SharedPreferences prefs;
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		mContext = this;
		prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		if(prefs.getBoolean(getString(R.string.ru_use_light_theme_key), false)){
			setTheme(android.R.style.Theme_DeviceDefault_Light);
		}
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings_fragment_base);
	    init();
	    initOption();
	}

	@Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch(item.getItemId()){
		case android.R.id.home:
			finish();
			break;
		}
        return super.onMenuItemSelected(featureId, item);
    }
	
	private static void showHomeButton(){
		if(mContext != null && ((Activity) mContext).getActionBar() != null){
			((Activity) mContext).getActionBar().setHomeButtonEnabled(true);
	        ((Activity) mContext).getActionBar().setDisplayHomeAsUpEnabled(true);
		}		
	}
	
	static void showRestartToast(){
		Toast.makeText(mContext, R.string.ru_restart_message, Toast.LENGTH_SHORT).show();
	}
	
	private void init(){}
	
	OnSharedPreferenceChangeListener mOnSharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {			
		@SuppressLint("WorldReadableFiles")
		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			@SuppressWarnings("deprecation")
			SharedPreferences target = mContext.getSharedPreferences(Settings.class.getPackage().getName(), Context.MODE_WORLD_READABLE | Context.MODE_MULTI_PROCESS);
			XUtil.copyPreferences(sharedPreferences, target, key);
		}
	};
	
	@SuppressWarnings("deprecation")
	private void initOption(){
		showHomeButton();
		setPreferenceChangeListener(getPreferenceScreen());
		prefs.registerOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);
	}
	
	@Override
	protected void onDestroy() {
		prefs.unregisterOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);
		super.onDestroy();
	}

	private static void setPreferenceChangeListener(PreferenceScreen preferenceScreen){
		for(int i = 0; i < preferenceScreen.getPreferenceCount(); i++){
			if(preferenceScreen.getPreference(i) instanceof PreferenceCategory){
				for(int j = 0; j < ((PreferenceCategory) preferenceScreen.getPreference(i)).getPreferenceCount(); j++){
					((PreferenceCategory) preferenceScreen.getPreference(i)).getPreference(j).setOnPreferenceChangeListener(onPreferenceChangeListener);
				}
			}else{
				preferenceScreen.getPreference(i).setOnPreferenceChangeListener(onPreferenceChangeListener);				
			}
		}
	}
	
	private static OnPreferenceChangeListener onPreferenceChangeListener = new OnPreferenceChangeListener(){
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			switch(preference.getTitleRes()){
			case R.string.is_hook_lower_actionbar_title:
			case R.string.is_hook_disable_scrolling_cache_title:
			case R.string.is_hook_led_off_battery_full_title:
				if(!prefs.getBoolean(preference.getKey(), false) && (Boolean) newValue){
					showRestartToast();
				}
				break;
			case R.string.is_hook_screenshot_sound_off_title:
				XUtil.writeBoolSettingsToDb(mContext, XUtil.SCREENSHOT_SOUND_OFF, (Boolean) newValue);
				break;
			default:
				return false;
			}
			return true;
		}		
	};
}
