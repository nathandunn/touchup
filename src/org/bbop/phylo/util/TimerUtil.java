package org.bbop.phylo.util;

import java.util.concurrent.TimeUnit;

public class TimerUtil {
	
	private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(LoginUtil.class);
	private static long startTime;
	
	public TimerUtil() {		
		startTime = System.nanoTime();
	}
	
	public long startTimer() {
		return startTime;
	}
	
	public String reportStartTime() {
		return reportTime(startTime);	
	}
	
	public String reportElapsedTime() {
		long difference = System.nanoTime() - startTime;
		return reportTime(difference);
	}
	
    private String reportTime(long time) {
    	long min = TimeUnit.NANOSECONDS.toMinutes(time);
    	long sec = TimeUnit.NANOSECONDS.toSeconds(time);
		String report = String.format("%d min, %d sec", min, (sec - (min * 60)));
		return report;
	}
}
