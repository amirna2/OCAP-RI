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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.apache.log4j.Logger;
import org.cablelabs.impl.util.Base64Codec;

public class SerializableWrapper extends AbstractValueWrapper
{
    private static final Logger log = Logger.getLogger(SerializableWrapper.class);

    public SerializableWrapper(boolean multiple, Object value)
    {
        super(multiple, value);
    }

    public SerializableWrapper(boolean multiple, Object[] value)
    {
        super(multiple, value);
    }

    public SerializableWrapper(boolean multiple, String value)
    {
        super(multiple, value);
    }

    public SerializableWrapper(boolean multiple, String[] value)
    {
        super(multiple, value);
    }

    protected Class elementClass()
    {
        return Serializable.class;
    }

    protected Object fromString(String s)
    {
        // TODO : do we throw here or swallow exceptions...spec clarification?
        if(s != null)
        {
            try
            {
                byte[] ba = Base64Codec.decodeBuffer(s);
                if (ba != null)
                {
                    ByteArrayInputStream is = new ByteArrayInputStream(ba);
                    ObjectInputStream ois = new ObjectInputStream(is);
                    return ois.readObject();
                }
            }
            catch (IOException e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Deserialization failed : " + e);
                }
            }
            catch(ClassNotFoundException ce)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Deserialization failed, class not found : " + ce);
                }
        }
        }

        return null;
    }

    protected String toString(Object o)
    {
        if(o != null)
        {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            try
            {
                ObjectOutputStream oos = new ObjectOutputStream(bo);
                oos.writeObject(o);
                oos.close();
                return Base64Codec.encode(bo.toByteArray());
            }
            catch (java.io.NotSerializableException nse)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("NotSerializableException Object value of type : " + o.getClass().getName());
                }
                if (log.isWarnEnabled())
                {
                    log.warn(nse);
                }
            }
            catch (IOException e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Serialization failed : " + e);
                }
        }
        }

        return "";
    }
}
