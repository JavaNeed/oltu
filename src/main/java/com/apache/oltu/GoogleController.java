package com.apache.oltu;

import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthBearerClientRequest;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthJSONAccessTokenResponse;
import org.apache.oltu.oauth2.client.response.OAuthResourceResponse;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/google")
public class GoogleController {
	private static final Logger LOGGER = LoggerFactory.getLogger(GoogleController.class);
	
	@Value("${google.client.id}")
	private String clientId;
	
	@Value("${google.client.secret}")
	private String secret;
	
	@Value("${google.authorization.url}")
	private String authUrl;
	
	@Value("${google.redirect.url}")
	private String redirectUrl;
	
	@Value("${google.token.location}")
	private String tokenLocation;
	
	@Value("${google.client.request.url}")
	private String requestUrl;
	
	@Value("${google.scope.url1}")
	private String scopeUrl1;
	
	@Value("${google.scope.url2}")
	private String scopeUrl2;
	
	@Value("${google.scope.url3}")
	private String scopeUrl3;
	
	@Value("${google.scope.url4}")
	private String scopeUrl4;
	

	@RequestMapping(value = "/auth", method = RequestMethod.GET)
	public String authenticate() throws OAuthSystemException {
		OAuthClientRequest request = OAuthClientRequest
				.authorizationLocation(authUrl)
				.setClientId(clientId)
				.setRedirectURI(redirectUrl)
				.setResponseType("code")
				.setScope(scopeUrl1 +" "+ scopeUrl2	+" "+ scopeUrl3+" "+ scopeUrl4)
				//.setScope("openId profile email")
				.buildQueryMessage();

		// https://www.google.com/m8/feeds, 
		// https://www.googleapis.com/auth/userinfo.email, 
		// https://www.googleapis.com/auth/userinfo.profile

		LOGGER.debug("REDIRECT TO: "+request.getLocationUri());
		return "redirect:" + request.getLocationUri();
	}


	@RequestMapping(value = "/redirect", method = RequestMethod.GET)
	public HttpEntity<String> redirect(
			@RequestParam(value = "code", required = false) String code) 
					throws OAuthSystemException, OAuthProblemException {
		String value = "UNKNOWN";

		if (code != null && code.length() > 0) {
			LOGGER.debug("Received CODE: "+code);
			String details = getAccessToken(code);
			value = details;
		}

		return new ResponseEntity<String>(value,HttpStatus.OK);
	}

	private String getAccessToken(String authorizationCode) throws OAuthSystemException, OAuthProblemException {
		OAuthClientRequest request = OAuthClientRequest
				.tokenLocation(tokenLocation)
				//.tokenProvider(OAuthProviderType.GOOGLE)
				.setGrantType(GrantType.AUTHORIZATION_CODE)
				.setClientId(clientId)
				.setClientSecret(secret)
				.setRedirectURI(redirectUrl)
				.setCode(authorizationCode)
				.buildBodyMessage();

		//create OAuth client that uses custom http client under the hood
		OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());


		OAuthJSONAccessTokenResponse oAuthResponse = oAuthClient.accessToken(request, "POST");                       
		//GitHubTokenResponse oAuthResponse = oAuthClient.accessToken(request, GitHubTokenResponse.class);
		//String accessToken = oAuthResponse.getAccessToken();

		LOGGER.debug("POSTING: "+request.getBody());	            
		LOGGER.debug("Received ACCESS_TOKEN: [ "+oAuthResponse.getAccessToken() + "]");
		LOGGER.debug("Received EXPIRES_IN: [ "+oAuthResponse.getExpiresIn() + "]");
		LOGGER.debug("REFRESH_TOKEN : [" + oAuthResponse.getRefreshToken() + "]");

		LOGGER.debug(oAuthResponse.getBody());

		//https://www.googleapis.com/plus/v1/people/me  ===> Not working
		//https://www.googleapis.com/auth/userinfo.profile ==>Not working - 
		// https://www.googleapis.com/oauth2/v2/userinfo ==> Working
		// https://www.googleapis.com/plus/v1/people/me  ===> Not working 
		// TODO: https://developers.google.com/accounts/docs/OAuth2ForDevices
		request= new OAuthBearerClientRequest(requestUrl).
				setAccessToken(oAuthResponse.getAccessToken()).
				buildQueryMessage();
		OAuthClient client = new OAuthClient(new URLConnectionClient());
		OAuthResourceResponse resourceResponse= client.resource(request, "GET", OAuthResourceResponse.class);

		if (resourceResponse.getResponseCode()==200){			
			LOGGER.debug("HTTP OK");
			LOGGER.debug(resourceResponse.getBody());
			return resourceResponse.getBody();
		}
		else{
			LOGGER.debug("Could not access resource: " + resourceResponse.getResponseCode() 
					+ " " + resourceResponse.getBody());
			return null;
		}
	}   
}
