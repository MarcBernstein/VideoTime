package info.marcbernstein.videotime;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

  private static final String USER_ID_KID = "0";
  private static final String USER_ID_PARENT = "100";
  private static final String TAG = MainActivity.class.getSimpleName();
  private static final String TIME_TO_USE = "time_to_use";
  private static final String MINUTES_TOTAL = "minutes_total";

  private FirebaseAnalytics mFirebaseAnalytics;

  @BindView(R.id.textview_minutes)
  TextView mTextViewMinutes;

  @BindView(R.id.fab)
  FloatingActionButton mFab;

  @BindView(R.id.toolbar)
  Toolbar mToolbar;

  @Inject
  protected FirebaseDatabase mFirebaseDatabase;

  private DatabaseReference mRefMinutesTotal;

  private Long mMinutesVal;

  private DatabaseReference mRefTimeToUse;

  private Long mTimeToUse;

  private ValueEventListener mMinutesTotalListener = new ValueEventListener() {
    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {
      mMinutesVal = dataSnapshot.getValue(Long.class);

      if (mMinutesVal == null || mMinutesVal < 0L) {
        mMinutesVal = 0L;
      }

      getSharedPrefs().edit()
          .putLong(MINUTES_TOTAL, mMinutesVal)
          .apply();

      if (mTextViewMinutes != null) {
        mTextViewMinutes.setText(String.valueOf(mMinutesVal));
      }
    }

    @Override
    public void onCancelled(DatabaseError error) {
      Log.w(TAG, "Failed to read value.", error.toException());
      FirebaseCrash.report(error.toException());
    }
  };

  private ValueEventListener mTimeToUseValueEventListener = new ValueEventListener() {
    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {
      mTimeToUse = dataSnapshot.getValue(Long.class);
      if (mRefTimeToUse == null) {
        mTimeToUse = 15L;
      }

      getSharedPrefs().edit()
          .putLong(TIME_TO_USE, mTimeToUse)
          .apply();

      Log.d(TAG, "Time To Use update: " + mTimeToUse);
    }

    @Override
    public void onCancelled(DatabaseError databaseError) {
      FirebaseCrash.report(databaseError.toException());
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    ButterKnife.bind(this);
    App.inject(this);

    String userId = getSharedPrefs().getString("USER_ID", USER_ID_KID);
    mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
    mFirebaseAnalytics.setUserId(userId);

    String token = FirebaseInstanceId.getInstance().getToken();
    logAppStart(token);

    setSupportActionBar(mToolbar);

    mTimeToUse = getSharedPrefs().getLong(TIME_TO_USE, 15);

    Long minutes = getSharedPrefs().getLong(MINUTES_TOTAL, 0L);
    mTextViewMinutes.setText(String.valueOf(minutes));

    mFab.setOnClickListener(this::fabClick);
  }

  private SharedPreferences getSharedPrefs() {
    return getSharedPreferences(BuildConfig.APPLICATION_ID, MODE_PRIVATE);
  }

  private void logAppStart(@Nullable String token) {
    Log.i(TAG, "[logAppStart] Token: " + token);
    mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, new Bundle());

    mFirebaseDatabase.getReference("current_app_token").setValue(token != null ? token : "Null Token");
  }

  private void fabClick(View view) {
    Bundle bundle = new Bundle();
    bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "fab");
    bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "click");
    bundle.putLong(FirebaseAnalytics.Param.QUANTITY, mMinutesVal);
    mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);

    if (mMinutesVal <= 0L) {
      if (view != null) {
        Snackbar snackbar = Snackbar.make(view, R.string.no_minutes, Snackbar.LENGTH_SHORT);
        snackbar.setAction(R.string.no_minutes_action_msg, v -> snackbar.dismiss()).show();
      }
      return;
    }

    final AtomicBoolean processing = new AtomicBoolean(false);
    if (view != null) {
      Snackbar.make(view, getString(R.string.used_minutes, mTimeToUse), Snackbar.LENGTH_LONG)
          .setAction("UNDO", v -> undoTimeUse(processing)).show();

      mMinutesVal -= mTimeToUse;
      mMinutesVal = Math.max(mMinutesVal, 0);
      mRefMinutesTotal.setValue(mMinutesVal);
    }
  }

  private void undoTimeUse(AtomicBoolean processing) {
    if (processing.get()) {
      return;
    }
    processing.compareAndSet(false, true);
    mMinutesVal += mTimeToUse;
    mRefMinutesTotal.setValue(mMinutesVal);
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
    if (id == R.id.action_parent_mode) {
      Toast.makeText(this, "Not implemented", Toast.LENGTH_SHORT).show();
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onResume() {
    super.onResume();

    mRefMinutesTotal = mFirebaseDatabase.getReference(MINUTES_TOTAL);
    mRefMinutesTotal.addValueEventListener(mMinutesTotalListener);

    mRefTimeToUse = mFirebaseDatabase.getReference(TIME_TO_USE);
    mRefTimeToUse.addValueEventListener(mTimeToUseValueEventListener);
  }

  @Override
  public void onPause() {
    super.onPause();

    mRefMinutesTotal.removeEventListener(mMinutesTotalListener);
    mRefMinutesTotal = null;

    mRefTimeToUse.removeEventListener(mTimeToUseValueEventListener);
    mRefMinutesTotal = null;
  }
}
