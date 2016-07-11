## org.lappsgrid.datasource.twitter
Twitter Datasource

# Usage

Takes a Data<String> object with a Uri.GET ("http://vocab.lappsgrid.org/ns/action/get") discriminator and a string payload representing the search string for the query. Optional parameters are listed below.

# Query operators
(This is from the Twitter API help page ("https://dev.twitter.com/rest/public/search")

The query can have operators that modify its behavior, the available operators are:

| Operator | Finds tweets… |
| --- | --- |
| watching now | containing both “watching” and “now”. This is the default operator. |
| “happy hour” | containing the exact phrase “happy hour”. |
| love OR hate | containing either “love” or “hate” (or both). |
| beer -root | containing “beer” but not “root”. |
| #haiku | containing the hashtag “haiku”. |
| from:interior | sent from Twitter account “interior”. |
| list:NASA/astronauts-in-space-now | sent from a Twitter account in the NASA list astronauts-in-space-now |
| to:NASA | a Tweet authored in reply to Twitter account “NASA”. |
| @NASA | mentioning Twitter account “NASA”. |
| politics filter:safe | containing “politics” with Tweets marked as potentially sensitive removed. |
| puppy filter:media | containing “puppy” and an image or video. |
| puppy filter:native_video | containing “puppy” and an uploaded video, Amplify video, Periscope, or Vine. |
| puppy filter:periscope | containing “puppy” and a Periscope video URL. |
| puppy filter:vine | containing “puppy” and a Vine. |
| puppy filter:images | containing “puppy” and links identified as photos, including third parties such as Instagram. |
| puppy filter:twimg | containing “puppy” and a pic.twitter.com link representing one or more photos. |
| hilarious filter:links | containing “hilarious” and linking to URL. |
| superhero since:2015-12-21 | containing “superhero” and sent since date “2015-12-21” (year-month-day). |
| puppy until:2015-12-21 | containing “puppy” and sent before the date “2015-12-21”. |
| movie -scary :) | containing “movie”, but not “scary”, and with a positive attitude. |
| flight :( | containing “flight” and with a negative attitude. |
| traffic ? | containing “traffic” and asking a question. |

# Parameters

- `count` : number of tweets to be retrieved. Default = 15.
- `type` : result type of query. Options are "Popular" and "Recent". Default = "Mixed"
- `since` : date since which tweets should be retrieved. Format: YYYY-MM-DD
- `until` : date until which tweets should be retrieved. Format: YYYY-MM-DD
- `lang` : language of tweets to be retrieved.
