package com.autoscale.server;

import java.io.IOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;

public class ClientHandler implements Runnable {
    private String name;
    final DataInputStream dis;
    final DataOutputStream dos;
    final int id;
    private volatile boolean running = true;

    public ClientHandler(int id, DataInputStream dis, DataOutputStream dos) {
        this.id = id;
        this.dis = dis;
        this.dos = dos;
    }
    public void terminate() {
        running = false;
    }
    @Override
    public void run() {
        try {
            while (running) {
                String input = dis.readUTF();
                System.out.println(input);                
            }
            this.dis.close();
            this.dos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
