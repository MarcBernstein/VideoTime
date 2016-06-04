package info.marcbernstein.videotime.di.modules;

import com.google.firebase.database.FirebaseDatabase;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import info.marcbernstein.videotime.MainActivity;
import info.marcbernstein.videotime.VideoTimeFirebaseMessagingService;

@Module(
    injects = {
        MainActivity.class,
        VideoTimeFirebaseMessagingService.class,
    }
)
public class VideoTimeModule {

//  @Provides
//  @Singleton
//  public App providesApplication() {
//    return App.getInstance();
//  }

  @Provides
  @Singleton
  public FirebaseDatabase providesFirebaseDatabase() {
    return FirebaseDatabase.getInstance();
  }
}
