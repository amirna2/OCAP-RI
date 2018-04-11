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

package org.cablelabs.impl.manager.environment;

import javax.tv.service.Service;

import org.havi.ui.HScreen;
import org.havi.ui.HScene;
import org.ocap.environment.EnvironmentState;
import org.ocap.system.MonitorAppPermission;

import org.cablelabs.impl.havi.DisplayMediator;
import org.cablelabs.impl.havi.ExtendedGraphicsDevice;
import org.cablelabs.impl.havi.HaviToolkit;
import org.cablelabs.impl.havi.port.mpe.HDScreen;
import org.cablelabs.impl.manager.AppDomain;
import org.cablelabs.impl.manager.EnvironmentManager;
import org.cablelabs.impl.manager.FocusManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.FocusManager.FocusContext;
import org.cablelabs.impl.manager.focus.FocusManagerImpl;
import org.cablelabs.impl.signalling.AppEntry;
import org.cablelabs.impl.util.SecurityUtil;
import java.awt.KeyboardFocusManager;

public class OcapEnvironment extends EnvironmentImplBase
{
    EnvironmentState state = EnvironmentState.SELECTED; // By default, this is
                                                        // selected.

    EnvironmentManager evm = null; // (EnvironmentManager)
                                   // ManagerManager.getInstance(EnvironmentManager.class);

    public OcapEnvironment(EnvironmentManager envMgr)
    {
        evm = envMgr;
    }

    public void setState(EnvironmentState newState)
    {
        // Send an event that we're transitioning.
        sendStateEvent(state, newState);

        // Do stuff based on the new state.

        // TODO: really need a bit more of a state machine which is smart
        // about old -> new state transitions. This is quick fix for CTP
        // test pass

        if (newState == EnvironmentState.BACKGROUND)
        {

            // remove focus from currently active focus context
            FocusManager fm = (FocusManager) ManagerManager.getInstance(FocusManager.class);
            if (fm != null)
            {
                FocusContext actFC = fm.getFocusOwnerContext();
                /*
                 * if ( actFC != null ) { //if(geom instanceof Polygon) HScene
                 * hs = (HScene)actFC; hs.setVisible(false); }
                 */

                fm.suspendFocus();
                // FocusContext actFC = fm.getFocusOwnerContext();
                // if ( actFC != null ) {
                // actFC.notifyDeactivated();
            }

            // make all HScenes non-visible
            HaviToolkit.getToolkit().setGraphicsVisible(false);
        }
        else if (newState == EnvironmentState.SELECTED)
        {
            FocusManager fm = (FocusManager) ManagerManager.getInstance(FocusManager.class);
            if (fm != null)
            {
                fm.restoreFocus();
                FocusContext actFC = fm.getFocusOwnerContext();
                if (actFC != null)
                {
                    // if(geom instanceof Polygon)
                    HScene hs = (HScene) actFC;
                    hs.setVisible(true);
                    hs.repaint();
                }

                // FocusContext actFC = fm.getFocusOwnerContext();
                // if ( actFC != null ) {
                // actFC.notifyDeactivated();
            }

            // Make all HScenes visible
            // TODO: need to eventually make only those HScenes visible that
            // were visible prior to leaving presenting state re-visible
            HaviToolkit.getToolkit().setGraphicsVisible(true);
        }

        state = newState;
    }

    public void deselect()
    {
        SecurityUtil.checkPermission(new MonitorAppPermission("environment.selection"));
        evm.deselectEnvironment(this);
    }

    public void select()
    {
        SecurityUtil.checkPermission(new MonitorAppPermission("environment.selection"));
        evm.selectEnvironment(this);
    }

    public EnvironmentState getState()
    {
        return state;
    }
}
