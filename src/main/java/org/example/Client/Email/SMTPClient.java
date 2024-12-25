package org.example.Client.Email;

import java.io.*;
import java.net.Socket;


public class SMTPClient {
    private String smtpServer;
    private int smtpPort;

    public SMTPClient(String smtpServer, int smtpPort) {
        this.smtpServer = smtpServer;
        this.smtpPort = smtpPort;
    }

    public boolean sendEmail(String from, String to, String subject, String body) {
        try (Socket socket = new Socket(smtpServer, smtpPort);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream()));
             BufferedWriter out = new BufferedWriter(
                     new OutputStreamWriter(socket.getOutputStream()))) {

            // Đọc lời chào từ server
            System.out.println("SMTP Server: " + in.readLine());

            // Gửi HELO
            out.write("HELO localhost\r\n");
            out.flush();
            System.out.println("SMTP Server: " + in.readLine());

            // Gửi MAIL FROM
            out.write("MAIL FROM:<" + from + ">\r\n");
            out.flush();
            System.out.println("SMTP Server: " + in.readLine());

            // Gửi RCPT TO
            out.write("RCPT TO:<" + to + ">\r\n");
            out.flush();
            System.out.println("SMTP Server: " + in.readLine());

            // Gửi DATA
            out.write("DATA\r\n");
            out.flush();
            System.out.println("SMTP Server: " + in.readLine());

            // Gửi nội dung email
            out.write("Subject: " + subject + "\r\n");
            out.write("From: " + from + "\r\n");
            out.write("To: " + to + "\r\n");
            out.write("\r\n"); // Ngắt giữa tiêu đề và thân email
            out.write(body + "\r\n.\r\n");
            out.flush();
            System.out.println("SMTP Server: " + in.readLine());

            // Gửi QUIT
            out.write("QUIT\r\n");
            out.flush();
            System.out.println("SMTP Server: " + in.readLine());

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
