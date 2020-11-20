package org.piangles.backbone.services.session.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.piangles.backbone.services.session.SessionDetails;
import org.piangles.backbone.services.session.SessionManagementService;
import org.piangles.core.dao.DAOException;
import org.piangles.core.resources.RedisCache;
import org.piangles.core.resources.ResourceManager;
import org.piangles.core.util.central.CentralConfigProvider;

import redis.clients.jedis.Jedis;

//All methods are synchronized 
public final class DistributedCacheDAOImpl extends AbstractSessionManagementDAO
{
	private static final String USER_ID = "UserId";
	private static final String SESSION_ID = "SessionId";
	private static final String CREATED_TS = "CreatedTS";
	private static final String LAST_ACCESSED_TS = "LastAccessedTS";
	
	private RedisCache redisCache = null;
	
	public DistributedCacheDAOImpl(long sessionTimeout) throws Exception
	{
		super(sessionTimeout);
		redisCache = ResourceManager.getInstance().getRedisCache(new CentralConfigProvider(SessionManagementService.NAME, SessionManagementService.NAME));
	}
	
	public void storeSessionDetails(SessionDetails sessionDetails) throws DAOException
	{
		Jedis jedis = redisCache.getCache();
		jedis.lpush(sessionDetails.getUserId(), sessionDetails.getSessionId());
		jedis.hmset(sessionDetails.getSessionId(), createMap(sessionDetails));
		jedis.close();
	}

	public void removeSessionDetails(String userId, String sessionId) throws DAOException
	{
		Jedis jedis = redisCache.getCache();
		jedis.lrem(userId, 1, sessionId);
		jedis.del(sessionId);
		jedis.close();
	}

	public void updateLastAccessed(String userId, String sessionId) throws DAOException
	{
		Jedis jedis = redisCache.getCache();
		jedis.hset(sessionId, LAST_ACCESSED_TS, "" + System.currentTimeMillis());
		jedis.close();
	}

	protected List<String> getAllUserSessionIds(String userId) throws DAOException
	{
		List<String> sessionIds = null;
		Jedis jedis = redisCache.getCache();
		sessionIds = jedis.lrange(userId, 0, 100);
		jedis.close();
		return sessionIds;
	}

	protected SessionDetails getSessionDetails(String sessionId) throws DAOException
	{
		SessionDetails sessionDetails = null;
		Jedis jedis = redisCache.getCache();
		Map<String, String> map = jedis.hgetAll(sessionId);
		jedis.close();
		sessionDetails = createSessionDetails(map);
		return sessionDetails;
	}
	
	protected void removeAllExpiredSessionDetails(String userId) throws DAOException
	{
		List<String> sessionIds = getAllUserSessionIds(userId);
		if (sessionIds != null)
		{
			Jedis jedis = redisCache.getCache();
			for (String sessionId : sessionIds)
			{
				SessionDetails sessionDetails = createSessionDetails(jedis.hgetAll(sessionId));
				
				if (!isSessionValid(sessionDetails.getLastAccessedTS()))
				{
					jedis.lrem(userId, 1, sessionId);
					jedis.del(sessionId);
				}
			}
			jedis.close();
		}
	}
	
	private Map<String, String> createMap(SessionDetails sessionDetails)
	{
		Map<String, String> map = new HashMap<>();
		map.put(USER_ID, sessionDetails.getUserId());
		map.put(SESSION_ID, sessionDetails.getSessionId());
		map.put(CREATED_TS, "" + sessionDetails.getCreatedTS());
		map.put(LAST_ACCESSED_TS, "" + sessionDetails.getLastAccessedTS());
		
		return map;
	}
	
	private SessionDetails createSessionDetails(Map<String, String> map)
	{
		String userId = map.get(USER_ID);
		String sessionId = map.get(SESSION_ID);
		long createdTS = Long.parseLong(map.get(CREATED_TS));
		long lastAccessedTS = Long.parseLong(map.get(LAST_ACCESSED_TS));
		return new SessionDetails(userId, sessionId, createdTS, lastAccessedTS);
	}
}