package org.piangles.backbone.services.session.dao;

import java.util.List;

import com.TBD.backbone.services.session.SessionDetails;
import com.TBD.core.dao.DAOException;

public abstract class AbstractSessionManagementDAO implements SessionManagementDAO
{
	private long sessionTimeout;

	public AbstractSessionManagementDAO(long sessionTimeout)
	{
		this.sessionTimeout = sessionTimeout;
	}
	
	@Override
	public final boolean isValid(String userId, String sessionId) throws DAOException
	{
		boolean valid = false;
		SessionDetails sessionDetails = getSessionDetailsById(userId, sessionId);
		if (sessionDetails != null)
		{
			valid = true;
		}
		
		return valid;
	}

	@Override
	public final void updateLastAccessed(String userId, String sessionId) throws DAOException
	{
		SessionDetails sessionDetails = getSessionDetailsById(userId, sessionId);
		if (sessionDetails != null)
		{
			touch(sessionDetails);
		}
	}

	@Override
	public final boolean doesUserHaveAnExistingSession(String userId) throws DAOException
	{
		boolean exists = false;
		
		removeAllExpiredSessionDetails(userId);
		List<String> allUserSessionIds = getAllUserSessionIds(userId);

		if (allUserSessionIds != null)
		{
			int validSessionCount = 0;
			 for (String sessionId : allUserSessionIds)
			 {
				 SessionDetails sessionDetails = getSessionDetailsById(userId, sessionId);
				 if (sessionDetails != null)
				 {
					 validSessionCount++;
				 }
			 }
			 exists = validSessionCount > 0;
		}
		
		return exists;
	}

	protected final SessionDetails getSessionDetailsById(String userId, String sessionId) throws DAOException
	{
		SessionDetails sessionDetails = getSessionDetails(sessionId);
		if (sessionDetails != null && sessionDetails.getUserId().equals(userId))
		{
			if (!isSessionValid(sessionDetails))
			{
				sessionDetails = null;
			}
		}
		else
		{
			sessionDetails = null;
		}
		return sessionDetails;
	}

	protected final boolean isSessionValid(SessionDetails sessionDetails)
	{
		long currentTime = System.currentTimeMillis();
		long lastAccessedTime = sessionDetails.getLastAccessedTS().getTime();
		return ((currentTime - lastAccessedTime) < sessionTimeout);
	}

	protected abstract void touch(SessionDetails sessionDetails) throws DAOException;
	protected abstract List<String> getAllUserSessionIds(String userId) throws DAOException;
	protected abstract SessionDetails getSessionDetails(String sessionId) throws DAOException;
}
