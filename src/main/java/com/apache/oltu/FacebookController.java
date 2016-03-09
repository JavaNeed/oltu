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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
@RequestMapping("/facebook")
public class FacebookController {
	private static final Logger LOGGER = LoggerFactory.getLogger(FacebookController.class);

	@Value("${facebook.auth.location}")
	private String authLocation;

	@Value("${facebook.client.id}")
	private String clientId;

	@Value("${facebook.redirect.url}")
	private String redirectUrl;

	@Value("${facebook.scope}")
	private String scope;

	@Value("${facebook.token.location}")
	private String tokenLocation;

	@Value("${facebook.client.secret}")
	private String secret;

	@Value("${facebook.client.request}")
	private String clientRequest;

	@RequestMapping(value = "/auth", method = RequestMethod.GET)
	public String authenticate() throws OAuthSystemException {
		OAuthClientRequest request = OAuthClientRequest
				.authorizationLocation(authLocation)
				.setClientId(clientId)
				//.setClientId("970222193006255")
				.setRedirectURI(redirectUrl)
				.setResponseType("code")
				.setScope(scope)
				.buildQueryMessage();

		LOGGER.debug("REDIRECT TO: "+request.getLocationUri());
		return "redirect:" + request.getLocationUri();
	}

	@RequestMapping(value = "/redirect", method = RequestMethod.GET)
	public HttpEntity<String> redirect(
			@RequestParam(value = "code", required = false) String code) 
					throws OAuthSystemException, OAuthProblemException {
		String value = "UNKNOWN";

		if (code != null && code.length() > 0) {
			System.out.println("Received CODE: "+code);
			value = getAccessToken(code);
		}

		return new ResponseEntity<String>(value, HttpStatus.OK);
	}

	private String getAccessToken(String authorizationCode) throws OAuthSystemException, OAuthProblemException {
		String value = null;
		OAuthClientRequest request = OAuthClientRequest
				.tokenLocation(tokenLocation)
				.setGrantType(GrantType.AUTHORIZATION_CODE)
				.setClientId(clientId)
				//.setClientId("970222193006255")
				.setClientSecret(secret)
				/*.setClientSecret("27aae52935369b43710e6e8bacc7f2d7")*/
				.setRedirectURI(redirectUrl)
				.setCode(authorizationCode)
				.buildBodyMessage();

		//create OAuth client that uses custom http client under the hood
		OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());

		try {
			//OAuthJSONAccessTokenResponse oAuthResponse = oAuthClient.accessToken(request, "POST");                       
			GitHubTokenResponse oAuthResponse = oAuthClient.accessToken(request, GitHubTokenResponse.class);

			LOGGER.debug("POSTING: "+request.getBody());	            
			LOGGER.debug("ACCESS_TOKEN: "+oAuthResponse.getAccessToken());

			request= new OAuthBearerClientRequest(clientRequest).setAccessToken(oAuthResponse.getAccessToken()).buildQueryMessage();
			OAuthClient client = new OAuthClient(new URLConnectionClient());
			OAuthResourceResponse resourceResponse= client.resource(request, "GET", OAuthResourceResponse.class);

			if (resourceResponse.getResponseCode()==200){			
				LOGGER.debug("HTTP OK");
				System.out.println(resourceResponse.getBody());
				value = resourceResponse.getBody();
			}
			else{
				System.out.println("Could not access resource: " + resourceResponse.getResponseCode() + " " 
						+ resourceResponse.getBody());
				value= String.valueOf(resourceResponse.getResponseCode());
			}
		}
		catch (OAuthProblemException prob) {
			System.out.println("EXCEPTION: "+prob.getMessage());
			prob.printStackTrace();
		}
		
		return value;
	}
}