package com.ru426.android.xposed.parts.behavior_and_etc.util;

import java.util.Map;
import java.util.Set;

import com.ru426.android.xposed.parts.behavior_and_etc.Settings;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;

public class XUtil {
	public static final String SCREENSHOT_SOUND_OFF = "screenshot_sound_off";
	public static final Uri DB_URI = Uri.parse("content://" + Settings.class.getPackage().getName() + ".dbprovider");
	public static int getIntDB(String name, Context context) {
		int result = 0;
		if(context == null) return result;
		try{
			Cursor cursor = context.getContentResolver().query(DB_URI, new String[]{"value"}, "name=?", new String[]{name}, null);
			if(cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()){
				result = cursor.getInt(cursor.getColumnIndex("value"));
			}
			if(cursor != null) cursor.close();
		}catch(Exception e){
			e.printStackTrace();
		}
		return result;
	}
	
	public static void putIntDB(String name, int value, Context context) {
		try {
			if(context.getContentResolver() != null){
				ContentValues values = new ContentValues();
				values.put("name", name);
				values.put("value", value);
				context.getContentResolver().insert(DB_URI, values);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
	public static void writeBoolSettingsToDb(Context context, String name, boolean isChecked) {
		int i = isChecked ? 1 : 0;
		putIntDB(name, i, context);
	}
	
	public static void copyPreferences(SharedPreferences source, SharedPreferences target, String sourceKey){
		Map<String, ?> pluginPrefAll = source.getAll();
		if(sourceKey == null || sourceKey.length() == 0){
			Object obj = pluginPrefAll.get(sourceKey);
			copyPreferenceCore(target, sourceKey, obj);
		}else{
			for(String key : pluginPrefAll.keySet()){
				Object obj = pluginPrefAll.get(key);
				copyPreferenceCore(target, key, obj);
			}
		}
	}
	
	private static void copyPreferenceCore(SharedPreferences target, String key, Object obj){
		try{
			boolean value = (Boolean) obj;
			target.edit().putBoolean(key, value).commit();
		}catch(ClassCastException e){
			try{
				int value = (Integer) obj;
				target.edit().putInt(key, value).commit();
			}catch(ClassCastException e1){
				try{
					long value = (Long) obj;
					target.edit().putLong(key, value).commit();
				}catch(ClassCastException e2){
					try{
						float value = (Float) obj;
						target.edit().putFloat(key, value).commit();
					}catch(ClassCastException e3){											
						try{
							String value = (String) obj;
							target.edit().putString(key, value).commit();
						}catch(ClassCastException e4){
							try{
								@SuppressWarnings("unchecked")
								Set<String> value = (Set<String>) obj;
								target.edit().putStringSet(key, value).commit();
							}catch(ClassCastException e5){
							}
						}
					}
				}
			}
		}
	}
}
