package com.TBD.backbone.services.session;

import java.util.HashMap;
import java.util.UUID;

import com.TBD.backbone.services.Locator;
import com.TBD.backbone.services.logging.LoggingService;
import com.TBD.backbone.services.session.dao.SessionManagementDAO;
import com.TBD.backbone.services.session.dao.SimpleSessionManagementDAOImpl;
import com.TBD.core.dao.DAOException;

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
	//TODO Vamsi - Need POM to be updated to include BackbonServicesLocator
	private LoggingService logger = Locator.getInstance().getLoggingService();
	private HashMap<String, String> predeterminedSessionIdMap = null;
	private SessionManagementDAO sessionManagementDAO;
	private boolean allowMultipleSessionsPerUser;

	public SessionManagementServiceImpl() throws Exception
	{
		predeterminedSessionIdMap = new HashMap<String, String>();

		allowMultipleSessionsPerUser = false;
		
		// TODO Populate Predetermined sessionIds from Central Service
		predeterminedSessionIdMap.put("LoggingService", "TODOSessionId");
		predeterminedSessionIdMap.put("UserPreferenceService", "TODOSessionId");
		predeterminedSessionIdMap.put("GatewayService", "TODOSessionId");
		predeterminedSessionIdMap.put("AuthenticationService", "TODOSessionId");
		
		//TODO Retrieve from Central Client the timeout property
		long sessionTimeout = 1000 * 60; 
		sessionManagementDAO = new SimpleSessionManagementDAOImpl(sessionTimeout);
	}

	@Override
	public SessionDetails register(String userId) throws SessionManagementException
	{
		SessionDetails sessionDetails = null;
		try
		{
			if (!allowMultipleSessionsPerUser && sessionManagementDAO.doesUserHaveAnExistingSession(userId))
			{
				throw new SessionManagementException("User " + userId + " already has an active session.");
			}

			String sessionId = UUID.randomUUID().toString();
			sessionDetails = new SessionDetails(userId, sessionId);

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
			String predeterminedSessionId = predeterminedSessionIdMap.get(userId);
			if (predeterminedSessionId != null && predeterminedSessionId.equals(sessionId))
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
