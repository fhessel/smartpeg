package de.tud.ami.smartpeg;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnector {

    static final String JDBC_DRIVER = "org.mariadb.jdbc.Driver";
    static final String DB_URL = "jdbc:mariadb://<your-ip-here>/smartpeg";
	
    static {
    	try {
    		Class.forName("org.mariadb.jdbc.Driver");
    	} catch (Exception e) {
    		System.err.println("Cannot load MariaDB Driver");
    		e.printStackTrace();
    	}
    }
    
    public static Connection getConnection() throws SQLException {
    	return DriverManager.getConnection(
    			DB_URL, "<your-user-here>", "<your-password-here>");
    }
    
}
