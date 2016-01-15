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

import org.osiam.client.exception.ConflictException
import org.osiam.client.query.Query
import org.osiam.client.query.QueryBuilder
import org.osiam.resources.scim.SCIMSearchResult
import spock.lang.Unroll

public class ScimExtensionSearchIT extends AbstractExtensionBaseIT {

    def setup() {
        setupDatabase('/database_seeds/ScimExtensionSearchIT/user_by_extension.xml')
    }

    @Unroll
    def 'search for user by #fieldType extension field and constraint #constraint separated by ":" works'() {

        given:
        Query query = new QueryBuilder().filter("extension:$fieldName $constraint \"$queryValue\"").build()

        when:
        SCIMSearchResult result = OSIAM_CONNECTOR.searchUsers(query, accessToken)

        then:
        result.getTotalResults() == expectedResult

        where:
        fieldType            | fieldName            | constraint | queryValue                            | expectedResult
        FIELD_TYPE_STRING    | FIELD_NAME_STRING    | 'eq'       | 'female'                              | 3
        FIELD_TYPE_STRING    | FIELD_NAME_STRING    | 'co'       | 'mal'                                 | 5
        FIELD_TYPE_STRING    | FIELD_NAME_STRING    | 'sw'       | 'fe'                                  | 3
        FIELD_TYPE_STRING    | FIELD_NAME_STRING    | 'gt'       | 'female'                              | 2
        FIELD_TYPE_STRING    | FIELD_NAME_STRING    | 'ge'       | 'male'                                | 2
        FIELD_TYPE_STRING    | FIELD_NAME_STRING    | 'lt'       | 'male'                                | 3
        FIELD_TYPE_STRING    | FIELD_NAME_STRING    | 'le'       | 'female'                              | 3
        FIELD_TYPE_INTEGER   | FIELD_NAME_INTEGER   | 'eq'       | 30                                    | 1
        FIELD_TYPE_INTEGER   | FIELD_NAME_INTEGER   | 'gt'       | 30                                    | 1
        FIELD_TYPE_INTEGER   | FIELD_NAME_INTEGER   | 'ge'       | 30                                    | 2
        FIELD_TYPE_INTEGER   | FIELD_NAME_INTEGER   | 'lt'       | 30                                    | 3
        FIELD_TYPE_INTEGER   | FIELD_NAME_INTEGER   | 'le'       | 30                                    | 4
        FIELD_TYPE_DECIMAL   | FIELD_NAME_DECIMAL   | 'eq'       | 80.7                                  | 1
        FIELD_TYPE_DECIMAL   | FIELD_NAME_DECIMAL   | 'gt'       | 80.7                                  | 1
        FIELD_TYPE_DECIMAL   | FIELD_NAME_DECIMAL   | 'ge'       | 80.7                                  | 2
        FIELD_TYPE_DECIMAL   | FIELD_NAME_DECIMAL   | 'lt'       | 80.7                                  | 3
        FIELD_TYPE_DECIMAL   | FIELD_NAME_DECIMAL   | 'le'       | 80.7                                  | 4
        FIELD_TYPE_BOOLEAN   | FIELD_NAME_BOOLEAN   | 'eq'       | true                                  | 3
        FIELD_TYPE_DATE      | FIELD_NAME_DATE      | 'eq'       | dateAsString(1986, 11, 11, 4, 56, 22) | 1
        FIELD_TYPE_DATE      | FIELD_NAME_DATE      | 'gt'       | dateAsString(1986, 11, 11, 4, 56, 22) | 2
        FIELD_TYPE_DATE      | FIELD_NAME_DATE      | 'ge'       | dateAsString(1986, 11, 11, 4, 56, 22) | 3
        FIELD_NAME_DATE      | FIELD_NAME_DATE      | 'lt'       | dateAsString(1986, 11, 11, 4, 56, 22) | 2
        FIELD_TYPE_DATE      | FIELD_NAME_DATE      | 'le'       | dateAsString(1986, 11, 11, 4, 56, 22) | 3
        FIELD_TYPE_REFERENCE | FIELD_NAME_REFERENCE | 'eq'       | 'https://beta.example.com/Users/28'   | 1
        FIELD_TYPE_REFERENCE | FIELD_NAME_REFERENCE | 'co'       | 'beta'                                | 1
        FIELD_TYPE_REFERENCE | FIELD_NAME_REFERENCE | 'sw'       | 'https://beta'                        | 1

    }

    @Unroll
    def 'search for user by extension field separated by "." works'() {
        given:
        Query query = new QueryBuilder().filter("extension.$fieldName $constraint \"$queryValue\"").build()

        when:
        SCIMSearchResult result = OSIAM_CONNECTOR.searchUsers(query, accessToken)

        then:
        result.getTotalResults() == expectedResult

        where:
        fieldType         | fieldName         | constraint | queryValue | expectedResult
        FIELD_TYPE_STRING | FIELD_NAME_STRING | 'eq'       | 'female'   | 3
        FIELD_TYPE_STRING | FIELD_NAME_STRING | 'co'       | 'mal'      | 5
        FIELD_TYPE_STRING | FIELD_NAME_STRING | 'sw'       | 'fe'       | 3
    }

    @Unroll
    def 'search for user by #fieldType extension field and constraint #constraint with query string raises exception'() {
        given:
        Query query = new QueryBuilder().filter("extension:$fieldName $constraint \"irrelevant\"").build()

        when:
        OSIAM_CONNECTOR.searchUsers(query, accessToken)

        then:
        thrown(ConflictException)

        where:
        fieldType            | fieldName            | constraint
        FIELD_TYPE_INTEGER   | FIELD_NAME_INTEGER   | 'co'
        FIELD_TYPE_INTEGER   | FIELD_NAME_INTEGER   | 'sw'
        FIELD_TYPE_BOOLEAN   | FIELD_NAME_BOOLEAN   | 'co'
        FIELD_TYPE_BOOLEAN   | FIELD_NAME_BOOLEAN   | 'sw'
        FIELD_TYPE_BOOLEAN   | FIELD_NAME_BOOLEAN   | 'gt'
        FIELD_TYPE_BOOLEAN   | FIELD_NAME_BOOLEAN   | 'ge'
        FIELD_TYPE_BOOLEAN   | FIELD_NAME_BOOLEAN   | 'lt'
        FIELD_TYPE_BOOLEAN   | FIELD_NAME_BOOLEAN   | 'le'
        FIELD_TYPE_DATE      | FIELD_TYPE_DATE      | 'co'
        FIELD_TYPE_DATE      | FIELD_TYPE_DATE      | 'sw'
        FIELD_TYPE_BINARY    | FIELD_NAME_BINARY    | 'eq'
        FIELD_TYPE_BINARY    | FIELD_NAME_BINARY    | 'co'
        FIELD_TYPE_BINARY    | FIELD_NAME_BINARY    | 'sw'
        FIELD_TYPE_BINARY    | FIELD_NAME_BINARY    | 'gt'
        FIELD_TYPE_BINARY    | FIELD_NAME_BINARY    | 'ge'
        FIELD_TYPE_BINARY    | FIELD_NAME_BINARY    | 'lt'
        FIELD_TYPE_BINARY    | FIELD_NAME_BINARY    | 'le'
        FIELD_TYPE_REFERENCE | FIELD_NAME_REFERENCE | 'gt'
        FIELD_TYPE_REFERENCE | FIELD_NAME_REFERENCE | 'ge'
        FIELD_TYPE_REFERENCE | FIELD_NAME_REFERENCE | 'lt'
        FIELD_TYPE_REFERENCE | FIELD_NAME_REFERENCE | 'le'
    }
}
