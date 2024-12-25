package org.example.Client;

import org.bson.types.ObjectId;
import org.example.Client.Email.POP3Client;
import org.example.Client.Email.SMTPClient;
import org.example.Client.Entity.Email;
import org.example.DTO.SpamCheckResult;
import org.example.Service.UserService;
import org.example.Server.SpamAssassinClient;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MailClientUI extends JFrame {
    private JTable emailTable;
    private JTextArea emailContent;
    private String currentUsername;
    private String currentUserEmail;
    private String currentUserPassword;
    private DefaultTableModel tableModel;
    private JButton inboxButton;
    private JButton sentButton;
    private JButton spamButton;
    private static final String VIEW_INBOX = "INBOX";
    private static final String VIEW_SENT = "SENT";
    private static final String VIEW_SPAM = "SPAM";
    private String currentView = VIEW_INBOX;

    public MailClientUI() {
        LoginDialog loginDlg = new LoginDialog(this);
        loginDlg.setVisible(true);
        if (loginDlg.isSucceeded()) {
            currentUsername = loginDlg.getUsername();
            currentUserPassword = loginDlg.getPassword();
            UserService userService = new UserService();
            currentUserEmail = userService.getUserEmail(currentUsername);
            initUI();
            loadEmails();
            startEmailCheckTimer();
        } else {
            System.exit(0);
        }
    }

    private void initUI() {
        setTitle("Java Mail Client - " + currentUsername);
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Thanh điều hướng bên trái
        JPanel navigationPanel = new JPanel();
        navigationPanel.setLayout(new BoxLayout(navigationPanel, BoxLayout.Y_AXIS));
        navigationPanel.setPreferredSize(new Dimension(200, getHeight()));

        inboxButton = new JButton("Hộp thư đến");
        sentButton = new JButton("Đã gửi");
        spamButton = new JButton("Thư rác");

        inboxButton.setBackground(Color.LIGHT_GRAY);
        navigationPanel.add(inboxButton);
        navigationPanel.add(sentButton);
        navigationPanel.add(spamButton);
        add(navigationPanel, BorderLayout.WEST);

        inboxButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("=== INBOX BUTTON CLICKED ===");
                currentView = VIEW_INBOX;
                loadEmailsByType(false); // Load non-spam emails
                resetButtonBackgrounds();
                inboxButton.setBackground(Color.LIGHT_GRAY);
            }
        });

        sentButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                currentView = VIEW_SENT;
                loadSentEmails();
                resetButtonBackgrounds();
                sentButton.setBackground(Color.LIGHT_GRAY);
            }
        });

        spamButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("=== SPAM BUTTON CLICKED ===");
                currentView = VIEW_SPAM;
                loadEmailsByType(true); // Load spam emails
                resetButtonBackgrounds();
                spamButton.setBackground(Color.LIGHT_GRAY);
            }
        });

        addHoverEffect(inboxButton);
        addHoverEffect(sentButton);

        addHoverEffect(spamButton);

        // Danh sách email
        String[] columnNames = {"ID", "Người gửi", "Người nhận", "Tiêu đề", "Ngày"};
        tableModel = new DefaultTableModel(columnNames, 0);
        emailTable = new JTable(tableModel);
        JScrollPane tableScrollPane = new JScrollPane(emailTable);
        add(tableScrollPane, BorderLayout.CENTER);

        // Khu vực xem email
        emailContent = new JTextArea();
        emailContent.setEditable(false);
        JScrollPane contentScrollPane = new JScrollPane(emailContent);
        contentScrollPane.setPreferredSize(new Dimension(800, 200));
        add(contentScrollPane, BorderLayout.SOUTH);

        // Thanh công cụ trên cùng
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton composeButton = new JButton("Soạn thư");
        JButton searchButton = new JButton("Tìm kiếm");
        JButton deleteButton = new JButton("Xóa Email"); // <-- Added button

        toolbar.add(composeButton);
        toolbar.add(searchButton);
        toolbar.add(deleteButton);
        add(toolbar, BorderLayout.NORTH);

        // Hành động bấm nút Soạn thư
        composeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showComposeDialog();
            }
        });

        deleteButton.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            int selectedRow = emailTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(MailClientUI.this,
                        "Vui lòng chọn một email để xóa.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            String emailId = emailTable.getValueAt(selectedRow, 0).toString();

            POP3Client pop3 = new POP3Client("localhost", 1100);
            if (pop3.connect()) {
                if (pop3.login(currentUserEmail, currentUserPassword)) {
                    if (pop3.deleteEmail(new org.bson.types.ObjectId(emailId))) {
                        JOptionPane.showMessageDialog(MailClientUI.this,
                                "Đã xóa email thành công!",
                                "Success",
                                JOptionPane.INFORMATION_MESSAGE);
                        loadEmails(); // reload emails
                    } else {
                        JOptionPane.showMessageDialog(MailClientUI.this,
                                "Không thể xóa email.",
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                    pop3.quit();
                }
            }
        }
    });

        // Hành động chọn email trong bảng
        emailTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && emailTable.getSelectedRow() != -1) {
                int selectedRow = emailTable.getSelectedRow();
                String emailId = emailTable.getValueAt(selectedRow, 0).toString();
                loadEmailContent(new ObjectId(emailId));
            }
        });

        setVisible(true);
    }

    private void showComposeDialog() {
        JDialog composeDialog = new JDialog(this, "Soạn thư", true);
        composeDialog.setSize(500, 400);
        composeDialog.setLayout(new BorderLayout(10, 10));

        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints cs = new GridBagConstraints();
        cs.fill = GridBagConstraints.HORIZONTAL;
        cs.insets = new Insets(5, 5, 5, 5);

        // Đến
        JLabel lbTo = new JLabel("Đến: ");
        cs.gridx = 0;
        cs.gridy = 0;
        cs.gridwidth = 1;
        fieldsPanel.add(lbTo, cs);

        JTextField toField = new JTextField(20);
        cs.gridx = 1;
        cs.gridy = 0;
        cs.gridwidth = 2;
        fieldsPanel.add(toField, cs);

        // Tiêu đề
        JLabel lbSubject = new JLabel("Tiêu đề: ");
        cs.gridx = 0;
        cs.gridy = 1;
        cs.gridwidth = 1;
        fieldsPanel.add(lbSubject, cs);

        JTextField subjectField = new JTextField(20);
        cs.gridx = 1;
        cs.gridy = 1;
        cs.gridwidth = 2;
        fieldsPanel.add(subjectField, cs);

        // Nội dung
        JLabel lbBody = new JLabel("Nội dung: ");
        cs.gridx = 0;
        cs.gridy = 2;
        cs.gridwidth = 1;
        fieldsPanel.add(lbBody, cs);

        cs.gridx = 1;
        cs.gridy = 2;
        cs.gridwidth = 2;
        cs.fill = GridBagConstraints.BOTH;
        cs.weightx = 1.0;
        cs.weighty = 1.0;
        JTextArea bodyArea = new JTextArea(10, 30);
        fieldsPanel.add(new JScrollPane(bodyArea), cs);

        composeDialog.add(fieldsPanel, BorderLayout.CENTER);

        // Nút Gửi và Hủy
        JPanel bp = new JPanel();
        JButton sendButton = new JButton("Gửi");
        JButton cancelButton = new JButton("Hủy");
        bp.add(sendButton);
        bp.add(cancelButton);
        composeDialog.add(bp, BorderLayout.SOUTH);

        // Hành động bấm nút Gửi
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String to = toField.getText().trim();
                String subject = subjectField.getText().trim();
                String body = bodyArea.getText().trim();

                if (to.isEmpty() || subject.isEmpty() || body.isEmpty()) {
                    JOptionPane.showMessageDialog(composeDialog,
                            "Vui lòng điền đầy đủ các trường.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Kiểm tra định dạng email người nhận
                if (!to.matches("^(.+)@(.+)$")) {
                    JOptionPane.showMessageDialog(composeDialog,
                            "Email người nhận không hợp lệ.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Gửi email qua SMTP
                SMTPClient smtp = new SMTPClient("localhost", 2525);
                boolean success = smtp.sendEmail(currentUserEmail, to, subject, body);
                if (success) {
                    JOptionPane.showMessageDialog(composeDialog, "Gửi email thành công!");
                    composeDialog.dispose();
                    // Bạn có thể cập nhật danh sách email đã gửi ở đây
                } else {
                    JOptionPane.showMessageDialog(composeDialog, "Gửi email thất bại!");
                }
            }
        });

        // Hành động bấm nút Hủy
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                composeDialog.dispose();
            }
        });

        composeDialog.setLocationRelativeTo(this);
        composeDialog.setVisible(true);
    }

    private void loadEmails() {
        loadEmailsByType(false); // Load non-spam emails by default
    }

    private void loadEmailsByType(boolean loadSpam) {
        try {
            System.out.println("\n=== LOADING EMAILS ===");
            System.out.println("LoadSpam parameter: " + loadSpam);
            
            POP3Client pop3 = new POP3Client("localhost", 1100);

            
            System.out.println("Connecting to POP3...");
            if (pop3.connect()) {
                System.out.println("Connected to POP3");
                if (pop3.login(currentUserEmail, currentUserPassword)) {
                    System.out.println("Logged in successfully");
                    List<String> emails ;
                     if (loadSpam) {
                        emails = pop3.listEmails("SPAM");
                    } else {
                        emails = pop3.listEmails();
                     }
                    DefaultTableModel model = (DefaultTableModel) emailTable.getModel();
                    
                    // Keep column names consistent for all views
                    model.setColumnIdentifiers(new Object[]{"ID", "Người gửi", "Người nhận", "Tiêu đề", "Ngày"});
                    
                    model.setRowCount(0); // Clear existing data

                    for (String emailInfo : emails) {
                        String[] parts = emailInfo.split(" ");
                        if (parts.length >= 2) {
                            try {
                                ObjectId id = new ObjectId(parts[0]);
                                String content = pop3.retrieveEmail(id).toString();
                                if (emailInfo != null ) {
                                    String sender = extractSender(content);
                                    String receiver = extractReceiver(content);
                                    String subject = extractSubject(content);
                                    String date = extractDate(content);
                                    model.addRow(new Object[]{id, sender, receiver, subject, date});
                                    System.out.println("Added email to table: " + subject);
                                }
                            } catch (Exception e) {
                                System.err.println("Error processing email ID " + parts[0] + ": " + e.getMessage());
                                e.printStackTrace();
                                // If spam check fails, show in non-spam folder
                                if (!loadSpam) {
                                    String content = pop3.retrieveEmail(new ObjectId(parts[0])).toString();
                                    String sender = extractSender(content);
                                    String receiver = extractReceiver(content);
                                    String subject = extractSubject(content);
                                    String date = extractDate(content);
                                    model.addRow(new Object[]{parts[0], sender, receiver, subject, date});
                                }
                            }
                        }
                    }
                } else {
                    System.err.println("POP3 login failed");
                }
            } else {
                System.err.println("POP3 connection failed");
            }
        } catch (Exception e) {
            System.err.println("Error in loadEmailsByType: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadSentEmails() {
        try {
            POP3Client pop3 = new POP3Client("localhost", 1100);
            if (pop3.connect()) {
                if (pop3.login(currentUserEmail, currentUserPassword)) {
                    List<String> emails = pop3.listSentEmails();
                    DefaultTableModel model = (DefaultTableModel) emailTable.getModel();
                    
                    // Keep column names consistent
                    model.setColumnIdentifiers(new Object[]{"ID", "Người gửi", "Người nhận", "Tiêu đề", "Ngày"});
                    model.setRowCount(0); // Clear existing data

                    for (String emailInfo : emails) {
                        String[] parts = emailInfo.split(" ");
                        if (parts.length >= 2) {
                            ObjectId id = new ObjectId(parts[0]);
                            Email email = pop3.retrieveSentEmail(id);
                            if (email != null) {
                                model.addRow(new Object[]{
                                    id,
                                    currentUserEmail, // Sender is current user
                                    email.getTo(),    // Receiver
                                    email.getSubject(),
                                    email.getDate()
                                });
                            }
                        }
                    }
                }
                pop3.quit();
            }
        } catch (Exception e) {
            System.err.println("Error loading sent emails: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadEmailContent(ObjectId id) {
        POP3Client pop3 = new POP3Client("localhost", 1100);
        if (pop3.connect()) {
            if (pop3.login(currentUserEmail, currentUserPassword)) { // Sử dụng mật khẩu đã lưu
                if(currentView == VIEW_SENT) {
                    String email = pop3.retrieveSentEmail(id).toString();
                    if (email == null) {
                        emailContent.setText("Không thể tải email");
                        return;
                    }
                    emailContent.setText(email);
                    return;
                }
                String content = pop3.retrieveEmail(id).toString();
                if (content == null) {
                    emailContent.setText("Không thể tải email");
                } else {
                    emailContent.setText(content);
                }

            }
            pop3.quit();
        }
    }

    private String extractSender(String emailContent) {
        // Tìm dòng bắt đầu bằng "From:"
        BufferedReader reader = new BufferedReader(new StringReader(emailContent));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("From:")) {
                    return line.substring(5).trim();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "(Unknown Sender)";
    }

    private String extractSubject(String emailContent) {
        // Tìm dòng bắt đầu bằng "Subject:"
        BufferedReader reader = new BufferedReader(new StringReader(emailContent));
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

    private String extractDate(String emailContent) {
        // Tìm dòng có chứa "Date:" nếu có
        BufferedReader reader = new BufferedReader(new StringReader(emailContent));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Date:")) {
                    return line.substring(5).trim();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "(No Date)";
    }

    // Add new method to extract receiver
    private String extractReceiver(String emailContent) {
        BufferedReader reader = new BufferedReader(new StringReader(emailContent));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("To:")) {
                    return line.substring(3).trim();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "(Unknown Receiver)";
    }

    private void startEmailCheckTimer() {
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    System.out.println("Timer check - Current view: " + currentView);
                    switch (currentView) {
                        case VIEW_SENT:
                            loadSentEmails();
                            break;
                        case VIEW_SPAM:
                            loadEmailsByType(true);
                            break;
                        case VIEW_INBOX:
                        default:
                            loadEmailsByType(false);
                            break;
                    }
                });
            }
        }, 0, 60000); // 10 seconds
    }


    private void resetButtonBackgrounds() {
        inboxButton.setBackground(null);
        sentButton.setBackground(null);
        spamButton.setBackground(null);
    }

    private void addHoverEffect(JButton button) {
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(Color.GRAY);
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (!button.getBackground().equals(Color.LIGHT_GRAY)) {
                    button.setBackground(null);
                }
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MailClientUI());
    }
}