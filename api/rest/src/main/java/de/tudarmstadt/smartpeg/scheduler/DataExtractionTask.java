package de.tudarmstadt.smartpeg.scheduler;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.NamingException;
import javax.sql.DataSource;

import static de.tudarmstadt.smartpeg.data.DataSourceProvider.getDataSource;

import static de.tudarmstadt.smartpeg.ml.MLVectorExtractor.createOutputVector;
import static de.tudarmstadt.smartpeg.ml.MLVectorExtractor.createInputVector;

/**
 * The DataExtractionTask is run periodically to generate training samples from the measurements table.
 * 
 * @author frank
 *
 */
public class DataExtractionTask implements Runnable {

    private static Logger logger = Logger.getLogger(DataExtractionTask.class.getName());
	    
    /**
     * Statement to select all peg IDs
     * (we use the measurement relation to assure they have already at least one value -
     * this makes creating the first period much easier)
     */
    private static final String STMT_SEL_ALL_PEGS = 
    		"SELECT DISTINCT peg_id FROM measurement ORDER BY peg_id ASC";
    
    /** Statement to check if there are values not covered by a drying period */
    private static final String STMT_SEL_MEASUREMENTS_AFTER_TS =
    		"SELECT count(peg_id) FROM measurement WHERE peg_id = ? and timestamp > ?";

    /** Find the max existing period id */
    private static final String STMT_SEL_MAX_DRYING_PERIOD =
    		"SELECT p.period_id, p.ts_start, p.ts_end FROM drying_period p WHERE p.peg_id = ? and p.period_id=" +
    		"(SELECT max(pmax.period_id) FROM drying_period pmax WHERE pmax.peg_id=p.peg_id)";
    
    /** Statement to create the first drying period */
    private static final String STMT_INS_CREATE_FIRST_PERIOD = 
    		"INSERT INTO drying_period(peg_id, period_id, ts_start, ts_end, ts_dry) " +
    		"VALUES(?, 1, (SELECT MIN(m.timestamp) FROM measurement m WHERE m.peg_id=?), NULL, NULL)";

    /** Statement to create a new drying period */
    private static final String STMT_INS_CREATE_PERIOD = 
    		"INSERT INTO drying_period(peg_id, period_id, ts_start, ts_end, ts_dry) " +
    		"VALUES(?, ?, (SELECT MIN(m.timestamp) FROM measurement m WHERE m.peg_id=? and m.timestamp>?), NULL, NULL)";
    
    /** Statement to find measurements for a new period */
    private static final String STMT_SEL_MEASUREMENTS_IN_PERIOD = 
    		"SELECT m.timestamp, m.conductance FROM measurement m WHERE m.peg_id = ? and m.timestamp >= ? ORDER BY m.timestamp ASC";
    
    /** Get DB server time */
    private static final String STMT_SEL_SERVERTIME = "SELECT NOW()";
    
    /** Update end and tsDry on a period */
    private static final String STMT_UPD_PERIOD_END = 
    		"UPDATE drying_period SET ts_dry=?, ts_end=? WHERE peg_id=? and period_id=? LIMIT 1";
    
    /** Select timestamps of a drying period */
    private static final String STMT_SELECT_PERIOD_DATA =
    		"SELECT ts_start, ts_end, ts_dry FROM drying_period WHERE peg_id=? and period_id=?";
    
    /** Select measurements within a period that do not have a train_data entry */
    private static final String STMT_SELECT_MEASUREMENTS_FOR_TRAIN_DATA_CREATION =
    		"SELECT m.nr, m.sensor_type, m.timestamp FROM measurement m " +
			"WHERE m.peg_id = ? and m.timestamp >= ? and m.timestamp <= ? " +
    		"and (SELECT count(mt.peg_id) FROM measurement_train mt WHERE mt.peg_id=m.peg_id and mt.nr=m.nr)=0 " +
			"ORDER BY m.timestamp ASC";
    
    /** Insert statement for the training data sample table */
    private static final String STMT_INS_MEASUREMENT_TRAIN = 
    		"INSERT INTO measurement_train(peg_id, nr, sensor_type, vec_data_in, vec_data_out) VALUES(?,?,?,?,?)";
        
    /** Amount of values that are used to calculate the moving average over conductance to estimate tsDry */
    private static final int MOVING_AVG_SIZE_CONDUCTANCE = 50;
    
