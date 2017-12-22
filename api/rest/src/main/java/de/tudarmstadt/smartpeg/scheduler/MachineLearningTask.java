package de.tudarmstadt.smartpeg.scheduler;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.NamingException;
import javax.sql.DataSource;

import static de.tudarmstadt.smartpeg.data.DataSourceProvider.getDataSource;

/**
 * The machine learning task is run periodically to transfer the measurements to the predictions table.
 * This is done using the model that has been trained before.
 * @author frank
 *
 */
public class MachineLearningTask implements Runnable {

    private static Logger logger = Logger.getLogger(MachineLearningTask.class.getName());
	
    @Override
	public void run() {
	    logger.info("Running scheduled MachineLearningTask now");
		
		try {
			DataSource smartpegDataSource = getDataSource();
		
			try (Connection con = smartpegDataSource.getConnection()) {
				
				// TODO: Machine learning goes here.
				// You do not need to close the connection explicitly, as it is contained in a
				// try-with-resources block
				// See https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html
				// for further information.
				// This can be used for PreparedStatements as well :)
				
				// Use MLVectorExtractor to convert the current measurement into an input vector for the
				// machine learning system.
				// The helper class is also used by the training data generator, so the process is the same.
				
				logger.warning("NOT IMPLEMENTED: MachineLearningTask scheduled, but not implemented yet.");
				
			} catch (SQLException ex) {
				logger.log(Level.SEVERE, "SQL Error during scheduled machine learning task", ex);
			}
			
		} catch (NamingException ex) {
			logger.log(Level.SEVERE, "Cannot get DataSource for scheduled machine learning task", ex);
		} catch (Exception ex) {
			logger.log(Level.SEVERE, "Unexpected error during machine learning task", ex);
		}
		
		
	}

}
