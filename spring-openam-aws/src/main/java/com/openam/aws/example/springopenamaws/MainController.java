package com.openam.aws.example.springopenamaws;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;

import reactor.core.publisher.Mono;


@Controller
public class MainController {

	@Autowired
	private OAuth2AuthorizedClientService authorizedClientService;

	@RequestMapping("/")
	public String index(Model model, OAuth2AuthenticationToken authentication) {
		OAuth2AuthorizedClient authorizedClient = this.getAuthorizedClient(authentication);
		System.out.println("class name : " + authentication.getPrincipal().getClass().getName());
		model.addAttribute("userName", authentication.getName());
		model.addAttribute("clientName", authorizedClient.getClientRegistration().getClientName());
		return "index";
	}

	@RequestMapping("/userinfo")
	public String userinfo(Model model, OAuth2AuthenticationToken authentication) {
		OAuth2AuthorizedClient authorizedClient = this.getAuthorizedClient(authentication);
		Map userAttributes = new HashMap();
		userAttributes.putAll(authentication.getPrincipal().getAttributes());
		DefaultOidcUser oidcUser = (DefaultOidcUser)authentication.getPrincipal();
		userAttributes.put("id_token", oidcUser.getIdToken().getTokenValue());
		model.addAttribute("userAttributes", userAttributes);

		return "userinfo";
	}

	private OAuth2AuthorizedClient getAuthorizedClient(OAuth2AuthenticationToken authentication) {
		return this.authorizedClientService.loadAuthorizedClient(
			authentication.getAuthorizedClientRegistrationId(), authentication.getName());
	}

	private ExchangeFilterFunction oauth2Credentials(OAuth2AuthorizedClient authorizedClient) {
		return ExchangeFilterFunction.ofRequestProcessor(
			clientRequest -> {
				ClientRequest authorizedRequest = ClientRequest.from(clientRequest)
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + authorizedClient.getAccessToken().getTokenValue())
					.build();
				return Mono.just(authorizedRequest);
			});
	}
}