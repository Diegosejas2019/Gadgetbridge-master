package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Security;
import java.util.Properties;
import javax.activation.DataHandler;

import javax.activation.DataSource;

import javax.activation.FileDataSource;

import javax.mail.BodyPart;

import javax.mail.Message;

import javax.mail.Multipart;

import javax.mail.PasswordAuthentication;

import javax.mail.Session;

import javax.mail.Transport;

import javax.mail.internet.InternetAddress;

import javax.mail.internet.MimeBodyPart;

import javax.mail.internet.MimeMessage;

import javax.mail.internet.MimeMultipart;

import java.io.ByteArrayInputStream;

import java.io.IOException;

import java.io.InputStream;

import java.io.OutputStream;

import java.security.Security;

import java.util.Properties;

public class GMailSender extends javax.mail.Authenticator {

    private String mailhost = "smtp.gmail.com";

    private String user;

    private String password;

    private Session session;



    private Multipart _multipart = new MimeMultipart();

    static {

        Security.addProvider(new nodomain.freeyourgadget.gadgetbridge.activities.charts.JSSEProvider());

    }



    public GMailSender(String user, String password) {

        this.user = user;

        this.password = password;



        Properties props = new Properties();

        props.setProperty("mail.transport.protocol", "smtp");

        props.setProperty("mail.host", mailhost);

        props.put("mail.smtp.auth", "true");

        props.put("mail.smtp.port", "465");

        props.put("mail.smtp.socketFactory.port", "465");

        props.put("mail.smtp.socketFactory.class",

                "javax.net.ssl.SSLSocketFactory");

        props.put("mail.smtp.socketFactory.fallback", "false");

        props.setProperty("mail.smtp.quitwait", "false");



        session = Session.getDefaultInstance(props, this);

    }



    protected PasswordAuthentication getPasswordAuthentication() {

        return new PasswordAuthentication(user, password);

    }



    public synchronized void sendMail(String subject, String body,

                                      String sender, String recipients) throws Exception {

        try {

            MimeMessage message = new MimeMessage(session);

            DataHandler handler = new DataHandler(new ByteArrayDataSource(

                    body.getBytes(), "text/html"));

            message.setSender(new InternetAddress(sender));

            message.setSubject(subject);

            message.setDataHandler(handler);

            BodyPart messageBodyPart = new MimeBodyPart();

            messageBodyPart.setText(body);

            _multipart.addBodyPart(messageBodyPart);



            // Put parts in message

            message.setContent(_multipart);

            if (recipients.indexOf(',') > 0)

                message.setRecipients(Message.RecipientType.TO,

                        InternetAddress.parse(recipients));

            else

                message.setRecipient(Message.RecipientType.TO,

                        new InternetAddress(recipients));

            Transport.send(message);

        } catch (Exception e) {



        }

    }



    public void addAttachment(String filename, String filename2, String filename3) throws Exception {

        if (filename != ""){

            BodyPart messageBodyPart = new MimeBodyPart();
            DataSource source = new FileDataSource(filename);
            messageBodyPart.setDataHandler(new DataHandler(source));
            messageBodyPart.setFileName("Imagen Adjunta");
            _multipart.addBodyPart(messageBodyPart);
        }
        if (filename2 != ""){
            BodyPart messageBodyPart2 = new MimeBodyPart();
            DataSource source2 = new FileDataSource(filename2);
            messageBodyPart2.setDataHandler(new DataHandler(source2));
            messageBodyPart2.setFileName("Imagen Adjunta2");
            _multipart.addBodyPart(messageBodyPart2);
        }
        if (filename3 != ""){
            BodyPart messageBodyPart3 = new MimeBodyPart();
            DataSource source3 = new FileDataSource(filename3);
            messageBodyPart3.setDataHandler(new DataHandler(source3));
            messageBodyPart3.setFileName("Imagen Adjunta3");
            _multipart.addBodyPart(messageBodyPart3);
        }
    }



    public class ByteArrayDataSource implements DataSource {

        private byte[] data;

        private String type;





        public ByteArrayDataSource(byte[] data, String type) {

            super();

            this.data = data;

            this.type = type;

        }



        public ByteArrayDataSource(byte[] data) {

            super();

            this.data = data;

        }





        public void setType(String type) {

            this.type = type;

        }



        public String getContentType() {

            if (type == null)

                return "application/octet-stream";

            else

                return type;

        }



        public InputStream getInputStream() throws IOException {

            return new ByteArrayInputStream(data);

        }



        public String getName() {

            return "ByteArrayDataSource";

        }



        public OutputStream getOutputStream() throws IOException {

            throw new IOException("Not Supported");

        }

    }

}