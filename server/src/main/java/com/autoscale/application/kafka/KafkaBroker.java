package com.autoscale.application.kafka;

import java.io.IOException;

import com.autoscale.core.ApplicationServer;

public class KafkaBroker extends ApplicationServer {
    String ip;
    String cloudInstance;

    public KafkaBroker(String ip, String cloudInstance) {
        this.ip = ip;
        this.cloudInstance = cloudInstance;
    }

    @Override
    public void initialize() {
        String[] script;
        script = new String[] { "sh", KafkaConfig.SERVER_START, ip };
        try {
            System.out.println(ip);
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
