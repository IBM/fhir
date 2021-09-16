## fhir-bucket - Synthetic Data Loader
Scans cloud object storage buckets and uploads data using the FHIR REST API.

In addition to data loading, the app also can:

* Make FHIR read/search calls at high volume to stress the read performance of the server.
* Drive the $reindex custom operation using concurrent requests. The reindex operation is needed when the search parameter configuration is changed and the user wants to update the extracted parameter values which are used to support indexes.

### Background

Synthea is a project for generating "synthetic" patient/population data for healthcare applications.
It generates realistic data based on census statistics and a lot of configuration.
It supports generating data in FHIR R4 

This "fhir-bucket" project will help you upload Synthea-generated data to a FHIR R4 server.

To facilitate high-volume load scenarios, multiple instances of the application can be run and their work coordinated so that each file is loaded exactly once.

The loader records the identities of the created resources. These identities can be used in other test and load generator applications to access the data.

### Steps

1. Follow the steps at https://github.com/synthetichealth/synthea to clone and install Synthea
2. Configure Synthea to generate FHIR R4 resources
3. Generate a bunch of patients
4. Clone this repo and set it up with Maven/Eclipse
5. Configure the truststore with root certs required to trust connections to your FHIR server, cloud object store and tracking database
5. Tweak fhir.properties to point at your target FHIR server
6. Tweak cos.properties to point at your COS bucket
7. Tweak db.properties to point at your tracking database
8. Execute Main.class as described in the Running section below


### Configuration

#### The cos.properties file

```
# the IBM COS API key or S3 access key.
cos.api.key=

# the IBM COS service instance id or S3 secret key.
cos.srvinstid=

# the IBM COS or S3 End point URL.
cos.endpoint.url=

# the IBM COS or S3 location.
cos.location=

# the IBM COS or S3 bucket name to import from.
cos.bucket.name=

# if use IBM credential(Y/N), default(Y).
cos.credential.ibm=Y

# COS network timeouts
cos.request.timeout=60000
cos.socket.timeout=60000

# The number of COS keys (items) to fetch per read
cos.max.keys=1000
```

#### The db2.properties file

```
db.host=<DB2-HOST-NAME>
db.port=<DB2-PORT>
db.database=<DB2-DATABASE>
user=<DB2-USER>
password=<DB2-PASSWORD>
sslConnection=true
sslTrustStoreLocation=/path/to/dbTruststore.p12
sslTrustStorePassword=<TRUSTSTORE-PASSWORD>
currentSchema=FHIRBUCKET
```

#### The postgres.properties file

```
db.host=<POSTGRES-HOST-NAME>
db.port=<POSTGRES-PORT>
db.database=<POSTGRES-DATABASE>
user=<POSTGRES-USER>
password=<POSTGRES-PASSWORD>
sslrootcert=/path/to/postgres.cert
ssl=true
sslmode=require
currentSchema=FHIRBUCKET
```

#### The derby.properties file

Db2 and PostgreSQL are the preferred databases for hosting the fhir-bucket schema. Derby can, however, be used for development. The derby.properties file must be configured as follows:

```
db.database=derby/bucketDB
db.create=Y
```

The name of the Derby database can be anything (without spaces) but must be contained within a folder called "derby".

### Schema Deployment

As a one-time activity, create the schema objects using the following command:

```
#!/usr/bin/env bash

JAR="~/git/FHIR/fhir-bucket/target/fhir-bucket-4.4.0-SNAPSHOT-cli.jar"

java -jar "${JAR}"               \
  --db-type db2                  \
  --db-properties db2.properties \
  --create-schema
```

If using a local Derby instance:

```
#!/usr/bin/env bash

JAR="~/git/FHIR/fhir-bucket/target/fhir-bucket-4.4.0-SNAPSHOT-cli.jar"

java -jar "${JAR}"                 \
  --db-type derby                  \
  --db-properties derby.properties \
  --create-schema
```

If using a local PostgreSQL instance:

```
#!/usr/bin/env bash

JAR="~/git/FHIR/fhir-bucket/target/fhir-bucket-4.4.0-SNAPSHOT-cli.jar"

java -jar "${JAR}"                    \
  --db-type postgresql                \
  --db-properties postgres.properties \
  --create-schema
```

This tracking database can be shared with the instance used by FHIR, but for proper performance testing it should be on a separate host. The standard schema for the tables is FHIRBUCKET.


### Running

The following script can be used to run the bucket loader from a local build:

