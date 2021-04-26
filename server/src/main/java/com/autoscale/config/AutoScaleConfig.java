package com.autoscale.config;

public class AutoScaleConfig {
    public static final int PORT = 1234;
    public static final String INSTANCE_START = "/home/parth/autoscale/ec2-instance-start.sh";
    public static final String MODEL_FILE = "/home/parth/autoscale/server/probabilities.txt";
    public static final int STARTUP_SERVERS = 3;
    public static final int UPPER_LIMIT = 70; // Upper limit of percent resource usage
    public static final int LOWER_LIMIT = 30; // Lower limit of percent resource usage
    public static final long INTERVAL_MINS = 2L;
}
