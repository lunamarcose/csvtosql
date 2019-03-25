package notification;

import java.io.IOException;
import java.util.Properties;

import javax.mail.*;
import javax.mail.internet.*;

public class MailSender {
public void systemSender(InternetAddress recepients, String subject, String body) throws IOException, AddressException, MessagingException {

        Properties properties = new Properties();
        Session session = Session.getDefaultInstance(properties , null);

        Message msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress("notificaciones@aserapp.biz", "Notificaciones - Asserapp"));
        msg.addRecipient(Message.RecipientType.TO, recepients);
        msg.setSubject(subject);
        msg.setText(body);
        Transport.send(msg);
    }
}