# MUC Extended Info Plugin for Openfire

Adds additional info to MUC rooms for discovery purpose.

Homepage: https://www.igniterealtime.org/projects/openfire/plugin-archive.jsp?plugin=mucextinfo

## Running local builds

To build: `mvn clean package`

Then, for running Openfire installations:
* `mv target/mucextinfo-openfire-plugin-assembly.jar target/mucextinfo.jar`
* upload target/mucextinfo.jar to https://your-openfire-server:9090/plugin-admin.jsp

For local Openfire builds:
* `cp target/mucextinfo-openfire-plugin-assembly.jar /path/to/git/Openfire/distribution/target/distribution-base/plugins/mucextinfo.jar`
