package org.example.Server;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;
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
                                    .into(new ArrayList<>());

                            System.out.println("POP3 List emails found: " + emails.size() + " emails for " + currentUserEmail);
                            out.write("+OK " + emails.size() + " messages\r\n");
                            if (emails.size() > 0) {
                                for (Document emailDoc : emails) {
                                    String body = emailDoc.getString("body");
                                    ObjectId idObj = emailDoc.getObjectId("_id");
                                    String id = idObj != null ? idObj.toHexString() : "";

                                    int bodyLength = body != null ? body.length() : 0;
                                    out.write(id + " " + bodyLength + "\r\n");
                                }
                            }
                            out.write(".\r\n");
                        } else {
                            out.write("-ERR Not authenticated\r\n");
                        }
                        break;
                    case "RETR":
                        if (authenticated) {
                            String objectIdStr = parts[1];
                            Document emailDoc = emailsCollection.find(eq("_id", new ObjectId(objectIdStr))).first();
                            if (emailDoc != null) {
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
                                emailsCollection.updateOne(eq("_id", emailDoc.getObjectId("_id")),
                                                           new Document("$set", new Document("isRead", true)));
                            } else {
                                out.write("-ERR No such message\r\n");
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
                            for (Document emailDoc : sentEmails) {
                                ObjectId idObj = emailDoc.getObjectId("_id");
                                String id = idObj != null ? idObj.toHexString() : "";
                                out.write(id + " " + emailDoc.getString("body").length() + "\r\n");
                            }
                            out.write(".\r\n");
                        } else {
                            out.write("-ERR Not authenticated\r\n");
                        }
                        break;
                    case "RETR_SENT":
                        if (authenticated){
                            String objectIdStr = parts[1];
                            Document emailDoc = emailsCollection.find(eq("_id", new ObjectId(objectIdStr))).first();
                            if (emailDoc != null) {
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
                    case "DEL":
                        if (authenticated) {
                            String objectIdStr = parts[1];
                            Document emailDoc = emailsCollection.find(eq("_id", new ObjectId(objectIdStr))).first();
                            if (emailDoc != null) {
                                emailsCollection.deleteOne(eq("_id", emailDoc.getObjectId("_id")));
                                out.write("+OK\r\n");
                            } else {
                                out.write("+FALSE\r\n");
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

