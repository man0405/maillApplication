package org.example.Server;

import org.example.Database.MongoDBConnection;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


public class SMTPServer {
    private static final int PORT = 2525;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("SMTP Server is running on port " + PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new SMTPHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            MongoDBConnection.closeConnection();
        }
    }
}
