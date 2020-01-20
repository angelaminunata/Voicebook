package com.smobileteam.voicecall;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputFilter;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.smobileteam.voicecall.controller.MyFileManager;
import com.smobileteam.voicecall.utils.MyConstants;
import com.smobileteam.voicecall.utils.PreferUtils;
import com.smobileteam.voicecall.utils.Utilities;

import java.util.ArrayList;

/**
 * Created by Anh Son on 6/27/2016.
 */
public class StorageActivity extends AppCompatActivity {
    private Context mContext;
    private PieChart mGraphicChart;

    private TextView mFreeSpaceLabel,mFreeSpace,mOthersDataLabel,mOthersData,mNameFolderSaveData;
    private TextView mUsedRecordData,mUsedRecordDataLabel;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_storage);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.storage_title));
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                CallRecorderApp.isNeedShowPasscode = false;
            }
        });
        initUI();
    }
    private void initUI(){
        mGraphicChart = (PieChart) findViewById(R.id.fragment_storage_rl_chart);

        mNameFolderSaveData = (TextView) findViewById(R.id.fragment_storage_btn_save_folder);
        mNameFolderSaveData.setText(PreferUtils.getNameFolderSaveData(mContext));
        mFreeSpaceLabel = (TextView) findViewById(R.id.fragment_storage_txt_free_storage_label);
        mFreeSpace = (TextView) findViewById(R.id.fragment_storage_txt_free_storage);
        mOthersDataLabel = (TextView) findViewById(R.id.fragment_storage_txt_other_data_label);
        mOthersData = (TextView) findViewById(R.id.fragment_storage_txt_other_data);
        mUsedRecordData =(TextView) findViewById(R.id.fragment_storage_txt_used_storage);
        mUsedRecordDataLabel =(TextView) findViewById(R.id.fragment_storage_txt_used_storage_label);
        setStyle();
        buildDataChart(getInfomationCapacity());
        setStyleChart();
        setPercentStorage();
    }
    public void buildDataChart (double[] values) {
        String [] valuesTitle = {"","",""};
        int count = values.length;
        ArrayList<Entry> yVals1 = new ArrayList<Entry>();
        for (int i = 0; i < count; i++) {
            yVals1.add(new Entry((float) values[i], i));
        }
        ArrayList<String> xVals = new ArrayList<String>();
        for (int i = 0; i < count; i++){
            xVals.add(valuesTitle[i]);
        }
        PieDataSet dataSet = new PieDataSet(yVals1, "");
        dataSet.setSliceSpace(2f);
        dataSet.setSelectionShift(5f);
        // add a lot of colors
        ArrayList<Integer> colors = new ArrayList<Integer>();
        colors.add(Color.parseColor("#25909A"));
        colors.add(Color.parseColor("#de9800"));
        colors.add(Color.parseColor("#e63600"));
        dataSet.setColors(colors);
        PieData data = new PieData(xVals, dataSet);
        data.setValueFormatter(new PercentFormatter());
        data.setDrawValues(false);
        mGraphicChart.setData(data);

        // undo all highlights
        mGraphicChart.highlightValues(null);
        mGraphicChart.invalidate();
    }
    private void setStyle(){
        mFreeSpaceLabel.setSelected(true);
        mOthersDataLabel.setSelected(true);
        mUsedRecordDataLabel.setSelected(true);
    }
    private void setPercentStorage(){
        double [] valueStorage = getInfomationCapacity();
        double total = valueStorage[0] + valueStorage[1] + valueStorage[2];
        mFreeSpace.setText(""+ Utilities.calculatePercent(valueStorage[0], total)+"%");
        mUsedRecordData.setText(""+ Utilities.calculatePercent(valueStorage[1], total)+"%");
        mOthersData.setText(""+ Utilities.calculatePercent(valueStorage[2], total)+"%");
    }
    public void setStyleChart(){
        mGraphicChart.setUsePercentValues(true);
        mGraphicChart.setDescription("");
        mGraphicChart.setExtraOffsets(5, 10, 5, 5);

        mGraphicChart.setDragDecelerationFrictionCoef(0.95f);
        mGraphicChart.setDrawHoleEnabled(true);
        mGraphicChart.setHoleColorTransparent(true);
        mGraphicChart.setTransparentCircleColor(Color.WHITE);
        mGraphicChart.setTransparentCircleAlpha(100);

        mGraphicChart.setHoleRadius(40f);
        mGraphicChart.setTransparentCircleRadius(61f);
        mGraphicChart.setCenterText(generateCenterSpannableText());
        mGraphicChart.setDrawCenterText(true);
        mGraphicChart.setCenterTextColor(Color.WHITE);
        mGraphicChart.setCenterTextSize(15);
        mGraphicChart.getLegend().setEnabled(false);
        mGraphicChart.setRotationAngle(0);
        // enable rotation of the chart by touch
        mGraphicChart.setRotationEnabled(true);
//		mGraphicChart.setHighlightPerTapEnabled(true);
        mGraphicChart.animateY(1400, Easing.EasingOption.EaseInOutQuad);

    }
    private SpannableString generateCenterSpannableText() {

        SpannableString s = new SpannableString("100%");
        s.setSpan(new RelativeSizeSpan(1.7f), 0, 4, 0);
        return s;
    }
    private double[] getInfomationCapacity (){
//		  final long KILOBYTE = 1024;
        StatFs internalStatFs = new StatFs( Environment.getRootDirectory().getAbsolutePath() );
        long internalTotal;
        long internalFree;

        StatFs externalStatFs = new StatFs( Environment.getExternalStorageDirectory().getAbsolutePath() );
        long externalTotal;
        long externalFree;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            internalTotal = ( internalStatFs.getBlockCountLong() * internalStatFs.getBlockSizeLong() );// / ( KILOBYTE * KILOBYTE );
            internalFree = ( internalStatFs.getAvailableBlocksLong() * internalStatFs.getBlockSizeLong());// / ( KILOBYTE * KILOBYTE );
            externalTotal = ( externalStatFs.getBlockCountLong() * externalStatFs.getBlockSizeLong());// / ( KILOBYTE * KILOBYTE );
            externalFree = ( externalStatFs.getAvailableBlocksLong() * externalStatFs.getBlockSizeLong());// / ( KILOBYTE * KILOBYTE );
        }
        else {
            internalTotal = ( (long) internalStatFs.getBlockCount() * (long) internalStatFs.getBlockSize());// / ( KILOBYTE * KILOBYTE );
            internalFree = ( (long) internalStatFs.getAvailableBlocks() * (long) internalStatFs.getBlockSize());// / ( KILOBYTE * KILOBYTE );
            externalTotal = ( (long) externalStatFs.getBlockCount() * (long) externalStatFs.getBlockSize());// / ( KILOBYTE * KILOBYTE );
            externalFree = ( (long) externalStatFs.getAvailableBlocks() * (long) externalStatFs.getBlockSize());// / ( KILOBYTE * KILOBYTE );
        }

        long total = internalTotal + externalTotal;
        long free = internalFree + externalFree;
        long mydata = MyFileManager.getFolderDataSize(mContext);
        long otherdata = total- (free + mydata);

        if(MyConstants.DEBUG) Log.i(MyConstants.TAG, "Total : "+total + "bytes,Free space : " +free + "bytes,Used : " +otherdata );
        return (new double[] {free,mydata,otherdata});
    }

    public void changeNameFolder(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_change_name_folder_save_file);
        builder.setMessage(R.string.dialog_change_name_folder_save_file_description);
        final EditText inputName = new EditText(mContext);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        inputName.setImeOptions(EditorInfo.IME_ACTION_DONE);
        inputName.setFilters(new InputFilter[] {new InputFilter.LengthFilter(1000)});// Maxium allow input is 1000 charater
        inputName.setText(PreferUtils.getNameFolderSaveData(mContext));
        inputName.setLayoutParams(lp);
        builder.setView(inputName);
        builder.setPositiveButton(R.string.string_ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                PreferUtils.saveStringPreferences(mContext,
                        MyConstants.KEY_NAME_FOLDER_SAVE_DATA, inputName.getText().toString().trim());
                mNameFolderSaveData.setText(inputName.getText().toString().trim());
            }
        });
        builder.setNegativeButton(R.string.string_cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                dialog.dismiss();
            }
        });
        builder.create();
        builder.show();
    }

    public void clearData(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_clear_all_data_title);
        builder.setMessage(R.string.dialog_clear_all_data_description);
        builder.setPositiveButton(R.string.string_yes, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                new DeleteAllFile().execute();
            }
        });
        builder.setNegativeButton(R.string.string_cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                dialog.dismiss();
            }
        });
        builder.show();
    }
    /**
     * Delete all file in local storage
     * @author Anh Son
     *
     */
    private class DeleteAllFile extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            // TODO Auto-generated method stub
            Utilities.clearData(mContext);
            //need fix when delete all will be affect to Save record????
            return null;
        }

        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            super.onPreExecute();
            setProgressBarIndeterminateVisibility(true);
        }
        @Override
        protected void onPostExecute(Void result) {
            // TODO Auto-generated method stub
            super.onPostExecute(result);
            setProgressBarIndeterminateVisibility(false);
            Intent intent = new Intent();
            intent.setAction(MyConstants.ACTION_BROADCAST_INTENT_DELETE_ALL);
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            sendBroadcast(intent);
//            updateCountNavDrawer(MENU_RECORDING,0);
//            updateRecordHistory();
        }

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        CallRecorderApp.isNeedShowPasscode = false;
    }
}
