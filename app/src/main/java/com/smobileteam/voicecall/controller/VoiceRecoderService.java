package com.smobileteam.voicecall.controller;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnErrorListener;
import android.media.MediaRecorder.OnInfoListener;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.smobileteam.voicecall.CloudActivity;
import com.smobileteam.voicecall.R;
import com.smobileteam.voicecall.adapter.DatabaseAdapter;
import com.smobileteam.voicecall.controller.ShakeDetector.OnShakeListener;
import com.smobileteam.voicecall.model.RecordModel;
import com.smobileteam.voicecall.utils.AndroidUtils;
import com.smobileteam.voicecall.utils.ConnectionUtils;
import com.smobileteam.voicecall.utils.MyConstants;
import com.smobileteam.voicecall.utils.PreferUtils;
import com.smobileteam.voicecall.utils.Utilities;

import java.io.File;
import java.io.IOException;


public class VoiceRecoderService extends Service implements MyConstants{
	private Context mContext;
	
	private final int TIME_OUT_START_RECORDER = 10;
	private MediaRecorder mRecorder = null;
	private String mPhoneNumber = null;
	private boolean isRecording = false;
	private int mStatusCall  = 50;
	
	private String mFileName;
	private String mPath;
	private long mDate;
	private NotificationController mNotificationHelper;
	private final int mNotificationRecordVoiceId = 1989;
	
	private static final String AUDIO_RECORDER_FILE_EXT_3GP = ".3gp";
	private static final String AUDIO_RECORDER_FILE_EXT_MP4 = ".mp4";
	private static final String AUDIO_RECORDER_FILE_EXT_WAV = ".wav";
	private static final String AUDIO_RECORDER_FILE_EXT_AMR = ".amr";
	private String type = "";
	
	public static RelativeLayout mContentContainerLayout;// Contains everything other than buttons and song info
	private WindowManager mWindowManager ;
	private RelativeLayout mRootLayout;
	private static final int TRAY_DIM_X_DP 					= 320;	// Width of the tray in dps
	private static final int TRAY_DIM_Y_DP 					= 78;  // Height of the tray in dps
	private static boolean isShowdialogAsk                  = false;
	
	//For sensor
    // The following are used for the shake detection
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private ShakeDetector mShakeDetector;
    
    // Database
    private DatabaseAdapter myDatabase;
    
