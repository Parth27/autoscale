package com.autoscale.application.kafka;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import com.autoscale.core.MetaServer;

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
    }

    @Override
    public void rebalance(List<String> serverList) {
        System.out.println("Rebalancing topic partitions, pause producers");
        StringBuilder serverString = new StringBuilder();
        for (int i=0; i < serverList.size(); i++) {
            serverString.append(i);
        }
        rebalancePartitions(serverString.toString());
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
            System.out.println("Topic "+topic+" created...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateTopic(String topic, int replication, int partitions) {
        String[] script = { "sh", KafkaConfig.TOPIC_UPDATE_SCRIPT, KafkaConfig.ZOOKEEPER_IP,
                String.valueOf(replication), String.valueOf(partitions) };
        try {
            Process p = new ProcessBuilder(script).start();
            p.waitFor();
            System.out.println("Topic "+topic+" updated...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void pingProducer(List<String> serverList) throws IOException {
        // Interrupt the producer to update its server list
        String[] producerList = KafkaConfig.PRODUCER_LIST.split(",");
        String[] address;
        for (int i=0; i < producerList.length; i++) {
            address = producerList[i].split(":");
            InetAddress producerIP = InetAddress.getByName(address[0]);
            try (Socket producerSocket = new Socket(producerIP, Integer.parseInt(address[1]))) {
                DataOutputStream dos = new DataOutputStream(producerSocket.getOutputStream());
                dos.writeUTF(String.join(",", serverList));
            }
        }
    }

    private void rebalancePartitions(String brokers) {
        // Function to reassign partitions after updating server list
        String[] script = { "sh", KafkaConfig.REBALANCE_SCRIPT, KafkaConfig.ZOOKEEPER_IP, brokers};
        try {
            Process p = new ProcessBuilder(script).start();
            p.waitFor();
            System.out.println("Partitions rebalanced...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
