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
package org.cablelabs.impl.media.streaming.session;

import javax.media.Time;

import org.cablelabs.impl.media.streaming.exception.HNStreamingException;
import org.cablelabs.impl.media.streaming.session.data.HNStreamContentDescription;
import org.ocap.hn.content.ContentItem;

/**
 * Support for streaming of individual contentitem types, including Recorded services, 
 * Channel content items and personal content published in the CDS.
 * 
 * Resources should be acquired in the constructor.  If transmit was never called, stop should still be called in order 
 * to release resources acquired by the constructor.
 */
public interface Stream 
{

    /**
     * Open the session
     * 
     * @param contentRequest the ContentRequest associated with this stream
     * 
     * @throws HNStreamingException if session could not be opened
     */
    void open(ContentRequest contentRequest) throws HNStreamingException;

    /**
     * Start transmission of the stream
     * 
     * NOTE: this method blocks indefinitely until stop is called.
     * 
     * @throws HNStreamingException if unable to transmit the stream
     */
    void transmit() throws HNStreamingException;

    /**
     * Stop transmission of the stream and release resources.
     * @param closeSocket true if the socket should be closed (only due to server-side initiated revocation)
     */
    void stop(boolean closeSocket);

    /**
     * Provide access to the connection ID associated with this stream
     * @return connection ID
     */
    Integer getConnectionId();

    /**
     * The URL associated with this stream
     * @return url
     */
    String getURL();

    /**
     * Provide access to the earliest mediatime available for this stream
     * 
     * @return available start time
     * @throws HNStreamingException if unable to retrieve available seek start time
     */
    Time getAvailableSeekStartTime() throws HNStreamingException;

    /**
     * Provide access to the latest mediatime available for this stream
     * 
     * @return available end time
     * @throws HNStreamingException if unable to retrieve available seek end time
     */
    Time getAvailableSeekEndTime() throws HNStreamingException;

    /**
     * Provide access to the frame rate in trick mode flag
     * 
     * @return frame rate in trick mode
     * @throws HNStreamingException if unable to retrieve frame rate in trick mode
     */
    int getFrameRateInTrickMode() throws HNStreamingException;
    
    /**
     * Provide access to the frame types in trick mode flag
     * 
     * @return frame type in trick mode
     * @throws HNStreamingException if unable to retrieve frame rate in trick mode
     */
    int getFrameTypesInTrickMode() throws HNStreamingException;
    
    /**
     * Provide access to the content location type
     * 
     * @return content location type
     */
    int getContentLocationType(); 
    
    /**
     * Provide access to the available seek start byte
     *
     * @param encrypted false for cleartext/local domain
     *                  true for encrypted/network domain
     *
     * @return available seek start byte; -1 if encrypted is false but the
     *         cleartext position cannot be determined
     * @throws HNStreamingException if unable to retrieve seek start byte
     */
    long getAvailableSeekStartByte(boolean encrypted) throws HNStreamingException; 
    
    /**
     * Provide access to the available seek end byte
     * 
     * @param encrypted false for cleartext/local domain
     *                  true for encrypted/network domain
     * 
     * @return available seek end byte; -1 if encrypted is false but the
     *         cleartext position cannot be determined
     * @throws HNStreamingException if unable to retrieve seek end byte
     */
    long getAvailableSeekEndByte(boolean encrypted) throws HNStreamingException; 
    
    /**
     * Provide access to the start byte position that will be transmitted by this stream
     * 
     * @return start byte
     */
    long getStartByte();
    
    /**
     * Provide access to the end byte position that will be transmitted by this stream
     * 
     * @return end byte
     */
    long getEndByte();

    /**
     * Accessor reporting if the content item is being streamed.
     * 
     * @param contentItem the content item to evaluate
     * @return true if the contentItem is being streamed
     */
    boolean isTransmitting(ContentItem contentItem);

    /**
     * Provide access to the content description 
     * 
     * @return content description 
     */
    HNStreamContentDescription getContentDescription(); 

    /**
     * Accessor returning the total number of bytes that will be streamed as long as the Sn increasing
     * flag is set to false or any other reason why this value cannot be derived ahead of time, else,
     * the value of -1 should be returned.
     * @return
     */
    long getRequestedContentLength();

    /**
     * Accessor returning the total duration of the content to be streamed (in milliseconds) 
     * or -1 if the duration is indeterminate/unknown
     */
    long getContentDuration();
}
