package org.cablelabs.xlet.TCKAgentTuner;

import javax.tv.locator.InvalidLocatorException;
import javax.tv.service.SIManager;
import javax.tv.service.Service;
import javax.tv.service.navigation.ServiceFilter;
import javax.tv.service.navigation.ServiceIterator;
import javax.tv.service.navigation.ServiceList;
import javax.tv.service.selection.InsufficientResourcesException;
import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceContextEvent;
import javax.tv.service.selection.ServiceContextFactory;
import javax.tv.service.selection.ServiceContextListener;
import javax.tv.service.selection.ServiceContentHandler;
import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import javax.tv.locator.LocatorFactory;


public class TCKAgentTunerXlet implements Xlet
{
    private ServiceContext sc;
    private Service s;

    /**
     * initilize xlet
     */
    public void initXlet(XletContext xletContext) throws XletStateChangeException
    {
    	try
    	{
    		sc = ServiceContextFactory.getInstance().createServiceContext();
    		s = SIManager.createInstance().getService(LocatorFactory.getInstance().createLocator("ocap://0x45A")); 
    	}
    	catch (Throwable t)
    	{
    		System.err.println("Exception during init: " + t.getMessage());
    		t.printStackTrace();
    	}
    }

    /**
     * start the xlet
     */
    public void startXlet() throws XletStateChangeException
    {
    	try
    	{
    		sc.select(s);
    	}
    	catch (Throwable t)
    	{
    		System.err.println("Exception during start: " + t.getMessage());
    		t.printStackTrace();
    	}
    }

    /**
     * pause the xlet
     */
    public void pauseXlet()
    {

    }

    /**
     * destroy the xlet
     */
    public void destroyXlet(boolean b) throws XletStateChangeException
    {

    }


}
