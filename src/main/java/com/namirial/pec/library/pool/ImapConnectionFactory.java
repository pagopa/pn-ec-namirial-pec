package com.namirial.pec.library.pool;

import java.util.Properties;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import com.namirial.pec.library.conf.Configuration;

import it.pagopa.pn.library.exceptions.PnSpapiTemporaryErrorException;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;

public class ImapConnectionFactory extends BasePooledObjectFactory<Store> {
	
    @Override
    public Store create() {
    	
		String host = Configuration.getImapHost();
		String port = Configuration.getImapPort();
		String username = Configuration.getPecUsername();
		String password = Configuration.getPecPassword();
		
        Properties properties = new Properties();
		properties.put("mail.imaps.auth", "true");
		properties.put("mail.imaps.starttls.enable", "true");
		properties.put("mail.imaps.ssl.protocols","TLSv1.2");
		
        Session session = Session.getDefaultInstance(properties);
 
        Store store = null;
		try {
			store = session.getStore("imaps");
			store.connect(host, Integer.valueOf(port), username, password);
		} catch (MessagingException e) {
            throw new PnSpapiTemporaryErrorException("create: " + e.getClass() + " " + e.getMessage());
		}
        
        return store;
    }

    @Override
    public PooledObject<Store> wrap(Store store) {
        return new DefaultPooledObject<>(store);
    }
    
    @Override
    public void destroyObject(PooledObject<Store> p) throws Exception {
        p.getObject().close();
    }

    @Override
    public boolean validateObject(PooledObject<Store> p) {
        Store store = p.getObject();
        try {
            return store.isConnected() && store.getDefaultFolder() != null;
        } catch (MessagingException e) {
            return false;
        }
    }
}