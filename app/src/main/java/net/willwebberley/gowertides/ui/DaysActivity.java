/*
Copyright 2013 Will Webberley.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

The full text of the License is available in the root of this
project repository.
*/

package net.willwebberley.gowertides.ui;

import java.text.SimpleDateFormat;
import java.util.*;

import android.net.Uri;
import net.willwebberley.gowertides.R;
import net.willwebberley.gowertides.classes.*;
import net.willwebberley.gowertides.utils.DayDatabase;
import net.willwebberley.gowertides.utils.Utilities;
import net.willwebberley.gowertides.utils.WeatherDatabase;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ScrollView;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;

/*
 * Main Activity of application
 *
 * This class maintains the ViewPager of days as well as indicating the currently selected day and handles some button
  * press events.
  *
  * ViewPager contains a list of fragments, each representing a day.
  *
  * This class ia also responsible for network tasks (getting weather), and communicating this to the day fragments.
  *
 */
public class DaysActivity extends ActionBarActivity {

	private SharedPreferences prefs;
	private ViewPager infoPager;
    private Menu menu;
    private int todayFragmentIndex;
    private int currentFragmentIndex;
	private Calendar currentDay, firstDay, lastDay;
	private Vector<DayFragment> fragments;
	private PagerAdapter mPagerAdapter;
	public DayDatabase db;
	public WeatherDatabase weather_db;
    public Boolean isPaused, isSyncing;

    private final int DAYS_TO_STORE = 40;

    private RelativeLayout buildProgressHolder;
    private ProgressBar buildProgress;

    public String [] locationNames;
    public int[] locationKeys;
    public int locationIndex;

    private int pauseCounter = 0;

    /*
    * onCreate() called upon activity start.
    *
    * Responsible for initializing major components, loading app preferences and initializing some global variables.
    * Creates the viewpager object and assigns an adapter.
    *
    * Starts the StartupTasks thread to handle long-running startup tasks (while progress bar shows on UI).
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dayview);

        // Add sync progressbar to actionbar
        ProgressBar progressBar = new ProgressBar(this);
        progressBar.setVisibility(View.GONE);
        progressBar.setIndeterminate(true);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(progressBar);

        // Allow theme to colour the statusbar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        }


        /*
        * Following two variables used by day fragments to check the status of parent activity
         */
        isPaused = false;
        isSyncing = false;

        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        initComponents();
        fragments = new Vector<DayFragment>();
        mPagerAdapter = new PagerAdapter(super.getSupportFragmentManager(), fragments);
        infoPager.setAdapter(mPagerAdapter);

