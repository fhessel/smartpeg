package de.tudarmstadt.smartpeg;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import static de.tudarmstadt.smartpeg.data.DataSourceProvider.getDataSource;

import java.util.logging.Level;
import java.util.logging.Logger;

@Path("/peg")
public class SmartPegService {
    // Received loggers
    private static Logger logger = Logger.getLogger(SmartPegService.class.getName());

    @GET
    @Path("/{pegID}")
    public Response getPegInfos(@PathParam("pegID") int pegID) {
    	try {
	        DataSource ds = getDataSource();
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

    @GET
    @Path("/{pegID}/measurement")
    public Response getLastMeasurement(@PathParam("pegID") int pegID){
        try {
        	DataSource ds = getDataSource();
            JSONObject pegInfos = PegManagement.getLastMeasurement(pegID, ds);
            // If no information related to the peg id is found, then a 404 is returned, else the object with a 200 is.
            if(pegInfos == null){
                return Response.status(404).entity("no measurement found for this peg id").build();
            }else {
                return Response.status(200).entity(pegInfos.toJSONString()).build();
            }

        } catch (NamingException ex) {
            logger.log(Level.SEVERE, "Cannot instantiate DataSource", ex);
            return Response.status(500).entity("Service not available").build();
        }
    }

    @GET
    @Path("/pegs")
    public Response getPegs(){
        try {
        	DataSource ds = getDataSource();
            JSONArray pegs = PegManagement.getPegs(ds);
            // If no information related to the peg id is found, then a 404 is returned, else the object with a 200 is.
            if(pegs == null){
                return Response.status(404).entity("No pegs found").build();
            }else {
                return Response.status(200).entity(pegs.toJSONString()).build();
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
        JSONArray json;
        try {
            json = (JSONArray) parser.parse(measurement);
        } catch (ParseException e) {
            logger.log(Level.WARNING, "Cannot read input json", e);
            return Response.status(400).entity("Error adding the measurement: Invalid JSON Format.").build();
        } catch (ClassCastException e) {
        	logger.log(Level.WARNING, "Expected array as input but got Object", e);
        	return Response.status(400).entity("Error adding the measurement: Expected array of Objects.").build();
        }
        logger.info("received post request with following json: " + measurement);
        try {
        	DataSource ds = getDataSource();
	        boolean success = PegManagement.setPegMeasurements(pegID, json, ds);
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

    @GET
    @Path("/{pegID}/prediction")
    public Response getPrediction(@PathParam("pegID") int pegID){
        try {
            DataSource ds = getDataSource();
            JSONObject prediction = PegManagement.getPrediction(pegID, ds);
            // If no information related to the peg id is found, then a 404 is returned, else the object with a 200 is.
            if(prediction == null){
                return Response.status(404).entity("no measurement found for this peg id").build();
            }else {
                return Response.status(200).entity(prediction.toJSONString()).build();
            }

        } catch (NamingException ex) {
            logger.log(Level.SEVERE, "Cannot instantiate DataSource", ex);
            return Response.status(500).entity("Service not available").build();
        }
    }
}