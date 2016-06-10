package info.marcbernstein.videotime.services;

import android.util.Log;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

public class VideoTimeFirebaseInstanceIDService extends FirebaseInstanceIdService {

  private static final String TAG = VideoTimeFirebaseInstanceIDService.class.getSimpleName();

  @Override
  public void onTokenRefresh() {
    // Get updated InstanceID token.
    String refreshedToken = FirebaseInstanceId.getInstance().getToken();
    Log.d(TAG, "Refreshed token: " + refreshedToken);

    FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
    firebaseDatabase.getReference("current_app_token").setValue(refreshedToken != null ? refreshedToken : "Null Token");
  }
}
