package de.tudarmstadt.smartpeg;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import java.util.logging.Level;
import java.util.logging.Logger;

@Path("/peg")
public class SmartPegService {
    // Received loggers
    private static Logger logger = Logger.getLogger(SmartPegService.class.getName());
    DataSource ds;

    @GET
    @Path("/{pegID}")
    public Response getPeg(@PathParam("pegID") int pegID) {
    	try {
	        ds = getDataSource();
	        JSONObject pegInfos = PegManagement.getPegInfos(pegID, ds);
	        // If no information related to the peg id is found, then a 404 is returned, else the object with a 200 is.
	        if(pegInfos == null){
	            return Response.status(404).entity("peg ID not found").build();
	        }else {
	            return Response.status(200).entity(pegInfos.toJSONString()).build();
	        }

        } catch (NamingException ex) {
        	logger.log(Level.SEVERE, "Cannot instantiate DataSource", ex);
        	return Response.status(500).entity("Service not available").build();
        }
    }

    @POST
    @Path("/{pegID}/readings")
    @Consumes("application/json")
    public Response setPeg(@PathParam("pegID") int pegID, String measurement){
        JSONParser parser = new JSONParser();
        JSONObject json;
        try {
            json = (JSONObject) parser.parse(measurement);
        } catch (ParseException e) {
            logger.log(Level.WARNING, "Cannot read input json", e);
            return Response.status(400).entity("Error adding the measurement: Invalid JSON Format.").build();
        }
        logger.info("received post request with following json: " + measurement);
        try {
	        ds = getDataSource();
	        boolean success = PegManagement.setPegMeasurement(pegID, json, ds);
	        if(success){
	            return Response.status(200).entity("Peg Measurement added with success").build();
	        }else{
	            return Response.status(404).entity("Error adding the measurement: No peg found for the requested id " +
	                    " or the json format does not match the expected. ").build();
	        }
        } catch (NamingException ex) {
        	logger.log(Level.SEVERE, "Cannot instantiate DataSource", ex);
        	return Response.status(500).entity("Service not available").build();
        }
    }

    /**
     * Helper-Function to get a JNDI-Datasource, as the Resource-Annotation does not work properly
     * @return The Datasource
     * @throws NamingException If the datasource could not be retrieved, most probably because it's not configured
     */
    private DataSource getDataSource() throws NamingException {
    	// Get JNDI-Context
        Context initCtx = new InitialContext();
        // Navigate to comp/env
        Context envCtx = (Context) initCtx.lookup("java:comp/env");
        // Get the datasource
        return (DataSource)envCtx.lookup("jdbc/smartpeg");
    }

}