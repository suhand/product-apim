/*
*Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.tests.samples;

import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.bean.*;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.Response;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class YouTubeAPITestCase extends APIMIntegrationBaseTest {
    private APIPublisherRestClient apiPublisher;
    private APIStoreRestClient apiStore;
    private String gatewayUrl;

    @Factory(dataProvider = "userModeDataProvider")
    public YouTubeAPITestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
        String publisherURLHttp = publisherUrls.getWebAppURLHttp();
        String storeURLHttp = storeUrls.getWebAppURLHttp();

        apiStore = new APIStoreRestClient(storeURLHttp);
        apiPublisher = new APIPublisherRestClient(publisherURLHttp);


        apiPublisher.login(publisherContext.getContextTenant().getContextUser().getUserName(),
                           publisherContext.getContextTenant().getContextUser().getPassword());
        apiStore.login(storeContext.getContextTenant().getContextUser().getUserName(),
                       storeContext.getContextTenant().getContextUser().getPassword());

        if (gatewayContext.getContextTenant().getDomain().equals("carbon.super")) {
            gatewayUrl = gatewayUrls.getWebAppURLNhttp();
        } else {
            gatewayUrl = gatewayUrls.getWebAppURLNhttp() + "t/" + gatewayContext.getContextTenant().getDomain() + "/";
        }

    }

    @Test(groups = {"wso2.am"}, description = "You Tube other")
    public void testYouTubeApiSample() throws Exception {
        APIRequest apiRequest = new APIRequest("YoutubeFeeds", "youtube",
                                               new URL("http://gdata.youtube.com/feeds/api/standardfeeds"));
        apiPublisher.addAPI(apiRequest);
        APILifeCycleStateRequest updateRequest =
                new APILifeCycleStateRequest("YoutubeFeeds", publisherContext
                        .getContextTenant().getContextUser().getUserName(),
                                             APILifeCycleState.PUBLISHED
                );
        apiPublisher.changeAPILifeCycleStatus(updateRequest);
        apiStore.addApplication("YoutubeFeeds-Application", "Gold", "", "this-is-test");

        String provider = storeContext.getContextTenant().getContextUser().getUserName();

        SubscriptionRequest subscriptionRequest = new SubscriptionRequest("YoutubeFeeds", provider);
        subscriptionRequest.setApplicationName("YoutubeFeeds-Application");
        subscriptionRequest.setTier("Gold");
        apiStore.subscribe(subscriptionRequest);

        APPKeyRequestGenerator generateAppKeyRequest =
                new APPKeyRequestGenerator("YoutubeFeeds-Application");
        String responseString = apiStore.generateApplicationKey(generateAppKeyRequest).getData();
        JSONObject response = new JSONObject(responseString);
        String accessToken =
                response.getJSONObject("data").getJSONObject("key").get("accessToken").toString();
        Map<String, String> requestHeaders = new HashMap<String, String>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);

        Thread.sleep(2000);
        /*HttpResponse youTubeResponse = HttpRequestUtil.doGet(
                  getApiInvocationURLHttp("youtube/1.0.0/most_popular"), requestHeaders);*/

        HttpResponse youTubeResponse = HttpRequestUtil.doGet(
                gatewayUrl + "youtube/1.0.0/most_popular", requestHeaders);
        assertEquals(youTubeResponse.getResponseCode(), Response.Status.OK.getStatusCode(),
                     "Response code mismatched when api invocation");
        assertTrue(youTubeResponse.getData().contains("<feed"),
                   "Response data mismatched when api invocation");
        assertTrue(youTubeResponse.getData().contains("<category"),
                   "Response data mismatched when api invocation");
        assertTrue(youTubeResponse.getData().contains("<entry>"),
                   "Response data mismatched when api invocation");

    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        apiStore.removeApplication("YoutubeFeeds-Application");
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN},
        };
    }
}