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

package org.osiam.test.integration

import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import org.osiam.client.exception.UnauthorizedException
import org.osiam.client.oauth.AccessToken
import org.osiam.client.oauth.Scope
import org.osiam.resources.scim.Email
import org.osiam.resources.scim.UpdateUser
import org.osiam.resources.scim.User
import spock.lang.Unroll

/**
 * Base class for server integration tests.
 *
 */
class ControllerIT extends AbstractIT {

    def setup() {
        setupDatabase('database_seed.xml')
    }

    @Unroll
    def "REGT-001-#testCase: An API request missing an accept header with scope #scope and content type #contentType on path #requestPath should return HTTP status code #expectedResponseCode and content type #expectedResponseType."() {
        given: "a valid access token"

        when: "a request is sent"
        def http = new HTTPBuilder(RESOURCE_ENDPOINT)

        def responseStatusCode
        def responseContentType

        http.request(Method.GET, contentType) { req ->
            uri.path = RESOURCE_ENDPOINT + requestPath
            headers."Authorization" = "Bearer " + accessToken.getToken()

            // response handler for a success response code:
            response.success = { resp, json ->
                responseStatusCode = resp.statusLine.statusCode
                responseContentType = resp.headers."Content-Type"
            }

            // handler for any failure status code:
            response.failure = { resp ->
                responseStatusCode = resp.statusLine.statusCode
                contentType = resp.headers."Content-Type"
            }

        }

        then: "the response should be as expected"
        assert responseStatusCode == expectedResponseCode

        expect: "the response type should be as expected"
        assert responseContentType == expectedResponseType

        where:
        testCase | requestPath                    | contentType        | expectedResponseCode | expectedResponseType
        "a"      | "/Users"                       | ContentType.JSON   | 200                  | "application/json;charset=UTF-8"
        "b"      | "/Users/"                      | ContentType.JSON   | 200                  | "application/json;charset=UTF-8"
        "c"      | "/Groups"                      | ContentType.JSON   | 200                  | "application/json;charset=UTF-8"
        "d"      | "/Groups/"                     | ContentType.JSON   | 200                  | "application/json;charset=UTF-8"
        "e"      | "/Users"                       | ContentType.ANY    | 200                  | "application/json;charset=UTF-8"
        "f"      | "/Users"                       | ContentType.TEXT   | 406                  | null
        "g"      | "/Users"                       | ContentType.BINARY | 406                  | null
        "i"      | "/Users"                       | ContentType.URLENC | 406                  | null
        "j"      | "/Users"                       | ContentType.XML    | 406                  | null
        "k"      | "/Users"                       | "invalid"          | 406                  | null
        "l"      | "/Users"                       | "/"                | 406                  | null
        "m"      | "/Metrics"                     | ContentType.JSON   | 200                  | "application/json;charset=UTF-8"
        "n"      | "/Metrics/"                    | ContentType.JSON   | 200                  | "application/json;charset=UTF-8"
        "o"      | "/osiam/extension-definition"  | ContentType.JSON   | 200                  | "application/json;charset=UTF-8"
        "p"      | "/osiam/extension-definition/" | ContentType.JSON   | 200                  | "application/json;charset=UTF-8"
    }

