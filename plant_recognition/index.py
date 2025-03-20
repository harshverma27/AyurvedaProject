import requests
import json
from pprint import pprint

API_KEY = "2b10r2y9pGKDug2UXEqv9P4I3u"	# Your API_KEY here
PROJECT = "all"; # try specific floras: "weurope", "canada"…
api_endpoint = f"https://my-api.plantnet.org/v2/identify/{PROJECT}?api-key={API_KEY}"

image_path_1 = "tulsi.jpg"
image_data_1 = open(image_path_1, 'rb')


data = { 'organs': ['leaf'] }

files = [
  ('images', (image_path_1, image_data_1))\
]

req = requests.Request('POST', url=api_endpoint, files=files, data=data)
prepared = req.prepare()

s = requests.Session()
response = s.send(prepared)
json_result = json.loads(response.text)

#Final Output
best_match = json_result.get("bestMatch")
common_names = json_result.get("results", [])[0].get("species", {}).get("commonNames", [])