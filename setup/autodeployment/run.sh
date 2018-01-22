#!/bin/bash
# go to repository folder
cd ~/repositories/smartpeg

ALL_RETURN=0

# do a pull
git pull

LASTCOMMIT=`cat ~/cron/autodeploy-smartpeg-api/lasthash.conf`
CURRENTCOMMIT=`git log -1 --format="%H"`

echo "Checking if the repository has been updated:"
echo "   Last commit:    $LASTCOMMIT"
echo "   Current commit: $CURRENTCOMMIT"
echo ""

# Check for update
if [ "$LASTCOMMIT" == "$CURRENTCOMMIT" ]; then
	# Nothing to do here
	echo "Nothing has changed in the repository"
else
	# Recompile the application
	echo "Repository update detected. Recompiling..."

	cd ~/repositories/smartpeg/api/rest

	echo "Running mvn clean..."
	mvn clean
	echo "Clean done. Running mvn install to build the war file from scratch..."
	mvn install
	MVN_RETURN=$?
	ALL_RETURN=$?

	echo

	# Redeploy (only on successful build)
	if (( $MVN_RETURN == 0 )); then
		echo "Successful build."

		echo "Updating python application."

		echo "Deleting previous version of the server files"
		rm -rf /home/smartpeg/data/smartpeg-python/*
		PYTHON_RETURN=$?

		if (( PYTHON_RETURN == 0 )); then
			echo "Copying new files from repository"
			cp -r /home/smartpeg/repositories/smartpeg/server/* /home/smartpeg/data/smartpeg-python/
			PYTHON_RETURN=$?
		else
			echo "Deleting old version of the python directory failed. Aborting."
		fi

		if (( PYTHON_RETURN == 0 )); then
			echo "Creation of python directory was successful"

			echo "Redeploying web application."

			POM_GROUPID=`mvn org.apache.maven.plugins:maven-help-plugin:2.2:evaluate -Dexpression=project.groupId |grep -Ev '(^\[|Download\w+:)'`
			POM_ARTIFACTID=`mvn org.apache.maven.plugins:maven-help-plugin:2.2:evaluate -Dexpression=project.artifactId |grep -Ev '(^\[|Download\w+:)'`
			POM_VERSION=`mvn org.apache.maven.plugins:maven-help-plugin:2.2:evaluate -Dexpression=project.version |grep -Ev '(^\[|Download\w+:)'`

			echo "Deploying:"
			echo "   Group ID:      $POM_GROUPID"
			echo "   Artifact ID:   $POM_ARTIFACTID"
			echo "   Version:       $POM_VERSION"

			ARTIFACT_FILE="$HOME/.m2/repository/"$(echo "$POM_GROUPID.$POM_ARTIFACTID" | sed "s/\\./\//g")"/$POM_VERSION/$POM_ARTIFACTID-$POM_VERSION.war"

			echo "   Artifact file: $ARTIFACT_FILE"

			cp "$ARTIFACT_FILE" "/var/lib/tomcat8/smartpeg/ROOT.war"
			DEPLOY_RETURN=$?

			if (( DEPLOY_RETURN == 0 )); then
				echo "Deployment complete"
			else
				ALL_RETURN=2
				echo "Deployment failed"
			fi
		else
			echo "Build not successful"
			ALL_RETURN=1
		fi
	else
		echo "Copying Python directory did not work"
		ALL_RETURN = 3
	fi

	# Notify all
	if (( $ALL_RETURN == 0)); then
		echo "Sending notifications"
		for MAILADDRESS in `cat ~/cron/autodeploy-smartpeg-api/mail.addresses.notify`; do
			echo "Sending mail to $MAILADDRESS"
			/usr/sbin/sendmail "$MAILADDRESS" < ~/cron/autodeploy-smartpeg-api/mail.template.notify
		done

	else
		echo "Sending failure notifications"
		for MAILADDRESS in `cat ~/cron/autodeploy-smartpeg-api/mail.addresses.notify`; do
			echo "Sending mail to $MAILADDRESS"
			/usr/sbin/sendmail "$MAILADDRESS" < ~/cron/autodeploy-smartpeg-api/mail.template.notify.failure
		done
	fi

	echo "Replacing current commit with $CURRENTCOMMIT"
	echo "$CURRENTCOMMIT" > ~/cron/autodeploy-smartpeg-api/lasthash.conf
fi

echo "Done."

exit $ALL_RETURN
