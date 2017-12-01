package de.tudarmstadt.smartpeg;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.StringWriter;
import java.sql.*;
import java.util.HashMap;
import java.util.Iterator;
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
     * This method gets all infos from the peg with the given id:
     * {
     *  "id": 0,
     *  "bat_status": 0,
     *  "measurements": [
     *      {
     *          "temperature": 0,
     *          "humidity": 0,
     *          "conductance": 0
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

        Connection connexion = null;
        PreparedStatement prepared_statement = null;
        ResultSet result = null;
        try {
            logger.info( "Connecting to the database..." );
            connexion = dataSource.getConnection();

        /* Create an object monitoring request getting the peg infos */
            prepared_statement = connexion.prepareStatement("SELECT * FROM peg WHERE id = ?");
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
                prepared_statement = connexion.prepareStatement("SELECT * FROM measurement WHERE peg_id = ?");
                prepared_statement.setInt(1, pegID);
                // Store the query response
                result = prepared_statement.executeQuery();
                JSONArray measurements = new JSONArray();
                // Add the measurements to the json
                while(result.next()){
                    JSONObject measurement = new JSONObject();
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
                    measurements.add(measurement);
                }
                json.put("measurements", measurements);

                // Create an object monitoring request getting the peg predictions
                prepared_statement = connexion.prepareStatement("SELECT * FROM prediction WHERE peg_id = ?");
                prepared_statement.setInt(1, pegID);
                // Store the query response
                result = prepared_statement.executeQuery();
                JSONArray predictions = new JSONArray();
                // Add the measurements to the json
                while(result.next()){
                    JSONObject prediction = new JSONObject();
                    Timestamp dry_at = result.getTimestamp("dry_at");
                    prediction.put("dry_at", dry_at);
                    predictions.add(prediction);
                }
                json.put("predictions", predictions);
                StringWriter out = new StringWriter();
                try {
                    json.writeJSONString(out);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                String jsonText = out.toString();
                System.out.print(jsonText);
            }else{
                return null;
            }
        } catch ( SQLException e ) {
            logger.severe( "Error while connecting : "
                    + e.getMessage() );
        } finally {
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
            if ( connexion != null ) {
                try {
                    connexion.close();
                } catch ( SQLException ignore ) {
                }
            }
        }
        return json;
    }

    public static boolean setPegMeasurement(int pegID, JSONObject measurement, DataSource dataSource){

        // verify that the JSON has the correct format and return false if it doesn't
        // TODO test if this function works
        if(!formatIsCorrect(measurement)){
            return false;
        }

        Connection connexion = null;
        PreparedStatement prepared_statement = null;
        ResultSet result = null;
        try {
            logger.info("Connecting to the database...");
            connexion = dataSource.getConnection();
            // Get the number of the last measurement
            PreparedStatement get_prep_st = connexion.prepareStatement(
                    "SELECT nr FROM measurement WHERE peg_id = ? ORDER BY nr DESC LIMIT 1;");
            get_prep_st.setInt(1, pegID);
            result = get_prep_st.executeQuery();
            // Set nr to the number of the last measurement if there is one
            int nr;
            if(result.next()) {
                nr = result.getInt("nr")+1;
            }else {
                nr = 0;
            }

        /* Create an object monitoring request setting the peg measurement */
            prepared_statement = connexion.prepareStatement(
                    "INSERT INTO measurement (peg_id, nr, temperature, humidity, conductance, sensor_type, timestamp)" +
            "VALUES(?, ?, ?, ?, ?, ?, NOW());");
            prepared_statement.setInt(1, pegID);
            prepared_statement.setInt(2, nr);
            prepared_statement.setFloat(3, new Float((Double)measurement.get("temperature")));
            prepared_statement.setFloat(4, new Float((Double)measurement.get("humidity")));
            prepared_statement.setFloat(5, new Float((Double)measurement.get("conductance")));
            prepared_statement.setString(6, measurement.get("sensor_type").toString());
            // Update the table if it is possible
            if(prepared_statement.executeUpdate() == -1){
                return false;
            }
        } catch ( Exception e ) {
            logger.severe( "Error while connecting : "
                    + e.getMessage() );
        } finally {
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
            if ( connexion != null ) {
                try {
                    connexion.close();
                } catch ( SQLException ignore ) {
                }
            }
        }
        return true;
    }

    private static boolean formatIsCorrect(JSONObject measurement) {
        HashMap<String, Class> values = new HashMap<String, Class>();
        values.put("temperature", Double.class);
        values.put("humidity", Double.class);
        values.put("conductance", Double.class);
        values.put("sensor_type", String.class);
        Set<Map.Entry<String, Class>> values_set = values.entrySet();
        Iterator<Map.Entry<String, Class>> values_it = values_set.iterator();
        while(values_it.hasNext()){
            Map.Entry<String, Class> value = values_it.next();
            String key = value.getKey();
            Class val_class = value.getValue();
            // TODO test if the class of the result is corresponding to the one wanted
            if(measurement == null){
                return false;
            }
            if(measurement.get(key) == null || !measurement.get(key).getClass().equals(val_class)){
            	logger.warning("Value for " + key + " in request with wrong data type (expected=" +
            			val_class + ", got=" + measurement.get(key).getClass() + ")");
                return false;
            }
        }
        return true;
    }
}
