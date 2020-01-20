package com.smobileteam.voicecall;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.smobileteam.voicecall.utils.MyConstants;
import com.smobileteam.voicecall.utils.Utilities;

/**
 * Created by Anh Son on 6/27/2016.
 */
public class HelpActivity extends PreferenceActivity implements MyConstants, Preference.OnPreferenceClickListener {
    private Context mContext;

    private Preference mAppVersion;
    private Preference mLicense;
    private Preference mGetAllApp;
    private Preference mFeedback;
    private Preference mSmobileOnWeb;
    private Preference mTranslation;

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        LinearLayout root = (LinearLayout) findViewById(android.R.id.list).getParent().getParent().getParent();
        Toolbar bar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.toolbar_layout, root, false);
        root.addView(bar, 0); // insert at top
        bar.setTitle(getString(R.string.nav_about));
        bar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                CallRecorderApp.isNeedShowPasscode = false;
            }
        });
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        addPreferencesFromResource(R.xml.help);
        getListView().setDivider(new ColorDrawable(Color.TRANSPARENT));
        initUI();
    }

    private void initUI() {
        mAppVersion = findPreference(KEY_ABOUT_APP_VERSION);
        mLicense = findPreference(KEY_ABOUT_APP_LICENSE);
        mGetAllApp = findPreference(KEY_ABOUT_APP_GET_ALL_APP);
        mFeedback = findPreference(KEY_ABOUT_APP_FEEDBACK);
        mSmobileOnWeb = findPreference(KEY_ABOUT_APP_ON_GOOGLEPLUS);
        mTranslation = findPreference(KEY_ABOUT_APP_TRANSLATION);

        mAppVersion.setSummary(Utilities.getVersion(mContext));
        mAppVersion.setOnPreferenceClickListener(this);
        mLicense.setOnPreferenceClickListener(this);
        mGetAllApp.setOnPreferenceClickListener(this);
        mFeedback.setOnPreferenceClickListener(this);
        mSmobileOnWeb.setOnPreferenceClickListener(this);
        mTranslation.setOnPreferenceClickListener(this);

    }

    public String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer) + " " + model;
        }
    }


    private String capitalize(String s) {
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

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();
        if (KEY_ABOUT_APP_LICENSE.equals(key)) {
            createLicenseDialogConfirm();
            return true;
        }
        if (KEY_ABOUT_APP_GET_ALL_APP.equals(key)) {
            try {
                mContext.startActivity(new Intent(Intent.ACTION_VIEW, Uri
                        .parse("market://search?q=pub:SMobileTeam")));

            } catch (android.content.ActivityNotFoundException anfe) {
                startActivity(new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("http://play.google.com/store/apps/developer?id=SMobileTeam")));
            }
            return true;
        }
        if (KEY_ABOUT_APP_FEEDBACK.equals(key)) {
            Intent feedbackIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                    "mailto", "anhson.duong@gmail.com", null));
            feedbackIntent.putExtra(Intent.EXTRA_SUBJECT,
                    getString(R.string.app_name) + " ("
                            + Utilities.getVersion(mContext) + "|"
                            + getDeviceName() + "): "
                            + getString(R.string.about_app_feedback_title));
            try {
                mContext.startActivity(Intent.createChooser(feedbackIntent, getString(R.string.feedback) + "..."));
            } catch (android.content.ActivityNotFoundException ex) {
                Utilities.showToast(mContext, getString(R.string.app_feedback_exception_no_app_handle));
            }

            return true;
        }
        if (KEY_ABOUT_APP_ON_GOOGLEPLUS.equals(key)) {
            String url = "https://plus.google.com/b/115825835888801940774/115825835888801940774/about";
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
            return true;
        }
        if (KEY_ABOUT_APP_TRANSLATION.equals(key)) {
            Intent translateIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                    "mailto", "anhson.duong@gmail.com", null));
            translateIntent.putExtra(Intent.EXTRA_SUBJECT,
                    getString(R.string.app_name) + " ("
                            + Utilities.getVersion(mContext) + "|"
                            + getDeviceName() + "): "
                            + getString(R.string.about_app_translation_title));
            translateIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.about_app_translation_body));
            try {
                mContext.startActivity(Intent.createChooser(translateIntent, getString(R.string.about_app_translation_title) + "..."));
            } catch (android.content.ActivityNotFoundException ex) {
                Utilities.showToast(mContext, getString(R.string.app_feedback_exception_no_app_handle));
            }

            return true;
        }
        return false;
    }

    private void createLicenseDialogConfirm() {
        AlertDialog.Builder licenseDialog = new AlertDialog.Builder(mContext);
        licenseDialog.setTitle(getString(R.string.about_app_license_title));

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_license, null);
        licenseDialog.setView(dialogView);

        licenseDialog.setPositiveButton(getString(R.string.string_ok), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                dialog.dismiss();
            }
        });

        AlertDialog alertDialog = licenseDialog.create();
        alertDialog.show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        CallRecorderApp.isNeedShowPasscode = false;
    }
}
