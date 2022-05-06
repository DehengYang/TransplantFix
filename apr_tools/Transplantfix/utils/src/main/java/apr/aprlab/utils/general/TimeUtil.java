package apr.aprlab.utils.general;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeUtil {

    public static void printTime() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println("Current time: " + df.format(new Date()));
    }

    public static String getReadableTime() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return df.format(new Date());
    }

    public static long getTime() {
        return System.currentTimeMillis();
    }

    public static String countTime(long startTime) {
        DecimalFormat dF = new DecimalFormat("0.0000");
        return dF.format((float) (System.currentTimeMillis() - startTime) / 1000);
    }

    public static String countTime(String message, long startTime) {
        return String.format("%s: %s", message, countTime(startTime));
    }

    public static void printCountTime(String message, long startTime) {
        System.out.format("%s: %s\n", message, countTime(startTime));
    }
}
