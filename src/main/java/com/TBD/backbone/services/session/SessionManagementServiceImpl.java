package com.TBD.backbone.services.session;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * SessionIdProvider will be providing SessionId during RequestCreation.
 * SessionId will be part of Request which is Service level Request.
 * There are two types of request which end up with a Service.
 * 1. Which originate from another service.
 * 2. Which originate from client.
 * 
 * 1. When from another service the issue is the calling service is not authenticated
 * so the calling service when it creates a request does not have sessionId populated.
 * Somehow the service needs to allow for serice to service calls which do not originate 
 * from request endpooint code.
 * 
 * One point to remember is : If Service A calls during it's creation / initialization
 * Service B. Now Service A does not have a sessionId, however when it calls the Service B
 * PREDETERMINED-sessionId is passed to it and that gets copied to RequestProcessor(Thread) 
 * which also implements SessionIdProvider.
 * 
 * 
 * Also services can be 
 * 1. Those that require Calers be authenticated.
 * 2. Those that do need Callers to be authenticated.
 *
 */
public class SessionManagementServiceImpl implements SessionManagementService
{
	private Map<String, SessionDetails> clientSessionMap = null;
	
	public SessionManagementServiceImpl() throws Exception
	{
		clientSessionMap = new HashMap<>();	
	}

	@Override
	public SessionDetails register(String clientId) throws SessionManagementException
	{
		String sessionId = UUID.randomUUID().toString();
		SessionDetails sessionDetails = new SessionDetails(sessionId);
		clientSessionMap.put(clientId, sessionDetails);
		
		return sessionDetails;
	}

	@Override
	public SessionDetails getSessionDetails(String clientId) throws SessionManagementException
	{
		return clientSessionMap.get(clientId);
	}

	@Override
	public void unregister(String clientId) throws SessionManagementException
	{
		clientSessionMap.remove(clientId);
	}

	@Override
	public boolean isValid(String clientId, String sessionId) throws SessionManagementException
	{
		boolean valid = false;
		SessionDetails details = clientSessionMap.get(clientId);
		if (details != null && details.getSessionId().equals(sessionId))
		{
			valid = true;
		}
		System.out.println("ClientId = " + clientId + " SessionId=" + sessionId);
		if (sessionId != null && sessionId.indexOf("TODOSessionId") != -1)
		{
			valid = true;
		}
		return valid;
	}
}
