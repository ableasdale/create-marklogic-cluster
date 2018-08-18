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

        WebClient webClient = new WebClient();
        webClient.setRefreshHandler(new ThreadedRefreshHandler());
        //webClient.se
        //webClient.setRedirectEnabled(true);
        //webClient.setThrowExceptionOnFailingStatusCode(false);
        // For when we need them!
        DefaultCredentialsProvider credentialsProvider =
                (DefaultCredentialsProvider) webClient.getCredentialsProvider();
        credentialsProvider.addCredentials("admin", "admin");


        HtmlPage page = null;
        try {
            page = webClient.getPage("http://engrlab-129-015.engrlab.marklogic.com:8001");
           //String pageAsXml = page.asXml();
           // LOG.info(pageAsXml);
//            LOG.info("c"+pageAsXml.contains("This server must now self-install the initial databases \n" +
//                    "and application servers.  Click OK to continue."));

            DomNodeList inputs = page.getElementsByTagName("b");
            Iterator nodesIterator = inputs.iterator();

            while(nodesIterator.hasNext()) {
                DomNode element = (DomNode) nodesIterator.next();
                if (element.getTextContent().equals("This server must now self-install the initial databases \n" +
                        "and application servers.  Click OK to continue.")){
                    LOG.info("Need to initialize!");
                }
            }

            // find the okbutton
            HtmlForm form = page.getFormByName("initialize");
            //HtmlImageInput button = form.getInputByName("ok");
            HtmlPage page2 = submitForm(page, form);


            //final HtmlDivision div = page.getHtmlElementById("ok_button");

            // At this point we have the server restart page
            //Server Restart - MarkLogic Server - localhost
            if(page2.getTitleText().equals("Server Restart - MarkLogic Server - localhost")){
                LOG.info("need to do a loop to wait");
                // for now we'll just sleep for 10 seconds
                sleep();
                page2 = webClient.getPage("http://engrlab-129-015.engrlab.marklogic.com:8001");
            }



            // ensure this is the "join a cluster" page
            if(page2.getTitleText().equals("Join a Cluster - MarkLogic Server - engrlab-129-015.engrlab.marklogic.com")){
                LOG.info("now at the Join a Cluster page");
                HtmlForm form2 = page2.getFormByName("join-admin");
                HtmlImageInput input = form2.getInputByName("cancel");
                HtmlPage page3 = (HtmlPage) input.click();


                //HtmlPage page3 = submitForm(page2, form2);
                //LOG.info("should have skipped");
                //String page3AsXml = page3.asXml();
                //LOG.info(page3AsXml);


                if(page3.getTitleText().equals("Security Setup - MarkLogic Server - engrlab-129-015.engrlab.marklogic.com")){
                    LOG.info("Now at the Security Setup page");
                    // enter the String "admin" 5 times:
                    // <input type="text" size="35" value="" class="valid-text" name="user" id="user-text-input"/>
                    // <input type="password" size="35" class="valid" name="password1" value=""/>
                    // <input type="password" size="35" class="valid" name="password2" value=""/>
                    // <input type="password" size="35" class="valid" name="wallet-password1" value=""/>
                    // <input type="password" size="35" class="valid" name="wallet-password2" value=""/>
                    HtmlForm securityForm = page3.getFormByName("security");
                    securityForm.getInputByName("user").setTextContent("admin");
                    securityForm.getInputByName("user").setValueAttribute("admin");

                    securityForm.getInputByName("password1").setTextContent("admin");
                    securityForm.getInputByName("password1").setValueAttribute("admin");

                    securityForm.getInputByName("password2").setTextContent("admin");
                    securityForm.getInputByName("password2").setValueAttribute("admin");

                    securityForm.getInputByName("wallet-password1").setTextContent("admin");
                    securityForm.getInputByName("wallet-password1").setValueAttribute("admin");

                    securityForm.getInputByName("wallet-password2").setTextContent("admin");
                    securityForm.getInputByName("wallet-password2").setValueAttribute("admin");

                    HtmlImageInput okBtn = securityForm.getInputByName("ok");
                    HtmlPage page4 = (HtmlPage) okBtn.click();
                    LOG.info("Password / User should have been created");

                    // Note: this would now throw a 401

                    if(page4.getTitleText().equals("System Summary - MarkLogic Server - engrlab-129-015.engrlab.marklogic.com")) {
                        LOG.info("Bootstrap host successfully created");
                    }

                }

            }

        LOG.info("----------------------------------------------");

        } catch (IOException e) {
            e.printStackTrace();
        }

        // *****************************************************
        // Set up second node and connect to the bootstrap host
        // Bootstrap host: engrlab-129-015.engrlab.marklogic.com
        // Second host: engrlab-129-040.engrlab.marklogic.com
        // *****************************************************

        try {
            LOG.info("Configuring the second host");
            HtmlPage initPage = webClient.getPage("http://engrlab-129-040.engrlab.marklogic.com:8001");

            if(initPage.getTitleText().equals("Server Install - MarkLogic Server - localhost")) {
                LOG.info("Connected and got to the initialisation page");
                HtmlForm initPageForm = initPage.getFormByName("initialize");
                HtmlImageInput okBtn = initPageForm.getInputByName("ok");
                HtmlPage page2 = (HtmlPage) okBtn.click();

                if(page2.getTitleText().equals("Server Restart - MarkLogic Server - localhost")){
                    LOG.info("need to do a loop to wait");
                    // for now we'll just sleep for 10 seconds
                    sleep();
                    page2 = webClient.getPage("http://engrlab-129-040.engrlab.marklogic.com:8001");
                }

                //String page2AsXml = page2.asXml();
                //LOG.info(page2AsXml);
                if(page2.getTitleText().equals("Join a Cluster - MarkLogic Server - engrlab-129-040.engrlab.marklogic.com")){
                    LOG.info("Now at the Join a Cluster page");
                    //<input type="text" size="35" class="valid-text" name="server" value=""/>
                    HtmlForm form2 = page2.getFormByName("join-admin");
                    form2.getInputByName("server").setValueAttribute("engrlab-129-015.engrlab.marklogic.com");
                    HtmlImageInput input = form2.getInputByName("ok");
                    HtmlPage page3 = (HtmlPage) input.click();

                    //String page3AsXml = page3.asXml();
                    //LOG.info(page3AsXml);

                    // At this point we should see an html mage with a meta element (refresh)
                    DomNodeList inputs = page3.getElementsByTagName("meta");
                    Iterator nodesIterator = inputs.iterator();

                    // get the redirect URL and take it
                    String redirectUrl ="";
                    while(nodesIterator.hasNext()) {

                        DomNode element = (DomNode) nodesIterator.next();
                        LOG.info(element.getAttributes().getNamedItem("content").getNodeValue());
                        redirectUrl = element.getAttributes().getNamedItem("content").getNodeValue();
                        /*
                        NamedNodeMap foo = element.getAttributes();

                        for (int i=0; i<foo.getLength(); i++){
                            LOG.info("ITEM:"+foo.item(i).getNodeValue());
                        } */


                        //LOG.info(element.getAttributes().getNamedItem("content").getNodeValue());

                        /*
                        if (element.getTextContent().equals("This server must now self-install the initial databases \n" +
                                "and application servers.  Click OK to continue.")){
                            LOG.info("Need to initialize!");
                        }*/
                    }
                    /* end this doesn't work */

                    LOG.info("TODO - now go here:"+redirectUrl.substring(redirectUrl.indexOf("=") + 1));
                    //System.out.println(redirectUrl.substring(redirectUrl.lastIndexOf("=") + 1));

                    // *** GO TO THE BOOTSTRAP HOST HERE ***
                    HtmlPage theNextPage = webClient.getPage("http://engrlab-129-015.engrlab.marklogic.com:8001"+redirectUrl.substring(redirectUrl.indexOf("=") + 1));

                    //String theNextPageAsXml = theNextPage.asXml();
                    //LOG.info(theNextPageAsXml);

                    // now we have an accept joiner page
                    // accept-joiner form
                    HtmlForm acceptJoinerForm = theNextPage.getFormByName("accept-joiner");
                    // click ok
                    HtmlImageInput okInput = acceptJoinerForm.getInputByName("ok");
                    HtmlPage anotherPage = (HtmlPage) okInput.click();



                    HtmlForm acceptJoinerConfirmForm = anotherPage.getFormByName("accept-joiner-confirm");
                    HtmlImageInput okInputBtn = acceptJoinerConfirmForm.getInputByName("ok");
                    HtmlPage yetAnotherPage = (HtmlPage) okInputBtn.click();


                    HtmlForm joinedAdminForm = yetAnotherPage.getFormByName("joined-admin");
                    HtmlImageInput anotherOkInputBtn = joinedAdminForm.getInputByName("ok");
                    HtmlPage oneMorePage = (HtmlPage) anotherOkInputBtn.click();

                    // this is a sleep page LOG.info(oneMorePage.asXml());
                    sleep();
                    HtmlPage checkTheSecondHost = webClient.getPage("http://engrlab-129-040.engrlab.marklogic.com:8001");
                    LOG.info(checkTheSecondHost.asXml());


                    /* Try to get the page head instead
                    DomNodeList pageHead = page.getElementsByTagName("head");
                    for (int i = 0; i < pageHead.getLength(); i++) {
                        Node node = pageHead.item(i);
                        if (node.getNodeType() == Node.ELEMENT_NODE) {
                            // do something with the current element
                            LOG.info(node.getNodeName() + "+" + node.getAttributes().item(1).getNodeValue());
                        }
                    }*/

                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }


        //Assert.assertEquals("HtmlUnit - Welcome to HtmlUnit", page.getTitleText());

           // final String pageAsXml = page.asXml();
            ///Assert.assertTrue(pageAsXml.contains("<body class=\"composite\">"));

           // final String pageAsText = page.asText();
            //Assert.assertTrue(pageAsText.contains("Support for the HTTP and HTTPS protocols"));


    }

    private static HtmlPage submitForm(HtmlPage page, HtmlForm form) throws IOException {
        // create a submit button to submit the form
        HtmlElement button = (HtmlElement) page.createElement("button");
        button.setAttribute("type", "submit");
        form.appendChild(button);
        return button.click();
    }

    private static void sleep() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

}
