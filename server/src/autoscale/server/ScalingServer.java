package autoscale.server;

import java.net.Socket;
import java.net.ServerSocket;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.HashMap;

public class ScalingServer {
    static HashMap<Integer, ClientHandler> map = new HashMap<>();
    static HashMap<Integer, Thread> threadMap = new HashMap<>();
    // counter for clients
    static int id = 0;
    public static void main(String[] args) {
        ServerSocket server;
        Socket socket;
        try {
            server = new ServerSocket(1234);
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
            for (int key:map.keySet()) {
                terminateClient(key);
            }
        } catch(Exception e) {
            System.out.println("Connection issue");
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