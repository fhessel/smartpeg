package de.tudarmstadt.smartpeg.ml;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;

import static de.tudarmstadt.smartpeg.ml.MLVectorExtractor.VALUE_SEPARATOR;
import static de.tudarmstadt.smartpeg.ml.MLVectorExtractor.TIME_SAMPLE_OFFSETS;

/**
 * Input stream that reads training data from the DB
 * @author frank
 *
 */
public class TrainingDataInputStream extends InputStream {

	private static Logger logger = Logger.getLogger(TrainingDataInputStream.class.getName());
	
	/** The sensor to read data from */
	private String sensor;
	
	/** The database */
	private DataSource ds;
	
	/** Internal buffer for the next row */
	private StringBuilder buffer;
		
	/** true if the connection has been closed */
	private boolean isClosed;
	
	/** the result set */
	private ResultSet rs;
	
	/** the statement */
	private PreparedStatement stmt;
	
	/** the connection */
	private Connection con;
	
	/**
	 * Creates a TrainigDataInputStream for the given sensor using the given DataSource
	 * @param ds the datasource
	 * @param sensor the sensor
	 */
	public TrainingDataInputStream(DataSource ds, String sensor) {
		super();
		this.sensor = sensor;
		this.ds = ds;
		this.isClosed = false;
		this.buffer = new StringBuilder();
		for(int i = 0; i < TIME_SAMPLE_OFFSETS.length; i++) {
			buffer.append("temp").append(i).append(VALUE_SEPARATOR);
			buffer.append("hum").append(i).append(VALUE_SEPARATOR);
			buffer.append("cond").append(i).append(VALUE_SEPARATOR);
		}
		buffer.append("time_remaining").append("\n");
	}
	
	@Override
	public void close() throws IOException {
		super.close();
		isClosed = true;
		if (rs!=null) {
			try {
				rs.close();
			} catch (SQLException ex) {
				logger.log(Level.WARNING, "Could not close ResultSet", ex);
			}
			rs = null;
		}
		
		if (stmt!=null) {
			try {
				stmt.close();
			} catch (SQLException ex) {
				logger.log(Level.WARNING, "Could not close Prepared Statement", ex);
			}
			stmt = null;
		}
		
		if (con!=null) {
			try {
				con.close();
			} catch (SQLException ex) {
				logger.log(Level.WARNING, "Could not close Connection", ex);
			}
			con = null;
		}
	}
	
	@Override
	public int read() throws IOException {
		updateBuffer();
		if (isClosed || buffer.length() == 0) {
			return -1;
		} else {
			int i = buffer.charAt(0);
			buffer.delete(0, 1);
			return i;
		}
	}
	
	/**
	 * Updates the buffer if it is empty. If the buffer is still empty after calling this, there's no more data
	 * @throws IOException Something went wrong
	 */
	private void updateBuffer() throws IOException {
		if (!isClosed && buffer.length() == 0) {
			try {
				// On first read, initialize the connection
				boolean initializedNow = false;
				if (rs == null) {
					initialize();
					initializedNow = true;
				}
				if (rs != null && rs.next()) {
					// From the second row on, append line break
					if(!initializedNow) {
						buffer.append("\n");
					}
					// Append data
					buffer.append(rs.getString(1));
					buffer.append(VALUE_SEPARATOR);
					buffer.append(rs.getString(2));
				}
			} catch (SQLException ex) {
				close();
				throw new IOException("Could not read training data due to SQL problems", ex);
			}
		}
	}
	
	/**
	 * Initializes the connection. Once called, the stream has to be closed(!)
	 * @throws SQLException something went wrong
	 */
	private void initialize() throws SQLException {
		con = ds.getConnection();
		stmt = con.prepareStatement("SELECT vec_data_in, vec_data_out FROM measurement_train WHERE sensor_type=?");
		stmt.setString(1, sensor);
		rs = stmt.executeQuery();
	}

}