```
#!/usr/bin/env bash

JAR="~/git/FHIR/fhir-bucket/target/fhir-bucket-4.4.0-SNAPSHOT-cli.jar"

java -jar "${JAR}"                  \
  --db-type db2                     \
  --db-properties db2.properties    \
  --cos-properties cos.properties   \
  --fhir-properties fhir.properties \
  --bucket example-bucket           \
  --tenant-name example-tenant      \
  --file-type NDJSON                \
  --max-concurrent-fhir-requests 40 \
  --max-concurrent-json-files 10    \
  --max-concurrent-ndjson-files 1   \
  --connection-pool-size 20         \
  --incremental
```

To run using Derby, change the relevant arguments to:

```
...
  --db-type derby                   \
  --db-properties derby.properties  \
...
```

To run using PostgreSQL, change the relevant arguments to:

```
...
  --db-type postgresql                 \
  --db-properties postgres.properties  \
...
```

| command-line options |
| ------- |
| `--incremental` </br> If the loader is stopped or fails before a bundle completes, the bundle will be reclaimed by another loader instance after the heartbeat timeout expires (60s). If the `--incremental` option is specified, the loader skips lines already processed in the NDJSON file. This is reasonably quick but is approximate, and may end up skipping rows due to threaded processing when the loader terminated. |
| `--incremental-exact` </br> the FHIRBUCKET tracking database is checked for every line in the NDJSON to see if any resources have been recorded for it, and if so, processing will be skipped. |
| `--db-type type` </br> where `type` is one of: db2, derby, postgresql. Specifies the type of database to use for the FHIRBUCKET tracking data. |
| `--db-properties properties-file` </br>  Connection properties file for the database |
| `--cos-properties properties-file` </br>  Connection properties file for COS | 
| `--fhir-properties properties-file` </br> Connection properties file for the FHIR server |
| `--bucket cos-bucket-name` </br> The bucket name in COS |
| `--tenant-name fhir-tenant-name` </br> The IBM FHIR Server tenant name|
| `--file-type file-type` </br> One of: JSON, NDJSON. Used to limit the discovery scan to a particular type of file/entry |       
| `--max-concurrent-ndjson-files pool-size` </br> The maximum number of NDJSON files to read in parallel. Typically a small number, like the default which is 1. |
| `--max-concurrent-json-files pool-size` </br> The maximum number of JSON files to read in parallel. Each JSON file translates to a single FHIR request, which may be a single resource, or a bundle with many resources. |
| `--max-concurrent-fhir-requests pool-size` </br> The maximum number concurrent FHIR requests. For example, an NDJSON file may contain millions of records. Although a single NDJSON file is read sequentially, each resource (row) can be processed in parallel, up to this limit |
| `--connection-pool-size pool-size` </br> The maximum size of the database connection pool. Threads will block and wait if the current number of active connections exceeds this value |
| `--recycle-seconds seconds` </br> Artificially force discovered entries to be reloaded some time after they have been loaded successfully. This permits the loader to be set up in a continuous mode of operation, where the resource bundles are loaded over and over again, generating new resources to fill the target system with lots of data. The processing times for each load is tracked, so this can be used to look for regression.
| `--cos-scan-interval-ms millis` </br> The number of milliseconds to wait before scanning the COS bucket again to discover new entries |
| `--path-prefix prefix` </br> Limit the discovery scan to keys with the given prefix. |
| `--pool-shutdown-timeout-seconds seconds` </br> How many seconds to wait for the resource pool to shutdown when the loader has been asked to terminate. This value should be slightly longer than the Liberty transaction timeout.
| `--create-schema` </br> Creates a new or updates an existing database schema. The program will exit after the schema operations have completed.|




### Internals

The purpose of fhir-bucket is to exercise the ingestion capability of the IBM FHIR Server (or any FHIR Server, for that matter). It scans IBM Cloud Object Store using the S3 connector and registers each matching entry in a tracking database.

This tracking database is used to allocate these discovered entries (resource bundles) to loaders with free capacity. Several loader instances (JVMs) can be run in parallel and will coordinate their work using the common tracking database.

When an instance of the fhir-bucket loader starts, it registers itself by creating a new entry in the loader_instances table. It periodically updates the heartbeat_tstamp of this record to publicize its liveness. Periodically, loader instances will perform a liveness check, looking for any other instances with an old heartbeat_tstamp. If the timestamp is considered too old, then the respective loader instance is considered to have failed. Any jobs it was running or had been assigned are cleared so that they can be picked up by another loader instance and hopefully completed successfully (see ClearStaleAllocations class).

The COS scanner periodically scans COS to look for new items. It also checks each current item to see if the signature (size, modified time or etag hash) has changed. If a change is detected, the version number is incremented and any current allocation is cleared (recinded).

The job allocation is performed by a thread in the `CosReader` class. This thread loops, asking the database to assign it new work when the loader has free capacity (see the AllocateJobs class). If the database assigns fewer jobs than the available capacity, this indicates we've run out of work to do so the loop will sleep for a short while before asking again so as not to overload the database.

