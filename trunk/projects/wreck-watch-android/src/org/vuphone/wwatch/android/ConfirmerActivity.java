// TODO - REFACTOR THE HELL OUT OF THIS!!!

package org.vuphone.wwatch.android;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This activity will only start if the starting Intent contains true in its
 * "ShowDialog" extra. In any other case, the activity will automatically exit.
 * Once started, the activity will display a "dialog" (actually part of its
 * layout) asking a user to confirm whether an accident occurred. If the user
 * does not respond in time, an accident will be reported. After processing
 * user's response or taking timeout action, this activity will finish.
 * 
 * Special note: When the keyboard is opened or closed, activities get destroyed
 * and recreated. Therefore, it is necessary to save current time in
 * onSaveInstanceState(Bundle) and then reconstruct the state of the object in
 * onCreate(Bundle).
 * 
 * @author Krzysztof Zienkiewicz
 * 
 */
public class ConfirmerActivity extends Activity implements OnClickListener {

	/**
	 * The string to be displayed in the timeRemaining_. ? will be substituted
	 * with the actual time remaining
	 */
	private static final String REM_STRING = "You have ? seconds remaining";
	/**
	 * The default allowed time. Used if max time wasn't specified in the
	 * preference file
	 */
	private static final int DEFAULT_MAX_TIME = 10;

	/** Buttons for user input */
	private Button yesButton_, noButton_;
	/** TextView displaying the time remaining */
	private TextView timeRemaining_;
	/** The ProgressBar showing time elapsed to time allowed ratio */
	private ProgressBar bar_;

	/** The maximum time alloted to make a decision (in seconds) */
	private int maxTime_;
	/** The time the activity started. 0 means unset*/
	private long startTime_ = 0;

	/** A reference to the vibrator. Used to catch user's attention */
	private Vibrator vibrator_;

	/**
	 * The time object that schedules the countdown. This needs to be explicitly
	 * deactivated in onDestroy() so that its thread dies together with the
	 * Activity
	 */
	private final Timer timer_ = new Timer("Countdown Timer");

	/**
	 * A runnable object that is responsible for time keeping and triggering the
	 * timeout event. This has to be canceled in onDestroy() so that it won't
	 * run after the activity dies
	 */
	private final CountdownTimerTask countdownTask_ = new CountdownTimerTask();

	/** Handler for requesting the main thread to do something */
	private final Handler handler_ = new Handler();

	/**
	 * A private inner class used for timeout control. The countdown will start
	 * with the first execution of run(). This class is responsible for updating
	 * the ProgressBar as well as the TextView.
	 * 
	 * @author Krzysztof Zienkiewicz
	 * 
	 */
	private class CountdownTimerTask extends TimerTask {

		/** Current time (in seconds) determined via startTime */
		private int time_;
		
		/**
		 * A no-op constructor
		 */
		public CountdownTimerTask() {
			super();
			time_ = (int) (System.currentTimeMillis() - startTime_) / 1000;
			Log.v(VUphone.tag, "TimerTask ctor");
		}

		/**
		 * Manage timing. If this timeouts, this task is canceled, removing it
		 * from the timer's queue and purges the timer. report(true) is then
		 * called.
		 */
		@Override
		public void run() {
			time_ = (int) (System.currentTimeMillis() - startTime_) / 1000;
			updateUI();

			if (time_ >= maxTime_) {
				Log.v(VUphone.tag, "TimerTask.run() entering timeout");
				handler_.post(new Runnable() {
					public void run() {
						report(true);
					}
				});
			}
		}

		/**
		 * Used to sync the UI with the data held by this object
		 */
		private void updateUI() {
			handler_.post(new Runnable() {
				public void run() {
					bar_.setProgress(time_);
					String remTimeStr = "" + (maxTime_ - time_);
					timeRemaining_.setText(REM_STRING.replace("?", remTimeStr));
				}
			});
		}
	}

	/**
	 * Called to clean up the Timer and TimerTask and Vibrator. Should only be
	 * called once this activity enters the shutdown path.
	 */
	private void cleanUp() {
		timer_.cancel();
		countdownTask_.cancel();
		vibrator_.cancel();
	}

	/**
	 * Process user input.
	 */
	public void onClick(View v) {
		report(v.equals(yesButton_));
	}

	/**
	 * The "constructor". Sets up the fields and the layout. This is either
	 * called because somebody requested this activity to start up or because
	 * the activity is being reloaded due to a keyboard flip.
	 */
	@Override
	protected void onCreate(Bundle save) {
		super.onCreate(save);
		Log.v(VUphone.tag, "\tonCreate()");
		setContentView(R.layout.confirmer);
		

		yesButton_ = (Button) findViewById(R.id.accident_true);
		yesButton_.setOnClickListener(this);
		noButton_ = (Button) findViewById(R.id.accident_false);
		noButton_.setOnClickListener(this);

		timeRemaining_ = (TextView) findViewById(R.id.time_remaining);

		vibrator_ = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		maxTime_ = getSharedPreferences(VUphone.PREFERENCES_FILE,
				Context.MODE_PRIVATE).getInt(VUphone.TIMEOUT_TAG,
				DEFAULT_MAX_TIME);

		bar_ = (ProgressBar) findViewById(R.id.progress_bar);
		bar_.setMax(maxTime_);

		if (save != null && save.containsKey("Time"))
			startTime_ = save.getLong("Time");
	}

	/**
	 * The "destructor" cleanUp() should have already been called so this is a
	 * no-op.
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.v(VUphone.tag, "\tonDestroy()");
	}

	/**
	 * Called before the Activity is killed (due to keyboard flip). Saves the
	 * current time so that we can recreate object state later.
	 */
	@Override
	public void onSaveInstanceState(Bundle save) {
		super.onSaveInstanceState(save);
		save.putLong("Time", startTime_);
	}

	/**
	 * The "main" method. Activate the "dialog" if started with the correct
	 * intent. Else, finish the activity.
	 */
	@Override
	public void onStart() {
		super.onStart();
		Log.v(VUphone.tag, "\tonStart()");
		
		// If startTime_ hasn't been set from the save Bundle
		if (startTime_ == 0)
			startTime_ = System.currentTimeMillis();

		// Intent intent = this.getIntent();
		// if (intent.getBooleanExtra("ShowDialog", false)) {
		// Schedule the timer task
		timer_.scheduleAtFixedRate(countdownTask_, 1000, 1000);
		// } else {
		// finish();
		// }
	}

	/**
	 * Takes the necessary actions to report whether an accident occurred.
	 * Cleans up and finishes the activity.
	 * 
	 * @param occurred
	 */
	private void report(boolean occurred) {
		cleanUp();
		Toast.makeText(this, "Report: " + occurred, Toast.LENGTH_SHORT).show();

		// Let the deceleration service know if there was a wreck
		Intent message = new Intent(this, DecelerationService.class);
		message.putExtra("WreckOccurred", occurred);
		// startService(message);

		Intent intent = new Intent(this,
				org.vuphone.wwatch.android.GPService.class);
		intent.putExtra("DidAccidentOccur", occurred);
		// startService(intent);

		finish();
	}
}