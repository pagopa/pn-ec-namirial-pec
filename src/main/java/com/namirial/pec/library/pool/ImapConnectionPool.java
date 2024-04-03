package com.namirial.pec.library.pool;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import com.namirial.pec.library.conf.Configuration;

import it.pagopa.pn.library.exceptions.PnSpapiTemporaryErrorException;
import jakarta.mail.Store;

public class ImapConnectionPool {
	
	private static GenericObjectPool<Store> pool = null;
	private static ImapConnectionPool single_instance = null;
	
    private ImapConnectionPool() { 
    	
    } 
    
    public static ImapConnectionPool getInstance() {
    	synchronized (ImapConnectionPool.class) {
    		if (single_instance == null)
    			single_instance = new ImapConnectionPool(); 
    		return single_instance; 
    	}
    }
	
	private static void createGenericPool() {
		synchronized (ImapConnectionPool.class) {
			if (pool == null) {
				GenericObjectPoolConfig<Store> config = new GenericObjectPoolConfig<>();
				
				config.setMaxTotal(Integer.valueOf(Configuration.getPoolImapMaxTotal()));
		        config.setMaxIdle(Integer.valueOf(Configuration.getPoolImapMaxIdle()));
		        config.setMinIdle(Integer.valueOf(Configuration.getPoolImapMinIdle()));
		        
		        pool = new GenericObjectPool<Store>(new ImapConnectionFactory(), config);
			}
		}
	}
	
	public Store getImapConnection() {
		
		try {
			if (pool == null)
				createGenericPool();
			
			return pool.borrowObject();
		} catch (Exception e) {
            throw new PnSpapiTemporaryErrorException ("getImapConnection: " + e.getClass() + " " + e.getMessage());
		}
	}
	
	public void releaseImapConnection(Store store) {
		pool.returnObject(store);
	}
}