    /** Threshold for the conductance value to be considered as dry */
    private static final float MOVING_AVG_THRESHOLD_CONDUCTANCE = 1.0f;
    
    /** Threshold (in seconds) of gap between two measurement periods */
    private static final long PERIOD_THRESHOLD = 15*60;
    
    @Override
	public void run() {
	    logger.info("Running scheduled DataExtractionTask now -> Updating training data");
		
	    // Lookup the data source
		try {
			DataSource smartpegDataSource = getDataSource();
		
			// Create a connection
			try (Connection con = smartpegDataSource.getConnection()) {
				
				// Iterate over all pegs
				try (
					PreparedStatement stmtPegIds = con.prepareStatement(STMT_SEL_ALL_PEGS);
					ResultSet rsPegIds = stmtPegIds.executeQuery()
				) {						
					// Iterate over all pegIDs
					while(rsPegIds.next()) {
						// Get pegID from the result set
						int pegId = rsPegIds.getInt(1);
						logger.log(Level.INFO, "Updating training data for peg #" + pegId);
						
						// Frist, we update the drying periods and get a set with updated IDs.
						Set<Integer> newPeriodIDs = updateDryingPeriods(con, pegId);
						
						if (!newPeriodIDs.isEmpty()) {
							// If there are new periods
							for(Integer periodId : newPeriodIDs) {
								// Create training data for them
								createTrainingData(con, pegId, periodId);
							}
						} else {
							logger.log(Level.INFO, "No new drying periods for peg #" + pegId +
									" -> No new training data for this peg.");
						}
					}
					
				}
								
			} catch (SQLException ex) {
				logger.log(Level.SEVERE, "SQL Error during scheduled DataExtractionTask", ex);
			}
			
		} catch (NamingException ex) {
			logger.log(Level.SEVERE, "Cannot get DataSource for scheduled DataExtractionTask", ex);
		} catch (Exception ex) {
			logger.log(Level.SEVERE, "Unexpected error during DataExtractionTask", ex);
		}
		
	}

