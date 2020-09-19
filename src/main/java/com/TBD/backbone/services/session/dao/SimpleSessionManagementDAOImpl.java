package com.TBD.backbone.services.session.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.TBD.backbone.services.session.SessionDetails;
import com.TBD.core.dao.DAOException;

public class SimpleSessionManagementDAOImpl extends AbstractSessionManagementDAO
{
	//Only one cache will have SessionDetails the rest just have a reference to SessionId
	private Map<String, SessionDetails> sessionIdMap = null;
	private Map<String, List<String>> userIdSessionMap = null;

	public SimpleSessionManagementDAOImpl(long sessionTimeout)
	{
		super(sessionTimeout);
		sessionIdMap = new HashMap<>();
		userIdSessionMap = new HashMap<String, List<String>>();
	}
	
	@Override
	public synchronized void storeSessionDetails(SessionDetails sessionDetails) throws DAOException
	{
		sessionIdMap.put(sessionDetails.getSessionId(), sessionDetails);
		List<String> sessionIds = userIdSessionMap.get(sessionDetails.getUserId()); 
		if (sessionIds == null)
		{
			sessionIds = new ArrayList<String>();
			userIdSessionMap.put(sessionDetails.getUserId(), sessionIds);
		}
		sessionIds.add(sessionDetails.getSessionId());
	}

	@Override
	public synchronized void removeAllExpiredSessionDetails(String userId) throws DAOException
	{
		List<String> validSessionIds = new ArrayList<String>();
		List<String> sessionIds = userIdSessionMap.get(userId);
		if (sessionIds != null)
		{
			SessionDetails sessionDetails = null;
			for (String sessionId : sessionIds)
			{
				sessionDetails = sessionIdMap.get(sessionId);
				if (!isSessionValid(sessionDetails))
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

	@Override
	public synchronized void removeSessionDetails(String userId, String sessionId) throws DAOException
	{
		sessionIdMap.get(sessionId);
		List<String> sessionIds = userIdSessionMap.get(userId);
		if (sessionIds != null)
		{
			sessionIds.remove(sessionId);
		}
	}

	@Override
	protected synchronized void touch(SessionDetails sessionDetails) throws DAOException
	{
		sessionDetails.touch();
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
}
