import datetime
import json
from random import choice
import requests

headers = {'content-type': 'application/json'}

services = [ "cuckoo", "hummingbird", "colony", "bigbird", "zipkin", "chickadee", "koalabird" ]
sources = [ "box1", "box2", "box3", "box4", "box5" ]
etypes = [ "incident", "deployment", "notice" ]
users = [ "cwatson", "yramin", "bdegenhardt", "ak", "kpawlowski" ]

now = datetime.datetime.now()

for h in range(0,17):
  for m in range(0,60):
    ev = {
        'service': choice(services),
        'source': choice(sources),
        'etype': choice(etypes),
        'user': choice(users),
        'content': 'asd',
        'date_begun': '{0}{1:02d}{2:02d}T{3:02d}{4:02d}00Z'.format(now.year, now.month, now.day, h, m)
    }

    r = requests.put('http://localhost:9000/store', data=json.dumps(ev), headers=headers)
    print r.status_code
