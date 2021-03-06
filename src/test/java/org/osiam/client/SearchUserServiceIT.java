package org.osiam.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osiam.client.query.Query;
import org.osiam.client.query.QueryResult;
import org.osiam.client.query.SortOrder;
import org.osiam.client.query.metamodel.User_;
import org.osiam.resources.scim.User;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseSetup;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/context.xml")
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class,
        DbUnitTestExecutionListener.class})
@DatabaseSetup("/database_seed.xml")
public class SearchUserServiceIT extends AbstractIntegrationTestBase {

    private static final int ITEMS_PER_PAGE = 3;
    private static final int STARTINDEX_SECOND_PAGE = 4;
    private QueryResult<User> queryResult;

    @Test
    public void search_for_user_by_username() {
        String searchString = encodeExpected("userName eq bjensen");
        whenSearchIsDoneByString(searchString);
        queryResultContainsOnlyValidUser();
    }

    @Test
    public void search_for_user_by_emails_value() {
        String searchString = encodeExpected("emails.value eq bjensen@example.com");
        whenSearchIsDoneByString(searchString);
        queryResultContainsOnlyValidUser();
    }

    @Test
    public void search_for_user_with_multiple_fields() throws UnsupportedEncodingException {
        Query.Filter filter = new Query.Filter(User.class, User_.title.equalTo("Dr."))
                .and(User_.nickName.equalTo("Barbara")).and(User_.displayName.equalTo("BarbaraJ."));
        Query query = new Query.Builder(User.class).setFilter(filter).build();
        whenSearchedIsDoneByQuery(query);
        queryResultContainsOnlyValidUser();
    }

    @Test
    public void search_for_user_by_non_used_username() {
        String searchString = encodeExpected("userName eq " + INVALID_STRING);
        whenSearchIsDoneByString(searchString);
        queryResultDoesNotContainValidUsers();
    }

    @Test
    public void search_for_3_users_by_username_using_and() {
        String user01 = "cmiller";
        String user02 = "hsimpson";
        String user03 = "kmorris";
        String searchString = encodeExpected("userName eq " + user01 + " and userName eq " + user02 + "and userName eq " + user03);
        whenSearchIsDoneByString(searchString);
        queryResultDoesNotContainValidUsers();
    }

    @Test
    public void search_for_3_users_by_username_using_or() {
        String user01 = "cmiller";
        String user02 = "hsimpson";
        String user03 = "kmorris";
        String searchString = encodeExpected("userName eq " + user01 + " or userName eq " + user02 + " or userName eq " + user03);
        whenSearchIsDoneByString(searchString);
        queryResultContainsUser(user01);
        queryResultContainsUser(user02);
        queryResultContainsUser(user03);
    }

    @Test
    public void search_with_braces() throws Exception {
        Query.Filter innerFilter = new Query.Filter(User.class, User_.userName.equalTo("marissa"))
                .or(User_.userName.equalTo("hsimpson"));

        DateTimeFormatter dateFormat = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
        DateTime date = dateFormat.parseDateTime("2000-05-23T13:12:45.672");
        Query.Filter mainFilter = new Query.Filter(User.class, User_.Meta.created.greaterThan(date))
                .and(innerFilter);
        Query.Builder queryBuilder = new Query.Builder(User.class);
        queryBuilder.setFilter(mainFilter);

        queryResult = oConnector.searchUsers(queryBuilder.build(), accessToken);
        assertEquals(2, queryResult.getTotalResults());
        queryResultContainsUser("marissa");
        queryResultContainsUser("hsimpson");
    }

    @Test
    public void nextPage_scrolls_forward() throws UnsupportedEncodingException {
        Query.Builder builder = new Query.Builder(User.class).setCountPerPage(ITEMS_PER_PAGE);
        Query query = builder.build().nextPage();
        whenSearchedIsDoneByQuery(query);
        assertEquals(STARTINDEX_SECOND_PAGE, queryResult.getStartIndex());
    }

    @Test
    public void prevPage_scrolls_backward() throws UnsupportedEncodingException {
        // since OSIAMs default startIndex is wrongly '0' using ITEMS_PER_PAGE works here.
        Query.Builder builder = new Query.Builder(User.class).setCountPerPage(ITEMS_PER_PAGE).setStartIndex(STARTINDEX_SECOND_PAGE);
        Query query = builder.build().previousPage();
        whenSearchedIsDoneByQuery(query);
        assertEquals(1, queryResult.getStartIndex());
    }

    @Test
    public void sorted_search() throws UnsupportedEncodingException {
        Query.Builder queryBuilder = new Query.Builder(User.class);
        queryBuilder.setSortBy(User_.userName).setSortOrder(SortOrder.ASCENDING);
        queryResult = oConnector.searchUsers(queryBuilder.build(), accessToken);

        ArrayList<String> sortedUserNames = new ArrayList<>();
        sortedUserNames.add("bjensen");
        sortedUserNames.add("jcambell");
        sortedUserNames.add("adavies");
        sortedUserNames.add("cmiller");
        sortedUserNames.add("dcooper");
        sortedUserNames.add("epalmer");
        sortedUserNames.add("gparker");
        sortedUserNames.add("hsimpson");
        sortedUserNames.add("kmorris");
        sortedUserNames.add("ewilley");
        sortedUserNames.add("marissa");
        Collections.sort(sortedUserNames);

        assertEquals(sortedUserNames.size(), queryResult.getTotalResults());
        int count = 0;
        for (User actUser : queryResult.getResources()) {
            assertEquals(sortedUserNames.get(count++), actUser.getUserName());
        }
    }
    
    @Test
    public void get_all_user_if_over_hundert_user_exists(){
    	create100NewUser();
    	List<User> allUsers = oConnector.getAllUsers(accessToken);
    	assertEquals(111, allUsers.size());
    }
    
    private void create100NewUser(){
		for(int count = 0; count < 100; count++){
    		User user = new User.Builder("user" + count).build();
    		oConnector.createUser(user, accessToken);
    	}	    	
    }
    
    private void queryResultContainsUser(String userName) {
        assertTrue(queryResult != null);
        for (User actUser : queryResult.getResources()) {
            if (actUser.getUserName().equals(userName)) {
                return; // OK
            }
        }
        fail("User " + userName + " could not be found.");
    }

    private void queryResultContainsOnlyValidUser() {
        assertTrue(queryResult != null);
        assertEquals(1, queryResult.getTotalResults());
        queryResultContainsValidUser();
    }

    private void queryResultDoesNotContainValidUsers() {
        assertTrue(queryResult != null);
        assertEquals(0, queryResult.getTotalResults());
    }

    private void whenSearchIsDoneByString(String queryString) {
        queryResult = oConnector.searchUsers("filter=" + queryString, accessToken);
    }

    private void queryResultContainsValidUser() {
        assertTrue(queryResult != null);
        for (User actUser : queryResult.getResources()) {
            if (actUser.getId().equals(VALID_USER_ID)) {
                return; // OK
            }
        }
        fail("Valid user could not be found.");
    }

    private void whenSearchedIsDoneByQuery(Query query) {
        queryResult = oConnector.searchUsers(query, accessToken);
    }

}
