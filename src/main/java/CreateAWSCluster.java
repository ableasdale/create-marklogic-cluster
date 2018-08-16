import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateAWSCluster {

    private static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static void main(String[] args) {

        String[] hosts = Configuration.getInstance().getStringArray("hosts");
        Map<String, HttpHost> hostMap = new HashMap<>();
        CredentialsProvider credsProvider = new BasicCredentialsProvider();

        // 1. iterate through each of the hosts, set up an HTTP Client for each
        for (String hostname : hosts) {
            LOG.info(MessageFormat.format("Configuring HTTP Client for host: {0}", hostname));
            HttpHost httpHost = new HttpHost(hostname, 8001, "http");
            credsProvider.setCredentials(
                    new AuthScope(httpHost),
                    new UsernamePasswordCredentials(Configuration.getInstance().getString("mluser"), Configuration.getInstance().getString("mlpass")));
            hostMap.put(hostname, httpHost);

        }

        CloseableHttpClient httpClient = HttpClients.custom().setDefaultCredentialsProvider(credsProvider)
                .build();

        // 2. iterate through each of the hosts, establish a connection and initialize
        for (String hostname : hosts) {
            httpPost("/admin/v1/init", hostMap.get(hostname), new ArrayList<>(), httpClient);
        }

        // 3. Configure bootstrap (primary) host
        List<NameValuePair> bootstrapHostParams = new ArrayList<>();
        bootstrapHostParams.add(new BasicNameValuePair("admin-username", "admin"));
        bootstrapHostParams.add(new BasicNameValuePair("admin-password", "admin"));
        bootstrapHostParams.add(new BasicNameValuePair("realm", "public"));
        httpPost("/admin/v1/instance-admin", hostMap.get(hosts[0]), bootstrapHostParams, httpClient);


        sleep();


        // 4. Loop through all other nodes in the array and join them to the bootstrap host
        for (int i = 1; i < hosts.length; i++) {
            LOG.info(MessageFormat.format("Adding: {0} to the cluster", hosts[i]));
            // Get initial joiner information
            byte[] joinerConfig = httpGet("/admin/v1/server-config", hostMap.get(hosts[i]), new ArrayList<>(), httpClient);

            // HTTP POST that Configuration to the bootstrap host
            List<NameValuePair> joiningHostConfiguration = new ArrayList<>();
            joiningHostConfiguration.add(new BasicNameValuePair("group", "Default"));
            joiningHostConfiguration.add(new BasicNameValuePair("server-config", new String(joinerConfig)));
            byte[] currentClusterConfig = httpPost("/admin/v1/cluster-config", hostMap.get(hosts[0]), joiningHostConfiguration, httpClient);

            // Join the host to the cluster
            HttpPost htp = new HttpPost("/admin/v1/cluster-config");
            htp.addHeader("Content-type", "application/zip");
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addBinaryBody("cluster-config.zip", currentClusterConfig,
                    ContentType.APPLICATION_OCTET_STREAM, "cluster-config.zip");
            httpPostBinaryData(htp, hostMap.get(hosts[i]), builder.build(), httpClient);
        }
    }

    private static void sleep() {
        try {
            Thread.sleep(2500);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }


    private static void httpPostBinaryData(HttpPost httpPost, HttpHost target, HttpEntity multipart, CloseableHttpClient httpClient) {
        LOG.info(String.format("Executing request: %s to target: %s", httpPost.getRequestLine(), target));
        try {
            httpPost.setEntity(multipart);
            HttpResponse response = httpClient.execute(target, httpPost);
            LOG.info(String.format("Request status line: %s | Response status line: %s", httpPost.getRequestLine(), response.getStatusLine()));
        } catch (IOException e) {
            LOG.error("IOException Caught: ", e);
        }
    }

    private static byte[] httpPost(String uri, HttpHost target, List<NameValuePair> nvps, CloseableHttpClient httpClient) {
        HttpPost httpPost = new HttpPost(uri);
        byte[] data = null;
        LOG.info(String.format("Executing request: %s to target: %s", httpPost.getRequestLine(), target));
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(nvps, StandardCharsets.UTF_8));
            HttpResponse response = httpClient.execute(target, httpPost);
            LOG.info(String.format("Request status line: %s | Response status line: %s", httpPost.getRequestLine(), response.getStatusLine()));
            data = EntityUtils.toByteArray(response.getEntity());
        } catch (IOException e) {
            LOG.error("IOException Caught: ", e);
        }
        return data;
    }

    private static byte[] httpGet(String uri, HttpHost target, List<NameValuePair> nvps, CloseableHttpClient httpClient) {
        HttpGet httpGet = new HttpGet(uri);
        httpGet.addHeader("Accept", "application/xml");
        byte[] data = null;
        LOG.info(String.format("Executing request: %s to target: %s", httpGet.getRequestLine(), target));
        try {
            HttpResponse response = httpClient.execute(target, httpGet);
            LOG.info(String.format("Request status line: %s | Response status line: %s", httpGet.getRequestLine(), response.getStatusLine()));
            data = EntityUtils.toByteArray(response.getEntity());
        } catch (IOException e) {
            LOG.error("IOException Caught: ", e);
        }
        return data;
    }

}
