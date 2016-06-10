package info.marcbernstein.videotime.utils;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateUtils {
  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM d HH:mm:ss:SS", Locale.US);

  public static String getTimestamp() {
    return DATE_FORMAT.format(new Date());
  }
}
