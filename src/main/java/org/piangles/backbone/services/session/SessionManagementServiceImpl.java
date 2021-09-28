/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
 
 
package org.piangles.backbone.services.session;

import java.util.HashMap;
import java.util.Properties;
import java.util.UUID;

import org.piangles.backbone.services.Locator;
import org.piangles.backbone.services.logging.LoggingService;
import org.piangles.backbone.services.session.dao.DistributedCacheDAOImpl;
import org.piangles.backbone.services.session.dao.InMemoryDAOImpl;
import org.piangles.backbone.services.session.dao.SessionManagementDAO;
import org.piangles.core.dao.DAOException;
import org.piangles.core.util.central.CentralClient;

/**
 * SessionIdProvider will be providing SessionId during RequestCreation.
 * SessionId will be part of Request which is Service level Request. There are
 * two types of request which end up with a Service. 1. Which originate from
 * another service. 2. Which originate from client.
 * 
 * 1. When from another service the issue is the calling service is not
 * authenticated so the calling service when it creates a request does not have
 * sessionId populated. Somehow the service needs to allow for serice to service
 * calls which do not originate from request endpooint code.
 * 
 * One point to remember is : If Service A calls during it's creation /
 * initialization Service B. Now Service A does not have a sessionId, however
 * when it calls the Service B PREDETERMINED-sessionId is passed to it and that
 * gets copied to RequestProcessor(Thread) which also implements
 * SessionIdProvider.
 * 
 * 
 * Also services can be 1. Those that require Calers be authenticated. 2. Those
 * that do need Callers to be authenticated.
 *
 */
public class SessionManagementServiceImpl implements SessionManagementService
{
	private static final String MANAGED_SERVICE = "ManagedService";
	private static final String PRE_APPROVED_SESSION_ID = "PreApprovedSessionId";
	private static final String SESSION_TIMEOUT = "SessionTimeout";
	private static final String MARK_SESSION_TIMEOUT = "MarkSessionTimeout";
	private static final String ALLOW_MULTIPLE_SESSIONS = "AllowMultipleSessions";
	private static final String MAX_SESSION_COUNT = "MaxSessionCount";
	private static final String DAO_TYPE = "DAOType";
	private static final String DEFAULT_DAO_TYPE = "DistributedCache";
	
	private LoggingService logger = Locator.getInstance().getLoggingService();
	private HashMap<String, String> predeterminedSessionIdMap = null;
	private SessionManagementDAO sessionManagementDAO;
	private boolean allowMultipleSessionsPerUser;
	private int maxSessiontCountPerUser;

	public SessionManagementServiceImpl() throws Exception
	{
		predeterminedSessionIdMap = new HashMap<String, String>();

		/**
		 * SessionService will always have a PassThruSessionValidator any calls to it
		 * will not have the session validated.
		 * 
		 * Tier1 services 
		 * - CryptoService
		 * - ConfigService
		 * get their Configuration from CentralService. So for them to come up they do 
		 * not need SessionValidation.
		 * 
		 * Rest of the services however need SessionValidation for retriving configuration
		 * and decrypting properties on StartUp. So for that reason there are 
		 * PreApprovedSessionId. When the rest of the services call for config and cyrpto 
		 * the SessionValidator calls
		 * 	> public boolean isValid(String userId, String sessionId) throws SessionManagementException
		 * 
		 * with userId being the name of the service and sessionId being null. The
		 * map below will help bypass the actual validation for the sessionId. It is a map to
		 * help lookup faster.
		 * 
		 */
		Properties sessionMgmtProperties = CentralClient.getInstance().tier1Config(NAME);
		Properties discoveryProperties = null;
		int count = 0;
		while (true)
		{
			String serviceName = sessionMgmtProperties.getProperty(MANAGED_SERVICE+count);
			/**
			 * Count 0 is reserved for FeaturesTestService, this needs to be disabled in
			 * production. And if disabled the count starts from 1 for the actual services.
			 */
			if (serviceName == null && count != 0)
			{
				break;
			}
			else if (serviceName != null)
			{
				logger.info("Looking up for " + PRE_APPROVED_SESSION_ID + " for service: " + serviceName);
				discoveryProperties = CentralClient.getInstance().discover(serviceName);
				predeterminedSessionIdMap.put(serviceName, discoveryProperties.getProperty(PRE_APPROVED_SESSION_ID));
			}
			count++;
		}
		
		if (predeterminedSessionIdMap.size() == 0)
		{
			throw new Exception("There are no PreApprovedSessionId configured.");
		}
		
		String sessionTimeoutAsStr = sessionMgmtProperties.getProperty(SESSION_TIMEOUT);
		String markSessionTimeoutAsStr = sessionMgmtProperties.getProperty(MARK_SESSION_TIMEOUT);
		long sessionTimeout;
		int markSessionTimeout;
		try
		{
			sessionTimeout = Integer.parseInt(sessionTimeoutAsStr) * 1000;
			markSessionTimeout = Integer.parseInt(markSessionTimeoutAsStr);
		}
		catch(Exception expt)
		{
			System.err.println("Could not parse into Integer " + SESSION_TIMEOUT + " property:" + sessionTimeoutAsStr);
			throw expt;
		}
		
		String allowMultipleSessionsPerUserAsStr = sessionMgmtProperties.getProperty(ALLOW_MULTIPLE_SESSIONS);
		try
		{
			if (allowMultipleSessionsPerUserAsStr == null)
			{
				throw new Exception(ALLOW_MULTIPLE_SESSIONS + " is null.");
			}
			allowMultipleSessionsPerUser = Boolean.parseBoolean(allowMultipleSessionsPerUserAsStr);
		}
		catch(Exception expt)
		{
			System.err.println("Could not parse into Boolean " + ALLOW_MULTIPLE_SESSIONS + " property:" + allowMultipleSessionsPerUserAsStr);
			throw expt;
		}

		String maxSessiontCountPerUserAsStr = sessionMgmtProperties.getProperty(MAX_SESSION_COUNT);
		try
		{
			if (allowMultipleSessionsPerUser)
			{
				maxSessiontCountPerUser = Integer.parseInt(maxSessiontCountPerUserAsStr);
			}
		}
		catch(Exception expt)
		{
			System.err.println("Could not parse into Integer " + MAX_SESSION_COUNT + " property:" + maxSessiontCountPerUserAsStr);
			throw expt;
		}

		if (DEFAULT_DAO_TYPE.equals(sessionMgmtProperties.getProperty(DAO_TYPE)))
		{
			sessionManagementDAO = new DistributedCacheDAOImpl(sessionTimeout, markSessionTimeout);
		}
		else
		{
			sessionManagementDAO = new InMemoryDAOImpl(sessionTimeout, markSessionTimeout);
		}
		logger.info("Starting SessionManagementService with DAO: " + sessionManagementDAO.getClass());
	}

