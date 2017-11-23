package de.tudarmstadt.smartpeg;

import org.json.simple.JSONObject;

import javax.sql.DataSource;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.logging.Logger;

@Path("/peg")
public class SmartPegService {
    // Received loggers
    private Logger logger = Logger.getLogger(SmartPegService.class.getName());
    DataSource ds;

    @GET
    @Path("/{pegID}")
    public Response getPeg(@PathParam("pegID") int pegID) {
        ds = getDataSource("mysql");
        JSONObject pegInfos = PegManagement.getPegInfos(pegID, ds, logger);
        // If no information related to the peg id is found, then a 404 is returned, else the object with a 200 is.
        if(pegInfos == null){
            return Response.status(404).entity("peg ID not found").build();
        }else {
            return Response.status(200).entity(pegInfos.toJSONString()).build();
        }

    }

    @POST
    @Path("/{pegID}/readings")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response setPeg(@PathParam("pegID") int pegID, JSONObject measurement){
        ds = getDataSource("mysql");
        boolean success = PegManagement.setPegMeasurement(pegID, measurement, ds, logger);
        if(success){
            return Response.status(200).entity("Peg Measurement added with success").build();
        }else{
            return Response.status(404).entity("Error adding the measurement: No peg found for the requested id " +
                    " or the json format does not match the expected. ").build();
        }
    }

    private DataSource getDataSource(String DBName) {
        logger.info( "Charging the data source..." );
        DataSource ds;
        if(DBName.equals("mysql")){
            ds = DataSourceFactory.getMySQLDataSource();
            return ds;
        }
        else if(DBName.equals("mariadb")){
            ds = DataSourceFactory.getMariaDBDataSource();
            return ds;
        }else{
            throw new IllegalArgumentException("DataSource could not be created: invalid DB name '" + DBName + "'");
        }

    }

}