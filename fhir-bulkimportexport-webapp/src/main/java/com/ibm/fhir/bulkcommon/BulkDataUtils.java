/*
 * (C) Copyright IBM Corp. 2019, 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.bulkcommon;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Logger;

import com.ibm.cloud.objectstorage.ClientConfiguration;
import com.ibm.cloud.objectstorage.SDKGlobalConfiguration;
import com.ibm.cloud.objectstorage.auth.AWSCredentials;
import com.ibm.cloud.objectstorage.auth.AWSStaticCredentialsProvider;
import com.ibm.cloud.objectstorage.auth.BasicAWSCredentials;
import com.ibm.cloud.objectstorage.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.ibm.cloud.objectstorage.oauth.BasicIBMOAuthCredentials;
import com.ibm.cloud.objectstorage.services.s3.AmazonS3;
import com.ibm.cloud.objectstorage.services.s3.AmazonS3ClientBuilder;
import com.ibm.cloud.objectstorage.services.s3.model.AbortMultipartUploadRequest;
import com.ibm.cloud.objectstorage.services.s3.model.Bucket;
import com.ibm.cloud.objectstorage.services.s3.model.CannedAccessControlList;
import com.ibm.cloud.objectstorage.services.s3.model.CompleteMultipartUploadRequest;
import com.ibm.cloud.objectstorage.services.s3.model.GetObjectRequest;
import com.ibm.cloud.objectstorage.services.s3.model.InitiateMultipartUploadRequest;
import com.ibm.cloud.objectstorage.services.s3.model.InitiateMultipartUploadResult;
import com.ibm.cloud.objectstorage.services.s3.model.PartETag;
import com.ibm.cloud.objectstorage.services.s3.model.S3Object;
import com.ibm.cloud.objectstorage.services.s3.model.S3ObjectInputStream;
import com.ibm.cloud.objectstorage.services.s3.model.UploadPartRequest;
import com.ibm.cloud.objectstorage.services.s3.model.UploadPartResult;
import com.ibm.fhir.bulkimport.ImportTransientUserData;
import com.ibm.fhir.model.format.Format;
import com.ibm.fhir.model.parser.FHIRParser;
import com.ibm.fhir.model.parser.exception.FHIRParserException;
import com.ibm.fhir.model.resource.Resource;

/**
 * Utility functions for IBM COS.
 *
 */

public class BulkDataUtils {
    private final static Logger logger = Logger.getLogger(BulkDataUtils.class.getName());

    /**
     * Logging helper.
     */
    private static void log(String method, Object msg) {
        logger.info(method + ": " + String.valueOf(msg));
    }

    public static String startPartUpload(AmazonS3 cosClient, String bucketName, String itemName, boolean isPublicAccess) throws Exception {
        try {
            log("startPartUpload", "Start multi-part upload for " + itemName + " to bucket - " + bucketName);

            InitiateMultipartUploadRequest initMultipartUploadReq = new InitiateMultipartUploadRequest(bucketName, itemName);
            if (isPublicAccess) {
                initMultipartUploadReq.setCannedACL(CannedAccessControlList.PublicRead);
            }

            InitiateMultipartUploadResult mpResult = cosClient.initiateMultipartUpload(initMultipartUploadReq);
            return mpResult.getUploadId();
        } catch (Exception sdke) {
            log("startPartUpload", "Upload start Error - " + sdke.getMessage());
            throw sdke;
        }
    }

    public static AmazonS3 getCosClient(String cosCredentialIbm, String cosApiKeyProperty, String cosSrvinstId,
            String cosEndpintUrl, String cosLocation) {
        SDKGlobalConfiguration.IAM_ENDPOINT = "https://iam.cloud.ibm.com/oidc/token";
        AWSCredentials credentials;
        if (cosCredentialIbm.equalsIgnoreCase("Y")) {
            credentials = new BasicIBMOAuthCredentials(cosApiKeyProperty, cosSrvinstId);
        } else {
            credentials = new BasicAWSCredentials(cosApiKeyProperty, cosSrvinstId);
        }

        ClientConfiguration clientConfig = new ClientConfiguration()
                .withRequestTimeout(Constants.COS_REQUEST_TIMEOUT)
                .withTcpKeepAlive(true)
                .withSocketTimeout(Constants.COS_SOCKET_TIMEOUT);

        return AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withEndpointConfiguration(new EndpointConfiguration(cosEndpintUrl, cosLocation))
                .withPathStyleAccessEnabled(true).withClientConfiguration(clientConfig).build();
    }

