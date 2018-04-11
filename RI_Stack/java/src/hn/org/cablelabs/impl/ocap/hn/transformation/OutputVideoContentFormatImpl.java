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

package org.cablelabs.impl.ocap.hn.transformation;

import org.ocap.hn.content.OutputVideoContentFormat;

public class OutputVideoContentFormatImpl extends ContentFormatImpl 
    implements OutputVideoContentFormatExt, OutputVideoContentFormat, Comparable
{
    private final NativeContentTransformation m_nativeTransformation;
    
    public OutputVideoContentFormatImpl( final NativeContentTransformation nativeTransformation, 
                                         final String protectionType )
    {
        super(nativeTransformation.transformedProfile, protectionType);
        m_nativeTransformation = nativeTransformation;
    }
    
    public int getVerticalResolution()
    {
        return m_nativeTransformation.height;
    }

    public int getHorizontalResolution()
    {
        return m_nativeTransformation.width;
    }

    public int getBitRate()
    {
        return m_nativeTransformation.bitrate;
    }

    public boolean isProgressive()
    {
        return m_nativeTransformation.progressive;
    }
    
    // OutputContentFormatExt implementation
    
    public int getOutputFormatId()
    {
        return m_nativeTransformation.id;
    }

    public NativeContentTransformation getNativeTransformation()
    {
        return m_nativeTransformation;
    }

    // Comparable interface implementation
    
    public int compareTo(Object obj)
    {
        return ( this.m_nativeTransformation.id 
                 - ((OutputVideoContentFormatImpl)obj).m_nativeTransformation.id);
    }
    
    // Object class overrides
    
    public boolean equals(Object obj)
    {
        return (obj instanceof OutputVideoContentFormatImpl) 
               && ( this.m_nativeTransformation.id 
                    == ((OutputVideoContentFormatImpl) obj).m_nativeTransformation.id );
    }

    public int hashCode()
    {
        return 31 + m_nativeTransformation.id;
    }
    
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("OutputVCF 0x").append(Integer.toHexString(this.hashCode()));
        sb.append(":(id ").append(m_nativeTransformation.id);
        sb.append(",profile ").append(super.getContentProfile());
        sb.append(",prot ").append(super.getProtectionType());
        sb.append(",bitrate ").append(m_nativeTransformation.bitrate);
        sb.append(",width ").append(m_nativeTransformation.width);
        sb.append(",height ").append(m_nativeTransformation.height);
        sb.append(",progressive ").append(m_nativeTransformation.progressive);
        sb.append(')');
        
        return sb.toString();
    }
}
