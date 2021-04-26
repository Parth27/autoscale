package com.autoscale.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import com.autoscale.config.MarkovChainConfig;
import com.autoscale.core.ApplicationServer;

public class ClientHandler implements Runnable {
    DataInputStream dis;
    DataOutputStream dos;
    final int id;
    private volatile boolean running = true;
    int diskUsage;
    int memoryUsage;
    AutoScaleServer parent;
    String ip;
    String instanceID;
    Socket socket;
    ServerSocket autoscaleServer;
    ApplicationServer applicationServer;

    public ClientHandler(int id, AutoScaleServer parent, String ip, String instanceID, ServerSocket server, ApplicationServer applicationServer) {
        this.id = id;
        this.parent = parent;
        this.ip = ip;
        this.instanceID = instanceID;
        autoscaleServer = server;
        this.applicationServer = applicationServer;
    }

    @Override
    public void run() {
        int offset = 100 / MarkovChainConfig.NUM_BINS;
        try {
            initialize();
            while (running) {
                String[] input = dis.readUTF().split(",");
                diskUsage = Math.round(Math.round(Double.parseDouble(input[0])) / offset);
                memoryUsage = Math.round(Math.round(Double.parseDouble(input[1])) / offset);
                if (!applicationServer.isStarted) {
                    applicationServer.isStarted = true;
                    System.out.println("Kafka broker: "+instanceID+" started");
                }
                System.out.println(input[0] + "," + input[1]);
                if (parent.clientMonitor.containsKey(id)) {
                    parent.clientMonitor.remove(id);
                }
            }
            this.dis.close();
            this.dos.close();
        } catch (IOException e) {
            // Do nothing
        }
    }

    private void initialize() throws IOException {
        socket = autoscaleServer.accept();
        dis = new DataInputStream(socket.getInputStream());
        dos = new DataOutputStream(socket.getOutputStream());
    }
}
