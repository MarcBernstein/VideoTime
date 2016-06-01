package info.marcbernstein.videotime.di.modules;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import info.marcbernstein.videotime.App;
import info.marcbernstein.videotime.MainActivityFragment;

@Module(
    injects = {
        MainActivityFragment.class,
    }
)
public class VideoTimeModule {

  @Provides
  @Singleton
  public App providesApplication() {
    return App.getInstance();
  }
}
