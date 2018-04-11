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

package org.cablelabs.impl.ocap.resource;

import java.util.Enumeration;
import java.util.Hashtable;

import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceProxy;
import org.dvb.application.AppID;
import org.ocap.resource.ResourceUsage;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.ApplicationManager;
import org.cablelabs.impl.manager.ManagerManager;

public class ResourceUsageImpl implements ResourceUsage
{

    private static final AppID SYSTEM_APP_ID = new AppID(0, 0xFFFF);
    private static final int SYSTEM_APP_PRIORITY = 256;

    public ResourceUsageImpl(CallerContext context)
    {
        AppID appid = SYSTEM_APP_ID;
        int priority = SYSTEM_APP_PRIORITY;
        Integer intObject = null;

        if (null != context)
        {
            appid = (AppID) context.get(CallerContext.APP_ID);
            if (appid == null)
            {
                appid = SYSTEM_APP_ID;
                this.m_systemUsage = true;
            }
            intObject = (Integer) context.get(CallerContext.APP_PRIORITY);
            if (intObject != null) priority = intObject.intValue();
        }

        this.m_priority = priority;
        this.m_id = appid;
        this.m_resourceUsageEAS = false;
    }

    public ResourceUsageImpl(AppID id, int priority)
    {
        // Expect that the creator of this class knows the priority.
     
        // if null assign system usage values 
        if (id == null)
        {
            id = SYSTEM_APP_ID;
            priority = SYSTEM_APP_PRIORITY;
            this.m_systemUsage = true;
        }

        // TODO special case for HScreenDevice.java ,,fix
        if (priority >= 0)
            this.m_priority = priority;
        else
            this.m_priority = SYSTEM_APP_PRIORITY;

        this.m_id = id;
    }

    public boolean isResourceUsageEAS()
    {
        return m_resourceUsageEAS;
    }

    public void setResourceUsageEAS(boolean resourceUsageEAS)
    {
        m_resourceUsageEAS = resourceUsageEAS;
    }

    public boolean isSystemUsage()
    {
        return m_resourceUsageEAS ||
            ((m_id == SYSTEM_APP_ID) && m_systemUsage);  

    }

    public void setSystemUsage(boolean systemUsage)
    {
        m_systemUsage = systemUsage;
    }

    /**
     * Returns the application id.
     * 
     * @return the application id
     */
    public AppID getAppID()
    {
        return m_id;
    }

