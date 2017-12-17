package de.tudarmstadt.smartpeg;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.StringWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Created by Malou on 23.11.17.
 * This class will hold all methods for communicating with the database
 */
public class PegManagement {

    private static Logger logger = Logger.getLogger(PegManagement.class.getName());
    
    /**
     * This method gets all infos from the peg with the given id (last 10 measurements and predictions):
     * {
     *  "id": 0,
     *  "bat_status": 0,
     *  "measurements": [
     *      {
     *          "nr": 0,
     *          "temperature": 0,
     *          "humidity": 0,
     *          "conductance": 0,
     *          "timestamp": "yyyy-mm-dd mm:hh:ss"
     *      }
     *  ],
     *  "predictions": [
     *      {
     *          "dryAt": "yyyy-mm-dd mm:hh:ss"
     *      }
     *  ]
     * }
     * @param pegID The ID of the peg to search
     * @param dataSource The dataSource pointing to the database
     * @return json in case it was found. Null if no peg was found
     */
    public static JSONObject getPegInfos(int pegID, DataSource dataSource){
        JSONObject json = new JSONObject();

        /* Connect to the database */

        Connection connection = null;
        PreparedStatement prepared_statement = null;
        PreparedStatement prepared_statement_measurements = null;
        PreparedStatement prepared_statement_predictions = null;
        ResultSet result = null;
        try {
            logger.info( "Connecting to the database..." );
            connection = dataSource.getConnection();

        /* Create an object monitoring request getting the peg infos */
            prepared_statement = connection.prepareStatement("SELECT * FROM peg WHERE id = ?");
            prepared_statement.setInt(1, pegID);

        /* Execute a reading query */
            result = prepared_statement.executeQuery();

        /* Fetch the result of the reading of the query */
            if ( result.next() ) {
                int idPeg = result.getInt( "id" );
                int batStatus = result.getInt("bat_status");
                /* Format the result for the output. */
                json.put("id", idPeg);
                json.put("bat_status", batStatus);

                // Create an object monitoring request getting the peg measurements
                prepared_statement_measurements = connection.prepareStatement("SELECT * FROM measurement WHERE peg_id = ? " +
                        "ORDER BY timestamp DESC LIMIT 10");
                prepared_statement_measurements.setInt(1, pegID);
                // Store the query response
                result = prepared_statement_measurements.executeQuery();
                JSONArray measurements = new JSONArray();
                // Add the measurements to the json
                while(result.next()){
                    JSONObject measurement = new JSONObject();
                    addMeasurement(result, measurement);
                    measurements.add(measurement);
                }
                json.put("measurements", measurements);

                // Create an object monitoring request getting the peg predictions
                prepared_statement_predictions = connection.prepareStatement("SELECT * FROM prediction WHERE peg_id = ? " +
                                "ORDER BY nr DESC LIMIT 10");
                prepared_statement_predictions.setInt(1, pegID);
                // Store the query response
                result = prepared_statement_predictions.executeQuery();
                JSONArray predictions = new JSONArray();
                // Add the measurements to the json
                while(result.next()){
                    JSONObject prediction = new JSONObject();
                    Timestamp dry_at = result.getTimestamp("dry_at");
                    prediction.put("dry_at", dry_at);
                    predictions.add(prediction);
                }
                json.put("predictions", predictions);
            }else{
                return null;
            }
        } catch ( SQLException e ) {
            logger.severe( "Error while connecting : "
                    + e.getMessage() );
        } finally {
            closeConnexion(result, prepared_statement, connection);
            logger.info( "Close the statement object for measurements." );
            if ( prepared_statement_measurements != null ) {
                try {
                    prepared_statement_measurements.close();
                } catch ( SQLException ignore ) {
                }
            }
            logger.info( "Close the statement object for predictions." );
            if ( prepared_statement_predictions != null ) {
                try {
                    prepared_statement_predictions.close();
                } catch ( SQLException ignore ) {
                }
            }

        }
        return json;
    }

