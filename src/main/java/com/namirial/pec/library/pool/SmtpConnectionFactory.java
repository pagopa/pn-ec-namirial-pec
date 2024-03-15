package com.namirial.pec.library.pool;

import java.util.Properties;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import com.namirial.pec.library.conf.Configuration;

import it.pagopa.pn.library.exceptions.PnSpapiTemporaryErrorException;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;

public class SmtpConnectionFactory extends BasePooledObjectFactory<Transport> {
	
    @Override
    public Transport create() {
    	
    	String host = Configuration.getSmtpHost();
    	String port = Configuration.getSmtpPort();
		String username = Configuration.getPecUsername();
    	String password = Configuration.getPecPassword();
    	
    	Properties props = new Properties();
        
        props.put("mail.transport.protocol", "smtps");
        props.put("mail.smtps.auth", "true");
        props.put("mail.smtps.starttls.enable","true");
        props.put("mail.smtps.ssl.protocols","TLSv1.2");
        
        Session session = Session.getDefaultInstance(props);        

        Transport transport;
		try {
			transport = session.getTransport("smtps");
			transport.connect(host, Integer.valueOf(port), username, password);
		} catch (MessagingException e) {
	        throw new PnSpapiTemporaryErrorException(e.getMessage());
		}
        
        return transport;
    }

    @Override
    public PooledObject<Transport> wrap(Transport transport) {
        return new DefaultPooledObject<>(transport);
    }
}