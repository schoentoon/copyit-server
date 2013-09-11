#!/usr/bin/python2
import httplib
import time
import oauth.oauth as oauth

SERVER = '127.0.0.1'
PORT = 8080

REQUEST_TOKEN_URL = 'http://127.0.0.1:8080/oauth/request_token'
ACCESS_TOKEN_URL = 'http://127.0.0.1:8080/oauth/access_token'
AUTHORIZATION_URL = 'http://127.0.0.1:8080/oauth/authorize'
CALLBACK_URL = 'http://127.0.0.1:1337/request_token_ready'

CONSUMER_KEY = '401a131e03357df2a563fba48f98749448ed63d37e007f7353608cf81fa70a2d'
CONSUMER_SECRET = 'ba3f5945ba6cfb18ca4869cef2c3daf9d4230e37629f3087b281be6ec8fda2bd'

class SimpleOAuthClient(oauth.OAuthClient):

    def __init__(self, server, port=httplib.HTTP_PORT, request_token_url='', access_token_url='', authorization_url=''):
        self.server = server
        self.port = port
        self.request_token_url = request_token_url
        self.access_token_url = access_token_url
        self.authorization_url = authorization_url
        self.connection = httplib.HTTPConnection("%s:%d" % (self.server, self.port))

    def fetch_request_token(self, oauth_request):
        # via headers
        # -> OAuthToken
        self.connection.request(oauth_request.http_method, self.request_token_url, body="", headers=oauth_request.to_header())
        response = self.connection.getresponse()
        raw = response.read()
        print raw
        return oauth.OAuthToken.from_string(raw)

    def fetch_access_token(self, oauth_request):
        # via headers
        # -> OAuthToken
        self.connection.request(oauth_request.http_method, self.access_token_url, headers=oauth_request.to_header())
        response = self.connection.getresponse()
        return oauth.OAuthToken.from_string(response.read())

    def authorize_token(self, oauth_request):
        # via url
        # -> typically just some okay response
        self.connection.request(oauth_request.http_method, oauth_request.to_url())
        response = self.connection.getresponse()
        return response.read()

    def access_resource(self, oauth_request):
        # via post body
        # -> some protected resources
        headers = {'Content-Type' :'application/x-www-form-urlencoded'}
        self.connection.request('POST', RESOURCE_URL, body=oauth_request.to_postdata(), headers=headers)
        response = self.connection.getresponse()
        return response.read()

def run_example():
    print '** OAuth Python Library Example **'
    client = SimpleOAuthClient(SERVER, PORT, REQUEST_TOKEN_URL, ACCESS_TOKEN_URL, AUTHORIZATION_URL)
    consumer = oauth.OAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET)
    signature_method_hmac_sha1 = oauth.OAuthSignatureMethod_HMAC_SHA1()
    pause()

    # get request token
    print '* Obtain a request token ...'
    pause()
    oauth_request = oauth.OAuthRequest.from_consumer_and_token(consumer, callback=CALLBACK_URL, http_url=client.request_token_url, http_method='POST')
    oauth_request.sign_request(signature_method_hmac_sha1, consumer, None)
    print 'REQUEST (via headers)'
    print 'parameters: %s' % str(oauth_request.parameters)
    pause()
    token = client.fetch_request_token(oauth_request)
    print 'GOT'
    print 'key: %s' % str(token.key)
    print 'secret: %s' % str(token.secret)
    print 'callback confirmed? %s' % str(token.callback_confirmed)
    pause()

    print '* Authorize the request token ...'
    pause()
    oauth_request = oauth.OAuthRequest.from_token_and_callback(token=token, http_url=client.authorization_url)
    print 'REQUEST (via url query string)'
    print 'parameters: %s' % str(oauth_request.parameters)
    pause()
    # this will actually occur only on some callback
    response = client.authorize_token(oauth_request)
    print 'GOT'
    print response
    # sad way to get the verifier
    import urlparse, cgi
    query = urlparse.urlparse(response)[4]
    params = cgi.parse_qs(query, keep_blank_values=False)
    verifier = params['oauth_verifier'][0]
    print 'verifier: %s' % verifier
    pause()

    # get access token
    print '* Obtain an access token ...'
    pause()
    oauth_request = oauth.OAuthRequest.from_consumer_and_token(consumer, token=token, verifier=verifier, http_url=client.access_token_url)
    oauth_request.sign_request(signature_method_plaintext, consumer, token)
    print 'REQUEST (via headers)'
    print 'parameters: %s' % str(oauth_request.parameters)
    pause()
    token = client.fetch_access_token(oauth_request)
    print 'GOT'
    print 'key: %s' % str(token.key)
    print 'secret: %s' % str(token.secret)
    pause()

    # access some protected resources
    print '* Access protected resources ...'
    pause()
    parameters = {'file': 'vacation.jpg', 'size': 'original'} # resource specific params
    oauth_request = oauth.OAuthRequest.from_consumer_and_token(consumer, token=token, http_method='POST', http_url=RESOURCE_URL, parameters=parameters)
    oauth_request.sign_request(signature_method_hmac_sha1, consumer, token)
    print 'REQUEST (via post body)'
    print 'parameters: %s' % str(oauth_request.parameters)
    pause()
    params = client.access_resource(oauth_request)
    print 'GOT'
    print 'non-oauth parameters: %s' % params
    pause()

def pause():
    print ''
    time.sleep(1)

if __name__ == '__main__':
    run_example()
    print 'Done.'