    //Beep sound
	private RepeatSoundReceiver mRepeatSoundAlarm;
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		mContext = this;
		mNotificationHelper = new NotificationController(this);
		if(PreferUtils.getbooleanPreferences(mContext, KEY_SHAKE_CANCEL_RECORD)) initSensor();
		myDatabase = new DatabaseAdapter(this);
		if(PreferUtils.getbooleanPreferences(mContext, KEY_BEEP_SOUND)){
			mRepeatSoundAlarm = new RepeatSoundReceiver();
		}
	}
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
				
		if (intent != null && intent.getBooleanExtra("stop_service", false)) {
			// If it's a call from the notification, stop the service.
//			stopSelf();
			stopAndReleaseRecorder(false);  // Need check and verify this solution
//			sendResult("Service is stopped");
		}
		
		if(mShakeDetector != null) {
			// Add the following line to register the Session Manager Listener onResume
	        mSensorManager.registerListener(mShakeDetector, mAccelerometer,    SensorManager.SENSOR_DELAY_UI);
	        mShakeDetector.setOnShakeListener(new OnShakeListener() {
	        	 
	            @Override
	            public void onShake(int count) {
	                /*
	                 * The following method, "handleShakeEvent(count):" is a stub //
	                 * method you would use to setup whatever you want done once the
	                 * device has been shook.
	                 */
//	                handleShakeEvent(count);
	            	if(DEBUG) Log.i(TAG, "setOnShakeListener:onShake : "+count);
	            	terminateAndEraseFile(true);
	            	
	            }
	        });
		}
		if (intent != null) {
			int commandType = intent.getIntExtra(COMMAND_TYPE, 50);
			mPhoneNumber = intent.getStringExtra(PHONE_NUMBER);
			int mode = Integer.parseInt(PreferUtils.getStringPreferences(mContext, KEY_MODE_RECORDER));
			if (commandType != 0) {
				if(commandType == INCOMING_CALL_STARTED || commandType == OUTGOING_CALL_STARTED){
					if(DEBUG) Log.d(TAG, "INCOMING_CALL_STARTED || " 
							+"OUTGOING_CALL_STARTED" + " PhoneNumber : " + mPhoneNumber);
					
					mStatusCall = commandType;
					if (mPhoneNumber != null && !isRecording) {
						switch (mode) {
						case MODE_RECORDER_PRIORITY_CONTACTS:
							if(DEBUG) Log.d(TAG, "Mode recorder : " + "MODE_RECORDER_PRIORITY_CONTACTS");
							if(Utilities.isPriorityContact(mContext, mPhoneNumber)){
								activeRecorder();
							}else {
								stopService();
							}
							break;
						case MODE_RECORDER_CONTACTS_ONLY:
							if(DEBUG) Log.d(TAG, "Mode recorder : " + "MODE_RECORDER_CONTACTS_ONLY");
							if(Utilities.isSaveContact(mContext, mPhoneNumber)){
								activeRecorder();
							} else {
								stopService();
							}
							
							break;
						case MODE_RECORDER_UNKNOW_NUMBER:
							if(DEBUG) Log.d(TAG, "Mode recorder : " + "MODE_RECORDER_UNKNOW_NUMBER"
											+"isUnknow Number: "+Utilities.isSaveContact(mContext, mPhoneNumber));
							if(!Utilities.isSaveContact(mContext, mPhoneNumber)){
								activeRecorder();
							}else {
								stopService();
							}
							break;
						case MODE_RECORDER_RECORD_ALL:
							if(DEBUG) Log.d(TAG, "Mode recorder : " + "MODE_RECORDER_RECORD_ALL");
							activeRecorder();
							break;
						default:
							break;
						}	
					}
				}else if (commandType == INCOMING_CALL_ENDED || commandType == OUTGOING_CALL_ENDED) {
					if(DEBUG) Log.d(TAG, "INCOMING_CALL_ENDED || OUTGOING_CALL_ENDED");
//					mPhoneNumber = null;
					stopAndReleaseRecorder(false);
					if(isShowdialogAsk && mWindowManager!= null && mRootLayout != null){
//						mWindowManager.removeView(mRootLayout);
						hideAskDialog();
					}

					isRecording = false;
					mNotificationHelper.completed(mNotificationRecordVoiceId);
				}else if (commandType == MISSED_CALL) {
					if(DEBUG) Log.d(TAG, "MISSED_CALL");
					if(isShowdialogAsk){
						hideAskDialog();
					}
					terminateAndEraseFile(true);
				}
			}
		}
		
		
		
		return super.onStartCommand(intent, flags, startId);
	}
	
	/**
	 * set service to forground so Android cannot kill this service
	 * untill service stop self.
	 */
	private void startforgroundService(){
		Intent notificationIntent = new Intent(this,VoiceRecoderService.class);
		notificationIntent.putExtra("stop_service", true);
		PendingIntent pendingIntent = PendingIntent.getService(this, 0,
		                notificationIntent, 0);

		NotificationCompat.Builder notification = new NotificationCompat.Builder(this)
		                .setSmallIcon(R.drawable.ic_notification_record_24dp)
		                .setContentTitle(getString(R.string.app_name))
		                .setContentText(getString(R.string.notification_stop_recording))
		                .setContentIntent(pendingIntent);
		
		startForeground(mNotificationRecordVoiceId, notification.build());
	}
	
	/**
	 * Stop foreground service and hide foreground notification
	 */
	private void stopService(){
		if(DEBUG) Log.d(TAG, "Stop Service");
		stopForeground(true);
		stopSelf();
	}
	
	private void initSensor(){
		// ShakeDetector initialization
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mShakeDetector = new ShakeDetector();
	}
	
	/**
	 * Start Record with notifiatio option
	 */
	@TargetApi(23) 
	private void activeRecorder(){
		//Add code to show notification
		if(PreferUtils.getbooleanPreferences(mContext, KEY_ENABLE_NOTIFICATION)
				&& PreferUtils.getbooleanPreferences(mContext, KEY_NOTIFICATION_ALWAYS_ASK)){
			if (!AndroidUtils.isAtLeastM() || (Settings.canDrawOverlays(mContext))) { //@TargetApi(23)
				createDialogAskConfirmRecord();
			}else {
				// Start recording or show dialog request permission==> TBD
				startRecording();
			}
			
		}else {
			startRecording();
		}
	}
	/**
	 * Start recorder
	 */
	private void startRecording() {
		if(DEBUG) Log.d(TAG, "RecordService startRecording");
		boolean exception = false;
		mRecorder = new MediaRecorder();
		int fileType = Integer.parseInt(PreferUtils.getStringPreferences(mContext, KEY_FILE_TYPE_OUTPUT));
		try {
			int audiosource = Utilities.getAudioSource(mContext);
			mRecorder.setAudioSource(audiosource);
			if(DEBUG) Log.d(TAG, "Audio Source : " +audiosource);
			mDate = System.currentTimeMillis();
			mFileName = MyFileManager.getFilename(this,mPhoneNumber, mStatusCall);
			File f = new File(PreferUtils.getPathFolderSaveData(mContext));
			
			if(fileType == FILE_TYPE_3GP){
				mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
				mPath = f.getAbsolutePath() + File.separator + mFileName + AUDIO_RECORDER_FILE_EXT_3GP;
				type = AUDIO_RECORDER_FILE_EXT_3GP;
			}else if (fileType == FILE_TYPE_MP4) {
				mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
				mPath = f.getAbsolutePath() + File.separator + mFileName + AUDIO_RECORDER_FILE_EXT_MP4;
				type = AUDIO_RECORDER_FILE_EXT_MP4;
			}else if (fileType == FILE_TYPE_AMR){
				mRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
				mPath = f.getAbsolutePath() + File.separator + mFileName + AUDIO_RECORDER_FILE_EXT_AMR;
				type = AUDIO_RECORDER_FILE_EXT_AMR;
			} else {
				mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
				mPath = f.getAbsolutePath() + File.separator + mFileName + AUDIO_RECORDER_FILE_EXT_WAV;
				type = AUDIO_RECORDER_FILE_EXT_WAV;
			}
			mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
			mRecorder.setOutputFile(mPath);

			OnErrorListener errorListener = new OnErrorListener() {
				public void onError(MediaRecorder arg0, int arg1, int arg2) {
					if(DEBUG) Log.e(TAG, "OnErrorListener " + arg1 + "," + arg2);
					terminateAndEraseFile(true);
				}
			};
			mRecorder.setOnErrorListener(errorListener);

			OnInfoListener infoListener = new OnInfoListener() {
				public void onInfo(MediaRecorder arg0, int what, int extra) {
					//if(DEBUG) Log.v(TAG, "OnInfoListener " + what + "," + extra);
					//terminateAndEraseFile();
				}
			};
			mRecorder.setOnInfoListener(infoListener);

			mRecorder.prepare();
			
			// Sometimes prepare takes some time to complete
			Thread.sleep(TIME_OUT_START_RECORDER);
			mRecorder.start();
			isRecording = true;
			if(DEBUG) Log.d(TAG, "RecordService recorderStarted");
		} catch (IllegalStateException e) {
			if(DEBUG) Log.e(TAG, "IllegalStateException");
			e.printStackTrace();
			exception = true;
		} catch (IOException e) {
			if(DEBUG) Log.e(TAG, "IOException");
			e.printStackTrace();
			exception = true;
		} catch (Exception e) {
			if(DEBUG) Log.e(TAG, "Exception");
			e.printStackTrace();
			exception = true;
		}

		if (exception) {
			exception = false;
			// Put exception to preference to not show dialog rate
			int count = PreferUtils.getIntPreferences(mContext, KEY_EXCEPTION);
			++count;
			PreferUtils.saveIntPreferences(mContext, KEY_EXCEPTION, count);
			
			if(count == 5){
				//Show notification to warning that: This device doesn't support VOICE_CALL
				mNotificationHelper.createNoRecordWarningNotification(R.drawable.ic_error,
						getString(R.string.warning_unable_record), 
						getString(R.string.app_name));
			}
			// Change Audio Source.
			int source = Utilities.getAudioSource(mContext);
			if(source == MediaRecorder.AudioSource.VOICE_CALL){
				terminateAndEraseFile(false);
				boolean isChangeSuccess = PreferUtils.saveStringPreferences(mContext, KEY_AUDIO_SOURCE, ""+MIC);
				if(isChangeSuccess){
					startRecording();
				}
			}else {
				terminateAndEraseFile(true);
			}
				
		}

		if (isRecording) {
			if(PreferUtils.getbooleanPreferences(mContext, KEY_ENABLE_NOTIFICATION)){
				startforgroundService();
			}
			if(PreferUtils.getbooleanPreferences(mContext, KEY_BEEP_SOUND)){
				if(mRepeatSoundAlarm != null ) mRepeatSoundAlarm.setAlarmBeepSound(mContext);
			}
		} else {
			Toast toast = Toast.makeText(this,
					this.getString(R.string.record_impossible),
					Toast.LENGTH_LONG);
			toast.show();
		}
		
	}

	/**
	 *  Stop recorder
	 */
	private void stopAndReleaseRecorder(boolean isCancelRecord) {
		if(DEBUG) Log.d(TAG, "RecordService stopAndReleaseRecorder");
		if(mRepeatSoundAlarm != null) mRepeatSoundAlarm.cancelAlarmBeepSound(mContext);
		if (mRecorder == null){
			stopService();
			return;
		}
		boolean recorderStopped = false;
		boolean exception = false;

		try {
			mRecorder.stop();
			recorderStopped = true;
		} catch (IllegalStateException e) {
			if(DEBUG) Log.e(TAG, "stopAndReleaseRecorder : IllegalStateException");
			e.printStackTrace();
			exception = true;
		} catch (RuntimeException e) {
			if(DEBUG) Log.e(TAG, "stopAndReleaseRecorder : RuntimeException");
			exception = true;
		} catch (Exception e) {
			if(DEBUG) Log.e(TAG, "stopAndReleaseRecorder : Exception");
			e.printStackTrace();
			exception = true;
		}
		try {
			mRecorder.reset();
		} catch (Exception e) {
			if(DEBUG) Log.e(TAG, "stopAndReleaseRecorder : Exception");
			e.printStackTrace();
			exception = true;
		}
		try {
			mRecorder.release();
		} catch (Exception e) {
			if(DEBUG) Log.e(TAG, "stopAndReleaseRecorder: Exception");
			e.printStackTrace();
			exception = true;
		}

		mRecorder = null;
		if (exception) {
			deleteFile(); // Need check again
		}
		if (recorderStopped && !isCancelRecord) {
			try {
				MyFileManager.hideMediaFile(mContext, mPath);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				if(DEBUG) Log.e(TAG, "stopAndReleaseRecorder: Cannot create .nomedia file");
			}
			// add record to database
			RecordModel record = new RecordModel(mPhoneNumber, 
					mDate, Utilities.getDurationAudioFile(mContext, mPath),
					mPath, mStatusCall, FALSE);
			long IdRecord = myDatabase.addRecord(record, DatabaseAdapter.TABLE_INBOX);
			record.setId((int)IdRecord);
			myDatabase.addSearchIndex(mContext, record);
			//For Auto Sync mode
			// Send broadcast to sync record to Cloud // dang bi Fc vi mDropboxApi = null , khi chua vao CouldFragment
			if(PreferUtils.getbooleanPreferences(mContext, IS_DROPBOX_LINKED)
					&& PreferUtils.getbooleanPreferences(mContext,KEY_AUTOMATIC_SYNC)){
				if(PreferUtils.getbooleanPreferences(mContext, KEY_CLOUD_SYNC_WIFI_ONLY)
						&& ConnectionUtils.isWifiAvailable(mContext)){
					// temp disable
					new UploadFile(mContext, CloudActivity.getInstanceDropbox(mContext))
							.execute(MODE_UPLOAD_ONE_FILE,(mFileName+type),mPath);
					if(DEBUG) Log.i(TAG, "stopAndReleaseRecorder- AutoSync: "+"wifi only + wifi connected");
				}else if (!PreferUtils.getbooleanPreferences(mContext, KEY_CLOUD_SYNC_WIFI_ONLY)
						&& ConnectionUtils.isConnectingToInternet(mContext)) {
					// temp disable
					new UploadFile(mContext, CloudActivity.getInstanceDropbox(mContext))
							.execute(MODE_UPLOAD_ONE_FILE,(mFileName+type),mPath);
					if(DEBUG) Log.i(TAG, "stopAndReleaseRecorder- AutoSync:"+"not wifi only + internet connected");
				}
			}
			//For Inbox size
			int inbox_mode = Integer.parseInt(PreferUtils.getStringPreferences(mContext, KEY_INBOX_SIZE));
			if(inbox_mode != MAXIMUM_INBOX_SIZE_UNLIMITED){
			if(myDatabase.getRecordCount(DatabaseAdapter.TABLE_INBOX) > inbox_mode){
					Utilities.deleteOldestRecord(mContext);
				}
			}
			stopService();
		}
		
	}
	/**
	 * in case it is impossible to record
	 */
	private void terminateAndEraseFile(boolean isNeedStopService) {
		if(DEBUG) Log.d(TAG, "RecordService terminateAndEraseFile");
		mNotificationHelper.completed(mNotificationRecordVoiceId);
		stopAndReleaseRecorder(true);
		isRecording = false;
		deleteFile();
		if(isNeedStopService) stopService();
	}
	
	private void deleteFile() {
		if(DEBUG) Log.d(TAG, "RecordService deleteFile");
		MyFileManager.deleteFile(mPath);
		mFileName = null;
	}
	
	@SuppressLint("InflateParams") 
	private void createDialogAskConfirmRecord(){

		mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
		mRootLayout = (RelativeLayout) LayoutInflater.from(this).
				inflate(R.layout.dialog_ask_when_record, null);
		WindowManager.LayoutParams 	mRootLayoutParams = new WindowManager.LayoutParams(
				Utilities.dpToPixels(TRAY_DIM_X_DP, getResources()),
				Utilities.dpToPixels(TRAY_DIM_Y_DP - 3, getResources()),
				WindowManager.LayoutParams.TYPE_PHONE, 
				WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE 
				| WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, 
				PixelFormat.TRANSLUCENT);

		mRootLayoutParams.gravity = Gravity.CENTER;
		mWindowManager.addView(mRootLayout, mRootLayoutParams);
		isShowdialogAsk = true;
		
	}
	public void dismissDialog(View view){
		if(isShowdialogAsk){
			hideAskDialog();
		}
		stopService();
	}
	/**
	 * Hide ask dialog.
	 */
	private void hideAskDialog(){
		isShowdialogAsk = false;
		if (mWindowManager!= null && mRootLayout != null){
			if(mRootLayout.getWindowToken() != null){
				mWindowManager.removeViewImmediate(mRootLayout);
			}
		}
	}
	public void recordThisCall(View view){
		if(isShowdialogAsk) {
			hideAskDialog();
		}
		//mNotificationHelper.createNotification();
		startRecording();
	}
	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		if(isShowdialogAsk){
			hideAskDialog();
		}
		// Add the following line to unregister the Sensor Manager onPause
	    if(mSensorManager != null ) mSensorManager.unregisterListener(mShakeDetector);
	    if(mRecorder != null){
	    	mRecorder.release();
	    	mRecorder = null;
	    }
		super.onDestroy();
		
	}
	
	

}
