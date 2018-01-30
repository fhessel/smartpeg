package de.tud.ami.smartpeg;

import static de.tud.ami.smartpeg.DatabaseConnector.getConnection;
import static de.tud.ami.smartpeg.ml.MLVectorExtractor.createInputVector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.logging.Level;

public class CreatePredictions {

	public static void main(String[] args) {
		try {
			if (args.length > 0) {
				if ("listPeriods".equals(args[0])) {
					listPeriods();
				} else if ("getPredictions".equals(args[0])) {
					try {
						int pegID = Integer.parseInt(args[1]);
						int periodID = Integer.parseInt(args[2]);
						String sensor = args[3];
						getPredictions(pegID, periodID, sensor);
					} catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
						System.err.println("Usage: getPrediction pegID periodID sensor");
						e.printStackTrace();
					}
				} else {
					System.out.println("Unknown command: " + args[0]);
				}
				
				
			} else {
				System.out.println("Parameter count is too low");
			}
		} catch (Exception e) {
			System.err.println("An error occured:");
			e.printStackTrace();
		}
	}

	static void getPredictions(int pegID, int periodID, String sensor) throws SQLException, IOException {
		System.out.println("peg;period;sensor;timestamp;temperature;humidity;conductance;prediction");
		
		// Call the external process (Python + TensorFlow)
		Process process = new ProcessBuilder(
			"./predict_continuous.sh"
		).start();
		BufferedReader predictionOutput = new BufferedReader(new InputStreamReader(process.getInputStream()));
		Writer predictionInput = new OutputStreamWriter(process.getOutputStream());
		
		try (
			Connection con = getConnection();
			PreparedStatement stmtPeriod = con.prepareStatement(
						"SELECT ts_start,coalesce(ts_end,now()) FROM drying_period WHERE period_id=? and peg_id=?");
		) {
			stmtPeriod.setInt(1, periodID);
			stmtPeriod.setInt(2, pegID);
			try (
					ResultSet rsPeriod = stmtPeriod.executeQuery();
			) {
				Timestamp tsStart, tsEnd;
				if (rsPeriod.next()) {
					tsStart = rsPeriod.getTimestamp(1);
					tsEnd = rsPeriod.getTimestamp(2);
				} else {
					System.out.println("Peg/Period combination does not exist. Use listPeriod parameter to show available periods");
					return;
				}
							
				try (
						PreparedStatement stmtMeasurements = con.prepareStatement(
								"SELECT * FROM measurement WHERE timestamp<? and timestamp>? and peg_id=? and sensor_type=? order by nr asc");
				) {
					stmtMeasurements.setTimestamp(1, tsEnd);
					stmtMeasurements.setTimestamp(2, tsStart);
					stmtMeasurements.setInt(3, pegID);
					stmtMeasurements.setString(4, sensor);
					try (
						ResultSet rs = stmtMeasurements.executeQuery();	
					) {
						while(rs.next()) {
							Timestamp tsPeriodStart = null;

							float conductance = rs.getFloat("conductance");
							float temperature = rs.getFloat("temperature");
							float humidity = rs.getFloat("humidity");
							
							// First measurement: Define period start
							if (tsPeriodStart == null) {
								tsPeriodStart = rs.getTimestamp("timestamp");
							}
							Timestamp measurementTs = rs.getTimestamp("timestamp");
								
							float prediction = 0.0f;
												
							// Store the first measuremnt with conductance under the threshold
							Timestamp tsDrySince = null;
														
							// Time (in seconds) how long the laundry has been dry.
							long dryFor = 0;
							if (conductance < 1.5f && tsDrySince == null) {
								tsDrySince = measurementTs;
							}
							
							if (tsDrySince != null) {
								dryFor = (measurementTs.getTime() - tsDrySince.getTime())/1000; 
							}
							
							// Only call machine learning if the laundry has not been dry for >5min by now
							if (dryFor < 300) {
								if (conductance > 1.5f) {
									tsDrySince = null;
								}
								// Now, we can create an input vector for the python script
								// Use MLVectorExtractor to convert the current measurement into an input vector for the
								// machine learning system.
								// The helper class is also used by the training data generator, so the process is the same.
								String inputVector = createInputVector(con, pegID, "HDC1080", measurementTs, tsPeriodStart);
								
								predictionInput.write(inputVector + "\r\n");
								predictionInput.flush();
											
								// Parse the output. The shellscript creates a line PREDICTION=... if successful
								String line = predictionOutput.readLine();
								if(line!=null) {
									prediction = Float.parseFloat(line);
								} else {
									throw new IOException("Connection to python subprocess died unexpectedly");
								}
														
							}
							
							// Remove negative predictions
							Math.max(0.0f, prediction);
							
							// Write output
							System.out.println(""+pegID+";"+periodID+";\""+sensor+"\";\""+measurementTs+"\";"+temperature+";"+humidity+";"+conductance+";"+prediction);
						}
					}
					
				}
			}
		}

		predictionInput.write("exit\n");
	}
	
	
	/**
	 * Prints available drying periods
	 * @throws SQLException Errors
	 */
	static void listPeriods() throws SQLException {
		try (
			Connection con = getConnection();
			PreparedStatement stmtPeriods = con.prepareStatement("SELECT * FROM drying_period ORDER BY period_id ASC");
			ResultSet rsPeriods = stmtPeriods.executeQuery()
		) {
			System.out.println("Peg     Period  Start                   End                     Dry at                  ");
			while(rsPeriods.next()) {
				System.out.format("%7d %7d ", rsPeriods.getInt("peg_id"),rsPeriods.getInt("period_id"));
				Timestamp tsStart = rsPeriods.getTimestamp("ts_start");
				if (tsStart != null) {
					Date d = new Date(tsStart.getTime());
					System.out.format("%td.%tm.%ty %tH:%tM:%tS       ", d, d, d, d, d, d);
				} else {
					System.out.print("-                      ");
				}
				Timestamp tsEnd = rsPeriods.getTimestamp("ts_end");
				if (tsEnd != null) {
					Date d = new Date(tsStart.getTime());
					System.out.format("%td.%tm.%ty %tH:%tM:%tS       ", d, d, d, d, d, d);
				} else {
					System.out.print("-                      ");
				}
				Timestamp tsDry = rsPeriods.getTimestamp("ts_dry");
				if (tsDry != null) {
					Date d = new Date(tsStart.getTime());
					System.out.format("%td.%tm.%ty %tH:%tM:%tS      %n", d, d, d, d, d, d);
				} else {
					System.out.println("-                      ");
				}
			}
		}
	}
	
	
}
