package org.bbop.phylo.tracking;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by suzi on 1/30/15.
 */
public class LogUtil {
    public static String dateNow() {
        long timestamp = System.currentTimeMillis();
		/* Date appears to be fixed?? */
        Date when = new Date(timestamp);
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy");
        sdf.setTimeZone(TimeZone.getDefault()); // local time
        return sdf.format(when);
    }
}
