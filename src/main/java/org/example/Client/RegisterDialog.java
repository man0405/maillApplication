package org.example.Client;

import org.example.Service.UserService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class RegisterDialog extends JDialog {
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JTextField emailField;
    private JButton registerButton;
    private JButton cancelButton;
    private boolean succeeded;

    public RegisterDialog(Frame parent) {
        super(parent, "Đăng ký tài khoản", true);
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints cs = new GridBagConstraints();

        cs.fill = GridBagConstraints.HORIZONTAL;

        // Email
        JLabel lbEmail = new JLabel("Email: ");
        cs.gridx = 0;
        cs.gridy = 0;
        cs.gridwidth = 1;
        panel.add(lbEmail, cs);

        emailField = new JTextField(20);
        cs.gridx = 1;
        cs.gridy = 0;
        cs.gridwidth = 2;
        panel.add(emailField, cs);

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

        // Confirm Password
        JLabel lbConfirmPassword = new JLabel("Confirm Password: ");
        cs.gridx = 0;
        cs.gridy = 2;
        cs.gridwidth = 1;
        panel.add(lbConfirmPassword, cs);

        confirmPasswordField = new JPasswordField(20);
        cs.gridx = 1;
        cs.gridy = 2;
        cs.gridwidth = 2;
        panel.add(confirmPasswordField, cs);

        panel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        // Buttons
        registerButton = new JButton("Đăng ký");

        registerButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Gọi phương thức đăng ký khi nhấn nút
                String email = getEmail();
                String password = getPassword();
                String confirmPassword = getConfirmPassword();

                if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                    JOptionPane.showMessageDialog(RegisterDialog.this,
                            "Vui lòng điền đầy đủ các trường.",
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Kiểm tra định dạng email (đơn giản)
                if (!email.matches("^(.+)@(.+)$")) {
                    JOptionPane.showMessageDialog(RegisterDialog.this,
                            "Email không hợp lệ.",
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Kiểm tra mật khẩu xác nhận
                if (!password.equals(confirmPassword)) {
                    JOptionPane.showMessageDialog(RegisterDialog.this,
                            "Mật khẩu xác nhận không khớp.",
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Gọi phương thức đăng ký
                UserService userService = new UserService();
                if (userService.userExists(email)) {
                    JOptionPane.showMessageDialog(RegisterDialog.this,
                            "Email đã được sử dụng.",
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                boolean success = userService.registerUser(email, password);
                if (success) {
                    JOptionPane.showMessageDialog(RegisterDialog.this,
                            "Đăng ký thành công!",
                            "Thành công",
                            JOptionPane.INFORMATION_MESSAGE);
                    succeeded = true;
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(RegisterDialog.this,
                            "Đăng ký thất bại! Vui lòng thử lại.",
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE);
                    // Reset fields
                    emailField.setText("");
                    passwordField.setText("");
                    confirmPasswordField.setText("");
                    succeeded = false;
                }
            }
        });

        cancelButton = new JButton("Hủy");

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        JPanel bp = new JPanel();
        bp.add(registerButton);
        bp.add(cancelButton);

        getContentPane().add(panel, BorderLayout.CENTER);
        getContentPane().add(bp, BorderLayout.PAGE_END);

        pack();
        setResizable(false);
        setLocationRelativeTo(parent);
    }

    public String getEmail() {
        return emailField.getText().trim();
    }

    public String getPassword() {
        return new String(passwordField.getPassword());
    }

    public String getConfirmPassword() {
        return new String(confirmPasswordField.getPassword());
    }

    public boolean isSucceeded() {
        return succeeded;
    }
}