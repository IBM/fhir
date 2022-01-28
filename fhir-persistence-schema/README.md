# IBM FHIR Server - fhir-persistence-schema

Builds and manages the multi-tenant FHIR R4 RDBMS schema for Db2 and PostgreSQL and includes Derby support for testing.

This module is built into two different jar files. The default jar is included with the IBM FHIR Server web application and is used for bootstrapping Apache Derby databases (if configured). There is also an executable command line interface (cli) version of this jar that packages this module with all of its dependencies.

The executable command line interface (cli) version of this module can be downloaded from the project [Releases tab](https://github.com/IBM/FHIR/releases).

The following guides contain detailed descriptions on usage, design and the multi-tenant variant used with Db2:

* [Schema Deployment and Upgrade Guide](https://github.com/IBM/FHIR/tree/main/fhir-persistence-schema/docs/SchemaToolUsageGuide.md)
* [Schema Design](https://github.com/IBM/FHIR/tree/main/fhir-persistence-schema/docs/SchemaDesign.md)
* [Db2 Multi-tenancy](https://github.com/IBM/FHIR/tree/main/fhir-persistence-schema/docs/DB2MultiTenancy.md)

---------
## TL;DR

1. Create a properties file containing your database connection information:

```
$ cat fhirdb.properties
db.host=localhost
db.port=5432
db.database=fhirdb
user=postgres
password=change-password
```

2. Run the schema tool CLI to create the target schema (for example `fhirdata`). Skip this step if a DBA has already created a schema for you.

``` shell
java -jar ./fhir-persistence-schema-${VERSION}-cli.jar \
  --db-type postgresql \
  --prop-file fhirdb.properties \
  --schema-name fhirdata \
  --create-schemas
```

Note: Replace `${VERSION}` with the version of the jar you're using or use the wildcard `*` to match any version.

3. Run the schema tool CLI again to create the tables and indexes in the `fhirdata` schema. We recommend following the least-privilege access model, so the IBM FHIR Server should connect using a non-admin user. Use the `--grant-to` option to grant the correct privileges to the non-admin user created for the IBM FHIR Server (the user `fhirserver` in the following example):

``` shell
java -jar ./fhir-persistence-schema-${VERSION}-cli.jar \
  --db-type postgresql \
  --prop-file fhirdb.properties \
  --schema-name fhirdata \
  --update-schema \
  --grant-to fhirserver \
  --pool-size 1
```


4. To upgrade the schema for a new release, run the schema tool CLI again. The tool handles version tracking and will apply all the necessary changes to roll forward to the latest version:

``` shell
java -jar ./fhir-persistence-schema-${VERSION}-cli.jar \
  --db-type postgresql \
  --prop-file fhirdb.properties \
  --schema-name fhirdata \
  --update-schema \
  --grant-to fhirserver \
  --pool-size 1
```

For details on configuring TLS and using other databases and options, read the full [Schema Deployment and Upgrade Guide](https://github.com/IBM/FHIR/tree/main/fhir-persistence-schema/docs/SchemaToolUsageGuide.md).

FHIR® is the registered trademark of HL7 and is used with the permission of HL7.
