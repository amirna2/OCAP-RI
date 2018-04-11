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

package org.cablelabs.impl.manager.lightweighttrigger;

import javax.tv.locator.Locator;

import org.davic.net.InvalidLocatorException;
import org.ocap.net.OcapLocator;

public class LightweightTriggerCarouselLocator extends OcapLocator
{
    public static final String ARTIFICIAL_CAROUSEL_ROOTPATH = "mtt:";

    public static final String LIGHTWEIGHT_TRIGGERS = "lightweight_triggers";

    private String url = null;

    private static final int DUMMY_SOURCEID = 0;

    private static int counter = 0;

    private int carouselId;

    /**
     * @throws InvalidLocatorException
     */
    LightweightTriggerCarouselLocator() throws InvalidLocatorException
    {
        super(DUMMY_SOURCEID);
        this.carouselId = getIncrementedCounter();
        this.url = ARTIFICIAL_CAROUSEL_ROOTPATH + carouselId;
    }

    /**
     * This constructor is called for carousel giving events while recording
     * playback.
     * 
     * @param carouselId
     * @throws InvalidLocatorException
     */
    LightweightTriggerCarouselLocator(Locator loc, int carouselId) throws InvalidLocatorException
    {
        super(carouselId);
        this.url = loc.toExternalForm() + ARTIFICIAL_CAROUSEL_ROOTPATH + carouselId;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.tv.locator.Locator#toExternalForm()
     */
    public String toExternalForm()
    {
        return url;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj)
    {
        return (obj instanceof Locator) && toString().equals(((Locator) obj).toString());
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    public int hashCode()
    {
        return url.hashCode();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ocap.net.OcapLocator#getSourceID()
     */
    public int getSourceID()
    {
        return -1;
    }

    /*
     * (non-Javadoc) Return the artifical carousel id
     */
    public int getCarouselId()
    {
        return carouselId;
    }

    private static synchronized int getIncrementedCounter()
    {
        return ++counter;
    }
}