Each time a bundle is allocated to a loader for processing, a new record in RESOURCE_BUNDLE_LOADS is created. Any logical ids or errors generated during the processing of the bundle are recorded against the specific RESOURCE_BUNDLE_LOADS record. This permits tracking of multiple loads of the same bundle over time. The same bundle may be loaded multiple times if previous loads did not complete, or if recycling is enabled.

Processing of files/entries from COS is performed with two thread pools. The first thread pool is used to parallelize the processing of individual files. This is useful when there are large numbers of files, e.g. one per patient.

The second thread pool is used to process resources read from a file/entry. This is useful when the entries are NDJSON files which may contain millions of entries. The reader reads and validates each resource then submits the resource to the thread pool to parallelize the calls to FHIR.

Limits are placed on the number of files/entries which can be allocated to a particular instance, as well as the number of resources which are currently inflight waiting to be processed. This is important to avoid unbounded memory growth. The goal is to keep the thread pool as busy as possible without consuming too much memory by reading and queueing too many resources (the assumption is that the fhir-bucket loader can read and validate resources more quickly than the FHIR server can process them).

If the resource is a Bundle, then FHIR returns a bundle containing the newly assigned logical ids of every resource created from that bundle. Each of these logical ids is recorded in the LOGICAL_RESOURCES table in the tracking database.

If the resource file/entry is not a Bundle, then FHIR returns the newly assigned logical id in the Location header. This value is also stored in the LOGICAL_RESOURCES table in the tracking database.


### Metrics

The FHIRBUCKET schema tracks some useful statistics captured during the load runs which can be used to analyze performance.

#### Schema

| Table Name | Description |
| ---------- | ----------- |
| loader_instances | Each time a loader starts up it is allocated a unique loader_instances record |
| bucket_paths | The bucket name and item path up to the last / to avoid repeating the long path string for every item |
| resource_bundles | Represents a JSON or NDJSON file found in COS |
| resource_bundle_loads | Records each attempt to load a particular bundle |
| resource_bundle_errors | records errors by line. Includes HTTP response if available |
| resource_types | The FHIR model resource names |
| logical_resources | Holds the logical id for every resource created by the FHIR server |

See the com.ibm.fhir.bucket.persistence.FhirBucketSchema class for details (columns and relationships) on the above tables.

The `resource_bundle_loads` table contains timestamp fields marking the start and end of processing. The end time is only updated if the bundle is completed before the loader is stopped. 

Each `logical_resources` record contains a `created_tstamp` column which marks the time when the record was created in the FHIRBUCKET database.



#### Db2 Analytic Queries

To compute an approximate resources-per-second rate for each NDJSON bundle:
```
SET CURRENT SCHEMA FHIRBUCKET;

SELECT loader_instance_id, substr(object_name, 1, 24) object_name, resource_type, resource_count, resource_count / run_seconds AS resources_per_second,
       timestampdiff(2, bundle_end - bundle_start) bundle_duration
  FROM (
       SELECT lr.loader_instance_id, resource_type_id, rb.object_name, count(*) AS resource_count,
              timestampdiff(2, max(lr.created_tstamp) - min(lr.created_tstamp)) run_seconds,
              min(rb.load_started) bundle_start,
              max(rb.load_completed) bundle_end
         FROM logical_resources lr,
              resource_bundles rb
        WHERE lr.loader_instance_id IS NOT NULL
          AND rb.resource_bundle_id = lr.resource_bundle_id
          AND rb.load_completed IS NOT NULL
     GROUP BY lr.loader_instance_id, resource_type_id, rb.object_name
     ) lr,
       resource_types rt
 WHERE rt.resource_type_id = lr.resource_type_id
   AND lr.run_seconds > 0
;
```

The resource rate is calculated using the first and last creation timestamps from the logical_resources table.

#### PostgreSQL Analytic Queries

PostgreSQL uses a different mechanism for calculating the gap between two timestamps:

```
SELECT abs(EXTRACT(EPOCH FROM end - start)) AS gap_in_seconds
  FROM ...
```

The resource rate approximation query therefore becomes:

```
SET search_path=FHIRBUCKET,PUBLIC;

SELECT loader_instance_id, substr(object_name, 1, 24) object_name, resource_type, resource_count, resource_count / run_seconds AS resources_per_second,
       EXTRACT(EPOCH FROM bundle_end - bundle_start) AS bundle_duration
  FROM (
       SELECT lr.loader_instance_id, resource_type_id, rb.object_name, count(*) AS resource_count,
              EXTRACT(EPOCH FROM max(lr.created_tstamp) - min(lr.created_tstamp)) AS run_seconds,
              min(rb.load_started) bundle_start,
              max(rb.load_completed) bundle_end
         FROM logical_resources lr,
              resource_bundles rb
        WHERE lr.loader_instance_id IS NOT NULL
          AND rb.resource_bundle_id = lr.resource_bundle_id
          AND rb.load_completed IS NOT NULL
     GROUP BY lr.loader_instance_id, resource_type_id, rb.object_name
     ) lr,
       resource_types rt
 WHERE rt.resource_type_id = lr.resource_type_id
   AND lr.run_seconds > 0
;
```


