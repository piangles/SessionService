package org.piangles.backbone.services.session.cache;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.piangles.backbone.services.session.SessionDetails;
import org.piangles.backbone.services.session.dao.AbstractSessionManagementDAO;
import org.piangles.core.dao.DAOException;

import com.google.gson.Gson;
import redis.clients.jedis.Jedis;

/**
 * Util class that enables in-memory persistence of session data.
 */
public class SessionCacheAdapter extends AbstractSessionManagementDAO
{

	private Jedis cache = null;
	private Gson gson = null;
	private String cacheSession = null;
	private Map<String, String> sessions = null;
	private List<String> cachedSessions = null;
	private List<SessionDetails> sessionDetails = null;
	private static final String SESSIONS = "SESSIONS";
	private static final String HOST = "ec2-3-88-33-144.compute-1.amazonaws.com:6379";

	/**
	 * Instantiate Jedis object and establish connection with redis server
	 */
	public SessionCacheAdapter(long sessionTimeout)
	{
		super(sessionTimeout);

		// TODO: If localhost
		InetAddress host = null;
		try
		{
			host = InetAddress.getLocalHost();
		} catch (UnknownHostException e)
		{
			e.printStackTrace();
		}

		cache = new Jedis(HOST);
	}

	/**
	 * Helper Methods to convert list of JSON strings to List of Objects
	 * 
	 * @param jsonString
	 * @return list of session objects
	 */
	private List<SessionDetails> cachedData(List<String> jsonString)
	{
		gson = new Gson();
		List<SessionDetails> sessions = new ArrayList<>();
		jsonString.forEach(js ->
		{
			sessions.add(gson.fromJson(js, SessionDetails.class));
		});
		return sessions;

	}

	/**
	 * Creates and maintains redis list for session objects.
	 * 
	 * @param session
	 */
	@Override
	public void storeSessionDetails(SessionDetails sessionDetails) throws DAOException
	{
		gson = new Gson();
		cacheSession = gson.toJson(sessionDetails);
		cache.hset(SESSIONS, sessionDetails.getSessionId(), cacheSession);

	}

	@Override
	public void removeAllExpiredSessionDetails(String userId) throws DAOException
	{
		List<String> sessionIds = getAllUserSessionIds(userId);
		sessionIds.forEach(sid ->
		{
			SessionDetails session = null;
			try
			{
				session = getSessionDetails(sid);
			} catch (DAOException e1)
			{
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			if (!isSessionValid(session))
			{
				try
				{
					removeSessionDetails(session.getUserId(), session.getSessionId());
				} catch (DAOException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

	}

	/**
	 * Method removes session from the redis cache. Retrieves the Session object
	 * from redis, translates it JSON string and pass the JSON string as a redis
	 * value for deletion
	 * 
	 * @param userId, sessionId
	 * @return null
	 */

	@Override
	public void removeSessionDetails(String userId, String sessionId) throws DAOException
	{
		cachedSessions = new ArrayList<String>();
		sessionDetails = new ArrayList<>();
		gson = new Gson();
		List<String> userSessions = new ArrayList<>();
		userSessions.addAll(cache.hgetAll(SESSIONS).values());
		sessionDetails = cachedData(userSessions);
		SessionDetails sd = sessionDetails.stream()
				.filter(session -> session.getUserId().equals(userId)
						&& session.getSessionId().equals(sessionId))
				.collect(Collectors.toList()).get(0);
		if (!sd.equals(null))
		{
			cache.hdel(SESSIONS, sessionId);
		}
	}

	@Override
	protected void touch(SessionDetails sessionDetails) throws DAOException
	{
		// TODO Auto-generated method stub

	}

	@Override
	public List<String> getAllUserSessionIds(String userId) throws DAOException
	{
		sessions = new HashMap<>();
		List<String> sessionIds = new ArrayList<>();
		List<String> userSessions = new ArrayList<>();
		sessions = cache.hgetAll(SESSIONS);
		userSessions.addAll(sessions.values());

		cachedData(userSessions).stream().filter(us -> us.getUserId().equals(userId))
				.collect(Collectors.toList()).forEach(session ->
				{
					sessionIds.add(session.getSessionId());
				});
		return sessionIds;
	}

	@Override
	public SessionDetails getSessionDetails(String sessionId) throws DAOException
	{
		cachedSessions = new ArrayList<String>();
		cachedSessions.add(cache.hget(SESSIONS, sessionId));
		SessionDetails sessionDetails = cachedData(cachedSessions).stream()
				.filter(session -> session.getSessionId().equals(sessionId))
				.collect(Collectors.toList()).get(0);
		return sessionDetails;
	}

}
