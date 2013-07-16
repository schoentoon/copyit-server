#!/usr/bin/python2
import oauth2
import urllib
import urllib2
import time

consumer = oauth2.Consumer(key='401a131e03357df2a563fba48f98749448ed63d37e007f7353608cf81fa70a2d'
                          ,secret='4d929bf50ce1069b1aef450d75de166e47d5b6832b04f133f9547a9beea27c5b')
token = oauth2.Token('oauth_token', 'oauth_token_secret')
postbody = urllib.urlencode({'data': 'This is some ASCII data.'})
params = { 'oauth_version': "1.0"
         , 'oauth_nonce': oauth2.generate_nonce()
         , 'oauth_timestamp': str(int(time.time())) }
request = oauth2.Request('POST', 'http://127.0.0.1:8080/1/clipboard/update', params, postbody)
request.sign_request(oauth2.SignatureMethod_HMAC_SHA1(), consumer, token)
headers = request.to_header()
req = urllib2.Request("http://127.0.0.1:8080/1/clipboard/update", None, headers)
response = urllib2.urlopen(req)
print response.read()
#response, content = client.request('http://127.0.0.1:8080/1/clipboard/update', 'GET', postbody, headers, force_auth_header=True)