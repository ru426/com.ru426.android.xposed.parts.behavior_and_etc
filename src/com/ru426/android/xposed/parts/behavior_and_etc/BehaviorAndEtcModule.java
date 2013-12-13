package com.ru426.android.xposed.parts.behavior_and_etc;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.BatteryManager;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;

import com.ru426.android.xposed.library.ModuleBase;
import com.ru426.android.xposed.parts.behavior_and_etc.util.XUtil;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class BehaviorAndEtcModule extends ModuleBase {
	private static final String TAG = BehaviorAndEtcModule.class.getSimpleName();
	
	private boolean lower_actionbar;
	private boolean disable_scrolling_cache;
	private boolean led_off_battery_full;
	private boolean screenshot_sound_off;
	
	private int mLowBatteryWarningLevel;
	private int mBatteryStatus;
	private int mBatteryLevel;
	
	private Bitmap mScreenBitmap;
	private AnimatorSet mScreenshotAnimation;
	
	@Override
	public void initZygote(XSharedPreferences prefs, boolean isDebug) {
		super.initZygote(prefs, isDebug);
		lower_actionbar = (Boolean) xGetValue(prefs, xGetString(R.string.is_hook_lower_actionbar_key), false);
		disable_scrolling_cache = (Boolean) xGetValue(prefs, xGetString(R.string.is_hook_disable_scrolling_cache_key), false);
		led_off_battery_full = (Boolean) xGetValue(prefs, xGetString(R.string.is_hook_led_off_battery_full_key), false);
		
		Class<?> xPhoneWindow = XposedHelpers.findClass("com.android.internal.policy.impl.PhoneWindow", null);
		Object callback[] = new Object[2];
		callback[0] = XposedHelpers.findClass("com.android.internal.policy.impl.PhoneWindow$DecorView", null);
		callback[1] = new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				super.afterHookedMethod(param);
				try {
					xLog(TAG + " : " + "afterHookedMethod generateLayout");
					ViewGroup mDecor = (ViewGroup) XposedHelpers.getObjectField(param.thisObject, "mDecor");
					ViewGroup mContentParent = (ViewGroup) mDecor.findViewById(Window.ID_ANDROID_CONTENT);
					if(mContentParent.getParent() != null){
						ViewGroup parent = (ViewGroup) mContentParent.getParent();
						for(int i = 0; i< parent.getChildCount(); i++){
							if(parent.getChildAt(i).getClass().getCanonicalName().equals("com.android.internal.widget.ActionBarContainer")){
								ViewGroup actionBarContainer = (ViewGroup) parent.getChildAt(i);
								parent.removeView(actionBarContainer);
								parent.addView(actionBarContainer);
								break;
							}
						}
					}
				} catch (Throwable throwable) {
					XposedBridge.log(throwable);
				}
			}
		};
		xHookMethod(xPhoneWindow, "generateLayout", callback, lower_actionbar);
		
		Class<?> xAbsListView = XposedHelpers.findClass("android.widget.AbsListView", null);
		Object callback2[] = new Object[1];
		callback2[0] = new XC_MethodReplacement() {			
			@Override
			protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
				try{
					xLog(TAG + " : " + "replaceHookedMethod createScrollingCache");
					if(disable_scrolling_cache){
					}else{
						XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
					}					
				} catch (Throwable throwable) {
					XposedBridge.log(throwable);
				}
				return null;
			}
		};
		xHookMethod(xAbsListView, "createScrollingCache", callback2, disable_scrolling_cache);
		
		Class<?> xBatteryService = XposedHelpers.findClass("com.android.server.BatteryService", null);
		Object callback3[] = new Object[1];
		callback3[0] = new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				super.beforeHookedMethod(param);
				try{
					xLog(TAG + " : " + "beforeHookedMethod processValues");
					if(led_off_battery_full){
						mLowBatteryWarningLevel = (Integer) XposedHelpers.getObjectField(param.thisObject, "mLowBatteryWarningLevel");
						mBatteryStatus = (Integer) XposedHelpers.getObjectField(param.thisObject, "mBatteryStatus");
						mBatteryLevel = (Integer) XposedHelpers.getObjectField(param.thisObject, "mBatteryLevel");
					}
				}catch (Throwable throwable) {
					XposedBridge.log(throwable);
				}
			}			
		};
		try{
			XposedHelpers.findAndHookMethod(xBatteryService, "processValuesLocked", callback3);
		}catch(NoSuchMethodError e){
			XposedHelpers.findAndHookMethod(xBatteryService, "processValues", callback3);
		}
		
		Class<?> xBatteryServiceLed = XposedHelpers.findClass("com.android.server.BatteryService$Led", null);
		Object callback4[] = new Object[1];
		callback4[0] = new XC_MethodReplacement() {			
			@Override
			protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
				try{
					xLog(TAG + " : " + "replaceHookedMethod updateLightsLocked");
					final int level = mBatteryLevel;
		            final int status = mBatteryStatus;
		            final Object mBatteryLight = XposedHelpers.getObjectField(param.thisObject, "mBatteryLight");
					if (level < mLowBatteryWarningLevel) {
					} else if (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL) {
						if (status == BatteryManager.BATTERY_STATUS_FULL || level >= 90) {
							if (level >= 98) {
								XposedHelpers.callMethod(mBatteryLight, "turnOff");
								xLog(TAG + " : " + "mBatteryLight callMethod : turnOff");
								return null;
							}
						}
					}				
				} catch (Throwable throwable) {
					XposedBridge.log(throwable);
				}
            	XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
				return null;
			}
		};
		xHookMethod(xBatteryServiceLed, "updateLightsLocked", callback4, led_off_battery_full);
	}

	@Override
	public void init(final XSharedPreferences prefs, ClassLoader classLoader, boolean isDebug) {
		super.init(prefs, classLoader, isDebug);		
		Class<?> xGlobalScreenshot = XposedHelpers.findClass("com.android.systemui.screenshot.GlobalScreenshot", classLoader);
		Object callback[] = new Object[6];
		callback[0] = Runnable.class;
		callback[1] = int.class;
		callback[2] = int.class;
		callback[3] = boolean.class;
		callback[4] = boolean.class;
		callback[5] = new XC_MethodReplacement() {			
			@Override
			protected Object replaceHookedMethod(final MethodHookParam param) throws Throwable {
				xLog(TAG + " : " + "replaceHookedMethod startAnimation");
				try{
					if(XposedHelpers.getObjectField(param.thisObject, "mContext") != null){
						mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
						try{
							screenshot_sound_off = XUtil.getIntDB(XUtil.SCREENSHOT_SOUND_OFF, mContext) == 1;							
						}catch(Exception e){
							screenshot_sound_off = (Boolean) xGetValue(prefs, xGetString(R.string.is_hook_screenshot_sound_off_key), false);
						}
						if(screenshot_sound_off){
							mScreenBitmap = (Bitmap) XposedHelpers.getObjectField(param.thisObject, "mScreenBitmap");
							final ImageView mScreenshotView = (ImageView) XposedHelpers.getObjectField(param.thisObject, "mScreenshotView");
							final View mScreenshotLayout = (View) XposedHelpers.getObjectField(param.thisObject, "mScreenshotLayout");
							mScreenshotAnimation = (AnimatorSet) XposedHelpers.getObjectField(param.thisObject, "mScreenshotAnimation");
							final WindowManager mWindowManager = (WindowManager) XposedHelpers.getObjectField(param.thisObject, "mWindowManager");
							WindowManager.LayoutParams mWindowLayoutParams = (LayoutParams) XposedHelpers.getObjectField(param.thisObject, "mWindowLayoutParams");
							mScreenshotView.setImageBitmap(mScreenBitmap);
					        mScreenshotLayout.requestFocus();
					        
					        if (mScreenshotAnimation != null) {
					            mScreenshotAnimation.end();
					            mScreenshotAnimation.removeAllListeners();
					        }
					        
					        mWindowManager.addView(mScreenshotLayout, mWindowLayoutParams);
					        ValueAnimator screenshotDropInAnim = (ValueAnimator) XposedHelpers.callMethod(param.thisObject, "createScreenshotDropInAnimation");
					        ValueAnimator screenshotFadeOutAnim = (ValueAnimator) XposedHelpers.callMethod(param.thisObject, "createScreenshotDropOutAnimation", new Object[]{param.args[1], param.args[2], param.args[3], param.args[4]});
					        mScreenshotAnimation = new AnimatorSet();
					        mScreenshotAnimation.playSequentially(screenshotDropInAnim, screenshotFadeOutAnim);
					        mScreenshotAnimation.addListener(new AnimatorListenerAdapter() {
					            @Override
					            public void onAnimationEnd(Animator animation) {
					            	XposedHelpers.callMethod(param.thisObject, "saveScreenshotInWorkerThread", param.args[0]);
					                mWindowManager.removeView(mScreenshotLayout);
					                mScreenBitmap = null;
					                mScreenshotView.setImageBitmap(null);
					            }
					        });
					        mScreenshotLayout.post(new Runnable() {
					            @Override
					            public void run() {
					                mScreenshotView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
					                mScreenshotView.buildLayer();
					                mScreenshotAnimation.start();
					            }
					        });	
						}else{
							XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
						}
					}else{
						XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
						return null;
					}			
				} catch (Throwable throwable) {
					XposedBridge.log(throwable);
				}
				return null;
			}
		};
		xHookMethod(xGlobalScreenshot, "startAnimation", callback, true);
	}
}
