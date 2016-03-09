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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
@RequestMapping("/linkedin")
public class LinkedinController {
	private static final Logger logger = LoggerFactory.getLogger(LinkedinController.class);
	
	@RequestMapping(value = "/auth", method = RequestMethod.GET)
	public String authenticate() throws OAuthSystemException {
		OAuthClientRequest request = OAuthClientRequest
				.authorizationLocation("https://www.linkedin.com/uas/oauth2/authorization")
				.setClientId("75ct78em12e2y1")
				.setRedirectURI("http://localhost:8080/apache-oltu/linkedin/redirect")
				.setResponseType("code")
				//.setScope("r_basicprofile")
				//.setScope("w_share")
				.setScope("r_network w_share r_basicprofile")
				.setState("987654321")
				.buildQueryMessage();

		System.out.println("REDIRECT TO: "+request.getLocationUri());
		return "redirect:" + request.getLocationUri();
	}


	@RequestMapping(value = "/redirect", method = RequestMethod.GET)
	public HttpEntity<String> redirect(
			@RequestParam(value = "code", required = false) String code) 
					throws OAuthSystemException, OAuthProblemException {
		String value = "UNKNOWN";

		if (code != null && code.length() > 0) {
			System.out.println("Received CODE: "+code);
			String details = getAccessToken(code);
			value = details;
		}

		return new ResponseEntity<String>(value,HttpStatus.OK);
	}

	private String getAccessToken(String authorizationCode) throws OAuthSystemException, OAuthProblemException {
		OAuthClientRequest request = OAuthClientRequest
				.tokenLocation("https://www.linkedin.com/uas/oauth2/accessToken")
				.setGrantType(GrantType.AUTHORIZATION_CODE)
				.setClientId("75ct78em12e2y1")
				.setClientSecret("RG76nzhgv0sz7wKx")
				.setRedirectURI("http://localhost:8080/apache-oltu/linkedin/redirect")
				.setCode(authorizationCode)
				.buildBodyMessage();

		//create OAuth client that uses custom http client under the hood
		OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());


		OAuthJSONAccessTokenResponse oAuthResponse = oAuthClient.accessToken(request, "POST");                       
		//GitHubTokenResponse oAuthResponse = oAuthClient.accessToken(request, GitHubTokenResponse.class);
		//String accessToken = oAuthResponse.getAccessToken();

		System.out.println("POSTING: "+request.getBody());	            
		System.out.println("Received ACCESS_TOKEN: [ "+oAuthResponse.getAccessToken() + "]");
		System.out.println("Received EXPIRES_IN: [ "+oAuthResponse.getExpiresIn() + "]");
		System.out.println("REFRESH_TOKEN : [" + oAuthResponse.getRefreshToken() + "]");

		System.out.println(oAuthResponse.getBody());

		// will cause - Scheme error
		//https://api.linkedin.com/v1/people/

		// Working
		// (1) http://api.linkedin.com/v1/people/~/shares
		// (2) https://api.linkedin.com/v1/people/~?oauth2_access_token=
		// (3) https://api.linkedin.com/v1/people/~/shares?oauth2_access_token=
		// (4) https://api.linkedin.com/v1/people/~?format=json&oauth2_access_token=
		// (5) https://api.linkedin.com/v1/people/~/network/updates?oauth2_access_token=
		request= new OAuthBearerClientRequest
				("https://api.linkedin.com/v1/people/~?oauth2_access_token="+oAuthResponse.getAccessToken()).
				buildQueryMessage();
		
		
		OAuthClient client = new OAuthClient(new URLConnectionClient());
		OAuthResourceResponse resourceResponse= client.resource(request, "GET", OAuthResourceResponse.class);

		if (resourceResponse.getResponseCode()==200){			
			System.out.println("HTTP OK");
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
