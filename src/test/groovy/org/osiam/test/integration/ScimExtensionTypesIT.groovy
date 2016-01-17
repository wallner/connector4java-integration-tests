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

import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method

import org.osiam.client.oauth.Scope
import org.osiam.client.exception.ConflictException
import org.osiam.resources.scim.Extension
import org.osiam.resources.scim.User
import spock.lang.Unroll

class ScimExtensionTypesIT extends AbstractExtensionBaseIT {

    def setup() {
        setupDatabase('/database_seeds/ScimExtensionIT/extensions.xml')
    }

    @Unroll
    def 'Saving \'#fieldValue\' into the extension field \'#fieldName\' which is from type #fieldType raises a exception'() {
        given: "a valid access token"
        Extension extension = new Extension.Builder(URN).setField(fieldName, fieldValue).build()

        User.Builder userBuilder = new User.Builder("irrelevant").addExtension(extension)

        when:
        OSIAM_CONNECTOR.createUser(userBuilder.build(), accessToken)

        then:
        thrown(ConflictException)

        where:
        fieldName            | fieldType            | fieldValue
        FIELD_NAME_BOOLEAN   | FIELD_TYPE_BOOLEAN   | STRING_VALUE
        FIELD_NAME_BOOLEAN   | FIELD_TYPE_BOOLEAN   | INTEGER_VALUE
        FIELD_NAME_BOOLEAN   | FIELD_TYPE_BOOLEAN   | DECIMAL_VALUE
        FIELD_NAME_BOOLEAN   | FIELD_TYPE_BOOLEAN   | DATE_TIME_VALUE
        FIELD_NAME_BOOLEAN   | FIELD_TYPE_BOOLEAN   | BINARY_VALUE
        FIELD_NAME_BOOLEAN   | FIELD_TYPE_BOOLEAN   | REFERENCE_VALUE

        FIELD_NAME_INTEGER   | FIELD_TYPE_INTEGER   | STRING_VALUE
        FIELD_NAME_INTEGER   | FIELD_TYPE_INTEGER   | BOOLEAN_VALUE
        FIELD_NAME_INTEGER   | FIELD_TYPE_INTEGER   | DECIMAL_VALUE
        FIELD_NAME_INTEGER   | FIELD_TYPE_INTEGER   | DATE_TIME_VALUE
        FIELD_NAME_INTEGER   | FIELD_TYPE_INTEGER   | BINARY_VALUE
        FIELD_NAME_INTEGER   | FIELD_TYPE_INTEGER   | REFERENCE_VALUE

        FIELD_NAME_DECIMAL   | FIELD_TYPE_DECIMAL   | STRING_VALUE
        FIELD_NAME_DECIMAL   | FIELD_TYPE_DECIMAL   | BOOLEAN_VALUE
        FIELD_NAME_DECIMAL   | FIELD_TYPE_DECIMAL   | DATE_TIME_VALUE
        FIELD_NAME_DECIMAL   | FIELD_TYPE_DECIMAL   | BINARY_VALUE
        FIELD_NAME_DECIMAL   | FIELD_TYPE_DECIMAL   | REFERENCE_VALUE

        FIELD_NAME_DATE      | FIELD_TYPE_DATE      | STRING_VALUE
        FIELD_NAME_DATE      | FIELD_TYPE_DATE      | BOOLEAN_VALUE
        FIELD_NAME_DATE      | FIELD_TYPE_DATE      | INTEGER_VALUE
        FIELD_NAME_DATE      | FIELD_TYPE_DATE      | DECIMAL_VALUE
        FIELD_NAME_DATE      | FIELD_TYPE_DATE      | BINARY_VALUE

        FIELD_NAME_BINARY    | FIELD_TYPE_BINARY    | STRING_VALUE
        FIELD_NAME_BINARY    | FIELD_TYPE_BINARY    | DECIMAL_VALUE
        FIELD_NAME_BINARY    | FIELD_TYPE_BINARY    | DATE_TIME_VALUE

        FIELD_NAME_REFERENCE | FIELD_TYPE_REFERENCE | STRING_VALUE
    }

    def 'URI: /osiam/extension-definition will return all persisted extension definitions'() {
        given:
        def accessToken = OSIAM_CONNECTOR.retrieveAccessToken("marissa", "koala", Scope.ADMIN)
        def statusCode
        def jsonContent

        when:
        def httpClient = new HTTPBuilder(OSIAM_ENDPOINT)

        httpClient.request(Method.GET) { req ->
            uri.path = OSIAM_ENDPOINT + '/osiam/extension-definition'
            headers.'Authorization' = 'Bearer ' + accessToken.getToken()

            response.success = { resp, json ->
                statusCode = resp.statusLine.statusCode
                jsonContent = json
            }

            response.failure = { resp ->
                statusCode = resp.statusLine.statusCode
            }
        }

        then:
        statusCode == 200
        jsonContent[0].urn == 'extension'
        jsonContent[0].namedTypePairs.birthday == 'DATE_TIME'
        jsonContent[0].namedTypePairs.weight == 'DECIMAL'
        jsonContent[0].namedTypePairs.mother == 'REFERENCE'
        jsonContent[0].namedTypePairs.newsletter == 'BOOLEAN'
        jsonContent[0].namedTypePairs.age == 'INTEGER'
        jsonContent[0].namedTypePairs.gender == 'STRING'
        jsonContent[0].namedTypePairs.photo == 'BINARY'
    }

    def 'URI: /osiam/extension-definition needs an authentication'() {
        given:
        def statusCode

        when:
        def httpClient = new HTTPBuilder(OSIAM_ENDPOINT)

        httpClient.request(Method.GET) { req ->
            uri.path = OSIAM_ENDPOINT + '/osiam/extension-definition'
            headers.'Authorization' = 'Bearer NOT-VALID'

            response.success = { resp, json ->
                statusCode = resp.statusLine.statusCode
                jsonContent = json
            }

            response.failure = { resp ->
                statusCode = resp.statusLine.statusCode
            }
        }

        then:
        statusCode == 401
    }
}
