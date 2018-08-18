import com.gargoylesoftware.htmlunit.DefaultCredentialsProvider;
import com.gargoylesoftware.htmlunit.ThreadedRefreshHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Iterator;

public class CreateClusterWithAdminGUI {

    private static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static void main(String[] args) {

        String[] hosts = Configuration.getInstance().getStringArray("hosts");
        String[] hostUrls = new String[hosts.length];

        WebClient webClient = new WebClient();
        webClient.setRefreshHandler(new ThreadedRefreshHandler());
        DefaultCredentialsProvider credentialsProvider =
                (DefaultCredentialsProvider) webClient.getCredentialsProvider();
        credentialsProvider.addCredentials(Configuration.getInstance().getString("mluser"), Configuration.getInstance().getString("mlpass"));

        // 1. Initialize every host in the cluster
        for (int i=0; i<hosts.length; i++) {
            String hostUrl = String.format("http://%s:8001", hosts[i]);
            hostUrls[i] = hostUrl;
            LOG.info("Initialising: "+hosts[i] + " | "+hostUrls[i]);
            checkPageAndSubmitForm(webClient, hostUrls[i], "Server Install - MarkLogic Server - localhost", "initialize");
        }
        sleep(5000);

        // 2. Configure bootstrap host (hosts[0])
        try {
            // 2a: skip adding host to cluster
            HtmlPage page = webClient.getPage(hostUrls[0]);
            HtmlForm form = page.getFormByName("join-admin");
            HtmlImageInput input = form.getInputByName("cancel");
            page = (HtmlPage) input.click();

            // 2b: set up admin user
            if(page.getTitleText().contains("Security Setup - MarkLogic Server")){
                LOG.info("Confirmed "+ hosts[0] + " is now at the Security Setup Form");
            }

            HtmlForm securityForm = page.getFormByName("security");
            securityForm.getInputByName("user").setValueAttribute(Configuration.getInstance().getString("mluser"));
            securityForm.getInputByName("password1").setValueAttribute(Configuration.getInstance().getString("mlpass"));
            securityForm.getInputByName("password2").setValueAttribute(Configuration.getInstance().getString("mlpass"));
            securityForm.getInputByName("wallet-password1").setValueAttribute(Configuration.getInstance().getString("mlpass"));
            securityForm.getInputByName("wallet-password2").setValueAttribute(Configuration.getInstance().getString("mlpass"));
            input = securityForm.getInputByName("ok");
            page = (HtmlPage) input.click();

            if(page.getTitleText().contains("System Summary - MarkLogic Server")){
                LOG.info("Confirmed "+ hostUrls[0] + " is the configured bootstrap host");
            }
            LOG.info("------------------------------------------------------------------");
        } catch (IOException e) {
            e.printStackTrace();
        }
        //System.exit(0);

        // 3. Add remaining hosts to cluster
        for (int i = 1; i < hosts.length; i++) {
            LOG.info("Adding "+hosts[i]+" to cluster with " + hosts[0] + " as the bootstrap host");
            try {
                LOG.info("1. Introduce host "+hosts[i]+" to the cluster (join-admin)");
                HtmlPage page = webClient.getPage(hostUrls[i]);
                if(page.getTitleText().contains("Join a Cluster - MarkLogic Server")){
                    LOG.info("Confirmed "+ hosts[i] + " is ready to start the process of joining the cluster");
                }
                HtmlForm form = page.getFormByName("join-admin");
                form.getInputByName("server").setValueAttribute(hosts[0]);
                HtmlImageInput input = form.getInputByName("ok");
                page = (HtmlPage) input.click();
                String redirectUrl = getRedirectUrl(page);

                LOG.info("2. Handling redirect: "+redirectUrl);
                page = checkPageAndSubmitForm(webClient, hostUrls[0] + redirectUrl, "Join a Cluster - MarkLogic Server - "+hosts[0], "accept-joiner");
                //sleep(20000);
                //LOG.info(page.asXml());
                LOG.info("3. Accept joiner");
                page = clickOk(page,"accept-joiner-confirm");
                LOG.info("4. Accept joiner");
                page = clickOk(page,"joined-admin");
                LOG.info("5. Joiner added? - sleep and check");
                //redirectUrl = getRedirectUrl(page);
                //page = webClient.getPage(hostUrls[0]+redirectUrl);
                sleep(10000);
                page = webClient.getPage(hostUrls[i]);
                // LOG.info(checkTheSecondHost.asXml());
                if(page.getTitleText().equals("Cluster Summary - MarkLogic Server - "+hosts[i])){
                    LOG.info(hosts[i]+" has been successfully added to the cluster");
                }

                //LOG.info(page.asXml());
                // The host should now be connected and will be restarting.
                // LOG.info(page.getTitleText());
                //page = webClient.getPage(hostUrls[i]);
                //LOG.info(page.getTitleText());

            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }

    private static String getRedirectUrl(HtmlPage page) {
        DomNodeList inputs = page.getElementsByTagName("meta");
        Iterator nodesIterator = inputs.iterator();
        String redirectUrl = "";
        while (nodesIterator.hasNext()) {
            DomNode element = (DomNode) nodesIterator.next();
            redirectUrl = element.getAttributes().getNamedItem("content").getNodeValue();
        }
        return redirectUrl.substring(redirectUrl.indexOf("=") + 1);
    }

    private static HtmlPage clickOk(HtmlPage page, String formName) {
        try {
            //LOG.info("Form name: "+formName + " | "+page.getTitleText());
            HtmlForm form = page.getFormByName(formName);
            //LOG.info("Form action: "+form.getActionAttribute());
            HtmlImageInput okBtn = form.getInputByName("ok");
           // HtmlPage newPage = (HtmlPage) okBtn.click();
            //LOG.info(newPage.asXml());
            return (HtmlPage) okBtn.click(); //return (HtmlPage) okBtn.click();
        } catch (IOException e) {
            LOG.error("IO Exception: ",e);
        }
        return null;
    }

    private static HtmlPage checkPageAndSubmitForm(WebClient webClient, String hostUri, String titleElement, String formName) {
        try {
            HtmlPage page = webClient.getPage(hostUri);
            LOG.info("page.getTitleText():"+page.getTitleText());
            // LOG.info(page.asXml());
            if(page.getTitleText().equals(titleElement)){
                LOG.info(String.format("%s confirmed page title as: %s", hostUri, titleElement));
            } else {
                LOG.info(page.getTitleText());
            }
            HtmlForm form = page.getFormByName(formName);
            HtmlImageInput okBtn = form.getInputByName("ok");
            return (HtmlPage) okBtn.click();
        } catch (IOException e) {
           LOG.error("IO Exception: ",e);
        }
        return null;
    }

    private static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
