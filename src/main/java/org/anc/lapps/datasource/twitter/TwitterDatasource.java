package org.anc.lapps.datasource.twitter;

import org.lappsgrid.api.DataSource;
import org.lappsgrid.discriminator.Discriminators;
import org.lappsgrid.metadata.DataSourceMetadata;
import org.lappsgrid.serialization.Data;
import org.lappsgrid.serialization.Serializer;
import org.lappsgrid.serialization.lif.Container;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;


/**
 *
 */
public class TwitterDatasource implements DataSource
{
	public static final String KEY_PROPERTY = "TWITTER_CONSUMER_KEY";
	public static final String SECRET_PROPERTY = "TWITTER_CONSUMER_SECRET";

	private String metadata;
	private static final Logger logger = LoggerFactory.getLogger(TwitterDatasource.class);

	public TwitterDatasource()
	{
		DataSourceMetadata md = new DataSourceMetadata();
		md.setEncoding("UTF-8");
		md.setName("Twitter Datasource");
		md.setLicense(Discriminators.Uri.APACHE2);
		md.setVendor("http://www.anc.org");
		md.setVersion(Version.getVersion());
		md.setAllow(Discriminators.Uri.ANY);
		metadata = Serializer.toPrettyJson(md);
	}

	/**
	 * Returns a JSON string containing metadata describing the service. The
	 * JSON <em>must</em> conform to the json-schema at
	 * <a href="http://vocab.lappsgrid.org/schema/service-schema.json">http://vocab.lappsgrid.org/schema/service-schema.json</a>
	 * (processing services) or
	 * <a href="http://vocab.lappsgrid.org/schema/datasource-schema.json">http://vocab.lappsgrid.org/schema/datasource-schema.json</a>
	 * (datasources).
	 */
	@Override
	public String getMetadata()
	{
		return metadata;
	}

	/**
	 * Entry point for a Lappsgrid service.
	 * <p>
	 * Each service on the Lappsgrid will accept {@code org.lappsgrid.serialization.Data} object
	 * and return a {@code Data} object with a {@code org.lappsgrid.serialization.lif.Container}
	 * payload.
	 * <p>
	 * Errors and exceptions the occur during processing should be wrapped in a {@code Data}
	 * object with the discriminator set to http://vocab.lappsgrid.org/ns/error
	 * <p>
	 * See <a href="https://lapp.github.io/org.lappsgrid.serialization/index.html?org/lappsgrid/serialization/Data.html>org.lappsgrid.serialization.Data</a><br />
	 * See <a href="https://lapp.github.io/org.lappsgrid.serialization/index.html?org/lappsgrid/serialization/lif/Container.html>org.lappsgrid.serialization.lif.Container</a><br />
	 *
	 * @param input A JSON string representing a Data object
	 * @return A JSON string containing a Data object with a Container payload.
	 */
	@Override
	public String execute(String input)
	{
		Data<String> data = Serializer.parse(input, Data.class);
		String discriminator = data.getDiscriminator();
		if (Discriminators.Uri.ERROR.equals(discriminator))
		{
			return input;
		}
		if (!Discriminators.Uri.GET.equals(discriminator))
		{
			return generateError("Invalid discriminator.\nExpected " + Discriminators.Uri.GET + "\nFound " + discriminator);
		}

		Configuration config = new ConfigurationBuilder()
				.setApplicationOnlyAuthEnabled(true)
				.setDebugEnabled(false)
				.build();
		Twitter twitter = new TwitterFactory(config).getInstance();
		String key = readProperty(KEY_PROPERTY);
		if (key == null) {
			return generateError("The Twitter Consumer Key property has not been set.");
		}
		String secret = readProperty(SECRET_PROPERTY);
		if (secret == null)
		{
			return generateError("The Twitter Consumer Secret property has not been set.");
		}
		twitter.setOAuthConsumer(key, secret);
		try
		{
			Query query = new Query(data.getPayload());
            int numberOfTweets = (int) data.getParameter("count");
			twitter.getOAuth2Token();
			//QueryResult result = twitter.search(query);
            if((String) data.getParameter("type") == "Popular")
                query.setResultType(Query.POPULAR);
            if((String) data.getParameter("type") == "Recent")
                query.setResultType(Query.RECENT);
            String untilString = (String) data.getParameter("until");
            if(validateDateFormat(untilString))
                query.setUntil(untilString);
            String sinceString = (String) data.getParameter("since");
            if(validateDateFormat(sinceString))
                query.setSince(sinceString);
            ArrayList<Status> tweets = getTweetsByCount(numberOfTweets, query, twitter);
            StringBuilder builder = new StringBuilder();
			// TODO Package tweets into a org.lappsgrid.serialization.Data object.
			for (Status status : tweets) {
                String single = status.getCreatedAt() + " : " + status.getUser().getScreenName() + " : " + status.getText() + "\n";
                builder.append(single);
            }
            Container container = new Container();
            container.setText(builder.toString());
			Data<Container> output = new Data<>(Discriminators.Uri.LAPPS, container);
			return output.asPrettyJson();
		}
		catch (TwitterException e)
		{
			// TODO Log this error.
			//e.printStackTrace();
			return generateError(e.getMessage());
		}
		// return null;
	}

	private String readProperty(String key)
	{
		String value = System.getProperty(key);
		if (value == null) {
			value = System.getenv(key);
		}
		return value;
	}

	private String generateError(String message)
	{
		Data<String> data = new Data<>();
		data.setDiscriminator(Discriminators.Uri.ERROR);
		data.setPayload(message);
		return data.asPrettyJson();
	}

    private ArrayList<Status> getTweetsByCount(int numberOfTweets, Query query, Twitter twitter) {
        ArrayList<Status> tweets = new ArrayList<>();
        if(!(numberOfTweets > 0)) {
            // Default of 15 tweets
            numberOfTweets = 15;
        }
        long lastID = Long.MAX_VALUE;
        while(tweets.size() < numberOfTweets) {
            if(numberOfTweets - tweets.size() > 100)
                query.setCount(100);
            else
                query.setCount(numberOfTweets - tweets.size());
                try {
                    QueryResult result = twitter.search(query);
                    tweets.addAll(result.getTweets());
                    for(Status status : tweets)
                        if(status.getId() < lastID)
                            lastID = status.getId();
                }

                // TODO log this error
                catch (TwitterException e) {
                    // TODO handle this
                    System.out.println("Failed in getTweetsByCount");
                }
        }
        return tweets;
    }

    private boolean validateDateFormat(String input) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-mm-dd");
        dateFormat.setLenient(false);
        try {
            dateFormat.parse(input.trim());
        } catch (ParseException pe) {
            return false;
        }
        return true;
    }
}
