package com.kapilagro.sasyak.services;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;


@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:swamypenupothula@pavamanagri.com}")
    private String SENDER_MAIL;

    @Value("${app.base-url:https://kapilagro.com}")
    private String BASE_URL;

    @Value("${app.logo-url:http://13.203.61.201:9000/sasyak/logo.avif}")
    private String LOGO_URL;

    @Value("${app.banner-url:http://13.203.61.201:9000/sasyak/banner.png}")
    private String BANNER_URL;

    @Value("${app.playstore-url:https://play.google.com/store/apps/details?id=your_app_id}")
    private String PLAYSTORE_URL;

    @Value("${app.appstore-url:https://apps.apple.com/app/idyour_app_id}")
    private String APPSTORE_URL;

    @Value("${app.contact-email:contact@kapilagro.com}")
    private String CONTACT_EMAIL;

    public void sendMail(String toEmail, String companyName, String generatedPassword) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(SENDER_MAIL);
            helper.setTo(toEmail);
            helper.setSubject("Account Created Successfully - " + companyName);

            String htmlContent = buildHtmlContent(companyName, generatedPassword);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);

            System.out.println("HTML Mail sent successfully to " + toEmail);

        } catch (MessagingException e) {
            System.err.println("Failed to send email to " + toEmail + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    public void sendResetEmail(String to, String name, String resetLink) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setTo(to);
        helper.setSubject("Password Reset Request - Kapil Agro");
        helper.setText(String.format(
                "Dear %s,\n\nPlease click the following link to reset your password:\n%s\n\nThis link will expire in 1 hour.\n\nRegards,\nKapil Agro Team",
                name, resetLink), true);

        try {
            mailSender.send(message);
            System.out.println("✅ [DEBUG] Email sent to: " + to);
        } catch (Exception e) {
            System.out.println("💥 [ERROR] Failed to send email to " + to + ": " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    private String buildResetHtmlContent(String userName, String resetLink) {
        return String.format("""
            <!DOCTYPE html>
            <html>
              <head>
                <meta charset="UTF-8" />
                <title>Password Reset - Kapil Agro</title>
                <style>
                  body { margin: 0; padding: 0; background-color: #f6f8fa; font-family: Arial, sans-serif; color: #333; }
                  .container { max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 10px; overflow: hidden; box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05); }
                  .header { background-color: #4caf50; color: white; padding: 20px; text-align: center; }
                  .header img { max-width: 120px; margin-bottom: 10px; }
                  .content { padding: 30px; }
                  .btn { display: inline-block; padding: 12px 20px; background-color: #4caf50; color: white; text-decoration: none; border-radius: 5px; font-weight: bold; }
                  .footer { text-align: center; font-size: 14px; color: #777; padding: 20px; }
                </style>
              </head>
              <body>
                <div class="container">
                  <div class="header">
                    <img src="%s%s" alt="Kapil Agro Logo" />
                    <h2>Password Reset</h2>
                  </div>
                  <div class="content">
                    <p>Hello %s,</p>
                    <p>We received a request to reset your password. Click the button below to set a new password:</p>
                    <p><a href="%s" class="btn">Reset Password</a></p>
                    <p>This link will expire in 1 hour for security reasons. If you did not request this, please ignore this email.</p>
                  </div>
                  <div class="footer">
                    Best regards,<br />
                    <strong>Kapil Agro Team</strong><br />
                    <a href="mailto:%s">%s</a>
                  </div>
                </div>
              </body>
            </html>
            """, BASE_URL, LOGO_URL, userName, resetLink, CONTACT_EMAIL, CONTACT_EMAIL);
    }

    private String buildHtmlContent(String companyName, String generatedPassword) {
        return String.format("""
                <!DOCTYPE html>
                <html>
                  <head>
                    <meta charset="UTF-8" />
                    <title>Account Created - %s</title>
                    <style>
                      body {
                        margin: 0;
                        padding: 0;
                        background-color: #f6f8fa;
                        font-family: Arial, sans-serif;
                        color: #333;
                      }
                      .container {
                        max-width: 600px;
                        margin: 0 auto;
                        background-color: #ffffff;
                        border-radius: 10px;
                        overflow: hidden;
                        box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
                      }
                      .header {
                        background-color: #4caf50;
                        color: white;
                        padding: 20px;
                        text-align: center;
                      }
                      .header img {
                        max-width: 120px;
                        margin-bottom: 10px;
                      }
                      .banner img {
                        width: 100%%;
                        height: auto;
                      }
                      .content {
                        padding: 30px;
                      }
                      .password-box {
                        background-color: #f0f4f8;
                        border-left: 4px solid #4caf50;
                        padding: 10px 20px;
                        font-size: 16px;
                        font-weight: bold;
                        margin: 20px 0;
                        word-break: break-all;
                      }
                      .btn {
                        display: inline-block;
                        padding: 12px 20px;
                        background-color: #4caf50;
                        color: white;
                        text-decoration: none;
                        border-radius: 5px;
                        font-weight: bold;
                      }
                      .footer {
                        text-align: center;
                        font-size: 14px;
                        color: #777;
                        padding: 20px;
                      }
                      .app-links img {
                        height: 40px;
                        margin: 10px;
                      }
                    </style>
                  </head>
                  <body>
                    <div class="container">
                      <div class="header">
                        <img src="%s%s" alt="%s Logo" />
                        <h2>Welcome to %s</h2>
                      </div>
                      <div class="banner">
                        <img src="%s%s" alt="Welcome Banner" />
                      </div>
                      <div class="content">
                        <p>Hello,</p>
                        <p>Your account has been created successfully at <strong>%s</strong>.</p>
                        <p>This is your generated password:</p>
                        <div class="password-box">%s</div>
                        <p>🔒 For your security, please log in and change your password immediately.</p>
                        <p>
                          <a href="%s/login" class="btn">Login to Your Account</a>
                        </p>
                        <hr style="margin: 30px 0;" />
                        <h3>📱 Get Our App</h3>
                        <p>Install the %s App for a better experience:</p>
                        <div class="app-links">
                          <a href="%s">
                            <img src="https://upload.wikimedia.org/wikipedia/commons/7/78/Google_Play_Store_badge_EN.svg" alt="Google Play" />
                          </a>
                          <a href="%s">
                            <img src="https://developer.apple.com/assets/elements/badges/download-on-the-app-store.svg" alt="App Store" />
                          </a>
                        </div>
                      </div>
                      <div class="footer">
                        Best regards,<br />
                        <strong>%s Team</strong><br />
                        <a href="mailto:%s">%s</a>
                      </div>
                    </div>
                  </body>
                </html>
                """,
                companyName, // title
                BASE_URL, LOGO_URL, companyName, companyName, // header
                BASE_URL, BANNER_URL, // banner
                companyName, // content company name
                generatedPassword, // password
                BASE_URL, // login URL
                companyName, // app section company name
                PLAYSTORE_URL, // Play Store URL
                APPSTORE_URL, // App Store URL
                companyName, // footer company name
                CONTACT_EMAIL, CONTACT_EMAIL // footer email
        );
    }

    // Fallback method for plain text emails (optional)
    public void sendPlainTextMail(String toEmail, String companyName, String generatedPassword) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");

            helper.setFrom(SENDER_MAIL);
            helper.setTo(toEmail);
            helper.setSubject("Account Created Successfully - " + companyName);

            String body = String.format(
                    "Hello,\n\n" +
                            "Your account has been created successfully at %s.\n\n" +
                            "This is your generated password: %s\n\n" +
                            "Please login and change your password immediately for security reasons.\n\n" +
                            "Login URL: %s/login\n\n" +
                            "Best regards,\n" +
                            "%s Team",
                    companyName, generatedPassword, BASE_URL, companyName
            );

            helper.setText(body, false);
            mailSender.send(mimeMessage);

            System.out.println("Plain text mail sent successfully to " + toEmail);

        } catch (MessagingException e) {
            System.err.println("Failed to send email to " + toEmail + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}