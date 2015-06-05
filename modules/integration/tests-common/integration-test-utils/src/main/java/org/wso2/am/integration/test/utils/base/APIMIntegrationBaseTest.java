/*
*Copyright (c) 2015​, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/
package org.wso2.am.integration.test.utils.base;

import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.bean.APIMURLBean;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.am.integration.test.utils.generic.APIMTestCaseUtils;
import org.wso2.am.integration.test.utils.generic.ServiceDeploymentUtil;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.engine.frameworkutils.FrameworkPathUtil;
import org.wso2.carbon.integration.common.utils.LoginLogoutClient;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Iterator;
import java.util.regex.Matcher;

import static org.testng.Assert.assertTrue;

/**
 * Base class for all API Manager integration tests
 * Users need to extend this class to write integration tests.
 */
public class APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(APIMIntegrationBaseTest.class);
    protected AutomationContext storeContext, publisherContext, gatewayContext;
    protected OMElement synapseConfiguration;
    protected APIMTestCaseUtils apimTestCaseUtils;
    protected TestUserMode userMode;
    protected APIMURLBean storeUrls, publisherUrls, gatewayUrls;


    /**
     * This method will initialize test environment
     * based on user mode and configuration given at automation.xml
     *
     * @throws APIManagerIntegrationTestException - if test configuration init fails
     */
    protected void init() throws APIManagerIntegrationTestException {
        userMode = TestUserMode.SUPER_TENANT_ADMIN;
        init(userMode);
    }

    /**
     * init the object with user mode , create context objects and get session cookies
     *
     * @param userMode - user mode to run the tests
     * @throws APIManagerIntegrationTestException - if test configuration init fails
     */
    protected void init(TestUserMode userMode) throws APIManagerIntegrationTestException {

        apimTestCaseUtils = new APIMTestCaseUtils();

        try {
            //create store server instance based on configuration given at automation.xml
            storeContext =
                    new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                                          APIMIntegrationConstants.AM_STORE_INSTANCE, userMode);
            storeUrls = new APIMURLBean(storeContext.getContextUrls());

            //create publisher server instance based on configuration given at automation.xml
            publisherContext =
                    new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                                          APIMIntegrationConstants.AM_PUBLISHER_INSTANCE, userMode);
            publisherUrls = new APIMURLBean(publisherContext.getContextUrls());

            //create gateway server instance based on configuration given at automation.xml
            gatewayContext =
                    new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                                          APIMIntegrationConstants.AM_GATEWAY_INSTANCE, userMode);
            gatewayUrls = new APIMURLBean(gatewayContext.getContextUrls());

        } catch (XPathExpressionException e) {
            log.error("APIM test environment initialization failed", e);
            throw new APIManagerIntegrationTestException("APIM test environment initialization failed", e);
        }

    }

    /**
     * init the object with tenant domain, user key and instance of store,publisher and gateway
     * create context objects and construct URL bean
     *
     * @param domainKey         - tenant domain key
     * @param userKey           - tenant user key
     * @param publisherInstance - publisher instance name in automation.xml
     * @param storeInstance     - store instance name in automation.xml
     * @param gatewayInstance   - gateway instance name in automation.xml
     * @throws APIManagerIntegrationTestException - if test configuration init fails
     */
    protected void init(String domainKey, String userKey, String publisherInstance,
                        String storeInstance, String gatewayInstance)
            throws APIManagerIntegrationTestException {

        apimTestCaseUtils = new APIMTestCaseUtils();

        try {
            //create store server instance based configuration given at automation.xml
            storeContext =
                    new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                                          storeInstance, domainKey, userKey);
            storeUrls = new APIMURLBean(storeContext.getContextUrls());

            //create publisher server instance
            publisherContext =
                    new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                                          publisherInstance, domainKey, userKey);
            publisherUrls = new APIMURLBean(publisherContext.getContextUrls());

            //create gateway server instance
            gatewayContext =
                    new AutomationContext(APIMIntegrationConstants.AM_PRODUCT_GROUP_NAME,
                                          gatewayInstance, domainKey, userKey);
            gatewayUrls = new APIMURLBean(gatewayContext.getContextUrls());

        } catch (XPathExpressionException e) {
            log.error("Init failed", e);
            throw new APIManagerIntegrationTestException("APIM test environment initialization failed", e);
        }

    }

    /**
     * proxy service URL of deployed server non secure
     *
     * @param proxyServiceName - name of proxy service
     * @return - URL of proxy service
     */
    protected String getProxyServiceURLHttp(String proxyServiceName)
            throws APIManagerIntegrationTestException {
        try {
            return gatewayContext.getContextUrls().getServiceUrl() + "/" + proxyServiceName + "/";
        } catch (XPathExpressionException e) {
            log.error("URL retrieve error", e);
            throw new APIManagerIntegrationTestException("APIM test environment initialization failed", e);
        }
    }

    /**
     * @param relativeFilePath - file path to load config
     * @throws APIManagerIntegrationTestException - Throws if load synapse configuration from file path
     *                                            fails
     */
    protected void loadSynapseConfigurationFromClasspath(String relativeFilePath,
                                                         AutomationContext automationContext,
                                                         String sessionCookie)
            throws APIManagerIntegrationTestException {

        relativeFilePath = relativeFilePath.replaceAll("[\\\\/]", Matcher.quoteReplacement(File.separator));
        OMElement synapseConfig;

        try {
            synapseConfig = apimTestCaseUtils.loadResource(relativeFilePath);
            updateSynapseConfiguration(synapseConfig, automationContext, sessionCookie);

        } catch (FileNotFoundException e) {
            log.error("synapse config loading issue", e);
            throw new APIManagerIntegrationTestException("synapse config loading issue", e);
        } catch (XMLStreamException e) {
            log.error("synapse config loading issue", e);
            throw new APIManagerIntegrationTestException("synapse config loading issue", e);
        }
    }

    /**
     * @param automationContext - automation context instance of given server
     * @return - created session cookie variable
     * @throws APIManagerIntegrationTestException - Throws if creating session cookie fails
     */
    protected String createSession(AutomationContext automationContext)
            throws APIManagerIntegrationTestException {
        LoginLogoutClient loginLogoutClient;
        try {
            loginLogoutClient = new LoginLogoutClient(automationContext);
            return loginLogoutClient.login();
        } catch (Exception e) {
            log.error("session creation error", e);
            throw new APIManagerIntegrationTestException("session creation error", e);
        }
    }

    /**
     * Get test artifact resources location
     *
     * @return - absolute patch of test artifact directory
     */
    protected String getAMResourceLocation() {
        return FrameworkPathUtil.getSystemResourceLocation() + "artifacts" + File.separator + "AM";
    }

    /**
     * update synapse config to server
     *
     * @param synapseConfig     - config to upload
     * @param automationContext - automation context of the server instance
     * @param sessionCookie     -  logged in session cookie
     * @throws APIManagerIntegrationTestException - If synapse config update fails
     */
    protected void updateSynapseConfiguration(OMElement synapseConfig,
                                              AutomationContext automationContext,
                                              String sessionCookie)
            throws APIManagerIntegrationTestException {

        if (synapseConfiguration == null) {
            synapseConfiguration = synapseConfig;
        } else {
            Iterator<OMElement> itr = synapseConfig.cloneOMElement().getChildElements();  //ToDo
            while (itr.hasNext()) {
                synapseConfiguration.addChild(itr.next());
            }
        }

        try {

            apimTestCaseUtils.updateSynapseConfiguration(synapseConfig,
                                                         automationContext.getContextUrls().getBackEndUrl(),
                                                         sessionCookie);

            if (automationContext.getProductGroup().isClusterEnabled()) {

                long deploymentDelay = Long.parseLong(automationContext.getConfigurationValue("//deploymentDelay"));
                Thread.sleep(deploymentDelay);
                Iterator<OMElement> proxies = synapseConfig.getChildrenWithLocalName("proxy"); //ToDo

                while (proxies.hasNext()) {
                    String proxy = proxies.next().getAttributeValue(new QName("name"));
                    assertTrue(isProxyWSDlExist(getProxyServiceURLHttp(proxy), deploymentDelay)
                            , "Deployment Synchronizing failed in workers");
                    assertTrue(isProxyWSDlExist(getProxyServiceURLHttp(proxy), deploymentDelay)
                            , "Deployment Synchronizing failed in workers");
                    assertTrue(isProxyWSDlExist(getProxyServiceURLHttp(proxy), deploymentDelay)
                            , "Deployment Synchronizing failed in workers");
                }
            }

        } catch (Exception e) {
            log.error("synapse config  upload error", e);
            throw new APIManagerIntegrationTestException("synapse config  upload error", e);
        }
    }


    /**
     * check whether the proxy service wsdl exist
     *
     * @param serviceUrl         - proxy service URL
     * @param synchronizingDelay - delay to update
     * @return - whether wsdl exist or not
     * @throws APIManagerIntegrationTestException - APIManagerIntegrationTestException
     */
    private boolean isProxyWSDlExist(String serviceUrl, long synchronizingDelay)
            throws APIManagerIntegrationTestException {
        try {
            return ServiceDeploymentUtil.isServiceWSDlExist(serviceUrl, synchronizingDelay);
        } catch (Exception e) {
            log.error("wsdl lookup error", e);
            throw new APIManagerIntegrationTestException("wsdl lookup error", e);
        }
    }

    public static void cleanup(){

    }

    /**
     * Cleaning up the API manager by removing all APIs and applications other than default application
     *
     * @param userName     - username of the api created tenant
     * @param passWord     - password of the api created tenant
     * @param storeUrl     - store url
     * @param publisherUrl - publisher url
     * @throws APIManagerIntegrationTestException - occurred when calling the apis
     * @throws org.json.JSONException                      - occurred when reading the json
     */
    public static void cleanUp(String userName, String passWord, String storeUrl,
                               String publisherUrl) throws APIManagerIntegrationTestException,
                                                           JSONException {

        APIStoreRestClient apiStore = new APIStoreRestClient(storeUrl);
        apiStore.login(userName, passWord);
        APIPublisherRestClient publisherRestClient = new APIPublisherRestClient(publisherUrl);
        publisherRestClient.login(userName, passWord);
        String subscriptionData = apiStore.getAllSubscriptions().getData();
        JSONObject jsonSubscription = new JSONObject(subscriptionData);

        if(jsonSubscription.getString("error").equals("false")) {
            JSONObject jsonSubscriptionsObject = jsonSubscription.getJSONObject("subscriptions");
            JSONArray jsonApplicationsArray = jsonSubscriptionsObject.getJSONArray("applications");

            //Remove API Subscriptions
            for (int i = 0; i < jsonApplicationsArray.length(); i++) {
                JSONObject appObject = jsonApplicationsArray.getJSONObject(i);
                int id = appObject.getInt("id");
                JSONArray subscribedAPIJSONArray = appObject.getJSONArray("subscriptions");
                for (int j = 0; j < subscribedAPIJSONArray.length(); j++) {
                    JSONObject subscribedAPI = subscribedAPIJSONArray.getJSONObject(j);
                    apiStore.removeAPISubscription(subscribedAPI.getString("name"), subscribedAPI.getString("version"),
                                                   subscribedAPI.getString("provider"), String.valueOf(id));
                }
            }
        }

        String apiData = apiStore.getAPI().getData();
        JSONObject jsonAPIData = new JSONObject(apiData);
        JSONArray jsonAPIArray = jsonAPIData.getJSONArray("apis");

        //delete all APIs
        for (int i = 0; i < jsonAPIArray.length(); i++) {
            JSONObject api = jsonAPIArray.getJSONObject(i);
            publisherRestClient.deleteAPI(api.getString("name"), api.getString("version"), userName);
        }

        //delete all application other than default application
        String applicationData = apiStore.getAllApplications().getData();
        JSONObject jsonApplicationData = new JSONObject(applicationData);
        JSONArray applicationArray = jsonApplicationData.getJSONArray("applications");
        for (int i = 0; i < applicationArray.length(); i++) {
            JSONObject jsonApplication = applicationArray.getJSONObject(i);
            if (!jsonApplication.getString("name").equals("DefaultApplication")) {
                apiStore.removeApplication(jsonApplication.getString("name"));
            }
        }

    }


}

