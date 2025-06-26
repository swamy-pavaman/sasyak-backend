package com.kapilagro.sasyak.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import java.util.*;

@Service
public class EmailService {

    // Resend API Configuration
    @Value("${resend.api.key}")
    private String resendApiKey;

    // Brevo API Configuration (keeping for backward compatibility)
    @Value("${brevo.api.key:}")
    private String brevoApiKey;

    // EmailJS Configuration
    @Value("${emailjs.service.id:service_dht4psi}")
    private String emailjsServiceId;

    @Value("${emailjs.user.id:PBdYqrhJGqu7EUQEc}")
    private String emailjsUserId;

    @Value("${emailjs.private.key:EWjVeE0CsRJPfnf83CnxR}")
    private String emailjsPrivateKey;

    @Value("${emailjs.template.admin:template_l86as9d}")
    private String emailjsAdminTemplateId;

    @Value("${emailjs.template.user:template_3ml3srz}")
    private String emailjsUserTemplateId;

    // Email service provider preference
    @Value("${email.provider:emailjs}")
    private String emailProvider; // "resend", "brevo", or "emailjs"

    // Your existing configuration values
    @Value("${spring.mail.username:swamypenupothula@pavamanagri.com}")
    private String SENDER_MAIL;

    @Value("${app.base-url:https://kapilagro.com}")
    private String BASE_URL;

    @Value("${app.admin-dashboard-url:https://admin.kapilagro.com}")
    private String ADMIN_DASHBOARD_URL;

    @Value("${app.user-app-url:https://app.kapilagro.com}")
    private String USER_APP_URL;

    @Value("${app.logo-url:http://13.203.61.201:9000/sasyak/logo.avif}")
    private String LOGO_URL;

    @Value("${app.banner-url:http://13.203.61.201:9000/sasyak/banner.png}")
    private String BANNER_URL;

    @Value("${app.contact-email:contact@kapilagro.com}")
    private String CONTACT_EMAIL;

    @Value("${app.company-name:Sasyak}")
    private String COMPANY_BRAND_NAME;

    private final RestTemplate restTemplate;

    public EmailService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Main method to send role-based emails - routes to appropriate provider
     */
    public void sendMail(String toEmail, String companyName, String generatedPassword, String userRole, String name) {
        switch (emailProvider.toLowerCase()) {
            case "emailjs":
                sendMailViaEmailJS(toEmail, companyName, generatedPassword, userRole, name);
                break;
            case "brevo":
                sendMailViaBrevo(toEmail, companyName, generatedPassword, userRole, name);
                break;
            default:
                sendMailViaResend(toEmail, companyName, generatedPassword, userRole, name);
                break;
        }
    }

    /**
     * Send email via EmailJS API
     */
    public void sendMailViaEmailJS(String toEmail, String companyName, String generatedPassword, String userRole, String name) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Determine template based on role
            String templateId = "ADMIN".equalsIgnoreCase(userRole) ?
                    emailjsAdminTemplateId : emailjsUserTemplateId;

            // Build message content based on role
            String messageContent = "ADMIN".equalsIgnoreCase(userRole) ?
                    buildAdminMessageContent(companyName, generatedPassword, name) :
                    buildUserMessageContent(companyName, generatedPassword, userRole, name);

            Map<String, Object> emailData = new HashMap<>();
            emailData.put("service_id", emailjsServiceId);
            emailData.put("template_id", templateId);
            emailData.put("user_id", emailjsUserId);

            // Template parameters - EmailJS templates expect these variable names
            Map<String, Object> templateParams = new HashMap<>();
            templateParams.put("email", toEmail);
            templateParams.put("name", name);
            templateParams.put("company_name",companyName);
            templateParams.put("from_name", COMPANY_BRAND_NAME + " Team");
            templateParams.put("message", messageContent); // Single message variable as requested

            emailData.put("template_params", templateParams);

