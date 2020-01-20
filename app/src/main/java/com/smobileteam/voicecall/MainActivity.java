package com.smobileteam.voicecall;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.smobileteam.voicecall.adapter.DatabaseAdapter;
import com.smobileteam.voicecall.adapter.SearchAdapter;
import com.smobileteam.voicecall.adapter.ViewPagerAdapter;
import com.smobileteam.voicecall.controller.AppRater;
import com.smobileteam.voicecall.customview.SlidingTabLayout;
import com.smobileteam.voicecall.customview.SwitchButton;
import com.smobileteam.voicecall.model.RecordModel;
import com.smobileteam.voicecall.model.SearchItem;
import com.smobileteam.voicecall.utils.AndroidUtils;
import com.smobileteam.voicecall.utils.MyConstants;
import com.smobileteam.voicecall.utils.PreferUtils;
import com.smobileteam.voicecall.utils.Utilities;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private Context mContext;

    private DrawerLayout mDrawerLayout;
    private SwitchButton mOnOffService;
    private ViewPager pager;
    private ViewPagerAdapter adapter;
    private SlidingTabLayout tabs;
    private String Titles[];
    private int Numboftabs =2;
    public Toolbar mToolbar;
    //For Search function
    private MenuItem mSearchAction;
    private boolean isSearchOpened = false;
    private AutoCompleteTextView edtSeach;

    //passcode screen
    private View mPasscodeScreen;
    private PasscodeScreen mPasscode;

    // For admob
    private AdView adView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_main);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);



        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, mDrawerLayout, mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawerLayout.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        Titles = new String[]{mContext.getString(R.string.tab_inbox), mContext.getString(R.string.tab_favorite)};
        // Creating The ViewPagerAdapter and Passing Fragment Manager, Titles fot the Tabs and Number Of Tabs.
        adapter =  new ViewPagerAdapter(getSupportFragmentManager(),Titles,Numboftabs);
        // Assigning ViewPager View and setting the adapter
        pager = (ViewPager) findViewById(R.id.pager);
        pager.setAdapter(adapter);
        // Assiging the Sliding Tab Layout View
        tabs = (SlidingTabLayout) findViewById(R.id.tabs);
        // To make the Tabs Fixed set this true, This makes the tabs Space Evenly in Available width
        tabs.setDistributeEvenly(true);
        // Setting Custom Color for the Scroll bar indicator of the Tab View
        tabs.setCustomTabColorizer(new SlidingTabLayout.TabColorizer() {
            @Override
            public int getIndicatorColor(int position) {
                return getResources().getColor(R.color.enableServiceHint);
            }
        });

        // Setting the ViewPager For the SlidingTabsLayout
       tabs.setViewPager(pager);

        //external feature
        if (!PreferUtils.getbooleanPreferences(this, MyConstants.KEY_PRIVATE_MODE)) {
            AppRater.app_launched(mContext);
        }
        boolean isNoSupport = getIntent().getBooleanExtra("warning_no_support_key", false);
        if(isNoSupport){
            showDialogWarningNoSupportRecord();
        }
        if ((!isNoSupport
                && !PreferUtils.getbooleanPreferences(mContext, MyConstants.KEY_DONT_SHOW_AGAIN)
                && AndroidUtils.isAtLeastM() && AndroidUtils.hasRequiredPermissions(mContext))
                || (!isNoSupport
                && !PreferUtils.getbooleanPreferences(mContext,MyConstants.KEY_DONT_SHOW_AGAIN)
                && !AndroidUtils.isAtLeastM())) {
            showDialogInformLimitInbox();
        }

        //Passcode
        initPasscodeUI();
        initAdsAdmob();

    }
    private void initAdsAdmob(){
        // Look up the AdView as a resource and load a request.
        adView = (AdView) findViewById(R.id.adView_main);
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(MyConstants.TEST_DEVICE_ID)
                .build();
        adView.loadAd(adRequest);
    }
    private void resumeAds(){
        adView.resume();
    }
    private void pauseAds(){
        adView.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(MyConstants.TAG, "isNeedShowPasscode = "+CallRecorderApp.isNeedShowPasscode);
        if(CallRecorderApp.isNeedShowPasscode && PreferUtils.getbooleanPreferences(this, MyConstants.KEY_PRIVATE_MODE)
                && !PreferUtils.getbooleanPreferences(mContext, MyConstants.KEY_IS_LOGINED)){
            mPasscode.showPasscode();
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        }
        resumeAds();
    }

    @Override
    protected void onPause() {
        super.onPause();
        pauseAds();
    }

    @Override
    protected void onStop() {
        super.onStop();
        CallRecorderApp.isNeedShowPasscode = true;
        PreferUtils.savebooleanPreferences(mContext, MyConstants.KEY_IS_LOGINED, false);
        Log.d(MyConstants.TAG, "isNeedShowPasscode = "+CallRecorderApp.isNeedShowPasscode);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if(isSearchOpened) {
                handleMenuSearch();
                return;
            }
            super.onBackPressed();
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        mSearchAction = menu.findItem(R.id.action_search);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        View imOnOffService = menu.findItem(R.id.enable_service).
                getActionView();
        if(imOnOffService != null) mOnOffService = (SwitchButton) imOnOffService.findViewById(R.id.switchForActionBar);
        mOnOffService.setChecked(PreferUtils.getbooleanPreferences(mContext,MyConstants.SERVICE_ENABLED));
        onToggleClicked();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id){
            case R.id.action_search:
                handleMenuSearch();
                break;
        }


        return super.onOptionsItemSelected(item);
    }
    protected void handleMenuSearch(){
        ActionBar action = getSupportActionBar(); //get the actionbar

        if(isSearchOpened){ //test if the search is open

            action.setDisplayShowCustomEnabled(false); //disable a custom view inside the actionbar
            action.setDisplayShowTitleEnabled(true); //show the title in the action bar

            //hides the keyboard
            Utilities.showOrHideKeyboard(this,edtSeach,false);

            //add the search icon in the action bar
            mSearchAction.setIcon(getResources().getDrawable(R.drawable.ic_search));
            isSearchOpened = false;
        } else { //open the search entry

            action.setDisplayShowCustomEnabled(true); //enable it to display a
            // custom view in the action bar.
            action.setCustomView(R.layout.search_bar);//add the custom view
            action.setDisplayShowTitleEnabled(false); //hide the title

            edtSeach = (AutoCompleteTextView)action.getCustomView().findViewById(R.id.search_layout_edt_inputkeyword); //the text editor

            //this is a listener to do a search when the user clicks on search button
            edtSeach.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                        doSearch();
                        return true;
                    }
                    return false;
                }
            });
            edtSeach.requestFocus();
            DatabaseAdapter database = DatabaseAdapter.getInstance(mContext);
            final ArrayList<SearchItem> dataSearch = database.getListSearchIndex();
            SearchAdapter searchAdapter = new SearchAdapter(mContext,R.layout.item_search, dataSearch);
            edtSeach.setAdapter(searchAdapter);
            edtSeach.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View view,
                                        int position, long id) {
                    // TODO Auto-generated method stub
                    RecordModel record = new RecordModel(dataSearch.get(position));
                    File file = new File(record.getPath());
                    if(!file.exists()){
                        Utilities.showToast(mContext, getString(R.string.file_is_not_exist));
                    }else {
                        Intent playIntent = new Intent(mContext, PlayerActivity.class);
                        playIntent.putExtra(MyConstants.KEY_SEND_RECORD_TO_PLAYER,record);
                        playIntent.putExtra(MyConstants.KEY_ACTIVITY, MyConstants.MAIN_ACTIVITY);
                        playIntent.putExtra(MyConstants.KEY_RECORD_TYPE_PLAY, MyConstants.FROM_SEARCH);
                        startActivity(playIntent);
                        CallRecorderApp.isNeedShowPasscode = false;
                    }

                }
            });
            //open the keyboard focused in the edtSearch
            Utilities.showOrHideKeyboard(this,edtSeach,true);
            //add the close icon
            mSearchAction.setIcon(getResources().getDrawable(R.drawable.ic_close_search));
            isSearchOpened = true;
        }
    }

    /**
     * Implement query search
     */
    private void doSearch() {
        Toast.makeText(this,"search",Toast.LENGTH_LONG).show();
    }

    public void onToggleClicked() {
        // Is the toggle on?
        mOnOffService.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean status) {
                if (status) {
                    // Enable record Service
                    PreferUtils.savebooleanPreferences(mContext, MyConstants.SERVICE_ENABLED, true);

                } else {
                    // Disable record Service
                    PreferUtils.savebooleanPreferences(mContext, MyConstants.SERVICE_ENABLED, false);

                }
            }
        });
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_cloud) {
            startActivity(new Intent(this,CloudActivity.class));
        } else if (id == R.id.nav_storage) {
            startActivity(new Intent(this,StorageActivity.class));
        } else if (id == R.id.nav_settings) {
            startActivity(new Intent(this,SettingActivity.class));
        }


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * dialog waring having some problems when record failed
     */
    private void showDialogWarningNoSupportRecord(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.warning_title);
        builder.setMessage(R.string.warning_device_no_support_voice_call);
        builder.setPositiveButton(R.string.string_ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
//				MyFileManager.deleteAllRecords(mContext); // Nen de trong mot Thread
                dialog.dismiss();
                finish();
            }
        });

        builder.show();
    }

    private void showDialogInformLimitInbox(){
        View checkBoxView = View.inflate(this, R.layout.dialog_limited_inform, null);
        final CheckBox checkBox = (CheckBox) checkBoxView.findViewById(R.id.checkbox);
        TextView message = (TextView) checkBoxView.findViewById(R.id.content);
        message.setMovementMethod(new ScrollingMovementMethod());
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_inform_limit_inbox_title);
        builder.setView(checkBoxView)
                .setPositiveButton(R.string.string_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if(checkBox.isChecked()){
                            PreferUtils.savebooleanPreferences(mContext, MyConstants.KEY_DONT_SHOW_AGAIN, true);
                        }
                        dialog.dismiss();
                    }
                }).show();
    }

    private void initPasscodeUI(){
        mPasscodeScreen = findViewById(R.id.main_passcode);
        Button number_0 = (Button) findViewById(R.id.numpad_0);
        Button number_1 = (Button) findViewById(R.id.numpad_1);
        Button number_2 = (Button) findViewById(R.id.numpad_2);
        Button number_3 = (Button) findViewById(R.id.numpad_3);
        Button number_4 = (Button) findViewById(R.id.numpad_4);
        Button number_5 = (Button) findViewById(R.id.numpad_5);
        Button number_6 = (Button) findViewById(R.id.numpad_6);
        Button number_7 = (Button) findViewById(R.id.numpad_7);
        Button number_8 = (Button) findViewById(R.id.numpad_8);
        Button number_9 = (Button) findViewById(R.id.numpad_9);
        ImageButton number_erase = (ImageButton) findViewById(R.id.button_erase);

        EditText pinfield_1 = (EditText) findViewById(R.id.pin_field_1);
        EditText pinfield_2 = (EditText) findViewById(R.id.pin_field_2);
        EditText pinfield_3 = (EditText) findViewById(R.id.pin_field_3);
        EditText pinfield_4 = (EditText) findViewById(R.id.pin_field_4);
        TextView resetPassword = (TextView) findViewById(R.id.passcode_txt_reset_password);
        mPasscode = new PasscodeScreen();
        mPasscode.setDrawerLayout(mDrawerLayout);
        mPasscode.initLayout(this,mPasscodeScreen,number_0, number_1, number_2, number_3,
                number_4, number_5, number_6, number_7, number_8, number_9,
                number_erase,pinfield_1,pinfield_2,pinfield_3,pinfield_4,resetPassword);


    }
}
