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

package javax.media.protocol;

/**
 * Abstracts a single stream of media data.
 * 
 * <h2>Stream Controls</h2>
 * 
 * A <code>SourceStream</code> might support an operation that is not part of
 * the <code>SourceStream</code> definition. For example a stream might support
 * seeking to a particular byte in the stream. Some operations are dependent on
 * the stream data, and support cannot be determined until the stream is in use.
 * <p>
 * 
 * To obtain all of the objects that provide control over a stream use
 * <code>getControls</code>. To determine if a particular kind of control is
 * available, and obtain the object that implements the control use
 * <code>getControl</code>.
 * 
 * 
 * @see DataSource
 * @see PushSourceStream
 * @see PullSourceStream
 * @see Seekable
 * 
 * @version 1.12, 97/08/28.
 */

public interface SourceStream extends Controls
{

    public static final long LENGTH_UNKNOWN = -1;

    /**
     * Get the current content type for this stream.
     * 
     * @return The current <CODE>ContentDescriptor</CODE> for this stream.
     */
    public ContentDescriptor getContentDescriptor();

    /**
     * Get the size, in bytes, of the content on this stream. LENGTH_UNKNOWN is
     * returned if the length is not known.
     * 
     * @return The content length in bytes.
     */
    public long getContentLength();

    /**
     * Find out if the end of the stream has been reached.
     * 
     * @return Returns <CODE>true</CODE> if there is no more data.
     */
    public boolean endOfStream();

}

/* 
 * ***** EDITOR CONTROL STRINGS ***** Local Variables: tab-width: 8
 * c-basic-offset: 4 indent-tabs-mode: t End: vi:set ts=8 sw=4:
 * *********************************
 */
