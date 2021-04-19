package com.autoscale.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.autoscale.config.AutoScaleConfig;

public class ClientHandler implements Runnable {
    final DataInputStream dis;
    final DataOutputStream dos;
    final int id;
    private volatile boolean running = true;
    int diskUsage;
    int memoryUsage;
    Thread parentThread;
    String ip;

    public ClientHandler(int id, DataInputStream dis, DataOutputStream dos, Thread parentThread, String ip) {
        this.id = id;
        this.dis = dis;
        this.dos = dos;
        this.parentThread = parentThread;
        this.ip = ip;
    }
    public void terminate() throws IOException {
        running = false;
        String[] script = { "sh", AutoScaleConfig.TERMINATE_SCRIPT+" "+ip };
        Runtime.getRuntime().exec(script);
    }
    @Override
    public void run() {
        try {
            while (running) {
                String[] input = dis.readUTF().split(",");
                diskUsage = (int) Math.round(Double.parseDouble(input[0]));
                memoryUsage = (int) Math.round(Double.parseDouble(input[1]));
                System.out.println(input[0]+","+input[1]);
                synchronized (parentThread) {
                    notify();
                }
            }
            this.dis.close();
            this.dos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
