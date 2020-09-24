package org.piangles.backbone.services.session.dao;

import com.TBD.backbone.services.session.SessionDetails;
import com.TBD.core.dao.DAOException;

public interface SessionManagementDAO
{
	public void storeSessionDetails(SessionDetails sessionDetails) throws DAOException;
	
	//Remove from all caches expired sessions for this User
	public void removeAllExpiredSessionDetails(String userId) throws DAOException;
	
	//Remove from all caches for this user and this sessionId
	public void removeSessionDetails(String userId, String sessionId) throws DAOException;
	
	//Returns true if session exists else false and if it exists will update the lastAccessedTS
	public boolean isValid(String userId, String sessionId) throws DAOException;
	
	//Touch lastAccessed if session is valid
	public void updateLastAccessed(String userId, String sessionId) throws DAOException;
	
	public boolean doesUserHaveAnExistingSession(String userId) throws DAOException;
}
