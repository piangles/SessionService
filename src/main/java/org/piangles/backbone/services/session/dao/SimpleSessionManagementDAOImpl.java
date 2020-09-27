package org.piangles.backbone.services.session.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.piangles.backbone.services.session.SessionDetails;
import org.piangles.backbone.services.session.cache.CacheAdapter;
import org.piangles.core.dao.DAOException;

public class SimpleSessionManagementDAOImpl extends AbstractSessionManagementDAO
{
	//Only one cache will have SessionDetails the rest just have a reference to SessionId

	private CacheAdapter cache= null;

	public SimpleSessionManagementDAOImpl(long sessionTimeout)
	{
		super(sessionTimeout);
	}
	
	@Override
	public synchronized void storeSessionDetails(SessionDetails sessionDetails) throws DAOException
	{ 
		cache = new CacheAdapter();
		cache.addSession(sessionDetails);
	}

	@Override
	public synchronized void removeAllExpiredSessionDetails(String userId) throws DAOException
	{
		cache = new CacheAdapter();
		List<String> sessionIds = cache.getAllUserSessionIDs(userId);
		sessionIds.forEach(sid -> {
			SessionDetails session = cache.getSession(sid);
			if (!isSessionValid(session))
			{
				cache.removeSession(session.getSessionId(), session.getUserId());
			}
			//no need to add validsessions to sepearte storage as we are having a single list which maintains both valid and invalid sessions
		});
		
	}

	@Override
	public synchronized void removeSessionDetails(String userId, String sessionId) throws DAOException
	{
		cache = new CacheAdapter();
		cache.removeSession(userId, sessionId);
	}

	@Override
	protected synchronized void touch(SessionDetails sessionDetails) throws DAOException
	{
		sessionDetails.touch();
	}

	@Override
	protected synchronized List<String> getAllUserSessionIds(String userId) throws DAOException
	{
		cache = new CacheAdapter();
		return cache.getAllUserSessionIDs(userId);
	}

	@Override
	protected synchronized SessionDetails getSessionDetails(String sessionId) throws DAOException
	{
		cache = new CacheAdapter();
		return cache.getSession(sessionId);
	}
}