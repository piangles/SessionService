package com.TBD.backbone.services.session;

import com.TBD.core.email.EmailSupport;
import com.TBD.core.services.remoting.AbstractContainer;
import com.TBD.core.services.remoting.ContainerException;

public class SessionManagementServiceContainer extends AbstractContainer
{
	public static void main(String[] args)
	{
		SessionManagementServiceContainer container = new SessionManagementServiceContainer();
		try
		{
			container.performSteps();
		}
		catch (ContainerException e)
		{
			//EmailSupport.notify(e, e.getMessage());
			//TODO Log the service could not come up here System.err.Println("")
			//Log scrappers / Process monitor will pick it up.
			System.exit(-1);
		}
	}

	public SessionManagementServiceContainer()
	{
		super("SessionManagementService");
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
