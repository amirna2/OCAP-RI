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

package org.cablelabs.impl.io;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Represents filesystems that may load file and directory data in an
 * asynchronous manner such as broadcast filesystems
 *	
 *  @author Greg Rutz
 */
public interface BroadcastFileSys
{
    /**
     * Performs a synchronous load of the file referenced by <code>path</code>.
     * Used to support <code>DSMCCObject.synchronousLoad()</code> and
     * <code>DSMCCObject.asynchrounousLoad()</code>.
     * 
     * @param path
     *            name and location of the file
     * @param loadMode
     *            specifies the retrieval mode for the file data. Value should
     *            be one of:
     *            <ul>
     *            <li><code>FROM_CACHE</code>
     *            <li><code>FROM_CACHE_OR_STREAM</code>
     *            <li><code>FROM_STREAM_ONLY</code>
     *            </ul>
     * @return a <code>FileSys</code> object that can be used to perform more
     *         operations on the loaded file. Should be an instance of
     *         <code>LoadedFileSys</code>.
     * @throws FileNotFoundException
     *             if the file referenced by the supplied pathname does not
     *             exist.
     */
    public FileSys load(String path, int loadMode) throws FileNotFoundException, IOException;

    /**
     * Performs an asynchronous load of the file referenced by <code>path</code>
     * . This method is used to support
     * <code>DSMCCObject.asynchronousLoad()</code>.
     * 
     * @param path
     *            abstract pathname of the file to load
     * @param loadMode
     *            specifies the retrieval mode for the file data. Value should
     *            be one of:
     *            <ul>
     *            <li><code>FROM_CACHE</code>
     *            <li><code>FROM_CACHE_OR_STREAM</code>
     *            <li><code>FROM_STREAM_ONLY</code>
     *            </ul>
     * @param cb
     *            asynchronous callback handle that notifies the caller of the
     *            load status
     * @return load handle that can be used to abort the load
     * @throws FileNotFoundException
     *             if the file referenced by <code>path</code> does not exist.
     */
    public AsyncLoadHandle asynchronousLoad(String path, int loadMode, AsyncLoadCallback cb)
            throws FileNotFoundException;
}
