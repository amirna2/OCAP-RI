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

package javax.media;

import java.awt.Component;

/**
 * <code>CachingControl</code> is an interface supported by <code>Players</code>
 * that are capable of reporting download progress. Typically, this control is
 * accessed through the <code>Controller.getControls</code> method.
 * 
 * A <code>Controller</code> that supports this control will post
 * <code>CachingControlEvents</code> often enough to support the implementation
 * of custom progress GUIs.
 * 
 * @see Controller
 * @see ControllerListener
 * @see CachingControlEvent
 * @see Player
 * @version 1.18, 97/08/25.
 */

public interface CachingControl extends Control
{

    /**
     * Use to indicate that the <CODE>CachingControl</CODE> doesn't know how
     * long the content is.
     * <p>
     * The definition is: LENGTH_UNKNOWN == Long.MAX_VALUE
     */
    public final static long LENGTH_UNKNOWN = Long.MAX_VALUE;

    /**
     * Check whether or not media is being downloaded.
     * 
     * @return Returns <CODE>true</CODE> if media is being downloaded; otherwise
     *         returns <CODE>false</CODE>. .
     */
    public boolean isDownloading();

    /**
     * Get the total number of bytes in the media being downloaded. Returns
     * <code>LENGTH_UNKNOWN</code> if this information is not available.
     * 
     * @return The media length in bytes, or <code>LENGTH_UNKNOWN</code>.
     */
    public long getContentLength();

    /**
     * Get the total number of bytes of media data that have been downloaded so
     * far.
     * 
     * @return The number of bytes downloaded.
     */
    public long getContentProgress();

    /**
     * Get a <CODE>Component</CODE> for displaying the download progress.
     * 
     * @return Progress bar GUI.
     */
    public Component getProgressBarComponent();

    /**
     * Get a <CODE>Component</CODE> that provides additional download control.
     * 
     * Returns <CODE>null</CODE> if only a progress bar is provided.
     * 
     * @return Download control GUI.
     */
    public Component getControlComponent();
}

/* 
 * ***** EDITOR CONTROL STRINGS ***** Local Variables: tab-width: 8
 * c-basic-offset: 4 indent-tabs-mode: t End: vi:set ts=8 sw=4:
 * *********************************
 */
