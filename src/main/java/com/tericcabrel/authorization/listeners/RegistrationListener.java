package com.tericcabrel.authorization.listeners;

import com.tericcabrel.authorization.events.OnRegistrationCompleteEvent;
import com.tericcabrel.authorization.models.entities.User;
import com.tericcabrel.authorization.services.interfaces.UserAccountService;
import org.springframework.context.ApplicationListener;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.util.UUID;

@Component
public class RegistrationListener implements ApplicationListener<OnRegistrationCompleteEvent> {

    private static final String TEMPLATE_NAME = "html/registration";
    private static final String SPRING_LOGO_IMAGE_PATH = "templates/html/images/spring.png";
    private static final String IMAGE_MIME_TYPE = "image/png";
    private static final String EMAIL_SUBJECT = "Registration Confirmation";

    private final Environment environment;
    private final UserAccountService userAccountService;
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    public RegistrationListener(UserAccountService userAccountService, JavaMailSender mailSender,
                                Environment environment, TemplateEngine templateEngine) {
        this.userAccountService = userAccountService;
        this.mailSender = mailSender;
        this.environment = environment;
        this.templateEngine = templateEngine;
    }

    @Override
    public void onApplicationEvent(OnRegistrationCompleteEvent event) {
        sendConfirmationEmail(event);
    }

    private void sendConfirmationEmail(OnRegistrationCompleteEvent event) {
        User user = event.getUser();
        String token = UUID.randomUUID().toString();
        userAccountService.save(user, token);

        String confirmationUrl = generateConfirmationUrl(token);
        String mailFrom = getMailFrom();
        String mailFromName = getMailFromName();

        if (mailFrom == null || mailFrom.isBlank()) {
            // Log or throw an error if mail configuration is missing
            System.err.println("Email sender address is not configured!");
            return;
        }

        try {
            MimeMessage message = createMimeMessage(user, confirmationUrl, mailFrom, mailFromName);
            mailSender.send(message);
        } catch (MessagingException | UnsupportedEncodingException e) {
            // Log the error for debugging purposes
            System.err.println("Failed to send email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String generateConfirmationUrl(String token) {
        return environment.getProperty("app.url.confirm-account", "http://localhost:8080/confirm-account") + "?token=" + token;
    }

    private String getMailFrom() {
        return environment.getProperty("spring.mail.properties.mail.smtp.from");
    }

    private String getMailFromName() {
        return environment.getProperty("mail.from.name", "Identity");
    }

    private MimeMessage createMimeMessage(User user, String confirmationUrl, String mailFrom, String mailFromName)
            throws MessagingException, UnsupportedEncodingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper emailHelper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        emailHelper.setTo(user.getEmail());
        emailHelper.setSubject(EMAIL_SUBJECT);
        emailHelper.setFrom(new InternetAddress(mailFrom, mailFromName));

        Context context = new Context(LocaleContextHolder.getLocale());
        context.setVariable("email", user.getEmail());
        context.setVariable("name", user.getFirstName() + " " + user.getLastName());
        context.setVariable("springLogo", SPRING_LOGO_IMAGE_PATH);
        context.setVariable("url", confirmationUrl);

        String htmlContent = templateEngine.process(TEMPLATE_NAME, context);
        emailHelper.setText(htmlContent, true);

        ClassPathResource logoResource = new ClassPathResource(SPRING_LOGO_IMAGE_PATH);
        if (logoResource.exists()) {
            emailHelper.addInline("springLogo", logoResource, IMAGE_MIME_TYPE);
        } else {
            System.err.println("Spring logo image not found at " + SPRING_LOGO_IMAGE_PATH);
        }

        return mimeMessage;
    }
}