    @Unroll
    def 'REGT-002-#testCase: A search operation on the Users endpoint with search string #searchString should return HTTP status code #expectedResponseCode.'() {
        given: "a valid access token"
        AccessToken validAccessToken = OSIAM_CONNECTOR.retrieveAccessToken(Scope.ADMIN)

        when: "a request is sent"
        def http = new HTTPBuilder(RESOURCE_ENDPOINT)

        def responseStatusCode
        def responseContentType
        def responseErrorCode

        http.request(Method.GET, ContentType.JSON) { req ->
            uri.path = RESOURCE_ENDPOINT + "/Users"
            uri.query = [filter: searchString]
            headers."Authorization" = "Bearer " + validAccessToken.getToken()

            // response handler for a success response code:
            response.success = { resp, json ->
                responseStatusCode = resp.statusLine.statusCode
                responseContentType = resp.headers."Content-Type"
            }

            // handler for any failure status code:
            response.failure = { resp, json ->
                responseErrorCode = json.status
                responseStatusCode = resp.statusLine.statusCode
                contentType = resp.headers."Content-Type"
            }

        }

        then: "the response and possible failure codes and text should be as expected"
        assert responseStatusCode == expectedResponseCode
        assert responseErrorCode == expectedResponseErrorCode

        where:
        testCase | searchString                                | expectedResponseCode | expectedResponseErrorCode
        'a'      | 'userName eq "marissa"'                     | 200                  | null                      // String
        'b'      | 'userName co "ari"'                         | 200                  | null                      // String
        'c'      | 'userName sw "mar"'                         | 200                  | null                      // String
        'd'      | 'userName pr'                               | 200                  | null                      // String
        'e'      | 'userName gt "l"'                           | 200                  | null                      // String
        'f'      | 'userName ge "m"'                           | 200                  | null                      // String
        'g'      | 'userName lt "n"'                           | 200                  | null                      // String
        'h'      | 'userName le "m"'                           | 200                  | null                      // String
        'i'      | 'emails.type eq "work"'                     | 200                  | null                      // Enum (EmailEntity)
        'j'      | 'emails.type co "work"'                     | 400                  | '400'                     // Enum (EmailEntity)
        'k'      | 'emails.type sw "work"'                     | 400                  | '400'                     // Enum (EmailEntity)
        'l'      | 'emails.type pr'                            | 200                  | null                      // Enum (EmailEntity)
        'm'      | 'emails.type gt "work"'                     | 400                  | '400'                     // Enum (EmailEntity)
        'n'      | 'emails.type ge "work"'                     | 400                  | '400'                     // Enum (EmailEntity)
        'o'      | 'emails.type lt "work"'                     | 400                  | '400'                     // Enum (EmailEntity)
        'p'      | 'emails.type le "work"'                     | 400                  | '400'                     // Enum (EmailEntity)
        'q'      | 'active eq "true"'                          | 200                  | null                      // boolean
        'r'      | 'active co "true"'                          | 400                  | '400'                     // boolean
        's'      | 'active sw "true"'                          | 400                  | '400'                     // boolean
        't'      | 'active pr'                                 | 200                  | null                      // boolean
        'u'      | 'active gt "true"'                          | 400                  | '400'                     // boolean
        'v'      | 'active ge "true"'                          | 400                  | '400'                     // boolean
        'w'      | 'active lt "true"'                          | 400                  | '400'                     // boolean
        'x'      | 'active le "true"'                          | 400                  | '400'                     // boolean
        'y'      | 'meta.created co "2013-08-08T19:46:20.638"' | 400                  | '400'                     // Date
        'z'      | 'meta.created sw "2013-08-08T1"'            | 400                  | '400'                     // Date
        'za'     | 'active pr "true"'                          | 400                  | '400'                     // pr with value
    }

    def "REGT-005: A search filter String matching two users should return totalResults=2 and two unique Resource elements."() {
        given: "a valid access token"
        AccessToken validAccessToken = OSIAM_CONNECTOR.retrieveAccessToken(Scope.ADMIN)

        when: "a filter request matching two users is sent"
        def http = new HTTPBuilder(RESOURCE_ENDPOINT)

        def responseStatusCode
        def responseContent

        http.request(Method.GET, ContentType.JSON) { req ->
            uri.path = RESOURCE_ENDPOINT + "/Users"
            uri.query = [filter: '(userName eq "cmiller" or userName eq "hsimpson") and meta.created gt "2003-05-23T13:12:45.672"']
            headers."Authorization" = "Bearer " + validAccessToken.getToken()

            // response handler for a success response code:
            response.success = { resp, json ->
                responseStatusCode = resp.statusLine.statusCode
                responseContent = json
            }
        }

        then: "the response elements should be unique and as expected"
        assert responseStatusCode == 200
        assert responseContent.totalResults == 2

        assert responseContent.Resources.size() == 2

        // Check uniqueness to prevent counting faulty items. Also check userName's.
        Collection elements = new HashSet()
        responseContent.Resources.each {
            assert elements.add(it) // Returns 'false' if already in HashSet.
            assert (it.toString().contains("cmiller") || it.toString().contains("hsimpson"))
        }
    }

    def 'REGT-OSNG-141: E-Mail address should not be unique. So two different users should be able to add the same address and getting displayed only the own entry.'() {

        given: 'a valid access token and two users with the same E-Mail address'
        AccessToken validAccessToken = OSIAM_CONNECTOR.retrieveAccessToken(Scope.ADMIN)
        def emailUserOne = new Email.Builder().setType(Email.Type.WORK).setValue('sameMail@osiam.de').build()
        def emailUserTwo = new Email.Builder().setType(Email.Type.HOME).setValue('sameMail@osiam.de').build()
        def user1 = new User.Builder('UserOne').addEmails([emailUserOne] as List).setExternalId('pew1').build()
        def user2 = new User.Builder('UserTwo').addEmails([emailUserTwo] as List).setExternalId('pew2').build()

        when: 'a add user request is sent'
        User retUser1 = OSIAM_CONNECTOR.createUser(user1, validAccessToken)
        User retUser2 = OSIAM_CONNECTOR.createUser(user2, validAccessToken)

        then: 'the response elements should contain the expected email for each user'
        assert retUser1.emails != retUser2.emails
        assert retUser1.emails[0].value == retUser2.emails[0].value
        assert retUser1.emails[0].type != retUser2.emails[0].type
        assert retUser1.emails[0].primary == retUser2.emails[0].primary
    }

