package csvtosql;

import java.util.ArrayList;
import javax.mail.internet.InternetAddress;
import notification.MailSender;

public class Email {
    
    private boolean enabled;
    private InternetAddress addresses;
    private String email_subjet;
    private MailSender sender;
    private ArrayList<String> email_content = new ArrayList();

    public MailSender getSender() {
        return sender;
    }

    public void setSender(MailSender sender) {
        this.sender = sender;
    }

    public InternetAddress getAddresses() {
        return addresses;
    }

    public void setAddresses(InternetAddress addresses) {
        this.addresses = addresses;
    }

    public String getEmail_subjet() {
        return email_subjet;
    }

    public void setEmail_subjet(String email_subjet) {
        if(this.isEnabled()){
            this.email_subjet += " - " + email_subjet;
        }
    }

    public String getEmail_content() {
        String contenido = "";
        for (int i = 0; i < this.email_content.size(); i++) {
            contenido += this.email_content.get(i);
        }
        return contenido;
    }

    public void setEmail_content(String email_content) {
        if(this.isEnabled()){
            this.email_content.add(email_content);   
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Email(boolean enabled, String[] addresses, String subject) {
        this.enabled = enabled;
        if(!"".equals(subject)){
            this.email_subjet = subject;
        }
        if(addresses.length > 0){
            if(!"".equals(addresses[0])){
                this.addresses = new InternetAddress();
                for (int i = 0; i < addresses.length; i++) {
                    this.addresses.setAddress(addresses[i]);
                }
            }
        }
    }
}
