import json
import requests

headers = {'content-type': 'application/json'}

for h in range(0,24):
  for m in range(0,60):
    ev = {
        'service': 'cuckoo',
        'source': 'box1',
        'etype': 'incident',
        'content': 'asd',
        'date_begun': '20130531T{0}{1}00Z'.format(h, m)
    }

    r = requests.put('http://localhost:9000/store', data=json.dumps(ev), headers=headers)
    print r.status_code