    /**
     * This method gets the ids and battery status of all pegs registered in the database:
     * [{
     *  "id": 0,
     *  "bat_status": 0
     * }]
     * @param dataSource The dataSource pointing to the database
     * @return json in case it was found. Null if no peg was found
     */
    public static JSONArray getPegs(DataSource dataSource) {
        JSONArray pegs = new JSONArray();

        /* Connect to the database */

        Connection connection = null;
        PreparedStatement prepared_statement = null;
        ResultSet result = null;
        try {
            logger.info( "Connecting to the database..." );
            connection = dataSource.getConnection();

            prepared_statement = connection.prepareStatement("SELECT * FROM peg");
            result = prepared_statement.executeQuery();

            while ( result.next() ) {
                JSONObject peg = new JSONObject();
                peg.put("id", result.getInt("id"));
                peg.put("bat_status", result.getInt("bat_status"));
                pegs.add(peg);
            }
            if(pegs.isEmpty()){
                return null;
            }

        } catch ( SQLException e ) {
            logger.severe( "Error while connecting : "
                    + e.getMessage() );
        } finally {
            closeConnexion(result, prepared_statement, connection);
        }
        return pegs;
    }

    /**
     * This method gets the last registered measurement:
     * {
     *      "nr": 0,
     *      "temperature": 0,
     *      "humidity": 0,
     *      "conductance": 0,
     *      "timestamp": "yyyy-mm-dd mm:hh:ss"
     *  }
     * @param pegID The ID of the peg that recorded the measurement
     * @param dataSource The dataSource pointing to the database
     * @return json in case it was found. Null if no peg was found
     */
    public static JSONObject getLastMeasurement(int pegID, DataSource dataSource){
        JSONObject json = new JSONObject();

        /* Connect to the database */

        Connection connection = null;
        PreparedStatement prepared_statement = null;
        ResultSet result = null;
        try {
            logger.info( "Connecting to the database..." );
            connection = dataSource.getConnection();

        /* Create an object monitoring request getting the peg infos */
            prepared_statement = connection.prepareStatement("SELECT * FROM measurement WHERE peg_id  = ? " +
                    "ORDER BY timestamp DESC LIMIT 1;");
            prepared_statement.setInt(1, pegID);

        /* Execute a reading query */
            result = prepared_statement.executeQuery();

        /* Fetch the result of the reading of the query */
            if ( result.next() ) {
                addMeasurement(result, json);
            }else{
                return null;
            }
        } catch ( SQLException e ) {
            logger.severe( "Error while connecting : "
                    + e.getMessage() );
        } finally {
            closeConnexion(result, prepared_statement, connection);
        }
        return json;
    }
    /**
     * Adds a measurement to the database.
     * @param pegID the peg from which the measurement was made
     * @param measurement a json object containing the measurement like following:
     *   {
     *      "temperature": 0,
     *      "humidity": 0,
     *      "conductance": 0,
     *      "sensor-type": "myType"
     *   }
     * */
    public static boolean setPegMeasurements(int pegID, JSONArray measurements, DataSource dataSource){

    	List<JSONObject> validatedMeasurements = new ArrayList<>(measurements.size());
    	
    	// Validation
    	for(Object oMeasurement : measurements) {
    		if (!(oMeasurement instanceof JSONObject)) {
    			logger.warning("One of the supplied measurements was not of type JSONObject");
    		}
    		JSONObject measurement = (JSONObject)oMeasurement;
    		
    		// verify that the JSON has the correct format and return false if it doesn't
            // TODO test if this function works
            if(!formatIsCorrect(measurement)){
            	logger.warning("One of the supplied measurements has a wrong format");
                return false;
            }
            validatedMeasurements.add(measurement);
    	}
	    
    	// Now process the validated measurements
        Connection connection = null;
        PreparedStatement prepared_statement = null;
        ResultSet result = null;
        try {
            logger.info("Connecting to the database...");
            connection = dataSource.getConnection();

            /* Create an object monitoring request setting the peg measurement */
            prepared_statement = connection.prepareStatement(
                    "INSERT INTO measurement (peg_id, nr, temperature, humidity, conductance, sensor_type, timestamp)" +
            "VALUES(?, (SELECT max(nr)+1 FROM measurement m where m.peg_id = ?), ?, ?, ?, ?, date_add(NOW(), interval ? second));");
            for(JSONObject measurement : validatedMeasurements) {
	            prepared_statement.setInt(1, pegID);
	            prepared_statement.setInt(2, pegID);
	            prepared_statement.setFloat(3, new Float((Double)measurement.get("temperature")));
	            prepared_statement.setFloat(4, new Float((Double)measurement.get("humidity")));
	            prepared_statement.setFloat(5, new Float((Double)measurement.get("conductance")));
	            prepared_statement.setString(6, measurement.get("sensor_type").toString());
	            long offset = 0;
	            if (measurement.containsKey("timeOffset")) {
	            	offset = (Long)measurement.get("timeOffset");
	            }
	            prepared_statement.setLong(7, offset);
	            // Update the table if it is possible
	            if(prepared_statement.executeUpdate() == -1){
	                return false;
	            }
            }
        } catch ( Exception e ) {
            logger.severe( "Error while connecting : "
                    + e.getMessage() );
        } finally {
            closeConnexion(result, prepared_statement, connection);
        }
        return true;
    }

