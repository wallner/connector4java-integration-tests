/*
 * Copyright (C) 2013 tarent AG
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.osiam.client;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osiam.client.oauth.Scope;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseTearDown;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/context.xml")
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class,
        DbUnitTestExecutionListener.class })
@DatabaseSetup("/database_seed.xml")
@DatabaseTearDown(value = "/database_tear_down.xml", type = DatabaseOperation.DELETE_ALL)
public class ClientManagementIT extends AbstractIntegrationTestBase {

    private static final String AUTH_SERVER_CLIENT_ENDPOINT_ADDRESS = AUTH_ENDPOINT_ADDRESS + "/Client";
    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER = "Bearer ";

    @Before
    public void setup() {
        accessToken = OSIAM_CONNECTOR.retrieveAccessToken("marissa", "koala", Scope.ADMIN);
    }

    @Test
    public void get_client_by_id() {
        String output = CLIENT.target(AUTH_SERVER_CLIENT_ENDPOINT_ADDRESS)
                .path("example-client")
                .request(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, BEARER + accessToken.getToken())
                .get(String.class);

        assertThat(output, containsString("example-client"));
    }

    @Test
    public void get_clients() {
        List<Map<String, Object>> clients = CLIENT.target(AUTH_SERVER_CLIENT_ENDPOINT_ADDRESS)
                .request(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, BEARER + accessToken.getToken())
                .get(new GenericType<List<Map<String, Object>>>() {
                });

        assertThat(clients, hasSize(3));
        assertTrue(containsClient(clients, "example-client"));
        assertTrue(containsClient(clients, "short-living-client"));
        assertTrue(containsClient(clients, "auth-server"));
    }

    private boolean containsClient(List<Map<String, Object>> clients, String clientId) {
        for (Map<String, Object> client : clients) {
            if (client.get("id").equals(clientId)) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void create_client() {
        String clientAsJsonString = "{\"id\":\"example-client-2\",\"accessTokenValiditySeconds\":2342,\"refreshTokenValiditySeconds\":2342,"
                + "\"redirectUri\":\"http://localhost:5055/oauth2\",\"client_secret\":\"secret-2\","
                + "\"scope\":[\"ADMIN\"],"
                + "\"grants\":[\"refresh_token\",\"client_credentials\",\"authorization_code\",\"password\"],"
                + "\"implicit\":false,\"validityInSeconds\":1337}";

        String response = CLIENT.target(AUTH_SERVER_CLIENT_ENDPOINT_ADDRESS)
                .request(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, BEARER + accessToken.getToken())
                .post(Entity.entity(clientAsJsonString, MediaType.APPLICATION_JSON), String.class);

        assertThat(response, containsString("example-client-2"));
    }

    @Test
    public void cant_create_client_with_already_existing_id() {
        String clientAsJsonString = "{\"id\":\"example-client\",\"accessTokenValiditySeconds\":2342,\"refreshTokenValiditySeconds\":2342,"
                + "\"redirectUri\":\"http://localhost:5055/oauth2\",\"client_secret\":\"secret-2\","
                + "\"scope\":[\"ADMIN\"],"
                + "\"grants\":[\"refresh_token\",\"client_credentials\",\"authorization_code\",\"password\"],"
                + "\"implicit\":false,\"validityInSeconds\":1337}";

        Response response = CLIENT.target(AUTH_SERVER_CLIENT_ENDPOINT_ADDRESS)
                .request(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, BEARER + accessToken.getToken())
                .post(Entity.entity(clientAsJsonString, MediaType.APPLICATION_JSON));

        assertThat(response.getStatus(), is(409));
    }

    @Test
    public void delete_client() throws IOException {
        Response deleteResponse = CLIENT.target(AUTH_SERVER_CLIENT_ENDPOINT_ADDRESS)
                .path("short-living-client")
                .request(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, BEARER + accessToken.getToken())
                .delete();

        assertThat(deleteResponse.getStatus(), is(equalTo(Status.OK.getStatusCode())));
        deleteResponse.close();

        Response getResponse = CLIENT.target(AUTH_SERVER_CLIENT_ENDPOINT_ADDRESS)
                .path("short-living-client")
                .request(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, BEARER + accessToken.getToken())
                .get();
        assertThat(getResponse.readEntity(String.class), containsString("NOT_FOUND"));
    }

    @Test
    public void update_client() throws JSONException {
        String clientAsJsonString = "{\"id\":\"example-client\",\"accessTokenValiditySeconds\":1,\"refreshTokenValiditySeconds\":1,"
                + "\"redirectUri\":\"http://newhost:5000/oauth2\",\"client_secret\":\"secret\","
                + "\"scope\":[\"ADMIN\"],"
                + "\"grants\":[\"refresh_token\",\"client_credentials\",\"authorization_code\"],"
                + "\"implicit\":true,\"validityInSeconds\":1}";

        String updated = CLIENT.target(AUTH_SERVER_CLIENT_ENDPOINT_ADDRESS)
                .path("example-client")
                .request(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, BEARER + accessToken.getToken())
                .put(Entity.entity(clientAsJsonString, MediaType.APPLICATION_JSON), String.class);

        String expected = "{\"id\":\"example-client\",\"accessTokenValiditySeconds\":1,\"refreshTokenValiditySeconds\":1,"
                + "\"redirectUri\":\"http://newhost:5000/oauth2\",\"client_secret\":\"secret\","
                + "\"scope\":[\"ADMIN\"],"
                + "\"grants\":[\"refresh_token\",\"client_credentials\",\"authorization_code\"],"
                + "\"implicit\":true,\"validityInSeconds\":1}";

        JSONAssert.assertEquals(expected, updated, false);
    }
}
