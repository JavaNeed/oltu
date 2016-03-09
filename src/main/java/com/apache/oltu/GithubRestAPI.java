package com.apache.oltu;

import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthBearerClientRequest;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.GitHubTokenResponse;
import org.apache.oltu.oauth2.client.response.OAuthResourceResponse;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GithubRestAPI {
	private static final Logger logger = LoggerFactory.getLogger(GithubRestAPI.class);
			
	private static final String AUTHORIZATION_URL = "https://github.com/login/oauth/authorize";
	private static final String CLIENT_ID = "8515a1e4dbda3b044e86";
	private static final String CLIENT_SECRET = "ae3487601d891d257f7193fc48d2cbe4287ef989";
	private static final String REDIRECT_URL = "http://localhost:8080/apache-oltu/github/redirect";
	private static final String ACCESS_TOKEN_URL = "https://github.com/login/oauth/access_token";
	
	
	public static void main(String[] args) throws OAuthSystemException {
		OAuthClientRequest request = OAuthClientRequest
				.authorizationLocation(AUTHORIZATION_URL)
				.setClientId(CLIENT_ID)
				.setRedirectURI(REDIRECT_URL)
				.setResponseType("code")
				//.setScope("r_basicprofile")
				//.setScope("w_share")
				.setScope("user,gist")
				.buildQueryMessage();

		System.out.println("REDIRECT TO: "+request.getLocationUri());
	}
	
	private String getAccessToken(String authorizationCode) throws OAuthSystemException, OAuthProblemException {
		OAuthClientRequest request = OAuthClientRequest
				.tokenLocation(ACCESS_TOKEN_URL)
				.setGrantType(GrantType.AUTHORIZATION_CODE)
				.setClientId(CLIENT_ID)
				.setClientSecret(CLIENT_SECRET)
				.setRedirectURI(REDIRECT_URL)
				.setCode(authorizationCode)
				.buildBodyMessage();

		//create OAuth client that uses custom http client under the hood
		OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());


		//OAuthJSONAccessTokenResponse oAuthResponse = oAuthClient.accessToken(request, "POST");                       
		GitHubTokenResponse oAuthResponse = oAuthClient.accessToken(request, GitHubTokenResponse.class);
		//String accessToken = oAuthResponse.getAccessToken();

		System.out.println("POSTING: "+request.getBody());	            
		System.out.println("Received ACCESS_TOKEN: [ "+oAuthResponse.getAccessToken() + "]");
		System.out.println("Received EXPIRES_IN: [ "+oAuthResponse.getExpiresIn() + "]");
		System.out.println("REFRESH_TOKEN : [" + oAuthResponse.getRefreshToken() + "]");

		System.out.println(oAuthResponse.getBody());

		request= new OAuthBearerClientRequest("https://api.github.com/user").
				setAccessToken(oAuthResponse.getAccessToken()).
				buildQueryMessage();
		
		
		OAuthClient client = new OAuthClient(new URLConnectionClient());
		OAuthResourceResponse resourceResponse= client.resource(request, "GET", OAuthResourceResponse.class);

		if (resourceResponse.getResponseCode()==200){			
			logger.debug("HTTP OK");
			System.out.println(resourceResponse.getBody());
			return resourceResponse.getBody();
		}
		else{
			System.out.println("Could not access resource: " + resourceResponse.getResponseCode() 
					+ " " + resourceResponse.getBody());
			return null;
		}
	} 
}
