package de.tudarmstadt.smartpeg;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Path("/peg")
public class smartPegService {
    // Received messages
    private StringBuilder messages = new StringBuilder();

    @GET
    @Path("/{id}")
    public Response getMsg(@PathParam("id") String id) {

        /* Charge the driver JDBC for MySQL */
        try {
            messages.append( "Charging the driver...\n" );
            Class.forName( "com.mysql.jdbc.Driver" );
            messages.append( "Driver charged !\n" );
        } catch ( ClassNotFoundException e ) {
            messages.append( "Erreur while charging the driver, driver not found ! <br/>\n"
                    + e.getMessage() );
        }

        /* Connect to the database */
        String url = "jdbc:mysql://localhost:3306/smart_peg";
        String user = "admin";
        String password = "admin";
        Connection connexion = null;
        Statement statement = null;
        ResultSet result = null;
        try {
            messages.append( "Connecting to the database...\n" );
            connexion = DriverManager.getConnection( url, user, password );
            messages.append( "Connexion successful !\n" );

        /* Create an object monitoring requests */
            statement = connexion.createStatement();
            messages.append( "Request Object created !\n" );

        /* Execute a reading query */
            result = statement.executeQuery( "SELECT * FROM peg;" );
            messages.append( "Request \"SELECT * FROM peg;\" executed !\n" );

        /* Fetch the result of the reading of the query */
            while ( result.next() ) {
                int idPeg = result.getInt( "id" );
                int batStatus = result.getInt("bat_status");
            /* Format the result for the output. */
                messages.append( "Result for the query : id = " + idPeg + ", bat_status = " + batStatus + ".\n" );
            }
        } catch ( SQLException e ) {
            messages.append( "Error while connecting : <br/>\n"
                    + e.getMessage() );
        } finally {
            messages.append( "Closing the ResultSet object.\n" );
            if ( result != null ) {
                try {
                    result.close();
                } catch ( SQLException ignore ) {
                }
            }
            messages.append( "Close the statement object.\n" );
            if ( statement != null ) {
                try {
                    statement.close();
                } catch ( SQLException ignore ) {
                }
            }
            messages.append( "Close the connection object.\n" );
            if ( connexion != null ) {
                try {
                    connexion.close();
                } catch ( SQLException ignore ) {
                }
            }
        }

        return Response.status(200).entity(messages.toString()).build();

    }

}