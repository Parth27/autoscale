package autoscale.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

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
        while (running) {
            try {
                Thread.sleep((long) 15000);
            } catch (InterruptedException e) {
                running = false;
            }
        }
        try {
            this.dis.close();
            this.dos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
