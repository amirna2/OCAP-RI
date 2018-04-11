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

/*
 * Copyright 2000-2003 by HAVi, Inc. Java is a trademark of Sun
 * Microsystems, Inc. All rights reserved.  
 */

package org.havi.ui.event;

/**
 * This event informs an application that a loading operation for an
 * {@link org.havi.ui.HBackgroundImage HBackgroundImage} has finished.
 * 
 * <hr>
 * The parameters to the constructors are as follows, in cases where parameters
 * are not used, then the constructor should use the default values.
 * <p>
 * <h3>Default parameter values exposed in the constructors</h3>
 * <table border>
 * <tr>
 * <th>Parameter</th>
 * <th>Description</th>
 * <th>Default value</th>
 * <th>Set method</th>
 * <th>Get method</th>
 * </tr>
 * <tr>
 * <td colspan=5>None.</td>
 * </tr>
 * </table>
 * <h3>Default parameter values not exposed in the constructors</h3>
 * <table border>
 * <tr>
 * <th>Description</th>
 * <th>Default value</th>
 * <th>Set method</th>
 * <th>Get method</th>
 * </tr>
 * <tr>
 * <td colspan=4>None.</td>
 * </tr>
 * </table>
 * 
 * @author Aaron Kamienski
 */

public class HBackgroundImageEvent extends java.util.EventObject
{
    /**
     * Marks the first integer for the range of background image events
     */
    public static final int BACKGROUNDIMAGE_FIRST = 1;

    /**
     * The loading succeeded
     */
    public static final int BACKGROUNDIMAGE_LOADED = 1;

    /**
     * The loading failed before attempting to load any data from the file. e.g.
     * the file not existing or due to a badly formed or otherwise broken
     * filename
     */
    public static final int BACKGROUNDIMAGE_FILE_NOT_FOUND = 2;

    /**
     * The loading failed due to an error while loading the data. e.g. the file
     * is not accessible or loading of it was interrupted
     */
    public static final int BACKGROUNDIMAGE_IOERROR = 3;

    /**
     * The loading failed because the data loaded is not valid. e.g. not a
     * supported coding format for background images.
     */
    public static final int BACKGROUNDIMAGE_INVALID = 4;

    /**
     * Marks the last integer for the range of background image events
     */
    public static final int BACKGROUNDIMAGE_LAST = 4;

    /**
     * Constructs a new {@link org.havi.ui.event.HBackgroundImageEvent
     * HBackgroundImageEvent}.
     * 
     * @param source
     *            the {@link org.havi.ui.HBackgroundImage HBackgroundImage}
     *            which has been loaded.
     * @param id
     *            the type of event (one of
     *            {@link org.havi.ui.event.HBackgroundImageEvent#BACKGROUNDIMAGE_LOADED
     *            BACKGROUNDIMAGE_LOADED},
     *            {@link org.havi.ui.event.HBackgroundImageEvent#BACKGROUNDIMAGE_FILE_NOT_FOUND
     *            BACKGROUNDIMAGE_FILE_NOT_FOUND},
     *            {@link org.havi.ui.event.HBackgroundImageEvent#BACKGROUNDIMAGE_IOERROR
     *            BACKGROUNDIMAGE_IOERROR} or
     *            {@link org.havi.ui.event.HBackgroundImageEvent#BACKGROUNDIMAGE_INVALID
     *            BACKGROUNDIMAGE_INVALID}).
     */
    public HBackgroundImageEvent(java.lang.Object source, int id)
    {
        super(source);
        this.id = id;
    }

    /**
     * Returns the {@link org.havi.ui.HBackgroundImage HBackgroundImage} for
     * which the data has been loaded.
     * 
     * @return the object which has been loaded.
     */
    public java.lang.Object getSource()
    {
        return super.getSource();
    }

    /**
     * Returns the type for this event.
     * 
     * @return the event type (one of
     *         {@link org.havi.ui.event.HBackgroundImageEvent#BACKGROUNDIMAGE_LOADED
     *         BACKGROUNDIMAGE_LOADED},
     *         {@link org.havi.ui.event.HBackgroundImageEvent#BACKGROUNDIMAGE_FILE_NOT_FOUND
     *         BACKGROUNDIMAGE_FILE_NOT_FOUND},
     *         {@link org.havi.ui.event.HBackgroundImageEvent#BACKGROUNDIMAGE_IOERROR
     *         BACKGROUNDIMAGE_IOERROR} or
     *         {@link org.havi.ui.event.HBackgroundImageEvent#BACKGROUNDIMAGE_INVALID
     *         BACKGROUNDIMAGE_INVALID}).
     */
    public int getID()
    {
        return id;
    }

    /**
     * The ID of this event.
     */
    private int id;
}
