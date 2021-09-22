/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
 
 
package org.piangles.backbone.services.session.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.piangles.backbone.services.session.SessionDetails;
import org.piangles.backbone.services.session.SessionManagementService;
import org.piangles.core.dao.DAOException;
import org.piangles.core.resources.RedisCache;
import org.piangles.core.resources.ResourceManager;
import org.piangles.core.util.abstractions.BoundedOp;
import org.piangles.core.util.central.CentralConfigProvider;

import redis.clients.jedis.Jedis;

public final class DistributedCacheDAOImpl extends AbstractSessionManagementDAO
{
	private static final String REDIS_USER2SESSION_ID = "user:%s:session:id";
	private static final String REDIS_USER2SESSION_DETAILS = "user:%s:session:details:%s";
	
	private static final String USER_ID = "UserId";
	private static final String SESSION_ID = "SessionId";
	private static final String CREATED_TS = "CreatedTS";
	private static final String LAST_ACCESSED_TS = "LastAccessedTS";
	
	/**
	 * The reason we are using Redis Lists and Map for saving Session related information
	 * is : SesssionManagementService allows for a user to have multiple sessions. So given
	 * a User we need to identify all the sessions, hence the User->SessionId is stored is a List.
	 * And the most efficient way to save SessionDetails is to break it down into NV Paid Vs
	 * saving it as JSON Objects. 
	 * 
	 * Saving it as JSON gives the additional challenege of having to deseralize the entire JSON Object 
	 * and specifically update LastAccessedTS and put it back into Cache.
	 * 
	 */
	private RedisCache redisCache = null;
	
	public DistributedCacheDAOImpl(long sessionTimeout) throws Exception
	{
		super(sessionTimeout);
		redisCache = ResourceManager.getInstance().getRedisCache(new CentralConfigProvider(SessionManagementService.NAME, SessionManagementService.NAME));
	}
	
	@Override
	public void storeSessionDetails(SessionDetails sessionDetails) throws DAOException
	{
		execute((jedis) -> {
			jedis.lpush(createUser2SessionIdKey(sessionDetails.getUserId()), sessionDetails.getSessionId());
			jedis.hmset(createUser2SessionDetailsKey(sessionDetails.getUserId(), sessionDetails.getSessionId()), createMap(sessionDetails));
			return null;
		});
	}

	@Override
	public void removeSessionDetails(String userId, String sessionId) throws DAOException
	{
		execute((jedis) -> {
			jedis.lrem(createUser2SessionIdKey(userId), 1, sessionId);
			jedis.del(createUser2SessionDetailsKey(userId, sessionId));
			return null;
		});
	}

	@Override
	public void updateLastAccessed(String userId, String sessionId) throws DAOException
	{
		execute((jedis) -> {
			jedis.hset(createUser2SessionDetailsKey(userId, sessionId), LAST_ACCESSED_TS, "" + System.currentTimeMillis());
			return null;
		});
	}

	@Override
	protected List<String> getAllUserSessionIds(String userId) throws DAOException
	{
		List<String> sessionIds = execute((jedis) -> {
			 return jedis.lrange(createUser2SessionIdKey(userId), 0, 100);
		});

		return sessionIds;
	}

	@Override
	protected SessionDetails getSessionDetails(String userId, String sessionId) throws DAOException
	{
		SessionDetails sessionDetails = execute((jedis) -> {
			Map<String, String> map = jedis.hgetAll(createUser2SessionDetailsKey(userId, sessionId));
			SessionDetails sd = createSessionDetails(map);
			return sd;
		});
		return sessionDetails;
	}
	
	@Override
	protected void removeAllExpiredSessionDetails(String userId) throws DAOException
	{
		List<String> sessionIds = getAllUserSessionIds(userId);
		if (sessionIds != null)
		{
			execute((jedis) -> {
				for (String sessionId : sessionIds)
				{
					SessionDetails sessionDetails = createSessionDetails(jedis.hgetAll(createUser2SessionDetailsKey(userId, sessionId)));
					
					if (!isSessionValid(sessionDetails.getLastAccessedTS()))
					{
						jedis.lrem(createUser2SessionIdKey(userId), 1, sessionId);
						jedis.del(createUser2SessionDetailsKey(userId, sessionId));
					}
				}
				return null;
			});
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
		SessionDetails sessionDetails = null;
		if (map != null && !map.isEmpty())
		{
			String userId = map.get(USER_ID);
			String sessionId = map.get(SESSION_ID);
			long createdTS = Long.parseLong(map.get(CREATED_TS));
			long lastAccessedTS = Long.parseLong(map.get(LAST_ACCESSED_TS));
			sessionDetails = new SessionDetails(userId, sessionId, createdTS, lastAccessedTS);
		}
		return sessionDetails;
	}
	
	private <R> R execute(BoundedOp<Jedis, R> op) throws DAOException
	{
		Jedis jedis = null;
		try
		{
			jedis = redisCache.getCache();
			return op.perform(jedis);
		}
		catch(Exception e)
		{
			throw new DAOException(e);
		}
		finally
		{
			if (jedis != null)
			{
				jedis.close();
			}
		}
	}
	
	private String createUser2SessionIdKey(String userId)
	{
		return String.format(REDIS_USER2SESSION_ID, userId);
	}
	
	private String createUser2SessionDetailsKey(String userId, String sessionId)
	{
		return String.format(REDIS_USER2SESSION_DETAILS, userId, sessionId);
	}
}
