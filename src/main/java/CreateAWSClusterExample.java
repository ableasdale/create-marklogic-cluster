import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.invoke.MethodHandles;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;

public class CreateAWSClusterExample {

    /* This is a complete working example showing the clustering of two MarkLogic hosts */

// http://ec2-34-240-98-107.eu-west-1.compute.amazonaws.com:8001

    private static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


    public static void main(String[] args) {

        String[] hosts = Configuration.getInstance().getStringArray("hosts");

        HttpHost target = new HttpHost(hosts[0], 8001, "http");
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(target),
                    new UsernamePasswordCredentials(Configuration.getInstance().getString("mluser"), Configuration.getInstance().getString("mlpass")));

        CloseableHttpClient httpClient = HttpClients.custom().setDefaultCredentialsProvider(credsProvider)
                .build();
        BasicHttpContext localContext = new BasicHttpContext();
        /*  .url(String.format("http://%s:8001/admin/v1/init", hostname))
                .post(RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"), ""))
                .build(); */
        //HttpGet httpGet = new HttpGet("/");
        HttpPost httpPost = new HttpPost("/admin/v1/init");
        List<NameValuePair> nvps = new ArrayList<>();



        LOG.info(String.format("Executing request: %s to target: %s", httpPost.getRequestLine(), target));


        try {
            httpPost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
            HttpResponse response = httpClient.execute(target, httpPost, localContext);
            LOG.info(String.format("Request status line: %s", httpPost.getRequestLine()));
            LOG.info(String.format("Response status line: %s", response.getStatusLine()));
//            LOG.info(String.format("Response Body: %s", EntityUtils.toString(response.getEntity())));
            //EntityUtils.consumeQuietly(response.getEntity());

        } catch (IOException e) {
            e.printStackTrace();
        }


        /*     .url(String.format("http://%s:8001/admin/v1/instance-admin", hostname))
                .post(RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"), String.format("admin-username=%s&admin-password=%s&realm=public", Util.getConfiguration().getString("mluser"), Util.getConfiguration().getString("mlpass"))))
                .build(); */

        try
        {
            Thread.sleep(2500);
        }
        catch(InterruptedException ex)
        {
            Thread.currentThread().interrupt();
        }



        HttpPost httpPost2 = new HttpPost("/admin/v1/instance-admin");
        List<NameValuePair> nvps2 = new ArrayList<>();
        nvps2.add(new BasicNameValuePair("admin-username", "admin"));
        nvps2.add(new BasicNameValuePair("admin-password", "admin"));
        nvps2.add(new BasicNameValuePair("realm", "public"));

