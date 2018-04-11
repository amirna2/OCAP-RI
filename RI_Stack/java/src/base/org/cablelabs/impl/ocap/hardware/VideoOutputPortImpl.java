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

package org.cablelabs.impl.ocap.hardware;

import org.cablelabs.impl.util.SecurityUtil;

import org.apache.log4j.Logger;
import java.util.Enumeration;
import java.util.NoSuchElementException;


import org.ocap.hardware.IEEE1394Node;
import org.ocap.hardware.VideoOutputPort;
import org.ocap.system.MonitorAppPermission;
import java.lang.Object;

import java.awt.Dimension;

/**
 * Implements <code>VideoOutputPort</code> using native implementation.
 * 
 * @author Aaron Kamienski
 */
public class VideoOutputPortImpl extends VideoOutputPort
{
    private static final Logger log = Logger.getLogger(VideoOutputPortImpl.class.getName());

    private static final int MAX_RESTRICTED_RESOLUTION = 520000;

    /**
     * Provides an implementation for
     * {@link Host#getVideoOutputPorts}.
     * 
     * @return an <code>Enumeration</code> of the supported
     *         <code>VideoOutputPorts</code>
     */
    public static Enumeration getVideoOutputPorts()
    {
        return new ArrayEnum(getPorts());
    }

    /**
     * Retrieves the array of known <code>VideoOutputPort</code>s. If the ports
     * have not yet been discovered and the <i>ports</i> array initialized, this
     * is performed first.
     * 
     * @return the array of known <code>VideoOutputPort</code>s.
     */
    private static synchronized VideoOutputPort[] getPorts()
    {
        if (ports == null)
        {
            int[] handles = nGetVideoOutputPorts();

            ports = new VideoOutputPort[handles.length];
            for (int i = 0; i < handles.length; ++i)
            {
                ports[i] = new VideoOutputPortImpl(handles[i]);
            }
        }
        return ports;
    }

    /**
     * Creates a new instance of <code>VideoOutputPortImpl</code> based upon the
     * given native handle.
     * 
     * @param handle
     *            native handle
     */
    protected VideoOutputPortImpl(int handle)
    {
        this.handle = handle;
        if (!nInitInfo(handle)) throw new RuntimeException("Could not get information about port");
    }

    // Description copied from VideoOutputPort
    public void enable() throws IllegalStateException, SecurityException
    {
        checkPermission();

        if (nEnable(handle, true) != 0) throw new IllegalStateException(this + " could not be enabled");
    }

    // Description copied from VideoOutputPort
    public void disable() throws IllegalStateException, SecurityException
    {
        checkPermission();

        if (nEnable(handle, false) != 0) throw new IllegalStateException(this + " could not be disabled");
    }

    // Description copied from VideoOutputPort
    public boolean status()
    {
        return nGetStatus(handle);
    }

    // Description copied from VideoOutputPort
    public Object queryCapability(int capabilityType)
    {
        switch (capabilityType)
        {
            case CAPABILITY_TYPE_DTCP:
                return new Boolean(dtcp);
            case CAPABILITY_TYPE_HDCP:
                return new Boolean(hdcp);
            case CAPABILITY_TYPE_RESOLUTION_RESTRICTION:
                if (getType() != AV_OUTPUT_PORT_TYPE_COMPONENT_VIDEO)
                {
                    return new Integer(-1);
                }
                if (restrictedResolution > MAX_RESTRICTED_RESOLUTION)
                {
                    throw new IllegalArgumentException ("Restricted Resolution " + restrictedResolution + " exceeds maximum " + MAX_RESTRICTED_RESOLUTION);
                }
                return new Integer(restrictedResolution);
            default:
                throw new IllegalArgumentException("Unknown capabilityType " + capabilityType);
        }
    }

    // Description copied from VideoOutputPort
    public IEEE1394Node[] getIEEE1394Node() throws IllegalStateException, SecurityException
    {
        if (type != AV_OUTPUT_PORT_TYPE_1394) throw new IllegalStateException("It's not IEEE1394 port");

        checkPermission();

        return IEEE1394NodeImpl.getIEEE1394NodeList(handle);
    }

    // Description copied from VideoOutputPort
    public void selectIEEE1394Sink(byte[] eui64, short subunitType) throws IllegalArgumentException,
            IllegalStateException, SecurityException
    {
        if (type != AV_OUTPUT_PORT_TYPE_1394) throw new IllegalStateException("It's not IEEE1394 port");

        checkPermission();

        IEEE1394NodeImpl.selectIEEE1394Sink(handle, eui64, subunitType);
    }

    // Description copied from VideoOutputPort
    public int getType()
    {
        return type;
    }

    public Dimension getResolution ()
    {
        return pixelResolution;
    }

