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

import javax.media.protocol.DataSource;
import java.io.IOException;

/**
 * <code>MediaProxy</code> is a <code>MediaHandler</code> which processes
 * content from one <code>DataSource</code>, to produce another
 * <code>DataSource</code>.
 * <p>
 * 
 * Typically, a <code>MediaProxy</code> reads a text configuration file that
 * contains all of the information needed to make a connection to a server and
 * obtain media data. To produce a <code>Player</code> from a
 * <code>MediaLocator</code> referencing the configuration file,
 * <code>Manger</code>:
 * <ul>
 * <li>constructs a <code>DataSource</code> for the protocol described by the
 * <code>MediaLocator</code>
 * <li>constructs a <code>MediaProxy</code> to read the configuration file using
 * the content-type of the <code>DataSource</code>
 * <li>obtains a new <code>DataSource</code> from the <code>MediaProxy</code>
 * <li>constructs the <code>Player</code> using the content-type of the new
 * <code>DataSource</code>
 * </ul>
 * 
 * 
 * @see Manager
 * 
 * @version 1.10, 97/08/25.
 */
public interface MediaProxy extends MediaHandler
{

    /**
     * Obtain the new <code>DataSource</code>. The <code>DataSource</code> is
     * already connected.
     * 
     * @exception IOException
     *                Thrown when if there are IO problems in reading the the
     *                original or new <code>DataSource</code>.
     * 
     * @exception NoDataSourceException
     *                Thrown if this proxy can't produce a
     *                <code>DataSource</code>.
     * 
     * @return the new <code>DataSource</code> for this content.
     */
    public DataSource getDataSource() throws IOException, NoDataSourceException;

}

/* 
 * ***** EDITOR CONTROL STRINGS ***** Local Variables: tab-width: 8
 * c-basic-offset: 4 indent-tabs-mode: t End: vi:set ts=8 sw=4:
 * *********************************
 */
