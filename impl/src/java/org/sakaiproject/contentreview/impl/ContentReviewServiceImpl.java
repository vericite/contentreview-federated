package org.sakaiproject.contentreview.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import net.sf.json.JSONObject;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.content.api.ContentHostingService;
import org.sakaiproject.content.api.ContentResource;
import org.sakaiproject.contentreview.exception.QueueException;
import org.sakaiproject.contentreview.exception.ReportException;
import org.sakaiproject.contentreview.exception.SubmissionException;
import org.sakaiproject.contentreview.exception.TransientSubmissionException;
import org.sakaiproject.contentreview.model.ContentReviewItem;
import org.sakaiproject.contentreview.service.ContentReviewService;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;

public class ContentReviewServiceImpl implements ContentReviewService {

	private ServerConfigurationService serverConfigurationService;
	private ContentHostingService contentHostingService;
	private UserDirectoryService userDirectoryService;
	
	private static final String PARAM_CONSUMER = "consumer";
	private static final String PARAM_CONSUMER_SECRET = "consumerSecret";
	private static final String PARAM_TOKEN = "token";
	private static final String PARAM_USER_FIRST_NAME = "userFirstName";
	private static final String PARAM_USER_LAST_NAME = "userLastName";
	private static final String PARAM_USER_EMAIL = "userEmail";
	private static final String PARAM_USER_ROLE = "userRole";
	private static final String PARAM_USER_ROLE_INSTRUCTOR = "Instructor";
	private static final String PARAM_USER_ROLE_LEARNER = "Learner";
	private static final String PARAM_CONTEXT_TITLE = "contextTitle";
	private static final String PARAM_VIEW_REPORT = "viewReport";
	private static final String PARAM_TOKEN_REQUEST = "tokenRequest";
	private static final String PARAM_FILE_DATA = "filedata";
	private static final String PARAM_EXTERNAL_CONTENT_ID = "externalContentId";
	
	private static final String ASN1_GRADE_PERM = "asn.grade";

	
	private String serviceUrl;
	private String consumer;
	private String consumerSecret;
	
	//Caches token requests for instructors so that we don't have to send a request for every student
	// ContextId -> Object{token, date}
	private Map<String, Object[]> instructorSiteTokenCache = new HashMap<String, Object[]>();
	private static final int CACHE_EXPIRE_MINS = 20;
	
	public void init(){
		serviceUrl = serverConfigurationService.getString("longsightPlagiarism.serviceUrl", "");
		consumer = serverConfigurationService.getString("longsightPlagiarism.consumer", "");
		consumerSecret = serverConfigurationService.getString("longsightPlagiarism.consumerSecret", "");
	}
	
	public boolean allowResubmission() {
		return true;
	}

	public void checkForReports() {
		
	}

	public void createAssignment(String arg0, String arg1, Map arg2)
			throws SubmissionException, TransientSubmissionException {
		
	}

	public List<ContentReviewItem> getAllContentReviewItems(String arg0,
			String arg1) throws QueueException, SubmissionException,
			ReportException {
		// TODO Auto-generated method stub
		return null;
	}

	public Map getAssignment(String arg0, String arg1)
			throws SubmissionException, TransientSubmissionException {
		// TODO Auto-generated method stub
		return null;
	}

	public Date getDateQueued(String arg0) throws QueueException {
		// TODO Auto-generated method stub
		return null;
	}

	public Date getDateSubmitted(String arg0) throws QueueException,
			SubmissionException {
		// TODO Auto-generated method stub
		return null;
	}

	public String getIconUrlforScore(Long score) {
		String urlBase = "/sakai-contentreview-tool/images/score_";
		String suffix = ".gif";

		if (score.equals(Long.valueOf(0))) {
			return urlBase + "blue" + suffix;
		} else if (score.compareTo(Long.valueOf(25)) < 0 ) {
			return urlBase + "green" + suffix;
		} else if (score.compareTo(Long.valueOf(50)) < 0  ) {
			return urlBase + "yellow" + suffix;
		} else if (score.compareTo(Long.valueOf(75)) < 0 ) {
			return urlBase + "orange" + suffix;
		} else {
			return urlBase + "red" + suffix;
		}
	}

