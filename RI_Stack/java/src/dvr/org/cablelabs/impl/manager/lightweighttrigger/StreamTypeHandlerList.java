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

package org.cablelabs.impl.manager.lightweighttrigger;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.ocap.dvr.event.LightweightTriggerHandler;

public class StreamTypeHandlerList
{
    private static StreamTypeHandlerList m_instance;

    // maps type to vector of LightweightTriggerCallback(s) associated with this
    // type
    Hashtable m_typesRegistered;

    Object m_mutex = new Object();

    static public synchronized StreamTypeHandlerList getInstance()
    {
        if (m_instance == null)
        {
            m_instance = new StreamTypeHandlerList();
        }
        return m_instance;
    }

    private StreamTypeHandlerList()
    {
        m_typesRegistered = new Hashtable();
    }

    public LightweightTriggerCallback registerHandler(LightweightTriggerHandler handler, short type)
    {
        Short typeObj = new Short(type);

        synchronized (m_mutex)
        {
            Vector callbackVector = (Vector) m_typesRegistered.get(typeObj);
            if (callbackVector == null)
            {
                // this is a new type to handle
                LightweightTriggerCallback callback = new LightweightTriggerCallback(handler);
                callbackVector = new Vector();
                callbackVector.add(callback);
                if (!m_typesRegistered.contains(typeObj)) // acossitt: is this
                                                          // necessary or will
                                                          // just putting the
                                                          // value be faster.
                {
                    m_typesRegistered.put(typeObj, callbackVector);
                }
                return callback;
            }
            else
            {
                // type exists

                // if a specific handler instance has already be registed for
                // THIS type, don't register it again.
                LightweightTriggerCallback callback;

                for (int i = 0; i < callbackVector.size(); i++)
                {
                    callback = (LightweightTriggerCallback) callbackVector.elementAt(i);
                    if (callback.getHandler() == handler)
                    {
                        return callback;
                    }
                }
                // okay, the type exists, but this is a new handler
                callback = new LightweightTriggerCallback(handler);
                callbackVector.add(callback);
                return callback;
            }
        }
    }

    /**
     * remove handler from all types
     * 
     * @param handler
     */

    public void unregisterHandler(LightweightTriggerHandler handler)
    {
        boolean foundHandler = false;
        synchronized (m_mutex)
        {
            Enumeration typeEnum = m_typesRegistered.keys();
            while (typeEnum.hasMoreElements())
            {
                Short type = (Short) typeEnum.nextElement();
                Vector callbackVector = (Vector) m_typesRegistered.get(type);
                for (int i = 0; i < callbackVector.size(); i++)
                {
                    LightweightTriggerCallback callback = (LightweightTriggerCallback) callbackVector.elementAt(i);
                    if (callback.getHandler() == handler)
                    {
                        callback.stop();
                        callbackVector.remove(callback);
                        if (callbackVector.isEmpty())
                        {
                            // remove type, hopefully this won't barf up the
                            // enumeration
                            m_typesRegistered.remove(type);
                        }
                        // 0-1 matches per vector
                        foundHandler = true;
                        break;
                    }
                }
            }
        }
        if (!foundHandler)
        {
            throw new IllegalArgumentException("Handler not registered");
        }
    }

    public Object[] getHandlersForType(short type)
    {
        synchronized (m_mutex)
        {
            Vector callbackVector = (Vector) m_typesRegistered.get(new Short(type));

            if (callbackVector == null) return null;
            Object[] array = callbackVector.toArray();
            return array;
        }
    }

    public boolean isHandled(Short pidType)
    {
        if (m_typesRegistered.get(pidType) == null)
        {
            return false;
        }
        else
        {
            return true;
        }
    }

    public int getNumTypesRegistered()
    {
        return m_typesRegistered.keySet().size();
    }

    public boolean isEmpty()
    {
        return m_typesRegistered.isEmpty();
    }

    public Enumeration getTypesRegistered()
    {
        return m_typesRegistered.keys();
    }
}
