package com.autoscale.application.kafka;

import java.io.IOException;

import com.autoscale.core.ApplicationServer;

public class KafkaBroker implements ApplicationServer {
    String ip;
    String cloudInstance;
    int id;

    public KafkaBroker(String ip, String cloudInstance, int id) {
        this.ip = ip;
        this.cloudInstance = cloudInstance;
        this.id = id;
    }

    @Override
    public void initialize() {
        String[] script;
        script = new String[] { "sh", KafkaConfig.SERVER_START, ip, String.valueOf(id) };
        try {
            Process p = new ProcessBuilder(script).start();
            p.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void terminate() {
        String[] script = { "sh", KafkaConfig.SERVER_TERMINATE, ip, cloudInstance, KafkaConfig.ZOOKEEPER_IP };
        try {
            new ProcessBuilder(script).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
