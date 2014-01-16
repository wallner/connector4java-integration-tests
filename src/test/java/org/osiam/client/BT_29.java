package org.osiam.client;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.osiam.client.query.Query;
import org.osiam.client.query.metamodel.User_;
import org.osiam.resources.scim.SCIMSearchResult;
import org.osiam.resources.scim.User;
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
public class BT_29 extends AbstractIntegrationTestBase {

    private static final String USER_NAME_MARISSA = "marissa";
    private static final String ID_MARISSA = "cef9452e-00a9-4cec-a086-d171374ffbef";

    @Test
    public void searchForUser() {
        for (int i = 0; i < 300; i++) {
            Query q = new Query.Builder(User.class)
                    .setFilter(
                            new Query.Filter(User.class, new Query.Filter(User.class, User_.userName
                                    .equalTo(USER_NAME_MARISSA))))
                    .build();

            SCIMSearchResult<User> result = oConnector.searchUsers(q, accessToken);

            assertThat((int) result.getTotalResults(), greaterThan(0));
        }
    }

    @Test
    public void retrieveUser() {
        for (int i = 0; i < 300; i++) {
            User user = oConnector.getUser(ID_MARISSA, accessToken);

            assertThat(user, is(notNullValue()));
        }
    }
}