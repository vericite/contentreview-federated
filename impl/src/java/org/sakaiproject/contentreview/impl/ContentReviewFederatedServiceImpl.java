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

/* This class is passed a list of providers in the bean as references, it will use the first
 * by default unless overridden by a site property.
 */
public class ContentReviewFederatedServiceImpl implements ContentReviewService {

	private ServerConfigurationService serverConfigurationService;
	private List <ContentReviewService> providers;
	String defaultProvider;
	
	public void init(){
		defaultProvider = serverConfigurationService.getString("contentreview.defaultProvider", "0");
	}
	
	public ContentReviewService getSelectedProvider() {
		if (providers.size() > 0)
			return providers.get(0);
		return null;
	}

	public List <ContentReviewService> getProviders() {
		return providers;
	}

	public void setProviders(List <ContentReviewService> providers) {
		this.providers = providers;
	}

	public ServerConfigurationService getServerConfigurationService() {
		return serverConfigurationService;
	}

	public void setServerConfigurationService(
			ServerConfigurationService serverConfigurationService) {
		this.serverConfigurationService = serverConfigurationService;
	}
	
	public boolean allowResubmission() {
		ContentReviewService provider = getSelectedProvider();
		if (provider != null)
			return provider.allowResubmission();
		return false;
	}

	public void checkForReports() {
		ContentReviewService provider = getSelectedProvider();
		if (provider != null)
			provider.checkForReports();	
	}

	public void createAssignment(String arg0, String arg1, Map arg2)
		throws SubmissionException, TransientSubmissionException {
		ContentReviewService provider = getSelectedProvider();
		if (provider != null)
			provider.createAssignment(arg0,arg1,arg2);
		
	}

	public List<ContentReviewItem> getAllContentReviewItems(String arg0,
			String arg1) throws QueueException, SubmissionException,
			ReportException {
		// TODO Auto-generated method stub
		ContentReviewService provider = getSelectedProvider();
		if (provider != null)
			return provider.getAllContentReviewItems(arg0,arg1);
		return null;
	}

	public Map getAssignment(String arg0, String arg1)
			throws SubmissionException, TransientSubmissionException {

		// TODO Auto-generated method stub
		ContentReviewService provider = getSelectedProvider();
		if (provider != null)
			return provider.getAssignment(arg0,arg1);
		return null;
	}

	public Date getDateQueued(String arg0) throws QueueException {
		// TODO Auto-generated method stub
		ContentReviewService provider = getSelectedProvider();
		if (provider != null)
			return provider.getDateQueued(arg0);
		return null;
	}

	public Date getDateSubmitted(String arg0) throws QueueException,
			SubmissionException {
		// TODO Auto-generated method stub
		ContentReviewService provider = getSelectedProvider();
		if (provider != null)
			return provider.getDateSubmitted(arg0);
		return null;
	}

	public String getIconUrlforScore(Long score) {
		ContentReviewService provider = getSelectedProvider();
		if (provider != null)
			return provider.getIconUrlforScore(score);
		return null;
	}

	public String getLocalizedStatusMessage(String arg0) {
		// TODO Auto-generated method stub
		ContentReviewService provider = getSelectedProvider();
		if (provider != null)
			return provider.getLocalizedStatusMessage(arg0);
		return null;
	}

	public String getLocalizedStatusMessage(String arg0, String arg1) {
		// TODO Auto-generated method stub
		ContentReviewService provider = getSelectedProvider();
		if (provider != null)
			return provider.getLocalizedStatusMessage(arg0,arg1);
		return null;
	}

	public String getLocalizedStatusMessage(String arg0, Locale arg1) {
		// TODO Auto-generated method stub
		ContentReviewService provider = getSelectedProvider();
		if (provider != null)
			return provider.getLocalizedStatusMessage(arg0,arg1);
		return null;
	}

	public List<ContentReviewItem> getReportList(String siteId)
			throws QueueException, SubmissionException, ReportException {
		// TODO Auto-generated method stub
		ContentReviewService provider = getSelectedProvider();
		if (provider != null)
			return provider.getReportList(siteId);
		return null;
	}

	public List<ContentReviewItem> getReportList(String siteId, String taskId)
			throws QueueException, SubmissionException, ReportException {
		ContentReviewService provider = getSelectedProvider();
		if (provider != null)
			return provider.getReportList(siteId,taskId);
		return null;
	}

	public String getReviewReport(String contentId) throws QueueException,
			ReportException {
		ContentReviewService provider = getSelectedProvider();
		if (provider != null)
			return provider.getReviewReport(contentId);
		return null;
	}

	public String getReviewReportInstructor(String contentId) throws QueueException,
			ReportException {
		ContentReviewService provider = getSelectedProvider();
		if (provider != null)
			return provider.getReviewReportInstructor(contentId);
		return null;
	}

	public String getReviewReportStudent(String contentId) throws QueueException,
			ReportException {
		ContentReviewService provider = getSelectedProvider();
		if (provider != null)
			return provider.getReviewReportStudent(contentId);
		return null;
	}
	
	public Long getReviewStatus(String contentId) throws QueueException {
		//dont worry about implementing this, our status is always ready
		ContentReviewService provider = getSelectedProvider();
		if (provider != null)
			return provider.getReviewStatus(contentId);
		return null;
	}

	public String getServiceName() {
		ContentReviewService provider = getSelectedProvider();
		if (provider != null)
			return provider.getServiceName();
		return null;
	}

	public boolean isAcceptableContent(ContentResource arg0) {
		ContentReviewService provider = getSelectedProvider();
		if (provider != null)
			return provider.isAcceptableContent(arg0);
		return false;
	}

	public boolean isSiteAcceptable(Site arg0) {
		ContentReviewService provider = getSelectedProvider();
		if (provider != null)
			return provider.isSiteAcceptable(arg0);
		return false;
	}

	public void processQueue() {
		// TODO Auto-generated method stub
		ContentReviewService provider = getSelectedProvider();
		if (provider != null)
			provider.processQueue();
	}

	public void queueContent(String userId, String siteId, String assignmentReference, String contentId)
			throws QueueException {
		ContentReviewService provider = getSelectedProvider();
		if (provider != null)
			provider.queueContent(userId,siteId,assignmentReference,contentId);
	}

	public void removeFromQueue(String arg0) {
		// TODO Auto-generated method stub
		ContentReviewService provider = getSelectedProvider();
		if (provider != null)
			provider.removeFromQueue(arg0);
	}

	public void resetUserDetailsLockedItems(String arg0) {
		// TODO Auto-generated method stub
		ContentReviewService provider = getSelectedProvider();
		if (provider != null)
			provider.resetUserDetailsLockedItems(arg0);
		
	}
	public String getReviewError(String contentId){
		ContentReviewService provider = getSelectedProvider();
		if (provider != null)
			return provider.getReviewError(contentId);
		return null;
	}

	@Override
	public int getReviewScore(String arg0) throws QueueException,
			ReportException, Exception {
		// TODO Auto-generated method stub
		ContentReviewService provider = getSelectedProvider();
		if (provider != null)
			return provider.getReviewScore(arg0);
		return 0;
	}
}
