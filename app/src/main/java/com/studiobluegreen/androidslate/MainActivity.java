package com.studiobluegreen.androidslate;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.Time;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private Timer mainTimer;					// Timer
    private MainTimerTask mainTimerTask;		// Timer task
    private TextView countText;					// Textview on activity
    private Handler mHandler = new Handler();   // Post handler for UI thread
    private TimeCode timeCode;
    private SharedPreferences sharedPreferences;
    private Typeface myTypeface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        /**
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        **/

        double frameRate = Double.valueOf(sharedPreferences.getString("timecode_framerate", "29.97"));
        boolean isDF = sharedPreferences.getString("timecode_dropframe", "Drop").equals("Drop");
        this.timeCode = new TimeCode(frameRate, isDF);

        this.timeCode.initToNow();
        this.mainTimer = new Timer();
        this.mainTimerTask = new MainTimerTask();
        double interval = 1000.0 / this.timeCode.getFrameRate();
        this.mainTimer.schedule(mainTimerTask, 1000, (int)Math.floor(interval));
        this.countText = (TextView)findViewById(R.id.countTextView);

        myTypeface = Typeface.createFromAsset(getAssets(), "fonts/ufonts.com_led-opentype.otf");
        countText.setTypeface(myTypeface);

        TextView dateText = (TextView)findViewById(R.id.textDate);
        Calendar today = Calendar.getInstance();
        dateText.setText(String.format(Locale.US, "%4d/%d/%02d",
                today.get(Calendar.YEAR),
                today.get(Calendar.MONTH) + 1,
                today.get(Calendar.DATE)));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public class MainTimerTask extends TimerTask {
        @Override
        public void run() {
            timeCode.incrementFrame();
            mHandler.post( new Runnable() {
                public void run() {
                    countText.setText(timeCode.getFormatedString());
                }
            });
        }
    }

    public class TimeCode {
        private int hours = 1;
        private int minutes = 0;
        private int seconds = 0;
        private int frames = 0;
        private double frameRate = 29.97;
        private Boolean isDF = true;

        TimeCode()
        {
            // all default values
        }

        TimeCode(double frameRate, boolean isDF)
        {
            this.frameRate = frameRate;
            this.isDF = isDF;
        }

        public void initToNow()
        {
            toNow();
            adjustDF();
        }

        public void set(int hours, int minutes, int seconds, int frames)
        {
            this.hours = hours;
            this.minutes = minutes;
            this.seconds = seconds;
            this.frames = frames;
        }

        public double getFrameRate()
        {
            return this.frameRate;
        }

        public void incrementFrame()
        {
            this.frames++;

            if (this.frames >= this.frameRate)
            {
                this.frames = 0;
                this.seconds++;
                if (this.seconds > 59)
                {
                    this.seconds = 0;
                    this.minutes++;
                    if (this.minutes > 59)
                    {
                        this.minutes = 0;
                        this.hours++;
                        if (this.hours > 23)
                        {
                            this.hours = 0;
                        }
                        // adjust to real time value every minute
                        toNow();
                    }
                }
                adjustDF();
            }
        }

        public String getFormatedString()
        {
            return String.format(Locale.US, (isDF ? "%02d:%02d:%02d;%02d" : "%02d:%02d:%02d:%02d"), this.hours, this.minutes, this.seconds, this.frames);
        }

        // set property values to Now
        private void toNow()
        {
            Calendar today = Calendar.getInstance();
            this.hours = today.get(Calendar.HOUR_OF_DAY);
            this.minutes = today.get(Calendar.MINUTE);
            this.seconds = today.get(Calendar.SECOND);

            double interval = 1000 / this.frameRate;

            this.frames = (int) Math.floor(today.get(Calendar.MILLISECOND) / interval);
        }

        // Adjust for drop frame
        private void adjustDF()
        {
            if (isDF && this.minutes != 0 && (this.minutes % 10) > 0 && this.seconds == 0 && this.frames < 2)
            {

                this.frames = 2;
            }
        }
    }
}
