package org.example.Server;
import org.example.DTO.SpamCheckResult;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class SpamAssassinClient {

    private String host;
    private int port;

    public SpamAssassinClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public SpamCheckResult checkSpam(String emailContent) throws IOException {
        // Replace LF with CRLF for proper protocol compliance
        String formattedEmail = emailContent.replace("\n", "\r\n");
        byte[] emailBytes = formattedEmail.getBytes(StandardCharsets.UTF_8);
        int contentLength = emailBytes.length;

        try (Socket socket = new Socket(host, port)) {
            socket.setSoTimeout(60000); // Set a timeout of 60 seconds

            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            // Construct the CHECK command with necessary headers
            String command = "CHECK SPAMC/1.1 \r\n" +
                    "Content-length: " + contentLength + "\r\n" +
                    "\r\n";
            out.write(command.getBytes(StandardCharsets.UTF_8));
            out.write(emailBytes);
            out.flush();

            // Signal that no more data will be sent
            socket.shutdownOutput();

            // Read the response
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String statusLine = reader.readLine();
            if (statusLine == null) {
                throw new IOException("No response from spamd");
            }


            // Example status lines:
            // "SPAMD/1.2 0 OK"
            // "SPAMD/1.1 0 EX_OK"
            String[] statusParts = statusLine.split(" ");
            if (statusParts.length < 3) { // Adjusted to accept at least 3 parts
                throw new IOException("Invalid response from spamd: " + statusLine);
            }

            String version = statusParts[0];
            String statusCodeStr = statusParts[1];
            String statusMessage = statusParts[2];

            int statusCode;
            try {
                statusCode = Integer.parseInt(statusCodeStr);
            } catch (NumberFormatException e) {
                throw new IOException("Invalid status code from spamd: " + statusCodeStr);
            }

            boolean isSuccess = (statusCode == 0) &&
                    (statusMessage.equalsIgnoreCase("OK") || statusMessage.equalsIgnoreCase("EX_OK"));

            if (!isSuccess) {
                throw new IOException("Spamd reported an error: " + statusLine);
            }

            // Initialize default values
            double score = 0.0;
            double requiredScore = 5.0; // Default threshold, adjust as needed
            boolean isSpam = false;
            StringBuilder report = new StringBuilder();

            // Read headers and parse the report
            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                if (line.startsWith("Spam:")) {
                    // Parse "Spam: True ; 7.4 / 5.0"
                    String[] parts = line.split(";");
                    isSpam = parts[0].toLowerCase().contains("true");

                    if (parts.length > 1) {
                        String[] scores = parts[1].trim().split("/");
                        if (scores.length == 2) {
                            try {
                                score = Double.parseDouble(scores[0].trim());
                                requiredScore = Double.parseDouble(scores[1].trim());
                            } catch (NumberFormatException e) {
                                // Keep default values if parsing fails
                                score = 0.0;
                                requiredScore = 5.0;
                            }
                        }
                    }
                }
                report.append(line).append("\n");
            }

            return new SpamCheckResult(isSpam, score, requiredScore, report.toString());
        }
    }

    public static void main(String[] args) {
        // Example usage
        String spamAssassinHost = "localhost"; // or Docker host IP
        int spamAssassinPort = 783;

        SpamAssassinClient client = new SpamAssassinClient(spamAssassinHost, spamAssassinPort);

        String testEmail = "Date: Wed, 21 Dec 2024 09:48:00 +0000\r\n"
                + "Subject: Limited Time Offer!\r\n"
                + "From: spammer@example.com\r\n"
                + "To: victim@example.com\r\n"
                + "\r\n"
                + "Congratulations! You've won a free iPhone. Click here to claim your prize now!";


        String email = "Date: Wed, 21 Dec 2024 09:48:00 +0000\r\n"
                + "Subject: Can You give me !\r\n"
                + "From: spammer@example.com\r\n"
                + "To: victim@example.com\r\n"
                + "\r\n"
                + "Congratulations! You've won a free iPhone. Click here to claim your prize now!";

        String email2 = "Date: Wed, 21 Dec 2024 09:48:00 +0000\r\n"
                + "Subject: Hello Can You Nooo Me Anythingf\r\n"
                + "From: man2@gmail.com \r\n"
                + "To: man@gmail.com \r\n"
                + "\r\n"
                + "Congratulations! You've won a free iPhone. Click here to claim your prize now!";

        String testEmail3 = "Date: Wed, 21 Dec 2024 09:48:00 +0000\n" +
                "Message-ID: <man@gmail.com>\n" +
                "Received: from man@gmail.com (172.17.0.1)\n" +
                "    for <man2@gmail.com>; Wed, 21 Dec 2024 09:48:00 +0000\n" +
                "Subject: [GitHub] Your personal access token (classic) is about to expire\n\n" +
                "From: man@gmail.com\n" +
                "To: man2@gmail.com\n" +
                "\n" +
                "Hi @man0405,\n" +
                "We noticed your personal access token (classic) \"YouTrack Chuyên đề \" with admin:repo_hook, read:org, and repo scopes will expire in 7 days.";


        try {
            SpamCheckResult result = client.checkSpam(testEmail3);
            if (result.isSpam()) {
                System.out.println("The email is classified as SPAM.");
                System.out.println("Score: " + result.getScore() + " (required: " + result.getRequiredScore() + ")");
            } else {
                System.out.println("The email is not classified as SPAM.");
                System.out.println("Score: " + result.getScore() + " (required: " + result.getRequiredScore() + ")");
            }


            // Print the full report for debugging
            System.out.println("Full Spam Report:\n" + result.getReport());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}



