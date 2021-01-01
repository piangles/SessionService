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

import org.piangles.core.services.remoting.AbstractContainer;
import org.piangles.core.services.remoting.ContainerException;

public class SessionManagementServiceContainer extends AbstractContainer
{
	public static void main(String[] args)
	{
		SessionManagementServiceContainer container = new SessionManagementServiceContainer(args);
		try
		{
			container.performSteps(args);
		}
		catch (ContainerException e)
		{
			//TODO Removed email it should be externally monitored.
			//EmailSupport.notify(e, e.getMessage());
			//TODO Log the service could not come up here System.err.Println("")
			//Log scrappers / Process monitor will pick it up.
			System.exit(-1);
		}
	}

	public SessionManagementServiceContainer(String[] args)
	{
		super(SessionManagementService.NAME);
	}
	
	@Override
	protected Object createServiceImpl() throws ContainerException
	{
		Object service = null;
		try
		{
			service = new SessionManagementServiceImpl();
		}
		catch (Exception e)
		{
			throw new ContainerException(e);
		}
		return service;
	}
}
