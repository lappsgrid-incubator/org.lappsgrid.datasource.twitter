package org.anc.lapps.datasource.twitter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.lappsgrid.metadata.DataSourceMetadata;
import org.lappsgrid.serialization.Data;
import org.lappsgrid.serialization.Serializer;

import static org.lappsgrid.discriminator.Discriminators.Uri;

import static org.junit.Assert.*;

/**
 * @author Keith Suderman
 */
public class TwitterDatasourceTest
{
	private TwitterDatasource twitter;

	@Before
	public void setup()
	{
		twitter = new TwitterDatasource();
	}

	@After
	public void cleanup()
	{
		twitter = null;

	}
	@Test
	public void testMetadata()
	{
		System.out.println("TwitterDatasourceTest.testMetadata");
		String json = twitter.getMetadata();
		DataSourceMetadata metadata = Serializer.parse(json, DataSourceMetadata.class);
		expect("http://www.anc.org", metadata.getVendor());
		expect(Uri.ANY, metadata.getAllow());
		expect(Uri.APACHE2, metadata.getLicense());
		expect("UTF-8", metadata.getEncoding());
		expect(Version.getVersion(), metadata.getVersion());
	}

	@Test
	public void testErrorInput()
	{
		System.out.println("TwitterDatasourceTest.testErrorInput");
		String message = "This is an error message";
		Data<String> data = new Data<>(Uri.ERROR, message);
		String json = twitter.execute(data.asJson());
		assertNotNull("No JSON returned from the service", json);

		data = Serializer.parse(json, Data.class);
		assertEquals("Invalid discriminator returned", Uri.ERROR, data.getDiscriminator());
		assertEquals("The error message has changed.", message, data.getPayload());
	}

	@Test
	public void testInvalidDiscriminator()
	{
		Data<String> data = new Data<>(Uri.QUERY, "Donald Trump");
		String json = twitter.execute(data.asJson());
		assertNotNull("No JSON returned from the service", json);
		data = Serializer.parse(json, Data.class);
		assertEquals("Invalid discriminator returned: " + data.getDiscriminator(), Uri.ERROR, data.getDiscriminator());
		System.out.println(data.getPayload());
	}

	@Test
	public void testExecute()
	{
		System.out.println("TwitterDatasourceTest.testExecute");
		Data<String> data = new Data<>(Uri.GET, "Hillary Clinton");

		// Number of tweets
		data.setParameter("count", 50);
		// Type (Recent or Popular)
		data.setParameter("type", "Recent");
		// Dates since and until
		data.setParameter("since", "2014-05-21");
		data.setParameter("until", "2016-05-21");
		// Language
		data.setParameter("lang", "en");
		// GeoLocation
		data.setParameter("address", "Poughkeepsie, NY, 12604");
		data.setParameter("radius", 10.00);
		data.setParameter("unit", "km");

		String response = twitter.execute(data.asJson());
		System.out.println(response);
	}

	private void expect(String expected, String actual)
	{
		String message = String.format("Expected: %s Actual: %s", expected, actual);
		assertTrue(message, actual.equals(expected));
	}
}
