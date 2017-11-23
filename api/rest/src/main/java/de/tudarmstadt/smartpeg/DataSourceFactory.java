package de.tudarmstadt.smartpeg;

import com.mysql.cj.jdbc.MysqlDataSource;
import org.mariadb.jdbc.MariaDbDataSource;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

public class DataSourceFactory {

    public static DataSource getMySQLDataSource() {
        Properties props = new Properties();
        FileInputStream fis = null;
        MysqlDataSource mysqlDS = null;
        try {
            InputStream is = DataSourceFactory.class.getClassLoader().getResourceAsStream("db.properties");
            props.load(is);
            mysqlDS = new MysqlDataSource();
            mysqlDS.setURL(props.getProperty("MYSQL_DB_URL"));
            mysqlDS.setUser(props.getProperty("MYSQL_DB_USERNAME"));
            mysqlDS.setPassword(props.getProperty("MYSQL_DB_PASSWORD"));
        } catch (IOException e) {
            System.out.println("problem loading the datasource, please change the location of the db.properties file in the DataSourceFactory class.");
            e.printStackTrace();
        }
        return mysqlDS;
    }

    public static DataSource getMariaDBDataSource(){
        Properties props = new Properties();
        FileInputStream fis = null;
        MariaDbDataSource mariaDS = null;
        try {
            InputStream is = DataSourceFactory.class.getClassLoader().getResourceAsStream("db.properties");
            props.load(is);
            mariaDS = new MariaDbDataSource();
            mariaDS.setURL(props.getProperty("MARIA_DB_URL"));
            mariaDS.setUser(props.getProperty("MARIA_DB_USERNAME"));
            mariaDS.setPassword(props.getProperty("MARIA_DB_PASSWORD"));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return mariaDS;
    }

}