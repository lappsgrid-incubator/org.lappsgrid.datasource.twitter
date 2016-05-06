# org.lappsgrid.datasource.twitter
Twitter Datasource

## Parameters

- `count` : number of tweets to be retrieved. Default = 15.
- `type` : result type of query. Options are "Popular" and "Recent". Default = "Mixed"
- `since` : date since which tweets should be retrieved, in format: YYYY-MM-DD
- `until` : date until which tweets should be retrieved, in format: YYYY-MM-DD
- `lang` : language of tweets to be retrieved.
- `address` : address around which to receive tweets. This parameter has to be accompanied by a radius.
- `radius` : value of radius around address. Default = 10
- `unit` : unit of radius around address. Options are "km" (kilometers) or "mi" (miles). Default = "mi"