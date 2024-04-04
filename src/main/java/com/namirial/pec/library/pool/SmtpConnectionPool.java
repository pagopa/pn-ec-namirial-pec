package com.namirial.pec.library.pool;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import com.namirial.pec.library.conf.Configuration;

import it.pagopa.pn.library.exceptions.PnSpapiTemporaryErrorException;
import jakarta.mail.Transport;

public class SmtpConnectionPool {
	
	private static GenericObjectPool<Transport> pool = null;
	private static SmtpConnectionPool single_instance = null;
	
    private SmtpConnectionPool() { 
    	
    } 
    
    public static SmtpConnectionPool getInstance() {
    	synchronized (SmtpConnectionPool.class) {
    		if (single_instance == null)
    			single_instance = new SmtpConnectionPool(); 
    		return single_instance; 
    	}
    }
	
	private static void createGenericPool() {
		synchronized (SmtpConnectionPool.class) {
			if (pool == null) {
				GenericObjectPoolConfig<Transport> config = new GenericObjectPoolConfig<>();
				
				config.setMaxTotal(Integer.valueOf(Configuration.getPoolSmtpMaxTotal()));
		        config.setMaxIdle(Integer.valueOf(Configuration.getPoolSmtpMaxIdle()));
		        config.setMinIdle(Integer.valueOf(Configuration.getPoolSmtpMinIdle()));
		        
		        pool = new GenericObjectPool<Transport>(new SmtpConnectionFactory(), config);
		        pool.setTestOnBorrow(true);
		        pool.setTestOnCreate(true);
			}
		}
	}
	
	public Transport getSmtpConnection() {
		
		try {
			if (pool == null)
				createGenericPool();
			
			return pool.borrowObject();
		} catch (Exception e) {
            throw new PnSpapiTemporaryErrorException ("getSmtpConnection: " + e.getClass() + " " + e.getMessage());
		}
	}
	
	public void releaseSmtpConnection(Transport transport) {
		pool.returnObject(transport);
	}
}
