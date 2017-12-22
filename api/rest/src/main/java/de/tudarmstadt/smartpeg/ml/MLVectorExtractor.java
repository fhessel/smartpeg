package de.tudarmstadt.smartpeg.ml;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import org.apache.commons.lang3.StringUtils;

/**
 * Helper class to convert a specific measurement into vectors that can be used
 * for the machine learning task
 * 
 * @author frank
 *
 */
public class MLVectorExtractor {

    private static Logger logger = Logger.getLogger(MLVectorExtractor.class.getName());
	
	/** Private constructor, it's only a helper class with static methods */
	private MLVectorExtractor() {}
		
	/**
	 * Time offsets for the values in the input vector, in minutes.
	 * 
	 * Example: We want to create the input vector for a sample taken at 3pm. This will be the reference time that
	 * the values in this array will be applied to, so the vector consists of (moving averaged) temperatur, humidity and
	 * conductance values for 12pm, 12:30pm, 1pm, 1:30pm, 1:45pm, 2pm, 2:10pm, 2:20pm, 2:30pm, 2:40pm, 2:50pm, 3pm
	 * 
	 * If there is not enough data to create input for a specific time (e.g. samples just after tsStart), the values are zeroed out.
	 */
	public static final int TIME_SAMPLE_OFFSETS[] = {-180, -150, -120, -90, -75, -60, -50, -40, -30, -20, -10, 0};
	
	/** Size (in minutes) of the moving average window (Note: the code below only works for even numbers) */
	public static final int MOVING_AVERAGE_WINDOW_SIZE = 6;
	
	/** Minimum amount of values within an moving average window to be considered (less than this will be zeroed) */
	public static final int MIN_VALUES_IN_AVERAGE = 5;
	
	/** Statement to create the moving average */
	private static final String STMT_SELECT_MOVING_AVG =
			// Value count (to check that we have enough data in the movavg period), and value averages
			"SELECT count(m.peg_id), avg(m.temperature), avg(m.humidity), avg(m.conductance) FROM measurement m " +
			// Define the data to use (correct peg, sensor type and only in the right period)
			// Note: We don't need to consider tsEnd, as we only look into the past (see TIME_SAMPLE_OFFSETS)
			"WHERE m.peg_id = ? and m.sensor_type = ? and m.timestamp > ? " +
			// Constraint to values in the window
			"and m.timestamp>? and m.timestamp<?";
	
	/** Separator between values in the input/output vector strings */
	public static final String VALUE_SEPARATOR = ";";
	
	/**
	 * Create a training/input sample for the machine learning process
	 * @param con SQLConnection to load measurement data
	 * @param pegId peg that the samples should be generated for
	 * @param sensorType The sensor that has been used (to separate the models)
	 * @param sampleTs The sample's timestamp
	 * @param tsStart The starting timestamp of the drying period (lower limit for data selection)
	 * @return A String representation of the vector
	 * @throws SQLException If the database hates you
	 */
	public static String createInputVector(Connection con, int pegId, String sensorType, Timestamp sampleTs, Timestamp tsStart) throws SQLException {
		List<Float> values = new LinkedList<>();
		try (PreparedStatement stmt = con.prepareStatement(STMT_SELECT_MOVING_AVG)) {
			// This data does not change for a specific series
			stmt.setInt(1, pegId);
			stmt.setString(2, sensorType);
			stmt.setTimestamp(3, tsStart);
			
			// For all offsets...
			for(int offset : TIME_SAMPLE_OFFSETS) {
				Timestamp tsWindowMin = new Timestamp(sampleTs.getTime() + (offset - MOVING_AVERAGE_WINDOW_SIZE/2) * 60 * 1000);
				Timestamp tsWindowMax = new Timestamp(sampleTs.getTime() + (offset + MOVING_AVERAGE_WINDOW_SIZE/2) * 60 * 1000);
				stmt.setTimestamp(4, tsWindowMin);
				stmt.setTimestamp(5, tsWindowMax);
				try (ResultSet rs = stmt.executeQuery()) {
					rs.next();
					if (rs.getLong(1) >= MIN_VALUES_IN_AVERAGE) {
						values.add(rs.getFloat(2));
						values.add(rs.getFloat(3));
						values.add(rs.getFloat(4));
					} else {
						values.add(0.0f);
						values.add(0.0f);
						values.add(0.0f);
					}
				}
			}
		}
		return StringUtils.join(values.stream().map(f->Float.toString(f)).iterator(), VALUE_SEPARATOR);
	}
	
	/**
	 * Creates the output vector for a machine learning sample
	 * @param sampleTs The sample's timestamp
	 * @param tsDry The tsDry for the drying period
	 * @return Output vector (the remaining time, actually)
	 */
	public static String createOutputVector(Timestamp sampleTs, Timestamp tsDry) {
		long remainingTime = tsDry.getTime() - sampleTs.getTime();
		if (remainingTime < 0) {
			remainingTime = 0;
		}
		return Long.toString(remainingTime/1000)+".0";
	}
	
	/**
	 * Looks up the filename that should be used to store the training data.
	 * @return The filename, or "traindata.csv" if no name is set
	 * @throws NamingException If a NamingException occurs during name resolution.
	 */
	public static String getTrainingDataFilename() throws NamingException {
    	// Get JNDI-Context
        Context initCtx = new InitialContext();
        // Navigate to comp/env
        Context envCtx = (Context) initCtx.lookup("java:comp/env");
        if (envCtx == null) {
        	throw new NamingException("Could not find java:comp/env");
        }
        // Get the datasource
        String filename = null;
        try {
        	filename = (String)envCtx.lookup("filenames/trainingDump");
        } catch (NameNotFoundException ex) {
        	logger.log(Level.WARNING, "filenames/trainingDump was not configured in JNDI, using default traindata.csv on app path");
        }
        if (filename == null) {
        	filename = "traindata.csv";
        }
        return filename;
    }
		
}
