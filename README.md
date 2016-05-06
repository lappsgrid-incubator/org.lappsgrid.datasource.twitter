## org.lappsgrid.datasource.twitter
Twitter Datasource

# Usage

Takes a Data<String> object with a Uri.GET ("http://vocab.lappsgrid.org/ns/action/get") discriminator and a string payload representing the search string for the query. Optional parameters are listed below.

# Parameters

- `count` : number of tweets to be retrieved. Default = 15.
- `type` : result type of query. Options are "Popular" and "Recent". Default = "Mixed"
- `since` : date since which tweets should be retrieved. Format: YYYY-MM-DD
- `until` : date until which tweets should be retrieved. Format: YYYY-MM-DD
- `lang` : language of tweets to be retrieved.
