package de.tudarmstadt.smartpeg;

import static de.tudarmstadt.smartpeg.data.DataSourceProvider.getDataSource;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import de.tudarmstadt.smartpeg.ml.TrainingDataInputStream;

@Path("/training")
public class TrainingDataService {

    private static Logger logger = Logger.getLogger(TrainingDataService.class.getName());
    
	@GET
    @Path("/{sensor}")
	@Produces(MediaType.TEXT_PLAIN)
    public Response getTrainingData(@PathParam("sensor") String sensor) {
    	try {
    		DataSource ds = getDataSource();
	        if ("HDC1080".equals(sensor) || "DHT22".equals(sensor)) {
	        	try {
	        		return Response.ok(new TrainingDataInputStream(ds, sensor)).build();
	        	} catch (Exception ex) {
	        		logger.log(Level.SEVERE, "Error reading trainig data", ex);
	        		return Response.status(500).entity("Internal server error").build();
	        	}
	        } else {
	        	return Response.status(404).entity("Sensor does not exist").build();
	        }
    		
        } catch (NamingException ex) {
        	logger.log(Level.SEVERE, "Cannot instantiate DataSource", ex);
        	return Response.status(500).entity("Service not available").build();
        }
    }
}
