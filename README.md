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

## CI Build Status

[![Build Status](https://github.com/igniterealtime/openfire-mucextinfo-plugin/workflows/Java%20CI/badge.svg)](https://github.com/igniterealtime/openfire-mucextinfo-plugin/actions)

## Reporting Issues

Issues may be reported to the [forums](https://discourse.igniterealtime.org) or via this repo's [Github Issues](https://github.com/igniterealtime/openfire-mucextinfo-plugin).