    public static PartETag multiPartUpload(AmazonS3 cosClient, String bucketName, String itemName, String uploadID,
            InputStream dataStream, int partSize, int partNum) throws Exception {
        try {
            log("multiPartUpload", "Upload part " + partNum + " to " + itemName);

            UploadPartRequest upRequest = new UploadPartRequest().withBucketName(bucketName).withKey(itemName)
                    .withUploadId(uploadID).withPartNumber(partNum).withInputStream(dataStream).withPartSize(partSize);

            UploadPartResult upResult = cosClient.uploadPart(upRequest);
            return upResult.getPartETag();
        } catch (Exception sdke) {
            log("multiPartUpload", "Upload part Error - " + sdke.getMessage());
            cosClient.abortMultipartUpload(new AbortMultipartUploadRequest(bucketName, itemName, uploadID));
            throw sdke;
        }
    }

    public static void finishMultiPartUpload(AmazonS3 cosClient, String bucketName, String itemName, String uploadID,
            List<PartETag> dataPacks) throws Exception {
        try {
            cosClient.completeMultipartUpload(
                    new CompleteMultipartUploadRequest(bucketName, itemName, uploadID, dataPacks));
            log("finishMultiPartUpload", "Upload finished for " + itemName);
        } catch (Exception sdke) {
            log("finishMultiPartUpload", "Upload finish Error: " + sdke.getMessage());
            cosClient.abortMultipartUpload(new AbortMultipartUploadRequest(bucketName, itemName, uploadID));
            throw sdke;
        }
    }

    public static void listBuckets(AmazonS3 cosClient) {
        if (cosClient == null) {
            return;
        }
        log("listBuckets", "Buckets:");
        final List<Bucket> bucketList = cosClient.listBuckets();

        for (final Bucket bucket : bucketList) {
            log("listBuckets", bucket.getName());
        }
    }

    /**
     * @param resReader - the buffer reader to read FHIR resource from.
     * @param numOfProcessedLines - number of the already processed lines.
     * @param fhirResources - List holds the FHIR resources.
     * @param isSkipProcessed - if need to skip the processed lines before read.
     * @return - the number of parsing failures.
     * @throws Exception
     */
    private static int getFhirResourceFromBufferReader(BufferedReader resReader, int numOfProcessedLines, List<Resource> fhirResources,
            boolean isSkipProcessed, String dataSource) throws Exception {
        int exported = 0;
        int lineRed = 0;
        int parseFailures = 0;

        String resLine = null;
        do {
            resLine = resReader.readLine();
            if (resLine != null) {
                lineRed++;
                if (isSkipProcessed && lineRed <= numOfProcessedLines) {
                    continue;
                }
                try {
                    fhirResources.add(FHIRParser.parser(Format.JSON).parse(new StringReader(resLine)));
                    exported++;
                    if (exported == Constants.IMPORT_NUMOFFHIRRESOURCES_PERREAD) {
                        break;
                    }
                } catch (FHIRParserException e) {
                    // Log and skip the invalid FHIR resource.
                    parseFailures++;
                    logger.log(Level.INFO, "getFhirResourceFromBufferReader: " + "Failed to parse line "
                            + (numOfProcessedLines + exported + parseFailures) + " of [" + dataSource + "].", e);
                    continue;
                }
            }
        } while (resLine != null);
        return parseFailures;
    }

    public static void cleanupTransientUserData(ImportTransientUserData transientUserData, boolean isAbort) throws Exception {
        if (transientUserData.getInputStream() != null) {
            if (isAbort && transientUserData.getInputStream() instanceof S3ObjectInputStream) {
                // For S3 input stream, if the read is not finished successfully, we have to abort it first.
                ((S3ObjectInputStream)transientUserData.getInputStream()).abort();
            }
            transientUserData.getInputStream().close();
            transientUserData.setInputStream(null);
        }

        if (transientUserData.getBufferReader() != null) {
            transientUserData.getBufferReader().close();
            transientUserData.setBufferReader(null);
        }
    }

