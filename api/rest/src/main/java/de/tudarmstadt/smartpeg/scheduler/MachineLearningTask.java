package de.tudarmstadt.smartpeg.scheduler;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import static de.tudarmstadt.smartpeg.data.DataSourceProvider.getDataSource;
import static de.tudarmstadt.smartpeg.ml.MLVectorExtractor.createInputVector;

/**
 * The machine learning task is run periodically to transfer the measurements to the predictions table.
 * This is done using the model that has been trained before.
 * @author frank
 *
 */
public class MachineLearningTask implements Runnable {

    private static Logger logger = Logger.getLogger(MachineLearningTask.class.getName());

    /** Select each peg and the timestamp for its last measurement */ 
    private static final String STMT_SEL_ALL_PEGS = 
    		"SELECT p.id as peg_id, ctm.timestamp as currentTimestamp, time_to_sec(timediff(ctm.timestamp,now())) as nowOffset "
    		+ "FROM peg p "
    		+ "LEFT JOIN measurement ctm "
    		+ "ON (ctm.peg_id=p.id and ctm.nr = (SELECT max(sq.nr) FROM measurement sq WHERE sq.peg_id=p.id)) "
    		+ "ORDER BY p.id ASC";
    
    /** Statement to get measurements for a specific peg to be able to find the start of the current process */
    private static final String STMT_SEL_FINDSTART =
    		"SELECT m.timestamp FROM measurement m WHERE peg_id = ? ORDER BY m.timestamp DESC";
    
    /** Statement to update the predictuion */
    private static final String STMT_UPD_PREDICTION = 
    		"UPDATE peg SET prediction = ? WHERE id = ?";
    
    @Override
	public void run() {
	    logger.info("Running scheduled MachineLearningTask now");
		
		try {
			DataSource smartpegDataSource = getDataSource();
			String pythonServerBase = getPythonServerBase();
			
			try (Connection con = smartpegDataSource.getConnection()) {
							

				// Iterate over all pegs
				try (
					PreparedStatement stmtPegIds = con.prepareStatement(STMT_SEL_ALL_PEGS);
					PreparedStatement stmtFindStart = con.prepareStatement(STMT_SEL_FINDSTART);
					PreparedStatement stmtUpdatePrediction = con.prepareStatement(STMT_UPD_PREDICTION);
					ResultSet rsPegs = stmtPegIds.executeQuery()
				) {
					while(rsPegs.next()) {
						int pegID = rsPegs.getInt(1);
				    	logger.log(Level.INFO, "Updating prediction for peg #" + pegID);
						Timestamp lastMeasurementTs = rsPegs.getTimestamp(2);
						
						float prediction = 0.0f;
						
						// Only update the prediction if we have measurements
						if (lastMeasurementTs != null) {
							// Value is only valid if we have a timestamp, so read it now
							int nowOffset = rsPegs.getInt(3);
							
							// Find the start of the current drying period by looking back in the data
							logger.log(Level.INFO, "Determining start of current drying process for peg " + pegID);
							Timestamp tsStart = lastMeasurementTs;
							stmtFindStart.setInt(1, pegID);
							try (ResultSet rsMeasurement = stmtFindStart.executeQuery()) {
								while(rsMeasurement.next()) {
									Timestamp tsCurrent = rsMeasurement.getTimestamp(1);
									long difference = tsStart.getTime() - tsCurrent.getTime();
									// If the difference is bigger than 15 minutes, we found the start
									if (difference > 15*60*1000) {
										break;
									} else {
										// If not, we use the new timestamp as reference
										tsStart = tsCurrent;
									}
								}
							}
							
							// Now, we can create an input vector for the python script
							// Use MLVectorExtractor to convert the current measurement into an input vector for the
							// machine learning system.
							// The helper class is also used by the training data generator, so the process is the same.
							String inputVector = createInputVector(con, pegID, "HDC1080", lastMeasurementTs, tsStart);

							logger.log(Level.INFO, "Calling external process to predict remaining duration for peg " + pegID);
							// Call the external process (Python + TensorFlow)
							Process process = new ProcessBuilder(
								pythonServerBase + "/predict.sh",
								inputVector
							).start();
							BufferedReader predictionOutput = new BufferedReader(new InputStreamReader(process.getInputStream()));

							// Parse the output. The shellscript creates a line PREDICTION=... if successful
							String line = "";
							logger.log(Level.INFO, "+--- Output of predict.sh for peg " + pegID + ":");
							while((line = predictionOutput.readLine())!=null) {
								logger.log(Level.INFO, "| " + line);
								if (line.startsWith("PREDICTION=")) {
									try {
										prediction = Float.parseFloat(line.substring("PREDICTION=".length()));
									} catch (NumberFormatException ex) {
										logger.log(Level.WARNING, "Found a PREDICTION=... line, but could not read prediction", ex);
									}
								}
							}
							logger.log(Level.INFO, "+--- End of predict.sh");
							
							// Finally, we need to relate the prediction to the difference between lastMeasurementTs and now,
							// As there might be some time in between. Calculation has been done in the query
							if (prediction > 0.0f) {
								prediction = Math.max(0.0f, prediction + nowOffset);
							}
						}
						
						// Write the prediction to the database
						stmtUpdatePrediction.setFloat(1, prediction);
						stmtUpdatePrediction.setInt(2, pegID);
						stmtUpdatePrediction.executeUpdate();

				    	logger.log(Level.INFO, "Prediction for peg #" + pegID + " successfully updated");
					}
				}
				
			} catch (SQLException ex) {
				logger.log(Level.SEVERE, "SQL Error during scheduled machine learning task", ex);
			}
			
		} catch (NamingException ex) {
			logger.log(Level.SEVERE, "Some JNDI-Configuration is missing (Data Source, Python base, ...)", ex);
		} catch (Exception ex) {
			logger.log(Level.SEVERE, "Unexpected error during machine learning task", ex);
		}
		
		
	}

    /**
     * Uses a JNDI Lookup to find the /server directory from the repository, which contains the pyhton scripts.
     * 
     * Configuration has to be made in the server config, check test-app-context.xml
     * 
     * @return The JNDI-Entry configured as java:/comp/env/pythonServerBase
     * @throws NamingException If the entry is not configured
     */
    private static String getPythonServerBase() throws NamingException {
    	InitialContext initialContext = new InitialContext();
    	Context environmentContext = (Context) initialContext.lookup("java:/comp/env");
    	return (String)environmentContext.lookup("pythonServerBase");
    }
    
}
