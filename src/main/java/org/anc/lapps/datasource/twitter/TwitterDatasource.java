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
 * @author Keith Suderman
 * @author Alexandru Mahmoud
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


        try {
            twitter.getOAuth2Token();
        } catch (TwitterException te) {
            String errorData = generateError(te.getMessage());
            logger.error(errorData);
            return errorData;
        }

		//QueryResult result = twitter.search(query);

        // Get query String from data payload
        Query query = new Query(data.getPayload());

        // Set the type to Popular or Recent if specified
        // Results will be Mixed by default.
        if(data.getParameter("type") == "Popular")
            query.setResultType(Query.POPULAR);
        if(data.getParameter("type") == "Recent")
            query.setResultType(Query.RECENT);

		// Get lang string
		String langCode = (String) data.getParameter("lang");

		// Verify the validity of the language code and add it to the query if it's valid
		if(validateLangCode(langCode))
			query.setLang(langCode);

        // Get date strings
        String sinceString = (String) data.getParameter("since");
        String untilString = (String) data.getParameter("until");


        // Verify the format of the date strings and set the parameters to query if correctly given
        if(validateDateFormat(untilString))
            query.setUntil(untilString);
        if(validateDateFormat(sinceString))
            query.setSince(sinceString);

        int numberOfTweets;

        // Get the number of tweets from count parameter, and set it to default = 15 if not specified
        try {
            numberOfTweets = (int) data.getParameter("count");
        }
        catch(NullPointerException e) {
            numberOfTweets = 15;
        }

        // Generate an ArrayList of the wanted number of tweets, and handle possible errors.
        // This is meant to avoid the 100 tweet limit set by twitter4j and extract as many tweets as needed
        ArrayList<Status> tweets;
        String tweetsDataJson = getTweetsByCount(numberOfTweets, query, twitter);
        String tweetsDataDisc = Serializer.parse(tweetsDataJson, Data.class).getDiscriminator();
        if (Discriminators.Uri.ERROR.equals(tweetsDataDisc))
            return tweetsDataJson;
        else {
            Data<ArrayList<Status>> tweetsData = Serializer.parse(tweetsDataJson, Data.class);
            tweets = tweetsData.getPayload();
        }


        // Initialize StringBuilder to hold the final string
        StringBuilder builder = new StringBuilder();

        // Append each Status (each tweet) to the initialized builder
		for (Status status : tweets) {
            String single = status.getCreatedAt() + " : " + status.getUser().getScreenName() + " : " + status.getText() + "\n";
                builder.append(single);
        }

        // Output results
        Container container = new Container();
        container.setText(builder.toString());
		Data<Container> output = new Data<>(Discriminators.Uri.LAPPS, container);
		return output.asPrettyJson();
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

    /** Contacts the Twitter API and gets any number of tweets corresponding to a certain query. The main
     * purpose of this function is to avoid the limit of 100 tweets that can be extracted at once.
     *
     * @param numberOfTweets the number of tweets to be printed
     * @param query the query to be searched by the twitter client
     * @param twitter the twitter client
     *
     * @return A JSON string containing a Data object with either a list containing the tweets as a payload
     * (when successful) or a String payload (for errors).
     */
    private String getTweetsByCount(int numberOfTweets, Query query, Twitter twitter) {
        ArrayList<Status> tweets = new ArrayList<>();
        if(!(numberOfTweets > 0)) {
            // Default of 15 tweets
            numberOfTweets = 15;
        }
        // Set the last ID to the maximum possible value as a default
        long lastID = Long.MAX_VALUE;

        try {
            while (tweets.size() < numberOfTweets) {
                // If there are still more than 100 tweets to be extracted, extract
                // 100 during the next query, since 100 is the limit number of tweets
                // that can be extracted at once
                if (numberOfTweets - tweets.size() > 100)
                    query.setCount(100);
                else
                    query.setCount(numberOfTweets - tweets.size());
                // Extract tweets corresponding to the query then add them to the list
                QueryResult result = twitter.search(query);
                tweets.addAll(result.getTweets());

                // Iterate through the list and get the lastID to know where to start from
                // if there are more tweets to be extracted
                for (Status status : tweets)
                    if (status.getId() < lastID)
                        lastID = status.getId();
            }
        }
        catch (TwitterException te) {
            // Put the list of tweets in Data format then output as JSon String.
            // Since we checked earlier for errors, we assume that an error occuring at this point due
            // to Rate Limits is caused by a too high request. Thus, we output the retrieved tweets and log
            // the error
            String errorData = generateError(te.getMessage());
            logger.error(errorData);
            if(te.exceededRateLimitation()) {
                Data<ArrayList<Status>> tweetsData = new Data<>();
                tweetsData.setDiscriminator(Discriminators.Uri.LIST);
                tweetsData.setPayload(tweets);
                return tweetsData.asJson();
            }
            else
                return errorData;
        }
        // Put the list of tweets in Data format then output as JSon String.
        Data<ArrayList<Status>> tweetsData = new Data<>();
        tweetsData.setDiscriminator(Discriminators.Uri.LIST);
        tweetsData.setPayload(tweets);
        return tweetsData.asJson();
    }

    /** Outputs whether the format of the input strings corresponds to the
     * date format (YYYY-MM-DD) needed for the query.
     *
     * @param input A string representing the date
     * @return a boolean corresponding to whether the input string has
     * the format YYYY-MM-DD
     *
      */
    private boolean validateDateFormat(String input) {
        if(input == null)
            return false;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-mm-dd");
        dateFormat.setLenient(false);
        try {
            dateFormat.parse(input.trim());
        } catch (ParseException pe) {
            return false;
        }
        return true;
    }


	/** Outputs whether the input is part of the list of valid ISO 639-1 codes
	 * for specifying query language criteria
	 *
	 * @param input A string representing a language code
	 * @return a boolean corresponding to whether the input string is valid
     */
	private boolean validateLangCode(String input) {
		if(input == null)
			return false;
		// TODO check what codes twitter accepts (this is a list of all ISO 639-1 codes from Wikipedia)
		String allcodes = "ab, aa, af, ak, sq, am, ar, an, hy, as, av, ae, ay, az, bm, ba, eu, be, bn, bh, bi, bs, br, bg, my, ca, ch, ce, ny, zh, cv, kw, co, cr, hr, cs, da, dv, nl, dz, en, eo, et, ee, fo, fj, fi, fr, ff, gl, ka, de, el, gn, gu, ht, ha, he, hz, hi, ho, hu, ia, id, ie, ga, ig, ik, io, is, it, iu, ja, jv, kl, kn, kr, ks, kk, km, ki, rw, ky, kv, kg, ko, ku, kj, la, lb, lg, li, ln, lo, lt, lu, lv, gv, mk, mg, ms, ml, mt, mi, mr, mh, mn, na, nv, nd, ne, ng, nb, nn, no, ii, nr, oc, oj, cu, om, or, os, pa, pi, fa, pl, ps, pt, qu, rm, rn, ro, ru, sa, sc, sd, se, sm, sg, sr, gd, sn, si, sk, sl, so, st, es, su, sw, ss, sv, ta, te, tg, th, ti, bo, tk, tl, tn, to, tr, ts, tt, tw, ty, ug, uk, ur, uz, ve, vi, vo, wa, cy, wo, fy, xh, yi, yo, za, zu";
		return allcodes.contains(input);
	}
}
