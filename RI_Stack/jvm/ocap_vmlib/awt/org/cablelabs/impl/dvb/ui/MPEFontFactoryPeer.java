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

import java.awt.Font;

/**
 * Peer class providing platform-specific implementation of
 * <code>FontFactory</code> native peer.
 * 
 * @note This implementation is currently a placeholder. The real implementation
 *       is part of the AWT implementation. This will remain the case as long as
 *       MPE/JNI is not built in a VM-dependent manner. (I.e., once we build
 *       MPE/JNI specific to a VM, the entire implementation could go here, with
 *       differences in native code... or the FontFactoryPeer could be gotten
 *       ride of altogether, with differences in native code.)
 */
public class MPEFontFactoryPeer implements FontFactoryPeer
{
    /**
     * Create a new <code>FontFactory</code> native peer.
     */
    public MPEFontFactoryPeer()
    {
        ff = nCreate();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.dvb.ui.FontFactoryPeer2#addFont(byte[],
     * java.lang.String, int, int, int)
     */
    public int addFont(byte[] data, String name, int style, int minSize, int maxSize)
    {
        return nAddFont(ff, data, name, style, minSize, maxSize);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.dvb.ui.FontFactoryPeer2#newFont(java.awt.Font)
     */
    public Font newFont(Font f)
    {
        return nNewFont(ff, f);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.dvb.ui.FontFactoryPeer2#dispose()
     */
    public void dispose()
    {
        int tmp = ff;
        ff = 0;
        if (tmp != 0) nDispose(tmp);
    }

    /**
     * Finalizer.
     */
    protected void finalize()
    {
        dispose();
    }

    /**
     * Native peer.
     */
    private int ff;

    /**
     * Initializes the native implementation.
     */
    private static native void initJNI();

    /**
     * Creates the native peer for this <code>FontFactory</code>. Will throw any
     * appropriate runtime exceptions or errors if necessary.
     */
    private static native int nCreate();

    /**
     * Adds the given font description to the native peer font factory.
     * 
     * @param ff
     *            native font factory
     * @param data
     *            array of PFR font data
     * @param name
     *            name of font or <code>null</code> if it should be assumed from
     *            PFR font data; if <code>null</code> then <code>style</code>,
     *            <code>minSize</code>, and <code>maxSize</code> will be ignored
     * @param style
     *            style for which this font is to be used
     * @param minSize
     *            minimum size for which this font is to be used; use 0 for all
     *            sizes
     * @param maxSize
     *            maximum size for which this font is to be used; use 65535 for
     *            all sizes
     * 
     * @return non-zero if the font format is incorrect
     */
    private static native int nAddFont(int ff, byte[] data, String name, int style, int minSize, int maxSize);

    /**
     * Using the native peer for this <code>FontFactory</code>, create a new
     * font. Instantiates a new font using the native peer font factory and
     * performs any necessary tasks to provide the "glue" into the
     * java.awt.Toolkit implementation.
     * 
     * @param ff
     *            native font factory
     * @param f
     *            Font to create
     * 
     * @return a new instance of a font or null if the font is not available
     */
    private static native Font nNewFont(int ff, Font f);

    /**
     * Disposes of the given FontFactory.
     * 
     * @param ff
     *            native font factory
     */
    private static native void nDispose(int ff);

    static
    {
        // implied loading of AWT
        java.awt.Toolkit.getDefaultToolkit();
        // init class/method ids
        initJNI();
    }
}
