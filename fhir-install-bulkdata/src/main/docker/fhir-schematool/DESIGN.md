# **Design**

The LinuxForHealth FHIR Schema Tool provides an Docker image that wraps the `fhir-persistence-schema` executable jar. The tool is state machine which take a single configuration to establish a current and running state of the LinuxForHealth FHIR Server.

The design is such that it COULD, but does not support multiple input files at this time.

When the container is started, the container process one of two flows:

* Schema Onboarding - creates and updates a schema and apply grants
* Schema Offboarding - drops a schema

**Schema Onboarding Flow**

The schema onboarding flow setup add a new tenant or update the database to the latest schema levels. 

1. Check if set to **skip**: 
    1. If `SKIP['true']`, stop and exit cleanly. 
    1. If `SKIP['false']`, proceed.
1. Check if **BEHAVIOR** set to ONBOARD:
    1. If not `BEHAVIOR['ONBOARD']`, skip. 
    1. If `BEHAVIOR['ONBOARD']`, proceed.
1. Create the database configuration file
1. Check connectivity: 
    1. If connectivity fails, stop and exit in error. 
    1. If connectivity succeeds, proceed.
1. Create the schema
1. Update the Schema 
1. Grant permissions to the LinuxForHealth FHIR Server database user

**Schema Offboarding Flow**

The schema offboarding flow offboards the current schema, while preserving the schema for multiple tenants (if supported by the database type).

1. Check if set to **SKIP**: 
    1. If `SKIP['true']`, stop and exit cleanly. 
    1. If `SKIP['false']`, proceed.
1. Check if **BEHAVIOR** set to OFFBOARD:
    1. If not `BEHAVIOR['OFFBOARD']`, skip. 
    1. If `BEHAVIOR['OFFBOARD']`, proceed.
    1. Drop FHIR Schema
    1. Drop Java Batch
    1. Drop OAuth

# **Implementation Details**

## Configuration

The configuration is drived primarily from a working directory, and in alternative circumstances backed by an Environment variable with the input.

The configuration data is mounted to `/fhir-schematool/workarea/input`.

## Logging
1. Logging - The logging is to standard error and standard out. 
 - The logs data is put into the `/fhir-schematool/workarea/output` folder.
2. Must Gather Flow - Each execution generates a new set of log files to the `/fhir-schematool/workarea/output` folder.

## Docker 
The design is to use as few layers as possible starting with `ibmsemeruruntime/open-11-jdk:ubi_min-jre`. 
The build uses multiple stages to avoid a bloated image after the necessary updates.