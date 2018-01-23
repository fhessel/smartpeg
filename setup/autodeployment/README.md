# Autodeployment

This folder contains all scripts that are required to deploy changes in the repository automatically to the server.

The script ...
- Pulls from git
- Compares the hashcodes of the previous and the new latest commit, and stops if nothing has changed
- When a change has been detected, the script
  - Performs a maven clean on the Java source code directory
  - Performs a maven build on the Java source code directory
  - Deletes the local python folder
  - Copys everything from the server directory of the repository to the local python folder
  - Deploys the latest build of the Java web application to the local tomcat8 server
- Finally, depending of the outcome of the deployment process, a success or failure notice is sent to all recipients listed in `mail.addresses.notify`

**Note:** It might be a good idea NOT to run this script directly from the local copy of the repository as it might override itself during auto deployment with undefined behavior. Furthermore, the script creates temporary files in this directory that are not included from version control, which may lead to failing pulls.

**Note:** The script is configured to keep up to the **branch** `deploy-api`, so that not every change to the master automatically gets pulled by the server.

## Setup of the script

Checkout the project's repository at `~/repositories/smartpeg` in the home of your `smartpeg` user.

Copy the whole `autodeployment` folder to `~/cron/autodeploy-smartpeg-api` in the home directory of your `smartpeg` user.

Check all template-files etc. for optional adjustment (e.g. you want to change the mail address in the notification templates)

If not yet done, `apt-get install tomcat8 openjdk-8-jdk maven sendmail python3 python-virtualenv` and configure sendmail properly.

Make sure, the `smartpeg` user is allowed to write to `/var/lib/tomcat8/smartpeg/` and that it is member of the group `tomcat8` to delete temporary files created while running the scripts.

To activate the script, create a cron job to call the `cron.sh` script regularly, for example, if you wish a check for updates every 10 minutes, you can use the following line:

```
#  m   h  dom   mon   dow   command
*/10   *    *     *     *   /home/smartpeg/cron/autodeploy-smartpeg-api/cron.sh > /dev/null
```

The `smartpeg` user needs to be able to (at least) read the git repository to do the pull. In practice, GitHub's _deployment keys_ feature might become handy here.
