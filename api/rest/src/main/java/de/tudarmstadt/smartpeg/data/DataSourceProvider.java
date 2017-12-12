package de.tudarmstadt.smartpeg.data;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * Helper class to provide static functionality to access data sources etc. that are configured in the server.
 * The implementation makes use of JNDI lookups.
 * 
 * @author frank
 *
 */
public class DataSourceProvider {
	
	/**
	 * Private constructor, this class should not be instantiated
	 */
	private DataSourceProvider() {
		
	}
	
	/**
     * Helper-Function to get a JNDI-Datasource, as the Resource-Annotation does not work properly
     * @return The Datasource
     * @throws NamingException If the datasource could not be retrieved, most probably because it's not configured
     */
    public static DataSource getDataSource() throws NamingException {
    	// Get JNDI-Context
        Context initCtx = new InitialContext();
        // Navigate to comp/env
        Context envCtx = (Context) initCtx.lookup("java:comp/env");
        // Get the datasource
        return (DataSource)envCtx.lookup("jdbc/smartpeg");
    }
}
