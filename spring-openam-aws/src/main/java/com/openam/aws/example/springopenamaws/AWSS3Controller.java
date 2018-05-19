package com.openam.aws.example.springopenamaws;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.amazonaws.auth.AWSSessionCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.cognitoidentity.AmazonCognitoIdentity;
import com.amazonaws.services.cognitoidentity.AmazonCognitoIdentityClientBuilder;
import com.amazonaws.services.cognitoidentity.model.Credentials;
import com.amazonaws.services.cognitoidentity.model.GetCredentialsForIdentityRequest;
import com.amazonaws.services.cognitoidentity.model.GetCredentialsForIdentityResult;
import com.amazonaws.services.cognitoidentity.model.GetIdRequest;
import com.amazonaws.services.cognitoidentity.model.GetIdResult;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;

@Controller
public class AWSS3Controller {
	
	@Value("${cognitoIdentityId}")
	private String cognitoIdentityId;
	
	@Value("${cognitoProvider}")
	private String cognitoProvider;
	
	@Value("${cognitoRegion}")
	private String cognitoRegion;

	@RequestMapping("/lists3")
	public String lists3buckets(Model model, OAuth2AuthenticationToken authentication) {
		System.out.println("cognitoIdentityId : "+ cognitoIdentityId);
		System.out.println("cognitoProvider : "+ cognitoProvider);
		System.out.println("cognitoRegion : "+ cognitoRegion);
		DefaultOidcUser oidcUser = (DefaultOidcUser)authentication.getPrincipal();
		String openIdToken = oidcUser.getIdToken().getTokenValue();
		Map<String,String> logins = new HashMap<>();
		logins.put(cognitoProvider, openIdToken);
		GetIdRequest idRequest = new GetIdRequest().withIdentityPoolId(cognitoIdentityId)
				.withLogins(logins);
		AmazonCognitoIdentityClientBuilder.standard().setRegion(cognitoRegion);
		AmazonCognitoIdentity cognitoIdentityClient = AmazonCognitoIdentityClientBuilder.defaultClient();
		GetIdResult idResult = cognitoIdentityClient.getId(idRequest);
		System.out.println("identity ID : " + idResult.getIdentityId());
		GetCredentialsForIdentityRequest getCredentialsRequest =
		    new GetCredentialsForIdentityRequest()
		    .withIdentityId(idResult.getIdentityId())
		    .withLogins(logins);
		GetCredentialsForIdentityResult getCredentialsResult = cognitoIdentityClient.getCredentialsForIdentity(getCredentialsRequest);
		Credentials credentials = getCredentialsResult.getCredentials();
		System.out.println(credentials.getAccessKeyId());
		AWSSessionCredentials sessionCredentials = new BasicSessionCredentials(
		    credentials.getAccessKeyId(),
		    credentials.getSecretKey(),
		    credentials.getSessionToken()
		);
		AmazonS3 s3 = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(sessionCredentials))
                .build();
		List<Bucket> buckets = s3.listBuckets();
		System.out.println("Your Amazon S3 buckets are:");
		Map s3bucket = new HashMap();
		List objectsList = null;
		for (Bucket b : buckets) {
	        objectsList = new ArrayList();
		    ListObjectsV2Result result = s3.listObjectsV2(b.getName());
	        List<S3ObjectSummary> objects = result.getObjectSummaries();
	        for (S3ObjectSummary os: objects) {
	            System.out.println("* " + os.getKey());
	            objectsList.add(os.getKey());
	        }
		    s3bucket.put(b.getName(), objectsList);
		}
		model.addAttribute("s3buckets", s3bucket);
		return "s3bucketlist";
	}
	
	@GetMapping("/s3bucket")
    public String displayS3AdminPage(Model model) {
		model.addAttribute("s3bucket", new S3Bucket());
        return "creates3bucket";
    }
	
	@PostMapping("/s3bucket")
    public String createS3Bucket(@ModelAttribute S3Bucket bucket, OAuth2AuthenticationToken authentication) {
		System.out.println(bucket.getBucketName());
		DefaultOidcUser oidcUser = (DefaultOidcUser)authentication.getPrincipal();
		String openIdToken = oidcUser.getIdToken().getTokenValue();
		Map<String,String> logins = new HashMap<>();
		logins.put(cognitoProvider, openIdToken);
		GetIdRequest idRequest = new GetIdRequest().withIdentityPoolId(cognitoIdentityId)
				.withLogins(logins);
		AmazonCognitoIdentityClientBuilder.standard().setRegion(cognitoRegion);
		AmazonCognitoIdentity cognitoIdentityClient = AmazonCognitoIdentityClientBuilder.defaultClient();
		GetIdResult idResult = cognitoIdentityClient.getId(idRequest);
		GetCredentialsForIdentityRequest getCredentialsRequest =
		    new GetCredentialsForIdentityRequest()
		    .withIdentityId(idResult.getIdentityId())
		    .withLogins(logins);
		GetCredentialsForIdentityResult getCredentialsResult = cognitoIdentityClient.getCredentialsForIdentity(getCredentialsRequest);
		Credentials credentials = getCredentialsResult.getCredentials();
		System.out.println(credentials.getAccessKeyId());
		AWSSessionCredentials sessionCredentials = new BasicSessionCredentials(
		    credentials.getAccessKeyId(),
		    credentials.getSecretKey(),
		    credentials.getSessionToken()
		);
		AmazonS3 s3 = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(sessionCredentials))
                .build();
		s3.createBucket(bucket.getBucketName());
        return "index";
    }

}
