package org.example.Server;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.example.Database.MongoDBConnection;
import org.example.Service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Projections.*;

public class POP3Handler implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(POP3Handler.class);
    private final Socket socket;
    private final UserService userService;
    private final MongoCollection<Document> emailsCollection;
    private String currentUserEmail;

    public POP3Handler(Socket socket) {
        this.socket = socket;
        this.userService = new UserService();
        MongoDatabase database = MongoDBConnection.getDatabase();
        this.emailsCollection = database.getCollection("emails");
    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                BufferedWriter out = new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream()))
        ) {
            out.write("+OK POP3 server ready\r\n");
            out.flush();
            String line;
            boolean authenticated = false;

            while ((line = in.readLine()) != null) {
                System.out.println("POP3 Received: " + line);
                String[] parts = line.split(" ");
                String command = parts[0].toUpperCase();

                switch (command) {
                    case "EMAIL":
                        String username = parts[1];
                        String email = userService.getUserEmail(username);
                        if (email != null) {
                            currentUserEmail = email;
                            out.write("+OK User accepted\r\n");
                        } else {
                            out.write("-ERR No such user\r\n");
                        }
                        break;
                    case "PASS":
                        String password = parts[1];
                        if (userService.authenticateUser(currentUserEmail, password)) {
                            authenticated = true;
                            out.write("+OK Password accepted\r\n");
                        } else {
                            out.write("-ERR Invalid password\r\n");
                        }
                        break;
                    case "LIST":
                        if (authenticated) {
                            boolean filterSpam = parts.length > 1 && parts[1].equalsIgnoreCase("SPAM");
                            List<Document> emails = emailsCollection.find(
                                            and(eq("to", currentUserEmail), eq("isSpam", filterSpam))
                                    )
                                    .projection(fields(include("to", "isSpam", "body"), excludeId()))
                                    .into(new ArrayList<>());

                            System.out.println("POP3 List emails: " + emails.size() + " (Spam: " + filterSpam + ")" + " for " + currentUserEmail);
                            out.write("+OK " + emails.size() + " messages\r\n");
                            if (emails.size() > 0) {
                                int id = 1;
                                for (Document emailDoc : emails) {
                                    String body = emailDoc.getString("body");
                                    int bodyLength = body != null ? body.length() : 0;
                                    out.write(id + " " + bodyLength + "\r\n");
                                    id++;
                                }
                            }
                            out.write(".\r\n");
                            out.flush();
                        } else {
                            out.write("-ERR Not authenticated\r\n");
                        }
                        break;
                    case "RETR":
                        if (authenticated) {
                            int emailId = Integer.parseInt(parts[1]);
                            List<Document> emails = emailsCollection.find(eq("to", currentUserEmail)).into(new ArrayList<>());

                            if (emailId > 0 && emailId <= emails.size()) {
                                Document emailDoc = emails.get(emailId - 1);
                                String body = emailDoc.getString("body");
                                if (body == null) body = "";

                                SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
                                String formattedDate = dateFormat.format(emailDoc.getDate("timestamp"));

                                out.write("+OK " + body.length() + " octets\r\n");
                                out.write("From: " + emailDoc.getString("from") + "\r\n");

                                // Fixed recipient handling
                                ArrayList<String> recipients = emailDoc.get("to", ArrayList.class);
                                String toField = recipients != null ? String.join(", ", recipients) : currentUserEmail;
                                out.write("To: " + toField + "\r\n");

                                out.write("Subject: " + emailDoc.getString("subject") + "\r\n");
                                out.write("Date: " + formattedDate + "\r\n");
                                out.write("\r\n");
                                out.write(body + "\r\n");
                                out.write(".\r\n");
                                // Đánh dấu email đã đọc
                                emailsCollection.updateOne(eq("_id", emailDoc.getObjectId("_id")), new Document("$set", new Document("isRead", true)));
                                System.out.println("POP3 Retrieved email ID: " + emailId);
                            } else {
                                out.write("-ERR No such message\r\n");
                                System.out.println("POP3 Error: No such message ID " + emailId);
                            }
                        } else {
                            out.write("-ERR Not authenticated\r\n");
                        }
                        break;
                    case "QUIT":
                        out.write("+OK Bye\r\n");
                        out.flush();
                        return;
                    case "SENT":
                        if (authenticated) {
                            List<Document> sentEmails = emailsCollection.find(eq("from", currentUserEmail)).into(new ArrayList<>());
                            out.write("+OK " + sentEmails.size() + " messages\r\n");
                            int id = 1;
                            for (Document emailDoc : sentEmails) {
                                out.write(id + " " + emailDoc.getString("body").length() + "\r\n");
                                id++;
                            }
                            out.write(".\r\n");
                        } else {
                            out.write("-ERR Not authenticated\r\n");
                        }
                        break;

                    case "RETR_SENT":
                        if (authenticated) {
                            int emailId = Integer.parseInt(parts[1]);
                            List<Document> sentEmails = emailsCollection.find(eq("from", currentUserEmail)).into(new ArrayList<>());
                            if (emailId > 0 && emailId <= sentEmails.size()) {
                                Document emailDoc = sentEmails.get(emailId - 1);
                                String body = emailDoc.getString("body");
                                if (body == null) body = "";

                                SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
                                String formattedDate = dateFormat.format(emailDoc.getDate("timestamp"));

                                out.write("+OK " + body.length() + " octets\r\n");
                                out.write("From: " + emailDoc.getString("from") + "\r\n");
                                ArrayList<String> recipients = emailDoc.get("to", ArrayList.class);
                                String toField = recipients != null ? String.join(", ", recipients) : "";
                                out.write("To: " + toField + "\r\n");
                                out.write("Subject: " + emailDoc.getString("subject") + "\r\n");
                                out.write("Date: " + formattedDate + "\r\n");
                                out.write("\r\n");
                                out.write(body + "\r\n");
                                out.write(".\r\n");
                            } else {
                                out.write("-ERR No such message\r\n");
                            }
                        } else {
                            out.write("-ERR Not authenticated\r\n");
                        }
                        break;
                    default:
                        out.write("-ERR Unknown command\r\n");
                }
                out.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

