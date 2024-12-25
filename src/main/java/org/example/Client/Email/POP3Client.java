package org.example.Client.Email;

import org.example.Client.Entity.Email;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class POP3Client {
    private String pop3Server;
    private int pop3Port;
    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;

    public POP3Client(String pop3Server, int pop3Port) {
        this.pop3Server = pop3Server;
        this.pop3Port = pop3Port;
    }

    public boolean connect() {
        try {
            socket = new Socket(pop3Server, pop3Port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            // Đọc lời chào
            System.out.println("POP3 Server: " + in.readLine());
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean login(String email, String pass) {
        try {
            out.write("EMAIL " + email + "\r\n");
            out.flush();
            System.out.println("POP3 Server: " + in.readLine());

            out.write("PASS " + pass + "\r\n");
            out.flush();
            String response = in.readLine();
            System.out.println("POP3 Server: " + response);
            return response.startsWith("+OK");
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<String> listEmails() {
        List<String> emails = new ArrayList<>();
        try {
            out.write("LIST\r\n");
            out.flush();
            String response = in.readLine();
            System.out.println("POP3 Server: " + response);
            String line;
            while (!(line = in.readLine()).equals(".")) {
                emails.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return emails;
    }

    public List<String> listEmails(String filter) {
        List<String> emails = new ArrayList<>();
        try {
            out.write("LIST " + filter + "\r\n");
            out.flush();
            String response = in.readLine();
            System.out.println("POP3 Server: " + response);
            String line;
            while (!(line = in.readLine()).equals(".")) {
                emails.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return emails;
    }

    public Email retrieveEmail(int id) {
        try {
            out.write("RETR " + id + "\r\n");
            out.flush();
            String response = in.readLine();
            System.out.println("POP3 Server: " + response);
            
            Map<String, String> headers = new HashMap<>();
            StringBuilder body = new StringBuilder();
            String line;
            boolean isBody = false;
            
            while (!(line = in.readLine()).equals(".")) {
                if (!isBody && line.isEmpty()) {
                    isBody = true;
                    continue;
                }
                
                if (!isBody) {
                    String[] headerParts = line.split(": ", 2);
                    if (headerParts.length == 2) {
                        headers.put(headerParts[0], headerParts[1]);
                    }
                } else {
                    body.append(line).append("\n");
                }
            }
            
            return new Email(
                headers.get("Date"),
                headers.get("Subject"),
                headers.get("From"),
                headers.get("To"),
                body.toString()
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<String> listSentEmails() {
        List<String> emails = new ArrayList<>();
        try {
            out.write("SENT\r\n");
            out.flush();
            String response = in.readLine();
            System.out.println("POP3 Server: " + response);
            String line;
            while (!(line = in.readLine()).equals(".")) {
                emails.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return emails;
    }

    public Email retrieveSentEmail(int id) {
        try {
            out.write("RETR_SENT " + id + "\r\n");
            out.flush();
            String response = in.readLine();
            System.out.println("POP3 Server: " + response);
            
            Map<String, String> headers = new HashMap<>();
            StringBuilder body = new StringBuilder();
            String line;
            boolean isBody = false;
            
            while (!(line = in.readLine()).equals(".")) {
                if (!isBody && line.isEmpty()) {
                    isBody = true;
                    continue;
                }
                
                if (!isBody) {
                    String[] headerParts = line.split(": ", 2);
                    if (headerParts.length == 2) {
                        headers.put(headerParts[0], headerParts[1]);
                    }
                } else {
                    body.append(line).append("\n");
                }
            }
            
            return new Email(
                headers.get("Date"),
                headers.get("Subject"),
                headers.get("From"),
                headers.get("To"),
                body.toString()
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void quit() {
        try {
            out.write("QUIT\r\n");
            out.flush();
            System.out.println("POP3 Server: " + in.readLine());
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
