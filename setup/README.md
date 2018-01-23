# Setup instructions

This guide will help you setting up the project including the server. For development purposes, we used a Raspberry Pi (Version 3) as server. Other distributions or even OSs should work, but the further you get away from Debian-like systems, the more things will have to be adapted.

## Setting up the server

The server is a Java web application, so it needs a Java server that is at least capable of running the Web Profile. In our case, Tomcat 8 was quite useful. It also requires a JDK:

```
apt-get install tomcat8 openjdk-8-jdk
```

Besides that, we need a database to store measurements, known pegs etc. We used MySQL and MariaDB for this:

```
apt-get install mariadb-server mariadb-client
```

The server needs to have the **right JDBC-Connector in the classpath**, so for example download [mariadb-java-client-2.2.0.jar](https://downloads.mariadb.org/connector-java/2.2.0/) and put it into `/var/lib/tomcat8/lib/`.

Create a `smartpeg` database user with a secure password and his own database `smartpeg`. The setup script for the database as `api/databaseDef.sql` in the repository.

The newly-created database then has to be configured in the server. To do so, edit `/var/lib/tomcat8/conf/context.xml` and add the following lines:

```
<Resource name="jdbc/smartpeg" auth="Container"
	type="javax.sql.DataSource"
	description="Connection to MariaDB"
	username="smartpeg"
	password="the-secure-password-you-created-before"
	driverClassName="org.mariadb.jdbc.Driver"
	url="jdbc:mariadb://localhost:3306/smartpeg"
/>
```

For the python scripts (machine learning), we need to point the server to a script location. This may be anywhere in the system (however, the server needs write access), but to use it in combination with the auto deployment feature (see below), we put it in a path on the `smartpeg` user's home directory.

This location also needs to be configured in the `context.xml`:

```
<Environment name="pythonServerBase" value="/home/smartpeg/data/smartpeg-python" type="java.lang.String"/>
```

**Note:** For the development server started within the IDE, a sample local configuration file is provided within the workspace and included in the maven build file.

**Note:** This is a very simple configuration, where the SmartPeg app will be the only app deployed in the servers root. In a multi-app setup, it's __highly__ recommended to make the above configuration entries in an app-only visible context file.

After configuring everything, restart the server: `systemctl restart tomcat8`

## Python-Scripts

The `server` folder of the repository contains python scripts for machine learning and some previously trained models. They need to be located at a place where the `tomcat8` (managing the virtual environment) user may write, as well as the `smartpeg` user (auto deployment feature, see below).

We use a directory in the home-directory of the `smartpeg` user:

```
mkdir /home/smartpeg/data
mkdir /home/smartpeg/data/smartpeg-python
chown smartpeg:tomcat8 /home/smartpeg/data/smartpeg-python
chmod u=rw,g=rws,o=r /home/smartpeg/data/smartpeg-python
```

To populate the folder, either run auto deployment or copy the content of the `server` directory of the repository into `smartpeg-python`.

**Note:** The Java application will call a shell script (`predict.sh`). When setting up the application on Windows, this has to be changed in the Java code.

**Note:** The Python virtual environment is created on-the-fly when the first prediction should be made by the `predict.sh` shell script.

## Autodeployment

Auto deployment is a tool that updates Java and Python application everytime the `deploy-master` branch of the remote repository changes.

More details on it can be found in the autodeployment subfolder.

## Manual deployment

To deploy the application manually, run the following commands to build it (working directory: `api/rest` in the repository):

```
mvn clean
mvn install
```

Then, copy rename the created `.war`-file from the `target` directory to `ROOT.war` and drop it into `/var/lib/tomcat8/smartpeg/`

The server will update the application within some seconds.
