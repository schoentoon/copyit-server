#!/usr/bin/python2
import oauth.oauth as oauth
import httplib
import urllib

consumer = oauth.OAuthConsumer(key='401a131e03357df2a563fba48f98749448ed63d37e007f7353608cf81fa70a2d'
                              ,secret='ba3f5945ba6cfb18ca4869cef2c3daf9d4230e37629f3087b281be6ec8fda2bd')
token = oauth.OAuthToken('9476f5130a07a7c0061de48bc19123f51636af704c5df369701960e0bc151255'
                        ,'b96fc9e22532b6bdc2fb760465ea19fa373c520703877ef7e3f0b6a728cefcb1')
RESOURCE_URL = 'http://127.0.0.1:8080/1/clipboard/get'
parameters = { 'data' : 'Hai there :D Can I be on your clipboard please? :3' }
oauth_request = oauth.OAuthRequest.from_consumer_and_token(consumer, token=token
                                                          ,http_method='GET', http_url=RESOURCE_URL)
oauth_request.sign_request(oauth.OAuthSignatureMethod_HMAC_SHA1(), consumer, token)
conn = httplib.HTTPConnection("%s:%d" % ('127.0.0.1', 8080))
headers = {'Content-Type' : 'application/x-www-form-urlencoded' }
headers.update(oauth_request.to_header())
conn.request('GET', RESOURCE_URL, headers=headers)
response = conn.getresponse()
print response.read()
