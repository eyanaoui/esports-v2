package com.esports.services;

import com.esports.models.Order;
import jakarta.activation.DataHandler;
import jakarta.activation.FileDataSource;
import jakarta.mail.Authenticator;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import java.io.File;
import java.util.Properties;

public class EmailService {

    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";

    /*
     * TEMPORARY FIX FOR DEMO
     * Put your Gmail address and Gmail App Password here.
     * Do NOT use your normal Gmail password.
     * Do NOT push this file to GitHub with the real password.
     */
    private static final String LOCAL_MAIL_USERNAME = "seif.amri31@gmail.com";
    private static final String LOCAL_MAIL_PASSWORD = "jtki tqef loha gkdx";

    private String username;
    private String password;

    public EmailService() {
        this.username = readConfig("ESPORTS_MAIL_USERNAME");
        this.password = readConfig("ESPORTS_MAIL_PASSWORD");

        if (this.username == null || this.username.trim().isEmpty()) {
            this.username = LOCAL_MAIL_USERNAME;
        }

        if (this.password == null || this.password.trim().isEmpty()) {
            this.password = LOCAL_MAIL_PASSWORD;
        }

        if (this.username != null) {
            this.username = this.username.trim();
        }

        if (this.password != null) {
            this.password = this.password.trim().replace(" ", "");
        }

        System.out.println("========== EMAIL CONFIG DEBUG ==========");
        System.out.println("MAIL USER = " + username);
        System.out.println("MAIL PASSWORD EXISTS = " + (password != null && !password.isEmpty()));
        System.out.println("========================================");
    }

    private String readConfig(String key) {
        String value = System.getenv(key);

        if (value == null || value.trim().isEmpty()) {
            value = System.getProperty(key);
        }

        return value;
    }

    public boolean isConfigured() {
        return username != null && !username.trim().isEmpty()
                && password != null && !password.trim().isEmpty()
                && !password.equals("PUT_YOUR_GMAIL_APP_PASSWORD_HERE");
    }

    public boolean sendInvoice(Order order, File invoiceFile) {
        if (!isConfigured()) {
            System.out.println("❌ Email service not configured.");
            System.out.println("❌ Replace PUT_YOUR_GMAIL_APP_PASSWORD_HERE with your Gmail App Password.");
            return false;
        }

        if (order == null) {
            System.out.println("❌ Order is null.");
            return false;
        }

        if (order.getCustomerEmail() == null || order.getCustomerEmail().trim().isEmpty()) {
            System.out.println("❌ Customer email is missing.");
            return false;
        }

        if (invoiceFile == null || !invoiceFile.exists()) {
            System.out.println("❌ Invoice file not found.");
            return false;
        }

        try {
            Session session = createSession();

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(order.getCustomerEmail())
            );

            message.setSubject("Your E-Sports Store Invoice - " + safe(order.getReference()));

            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setText(
                    "Hello " + safe(order.getCustomerFirstName()) + ",\n\n"
                            + "Thank you for your order.\n"
                            + "Your order reference is: " + safe(order.getReference()) + "\n"
                            + "Total amount: " + String.format("%.2f", order.getTotalAmount()) + "\n\n"
                            + "Your invoice is attached to this email.\n\n"
                            + "Best regards,\n"
                            + "E-Sports Store"
            );

            MimeBodyPart attachmentPart = new MimeBodyPart();
            FileDataSource source = new FileDataSource(invoiceFile);
            attachmentPart.setDataHandler(new DataHandler(source));
            attachmentPart.setFileName(invoiceFile.getName());

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(textPart);
            multipart.addBodyPart(attachmentPart);

            message.setContent(multipart);

            System.out.println("📧 Trying to send email to: " + order.getCustomerEmail());

            Transport.send(message);

            System.out.println("✅ Invoice email sent successfully to " + order.getCustomerEmail());
            return true;

        } catch (AuthenticationFailedException e) {
            System.out.println("❌ Gmail authentication failed.");
            System.out.println("❌ Use a Gmail App Password, not your normal Gmail password.");
            System.out.println("❌ Full error: " + e.getMessage());
            e.printStackTrace();
            return false;

        } catch (MessagingException e) {
            System.out.println("❌ SMTP/Messaging error while sending email.");
            System.out.println("❌ Full error: " + e.getMessage());
            e.printStackTrace();
            return false;

        } catch (Exception e) {
            System.out.println("❌ Unknown error while sending email.");
            System.out.println("❌ Full error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private Session createSession() {
        Properties props = new Properties();

        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.ssl.trust", SMTP_HOST);

        return Session.getInstance(
                props,
                new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                }
        );
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}