	@Override
	public SessionDetails register(String userId) throws SessionManagementException
	{
		SessionDetails sessionDetails = null;
		try
		{
			int existingValidSessionCount = sessionManagementDAO.getExistingValidSessionCount(userId);
			if (!allowMultipleSessionsPerUser && existingValidSessionCount > 1)
			{
				throw new SessionManagementException("User " + userId + " already has an active session.");
			}
			else if (allowMultipleSessionsPerUser && existingValidSessionCount >= maxSessiontCountPerUser)
			{
				throw new SessionManagementException("User " + userId + " has reached maximum active sessions.");
			}

			String sessionId = UUID.randomUUID().toString();
			sessionDetails = new SessionDetails(userId, sessionId);

			logger.info("Register Session for UserId:" + userId + " SessionId:"+sessionId);

			sessionManagementDAO.storeSessionDetails(sessionDetails);
		}
		catch (DAOException e)
		{
			String message = "Unable to register session because of : " + e.getMessage();
			logger.error(message, e);
			throw new SessionManagementException(message);
		}

		return sessionDetails;
	}

	@Override
	public boolean isValid(String userId, String sessionId) throws SessionManagementException
	{
		boolean valid = false;
		try
		{
			logger.info("Validating Session for UserId:" + userId + " SessionId:"+sessionId);
			String preApprovedSessionId = predeterminedSessionIdMap.get(userId);
			if (preApprovedSessionId != null && preApprovedSessionId.equals(sessionId))
			{
				valid = true;
			}
			else
			{
				valid = sessionManagementDAO.isValid(userId, sessionId);
			}
		}
		catch (DAOException e)
		{
			String message = "Unable to validate session because of : " + e.getMessage();
			logger.error(message, e);
			throw new SessionManagementException(message);
		}
		return valid;
	}

	@Override
	public void unregister(String userId, String sessionId) throws SessionManagementException
	{
		try
		{
			logger.info("Unregister Session for UserId:" + userId + " SessionId:"+sessionId);
			sessionManagementDAO.removeSessionDetails(userId, sessionId);
		}
		catch (DAOException e)
		{
			String message = "Unable to unregister session because of : " + e.getMessage();
			logger.error(message, e);
			throw new SessionManagementException(message);
		}
	}

	@Override
	public void markForUnregister(String userId, String sessionId) throws SessionManagementException
	{
		try
		{
			logger.info("Marking for Unregister Session for UserId:" + userId + " SessionId:"+sessionId);
			sessionManagementDAO.markForRemoveSessionDetails(userId, sessionId);
		}
		catch (DAOException e)
		{
			String message = "Unable to markForUnregister session because of : " + e.getMessage();
			logger.error(message, e);
			throw new SessionManagementException(message);
		}
	}

	@Override
	public void makeLastAccessedCurrent(String userId, String sessionId) throws SessionManagementException
	{
		try
		{
			sessionManagementDAO.updateLastAccessed(userId, sessionId);
		}
		catch (DAOException e)
		{
			String message = "Unable to makeLastAccessedCurrent for session because of : " + e.getMessage();
			logger.error(message, e);
			throw new SessionManagementException(message);
		}
	}
}