    def "REGT-OSNG-37: The token validation should not raise an exception in case of the OAuth2 client credentials grant because of missing user authentication"() {

        given: "a valid access token"
        AccessToken validAccessToken = OSIAM_CONNECTOR.retrieveAccessToken(Scope.ADMIN)
        def responseStatusCode
        def responseContent

        when: "retrieving a user"
        new HTTPBuilder(RESOURCE_ENDPOINT).request(Method.GET, ContentType.JSON) { req ->
            uri.path = RESOURCE_ENDPOINT + "/Users"
            uri.query = [filter: 'userName eq "marissa"']
            headers."Authorization" = "Bearer " + validAccessToken.getToken()

            response.success = { resp, json ->
                responseStatusCode = resp.statusLine.statusCode
                responseContent = json
            }

            response.failure = { resp ->
                responseStatusCode = resp.statusLine.statusCode
            }
        }

        then: "the user should be retrieved without triggering an exception"
        responseStatusCode == 200
        responseContent.Resources[0].userName == 'marissa'
    }

    def 'OSNG-444: A request to revoke a valid token should invalidate the token'() {

        given: 'a valid access token'
        AccessToken accessToken = OSIAM_CONNECTOR.retrieveAccessToken(Scope.ADMIN)

        when: 'a token revocation is performed'
        AccessToken validationResult = OSIAM_CONNECTOR.validateAccessToken(accessToken)
        OSIAM_CONNECTOR.revokeAccessToken(accessToken)
        OSIAM_CONNECTOR.validateAccessToken(accessToken) // authorization should now be invalid

        then: 'the token should be revoked'
        validationResult.expired == false
        thrown(UnauthorizedException)
    }

    def 'OSNG-444: A request to revoke an invalid token is not authorized'() {

        given: 'an invalid access token'
        AccessToken accessToken = new AccessToken.Builder("invalid").build()

        when: 'a token revocation is performed'
        OSIAM_CONNECTOR.revokeAccessToken(accessToken)

        then: 'the request is not authorized'
        thrown(UnauthorizedException)
    }

    def 'OSNG-444: Subsequent requests to revoke a valid token should not be authorized'() {

        given: 'a valid access token'
        AccessToken accessToken = OSIAM_CONNECTOR.retrieveAccessToken(Scope.ADMIN)

        when: 'multiple token revocations are performed'
        AccessToken validationResult = OSIAM_CONNECTOR.validateAccessToken(accessToken)
        OSIAM_CONNECTOR.revokeAccessToken(accessToken)
        OSIAM_CONNECTOR.revokeAccessToken(accessToken)

        then: 'subsequent requests are not authorized'
        thrown(UnauthorizedException)
    }

    def 'OSNG-467: A request to revoke access tokens of a given user should invalidate his token'() {
        given: 'a valid access token'
        def userId = "cef9452e-00a9-4cec-a086-d171374ffbef"
        AccessToken serviceAccessToken = OSIAM_CONNECTOR.retrieveAccessToken(Scope.ADMIN)

        when: 'a token revocation is performed'
        AccessToken validationResult = OSIAM_CONNECTOR.validateAccessToken(accessToken)
        OSIAM_CONNECTOR.revokeAllAccessTokens(userId, serviceAccessToken)
        validationResult = OSIAM_CONNECTOR.validateAccessToken(accessToken)

        then: 'the tokens should be invalid'
        validationResult.expired == false
        thrown(UnauthorizedException)
    }

    def 'OSNG-467: Repeating requests to revoke access tokens of a given user should not have negative effect'() {
        given: 'valid access tokens'
        def userId = "cef9452e-00a9-4cec-a086-d171374ffbef"
        AccessToken serviceAccessToken = OSIAM_CONNECTOR.retrieveAccessToken(Scope.ADMIN)

        when: 'multiple token revocations are performed'
        OSIAM_CONNECTOR.revokeAllAccessTokens(userId, serviceAccessToken)
        OSIAM_CONNECTOR.revokeAllAccessTokens(userId, serviceAccessToken)

        then: 'nothing should happen'
    }

