package de.tudarmstadt.smartpeg;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Path("/peg")
public class SmartPegService {
    // Received loggers
    private Logger logger = Logger.getLogger(SmartPegService.class.getName());

    @GET
    @Path("/{id}")
    public Response getMsg(@PathParam("id") int id) {

        JSONObject json = new JSONObject();

        /* Charge the driver JDBC for MySQL */
        try {
            logger.info( "Charging the driver..." );
            Class.forName( "com.mysql.jdbc.Driver" );
            logger.info( "Driver charged !" );
        } catch ( ClassNotFoundException e ) {
            logger.severe( "Erreur while charging the driver, driver not found ! "
                    + e.getMessage() );
        }

        /* Connect to the database */
        String url = "jdbc:mysql://localhost:3306/smart_peg";
        String user = "admin";
        String password = "admin";
        Connection connexion = null;
        PreparedStatement prepared_statement = null;
        ResultSet result = null;
        try {
            logger.info( "Connecting to the database..." );
            connexion = DriverManager.getConnection( url, user, password );
            logger.info( "Connexion successful !" );

        /* Create an object monitoring request getting the peg infos */
            prepared_statement = connexion.prepareStatement("SELECT * FROM peg WHERE id = ?");
            prepared_statement.setInt(1, id);
            logger.info( "Request Object created !" );

        /* Execute a reading query */
            result = prepared_statement.executeQuery();
            logger.info( "Request \"SELECT * FROM peg WHERE id = " + id + ";\" executed !" );

        /* Fetch the result of the reading of the query */
            if ( result.next() ) {
                int idPeg = result.getInt( "id" );
                int batStatus = result.getInt("bat_status");
                /* Format the result for the output. */
                logger.info( "Result for the query : id = " + idPeg + ", bat_status = " + batStatus + "." );
                json.put("id", idPeg);
                json.put("bat_status", batStatus);

                // Create an object monitoring request getting the peg measurements
                prepared_statement = connexion.prepareStatement("SELECT * FROM measurement WHERE peg_id = ?");
                prepared_statement.setInt(1, id);
                // Store the query response
                result = prepared_statement.executeQuery();
                JSONArray measurements = new JSONArray();
                // Add the measurements to the json
                while(result.next()){
                    JSONObject measurement = new JSONObject();
                    int nr = result.getInt("nr");
                    float temperature = result.getFloat("temperature");
                    float humidity = result.getFloat("humidity");
                    int conductance = result.getInt("conductance");
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
                prepared_statement.setInt(1, id);
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
                logger.severe("Peg ID does not exist");
                return Response.status(404).entity("peg ID not found").build();
                //throw new IllegalArgumentException("Peg ID does not exist.");
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
        return Response.status(200).entity(json.toJSONString()).build();

    }

}