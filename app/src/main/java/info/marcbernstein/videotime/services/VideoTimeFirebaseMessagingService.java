package info.marcbernstein.videotime.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Locale;
import java.util.Map;
import java.util.Random;

import javax.inject.Inject;

import info.marcbernstein.videotime.App;
import info.marcbernstein.videotime.MainActivity;
import info.marcbernstein.videotime.R;
import info.marcbernstein.videotime.utils.DateUtils;

import static info.marcbernstein.videotime.MainActivity.MINUTES_TOTAL;

public class VideoTimeFirebaseMessagingService extends FirebaseMessagingService {
  private static final String TAG = VideoTimeFirebaseMessagingService.class.getSimpleName();
  private static final String TIMESTAMPS_RECEIVED = "timestamps_received";

  @Inject
  FirebaseDatabase mFirebaseDatabase;

  @Override
  public void onCreate() {
    super.onCreate();
    App.inject(this);
  }

  @Override
  public void onMessageReceived(RemoteMessage remoteMessage) {
    // If the application is in the foreground handle both data and notification messages here.
    // Also if you intend on generating your own notifications as a result of a received FCM
    // message, here is where that should be initiated. See sendNotification method below.
    Log.d(TAG, "From: " + remoteMessage.getFrom());
    Map<String, String> data = remoteMessage.getData();
    Log.d(TAG, "Data: " + data);

    sendNotification(data);
  }
  // [END receive_message]

  /**
   * Create and show a simple notification containing the received FCM message.
   */
  private synchronized void sendNotification(Map<String, String> data) {
    final Integer minutes = Integer.valueOf(data.get("minutes"));
    String reasonData = data.get("reason");
    final String from = data.get("from_who");
    if (!(reasonData.endsWith(".") || reasonData.endsWith("!") || reasonData.endsWith("..."))) {
      reasonData += '.';
    }
    final String reason = reasonData;

    String msg = String.format(Locale.US, "You got %d minutes from %s for %s", minutes, from, reason);

    final DatabaseReference ref = mFirebaseDatabase.getReference(MINUTES_TOTAL);
    ref.addListenerForSingleValueEvent(new ValueEventListener() {
      @Override
      public void onDataChange(DataSnapshot dataSnapshot) {
        Integer currentValue = dataSnapshot.getValue(Integer.class);
        if (currentValue == null) {
          currentValue = 0;
        }
        currentValue += minutes;

        ref.setValue(currentValue);

        // Log the timestamp to DB
        String receivedMsg = String.format(Locale.US, "%sm -> %sm, Msg: %d, %s, %s", currentValue - minutes, currentValue, minutes, reason, from);
        mFirebaseDatabase.getReference(TIMESTAMPS_RECEIVED).child(DateUtils.getTimestamp()).setValue(receivedMsg);
      }

      @Override
      public void onCancelled(DatabaseError databaseError) {
        Log.e(TAG, databaseError.getMessage(), databaseError.toException());
        FirebaseCrash.report(databaseError.toException());
      }
    });

    Intent intent = new Intent(this, MainActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
        PendingIntent.FLAG_UPDATE_CURRENT);

    NotificationCompat.BigPictureStyle style = new
        NotificationCompat.BigPictureStyle();
    style.setBigContentTitle(getString(R.string.new_message));
    style.setSummaryText(msg);

    Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
    NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
        .setSmallIcon(R.drawable.ic_plus_box_white_48dp)
        .setContentTitle(getString(R.string.new_message))
        .setContentText(msg)
        .setAutoCancel(true)
        .setSound(defaultSoundUri)
        .setStyle(new NotificationCompat.BigTextStyle()
            .bigText(msg))
        .setContentIntent(pendingIntent);

    NotificationManager notificationManager =
        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

    notificationManager.notify(new Random().nextInt(), notificationBuilder.build());
  }
}
