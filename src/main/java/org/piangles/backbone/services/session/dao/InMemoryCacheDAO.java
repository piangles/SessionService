package org.piangles.backbone.services.session.dao;

import java.util.List;

import org.piangles.backbone.services.session.SessionDetails;
import org.piangles.backbone.services.session.cache.SessionCacheAdapter;
import org.piangles.core.dao.DAOException;

public class InMemoryCacheDAO
{
	//Only one cache will have SessionDetails the rest just have a reference to SessionId

	private SessionCacheAdapter cache;

	public InMemoryCacheDAO(long sessionTimeout)
	{
		cache = new SessionCacheAdapter(sessionTimeout);
	}
	
	
	public synchronized void storeSessionDetails(SessionDetails sessionDetails) throws DAOException
	{ 
		cache.storeSessionDetails(sessionDetails);
	}

	public synchronized void removeAllExpiredSessionDetails(String userId) throws DAOException
	{
		cache.removeAllExpiredSessionDetails(userId);
	}

	
	public synchronized void removeSessionDetails(String userId, String sessionId) throws DAOException
	{
		cache.removeSessionDetails(userId, sessionId);
	}


	protected synchronized void touch(SessionDetails sessionDetails) throws DAOException
	{
		sessionDetails.touch();
	}

	
	protected synchronized List<String> getAllUserSessionIds(String userId) throws DAOException
	{
		return cache.getAllUserSessionIds(userId);
	}

	
	protected synchronized SessionDetails getSessionDetails(String sessionId) throws DAOException
	{
		return cache.getSessionDetails(sessionId);
	}
}