    def 'OSNG-467: Deactivating a user should revoke his access token'() {
        given: 'active user with valid access token'
        def userId = "cef9452e-00a9-4cec-a086-d171374ffbef"
        AccessToken serviceAccessToken = OSIAM_CONNECTOR.retrieveAccessToken(Scope.ADMIN)
        UpdateUser updateUser = new UpdateUser.Builder().updateActive(false).build()

        when: 'the user is deactivated'
        AccessToken validationResult = OSIAM_CONNECTOR.validateAccessToken(accessToken) // should be valid
        User updatedUser = OSIAM_CONNECTOR.updateUser(userId, updateUser, serviceAccessToken)
        validationResult = OSIAM_CONNECTOR.validateAccessToken(accessToken) // should not be authorized

        then: 'the user should be deactivated and the access token should be revoked'
        updatedUser.isActive() == false
        thrown(UnauthorizedException)
    }

    def 'OSNG-467: Updating a user without deactivating him should not revoke his access token'() {
        given: 'active user with valid access token'
        def userId = "cef9452e-00a9-4cec-a086-d171374ffbef"
        AccessToken serviceAccessToken = OSIAM_CONNECTOR.retrieveAccessToken(Scope.ADMIN)
        UpdateUser updateUser = new UpdateUser.Builder().updateDisplayName('Marissa').build()

        when: 'the user is updated'
        User updatedUser = OSIAM_CONNECTOR.updateUser(userId, updateUser, serviceAccessToken)
        AccessToken validationResult = OSIAM_CONNECTOR.validateAccessToken(accessToken)

        then: 'update was successful and the token is still valid'
        updatedUser.getDisplayName() == 'Marissa'
        validationResult.expired == false
    }

    def 'OSNG-467: Replacing a user with deactivating him should revoke his access token'() {
        given: 'active user with valid access token'
        def userId = "cef9452e-00a9-4cec-a086-d171374ffbef"
        AccessToken serviceAccessToken = OSIAM_CONNECTOR.retrieveAccessToken(Scope.ADMIN)
        User user = OSIAM_CONNECTOR.getUser(userId, serviceAccessToken)
        User newUser = new User.Builder(user).setActive(false).build()

        when: 'the user is replaced'
        AccessToken validationResult = OSIAM_CONNECTOR.validateAccessToken(accessToken) // should be valid
        User replacedUser = OSIAM_CONNECTOR.replaceUser(userId, newUser, serviceAccessToken)
        validationResult = OSIAM_CONNECTOR.validateAccessToken(accessToken) // should not be authorized

        then: 'the user should be deactivated and the access token should be revoked'
        replacedUser.isActive() == false
        thrown(UnauthorizedException)
    }

    def 'OSNG-467: Replacing a user without deactivating him should not revoke his access token'() {
        given: 'active user with valid access token'
        def userId = "cef9452e-00a9-4cec-a086-d171374ffbef"
        AccessToken serviceAccessToken = OSIAM_CONNECTOR.retrieveAccessToken(Scope.ADMIN)
        User user = OSIAM_CONNECTOR.getUser(userId, serviceAccessToken)
        User newUser = new User.Builder(user).setDisplayName('Marissa').build()

        when: 'the user is replaced'
        User replacedUser = OSIAM_CONNECTOR.replaceUser(userId, newUser, serviceAccessToken)
        AccessToken validationResult = OSIAM_CONNECTOR.validateAccessToken(accessToken)

        then: 'update was successful and the token is still valid'
        replacedUser.getDisplayName() == 'Marissa'
        validationResult.expired == false
    }

    def 'OSNG-479: Deleting a user should revoke his access token'() {
        given: 'active user with valid access token'
        def userId = "cef9452e-00a9-4cec-a086-d171374ffbef"
        AccessToken serviceAccessToken = OSIAM_CONNECTOR.retrieveAccessToken(Scope.ADMIN)

        when: 'the user is deleted'
        AccessToken validationResult = OSIAM_CONNECTOR.validateAccessToken(accessToken) // should be valid
        OSIAM_CONNECTOR.deleteUser(userId, serviceAccessToken)
        validationResult = OSIAM_CONNECTOR.validateAccessToken(accessToken) // should not be authorized

        then: 'the access token should be revoked'
        validationResult.expired == false
        thrown(UnauthorizedException)
    }
}
