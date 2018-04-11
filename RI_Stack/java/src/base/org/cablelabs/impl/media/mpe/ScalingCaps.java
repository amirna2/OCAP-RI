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
package org.cablelabs.impl.media.mpe;

import org.apache.log4j.Logger;
import org.cablelabs.impl.util.Arrays;

/**
 * @author jspruiel
 * 
 */
public class ScalingCaps
{
    public boolean isArbitrary()
    {
        if (supportsArbScaling == true)
        {
            if (log.isDebugEnabled())
            {
                log.debug("ScalingCaps: Arbitrary scaling factors supported; getHorz/getVert are nulls.");
            }
        }
            else
        {
            if (log.isDebugEnabled())
            {
                log.debug("ScalingCaps: Arbitrary scaling factors Not supported; getHorz/getVert are NOT null.");
            }
        }
        return supportsArbScaling;
    }

    public float[] getDiscreteHFactors()
    {
        return supportsArbScaling ? null : Arrays.copy(hScalingFactors);
    }

    public float[] getDiscreteVFactors()
    {
        return supportsArbScaling ? null : Arrays.copy(vScalingFactors);
    }

    public float[] getContinuousHorizRange()
    {
        return supportsArbScaling ? hScalingFactors : null;
    }

    public float[] getContinuousVertRange()
    {
        return supportsArbScaling ? vScalingFactors : null;
    }

    public void setIsArbitraryFlag(boolean supported)
    {
        if (log.isDebugEnabled())
        {
            log.debug("ScalingCaps:setIsArbitraryFlag - arbitraryScaling = " + supported);
        }

        supportsArbScaling = supported;
    }

    public void setHorizCaps(float[] factors)
    {
        if (log.isDebugEnabled())
        {
            log.debug("ScalingCaps:setHorizCaps - scaleFactors = " + factors.length);
        }
            for (int i = 0; i < factors.length; i++)
            {
            if (log.isDebugEnabled())
            {
                log.debug("ScalingCaps:setHorizCaps - scaleFactors[" + i + "] = " + factors[i]);
            }
        }

        hScalingFactors = Arrays.copy(factors);
    }

    public void setVertCaps(float[] factors)
    {
        if (log.isDebugEnabled())
        {
            log.debug("ScalingCaps:setVertCaps - scaleFactors = " + factors.length);
        }
            for (int i = 0; i < factors.length; i++)
            {
            if (log.isDebugEnabled())
            {
                log.debug("ScalingCaps:setHorizCaps - scaleFactors[" + i + "] = " + factors[i]);
            }
        }

        vScalingFactors = Arrays.copy(factors);
    }

    private static final Logger log = Logger.getLogger(ScalingCaps.class);

    // Flag indicating support for arbitrary scaling factors.
    protected boolean supportsArbScaling;

    // Arbitrary horizontal scaling factors
    protected float[] hScalingFactors;

    // Vertical scaling factors
    protected float[] vScalingFactors;
}
