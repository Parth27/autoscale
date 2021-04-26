package com.autoscale.application.kafka;

public class KafkaConfig {
    public static final String TOPIC_NAME = "Messages";
    public static final String TOPIC_CREATE_SCRIPT = "/home/parth/autoscale/ec2-kafka-topic-create.sh";
    public static final String TOPIC_UPDATE_SCRIPT = "/home/parth/autoscale/ec2-kafka-topic-update.sh";
    public static final String REBALANCE_SCRIPT = "/home/parth/autoscale/ec2-kafka-rebalance.sh";
    public static final String SERVER_START = "/home/parth/autoscale/ec2-kafka-start.sh";
    public static final String SERVER_TERMINATE = "/home/parth/autoscale/ec2-kafka-stop.sh";
    public static final String ZOOKEEPER_IP = "52.15.62.97";
    public static final int PRODUCER_PORT = 4567;
    public static final String PRODUCER_IP = "localhost";
    public static final int NUM_PARTITIONS = 6;
}