	public String getLocalizedStatusMessage(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public String getLocalizedStatusMessage(String arg0, String arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	public String getLocalizedStatusMessage(String arg0, Locale arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<ContentReviewItem> getReportList(String siteId)
			throws QueueException, SubmissionException, ReportException {
		// TODO Auto-generated method stub
		return null;
	}

	public List<ContentReviewItem> getReportList(String siteId, String taskId)
			throws QueueException, SubmissionException, ReportException {
		return null;
	}

	public String getReviewReport(String contentId) throws QueueException,
			ReportException {
		return getAccessUrl(contentId, false);
	}

	public String getReviewReportInstructor(String contentId) throws QueueException,
			ReportException {
		/**
		 * contentId: /attachment/04bad844-493c-45a1-95b4-af70129d54d1/Assignments/b9872422-fb24-4f85-abf5-2fe0e069b251/plag.docx
		 */
		return getAccessUrl(contentId, true);
	}

	public String getReviewReportStudent(String contentId) throws QueueException,
			ReportException {
		return getAccessUrl(contentId, false);
	}
	
	private String getAccessUrl(String contentId, boolean instructor){
		String[] contentSplit = contentId.split("/");
		if(contentSplit.length > 2){
			String context = contentSplit[2];
			Map<String, String> params = new HashMap<String, String>();
			params.put(PARAM_CONSUMER, consumer);
			String token = null;
			if(instructor){
				//see if token already exist and isn't expired (we'll expire it after 1 minute so the user has enough time to use the token)
				if(instructorSiteTokenCache.containsKey(context)){
					Object[] cacheItem = instructorSiteTokenCache.get(context);
					Calendar cal = Calendar.getInstance();
				    cal.setTime(new Date());
				    //subtract the exipre time (currently set to 20 while the plag token is set to 30, leaving 10 mins in worse case for instructor to use token)
				    cal.add(Calendar.MINUTE, CACHE_EXPIRE_MINS * -1);
				    if(((Date) cacheItem[1]).after(cal.getTime())){
				    	//token hasn't expired, use it
				    	token = (String) cacheItem[0];
				    }else{
				    	//token is expired, remove it
				    	instructorSiteTokenCache.remove(context);
				    }
				}
			}
			String url = generateUrl(context, null, null);
			if(token == null){
				//token wasn't cached, let's look it up
				params.put(PARAM_CONSUMER_SECRET, consumerSecret);
				if(!instructor){
					//if the user is an instructor then we don't need to worry about specific content ids 
					//when checking access
					params.put(PARAM_EXTERNAL_CONTENT_ID, contentId);
				}
				params.put(PARAM_TOKEN_REQUEST, "true");
				JSONObject results = getResults(url, params);
				if(results != null){
					token = results.getString(PARAM_TOKEN);
				}
			}
			if(token != null){
				//if token doesn't already exist in the cache, store it and set the date
				if(!instructorSiteTokenCache.containsKey(context)){
					instructorSiteTokenCache.put(context, new Object[]{token, new Date()});
				}
				//we have a request token instead of the secret so that a user can see it
				params.remove(PARAM_CONSUMER_SECRET);
				params.put(PARAM_TOKEN, token);
				//get rid of the request for the token
				params.remove(PARAM_TOKEN_REQUEST);
				//now tell the service we want to view the report
				params.put(PARAM_VIEW_REPORT, "true");
				//since we could have stripped out this parameter for the instructor, put it back 
				//in so we know what content the user wants to view
				params.put(PARAM_EXTERNAL_CONTENT_ID, contentId);
				String urlParameters = "";
				if(params != null){
					for(Entry<String, String> entry : params.entrySet()){
						if(!"".equals(urlParameters)){
							urlParameters += "&";
						}
						urlParameters += entry.getKey() + "=" + entry.getValue();
					}
				}
				return url + "?" + urlParameters;
			}
		}
		
		return null;
	}

	public int getReviewScore(String contentId) throws QueueException,
			ReportException, Exception {
		/**
		 * contentId: /attachment/04bad844-493c-45a1-95b4-af70129d54d1/Assignments/b9872422-fb24-4f85-abf5-2fe0e069b251/plag.docx
		 */
		
		int score = 0;
		String[] contentSplit = contentId.split("/");
		if(contentSplit.length > 2){
			String context = contentSplit[2];
			Map<String, String> params = new HashMap<String, String>();
			params.put(PARAM_CONSUMER, consumer);
			params.put(PARAM_CONSUMER_SECRET, consumerSecret);
			params.put(PARAM_EXTERNAL_CONTENT_ID, contentId);
			JSONObject results = getResults(generateUrl(context, null, null), params);
			if(results != null && results.size() == 1){
				score = new ArrayList<Integer>(results.values()).get(0);
			}
		}
		
		return score;
	}

	public Long getReviewStatus(String contentId) throws QueueException {
		//dont worry about implementing this, our status is always ready
		return null;
	}

	public String getServiceName() {
		return "Plagiarism Check";
	}

	public boolean isAcceptableContent(ContentResource arg0) {
		return true;
	}

	public boolean isSiteAcceptable(Site arg0) {
		return true;
	}

	public void processQueue() {
		// TODO Auto-generated method stub
		
	}

	public void queueContent(String userId, String siteId, String assignmentReference, String contentId)
			throws QueueException {
		/**
		 * Example call:
		 * userId: null
		 * siteId: null
		 * assignmentReference: /assignment/a/04bad844-493c-45a1-95b4-af70129d54d1/fa40eac1-5396-4a71-9951-d7d64b8a7710
		 * contentId: /attachment/04bad844-493c-45a1-95b4-af70129d54d1/Assignments/b9872422-fb24-4f85-abf5-2fe0e069b251/plag.docx
		 */
		
		try {
			ContentResource res = contentHostingService.getResource(contentId);
			if(res != null){
				String userParam = res.getProperties().getProperty(ResourceProperties.PROP_CREATOR);
				String[] split = assignmentReference.split("/");
				if(split.length == 5){
					String contextParam = split[3];
					String assignmentParam = split[4];
					User u = userDirectoryService.getUser(userParam);
					String userFirstNameParam = u.getFirstName();
					String userLastNameParam = u.getLastName();
					String userEmailParam = u.getEmail();
					//it doesn't matter, all users are learners in the Sakai Integration
					String userRoleParam = PARAM_USER_ROLE_LEARNER;
					
					
					HttpClient client = HttpClientBuilder.create().build();
					HttpPost post = new HttpPost(generateUrl(contextParam, assignmentParam, userParam));
					try {

						MultipartEntityBuilder builder = MultipartEntityBuilder.create();        
					    builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
					    ContentBody bin = new ByteArrayBody(res.getContent(), res.getProperties().getProperty(ResourceProperties.PROP_DISPLAY_NAME));
					    builder.addPart(PARAM_FILE_DATA, bin);  
					    builder.addTextBody(PARAM_CONSUMER, consumer);
					    builder.addTextBody(PARAM_CONSUMER_SECRET, consumerSecret);
					    builder.addTextBody(PARAM_USER_FIRST_NAME, userFirstNameParam);
					    builder.addTextBody(PARAM_USER_LAST_NAME, userLastNameParam);
					    builder.addTextBody(PARAM_USER_EMAIL, userEmailParam);
					    builder.addTextBody(PARAM_USER_ROLE, userRoleParam);
					    builder.addTextBody(PARAM_EXTERNAL_CONTENT_ID, contentId);
					    final HttpEntity entity = builder.build();
					    post.setEntity(entity);
					    HttpResponse response = client.execute(post);        

					    try{
					    	String resultStr = getContent(response);
					    	JSONObject responseObj = JSONObject.fromObject(resultStr);
					    	if(responseObj != null && responseObj.containsKey("result")){
					    		if(!"success".equals(responseObj.get("result"))){
					    			throw new QueueException("result wasn't a success: " + resultStr);
					    		}
					    	}else{
					    		throw new QueueException("result was either null, invalid json, or didn't have a result value: " + resultStr);
					    	}
					    }catch(Exception e){
					    	throw new QueueException("Error while processing resonse result content", e);
					    }
					} catch (IOException e) {
						throw new QueueException("Error while submitting paper: " + e.getMessage(), e);
					}
				}
			}
		}catch(Exception e){
			throw new QueueException(e);
		}
		
	}

	public void removeFromQueue(String arg0) {
		// TODO Auto-generated method stub
		
	}

	public void resetUserDetailsLockedItems(String arg0) {
		// TODO Auto-generated method stub
		
	}
	

	public String getReviewError(String contentId){
		return null;
	}
	
	/**
	 * returns a map of {User => Score}
	 * @return
	 */
	public JSONObject getResults(String url, Map<String, String> params){

		HttpClient client = HttpClientBuilder.create().build();
		HttpPost post = new HttpPost(url);

		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
		for(Entry<String, String> entry : params.entrySet()){
			nameValuePairs.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
		}
		try {
			post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			HttpResponse response = client.execute(post);
			return JSONObject.fromObject(getContent(response));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private String generateUrl(String context, String assignment, String user){
		String url = serviceUrl;
		if(!serviceUrl.endsWith("/")){
			serviceUrl += "/";
		}
		if(context != null){
			url += context + "/";
		}
		if(assignment != null){
			url += assignment + "/";
		}
		if(user != null){
			url += user + "/";
		}
		
		return url;
	}
	
	public static String getContent(HttpResponse response) throws IOException {
	    BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
	    String body = "";
	    String content = "";

	    while ((body = rd.readLine()) != null) 
	    {
	        content += body + "\n";
	    }
	    return content.trim();
	}
	
	public ServerConfigurationService getServerConfigurationService() {
		return serverConfigurationService;
	}

	public void setServerConfigurationService(
			ServerConfigurationService serverConfigurationService) {
		this.serverConfigurationService = serverConfigurationService;
	}

	public ContentHostingService getContentHostingService() {
		return contentHostingService;
	}

	public void setContentHostingService(ContentHostingService contentHostingService) {
		this.contentHostingService = contentHostingService;
	}

	public UserDirectoryService getUserDirectoryService() {
		return userDirectoryService;
	}

	public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
		this.userDirectoryService = userDirectoryService;
	}
}
