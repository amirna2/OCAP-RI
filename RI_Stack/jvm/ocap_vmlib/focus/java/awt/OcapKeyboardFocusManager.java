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

package java.awt;

import java.awt.event.KeyEvent;
import java.beans.PropertyChangeListener;
import java.beans.VetoableChangeListener;
import java.util.Set;

class OcapKeyboardFocusManager extends java.awt.KeyboardFocusManager
{
    public OcapKeyboardFocusManager(org.cablelabs.impl.awt.KeyboardFocusManager kfm)
    {
        ocapKFM = kfm;
    }

    private org.cablelabs.impl.awt.KeyboardFocusManager ocapKFM;

    /**
     * All of the following calls are simply passed on to the KFM implementation
     * object
     */

    boolean requestFocus(Component c, boolean temporary)
    {
        return ocapKFM.requestFocus(c, temporary);
    }

    public Component getFocusOwner()
    {
        return ocapKFM.getFocusOwner();
    }

    public void clearGlobalFocusOwner()
    {
        ocapKFM.clearGlobalFocusOwner();
    }

    public Component getPermanentFocusOwner()
    {
        return ocapKFM.getPermanentFocusOwner();
    }

    public FocusTraversalPolicy getDefaultFocusTraversalPolicy()
    {
        return ocapKFM.getDefaultFocusTraversalPolicy();
    }

    public void setDefaultFocusTraversalPolicy(FocusTraversalPolicy policy)
    {
        ocapKFM.setDefaultFocusTraversalPolicy(policy);
    }

    public Set getDefaultFocusTraversalKeys(int id)
    {
        return ocapKFM.getDefaultFocusTraversalKeys(id);
    }

    public void setDefaultFocusTraversalKeys(int id, Set keystrokes)
    {
        ocapKFM.setDefaultFocusTraversalKeys(id, keystrokes);
    }

    public Container getCurrentFocusCycleRoot()
    {
        return ocapKFM.getCurrentFocusCycleRoot();
    }

    public void addPropertyChangeListener(PropertyChangeListener listener)
    {
        ocapKFM.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener)
    {
        ocapKFM.removePropertyChangeListener(listener);
    }

    public void addVetoableChangeListener(VetoableChangeListener listener)
    {
        ocapKFM.addVetoableChangeListener(listener);
    }

    public void removeVetoableChangeListener(VetoableChangeListener listener)
    {
        ocapKFM.removeVetoableChangeListener(listener);
    }

    public void addKeyEventDispatcher(KeyEventDispatcher dispatcher)
    {
        ocapKFM.addKeyEventDispatcher(dispatcher);
    }

    public void removeKeyEventDispatcher(KeyEventDispatcher dispatcher)
    {
        ocapKFM.removeKeyEventDispatcher(dispatcher);
    }

    public void addKeyEventPostProcessor(KeyEventPostProcessor processor)
    {
        ocapKFM.addKeyEventPostProcessor(processor);
    }

    public void removeKeyEventPostProcessor(KeyEventPostProcessor processor)
    {
        ocapKFM.removeKeyEventPostProcessor(processor);
    }

    public boolean dispatchEvent(AWTEvent e)
    {
        return ocapKFM.dispatchEvent(e);
    }

    public boolean dispatchKeyEvent(KeyEvent e)
    {
        return ocapKFM.dispatchKeyEvent(e);
    }

    public boolean postProcessKeyEvent(KeyEvent e)
    {
        return ocapKFM.postProcessKeyEvent(e);
    }

    public void processKeyEvent(Component focusedComponent, KeyEvent e)
    {
        ocapKFM.processKeyEvent(focusedComponent, e);
    }

    public void focusNextComponent(Component c)
    {
        ocapKFM.focusNextComponent(c);
    }

    public void focusPreviousComponent(Component c)
    {
        ocapKFM.focusPreviousComponent(c);
    }

    public void upFocusCycle(Component c)
    {
        ocapKFM.upFocusCycle(c);
    }

    public void downFocusCycle(Container c)
    {
        ocapKFM.downFocusCycle(c);
    }

    /**
     * We do not support "global" or cross-app access to information, so these
     * calls return their non-global equivalent
     */

    public Component getGlobalFocusOwner()
    {
        return ocapKFM.getFocusOwner();
    }

    public Container getGlobalCurrentFocusCycleRoot()
    {
        return ocapKFM.getCurrentFocusCycleRoot();
    }

    public Component getGlobalPermanentFocusOwner()
    {
        return ocapKFM.getPermanentFocusOwner();
    }

    /**
     * These operations are not permitted and/or supported
     */

    public void setGlobalPermanentFocusOwner(Component focusOwner)
    {
    }

    public void setGlobalFocusOwner(Component focusOwner)
    {
    }

    public void setGlobalCurrentFocusCycleRoot(Container focusCycleRoot)
    {
    }

    public Window getFocusedWindow()
    {
        return null;
    }

    public Window getGlobalFocusedWindow()
    {
        return null;
    }

    public void setGlobalFocusedWindow(Window focusedWindow)
    {
    }

    public Window getActiveWindow()
    {
        return null;
    }

    public Window getGlobalActiveWindow()
    {
        return null;
    }

    void dequeueKeyEvents(long after, Component untilFocused)
    {
    }

    void discardKeyEvents(Component comp)
    {
    }

    void enqueueKeyEvents(long after, Component untilFocused)
    {
    }
}
