package dev.hartcode.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    // List to hold ObjectOutputStream instances for connected clients
    private final List<ObjectOutputStream> clientOutputStreams = new ArrayList<>();

    public static void main(String[] args) {
        new Server().start();
    }

    private void start() {
        try {
            ServerSocket serverSocket = new ServerSocket(6000);
            ExecutorService executorService = Executors.newCachedThreadPool();

            while (!serverSocket.isClosed()) {
                System.out.println("The server is up -> Waiting for client connection...");
                Socket clientSocket = serverSocket.accept();
                ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                clientOutputStreams.add(out);

                // Create a new ClientHandler for the connected client
                ClientHandler client = new ClientHandler(clientSocket);
                executorService.execute(client);
                System.out.println("Connection established with a client...");
            }
        } catch (IOException e) {
            System.out.println("Server could not start up -> " + e.getMessage());
        }
    }

    // Send messages to all connected clients
    private void tellEveryone(Object one, Object two) {
        for (ObjectOutputStream out : clientOutputStreams) {
            try {
                out.writeObject(one);
                out.writeObject(two);
            } catch (IOException e) {
                System.out.println("The server could not send the message to the clients -> " + e.getMessage());
            }
        }
    }

    // Inner class to handle each connected client
    public class ClientHandler implements Runnable {
        private ObjectInputStream in;

        public ClientHandler(Socket socket) {
            try {
                in = new ObjectInputStream(socket.getInputStream());
            } catch (IOException e) {
                System.out.println("Error while initializing client handler: " + e.getMessage());
            }
        }

        @Override
        public void run() {
            Object userName;
            Object beatSequence;
            try {
                while ((userName = in.readObject()) != null) {
                    beatSequence = in.readObject();
                    tellEveryone(userName, beatSequence);
                }
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Error in client handler: " + e.getMessage());
            }
        }
    }
}
