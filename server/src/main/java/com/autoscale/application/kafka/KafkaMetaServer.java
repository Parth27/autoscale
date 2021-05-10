package com.autoscale.application.kafka;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import com.autoscale.core.MetaServer;

import kafka.admin.ReassignPartitionsCommand;

public class KafkaMetaServer implements MetaServer {
    // This is a meta server required by this application for performing
    // functionalities other than initialization and termination
    // This class is meant to perform tasks for all servers in the cluster
    List<String> topicList;

    public KafkaMetaServer() {
        topicList = new ArrayList<>();
    }

    @Override
    public void start(List<String> serverList) {
        String topic = KafkaConfig.TOPIC_NAME;
        if (!topicList.contains(topic)) {
            createTopic(topic);
        }
        System.out.println("Topic created, start producers");
    }

    @Override
    public void rebalance(List<String> serverList) {
        System.out.println("Rebalancing topic partitions, pause producers");
        // Sleep for some time to give time to interrupt producers
        try {
            Thread.sleep(20 * 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        StringBuilder serverString = new StringBuilder();
        for (int i = 0; i < serverList.size(); i++) {
            serverString.append(","+i);
        }
        rebalancePartitions(serverString.toString().substring(1));
    }

    private void createTopic(String topic) {
        String[] script = { "sh", KafkaConfig.TOPIC_CREATE_SCRIPT, KafkaConfig.ZOOKEEPER_IP, topic };
        try {
            Process p = new ProcessBuilder(script).start();
            BufferedReader processInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = processInput.readLine()) != null) {
                System.out.println(line);
            }
            p.waitFor();
            topicList.add(topic);
            System.out.println("Topic " + topic + " created...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void rebalancePartitions(String brokers) {
        // Function to reassign partitions after updating server list
        System.out.println("New brokers: "+brokers);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        PrintStream systemPStream = System.out;
        String[] script = { "sh", KafkaConfig.REBALANCE_SCRIPT, KafkaConfig.ZOOKEEPER_IP, brokers };
        try {
            Process p = new ProcessBuilder(script).start();
            p.waitFor();
            ReassignPartitionsCommand.main(new String[] { "--zookeeper", KafkaConfig.ZOOKEEPER_IP + ":2181",
                    "--reassignment-json-file", KafkaConfig.REBALANCE_JSON, "--execute" });
            Thread.sleep(5 * 1000);
            System.setOut(ps);
            String result = "pending";
            while (result.contains("pending")) {
                ReassignPartitionsCommand.main(new String[] { "--zookeeper", KafkaConfig.ZOOKEEPER_IP + ":2181",
                        "--reassignment-json-file", KafkaConfig.REBALANCE_JSON, "--verify" });
                result = baos.toString();
            }
            System.out.flush();
            System.setOut(systemPStream);
            System.out.println(result);
            System.out.println("Partitions rebalanced...");
        } catch (Exception e) {
            // Do Nothing
        }
    }
}
