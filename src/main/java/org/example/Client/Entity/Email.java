package org.example.Client.Entity;


public class Email {
    private String date;
    private String subject;
    private String from;
    private String to;
    private String body;

    public Email(String date, String subject, String from, String to, String body) {
        this.date = date;
        this.subject = subject;
        this.from = from;
        this.to = to;
        this.body = body;
    }

    // Getters
    public String getDate() { return date; }
    public String getSubject() { return subject; }
    public String getFrom() { return from; }
    public String getTo() { return to; }
    public String getBody() { return body; }

    @Override
    public String toString() {
        return String.format("Date: %s\r\nSubject: %s\r\nFrom: %s\r\nTo: %s\r\n\r\n%s",
                date, subject, from, to, body);
    }
}
