package info.marcbernstein.videotime.events;

public class TimeEvent {
  public final String message;
  public final int minutes;

  public TimeEvent(String message, int minutes) {
    this.message = message;
    this.minutes = minutes;
  }
}