    public String toString()
    {
        return getClass().getName()
                + "["
                + "type="
                + ((type == AV_OUTPUT_PORT_TYPE_RF) ? ("RF channel 3/4")
                        : (type == AV_OUTPUT_PORT_TYPE_BB) ? ("Baseband (RCA connector)")
                                : (type == AV_OUTPUT_PORT_TYPE_SVIDEO) ? ("S-Video")
                                        : (type == AV_OUTPUT_PORT_TYPE_1394) ? ("1394 (Firewire)")
                                                : (type == AV_OUTPUT_PORT_TYPE_DVI) ? ("DVI (Panel Link, HDCP)")
                                                        : (type == AV_OUTPUT_PORT_TYPE_COMPONENT_VIDEO) ? ("Component Video")
                                                                : (type == AV_OUTPUT_PORT_TYPE_HDMI) ? ("HDMI")
                                                                        : (type == AV_OUTPUT_PORT_TYPE_INTERNAL) ? ("Internal (integrated/internal display)")
                                                                                : "unknown") + ",dtcp="
                + ((dtcp == true) ? "true" : "false") + ",hdcp=" + ((hdcp == true) ? "true" : "false")
                + ", pixelResolution=" + pixelResolution
                + ",restrictedResolution=" + restrictedResolution + ",handle=" + handle + "]";
    }

    public void refreshPortInfo()
    {
        if (!nInitInfo(handle)) throw new RuntimeException("Could not get information about port");
    }

    /**
     * Determines if the caller has
     * <code>MonitorAppPermission("setVideoPort")</code> or not. If not, then a
     * <code>SecurityException</code> is thrown.
     * 
     * @throws SecurityException
     *             if the caller has not been granted
     *             MonitorAppPermission("setVideoPort")
     */
    private void checkPermission() throws SecurityException
    {
        SecurityUtil.checkPermission(PERMISSION);
    }

    /**
     * Initializes JNI.
     */
    private static native void nInit();

    /**
     * Creates a new array containing native interface handles for the known
     * video output ports.
     * 
     * @return a new array containing native interface handles for the known
     *         video output ports
     */
    protected static native int[] nGetVideoOutputPorts();

    /**
     * Enables or disables the given video output port.
     * 
     * @param handle
     *            native video output port handle
     * @param enable
     *            if <code>true</code> then enable the port; if
     *            <code>false</code> then disable the port
     * @return zero if succeeded otherwise non-zero error code
     */
    // TODO_DS: does nEnable need to enable the audio port?
    private static native int nEnable(int handle, boolean enable);

    /**
     * Retrieves the status of the video output port.
     * 
     * @param handle
     *            native video output port handle
     * @return <code>true</code> if the port is enabled; <code>false</code>
     *         otherwise
     */
    private static native boolean nGetStatus(int handle);

    /**
     * Retrieves the id string (name) of the main video output port.
     * 
     * @param handle
     *            native video output port handle
     * @return <code>true</code> if the port is enabled; <code>false</code>
     *         otherwise
     */
    protected static native String nGetMainVideoOutputPort();

    /**
     * Initializes this <code>VideoOutputPortImpl</code> with information about
     * the native video output port. This includes the following (which may be
     * expanded as necessary):
     * <ul>
     * <li>type
     * <li>dtcp
     * <li>hdcp
     * <li>restrictedResolution
     * </ul>
     * 
     * @param nHandle
     *            native video output port handle
     */
    private native boolean nInitInfo(int nHandle);

    /**
     * The type of this video output port.
     */
    private int type;

    /**
     * Whether DTCP is supported or not.
     */
    private boolean dtcp;

    /**
     * Whether HDCP is supported or not.
     */
    private boolean hdcp;

    /**
     * The pixel resolution of the video output port.
     */
    private Dimension pixelResolution = new Dimension();

    /**
     * The restricted vertical pixel resolution for HD video.
     */
    private int restrictedResolution;

    /**
     * The native video output port handle.
     */
    private int handle;

    /**
     * Array of the <code>VideoOutputPort</code> instances.
     */
    private static VideoOutputPort[] ports;

    /**
     * Permission necessary to manipulate <code>VideoOutputPort</code>.
     */
    private static MonitorAppPermission PERMISSION = new MonitorAppPermission("setVideoPort");

    /**
     * Initializes JNI.
     */
    static
    {
        org.cablelabs.impl.ocap.OcapMain.loadLibrary();
        nInit();
    }

    /**
     * Enumeration implementation based upon an array of <code>Object</code>s.
     * 
     * @author Aaron Kamienski
     */
    public static class ArrayEnum implements Enumeration
    {
        private int index;

        private Object[] array;

        /**
         * Create a new <code>ArrayEnum</code> based upon the given non-
         * <code>null</code> array.
         * 
         * @param array
         *            the basis for the new <code>Enumeration</code>
         */
        public ArrayEnum(Object[] array)
        {
            if (array == null) throw new NullPointerException("Invalid array");

            this.array = array;
            this.index = 0;
        }

        // Description copied from Enumeration
        public synchronized boolean hasMoreElements()
        {
            return index < array.length;
        }

        // Description copied from Enumeration
        public synchronized Object nextElement()
        {
            try
            {
                return array[index++];
            }
            catch (IndexOutOfBoundsException e)
            {
                throw new NoSuchElementException();
            }
        }
    }

    public int getHandle()
    {
        return handle;
    }

    void setHandle(int handle)
    {
        this.handle = handle;
    }

    // dump the current state of this object
    public void dump(String sPre)
    {
        if (log.isDebugEnabled())
        {
            log.debug(sPre + "type: " + type + ", pixelResolution: " + pixelResolution.width +
                    "x" + pixelResolution.height + sPre + "native handle: " + handle +
                    sPre + "dtcp supported: " + dtcp +
                    sPre + "hdcp supported: " + hdcp +
                    sPre + "restrictedResolution: " + restrictedResolution);
        }
}
}