    /**
     * Return the application priority. Expect that the creator of this class
     * knows the priority.
     * 
     * @return the application priority.
     */
    public int getPriority()
    {
        // if the priority is explicitly set to a value, return that value
        // if -1, then ask the ApplicationManager for the priority
        if (priorityExplicitlySet || m_priority == 256)
            return this.m_priority;
        else
        {
            ApplicationManager am = (ApplicationManager) ManagerManager.getInstance(ApplicationManager.class);
            return am.getRuntimePriority(m_id);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.ocap.resource.ExtendedResourceUsage#setPriority(int)
     */
    public void setPriority(int priority)
    {
        this.m_priority = priority;
        priorityExplicitlySet = true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ocap.resource.ResourceUsage#getResource(java.lang.String)
     */
    public ResourceProxy getResource(String resourceName)
    {
        ResourceProxy rp = (ResourceProxy) m_resources.get(resourceName);

        if (rp == null) throw new IllegalArgumentException("Resource name not found");

        return (rp == NULL_PROXY) ? null : rp;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ocap.resource.ResourceUsage#getResourceNames()
     */
    public String[] getResourceNames()
    {
        synchronized (m_resources)
        {
            int size = m_resources.size();
            String[] array = new String[size];

            if (size > 0)
            {
                int j = 0;

                for (Enumeration e = m_resources.keys(); e.hasMoreElements();)
                    array[j++] = (String) e.nextElement();
            }
            return array;
        }
    }

    /**
     * Associates or disassociates a <code>ResourceProxy</code> to/from a
     * required resource type. Assuming that the <code>type</code> type is
     * supported, calling this method will add/retain the required resource type
     * regardless of the value of <code>proxy</code>.
     * 
     * @param type
     *            the fully qualified path of the resource type.
     * @param proxy
     *            <i>non-null </i> if the resource associated with
     *            <code>proxy</code> should be reserved, <code>null</code> if
     *            the resource associated with proxy should be released
     * 
     * @return Specifies whether the <code>set</code> worked. It will not work
     *         if the <code>type</code> is not supported or if the
     *         <code>type</code> and <code>proxy</code> are mismatched.
     */
    public boolean set(String type, ResourceProxy proxy)
    {
        if (type != null)
        {
            for (int i = 0; i < allowedTypes.length; i++)
            {
                // See if the type is on the approved list.
                if (allowedTypes[i].getName().equals(type))
                {
                    // We should probably check to make sure the proxy is either
                    // null or of the type it claims.
                    if ((proxy == null) || (allowedTypes[i].isInstance(proxy)))
                    {
                        m_resources.put(type, (proxy != null ? proxy : NULL_PROXY));
                        return true;
                    }
                    // Might as well quit since we've found the type we
                    // want, but it was not of the type it claimed.
                    break;
                }
            }
        }
        return false;
    }

    /**
     * Associates or disassociates a <code>ResourceProxy</code> to/from a
     * required resource type. The resource type does not need to be specified
     * since it can be gleened directly from the <code>proxy</code>. Assuming
     * that the <code>ResourceProxy</code> type is supported, calling this
     * method will add/retain the required resource type regardless of the value
     * of <code>reserve</code>.
     * 
     * @param proxy
     *            the resource to mark as reserved or released
     * @param reserve
     *            <code>true</code> if the resource associated with
     *            <code>proxy</code> should be reserved, <code>false</code> if
     *            the resource associated with <code>proxy</code> should be
     *            released
     * 
     * @return Specifies whether the <code>set</code> worked. It may not work if
     *         the type of <code>ResourceProxy</code> is not supported.
     */
    public boolean set(ResourceProxy proxy, boolean reserve)
    {
        if (proxy != null)
        {
            String type = enumType(proxy);

            if (type != null)
            {
                m_resources.put(type, (reserve == true ? proxy : NULL_PROXY));
                return true;
            }
        }
        return false;
    }

    /**
     * This method removes the <code>proxy</code> reservation (or resource type
     * association), if it exists. It also removes the resource type from the
     * list of resource names returned from
     * {@link ResourceUsage#getResourceNames()}. The resource type does not need
     * to be passed since it can be gleened from the proxy directly.
     * 
     * 
     * @param proxy
     *            resource to disassociate from the <code>ResourceUsage</code>
     * 
     * @return <code>true</code> if the <code>proxy</code> type was removed and
     *         <code>false</code> if it could not be removed. It may not be
     *         removed if the <code>proxy</code> type is not supported or if the
     *         <code>proxy</code> was not associated with a required resource
     *         type.
     */
    public boolean remove(ResourceProxy proxy)
    {
        if (proxy != null)
        {
            String type = enumType(proxy);

            if (type != null) return m_resources.remove(type) != null;
        }
        return false;
    }

    /**
     * Returns the <code>ResourceUsage</code> super-interface for use by the
     * resource contention handler.
     * 
     * @return The <code>ResourceUsage</code> super-interface.
     */
    public ResourceUsage getResourceUsage()
    {
        return this;
    }

    /**
     * Tests whether another <code>ResourceUsage</code> is equals to this one.
     * The required resource types will be used for the compare. It will also
     * test to make sure that the two objects are of the same leaf type. We are
     * not testing whether there are actual reservations for the required types.
     * 
     * @param usage
     *            the ResourceUsage that will be used for the equality test
     * @return <code>true</code> if the <code>usage</code> is the same as this
     *         one and <code>false</code> if it is not
     */
    public boolean isEquals(ResourceUsage usage)
    {
        // If a reference to this object was passed in, then we know they are
        // equal and there is really no need to go further.
        if (this == usage) return true;

        // If it's not null and is of this type, then recast it so we can
        // look at the internals.
        if (usage != null && (usage instanceof ResourceUsageImpl))
        {
            ResourceUsageImpl compare = (ResourceUsageImpl) usage;
            // Make sure the objects are of the identical leaf type
            if (this.getClass() == compare.getClass())
            {
                // Compare the resources Hashtable.
                if (this.m_resources.size() == compare.m_resources.size())
                {
                    for (Enumeration e = this.m_resources.keys(); e.hasMoreElements();)
                    {
                        if (compare.m_resources.containsKey(e.nextElement()) == false) return false;
                    }
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * This method takes an instance of a <code>ResourceProxy</code> and returns
     * the fully qualified path name for an approved base of that object.
     * 
     * <p>
     * Supported values are specified by OCAP 19.2.1.1:
     * <ul>
     * <li><code>org.davic.mpeg.sections.SectionFilterGroup</code>
     * <li><code>org.davic.net.tuning.NetworkInterfaceController</code>
     * <li><code>org.havi.ui.HBackgroundDevice</code>
     * <li><code>org.havi.ui.HGraphicsDevice</code>
     * <li><code>org.havi.ui.HVideoDevice</code>
     * </ul>
     * 
     * If <i>proxy </i> is of a type not inherited from one of the preceding
     * types, then an empty string is returned.
     * 
     * @param proxy
     *            An instance of a <code>ResourceProxy</code> object whose base
     *            type will be determined.
     */
    protected String enumType(ResourceProxy proxy)
    {
        String resourceType = null;

        for (int i = 0; i < allowedTypes.length; i++)
        {
            if (allowedTypes[i].isInstance(proxy))
            {
                resourceType = allowedTypes[i].getName();
                break;
            }
        }
        return resourceType;
    }

    // Used to tie resource types to actual resource reservations.
    private Hashtable m_resources = new Hashtable();

    // Object that will act as a non-reservation since we can't use null in a
    // Hashtable.
    private final ResourceProxy NULL_PROXY = new ResourceProxy()
    {

        public ResourceClient getClient()
        {
            return null;
        }
    };

    // An array of the allowed resource proxy types.
    private Class allowedTypes[] = { org.havi.ui.HBackgroundDevice.class, org.havi.ui.HGraphicsDevice.class,
            org.havi.ui.HVideoDevice.class, org.davic.net.tuning.NetworkInterfaceController.class,
            org.davic.mpeg.sections.SectionFilterGroup.class };

    /**
     * This method is a general purpose way of seeing if a
     * <code>ResourceProxy</code> is marked as <i>reserved </i> by a
     * <code>ResourceUsage</code>.
     * 
     * @param proxy
     *            An instance of a <code>ResourceProxy</code> that will be
     *            checked for reservation within <code>ResourceUsage</code>
     * 
     * @return boolean <code>true</code> if the <code>proxy</code> is
     *         <i>reserved </i> within the <code>ResourceUsage</code>;
     *         <code>false</code> otherwise
     */
    public boolean isReserved(ResourceProxy proxy)
    {
        ResourceProxy rp = null;

        if (proxy == null) return false;

        String resourceName = enumType(proxy);

        if (resourceName != null) rp = (ResourceProxy) m_resources.get(resourceName);
        return (rp == proxy) ? true : false;
    }

    /**
     * Overrides {@link java.lang.Object#toString()} to provide an
     * implementation-specific representation of a
     * <code>ResourceUsageImpl</code>.
     * 
     * @return a string representation of this <code>ResourceUsageImpl</code>
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer(getClass().getName());

        sb.append('@').append(System.identityHashCode(this)).append('[');
        sb.append("id=").append(m_id).append(',');
        sb.append("prio=").append(getPriority()).append(',');
        synchronized (m_resources)
        {
            for (Enumeration e = m_resources.keys(); e.hasMoreElements();)
                sb.append(e.nextElement()).append(',');
        }
        sb.append("EAS: ");
        sb.append(m_resourceUsageEAS);
        sb.append(']');

        return sb.toString();
    }

    /**
     * The encapsulated AppID, cached for reference. Fixed at construction time.
     */
    private AppID m_id;

    /** The fixed <i>override</i> priority. */
    private int m_priority;

    private boolean priorityExplicitlySet = false;

    /**
     * Flag used to track if ResourceUsage is being used to present EAS
     */
    private boolean m_resourceUsageEAS;

    /**
     * Flag used to track if ResourceUsage owned by system
     */
    private boolean m_systemUsage = false;
}
