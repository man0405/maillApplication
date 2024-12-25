package org.example.Client;

import org.example.Service.UserService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LoginDialog extends JDialog {
    private JTextField emailFiled;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton registerButton;
    private boolean succeeded;
    private String username;
    private String password; // Thêm biến này để lưu mật khẩu

    public LoginDialog(Frame parent) {
        super(parent, "Đăng nhập", true);
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints cs = new GridBagConstraints();

        cs.fill = GridBagConstraints.HORIZONTAL;

        // Username
        JLabel lbUsername = new JLabel("Email: ");
        cs.gridx = 0;
        cs.gridy = 0;
        cs.gridwidth = 1;
        panel.add(lbUsername, cs);

        emailFiled = new JTextField(20);
        cs.gridx = 1;
        cs.gridy = 0;
        cs.gridwidth = 2;
        panel.add(emailFiled, cs);

        // Password
        JLabel lbPassword = new JLabel("Password: ");
        cs.gridx = 0;
        cs.gridy = 1;
        cs.gridwidth = 1;
        panel.add(lbPassword, cs);

        passwordField = new JPasswordField(20);
        cs.gridx = 1;
        cs.gridy = 1;
        cs.gridwidth = 2;
        panel.add(passwordField, cs);

        panel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        // Buttons
        loginButton = new JButton("Login");
        registerButton = new JButton("Register");

        JPanel bp = new JPanel();
        bp.add(loginButton);
        bp.add(registerButton);

        getContentPane().add(panel, BorderLayout.CENTER);
        getContentPane().add(bp, BorderLayout.PAGE_END);

        pack();
        setResizable(false);
        setLocationRelativeTo(parent);

        // Hành động khi nhấn nút Login
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                UserService userService = new UserService();
                String user = getUsername();
                String pass = getPassword();
                if (user.isEmpty() || pass.isEmpty()) {
                    JOptionPane.showMessageDialog(LoginDialog.this,
                            "Vui lòng điền đầy đủ các trường.",
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (userService.authenticateUser(user, pass)) {
                    JOptionPane.showMessageDialog(LoginDialog.this,
                            "Đăng nhập thành công!",
                            "Thành công",
                            JOptionPane.INFORMATION_MESSAGE);
                    succeeded = true;
                    username = user;
                    password = pass;
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(LoginDialog.this,
                            "Username hoặc Password không đúng.",
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                    // Reset fields
                    emailFiled.setText("");
                    passwordField.setText("");
                    succeeded = false;
                }
            }
        });

        // Hành động khi nhấn nút Register
        registerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                RegisterDialog registerDlg = new RegisterDialog(parent);
                registerDlg.setVisible(true);
                // Bạn có thể thêm logic sau khi đăng ký thành công nếu cần
            }
        });
    }

    public String getUsername() {
        return emailFiled.getText().trim();
    }

    public String getPassword() {
        return new String(passwordField.getPassword());
    }

    public boolean isSucceeded() {
        return succeeded;
    }

    public String getRegisteredUsername() {
        return username;
    }

    public String getRegisteredPassword() {
        return password;
    }
}
