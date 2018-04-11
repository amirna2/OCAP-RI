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

package org.cablelabs.xlet.monappautotest;

import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import org.dvb.application.AppAttributes;
import org.dvb.application.AppID;
import org.dvb.application.AppsDatabase;
import org.dvb.application.AppStateChangeEvent;
import org.dvb.io.ixc.IxcRegistry;
import org.ocap.application.AppFilter;
import org.ocap.application.OcapAppAttributes;
import org.ocap.application.PermissionInformation;
import org.ocap.net.OcapLocator;
import org.ocap.resource.ResourceUsage;

public class MonAppAutoTestXlet implements Xlet
{

    // Data
    AppID m_appID;

    XletContext m_ctx;

    String m_xletName;

    MonAppAutoTestIxc m_eventHandler;

    int m_XletNum;

    int m_TestNum;

    int m_StepNum;

    public void destroyXlet(boolean unconditional) throws XletStateChangeException
    {
        // TODO Auto-generated method stub

    }

    public void initXlet(XletContext ctx) throws XletStateChangeException
    {

        // Store this xlet's name and context
        m_appID = new AppID((int) (Long.parseLong((String) (ctx.getXletProperty("dvb.org.id")), 16)),
                (int) (Long.parseLong((String) (ctx.getXletProperty("dvb.app.id")), 16)));
        m_xletName = AppsDatabase.getAppsDatabase().getAppAttributes(m_appID).getName();
        m_ctx = ctx;
    }

    public void pauseXlet()
    {
        // TODO Auto-generated method stub

    }

    public void startXlet() throws XletStateChangeException
    {

        // /////////////////////////////////////////////////////////////////////
        // //
        // Get the args we were started with. Apparently, the method of //
        // getting args in initXlet using //
        // ctx.getXletProperty(XletContext.ARGS) only gets args that come //
        // from hostapp.properties. When an Xlet is started by another Xlet, //
        // args aren't available until startXlet and are retrieved as shown. //
        // At least so sayeth Josh. //
        // //
        // /////////////////////////////////////////////////////////////////////

        String[] argList = new String[4];

        try
        {
            argList = ((String[]) (m_ctx.getXletProperty("dvb.caller.parameters")));

            // Parse the individual appID and orgID from the 48-bit int
            long orgIDappID = Long.parseLong(argList[0], 16);
            int oID = (int) ((orgIDappID >> 16) & 0xFFFFFFFF);
            int aID = (int) (orgIDappID & 0xFFFF);

            m_eventHandler = (MonAppAutoTestIxc) (IxcRegistry.lookup(m_ctx, "/" + Integer.toHexString(oID) + "/"
                    + Integer.toHexString(aID) + "/MonAppAutoTestIxc"));

            try
            {
                m_XletNum = Integer.parseInt(argList[1]);
            }
            catch (Exception e)
            {
            }
            try
            {
                m_TestNum = Integer.parseInt(argList[2]);
            }
            catch (Exception e)
            {
            }
            try
            {
                m_StepNum = Integer.parseInt(argList[3]);
            }
            catch (Exception e)
            {
            }
        }
        catch (Exception e)
        {
            throw new XletStateChangeException("Error setting up IXC communication with runner! -- " + e.getMessage());
        }

        // Do any tasks for this test and step.

        switch (m_TestNum)
        {
            // For the app filter tests, there's nothing more to do. So, die.
            case MonAppAutoTestConstants.APPDENY:
            case MonAppAutoTestConstants.APPALLOW:
            case MonAppAutoTestConstants.APPASK:
                break;

            case MonAppAutoTestConstants.REZDENYSECTION:
            case MonAppAutoTestConstants.REZDENYNIC:
            case MonAppAutoTestConstants.REZDENYBGDEV:
            case MonAppAutoTestConstants.REZDENYGFXDEV:
            case MonAppAutoTestConstants.REZDENYVIDEODEV:
            case MonAppAutoTestConstants.REZDENYVBI:
            case MonAppAutoTestConstants.REZALLOW:
            case MonAppAutoTestConstants.REZASK:
            case MonAppAutoTestConstants.REZCONTRELEASE:
            case MonAppAutoTestConstants.REZCONTDEFAULT:
            case MonAppAutoTestConstants.REZCONTALLDENY:
            case MonAppAutoTestConstants.REZCONTNEVER:
            case MonAppAutoTestConstants.REZCONTALWAYS:
            case MonAppAutoTestConstants.PERMADD:
            case MonAppAutoTestConstants.PERMDENY:

                // Launch thread to do these tests
                break;
            default:
                // Log an error
                break;
        }

    }
}
