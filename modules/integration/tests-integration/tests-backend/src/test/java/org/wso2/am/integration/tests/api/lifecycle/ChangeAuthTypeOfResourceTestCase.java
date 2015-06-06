/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.am.integration.tests.api.lifecycle;

import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.APIResourceBean;
import org.wso2.am.integration.test.utils.bean.ApplicationKeyBean;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Change the Auth type of the Resource and invoke the APi
 */
public class ChangeAuthTypeOfResourceTestCase extends APIManagerLifecycleBaseTest {

    private static final String API_NAME = "ChangeAuthTypeOfResourceTest";
    private static final String API_CONTEXT = "ChangeAuthTypeOfResource";
    private static final String API_TAGS = "testTag1, testTag2, testTag3";
    private static final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";
    private static final String API_DESCRIPTION = "This is test API create by API manager integration test";
    private static final String API_VERSION_1_0_0 = "1.0.0";
    private static final String RESPONSE_GET = "<id>123</id><name>John</name></Customer>";
    private static final String API_GET_ENDPOINT_METHOD = "/customers/123";
    private String APPLICATION_NAME = "ChangeAuthTypeOfResourceTestCase";
    private APIPublisherRestClient apiPublisherClientUser1;
    private String apiEndPointUrl;
    private APIStoreRestClient apiStoreClientUser1;
    private String providerName;
    private APIIdentifier apiIdentifier;
    private ApplicationKeyBean applicationKeyBean;
    private HashMap<String, String> requestHeadersGet;


    @BeforeClass(alwaysRun = true)
    public void initialize() throws Exception {
        super.init();
        apiEndPointUrl = gatewayUrls.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;
        providerName = publisherContext.getContextTenant().getContextUser().getUserName();
        String publisherURLHttp = publisherUrls.getWebAppURLHttp();
        String storeURLHttp = storeUrls.getWebAppURLHttp();
        apiPublisherClientUser1 = new APIPublisherRestClient(publisherURLHttp);
        apiStoreClientUser1 = new APIStoreRestClient(storeURLHttp);
        //Login to API Publisher with  admin
        apiPublisherClientUser1.login(publisherContext.getContextTenant().getContextUser().getUserName(),
                publisherContext.getContextTenant().getContextUser().getPassword());
        //Login to API Store with  admin
        apiStoreClientUser1.login(storeContext.getContextTenant().getContextUser().getUserName(),
                storeContext.getContextTenant().getContextUser().getPassword());
        requestHeadersGet = new HashMap<String, String>();
        requestHeadersGet.put("accept", "text/xml");
    }


