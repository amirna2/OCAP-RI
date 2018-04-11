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
package org.dvb.spi.selection;

import org.davic.net.InvalidLocatorException;
import org.davic.net.Locator;
import org.ocap.net.OcapLocator;

/**
 * CannedSelectionSession
 * 
 * @author Joshua Keplinger
 * 
 */
public class MockSelectionSession implements SelectionSession
{

    public static final int IDLE = 1;

    public static final int SELECTING = 2;

    public static final int SELECTED = 3;

    public static final int DESTROYED = 4;

    int state = IDLE;

    MockSelectionProvider provider;

    ServiceReference service;

    long position = 0L;

    float rate = 1.0f;

    public MockSelectionSession(MockSelectionProvider provider, ServiceReference service)
    {
        this.provider = provider;
        this.service = service;

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.dvb.spi.selection.SelectionSession#destroy()
     */
    public void destroy()
    {
        if (state == DESTROYED) throw new IllegalStateException("SelectionSession already destroyed");
        state = DESTROYED;
        service = null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.dvb.spi.selection.SelectionSession#select()
     */
    public Locator select()
    {
        checkState(IDLE);
        state = SELECTING;
        // These values refer to CannedSIDatabase.transportStream7 and
        // serviceDetails33.
        // This should probably be changed to a more flexible Service mapping
        // routine.
        int freq = 5000;
        int prog = 2;
        int mod = 1;
        OcapLocator locator = null;
        try
        {
            locator = new OcapLocator(freq, prog, mod);
        }
        catch (InvalidLocatorException ex)
        {
            return null;
        }
        if (!(service instanceof KnownServiceReference))
        {
            KnownServiceReference newservice = new KnownServiceReference(service.getServiceIdentifier(),
                    service.getServiceIdentifier(), locator);
            ServiceDescription[] descs = provider.getServiceDescriptions(new ServiceReference[] { service });
            provider.cannedUpdateServiceReference(service, newservice, descs[0]);
            service = newservice;
        }
        return (Locator) ((KnownServiceReference) service).getActualLocation();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.dvb.spi.selection.SelectionSession#selectionReady()
     */
    public void selectionReady()
    {
        checkState(SELECTING);
        state = SELECTED;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.dvb.spi.selection.SelectionSession#setPosition(long)
     */
    public long setPosition(long position)
    {
        if (state == DESTROYED) throw new IllegalStateException("SelectionSession already destroyed");

        return this.position = position;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.dvb.spi.selection.SelectionSession#setRate(float)
     */
    public float setRate(float newRate)
    {
        if (state == DESTROYED) throw new IllegalStateException("SelectionSession already destroyed");

        return this.rate = newRate;
    }

    public int cannedGetState()
    {
        return state;
    }

    private void checkState(int expected)
    {
        if (state != expected)
            throw new IllegalStateException("Session state was not " + stateToString(expected) + ", instead was "
                    + stateToString(state));
    }

    private String stateToString(int state)
    {
        switch (state)
        {
            case IDLE:
                return "IDLE";
            case SELECTING:
                return "SELECTING";
            case SELECTED:
                return "SELECTED";
            case DESTROYED:
                return "DESTROY";
            default:
                return "UNKNOWN";
        }
    }
}
