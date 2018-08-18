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
        for (int i = 0; i < hosts.length; i++) {
            String hostUrl = String.format("http://%s:8001", hosts[i]);
            hostUrls[i] = hostUrl;
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
            if (page.getTitleText().contains("Security Setup - MarkLogic Server")) {
                LOG.info(String.format("Confirmed %s is now at the Security Setup Form", hosts[0]));
            }

            HtmlForm securityForm = page.getFormByName("security");
            securityForm.getInputByName("user").setValueAttribute(Configuration.getInstance().getString("mluser"));
            securityForm.getInputByName("password1").setValueAttribute(Configuration.getInstance().getString("mlpass"));
            securityForm.getInputByName("password2").setValueAttribute(Configuration.getInstance().getString("mlpass"));
            securityForm.getInputByName("wallet-password1").setValueAttribute(Configuration.getInstance().getString("mlpass"));
            securityForm.getInputByName("wallet-password2").setValueAttribute(Configuration.getInstance().getString("mlpass"));
            input = securityForm.getInputByName("ok");
            page = (HtmlPage) input.click();

            if (page.getTitleText().contains("System Summary - MarkLogic Server")) {
                LOG.info(String.format("Confirmed %s is the configured bootstrap host", hostUrls[0]));
            }
            LOG.info("------------------------------------------------------------------");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 3. Add remaining hosts to cluster
        for (int i = 1; i < hosts.length; i++) {
            LOG.info(String.format("Adding %s to cluster with %s as the bootstrap host", hosts[i], hosts[0]));
            try {
                LOG.info(String.format("Introduce host %s to the cluster (join-admin)", hosts[i]));
                HtmlPage page = webClient.getPage(hostUrls[i]);
                if (page.getTitleText().contains("Join a Cluster - MarkLogic Server")) {
                    LOG.info(String.format("Confirmed %s is ready to start the process of joining the cluster", hosts[i]));
                }
                HtmlForm form = page.getFormByName("join-admin");
                form.getInputByName("server").setValueAttribute(hosts[0]);
                HtmlImageInput input = form.getInputByName("ok");
                page = (HtmlPage) input.click();
                String redirectUrl = getRedirectUrl(page);
                page = checkPageAndSubmitForm(webClient, String.format("%s%s", hostUrls[0], redirectUrl), String.format("Join a Cluster - MarkLogic Server - %s", hosts[0]), "accept-joiner");
                page = clickOk(page, "accept-joiner-confirm");
                page = clickOk(page, "joined-admin");
                sleep(10000);
                page = webClient.getPage(hostUrls[i]);
                if (page.getTitleText().equals(String.format("Cluster Summary - MarkLogic Server - %s", hosts[i]))) {
                    LOG.info(String.format("%s has been successfully added to the cluster", hosts[i]));
                }
                LOG.info("------------------------------------------------------------------");
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
            HtmlForm form = page.getFormByName(formName);
            HtmlImageInput okBtn = form.getInputByName("ok");
            return (HtmlPage) okBtn.click();
        } catch (IOException e) {
            LOG.error("IO Exception: ", e);
        }
        return null;
    }

    private static HtmlPage checkPageAndSubmitForm(WebClient webClient, String hostUri, String titleElement, String formName) {
        try {
            HtmlPage page = webClient.getPage(hostUri);
            if (page.getTitleText().equals(titleElement)) {
                LOG.info(String.format("Confirmed page title as: %s", titleElement));
            }
            HtmlForm form = page.getFormByName(formName);
            HtmlImageInput okBtn = form.getInputByName("ok");
            return (HtmlPage) okBtn.click();
        } catch (IOException e) {
            LOG.error("IO Exception: ", e);
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
