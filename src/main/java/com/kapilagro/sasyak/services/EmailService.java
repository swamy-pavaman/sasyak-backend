package com.kapilagro.sasyak.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:swamypenupothula@pavamanagri.com}")
    private String SENDER_MAIL;

    public void sendMail(String toEmail, String companyName, String generatedPassword) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom(SENDER_MAIL);
        mailMessage.setTo(toEmail);
        mailMessage.setSubject("Account Created successfully");

        String body = String.format(
                "Hello,\n\n" +
                        "Your account has been created successfully at %s.\n\n" +
                        "This is your generated password: %s\n\n" +
                        "Please login and change your password immediately for security reasons.\n\n" +
                        "Best regards,\n" +
                        "%s Team",
                companyName, generatedPassword, companyName
        );

        mailMessage.setText(body);
        mailSender.send(mailMessage);

        System.out.println("Mail sent successfully to " + toEmail);
    }

}