    @Test(groups = {"wso2.am"}, description = "Invoke a resource with auth type Application And Application User")
    public void testInvokeResourceWithAuthTypeApplicationAndApplicationUser() throws Exception {
        //Create application
        apiStoreClientUser1.addApplication(APPLICATION_NAME, TIER_GOLD, "", "");
        APICreationRequestBean apiCreationRequestBean =
                new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0, providerName, new URL(apiEndPointUrl));
        apiCreationRequestBean.setTags(API_TAGS);
        apiCreationRequestBean.setDescription(API_DESCRIPTION);
        apiCreationRequestBean.setVisibility("public");
        List<APIResourceBean> apiResourceBeansList = new ArrayList<APIResourceBean>();
        APIResourceBean apiResourceBeanGET = new APIResourceBean("GET", "Application & Application User", "Unlimited", "/*");
        apiResourceBeansList.add(apiResourceBeanGET);
        apiCreationRequestBean.setResourceBeanList(apiResourceBeansList);
        //Create publish and subscribe a API
        apiIdentifier = new APIIdentifier(providerName, API_NAME, API_VERSION_1_0_0);
        createPublishAndSubscribeToAPI(apiIdentifier, apiCreationRequestBean, apiPublisherClientUser1,
                apiStoreClientUser1, APPLICATION_NAME);
        //get the  access token
        applicationKeyBean = generateApplicationKeys(apiStoreClientUser1, APPLICATION_NAME);
        String accessToken = applicationKeyBean.getAccessToken();
        requestHeadersGet.put("Authorization", "Bearer " + accessToken);
        //Send GET request
        HttpResponse httpResponseGet =
                HttpRequestUtil.doGet(gatewayWebAppUrl + API_CONTEXT + "/" + API_VERSION_1_0_0 + API_GET_ENDPOINT_METHOD,
                        requestHeadersGet);
        assertEquals(httpResponseGet.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Invocation fails for GET request for " +
                "auth type Application & Application User");
        assertTrue(httpResponseGet.getData().contains(RESPONSE_GET), "Response Data not match for GET request for" +
                " auth type Application & Application User. Expected value :\"" + RESPONSE_GET + "\" not contains" +
                " in response data:\"" + httpResponseGet.getData() + "\"");

    }


    @Test(groups = {"wso2.am"}, description = "Invoke a resource with auth type Application",
            dependsOnMethods = "testInvokeResourceWithAuthTypeApplicationAndApplicationUser")
    public void testInvokeResourceWithAuthTypeApplication() throws Exception {
        APICreationRequestBean apiCreationRequestBean =
                new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0, providerName,
                        new URL(apiEndPointUrl));
        apiCreationRequestBean.setTags(API_TAGS);
        apiCreationRequestBean.setDescription(API_DESCRIPTION);
        apiCreationRequestBean.setVisibility("public");
        List<APIResourceBean> apiResourceBeansList = new ArrayList<APIResourceBean>();
        APIResourceBean apiResourceBeanGET = new APIResourceBean("GET", "Application", "Unlimited", "/*");
        apiResourceBeansList.add(apiResourceBeanGET);
        apiCreationRequestBean.setResourceBeanList(apiResourceBeansList);
        //Update API with Edited information
        HttpResponse updateAPIHTTPResponse = apiPublisherClientUser1.updateAPI(apiCreationRequestBean);
        assertEquals(updateAPIHTTPResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK,
                "Update APi with new Resource information fail");
        assertEquals(getValueFromJSON(updateAPIHTTPResponse, "error"), "false", "Update APi with new Resource information fail");
        //Send GET request
        HttpResponse httpResponseGet =
                HttpRequestUtil.doGet(gatewayWebAppUrl + API_CONTEXT + "/" + API_VERSION_1_0_0 + API_GET_ENDPOINT_METHOD,
                        requestHeadersGet);
        assertEquals(httpResponseGet.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Invocation fails for GET request for " +
                "auth type Application");
        assertTrue(httpResponseGet.getData().contains(RESPONSE_GET), "Response Data not match for GET request for" +
                " auth type Application. Expected value :\"" + RESPONSE_GET + "\" not contains in response data:\"" +
                httpResponseGet.getData() + "\"");

    }


    @Test(groups = {"wso2.am"}, description = "Invoke a resource with auth type Application User",
            dependsOnMethods = "testInvokeResourceWithAuthTypeApplication")
    public void testInvokeGETResourceWithAuthTypeApplicationUser() throws Exception {

        APICreationRequestBean apiCreationRequestBean =
                new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0, providerName, new URL(apiEndPointUrl));
        apiCreationRequestBean.setTags(API_TAGS);
        apiCreationRequestBean.setDescription(API_DESCRIPTION);

        apiCreationRequestBean.setVisibility("public");
        List<APIResourceBean> apiResourceBeansList = new ArrayList<APIResourceBean>();
        APIResourceBean apiResourceBeanGET = new APIResourceBean("GET", "Application User", "Unlimited", "/*");
        apiResourceBeansList.add(apiResourceBeanGET);
        apiCreationRequestBean.setResourceBeanList(apiResourceBeansList);
        //Update API with Edited information
        HttpResponse updateAPIHTTPResponse = apiPublisherClientUser1.updateAPI(apiCreationRequestBean);
        assertEquals(updateAPIHTTPResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Update APi with new Resource information fail");
        assertEquals(getValueFromJSON(updateAPIHTTPResponse, "error"), "false", "Update APi with new Resource information fail");
        //Generate User Access Key
        String requestBody = "grant_type=password&username=admin&password=admin&scope=PRODUCTION";
        URL tokenEndpointURL = new URL(gatewayUrls.getWebAppURLNhttp() + "token");
        JSONObject accessTokenGenerationResponse = new JSONObject(
                apiStoreClientUser1.generateUserAccessKey(applicationKeyBean.getConsumerKey(),
                        applicationKeyBean.getConsumerSecret(), requestBody, tokenEndpointURL).getData());
        requestHeadersGet.put("Authorization", "Bearer " + accessTokenGenerationResponse.getString("access_token"));
        //Send GET request
        HttpResponse httpResponseGet =
                HttpRequestUtil.doGet(gatewayWebAppUrl + API_CONTEXT + "/" + API_VERSION_1_0_0 + API_GET_ENDPOINT_METHOD,
                        requestHeadersGet);
        assertEquals(httpResponseGet.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Invocation fails for GET request for " +
                "auth type Application User");
        assertTrue(httpResponseGet.getData().contains(RESPONSE_GET), "Response Data not match for GET request for" +
                " auth type Application User. Expected value :\"" + RESPONSE_GET + "\" not contains in response data:\"" +
                httpResponseGet.getData() + "\"");

    }

    @Test(groups = {"wso2.am"}, description = "Invoke a resource with auth type None",
            dependsOnMethods = "testInvokeGETResourceWithAuthTypeApplicationUser")
    public void testInvokeGETResourceWithAuthTypeNone() throws Exception {

        APICreationRequestBean apiCreationRequestBean =
                new APICreationRequestBean(API_NAME, API_CONTEXT, API_VERSION_1_0_0, providerName, new URL(apiEndPointUrl));
        apiCreationRequestBean.setTags(API_TAGS);
        apiCreationRequestBean.setDescription(API_DESCRIPTION);
        apiCreationRequestBean.setVisibility("public");
        List<APIResourceBean> apiResourceBeansList = new ArrayList<APIResourceBean>();
        APIResourceBean apiResourceBeanGET = new APIResourceBean("GET", "None", "Unlimited", "/*");
        apiResourceBeansList.add(apiResourceBeanGET);
        apiCreationRequestBean.setResourceBeanList(apiResourceBeansList);
        //Update API with Edited information
        HttpResponse updateAPIHTTPResponse = apiPublisherClientUser1.updateAPI(apiCreationRequestBean);
        assertEquals(updateAPIHTTPResponse.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Update APi with new Resource information fail");
        assertEquals(getValueFromJSON(updateAPIHTTPResponse, "error"), "false", "Update APi with new Resource information fail");
        //Send GET request
        HttpResponse httpResponseGet =
                HttpRequestUtil.doGet(gatewayWebAppUrl + API_CONTEXT + "/" + API_VERSION_1_0_0 + API_GET_ENDPOINT_METHOD,
                        requestHeadersGet);
        assertEquals(httpResponseGet.getResponseCode(), HTTP_RESPONSE_CODE_OK, "Invocation fails for GET request for " +
                "auth type None");
        assertTrue(httpResponseGet.getData().contains(RESPONSE_GET), "Response Data not match for GET request for" +
                " auth type Non3. Expected value :\"" + RESPONSE_GET + "\" not contains in response data:\"" +
                httpResponseGet.getData() + "\"");
    }


    @AfterClass(alwaysRun = true)
    public void cleanUpArtifacts() throws APIManagerIntegrationTestException {
        apiStoreClientUser1.removeApplication(APPLICATION_NAME);
        deleteAPI(apiIdentifier, apiPublisherClientUser1);
    }

}
