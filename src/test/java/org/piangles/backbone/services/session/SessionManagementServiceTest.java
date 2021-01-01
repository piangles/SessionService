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
