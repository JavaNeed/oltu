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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/profile")
public class FacebookProfileController {
	private static final Logger logger = LoggerFactory.getLogger(FacebookProfileController.class);

	@RequestMapping(value = "/auth", method = RequestMethod.GET)
	public String authenticate() throws OAuthSystemException {

		OAuthClientRequest request = OAuthClientRequest
				.authorizationLocation("https://www.facebook.com/dialog/oauth")
				.setClientId("233668646673605")
				//.setClientId("970222193006255")
				.setRedirectURI("http://localhost:8080/apache-oltu/profile/redirect-profile")
				.setResponseType("code")
				.setScope("email,offline_access,user_about_me,user_birthday,read_friendlists")
				.buildQueryMessage();

		System.out.println("REDIRECT TO: "+request.getLocationUri());
		return "redirect:" + request.getLocationUri();
	}

	@RequestMapping(value = "/redirect-profile", method = RequestMethod.GET)
	public HttpEntity<String> redirect(@RequestParam(value = "code", required = false) String code) 
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
				.tokenLocation("https://graph.facebook.com/oauth/access_token")
				.setGrantType(GrantType.AUTHORIZATION_CODE)
				.setClientId("233668646673605")
				//.setClientId("970222193006255")
				.setClientSecret("33b17e044ee6a4fa383f46ec6e28ea1d")
				/*.setClientSecret("27aae52935369b43710e6e8bacc7f2d7")*/
				.setRedirectURI("http://localhost:8080/apache-oltu/profile/redirect-profile")
				.setCode(authorizationCode)
				.buildBodyMessage();

		//create OAuth client that uses custom http client under the hood
		OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());


		//OAuthJSONAccessTokenResponse oAuthResponse = oAuthClient.accessToken(request, "POST");                       
		GitHubTokenResponse oAuthResponse = oAuthClient.accessToken(request, GitHubTokenResponse.class);
		//String accessToken = oAuthResponse.getAccessToken();

		System.out.println("access_token : ["+oAuthResponse.getAccessToken()+" ]");

		request= new OAuthBearerClientRequest("https://graph.facebook.com/me/friends").
				setAccessToken(oAuthResponse.getAccessToken()).
				buildQueryMessage();

		OAuthClient client = new OAuthClient(new URLConnectionClient());
		OAuthResourceResponse resourceResponse= client.resource(request, "GET", OAuthResourceResponse.class);

		if (resourceResponse.getResponseCode()==200){			
			System.out.println("HTTP OK");
			System.out.println(resourceResponse.getBody());
			return resourceResponse.getBody();
		}
		else{
			System.out.println("Could not access resource: " + resourceResponse.getResponseCode() + " " + resourceResponse.getBody());
			return String.valueOf(resourceResponse.getResponseCode());
		}
	}
}

