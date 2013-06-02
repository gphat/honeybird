# What?

Honeybird is a general purpose event log for recording things that happen.

# Why?

While it aims to have a useful and friendly user-interface it's primary purpose
will be to store events sent to it's REST API and serve them back out to
interested parties.

# How

It stores everything in [ElasticSearch](http://www.elasticsearch.org) because I'm a fan boy.

# Examples

You can store an event by PUTing some JSON:

  curl --header "Content-type: application/json" --request PUT --data '{"service": "cuckoo", "source": "box1", "etype": "deployment", "user": "cwatson", "content": "abc", "date_begun": "20130529T204100Z"}' http://localhost:9000/store

You can fetch the events by GETting: http://127.0.0.1:9000/search

You can filter events with params: service, source, etype, user, date_begun and date_ended. The last two are inclusive.

You can use the UI at http://127.0.0.1:9000/ to do the same.
