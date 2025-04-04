import requests

url = "http://127.0.0.1:8000/chat"
data = {"message": "Can you give me five herbal plants along with how to use them in a raw text form of a json object where both names and how to use are stored for specific symptom fever at himachal pradesh, just give me the json thing no need to give introductory line"}

response = requests.post(url, json=data)
print(response.json())  # Output: {'response': 'Apple's earnings in 2022 were...'}
