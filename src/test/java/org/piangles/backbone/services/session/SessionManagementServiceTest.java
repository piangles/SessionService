package org.piangles.backbone.services.session;

import org.piangles.backbone.services.Locator;

public class SessionManagementServiceTest
{
	public static void main(String[] args) throws Exception
	{
		SessionManagementService ss = Locator.getInstance().getSessionManagementService();
		System.out.println("Calling session management service....");
		boolean validty = ss.isValid("LoggingService", "TODOSessionId");
		System.out.println("Response for isValid: " + validty);
		System.exit(1);
	}
}
