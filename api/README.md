# API

This folder contains the [Swagger](https://swagger.io) API definition.

The API provides a REST interface for the smart peg to report its values, as well as an interface for the app to request current measurement values from the database and prediction times based on the machine learning process.

## Editing the API

While swagger can be installed locally, the easiest way is probably to use the [Online Editor](https://editor.swagger.io/). The smartpeg.yaml file can be copied from the repository and pasted into the editor, be edited, and then copied to the repository again.

The online editor also allows easy generation of code stubs. Be aware that for the default setup this requires a hostname to be defined in the interface definition file.

There are two ways to handle this for the final project:

- Use a real, DNS-resolvable hostname (either by creating a real DNS entry - I could provide smartpeg.fhessel.de) or by setting up a test environment consisting of DHCP and local DNS server, that handels the fake smart.peg DNS name that is currently configured
- Ignore the name defined in the yaml-file and change the target in the code (less dynamic, as it requires reconfiguration for every change of destination host)