    /**
     * @param cosClient - COS/S3 client.
     * @param bucketName - COS/S3 bucket name to read from.
     * @param itemName - COS/S3 object name to read from.
     * @param numOfLinesToSkip - number of lines to skip before read.
     * @param fhirResources - List holds the FHIR resources.
     * @param transientUserData - transient user data for the chunk.
     * @return - number of parsing failures.
     * @throws Exception
     */
    public static int readFhirResourceFromObjectStore(AmazonS3 cosClient, String bucketName, String itemName,
           int numOfLinesToSkip, List<Resource> fhirResources, ImportTransientUserData transientUserData) throws Exception {
        int parseFailures = 0;
        int retryTimes = Constants.IMPORT_RETRY_TIMES;
        do {
            try {
                if (transientUserData.getBufferReader() == null) {
                    S3Object item = cosClient.getObject(new GetObjectRequest(bucketName, itemName));
                    S3ObjectInputStream s3InStream = item.getObjectContent();
                    transientUserData.setInputStream(s3InStream);
                    BufferedReader resReader = new BufferedReader(new InputStreamReader(s3InStream));
                    transientUserData.setBufferReader(resReader);
                    // Skip the already processed lines after opening the input stream for first read.
                    parseFailures = getFhirResourceFromBufferReader(transientUserData.getBufferReader(), numOfLinesToSkip, fhirResources, true, itemName);
                } else {
                    parseFailures = getFhirResourceFromBufferReader(transientUserData.getBufferReader(), numOfLinesToSkip, fhirResources, false, itemName);
                }
                break;
            } catch (Exception ex) {
                // Prepare for retry, skip all the processed lines in previous batches and this batch.
                numOfLinesToSkip = numOfLinesToSkip + fhirResources.size() + parseFailures;
                cleanupTransientUserData(transientUserData, true);
                logger.warning("readFhirResourceFromObjectStore: Error proccesing file [" + itemName + "] - " + ex.getMessage());
                if ((retryTimes--) > 0) {
                    logger.warning("readFhirResourceFromObjectStore: Retry ...");
                } else {
                    // Throw exception to fail the job, the job can be continued from the current checkpoint after the problem is solved.
                    throw ex;
                }
            }
        } while (retryTimes > 0);

        return parseFailures;
    }

    /**
     * @param filePath - file path to the ndjson file.
     * @param numOfLinesToSkip - number of lines to skip before read.
     * @param fhirResources - List holds the FHIR resources.
     * @param transientUserData - transient user data for the chunk.
     * @return - number of parsing failures.
     * @throws Exception
     */
    public static int readFhirResourceFromLocalFile(String filePath, int numOfLinesToSkip, List<Resource> fhirResources,
            ImportTransientUserData transientUserData) throws Exception {
        int parseFailures = 0;

        try {
            if (transientUserData.getBufferReader() == null) {
                BufferedReader resReader = Files.newBufferedReader(Paths.get(filePath));
                transientUserData.setBufferReader(resReader);
                // Skip the already processed lines after opening the input stream for first read.
                parseFailures = getFhirResourceFromBufferReader(transientUserData.getBufferReader(), numOfLinesToSkip, fhirResources, true, filePath);
            } else {
                parseFailures = getFhirResourceFromBufferReader(transientUserData.getBufferReader(), numOfLinesToSkip, fhirResources, false, filePath);
            }
        } catch (Exception ex) {
            // Clean up.
            fhirResources.clear();
            cleanupTransientUserData(transientUserData, true);
            // Log the error and throw exception to fail the job, the job can be continued from the current checkpoint after the problem is solved.
            logger.warning("readFhirResourceFromLocalFile: Error proccesing file [" + filePath + "] - " + ex.getMessage());
            throw ex;
        }

        return parseFailures;
    }


    /**
     * @param dataUrl - URL to the ndjson file.
     * @param numOfLinesToSkip - number of lines to skip before read.
     * @param fhirResources - List holds the FHIR resources.
     * @param transientUserData - transient user data for the chunk.
     * @return - number of parsing failures.
     * @throws Exception
     */
    public static int readFhirResourceFromHttps(String dataUrl, int numOfLinesToSkip, List<Resource> fhirResources,
            ImportTransientUserData transientUserData) throws Exception {
        int parseFailures = 0;
        int retryTimes = Constants.IMPORT_RETRY_TIMES;
        do {
            try {
                if (transientUserData.getBufferReader() == null) {
                    InputStream inputStream = new URL(dataUrl).openConnection().getInputStream();
                    transientUserData.setInputStream(inputStream);
                    BufferedReader resReader = new BufferedReader(new InputStreamReader(inputStream));
                    transientUserData.setBufferReader(resReader);
                    // Skip the already processed lines after opening the input stream for first read.
                    parseFailures = getFhirResourceFromBufferReader(transientUserData.getBufferReader(), numOfLinesToSkip, fhirResources, true, dataUrl);
                } else {
                    parseFailures = getFhirResourceFromBufferReader(transientUserData.getBufferReader(), numOfLinesToSkip, fhirResources, false, dataUrl);
                }
                break;
            } catch (Exception ex) {
                // Prepare for retry, skip all the processed lines in previous batches and this batch.
                numOfLinesToSkip = numOfLinesToSkip + fhirResources.size() + parseFailures;
                cleanupTransientUserData(transientUserData, true);
                logger.warning("readFhirResourceFromHttps: Error proccesing file [" + dataUrl + "] - " + ex.getMessage());
                if ((retryTimes--) > 0) {
                    logger.warning("readFhirResourceFromHttps: Retry ...");
                } else {
                    // Throw exception to fail the job, the job can be continued from the current checkpoint after the problem is solved.
                    throw ex;
                }
            }
        } while (retryTimes > 0);

        return parseFailures;
    }
}
