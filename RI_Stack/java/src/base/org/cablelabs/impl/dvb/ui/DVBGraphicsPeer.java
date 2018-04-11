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

package org.cablelabs.impl.dvb.ui;

/**
 * Peer interface providing platform-specific implementation of
 * <code>DVBGraphics</code> native peer.
 * <p>
 * This interface provides definitions for the Porter-Duff alpha composition
 * rules that are compatible with those defined by
 * <code>DVBAlphaComposite</code>.
 * <p>
 * This interface should be defined by the <i>native</i> AWT Graphics
 * implementation.
 * 
 * @see DVBGraphicsImpl
 * @author Aaron Kamienski
 */
public interface DVBGraphicsPeer
{
    /**
     * Porter-Duff Clear rule. Both the color and the alpha of the destination
     * are cleared. Neither the source nor the destination is used as input.
     *<p>
     * Fs = 0 and Fd = 0, thus:
     * 
     * <pre>
     * 	cn = 0
     * 	An = 0
     *  Cn = 0
     *</pre>
     * 
     * <img src="CLEAR.jpg">
     * <p>
     * <b>Note that this operation is a fast drawing operation</b> This
     * operation is the same as using a source with alpha= 0 and the SRC rule
     * 
     */
    public static final int CLEAR = 1;

    /**
     * Porter-Duff Source rule. The source is copied to the destination. The
     * destination is not used as input.
     *<p>
     * Fs = 1 and Fd = 0, thus:
     * 
     * <pre>
     * 	cn = (As*Ar)*Cs
     * 	An = As*Ar
     *  Cn = Cs
     *</pre>
     * 
     * <img src="SRC.jpg">
     * <p>
     * <b>Note that this is a fast drawing routine</b>
     */
    public static final int SRC = 2;

    /**
     * Porter-Duff Source Over Destination rule. The source is composited over
     * the destination.
     *<p>
     * Fs = 1 and Fd = (1-(As*Ar)), thus:
     * 
     * <pre>
     * 	cn = (As*Ar)*Cs + Ad*Cd*(1-(As*Ar))
     * 	An = (As*Ar) + Ad*(1-(As*Ar))
     *</pre>
     * 
     * <img src="SRC_OVER.jpg">
     * <p>
     * <b>Note that this can be a very slow drawing operation</b>
     */
    public static final int SRC_OVER = 3;

    /**
     * Porter-Duff Destination Over Source rule. The destination is composited
     * over the source and the result replaces the destination.
     *<p>
     * Fs = (1-Ad) and Fd = 1, thus:
     * 
     * <pre>
     * 	cn = (As*Ar)*Cs*(1-Ad) + Ad*Cd
     * 	An = (As*Ar)*(1-Ad) + Ad
     *</pre>
     * 
     * <img src="DST_OVER.jpg">
     * <p>
     * <b>Note that this can be a very slow drawing operation</b>
     */
    public static final int DST_OVER = 4;

    /**
     * Porter-Duff Source In Destination rule. The part of the source lying
     * inside of the destination replaces the destination.
     *<p>
     * Fs = Ad and Fd = 0, thus:
     * 
     * <pre>
     * 	cn = (As*Ar)*Cs*Ad
     * 	An = (As*Ar)*Ad
     *  Cn = Cs
     *</pre>
     * 
     * <img src="SRC_IN.jpg">
     * <p>
     * <b>Note that this operation is faster than e.g. SRC_OVER but slower then
     * SRC</b>
     */
    public static final int SRC_IN = 5;

    /**
     * Porter-Duff Destination In Source rule. The part of the destination lying
     * inside of the source replaces the destination.
     *<p>
     * Fs = 0 and Fd = (As*Ar), thus:
     * 
     * <pre>
     * 	cn = Ad*Cd*(As*Ar)
     * 	An = Ad*(As*Ar)
     *  Cn = Cd
     *</pre>
     * 
     * <img src="DST_IN.jpg">
     * <p>
     * <b>Note that this operation is faster than e.g. SRC_OVER but slower than
     * SRC</b>
     */
    public static final int DST_IN = 6;

    /**
     * Porter-Duff Source Held Out By Destination rule. The part of the source
     * lying outside of the destination replaces the destination.
     *<p>
     * Fs = (1-Ad) and Fd = 0, thus:
     * 
     * <pre>
     * 	cn = (As*Ar)*Cs*(1-Ad)
     * 	An = (As*Ar)*(1-Ad)
     *  Cn = Cs
     *</pre>
     * 
     * <img src="SRC_OUT.jpg">
     * <p>
     * <b>Note that this operation is faster than e.g. SRC_OVER but slower than
     * SRC</b>
     */
    public static final int SRC_OUT = 7;

    /**
     * Porter-Duff Destination Held Out By Source rule. The part of the
     * destination lying outside of the source replaces the destination.
     *<p>
     * Fs = 0 and Fd = (1-(As*Ar)), thus:
     * 
     * <pre>
     * 	cn = Ad*Cd*(1-(As*Ar))
     * 	An = Ad*(1-(As*Ar))
     *  Cn = Cd
     *</pre>
     * 
     * <img src="DST_OUT.jpg">
     * <p>
     * <b>Note that this operation is faster than e.g. SRC_OVER but slower than
     * SRC</b>
     */
    public static final int DST_OUT = 8;

    /**
     * Returns the available peer composition modes.
     */
    public int[] getAvailablePeerCompositeRules();

    /**
     * Sets the peer composition mode.
     */
    public void setPeerComposite(int rule, float alpha);

    /**
     * Returns the peer composition rule.
     */
    public int getPeerCompositeRule();

    /**
     * Returns the peer composition alpha modulation constant.
     */
    public float getPeerCompositeAlpha();
}
