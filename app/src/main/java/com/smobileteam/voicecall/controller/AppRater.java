package com.smobileteam.voicecall.controller;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.smobileteam.voicecall.R;
import com.smobileteam.voicecall.utils.MyConstants;
import com.smobileteam.voicecall.utils.PreferUtils;
import com.smobileteam.voicecall.utils.Utilities;

public class AppRater {

    private final static String APP_PNAME = "com.smobileteam.voicecall";// Package Name

    private final static int DAYS_UNTIL_PROMPT = 3;//Min number of days
    private final static int LAUNCHES_UNTIL_PROMPT = 10;//Min number of launches

    public static void app_launched(Context mContext) {
        SharedPreferences prefs = mContext.getSharedPreferences("apprater", 0);
        if (prefs.getBoolean("dontshowagain", false)) { return ; }

        SharedPreferences.Editor editor = prefs.edit();

        // Increment launch counter
        long launch_count = prefs.getLong("launch_count", 0) + 1;
        editor.putLong("launch_count", launch_count);

        // Get date of first launch
        Long date_firstLaunch = prefs.getLong("date_firstlaunch", 0);
        if (date_firstLaunch == 0) {
            date_firstLaunch = System.currentTimeMillis();
            editor.putLong("date_firstlaunch", date_firstLaunch);
        }

        // Wait at least n days before opening
        int number_exception = PreferUtils.getIntPreferences(mContext, MyConstants.KEY_EXCEPTION);
        if (launch_count >= LAUNCHES_UNTIL_PROMPT && number_exception <= 3) {
            if (System.currentTimeMillis() >= date_firstLaunch + 
                    (DAYS_UNTIL_PROMPT * 24 * 60 * 60 * 1000)) {
                showRateDialog(mContext, editor);
            }
        }
        editor.commit();
    }   

    public static void showRateDialog(final Context context, final SharedPreferences.Editor editor) {
        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.rate_dialog_new);
        
        TextView message = (TextView) dialog.findViewById(R.id.rate_dialog_txt_title);
        message.setText(context.getString(R.string.rate_dialog_txt_inform)+" "+context.getString(R.string.app_name)+"?");
        
        ImageView btn_ratenow = (ImageView) dialog.findViewById(R.id.rate_dialog_img_happy);
        btn_ratenow.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + APP_PNAME)));
                if (editor != null) {
                    editor.putBoolean("dontshowagain", true);
                    editor.commit();
                }
                dialog.dismiss();
            }
        }); 

        ImageView btn_not_happy = (ImageView) dialog.findViewById(R.id.rate_dialog_img_sad);
        btn_not_happy.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
    		            "mailto","anhson.duong@gmail.com", null));
    			emailIntent.putExtra(Intent.EXTRA_SUBJECT, 
    					context.getString(R.string.app_name) + " ("
    							+ Utilities.getVersion(context) + "|"
    							+ getDeviceName() + "): "
    							+ context.getString(R.string.about_app_feedback_title));
    			 try {
    				 context.startActivity(Intent.createChooser(emailIntent, context.getString(R.string.feedback) + "..."));
    	         } catch (android.content.ActivityNotFoundException ex) {
    	                Utilities.showToast(context, context.getString(R.string.app_feedback_exception_no_app_handle));
    	         }
    			 if (editor != null) {
                     editor.putBoolean("dontshowagain", true);
                     editor.commit();
                 }
    			 dialog.dismiss();
            }
        });
        

        CheckBox btn_no_thanks= (CheckBox) dialog.findViewById(R.id.rate_dialog_cb_nothanks);
        btn_no_thanks.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonview, boolean isChecked) {
				// TODO Auto-generated method stub
				if(isChecked){
					if (editor != null) {
	                    editor.putBoolean("dontshowagain", true);
	                    editor.commit();
	                }
				}else {
					if (editor != null) {
	                    editor.putBoolean("dontshowagain", false);
	                    editor.commit();
	                }
				}
				
			}
		});    
        dialog.show();        
    }
	public static String getDeviceName() {
	    String manufacturer = Build.MANUFACTURER;
	    String model = Build.MODEL;
	    if (model.startsWith(manufacturer)) {
	        return capitalize(model);
	    } else {
	        return capitalize(manufacturer) + " " + model;
	    }
	}
	private static String capitalize(String s) {
	    if (s == null || s.length() == 0) {
	        return "";
	    }
	    char first = s.charAt(0);
	    if (Character.isUpperCase(first)) {
	        return s;
	    } else {
	        return Character.toUpperCase(first) + s.substring(1);
	    }
	}
}