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

    public void sendMail(String toEmail, String companyName, String generatedPassword) {
        MimeMessage mimeMessage = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(SENDER_MAIL);
            helper.setTo(toEmail);
            helper.setSubject("Account Created Successfully");

            String htmlContent = generateHtmlContent(companyName, generatedPassword);
            helper.setText(htmlContent, true); // true = isHtml

            mailSender.send(mimeMessage);
            System.out.println("HTML mail sent successfully to " + toEmail);
        } catch (MessagingException e) {
            e.printStackTrace();
            System.err.println("Error sending HTML mail to " + toEmail);
        }
    }

    private String generateHtmlContent(String companyName, String password) {
        return """
            <html>
              <body style="margin:0;padding:0;background-color:#f6f8fa;font-family:Arial,sans-serif;">
                <div style="max-width:600px;margin:0 auto;background:#fff;border-radius:10px;box-shadow:0 2px 10px rgba(0,0,0,0.05);overflow:hidden;">
                  <div style="background:#4caf50;color:#fff;padding:20px;text-align:center;">
                    <img src="http://13.203.61.201:9000/sasyak/logo.avif" alt="%s Logo" style="max-width:120px;margin-bottom:10px;" />
                    <h2>Welcome to %s</h2>
                  </div>
                  <img src="http://13.203.61.201:9000/sasyak/banner.png" alt="Welcome Banner" style="width:100%;height:auto;" />
                  <div style="padding:30px;">
                    <p>Hello,</p>
                    <p>Your account has been created successfully at <strong>%s</strong>.</p>
                    <p>This is your generated password:</p>
                    <div style="background:#f0f4f8;border-left:4px solid #4caf50;padding:10px 20px;font-size:16px;font-weight:bold;margin:20px 0;word-break:break-all;">
                      %s
                    </div>
                    <p>🔒 For your security, please log in and change your password immediately.</p>
                    <p><a href="https://kapilagro.com/login" style="display:inline-block;padding:12px 20px;background:#4caf50;color:#fff;text-decoration:none;border-radius:5px;font-weight:bold;">Login to Your Account</a></p>
                    <hr style="margin:30px 0;" />
                    <h3>📱 Get Our App</h3>
                    <p>Install the Kapil Agro App for a better experience:</p>
                    <div style="text-align:center;">
                      <a href="https://play.google.com/store/apps/details?id=your_app_id">
                        <img src="https://upload.wikimedia.org/wikipedia/commons/7/78/Google_Play_Store_badge_EN.svg" style="height:40px;margin:10px;" alt="Google Play" />
                      </a>
                      <a href="https://apps.apple.com/app/idyour_app_id">
                        <img src="https://developer.apple.com/assets/elements/badges/download-on-the-app-store.svg" style="height:40px;margin:10px;" alt="App Store" />
                      </a>
                    </div>
                  </div>
                  <div style="text-align:center;font-size:14px;color:#777;padding:20px;">
                    Best regards,<br /><strong>%s Team</strong><br />
                    <a href="mailto:contact@kapilagro.com">contact@kapilagro.com</a>
                  </div>
                </div>
              </body>
            </html>
            """.formatted(companyName, companyName, companyName, password, companyName);
    }
}
