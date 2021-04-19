package com.autoscale.config;

public class AutoScaleConfig {
    public static final int PORT = 1234;
    public static final int PRODUCER_PORT = 4567;
    public static final String PRODUCER_IP = "localhost";
    public static final String STARTUP_SCRIPT = "/home/parth/autoscale/ec2-kafka-start.sh";
    public static final String TERMINATE_SCRIPT = "/home/parth/autoscale/ec2-kafka-stop.sh";
    public static final String MODEL_FILE = "/home/parth/autoscale/server/probabilities.txt";
    public static final int STARTUP_SERVERS = 3;
    public static final int UPPER_LIMIT = 6;
    public static final int LOWER_LIMIT = 2;
    public static final long INTERVAL_MINS = 2L;
}
