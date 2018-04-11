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

package org.cablelabs.impl.ocap.hn.content;

import java.io.IOException;

import org.cablelabs.impl.ocap.hn.transformation.OutputVideoContentFormatExt;
import org.cablelabs.impl.util.SecurityUtil;
import org.ocap.hn.content.ContentFormat;
import org.ocap.hn.content.StreamableContentResource;
import org.ocap.hn.content.VideoResource;

public class LocalChannelContentItemResource extends ContentResourceImpl
    implements StreamableContentResource, VideoResource
{
    protected final ChannelContentItemImpl m_ccii; 
    protected final OutputVideoContentFormatExt m_outputContentFormat;
    protected final String m_nativeProfile;

    public LocalChannelContentItemResource( ChannelContentItemImpl ccii, 
                                            String resURI,
                                            String resProtocolInfo, 
                                            String resSize, 
                                            String resDuration, 
                                            String resBitrate, 
                                            String resAudioBitsPerSample, 
                                            String[] audioLanguages, 
                                            String resAudioNumberOfChannels, 
                                            String resAudioSampleFrequency, 
                                            String resVideoColorDepth,  
                                            String resVideoWidth,
            String resVideoHeight, String cleartextSize, String alternateURI, 
            String nativeProfile )
    {
        super( ccii, resURI, resProtocolInfo, resSize, resDuration, 
               resBitrate, resAudioBitsPerSample, audioLanguages, 
               resAudioNumberOfChannels, resAudioSampleFrequency, resVideoColorDepth,
               resVideoWidth, resVideoHeight, cleartextSize, alternateURI );
        m_ccii = ccii;
        m_nativeProfile = nativeProfile;
        m_outputContentFormat = null;
    }

    public LocalChannelContentItemResource( ChannelContentItemImpl ccii, 
                                            String resURI, 
                                            String resProtocolInfo, 
                                            String resSize, 
                                            String resDuration, 
                                            String resAudioBitsPerSample, 
                                            String[] audioLanguages, 
                                            String resAudioNumberOfChannels, 
                                            String resAudioSampleFrequency, 
                                            String resVideoColorDepth, 
                                            String cleartextSize, 
                                            String alternateURI, 
                                            OutputVideoContentFormatExt outputContentFormat )
    {
        super( ccii, resURI, resProtocolInfo, resSize, resDuration, 
               Integer.toString(outputContentFormat.getBitRate()), 
               resAudioBitsPerSample, audioLanguages, resAudioNumberOfChannels, 
               resAudioSampleFrequency, resVideoColorDepth,
               Integer.toString(outputContentFormat.getHorizontalResolution()), 
               Integer.toString(outputContentFormat.getVerticalResolution()), 
               cleartextSize, alternateURI );
        m_ccii = ccii;
        m_nativeProfile = null;
        m_outputContentFormat = outputContentFormat;
    }

    //
    // ContentResourceImpl overrides
    //
    
    // Override
    public ContentFormat[] getTransformedContentFormats() throws IOException
    {
        if (m_outputContentFormat != null)
        {
            return new ContentFormat[] {m_outputContentFormat};
        }
        else
        {
            return new ContentFormat[0];
        }
    }
    
    // Override
    public boolean delete() throws IOException, SecurityException
    {
        // This content resource may be associated with a native profile or a transformation
        SecurityUtil.checkPermission(CONTENT_MANAGEMENT_PERMISSION);
        
        final ContentItemExt contentItem = (ContentItemExt)getContentItem();

        if (contentItem == null)
        {
            return false;
        }

        if (!contentItem.hasWritePermission())
        {
            throw new SecurityException(ContentEntryImpl.NO_WRITE_PERMISSONS);
        }
        
        return m_ccii.deleteContentResource(this);
    } // END delete()

    // Override
    public boolean isRenderable()
    {
        return true;
    }

    //
    // Class methods
    //
    public OutputVideoContentFormatExt getOutputVideoContentFormat()
    {
        return m_outputContentFormat;
    }

    public String getNativeProfile()
    {
        return m_nativeProfile;
    }
}
