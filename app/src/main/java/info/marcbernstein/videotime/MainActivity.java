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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import info.marcbernstein.videotime.realm.Minutes;
import info.marcbernstein.videotime.realm.TimeToUse;
import io.realm.Realm;

public class MainActivity extends AppCompatActivity {

  private static final String USER_ID_KID = "0";
  private static final String USER_ID_PARENT = "100";
  private static final String TAG = MainActivity.class.getSimpleName();
  private static final String TIME_TO_USE = "time_to_use";
  private static final String MINUTES_TOTAL = "minutes_total";
  private static final long DEFAULT_TIME_TO_USE = 15L;

  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("hh:mm:ss:SS a MMM d", Locale.US);

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

  private final ValueEventListener mMinutesTotalListener = new ValueEventListener() {
    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {
      mMinutesVal = dataSnapshot.getValue(Long.class);

      if (mMinutesVal == null || mMinutesVal < 0L) {
        mMinutesVal = 0L;
      }

      executeRealmTransaction(this::updateMinutes);

      if (mTextViewMinutes != null) {
        mTextViewMinutes.setText(String.valueOf(mMinutesVal));
      }
    }

    private void updateMinutes(Realm realm) {
      Minutes minutes = realm.where(Minutes.class).findFirst();
      if (minutes == null) {
        minutes = realm.createObject(Minutes.class);
      }
      minutes.minutes = mMinutesVal;
    }

    @Override
    public void onCancelled(DatabaseError error) {
      Log.w(TAG, "Failed to read value.", error.toException());
      FirebaseCrash.report(error.toException());
    }
  };

  private void executeRealmTransaction(Realm.Transaction transaction) {
    Realm realm = null;
    try {
      realm = Realm.getDefaultInstance();
      realm.executeTransaction(transaction);
    } finally {
      if (realm != null) {
        realm.close();
      }
    }
  }

  private final ValueEventListener mTimeToUseValueEventListener = new ValueEventListener() {
    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {
      mTimeToUse = dataSnapshot.getValue(Long.class);
      if (mRefTimeToUse == null) {
        mTimeToUse = DEFAULT_TIME_TO_USE;
      }

      executeRealmTransaction(this::updateTimeToUse);
      Log.d(TAG, "Time To Use update: " + mTimeToUse);
    }

    private void updateTimeToUse(Realm realm) {
      TimeToUse timeToUse = realm.where(TimeToUse.class).findFirst();
      if (timeToUse == null) {
        timeToUse = realm.createObject(TimeToUse.class);
      }
      timeToUse.timeToUse = mTimeToUse;
    }

    @Override
    public void onCancelled(DatabaseError databaseError) {
      FirebaseCrash.report(databaseError.toException());
    }
  };
  private Realm mRealm;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    ButterKnife.bind(this);
    App.inject(this);

    mRealm = Realm.getDefaultInstance();

    String userId = getSharedPrefs().getString("USER_ID", USER_ID_KID);
    mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
    mFirebaseAnalytics.setUserId(userId);

    String token = FirebaseInstanceId.getInstance().getToken();
    logAppStart(token);

    setSupportActionBar(mToolbar);

    //mTimeToUse = getSharedPrefs().getLong(TIME_TO_USE, 15);
    TimeToUse timeToUse = mRealm.where(TimeToUse.class).findFirst();
    mTimeToUse = timeToUse != null ? timeToUse.timeToUse : DEFAULT_TIME_TO_USE;

    //Long minutes = getSharedPrefs().getLong(MINUTES_TOTAL, 0L);
    Minutes minutes = mRealm.where(Minutes.class).findFirst();
    mMinutesVal = minutes != null ? minutes.minutes : 0L;
    mFirebaseDatabase.getReference(MINUTES_TOTAL).setValue(mMinutesVal);
    mTextViewMinutes.setText(String.valueOf(mMinutesVal));

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

      // Log the timestamp to DB
      String timestamp = DATE_FORMAT.format(new Date());
      String msg = String.format(Locale.US, "%sm -> %sm", mMinutesVal, mMinutesVal - mTimeToUse);
      mFirebaseDatabase.getReference("timestamps").child(timestamp).setValue(msg);

      mMinutesVal -= mTimeToUse;
      mMinutesVal = Math.max(mMinutesVal, 0);
      mRefMinutesTotal.setValue(mMinutesVal);
    }
  }

  private void undoTimeUse(AtomicBoolean processing) {
    if (processing.get()) {
      return;
    }

    // Log the timestamp to DB
    String timestamp = DATE_FORMAT.format(new Date());
    String msg = String.format(Locale.US, "Undo: %sm -> %sm", mMinutesVal, mMinutesVal + mTimeToUse);
    mFirebaseDatabase.getReference("timestamps").child(timestamp).setValue(msg);

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

  @Override
  protected void onDestroy() {
    super.onDestroy();
    mRealm.close();
  }
}
