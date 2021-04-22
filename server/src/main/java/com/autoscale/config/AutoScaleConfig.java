package com.autoscale.config;

public class AutoScaleConfig {
    public static final int PORT = 1234;
    public static final int PRODUCER_PORT = 4567;
    public static final String PRODUCER_IP = "localhost";
    public static final String SERVER_START = "/home/parth/autoscale/ec2-kafka-start.sh";
    public static final String SERVER_TERMINATE = "/home/parth/autoscale/ec2-kafka-stop.sh";
    public static final String INSTANCE_START = "/home/parth/autoscale/ec2-instance-start.sh";
    public static final String TOPIC_SCRIPT = "/home/parth/autoscale/ec2-kafka-topics.sh";
    public static final String MODEL_FILE = "/home/parth/autoscale/server/probabilities.txt";
    public static final int STARTUP_SERVERS = 2;
    public static final int UPPER_LIMIT = 6;
    public static final int LOWER_LIMIT = 2;
    public static final long INTERVAL_MINS = 2L;
    public static final String ZOOKEEPER_IP = "13.59.219.38";
}
