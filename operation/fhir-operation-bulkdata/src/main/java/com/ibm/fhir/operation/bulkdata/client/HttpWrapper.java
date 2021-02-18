/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.fhir.operation.bulkdata.client;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;

/**
 *
 */
public class HttpWrapper {
    private static final String CLASSNAME = HttpWrapper.class.getName();
    private static final Logger log = Logger.getLogger(CLASSNAME);

    private static final int TIMEOUT = 10000;

    private static final SSLConnectionSocketFactory sf = generateSSF();
    private static final HttpRequestRetryHandler rh = new HttpRequestRetryHandler() {
        @Override
        public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
            return executionCount < 2;
        }
    };

    private static final RequestConfig config = RequestConfig.custom()
            .setConnectTimeout(TIMEOUT)
            .setConnectionRequestTimeout(TIMEOUT)
            .setSocketTimeout(TIMEOUT)
            .build();

    /*
     * generates a static SSL Connection socket factory.
     * @return
     */
    private static SSLConnectionSocketFactory generateSSF() {
        try {
            org.apache.http.ssl.SSLContextBuilder sslContextBuilder = SSLContextBuilder.create();
            //sslContextBuilder.loadTrustMaterial(new org.apache.http.conn.ssl.TrustSelfSignedStrategy());
            sslContextBuilder.loadTrustMaterial(new org.apache.http.conn.ssl.TrustAllStrategy());
            SSLContext sslContext = sslContextBuilder.build();

            return new SSLConnectionSocketFactory(sslContext, new org.apache.http.conn.ssl.NoopHostnameVerifier());
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            log.warning("Default Algorithm for BulkData Http Client not found " + e.getMessage());
        }
        return SSLConnectionSocketFactory.getSocketFactory();
    }

    public CloseableHttpClient getHttpClient(String username, String password) {
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(username, password));

        return HttpClients.custom()
                .setSSLSocketFactory(sf)
                .setRetryHandler(rh)
                .setDefaultRequestConfig(config)
                .setDefaultCredentialsProvider(credsProvider)
                .build();
    }
}