    /**
     * This task updates the drying periods, by creating new periods if we have new
     * data samples, or by closing a period that has ended.
     * 
     * Only closed periods are considered for creation of training data.
     * 
     * @param con The database connection
     * @param pegId The peg to consider
     */
    private Set<Integer> updateDryingPeriods(Connection con, int pegId) throws SQLException {
    	logger.log(Level.INFO, "Updating drying periods for peg #" + pegId);
    	Set<Integer> newPeriodIDs = new HashSet<>();
    	
    	try (
			PreparedStatement stmtValuesAfterTs = con.prepareStatement(STMT_SEL_MEASUREMENTS_AFTER_TS);
			PreparedStatement stmtMaxPeriodId = con.prepareStatement(STMT_SEL_MAX_DRYING_PERIOD);
			PreparedStatement stmtCreateFirstPeriod = con.prepareStatement(STMT_INS_CREATE_FIRST_PERIOD);
			PreparedStatement stmtCreatePeriod = con.prepareStatement(STMT_INS_CREATE_PERIOD);
    		PreparedStatement stmtMeasurementsInPeriod = con.prepareStatement(STMT_SEL_MEASUREMENTS_IN_PERIOD);
    		PreparedStatement stmtEndPeriod = con.prepareStatement(STMT_UPD_PERIOD_END)
		) {
    		// Track if something has changed. If not, we are done.
    		boolean somethingChanged = false;
    		do {
    			// Reset change tracker
    			somethingChanged=false;
    			
	    		// Check if we have a period and what its ID is
	    		stmtMaxPeriodId.setInt(1, pegId);
	    		Integer lastPeriodId = null;
	    		boolean lastPeriodIsOpen = false;
	    		Timestamp lastPeriodEnd = null;
	    		Timestamp lastPeriodStart = null;
	    		try (ResultSet rs = stmtMaxPeriodId.executeQuery()) {
	    			// If we do not find any period, leave periodId=null, the code below will care about it
	    			if (rs.next()) {
	    				lastPeriodId = rs.getInt(1);
	    				lastPeriodStart = rs.getTimestamp(2);
	    				lastPeriodEnd = rs.getTimestamp(3);
	    				lastPeriodIsOpen = lastPeriodEnd == null;
	    				logger.log(Level.INFO, "Current drying period for peg #" + pegId + " is period #" + lastPeriodId);
	    			}
	    		}
	    		
	    		// If it does not exist, create the first period
	    		if (lastPeriodId == null) {
	    			logger.log(Level.INFO, "Creating the first drying period for peg #" + pegId);
	    			stmtCreateFirstPeriod.setInt(1, pegId); // relation drying_period
	    			stmtCreateFirstPeriod.setInt(2, pegId); // relation measurement (for lookup of ts_start)
	    			stmtCreateFirstPeriod.executeUpdate();
	    			somethingChanged = true;
	    			newPeriodIDs.add(1);
	    			continue;
	    		}
	    		
	    		if (lastPeriodIsOpen) {
	        		// If the last period is open, try to close it.
	    			stmtMeasurementsInPeriod.setInt(1, pegId);
	    			stmtMeasurementsInPeriod.setTimestamp(2, lastPeriodStart);
	    			
	    			// Iterate through the measurements to find out when the period stops.
	    			// Also, we calculate a moving average over the last conductance values to
	    			// estimate ts_dry.
	    			Timestamp tsDry = null;
	    			Timestamp tsEnd = null;
	    			
	    			// We need the previous timestamp to find gaps in the drying process
	    			Timestamp previousTs = null;
	    			Queue<Float> avgValues = new ArrayDeque<>();
	    			try (ResultSet rs = stmtMeasurementsInPeriod.executeQuery()) {
	    				while(rs.next() && tsEnd==null) {
	    					Timestamp currentTs = rs.getTimestamp(1);
	    					if (previousTs == null || currentTs.getTime() - previousTs.getTime() < PERIOD_THRESHOLD*1000) {
		    					if (tsDry == null) {
			    					// Calculate the moving average if we don't have tsDry yet
		    						// We use the queue to track the last n values and calculate the average each round.
			    					avgValues.add(rs.getFloat(2));
			    					if (avgValues.size() > MOVING_AVG_SIZE_CONDUCTANCE) {
			    						avgValues.poll();
			    					}
			    					
			    					// Require the queue to have at least 20 percent of the values filled (so that some
			    					// zeroes at the start of the period do not directly terminate estimation of tsDry)
			    					if (avgValues.size() > MOVING_AVG_SIZE_CONDUCTANCE/5) {
				    					// Actually calculate moving average
				    					double movAvg = avgValues.stream().mapToDouble(f->f).average().getAsDouble();
				    					
				    					// If the we are under the threshold, we have tsDry now.
				    					if (movAvg < MOVING_AVG_THRESHOLD_CONDUCTANCE) {
				    						tsDry = currentTs;
				    					}
			    					}
		    					}
	    					} else {
	    						// currentTs is the first timestamp that does no longer fit into the period.
	    						// This means we cut the intervals here.
	    						tsEnd = previousTs;
	    					}
	    					previousTs = currentTs;
	    				}
	    			}
	    			
	    			// Corner case: The last measurement in the db is the end of a period
	    			if (tsEnd == null && getDbServerTime(con).getTime() - previousTs.getTime() > PERIOD_THRESHOLD*1000) {
	    				tsEnd = previousTs;
	    			}
	    			
	    			// We found the end of a period
	    			if (tsEnd != null) {
		    			logger.log(Level.INFO, "Closing drying period #" + lastPeriodId + " for peg #" + pegId);
	    				// Update the period in the database
	    				stmtEndPeriod.setTimestamp(1, tsDry);
	    				stmtEndPeriod.setTimestamp(2, tsEnd);
	    				stmtEndPeriod.setInt(3, pegId);
	    				stmtEndPeriod.setInt(4, lastPeriodId);
	    				stmtEndPeriod.executeUpdate();
	    				
		    			// We changed something by closing the period
		    			somethingChanged = true;
		    			// And as well we closed a period, which will require post-processing
		    			newPeriodIDs.add(lastPeriodId);
	    			}
	    		} else {
	    			// If the last period is not open, check whether we should create a new one.
	    			stmtValuesAfterTs.setInt(1, pegId);
	    			stmtValuesAfterTs.setTimestamp(2, lastPeriodEnd);
	    			
	    			// Count unprocessed measurements
	    			int newMeasurementCount = 0;
	    			try (ResultSet rs = stmtValuesAfterTs.executeQuery()) {
	    				// Count will always return one row
	    				rs.next();
	    				newMeasurementCount = rs.getInt(1);
	    			}
	    			
	    			// We have new measurements
	    			if (newMeasurementCount > 0) {
		    			logger.log(Level.INFO, "Creating drying period #" + (lastPeriodId + 1) + " for peg #" + pegId);
	    				// Insert an open period
	    				stmtCreatePeriod.setInt(1, pegId); // relation drying_period
	    				stmtCreatePeriod.setInt(2, lastPeriodId + 1); // new period id
	    				stmtCreatePeriod.setInt(3, pegId); // relation measurements (to find the suitable timestamp)
	    				stmtCreatePeriod.setTimestamp(4, lastPeriodEnd); // Find next measurement with bigger timestamp
	    				stmtCreatePeriod.executeUpdate();
	    				
	    				// We inserted a new period, so something has changed
	    				// (next round will check if it can be closed)
	    				somethingChanged = true;
	    			}
	    		}
    		} while(somethingChanged);
    	}
    	
    	logger.log(Level.INFO, "Done updating drying periods for peg #" + pegId + " (found " +
    			newPeriodIDs.size() + " new periods)");
    	return newPeriodIDs;
    }
    