        new StartupTasks().execute("");
    }

    /*
    * Called from StartupTasks thread and is used to populate the viewpager with the day fragments.
    *
    * Creates DAYS_TO_STORE number of day fragments equally distributed around the current day and calculates and stores
    * the index of the current day of the list.
     */
    private void populatePager(Calendar newToday, int daysToLoad){
        fragments.clear();
        Calendar startDay = (Calendar)newToday.clone();
        // Change the day to start the pager at (e.g., if 4, will start at day - DAYS_TO_STORE/4 and end at
        // 3*DAYS_TO_STORE/4.
        startDay.add(Calendar.DATE,-(daysToLoad/4));
        for(int i =0; i < DAYS_TO_STORE; i++){
            Calendar newDay = (Calendar)startDay.clone();
            newDay.add(Calendar.DATE,i);

            Day day = Utilities.createDay(db,weather_db, newDay, locationKeys[locationIndex]);
            DayFragment dayFrag = new DayFragment(day, prefs, this);

            if (currentDay.getTimeInMillis() == newDay.getTimeInMillis()){
                todayFragmentIndex = i;
            }
            fragments.add(dayFrag);
            if (newDay.getTimeInMillis() == lastDay.getTimeInMillis()){
                break;
            }
        }
    }

    /*
    * Inner class to act as the PagerAdapter for the viewpager.
    *
    * Implements various methods to assist with handling the pager.
     */
    class PagerAdapter extends FragmentPagerAdapter {
        private List<DayFragment> fragments;
				private boolean doNotifyDataSetChangedOnce = false;
        public PagerAdapter(android.support.v4.app.FragmentManager fm, List<DayFragment> fragments) {
            super(fm);
            this.fragments = fragments;
        }
        @Override
        public DayFragment getItem(int position) {
            return this.fragments.get(position);
        }
        @Override
        public int getCount() {
					if (doNotifyDataSetChangedOnce) {
						doNotifyDataSetChangedOnce = false;
						notifyDataSetChanged();
					}
            return this.fragments.size();
        }
        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }
    }

    /*
     * Invoke this method (in onCreate()) for testing.
     * Allows the app to be started at a desired date instead of 'day'
     */
    private Calendar setDayForTesting(String test){
    	Date tester = null;
    	try{
    		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
    		tester = (Date)sdf.parse(test);
    	}
    	catch(Exception e){
    		System.err.println(e);
    	}

    	Calendar cal = Calendar.getInstance();
        cal.setTime(tester);

    	return cal;
    }

    /*
     * Generate options menu (from activity_dayview.xml in menu/)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_dayview, menu);
        this.menu = menu;
        return true;
    }

    /*
     * When application resumes, refresh the UI of each day fragment..
     */
    public void onResume(){
    	super.onResume();
        isPaused = false;
        if(pauseCounter > 0){
            try{
                fragmentsRefreshUI();
            }
            catch(Exception e){
                System.err.println(e);
            }
        }
        pauseCounter ++;
    }

    @Override
    public void onPause() {
        super.onPause();
        isPaused = true;
    }

    /*
     * Listen for click events on the options menu.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_about:
            	Intent intent_about = new Intent(this, AboutActivity.class);
                startActivity(intent_about);
                return true;
            case R.id.menu_settings:
            	Intent intent_settings = new Intent(this, PreferencesActivity.class);
                startActivity(intent_settings);
                return true;
            case R.id.menu_revert:
            	toDay();
                return true;
            case R.id.sync:
                refresh();
                return true;
            case R.id.menu_location:
                editLocation();
                return true;
            case R.id.next_day:
                addDay(1);
                return true;
            case R.id.prev_day:
                addDay(-1);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    /*
     * Executed if there's an issue loading the requested date. If exception thrown
     * assume date doesn't exist and make the error message visible.
     */
    private void dayNotFound(){
    	((TextView)findViewById(R.id.dayNotFound)).setVisibility(View.VISIBLE);
    	((ScrollView)findViewById(R.id.scrollView)).setVisibility(View.GONE);
    }

    /*
        Add or remove days (e.g. addDay(1) goes to tomorrow and addDay(-1) goes to yesterday)
     */
    public void addDay(int days){
        try{
            infoPager.setCurrentItem(infoPager.getCurrentItem()+days, true);
        }
        catch(Exception e){
            System.err.println("Could not load day");
            dayNotFound();
        }
    }

    /*
     * Set the current day to the current day. If error, assume no data for day.
     * (Hopefully this will never result in a error if app is updated!)
     */
    public void toDay(){
    	try{
            infoPager.setCurrentItem(todayFragmentIndex, true);
    	}
    	catch(Exception e){
    		System.err.println("Could not load day");
    		dayNotFound();
    	}
    }

    /*
    * Listen for clicks on MSW logo, and open up their site if clicked.
     */
    public void openMSW(View view){
        String url = "http://www.magicseaweed.com";
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        startActivity(i);
    }

    /*
    * Listen for clicks on 'location edit' image to open location select dialog.
     */
    public void editLocation(){
        LocationDialog ld = new LocationDialog();
        Bundle args = new Bundle();
        args.putStringArray("names", locationNames);
        args.putIntArray("keys", locationKeys);
        ld.setArguments(args);
        ld.show(getSupportFragmentManager(), "");

    }

    /*
    * If network available, sync surf and weather data
     */
    public void refresh(){
        if(isSyncing){
            return;
        }
        getSupportActionBar().getCustomView().setVisibility(true ? View.VISIBLE : View.GONE);
        isSyncing = true;
        if(this.isOnline()){
            new Utilities.DataGetter(this, locationKeys[locationIndex], weather_db).execute();
        }
        else{
            Toast.makeText(getApplicationContext(), "Unable to sync: network unavailable.", Toast.LENGTH_LONG).show();
            finishRefresh();
        }
    }

    /*
    * Called upon completion of the refresh task
     */
    public void notifySyncFinished(Boolean result){
        finishRefresh();
        if(!result){
            Toast.makeText(getApplicationContext(), "Sync error: Please try again later.", Toast.LENGTH_LONG).show();
        }
        isSyncing = false;
    }

    /*
     * Check for network connection. If Internet connection return true. Else, return false.
     */
    public boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(getApplicationContext().CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            return true;
        }
        return false;
    }

    public void updateLocation(int index){
        locationIndex = index;
        fragmentsRefreshUI();
        refresh();

        // Update prefs last in case there's an error with the location (as this would prevent app
        // from opening again!
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("location_index",index);
        editor.commit();
    }

    /*
    * Update Fragments in ViewPager. Will only update the three Fragments currently in memory.
    *
    * These three methods execute different methods within the loaded pages.
     */
    private void fragmentsRefreshUI(){
        Utilities.reprocessDay(db, weather_db, ((DayFragment)fragments.get(currentFragmentIndex)).day, locationKeys[locationIndex]);
        ((DayFragment)fragments.get(currentFragmentIndex)).refreshUI();
        try{
            Utilities.reprocessDay(db, weather_db, ((DayFragment)fragments.get(currentFragmentIndex-1)).day, locationKeys[locationIndex]);
            ((DayFragment)fragments.get(currentFragmentIndex-1)).refreshUI();
            Utilities.reprocessDay(db, weather_db, ((DayFragment)fragments.get(currentFragmentIndex+1)).day, locationKeys[locationIndex]);
            ((DayFragment)fragments.get(currentFragmentIndex+1)).refreshUI();
        }
        catch(Exception e){
            System.err.println("At end of viewpager.");
        }
    }

    /*
    * Called when network sync finishes, to update the UI (i.e. hide progress bar and re-show the button)
     */
    private void finishRefresh(){
        getSupportActionBar().getCustomView().setVisibility(false ? View.VISIBLE : View.GONE);
        fragmentsRefreshUI();
    }

    /*
     * Get the layout components initialized and make their variable names global.
     */
    private void initComponents(){
        infoPager = (ViewPager)findViewById(R.id.infoPager);
        buildProgress = (ProgressBar)findViewById(R.id.buildProgress);
        buildProgressHolder = (RelativeLayout)findViewById(R.id.buildProgressHolder);
    }

    /*
    * Threaded task to run startup routine (while progressbar shows on UI).
    *
    * Responsible for initialising view pager and other general tasks.
    * On finish, onPostExecute() is called.
     */
    private class StartupTasks extends AsyncTask<String, Integer, Boolean>{
        @Override
        protected Boolean doInBackground(String... arg0) {
            locationNames = getResources().getStringArray(R.array.locationDisplay);
            locationKeys = getResources().getIntArray(R.array.locationKey);
            locationIndex = prefs.getInt("location_index",0);

            System.out.println("Initializing databases...");
            try{
                db = new DayDatabase(getApplicationContext());
                weather_db = new WeatherDatabase(getApplicationContext());
                firstDay = db.getFirstDay();
                lastDay = db.getLastDay();
            }
            catch(Exception e){
                System.err.println(e);
            }
            System.out.println("Preparing day fragments...");
            currentDay = Calendar.getInstance();
            //currentDay = setDayForTesting("01/01/2031");

            System.out.println("Populating viewpager...");
            populatePager(currentDay, DAYS_TO_STORE);

            System.out.println("Setting listener...");
            infoPager.setOnPageChangeListener(new OnPageChangeListener() {
                public void onPageScrollStateChanged(int state) {}
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}
                public void onPageSelected(int position) {
                    currentFragmentIndex = position;
                    DayFragment myNow = mPagerAdapter.getItem(position);
                    myNow.slideSurf();
                    getSupportActionBar().setSubtitle(myNow.day.toString());
                    if(position == todayFragmentIndex){
                        menu.findItem(R.id.menu_revert).setVisible(false);
                    }
                    else{
                        menu.findItem(R.id.menu_revert).setVisible(true);
                    }
                }
            });

            /*
            * Set each Day's yesterday Day and tomorrow Day (if available)
             */
            for(int i=0; i < fragments.size(); i++){
                if(i > 0){
                    Day yesterday = fragments.get(i-1).day;
                    fragments.get(i).day.setYesterday(yesterday);
                }
                if(i < fragments.size()-1){
                    Day tomorrow = fragments.get(i+1).day;
                    fragments.get(i).day.setTomorrow(tomorrow);
                }
            }
            return true;
        }
        protected void onPostExecute(Boolean result) {
            infoPager.getAdapter().notifyDataSetChanged();
            infoPager.setCurrentItem(todayFragmentIndex); // set initial pager position to current day

            /*
            * If preference is to sync weather on startup, then sync weather task now.
            * (this is done after initialising day fragments due to UI updates on the fragments during this task.)
             */
            if(prefs.getBoolean("sync_enabled", true)) {
                refresh();
            }

            System.out.println("Final bits...");
            buildProgressHolder.setVisibility(View.GONE);
            buildProgress.setVisibility(View.GONE);
            infoPager.setVisibility(View.VISIBLE);
            System.out.println("Done.");

        }
    }


}