        try {
            httpPost2.setEntity(new UrlEncodedFormEntity(nvps2, HTTP.UTF_8));
            HttpResponse response2 = httpClient.execute(target, httpPost2, localContext);
            LOG.info(String.format("Request status line: %s", httpPost2.getRequestLine()));
            LOG.info(String.format("Response status line: %s", response2.getStatusLine()));
            LOG.info(String.format("Response Body: %s", EntityUtils.toString(response2.getEntity())));
            EntityUtils.consumeQuietly(response2.getEntity());
            //httpClient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        LOG.info ("http://"+hosts[0]+":8001 is ready to go");
        // HOST 1
        LOG.info("*******************\nHOST 1\n********************");


        HttpHost target1 = new HttpHost(hosts[1], 8001, "http");
        /*CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(target),
                new UsernamePasswordCredentials(CONFIG.getString("mluser"), CONFIG.getString("mlpass"))); */

        //CloseableHttpClient httpClient = HttpClients.custom().setDefaultCredentialsProvider(credsProvider)
               // .build();
        //BasicHttpContext localContext = new BasicHttpContext();
        /*  .url(String.format("http://%s:8001/admin/v1/init", hostname))
                .post(RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"), ""))
                .build(); */
        //HttpGet httpGet = new HttpGet("/");
        LOG.info("Step 1:\n**************************");
        HttpPost httpPost3 = new HttpPost("/admin/v1/init");
        List<NameValuePair> nvps3 = new ArrayList<>();



        LOG.info(String.format("Executing request: %s to target: %s", httpPost3.getRequestLine(), target1));


        try {
            httpPost3.setEntity(new UrlEncodedFormEntity(nvps3, HTTP.UTF_8));
            HttpResponse response3 = httpClient.execute(target1, httpPost3, localContext);
            LOG.info(String.format("Request status line: %s", httpPost3.getRequestLine()));
            LOG.info(String.format("Response status line: %s", response3.getStatusLine()));
//            LOG.info(String.format("Response Body: %s", EntityUtils.toString(response.getEntity())));
            //EntityUtils.consumeQuietly(response.getEntity());

        } catch (IOException e) {
            e.printStackTrace();
        }

        try
        {
            Thread.sleep(2500);
        }
        catch(InterruptedException ex)
        {
            Thread.currentThread().interrupt();
        }

        LOG.info("Step 2 - initial joiner config:\n**************************");
        /* Join Cluster 1. `$CURL -X GET -H "Accept: application/xml" http://${JOINING_HOST}:8001/admin/v1/server-config` */
        /* public static Request getJoinerHostConfiguration(String joinerHost) {
            return new Request.Builder()
                    .url(String.format("http://%s:8001/admin/v1/server-config", joinerHost))
                    .header("Accept", "application/xml")
                    .get()
                    .build();
        } */

        HttpGet httpGet = new HttpGet("/admin/v1/server-config");
        httpGet.addHeader("Accept" , "application/xml");

        LOG.info(String.format("Executing request: %s to target: %s", httpGet.getRequestLine(), target1));
        byte[] joinerData = null;
        //String joinerData = "";

        try {
            //httpPost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
            HttpResponse response5 = httpClient.execute(target1, httpGet, localContext);
            LOG.info(String.format("Request status line: %s", httpGet.getRequestLine()));
            LOG.info(String.format("Response status line: %s", response5.getStatusLine()));
            //LOG.info(String.format("Response Body: %s", EntityUtils.toString(response.getEntity())));
            joinerData = EntityUtils.toByteArray(response5.getEntity());
            //joinerData = EntityUtils.toString(response5.getEntity());
            //EntityUtils.consumeQuietly(response.getEntity());

        } catch (IOException e) {
            e.printStackTrace();
        }

        LOG.info("Step 3 - send joiner config to bootstrap host:\n**************************");

    /* Join Cluster 2. $AUTH_CURL -X POST -o cluster-config.zip -d "group=Default" \
        --data-urlencode "server-config=${JOINER_CONFIG}" \
            -H "Content-type: application/x-www-form-urlencoded" \
    http://${BOOTSTRAP_HOST}:8001/admin/v1/cluster-config */
    /*    public static Request joinBootstrapHost(String bootstrapHost, byte[] joinerConfiguration) {
            Request r = null;
            try {
                r = new Request.Builder()
                        .url(String.format("http://%s:8001/admin/v1/cluster-config", bootstrapHost))
                        .post(RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"), String.format("group=Default&server-config=%s", URLEncoder.encode(new String(joinerConfiguration), "UTF-8"))))
                        .build();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            return r;
        } */

        byte[] joinerFullConfiguration = null;


        // LOG.info("Joiner data looks like this: "+new String(joinerData));
        HttpPost httpPost4 = new HttpPost("/admin/v1/cluster-config");
        List<NameValuePair> nvps4 = new ArrayList<>();
        nvps4.add(new BasicNameValuePair("group", "Default"));
        nvps4.add(new BasicNameValuePair("server-config", new String(joinerData)));
       // nvps4.add(new BasicNameValuePair("server-config", Base64.getEncoder().encodeToString(joinerData)));

        LOG.info(String.format("Executing request: %s to target: %s", httpPost4.getRequestLine(), target));


        try {
            httpPost4.setEntity(new UrlEncodedFormEntity(nvps4, StandardCharsets.UTF_8));

            HttpResponse response4 = httpClient.execute(target, httpPost4, localContext);
            LOG.info(String.format("Request status line: %s", httpPost4.getRequestLine()));
            LOG.info(String.format("Response status line: %s", response4.getStatusLine()));
            joinerFullConfiguration = EntityUtils.toByteArray(response4.getEntity());
//            LOG.info(String.format("Response Body: %s", EntityUtils.toString(response.getEntity())));
            //EntityUtils.consumeQuietly(response.getEntity());

        } catch (IOException e) {
            e.printStackTrace();
        }

        LOG.info("b:"+new String(joinerFullConfiguration));

    /* Join Cluster 3. $CURL -X POST -H "Content-type: application/zip" \
        --data-binary @./cluster-config.zip \
        http://${JOINING_HOST}:8001/admin/v1/cluster-config \ */
        /* public static Request joinTargetHostToCluster(String joiningHost, byte[] joinerZipConfiguration) {
            return new Request.Builder()
                    .url(String.format("http://%s:8001/admin/v1/cluster-config", joiningHost))
                    .header("Accept", "application/zip")
                    .post(RequestBody.create(MediaType.parse("application/zip"), joinerZipConfiguration))
                    .build();
        } */

        LOG.info("Step 4 - join cluster:\n**************************");
        HttpPost httpPost5 = new HttpPost("/admin/v1/cluster-config");
        httpPost5.addHeader("Content-type" , "application/zip");

        //List<NameValuePair> nvps5 = new ArrayList<>();
        //nvps5.add(new BasicNameValuePair("group", "Default"));
        //nvps5.add(new BasicNameValuePair("server-config", new String(joinerData)));
        // nvps4.add(new BasicNameValuePair("server-config", Base64.getEncoder().encodeToString(joinerData)));

        LOG.info(String.format("Executing request: %s to target: %s", httpPost5.getRequestLine(), target1));


        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addBinaryBody("cluster-config.zip", joinerFullConfiguration,
                ContentType.APPLICATION_OCTET_STREAM, "cluster-config.zip");
        HttpEntity multipart = builder.build();

        //httpPost.setEntity(multipart);

        try {
          //  httpPost5.setEntity(new ByteArrayEntity(joinerFullConfiguration));
            httpPost5.setEntity(multipart);
            HttpResponse response5 = httpClient.execute(target1, httpPost5, localContext);
            LOG.info(String.format("Request status line: %s", httpPost5.getRequestLine()));
            LOG.info(String.format("Response status line: %s", response5.getStatusLine()));

//            LOG.info(String.format("Response Body: %s", EntityUtils.toString(response.getEntity())));
            //EntityUtils.consumeQuietly(response.getEntity());

        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