## Configuring Logging

Use `-Djava.util.logging.config.file=logging.properties` as the first argument on the command line to configure Java Util Logging using the `logging.properties` file. An example of this file is given below:

```
handlers=java.util.logging.ConsoleHandler,java.util.logging.FileHandler
.level=INFO

# Minimal console output
java.util.logging.ConsoleHandler.level = INFO
java.util.logging.ConsoleHandler.formatter=com.ibm.fhir.database.utils.common.LogFormatter

# INFO to the log file, unless you want to see more
java.util.logging.FileHandler.level=INFO

# 50MB * 20 files ~= 1GB of log retention
java.util.logging.FileHandler.formatter=com.ibm.fhir.database.utils.common.LogFormatter
java.util.logging.FileHandler.limit=50000000
java.util.logging.FileHandler.count=20
java.util.logging.FileHandler.pattern=fhirbucket-%u-%g.log


# See FINE stuff for the scanner
#com.ibm.fhir.bucket.scanner.level=FINE
```



## Driving the `$reindex` Custom Operation

When the IBM FHIR Server stores a FHIR resource, it extracts a configurable set of searchable parameter values and stores them in specially indexed tables which are used to support search queries. When the search parameter configuration is changed (perhaps because a profile has been updated), users may want to apply this new configuration to resources already stored. By default, such configuration changes only apply to new resources.

The IBM FHIR Server supports a custom operation to rebuild or "reindex" the search parameters extracted from resources currently stored. There are two approaches for driving the reindex, server-side-driven or client-side-driven. Using server-side-driven is the default; to use client-side-driven, include the `--index-client-side-driven` parameter.

With server-side-driven, the fhir-bucket will repeatedly call the `$reindex` operation. The user selects a date or timestamp as the reindex "marker", which is used to determine which resources have been reindexed, and which still need to be reindexed. When a resource is successfully reindexed, it is marked with this user-selected timestamp. Each reindex REST call will process up to the requested number of resources and return an OperationOutcome resource containing issues describing which resources were processed. When there are no resources left to update, the call returns an OperationOutcome with one issue, with an issue diagnostic value "Reindex complete", indicating that the reindex is complete.

With client-side-driven, the fhir-bucket will repeatedly call two operations in parallel; the `$retrieve-index` operation to determine the list of resources available to reindex, and the `$reindex` operation with a list of resources to reindex. Driving the reindex this way avoids database contention associated with updating the reindex timestamp of each resource with reindex "marker", which is used by the server-side-driven approach to keep track of the next resource to reindex.

To avoid read timeouts, the number of resources processed in a single reindex call can be limited. Reindex calls can be made in parallel to increase throughput. The best number for concurrent requests depends on the capabilities of the underlying platform and any desire to balance load with other users. Concurrency up to 200 threads have been tested. Monitor the IBM FHIR Server response times when increasing concurrency. Also, make sure that the connection pool configured in the FHIR server cluster can support the required number of threads. This also means that the database needs to be configured to support this number of connections (sessions) plus any overhead.

The fhir-bucket main app has been extended to support driving a reindex operation with high concurrency. 

```
java \
  -Djava.util.logging.config.file=logging.properties \
  -jar "${JAR}" \
  --fhir-properties your-fhir-server.properties \
  --tenant-name your-tenant-name \
  --max-concurrent-fhir-requests 100 \
  --no-scan \
  --reindex-tstamp 2020-12-01T00:00:00Z \
  --reindex-resource-count 50 \
  --reindex-concurrent-requests 20 \
  --reindex-client-side-driven
```

The format of the reindex timestamp can be a date `YYYY-MM-DD` representing `00:00:00` UTC on the given day, or an ISO timestamp `YYYY-MM-DDThh:mm:ssZ`.

Values for `--reindex-resource-count` larger than 1000 will be clamped to 1000 to ensure that the `$reindex` server calls return within a reasonable time.

The value for `--reindex-concurrent-requests` can be increased/decreased to maximize throughput or avoid overloading a system. The number represents the total number of client threads used to invoke the $reindex operation. Each thread uses its own connection to the IBM FHIR Server so you must also set `--max-concurrent-fhir-requests` to be at least equal to `--reindex-concurrent-requests`.
