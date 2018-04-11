// COPYRIGHT_BEGIN
//  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
//  
//  Copyright (C) 2008-2013, Cable Television Laboratories, Inc. 
//  
//  This software is available under multiple licenses: 
//  
//  (1) BSD 2-clause 
//   Redistribution and use in source and binary forms, with or without modification, are
//   permitted provided that the following conditions are met:
//        ·Redistributions of source code must retain the above copyright notice, this list 
//             of conditions and the following disclaimer.
//        ·Redistributions in binary form must reproduce the above copyright notice, this list of conditions 
//             and the following disclaimer in the documentation and/or other materials provided with the 
//             distribution.
//   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
//   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED 
//   TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A 
//   PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT 
//   HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
//   SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
//   LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
//   DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
//   THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
//   (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF 
//   THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//  
//  (2) GPL Version 2
//   This program is free software; you can redistribute it and/or modify
//   it under the terms of the GNU General Public License as published by
//   the Free Software Foundation, version 2. This program is distributed
//   in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
//   even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
//   PURPOSE. See the GNU General Public License for more details.
//  
//   You should have received a copy of the GNU General Public License along
//   with this program.If not, see<http:www.gnu.org/licenses/>.
//  
//  (3)CableLabs License
//   If you or the company you represent has a separate agreement with CableLabs
//   concerning the use of this code, your rights and obligations with respect
//   to this code shall be as set forth therein. No license is granted hereunder
//   for any other purpose.
//  
//   Please contact CableLabs if you need additional information or 
//   have any questions.
//  
//       CableLabs
//       858 Coal Creek Cir
//       Louisville, CO 80027-9750
//       303 661-9100
// COPYRIGHT_END
package org.cablelabs.xlet.providerregistry;

import java.rmi.Remote;
import java.rmi.RemoteException;

import javax.tv.service.SIManager;
import javax.tv.service.Service;
import javax.tv.service.navigation.ServiceList;
import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import org.dvb.io.ixc.IxcRegistry;

import org.cablelabs.xlet.providerregistry.ServiceProviderTestXlet.TestProxyInterface;

/**
 * This xlets looks for services that are provided through a ServiceProvider
 * which has been registered in a different xlet
 */
public class CheckForProvidedServicesXlet implements Xlet
{
    String targetLocator;

    XletContext context;

    public void destroyXlet(boolean unconditional) throws XletStateChangeException
    {
    }

    public void initXlet(XletContext ctx) throws XletStateChangeException
    {

        context = ctx;
    }

    public void pauseXlet()
    {
    }

    public void startXlet() throws XletStateChangeException
    {
        String[] args = (String[]) (context.getXletProperty("dvb.caller.parameters"));
        if (args.length < 1) throw new XletStateChangeException("Target locator string was not specified");
        targetLocator = args[0];

        ServiceList list = SIManager.createInstance().filterServices(null);
        TestProxyInterface test = lookupTest();
        if (test == null)
        {
            throw new XletStateChangeException("Could not load test object from IXC");
        }

        Service targetService = null;
        for (int i = 0; i < list.size(); i++)
        {
            Service testService = list.getService(i);
            if (testService.getLocator().toString().equals(targetLocator))
            {
                targetService = testService;
                break;
            }
        }

        try
        {
            test.assertTrue("Could not find service from ServiceProvider " + "installed in another xlet",
                    targetService != null);

        }
        catch (RemoteException exc)
        {
            throw new XletStateChangeException("Could not call assert on TestProxy");
        }

    }

    private TestProxyInterface lookupTest() throws XletStateChangeException
    {
        try
        {
            ServiceProviderTestXlet.listIXC(context);

            Remote remoteObject = IxcRegistry.lookup(context, "/1/7007/Test");
            return (TestProxyInterface) remoteObject;
        }
        catch (Exception exc)
        {
            exc.printStackTrace();
            throw new XletStateChangeException("Could not load test object" + exc);
        }
    }
}