            // Add private key for authentication
            if (emailjsPrivateKey != null && !emailjsPrivateKey.isEmpty()) {
                emailData.put("accessToken", emailjsPrivateKey);
            }

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(emailData, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    "https://api.emailjs.com/api/v1.0/email/send",
                    request,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                System.out.println("Email sent successfully to " + toEmail + " (Role: " + userRole + ") via EmailJS");
            } else {
                System.err.println("Failed to send email via EmailJS: " + response.getStatusCode());
            }

        } catch (HttpClientErrorException e) {
            System.err.println("EmailJS API Error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
        } catch (Exception e) {
            System.err.println("Failed to send email to " + toEmail + " via EmailJS: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Build message content for Admin emails (for EmailJS {{message}} variable)
     */
    private String buildAdminMessageContent(String companyName, String generatedPassword, String name) {
        return String.format("""
            Hello %s,

            🎉 Your farm "%s" has been created successfully!

            ADMIN ACCESS CREDENTIALS:
            Password: %s

            🔧 Admin Dashboard Access:
            %s

            As an admin, you can:
            ✓ Create and manage users
            ✓ Configure tenant settings
            ✓ Monitor farm operations
            ✓ Generate reports
            ✓ Manage permissions

            🔒 SECURITY NOTICE: Please change your password immediately after your first login for security reasons.

            Best regards,
            %s Team
            Contact: %s
            """,
                name, companyName, generatedPassword, ADMIN_DASHBOARD_URL,
                COMPANY_BRAND_NAME, CONTACT_EMAIL
        );
    }

    /**
     * Build message content for User emails (for EmailJS {{message}} variable)
     */
    private String buildUserMessageContent(String companyName, String generatedPassword, String userRole, String name) {
        return String.format("""
            Hello %s,

            Your account has been created successfully at %s with role: %s

            Your generated password: %s

            📱 Access Your Account:
            %s

            🔒 For your security: Please log in and change your password immediately.

            Best regards,
            %s Team
            Contact: %s
            """,
                name, companyName, userRole, generatedPassword, USER_APP_URL,
                COMPANY_BRAND_NAME, CONTACT_EMAIL
        );
    }

    /**
     * Send simple email via EmailJS (utility method)
     */
    public void sendSimpleEmailViaEmailJS(String toEmail, String subject, String message, String templateId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Use provided template ID or default to user template
            String finalTemplateId = (templateId != null && !templateId.isEmpty()) ?
                    templateId : emailjsUserTemplateId;

            Map<String, Object> emailData = new HashMap<>();
            emailData.put("service_id", emailjsServiceId);
            emailData.put("template_id", finalTemplateId);
            emailData.put("user_id", emailjsUserId);

            Map<String, Object> templateParams = new HashMap<>();
            templateParams.put("to_email", toEmail);
            templateParams.put("from_name", COMPANY_BRAND_NAME + " Team");
            templateParams.put("subject", subject);
            templateParams.put("message", message);

            emailData.put("template_params", templateParams);

            if (emailjsPrivateKey != null && !emailjsPrivateKey.isEmpty()) {
                emailData.put("accessToken", emailjsPrivateKey);
            }

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(emailData, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    "https://api.emailjs.com/api/v1.0/email/send",
                    request,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                System.out.println("Simple email sent successfully to " + toEmail + " via EmailJS");
            } else {
                System.err.println("Failed to send simple email via EmailJS: " + response.getStatusCode());
            }

        } catch (Exception e) {
            System.err.println("Failed to send simple email to " + toEmail + " via EmailJS: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Send email via Resend API
     */
    public void sendMailViaResend(String toEmail, String companyName, String generatedPassword, String userRole, String name) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + resendApiKey);

            Map<String, Object> emailData = new HashMap<>();

            // From
            emailData.put("from", COMPANY_BRAND_NAME + " Team <" + SENDER_MAIL + ">");

            // To
            emailData.put("to", Arrays.asList(toEmail));

            // Subject based on role
            String subject = "ADMIN".equalsIgnoreCase(userRole) ?
                    "Farm Created Successfully - Admin Access" :
                    "Account Created Successfully - " + companyName;
            emailData.put("subject", subject);

            // HTML Content based on role
            String htmlContent = "ADMIN".equalsIgnoreCase(userRole) ?
                    buildAdminHtmlTemplate(companyName, generatedPassword, name) :
                    buildUserHtmlTemplate(companyName, generatedPassword, userRole, name);
            emailData.put("html", htmlContent);

            // Plain text version
            String textContent = "ADMIN".equalsIgnoreCase(userRole) ?
                    buildAdminPlainTextTemplate(companyName, generatedPassword, name) :
                    buildUserPlainTextTemplate(companyName, generatedPassword, userRole, name);
            emailData.put("text", textContent);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(emailData, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    "https://api.resend.com/emails",
                    request,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                System.out.println("Email sent successfully to " + toEmail + " (Role: " + userRole + ") via Resend");
            } else {
                System.err.println("Failed to send email via Resend: " + response.getStatusCode());
            }

        } catch (HttpClientErrorException e) {
            System.err.println("Resend API Error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
        } catch (Exception e) {
            System.err.println("Failed to send email to " + toEmail + " via Resend: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Send email via Brevo API (existing method)
     */
    public void sendMailViaBrevo(String toEmail, String companyName, String generatedPassword, String userRole, String name) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", brevoApiKey);

            Map<String, Object> emailData = new HashMap<>();

            // Sender
            Map<String, String> sender = new HashMap<>();
            sender.put("email", SENDER_MAIL);
            sender.put("name", COMPANY_BRAND_NAME + " Team");
            emailData.put("sender", sender);

            // Recipient
            Map<String, String> recipient = new HashMap<>();
            recipient.put("email", toEmail);
            recipient.put("name", name);
            emailData.put("to", Arrays.asList(recipient));

            // Subject based on role
            String subject = "ADMIN".equalsIgnoreCase(userRole) ?
                    "Farm Created Successfully - Admin Access" :
                    "Account Created Successfully - " + companyName;
            emailData.put("subject", subject);

            // HTML Content based on role
            String htmlContent = "ADMIN".equalsIgnoreCase(userRole) ?
                    buildAdminHtmlTemplate(companyName, generatedPassword, name) :
                    buildUserHtmlTemplate(companyName, generatedPassword, userRole, name);
            emailData.put("htmlContent", htmlContent);

            // Plain text version
            String textContent = "ADMIN".equalsIgnoreCase(userRole) ?
                    buildAdminPlainTextTemplate(companyName, generatedPassword, name) :
                    buildUserPlainTextTemplate(companyName, generatedPassword, userRole, name);
            emailData.put("textContent", textContent);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(emailData, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    "https://api.brevo.com/v3/smtp/email",
                    request,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.CREATED) {
                System.out.println("Email sent successfully to " + toEmail + " (Role: " + userRole + ") via Brevo");
            } else {
                System.err.println("Failed to send email: " + response.getStatusCode());
            }

        } catch (HttpClientErrorException e) {
            System.err.println("Brevo API Error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
        } catch (Exception e) {
            System.err.println("Failed to send email to " + toEmail + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Send simple email via Resend (utility method)
     */
    public void sendSimpleEmailViaResend(String toEmail, String subject, String htmlContent, String textContent) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + resendApiKey);

            Map<String, Object> emailData = new HashMap<>();
            emailData.put("from", COMPANY_BRAND_NAME + " Team <" + SENDER_MAIL + ">");
            emailData.put("to", Arrays.asList(toEmail));
            emailData.put("subject", subject);
            emailData.put("html", htmlContent);
            if (textContent != null && !textContent.isEmpty()) {
                emailData.put("text", textContent);
            }

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(emailData, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    "https://api.resend.com/emails",
                    request,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                System.out.println("Simple email sent successfully to " + toEmail + " via Resend");
            } else {
                System.err.println("Failed to send simple email via Resend: " + response.getStatusCode());
            }

        } catch (Exception e) {
            System.err.println("Failed to send simple email to " + toEmail + " via Resend: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Send email with attachment via Resend
     */
    public void sendEmailWithAttachmentViaResend(String toEmail, String subject, String htmlContent,
                                                 String textContent, String attachmentName,
                                                 String attachmentContent, String contentType) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + resendApiKey);

            Map<String, Object> emailData = new HashMap<>();
            emailData.put("from", COMPANY_BRAND_NAME + " Team <" + SENDER_MAIL + ">");
            emailData.put("to", Arrays.asList(toEmail));
            emailData.put("subject", subject);
            emailData.put("html", htmlContent);
            if (textContent != null && !textContent.isEmpty()) {
                emailData.put("text", textContent);
            }

            // Add attachment
            Map<String, Object> attachment = new HashMap<>();
            attachment.put("filename", attachmentName);
            attachment.put("content", attachmentContent); // Base64 encoded content
            attachment.put("content_type", contentType);
            emailData.put("attachments", Arrays.asList(attachment));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(emailData, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    "https://api.resend.com/emails",
                    request,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                System.out.println("Email with attachment sent successfully to " + toEmail + " via Resend");
            } else {
                System.err.println("Failed to send email with attachment via Resend: " + response.getStatusCode());
            }

        } catch (Exception e) {
            System.err.println("Failed to send email with attachment to " + toEmail + " via Resend: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * ADMIN EMAIL TEMPLATE
     */
    private String buildAdminHtmlTemplate(String companyName, String generatedPassword, String name) {
        return String.format("""
                <!DOCTYPE html>
                <html>
                  <head>
                    <meta charset="UTF-8" />
                    <title>Farm Created - Admin Access</title>
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
                        background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                        color: white;
                        padding: 25px;
                        text-align: center;
                      }
                      .header img {
                        max-width: 120px;
                        margin-bottom: 15px;
                      }
                      .admin-badge {
                        background-color: rgba(255,255,255,0.2);
                        padding: 5px 15px;
                        border-radius: 20px;
                        font-size: 12px;
                        font-weight: bold;
                        display: inline-block;
                        margin-top: 10px;
                      }
                      .banner img {
                        width: 100%%;
                        height: auto;
                      }
                      .content {
                        padding: 30px;
                      }
                      .welcome-message {
                        background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                        color: white;
                        padding: 20px;
                        border-radius: 8px;
                        text-align: center;
                        margin: 20px 0;
                      }
                      .password-box {
                        background-color: #fff3cd;
                        border: 2px solid #ffc107;
                        border-radius: 8px;
                        padding: 15px 20px;
                        font-size: 16px;
                        font-weight: bold;
                        margin: 20px 0;
                        word-break: break-all;
                        text-align: center;
                      }
                      .admin-actions {
                        background-color: #f8f9fa;
                        border-radius: 8px;
                        padding: 20px;
                        margin: 20px 0;
                      }
                      .btn {
                        display: inline-block;
                        padding: 15px 25px;
                        background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                        color: white;
                        text-decoration: none;
                        border-radius: 8px;
                        font-weight: bold;
                        margin: 10px 5px;
                        text-align: center;
                      }
                      .footer {
                        text-align: center;
                        font-size: 14px;
                        color: #777;
                        padding: 20px;
                        background-color: #f8f9fa;
                      }
                      .feature-list {
                        list-style: none;
                        padding: 0;
                      }
                      .feature-list li {
                        padding: 8px 0;
                        border-bottom: 1px solid #eee;
                      }
                      .feature-list li:before {
                        content: "✓ ";
                        color: #28a745;
                        font-weight: bold;
                      }
                      .security-notice {
                        background-color: #d1ecf1;
                        border-left: 4px solid #bee5eb;
                        padding: 15px;
                        margin: 20px 0;
                        border-radius: 4px;
                      }
                    </style>
                  </head>
                  <body>
                    <div class="container">
                      <div class="header">
                        <img src="%s" alt="%s Logo" />
                        <h2>Welcome Admin!</h2>
                        <div class="admin-badge">ADMIN ACCESS</div>
                      </div>
                      <div class="banner">
                        <img src="%s" alt="Admin Dashboard Banner" />
                      </div>
                      <div class="content">
                        <div class="welcome-message">
                          <h3>🎉 Hello %s!</h3>
                          <p>Your farm <strong>"%s"</strong> has been created successfully!</p>
                        </div>
                        
                        <p>Your admin credentials:</p>
                        <div class="password-box">
                          <strong>Password:</strong> %s
                        </div>
                        
                        <div class="admin-actions">
                          <h4>🔧 Admin Dashboard Access</h4>
                          <p>Please login to your admin dashboard to:</p>
                          <ul class="feature-list">
                            <li>Create and manage users</li>
                            <li>Configure tenant settings</li>
                            <li>Monitor farm operations</li>
                            <li>Generate reports</li>
                            <li>Manage permissions</li>
                          </ul>
                          
                          <div style="text-align: center; margin: 25px 0;">
                            <a href="%s" class="btn">🚀 Access Admin Dashboard</a>
                          </div>
                        </div>
                        
                        <div class="security-notice">
                          <p><strong>🔒 Security Notice:</strong> Please change your password immediately after your first login for security reasons.</p>
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
                LOGO_URL, COMPANY_BRAND_NAME, // header
                BANNER_URL, // banner
                name, companyName, // welcome message
                generatedPassword, // password
                ADMIN_DASHBOARD_URL, // admin dashboard URL
                COMPANY_BRAND_NAME, // footer company name
                CONTACT_EMAIL, CONTACT_EMAIL // footer email
        );
    }

    /**
     * USER EMAIL TEMPLATE
     */
    private String buildUserHtmlTemplate(String companyName, String generatedPassword, String userRole, String name) {
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
                        padding: 15px 20px;
                        font-size: 16px;
                        font-weight: bold;
                        margin: 20px 0;
                        word-break: break-all;
                        border-radius: 4px;
                      }
                      .btn {
                        display: inline-block;
                        padding: 12px 20px;
                        background-color: #4caf50;
                        color: white;
                        text-decoration: none;
                        border-radius: 5px;
                        font-weight: bold;
                        margin: 10px 5px;
                      }
                      .footer {
                        text-align: center;
                        font-size: 14px;
                        color: #777;
                        padding: 20px;
                      }
                      .app-section {
                        background-color: #f8f9fa;
                        border-radius: 8px;
                        padding: 20px;
                        margin: 20px 0;
                        text-align: center;
                      }
                      .security-notice {
                        background-color: #fff3cd;
                        border-left: 4px solid #ffc107;
                        padding: 15px;
                        margin: 20px 0;
                        border-radius: 4px;
                      }
                    </style>
                  </head>
                  <body>
                    <div class="container">
                      <div class="header">
                        <img src="%s" alt="%s Logo" />
                        <h2>Welcome to %s</h2>
                      </div>
                      <div class="banner">
                        <img src="%s" alt="Welcome Banner" />
                      </div>
                      <div class="content">
                        <p>Hello <strong>%s</strong>,</p>
                        <p>Your account has been created successfully at <strong>%s</strong> with role: <strong>%s</strong></p>
                        <p>This is your generated password:</p>
                        <div class="password-box">%s</div>
                        
                        <div class="security-notice">
                          <p><strong>🔒 For your security:</strong> Please log in and change your password immediately.</p>
                        </div>
                        
                        <div class="app-section">
                          <h3>📱 Access Your Account</h3>
                          <p>Click the button below to access the %s application:</p>
                          <a href="%s" class="btn">🚀 Open Application</a>
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
                LOGO_URL, COMPANY_BRAND_NAME, companyName, // header
                BANNER_URL, // banner
                name, companyName, userRole, // content
                generatedPassword, // password
                companyName, // app section company name
                USER_APP_URL, // user app URL
                COMPANY_BRAND_NAME, // footer company name
                CONTACT_EMAIL, CONTACT_EMAIL // footer email
        );
    }

    /**
     * ADMIN PLAIN TEXT TEMPLATE
     */
    private String buildAdminPlainTextTemplate(String companyName, String generatedPassword, String name) {
        return String.format(
                "Hello %s,\n\n" +
                        "Your farm '%s' has been created successfully!\n\n" +
                        "ADMIN ACCESS CREDENTIALS:\n" +
                        "Password: %s\n\n" +
                        "Admin Dashboard: %s\n\n" +
                        "As an admin, you can:\n" +
                        "- Create and manage users\n" +
                        "- Configure tenant settings\n" +
                        "- Monitor farm operations\n" +
                        "- Generate reports\n" +
                        "- Manage permissions\n\n" +
                        "SECURITY NOTICE: Please change your password immediately after your first login.\n\n" +
                        "Best regards,\n" +
                        "%s Team\n" +
                        "%s",
                name, companyName, generatedPassword, ADMIN_DASHBOARD_URL,
                COMPANY_BRAND_NAME, CONTACT_EMAIL
        );
    }

    /**
     * USER PLAIN TEXT TEMPLATE
     */
    private String buildUserPlainTextTemplate(String companyName, String generatedPassword, String userRole, String name) {
        return String.format(
                "Hello %s,\n\n" +
                        "Your account has been created successfully at %s.\n" +
                        "Role: %s\n\n" +
                        "Your generated password: %s\n\n" +
                        "Application URL: %s\n\n" +
                        "Please login and change your password immediately for security reasons.\n\n" +
                        "Best regards,\n" +
                        "%s Team\n" +
                        "%s",
                name, companyName, userRole, generatedPassword, USER_APP_URL,
                COMPANY_BRAND_NAME, CONTACT_EMAIL
        );
    }
}