package com.bank.frauddetection.service;

import com.bank.frauddetection.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final JavaMailSender mailSender;
    private final boolean mailEnabled;
    private final String fromAddress;

    public EmailNotificationService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${app.mail.enabled:false}") boolean mailEnabled,
            @Value("${app.mail.from:no-reply@securebank.local}") String fromAddress
    ) {
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.mailEnabled = mailEnabled;
        this.fromAddress = fromAddress;
    }

    public boolean send(User recipient, String subject, String body) {
        if (recipient == null || recipient.getEmail() == null || recipient.getEmail().isBlank()) {
            log.warn("EMAIL_SKIPPED reason=missing_recipient subject={}", subject);
            return false;
        }

        if (!mailEnabled || mailSender == null) {
            log.info(
                    "EMAIL_SIMULATED to={} subject=\"{}\" body=\"{}\"",
                    recipient.getEmail(),
                    subject,
                    body.replace('\n', ' ')
            );
            return false;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(recipient.getEmail());
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("EMAIL_SENT to={} subject=\"{}\"", recipient.getEmail(), subject);
            return true;
        } catch (MailException ex) {
            log.warn("EMAIL_SEND_FAILED to={} subject=\"{}\" reason={}", recipient.getEmail(), subject, ex.getMessage());
            return false;
        }
    }
}
