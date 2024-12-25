package org.example.Server;

import org.example.Database.MongoDBConnection;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


public class POP3Server {
    private static final int PORT = 1100;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("POP3 Server is running on port " + PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new POP3Handler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            MongoDBConnection.closeConnection();
        }
    }
}
