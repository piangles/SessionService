package org.piangles.backbone.services.session.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.piangles.backbone.services.session.SessionDetails;
import org.piangles.core.dao.DAOException;

public final class InMemoryDAOImpl extends AbstractSessionManagementDAO
{
	//Only one cache will have SessionDetails the rest just have a reference to SessionId
	private Map<String, SessionDetails> sessionIdMap = null;
	private Map<String, List<String>> userIdSessionMap = null;

	public InMemoryDAOImpl(long sessionTimeout) throws Exception
	{
		super(sessionTimeout);
		sessionIdMap = new HashMap<>();
		userIdSessionMap = new HashMap<String, List<String>>();
	}
	
	@Override
	public synchronized void storeSessionDetails(SessionDetails sessionDetails) throws DAOException
	{
		//Store in the userMap the sessionId
		List<String> sessionIds = userIdSessionMap.get(sessionDetails.getUserId()); 
		if (sessionIds == null)
		{
			sessionIds = new ArrayList<String>();
			userIdSessionMap.put(sessionDetails.getUserId(), sessionIds);
		}

		//Store the session details
		sessionIdMap.put(sessionDetails.getSessionId(), sessionDetails);
		sessionIds.add(sessionDetails.getSessionId());
	}

	@Override
	public synchronized void removeSessionDetails(String userId, String sessionId) throws DAOException
	{
		List<String> sessionIds = userIdSessionMap.get(userId);
		if (sessionIds != null)
		{
			sessionIds.remove(sessionId);
			sessionIdMap.remove(sessionId);
		}
	}

	@Override
	public synchronized void updateLastAccessed(String userId, String sessionId) throws DAOException
	{
		SessionDetails sessionDetails = getSessionDetailsIfValidById(userId, sessionId);
		if (sessionDetails != null)
		{
			sessionDetails.touch();
		}
	}

	@Override
	protected synchronized List<String> getAllUserSessionIds(String userId) throws DAOException
	{
		return userIdSessionMap.get(userId);
	}

	@Override
	protected synchronized SessionDetails getSessionDetails(String sessionId) throws DAOException
	{
		return sessionIdMap.get(sessionId);
	}
	
	@Override
	protected synchronized void removeAllExpiredSessionDetails(String userId) throws DAOException
	{
		List<String> validSessionIds = new ArrayList<String>();
		List<String> sessionIds = userIdSessionMap.get(userId);
		if (sessionIds != null)
		{
			for (String sessionId : sessionIds)
			{
				SessionDetails sessionDetails = sessionIdMap.get(sessionId);
				
				if (!isSessionValid(sessionDetails.getLastAccessedTS()))
				{
					sessionIdMap.remove(sessionId);
				}
				else
				{
					validSessionIds.add(sessionId);
				}
			}
			userIdSessionMap.put(userId, validSessionIds);
		}
	}
}