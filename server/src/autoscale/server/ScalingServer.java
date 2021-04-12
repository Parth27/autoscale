package autoscale.server;

import java.util.HashMap;

import java.net.Socket;
import java.net.ServerSocket;

import java.io.IOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;

public class ScalingServer {
    static HashMap<Integer, ClientHandler> map = new HashMap<>();
    static HashMap<Integer, Thread> threadMap = new HashMap<>();
    static final int PORT = 1234;
    // counter for clients
    static int id = 0;
    public static void main(String[] args) throws IOException {
        try (ServerSocket server = new ServerSocket(PORT)) {
            Socket socket;
            System.out.print("Started server");
            while (id < 100) {
                socket = server.accept();
                System.out.print("New client received: "+socket);
                
                DataInputStream dis = new DataInputStream(socket.getInputStream());
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                ClientHandler client = new ClientHandler(id,dis,dos);
                Thread t = new Thread(client);

                System.out.println("Client id = "+client.id);
                map.put(id, client);
                t.start();
                id++;
            }
        }
        for (int key:map.keySet()) {
            terminateClient(key);
        }
    }

    public static void terminateClient(int id) {
        ClientHandler client = map.get(id);
        Thread t = threadMap.get(id);
        client.terminate();
        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        map.remove(id);
        threadMap.remove(id);
    }
}