    /**
     * Checks if the format of the json object corresponds to a measurement.
     * @param measurement the Json Object to check
     * */
    private static boolean formatIsCorrect(JSONObject measurement) {
        if(measurement == null){
            return false;
        }
        
        // Define the expected data types
        HashMap<String, Class> values = new HashMap<String, Class>();
        values.put("temperature", Double.class);
        values.put("humidity", Double.class);
        values.put("conductance", Double.class);
        values.put("sensor_type", String.class);
        values.put("timeOffset", Long.class);
        
        // Set of the keys that have to be checked
        Set<Map.Entry<String, Class>> values_set = values.entrySet();
        
        // Iterate over the keys
        Iterator<Map.Entry<String, Class>> values_it = values_set.iterator();
        while(values_it.hasNext()){
            Map.Entry<String, Class> value = values_it.next();
            
            // Key in the object
            String key = value.getKey();
            // Class that this key should have
            Class val_class = value.getValue();
            // TODO test if the class of the result is corresponding to the one wanted
            if (measurement.get(key) == null ) {
            	logger.warning("Value for " + key + " is missing in request");
                return false;
            } else if(!measurement.get(key).getClass().equals(val_class)){
            	logger.warning("Value for " + key + " in request with wrong data type (expected=" +
            			val_class + ", got=" + measurement.get(key).getClass() + ")");
                return false;
            }
        }
        return true;
    }

    /**
     * Add a measurement read in a database to a json object
     * @param result the result of the query on the database
     * @param measurement the json object in which the measurement should be added
     * */
    private static void addMeasurement(ResultSet result, JSONObject measurement) throws SQLException {
        int nr = result.getInt("nr");
        float temperature = result.getFloat("temperature");
        float humidity = result.getFloat("humidity");
        float conductance = result.getFloat("conductance");
        Timestamp timestamp = result.getTimestamp("timestamp");
        measurement.put("nr", nr);
        measurement.put("temperature", temperature);
        measurement.put("humidity", humidity);
        measurement.put("conductance", conductance);
        measurement.put("timestamp", timestamp);
    }

    /**
     * Closes the connection to a database
     * @param result the result object to close
     * @param prepared_statement the preparedStatement object to close
     * @param connection the connection to close
     * */

    private static void closeConnexion(ResultSet result, PreparedStatement prepared_statement, Connection connection) {
        logger.info( "Closing the ResultSet object." );
        if ( result != null ) {
            try {
                result.close();
            } catch ( SQLException ignore ) {
            }
        }
        logger.info( "Close the statement object." );
        if ( prepared_statement != null ) {
            try {
                prepared_statement.close();
            } catch ( SQLException ignore ) {
            }
        }
        logger.info( "Close the connection object." );
        if ( connection != null ) {
            try {
                connection.close();
            } catch ( SQLException ignore ) {
            }
        }
    }
}
