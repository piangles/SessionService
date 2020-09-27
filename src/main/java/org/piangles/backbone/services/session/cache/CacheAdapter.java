package org.piangles.backbone.services.session.cache;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.piangles.backbone.services.session.SessionDetails;
import com.google.gson.Gson;
import redis.clients.jedis.Jedis;
/**
 * @author vamsi
 *
 *Util class that enables in-memory persistence of session data.
 */
public class CacheAdapter {
	
	private Jedis cache = null;
	private Gson gson = null;
	private String cacheSession = null;
	private List<String> sessions = null;
	private List<String> cachedSessions = null;
	private List<SessionDetails> sessionDetails = null;
	private static final String SESSIONS = "SESSIONS";
	private static final String USERS_SESSIONS = "USER_SESSIONS";

	/**
	 * Instantiate Jedis object and establish connection with redis server
	 */
	public CacheAdapter()
	{
		InetAddress host = null;
		try
		{
			host = InetAddress.getLocalHost();
		} catch (UnknownHostException e)
		{
			e.printStackTrace();
		}
		cache = new Jedis(host.getHostAddress());
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
	 *@param session
	 */
	public void addSession(SessionDetails session)
	{
		gson = new Gson();
		cacheSession = gson.toJson(session);
		cache.rpush(SESSIONS, cacheSession);
	}

	/**
	 *@param sessionID
	 */
	public SessionDetails getSession(String sessionID)
	{
		cachedSessions = new ArrayList<String>();
		cachedSessions = cache.lrange(SESSIONS, 0, -1);
		SessionDetails sessionDetails = cachedData(cachedSessions).stream()
				.filter(session -> session.getSessionId().equals(sessionID)).collect(Collectors.toList()).get(0);
		return sessionDetails;
	}

	/**
	 * Fetches all sessions from redis list
	 * @return
	 */
	public List<SessionDetails> getAllSessions()
	{
		cachedSessions = new ArrayList<String>();
		cachedSessions = cache.lrange(SESSIONS, 0, -1);
		return cachedData(cachedSessions);
	}

	/**
	 * @param userID
	 * @return
	 */
	public List<String> getAllUserSessionIDs(String userID)
	{
		sessions = new ArrayList<String>();
		List<String> sessionIds = new ArrayList<>();
		sessions = cache.lrange(SESSIONS, 0, -1);
		cachedData(sessions).forEach(session ->
		{
			sessionIds.add(session.getSessionId());
		});
		return sessionIds;
	}

	/**
	 * Method removes session from the redis cache. Retrieves the Session object from redis, 
	 * translates it JSON string and pass the JSON string as a redis value for deletion
	 * 
	 *@param userId, sessionId
	 *@return null
	 */
	public void removeSession(String userId, String sessionId)
	{
		cachedSessions = new ArrayList<String>();
		sessionDetails = new ArrayList<>();
		gson = new Gson();
		sessions = cache.lrange(SESSIONS, 0, -1);
		sessionDetails = cachedData(sessions);
		SessionDetails sd = sessionDetails.stream()
				.filter(session -> session.getUserId().equals(userId) && session.getSessionId().equals(sessionId))
				.collect(Collectors.toList()).get(0);
		if(!sd.equals(null))
		{
			cache.lrem(SESSIONS, 1, gson.toJson(sd));
		}
	}


}
