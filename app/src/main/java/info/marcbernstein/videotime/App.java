package info.marcbernstein.videotime;

import android.app.Application;

import com.squareup.leakcanary.LeakCanary;

import org.greenrobot.eventbus.EventBus;

import dagger.ObjectGraph;
import info.marcbernstein.videotime.di.modules.VideoTimeModule;

public class App extends Application {

  private static App sInstance;

  private ObjectGraph mObjectGraph;
  private ObjectGraph mTestGraph;

  @Override
  public void onCreate() {
    super.onCreate();

    sInstance = this;

    initLibs();
    setupObjectGraph();
  }

  public static App getInstance() {
    return sInstance;
  }

  private void initLibs() {
    LeakCanary.install(this);
    EventBus.builder()
        .sendNoSubscriberEvent(false)
        .throwSubscriberException(true)
        .installDefaultEventBus();
  }

  //region Dependency Injection
  private void setupObjectGraph() {
    mObjectGraph = ObjectGraph.create(getModules());
  }

  private Object[] getModules() {
    return new Object[]{new VideoTimeModule()};
  }

  public static void inject(Object object) {
    if (getInstance().mTestGraph != null) {
      getInstance().mTestGraph.inject(object);
      return;
    }

    getInstance().mObjectGraph.inject(object);
  }

  public void setTestGraph(ObjectGraph graph) {
    mTestGraph = graph;
  }
  //endregion
}