    /**
     * This tasks creates the training data for a specific peg and period id.
     * 
     * @param con The database connection
     * @param pegId The peg to consider
     * @param periodId The period to consider (usually output of {@link #updateDryingPeriods(Connection, int)}).
     */
    private void createTrainingData(Connection con, int pegId, int periodId) throws SQLException {
    	logger.log(Level.INFO, "Updating training data for peg #" + pegId + " in drying period " + periodId);

    	try (
    		PreparedStatement stmtSelectTimestamps = con.prepareStatement(STMT_SELECT_PERIOD_DATA);
    		PreparedStatement stmtSelectMeasurements = con.prepareStatement(STMT_SELECT_MEASUREMENTS_FOR_TRAIN_DATA_CREATION);
    		PreparedStatement stmtCreateTrainingData = con.prepareStatement(STMT_INS_MEASUREMENT_TRAIN)
    	) {
    		// Fetch timestamp data
    		stmtSelectTimestamps.setInt(1, pegId);
    		stmtSelectTimestamps.setInt(2, periodId);
    		try (ResultSet rsTimestamps = stmtSelectTimestamps.executeQuery()) {
    			// For each and every measurement in the period, create the moving average and store it into measurement_train
    			rsTimestamps.next();
    			Timestamp tsStart = rsTimestamps.getTimestamp(1);
    			//Timestamp tsEnd = rsTimestamps.getTimestamp(2);
    			Timestamp tsDry   = rsTimestamps.getTimestamp(3);

    			// If we have not ts_dry, we cannot do anything 
    			if (tsDry == null) {
    				logger.log(Level.WARNING, "Cannot create training data for peg #" + pegId + ", period #" + periodId + " - No tsDry could be found");
    				return;
    			}
    			
    			// Get measurements from measurements table
    			stmtSelectMeasurements.setInt(1, pegId);
    			stmtSelectMeasurements.setTimestamp(2, tsStart);
    			stmtSelectMeasurements.setTimestamp(3, tsDry);
    			try (ResultSet rsMeasurement = stmtSelectMeasurements.executeQuery()) {
    				while(rsMeasurement.next()) {
    					int sampleNr = rsMeasurement.getInt(1);
    					String sampleSensor = rsMeasurement.getString(2);
    					Timestamp sampleTs = rsMeasurement.getTimestamp(3);
    					
    					// Create training sample
    					String inputVector = createInputVector(con, pegId, sampleSensor, sampleTs, tsStart);
    					String outputVector = createOutputVector(sampleTs, tsDry);
    					stmtCreateTrainingData.setInt(1, pegId);
    					stmtCreateTrainingData.setInt(2, sampleNr);
    					stmtCreateTrainingData.setString(3, sampleSensor);
    					stmtCreateTrainingData.setString(4, inputVector);
    					stmtCreateTrainingData.setString(5, outputVector);
    					stmtCreateTrainingData.executeUpdate();
    				}
    			}
    		}
    	}
    	
    	logger.log(Level.INFO, "Done updating training data for peg #" + pegId + " in drying period " + periodId);
    }
        
    /**
     * Returns the server's time
     * @param con Connection
     * @return Timestamp
     * @throws SQLException Communication error
     */
    private Timestamp getDbServerTime(Connection con) throws SQLException {
    	try (
			PreparedStatement stmt = con.prepareStatement(STMT_SEL_SERVERTIME);
			ResultSet rs = stmt.executeQuery()
		) {
    		rs.next();
    		return rs.getTimestamp(1);
    	}
    }
}
