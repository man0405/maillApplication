package org.example.Server;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.example.DTO.SpamCheckResult;
import org.example.Database.MongoDBConnection;
import org.example.Service.UserService;
import org.example.Server.SpamAssassinClient;


import java.io.*;
import java.net.Socket;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Locale;


public class SMTPHandler implements Runnable {
    private Socket socket;
    private UserService userService;
    private MongoCollection<Document> emailsCollection;
    private SpamAssassinClient SpamAssassinClient ;

    public SMTPHandler(Socket socket) {
        this.socket = socket;
        this.userService = new UserService();
        MongoDatabase database = MongoDBConnection.getDatabase();
        this.emailsCollection = database.getCollection("emails");
        SpamAssassinClient = new SpamAssassinClient("localhost", 783);
    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                BufferedWriter out = new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream()));
        ) {
            out.write("220 Welcome to Java SMTP Server\r\n");
            out.flush();
            String line;
            String from = null;
            String to = null;
            StringBuilder data = new StringBuilder();
            boolean dataMode = false;

            while ((line = in.readLine()) != null) {
                System.out.println("SMTP Received: " + line);
                if (dataMode) {
                    if (line.equals(".")) {
                        // Lưu trữ email vào MongoDB
                        String subject = extractSubject(data.toString());
                        String body = extractBody(data.toString());

                        // Lấy danh sách người nhận
                        String[] recipients = to.split(",");
                        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);

                        String emailCotent = "Date: " + dateFormat.format(new java.util.Date()) + "\n"
                                + "Message-ID: <" + from + ">\n" +
                                "Received: from " + from + " ( " + socket.getInetAddress().getHostAddress() + ")\n"
                                + "    for " + to + "; " + dateFormat.format(new java.util.Date()) + "\n"
                                + "Subject: " + subject + "\n"
                                + "From: " + from + "\n"
                                + "To: " + to + "\n"
                                + "\n"
                                + body;

                        SpamCheckResult result = SpamAssassinClient.checkSpam(emailCotent);

                        Document email = new Document("from", from)
                                .append("to", Arrays.asList(recipients))
                                .append("subject", subject)
                                .append("body", body)
                                .append("timestamp", new java.util.Date())
                                .append("isRead", false)
                                .append("isSpam", result.isSpam())
                                .append("spamScore", result.getScore());


                        emailsCollection.insertOne(email);
                        out.write("250 Message accepted for delivery\r\n");
                        dataMode = false;
                        data.setLength(0);
                    } else {
                        data.append(line).append("\n");
                    }
                } else {
                    if (line.startsWith("HELO") || line.startsWith("EHLO")) {
                        out.write("250 Hello\r\n");
                    } else if (line.startsWith("MAIL FROM")) {
                        from = extractEmail(line);
                        // Kiểm tra người gửi tồn tại
                        if (userService.getUserEmail(from) != null) {
                            out.write("250 OK\r\n");
                        } else {
                            out.write("550 No such user here\r\n");
                        }
                    } else if (line.startsWith("RCPT TO")) {
                        to = extractEmail(line);
                        // Kiểm tra người nhận tồn tại
                        if (userService.getUserEmail(to) != null) {
                            out.write("250 OK\r\n");
                        } else {
                            out.write("550 No such user here\r\n");
                        }
                    } else if (line.startsWith("DATA")) {
                        out.write("354 End data with <CR><LF>.<CR><LF>\r\n");
                        dataMode = true;
                    } else if (line.startsWith("QUIT")) {
                        out.write("221 Bye\r\n");
                        break;
                    } else {
                        out.write("502 Command not implemented\r\n");
                    }
                }
                out.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String extractEmail(String line) {
        // Giả sử định dạng là MAIL FROM:<email@example.com>
        int start = line.indexOf("<") + 1;
        int end = line.indexOf(">");
        return line.substring(start, end).trim();
    }

    private String extractSubject(String data) {
        // Tìm dòng bắt đầu bằng "Subject:"
        BufferedReader reader = new BufferedReader(new StringReader(data));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Subject:")) {
                    return line.substring(8).trim();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "(No Subject)";
    }

    private String extractBody(String data) {
        // Tìm phần sau dòng trống (dấu phân cách giữa header và body)
        int index = data.indexOf("\n\n");
        if (index != -1) {
            return data.substring(index + 2).trim();
        }
        return data.trim();
    }
}


