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

/*
 * Created on Mar 20, 2007
 */
package org.ocap.hardware.device;

import java.util.Hashtable;

import org.dvb.media.VideoFormatControl;
import org.ocap.hardware.VideoOutputPort;

/**
 * The <code>VideoOutputSettings</code> interface extends the functionality of
 * the video outputs to support configuration of video output resolution. In
 * Host devices supporting this specification, all instances of
 * <code>VideoOutputPort</code> SHALL implement this interface.
 * <p>
 * An application MAY query the set of output configurations supported by a
 * given video output port by calling {@link #getSupportedConfigurations()}. The
 * supported configurations MAY be used to configure the given video output port
 * by calling {@link #setOutputConfiguration}.
 *
 * @see VideoOutputPort
 */
public interface VideoOutputSettings
{
    /**
     * Get the fixed set of supported output configurations for this video
     * output port. This returns all supported output resolutions, regardless of
     * input resolution, as an array of {@link VideoOutputConfiguration}
     * instances.
     * <p>
     * The returned set of configurations SHALL be the intersection of those
     * supported by the output port of the Host device and those supported by
     * the connected display device, where such information is known by the
     * Host.
     *
     * @return The set of all supported output resolutions as an array.
     */
    public VideoOutputConfiguration[] getSupportedConfigurations();

    /**
     * Determine if this video output port supports dynamic output configuration
     * based upon input video attributes.
     *
     * @return <code>true</code> if instances of
     *         <code>DynamicVideoOutputConfiguration</code> may be set via
     *         {@link #setOutputConfiguration}
     */
    public boolean isDynamicConfigurationSupported();

    /**
     * Get the current output configuration for this video output port. Changes
     * to a returned configuration instance SHALL have no effect on the current
     * configuration unless applied via invocation of
     * <code>setOutputConfiguration</code>.
     * <p>
     * If the returned <code>VideoOutputConfiguration</code> corresponds to a
     * configuration returned by {@link #getSupportedConfigurations}, that same
     * configuration SHALL be returned.
     *
     * @return The current output configuration.
     *
     * @see #setOutputConfiguration
     */
    public VideoOutputConfiguration getOutputConfiguration();

    /**
     * Set the output configuration for this video output port. Only
     * <code>VideoOutputConfiguration</code>s instances corresponding to
     * supported video configurations SHALL be accpeted; anything else results
     * in an exception.
     * <p>
     * The output resolution configuration is applied at method invocation time.
     * Changes to a configuration instance SHALL have no effect on the current
     * configuration unless applied via invocation of this method specifying the
     * given configuration.
     * <p>
     * Changing this setting on the "main" video output port for an
     * <code>HScreen</code> MAY affect the configuration of the
     * <code>HScreenDevice</code>s of that <code>HScreen</code> to maintain
     * consistent display aspect ratios as described in the body of this
     * specification.
     *
     * @param config
     *            The new output configuration.
     *
     * @throws IllegalArgumentException
     *             if the output resolution specified does not correspond to one
     *             of the supported configurations
     * @throws FeatureNotSupportedException
     *             if the given configuration corresponds to one of the
     *             supported resolutions, but cannot be satisfied
     *
     * @throws SecurityException
     *             if the caller does not have
     *             <code>MonitorAppPermission("deviceController")</code>
     *
     * @see #getOutputConfiguration
     */
    public void setOutputConfiguration(VideoOutputConfiguration config) throws FeatureNotSupportedException;

    /**
     * Get the <code>AudioOutputPort</code> object which can be used to control
     * the audio output port associated with this video output port.
     *
     * @return the <code>AudioOutputPort</code> for this video output port
     */
    public AudioOutputPort getAudioOutputPort();

    /**
     * Add the given listener to monitor this video output port for status
     * changes.
     *
     * @param l
     *            the listener to add
     *
     * @see #removeListener(VideoOutputPortListener)
     */
    public void addListener(VideoOutputPortListener l);

    /**
     * Remove the given listener from further notification of status changes for
     * this video output port.
     *
     * @param l
     *            the listener to remove
     *
     * @see #addListener(VideoOutputPortListener)
     */
    public void removeListener(VideoOutputPortListener l);

    /**
     * Get the set of attributes describing the display currently connected to
     * this <code>VideoOutputPort</code>. The display attributes information is
     * returned in the form of a <code>Hashtable</code>, mapping attribute names
     * (specified as <code>String</code>s) to attribute values (specified as
     * attribute-specific <code>Object</code>s).
     * <p>
     * For example, where EEDID (Enhanced Extended Display Identification Data)
     * retrieval is supported, this information SHALL be accessible as
     * attributes of the connected display. In case of EEDID, the following
     * table describes the attribute mappings that SHALL be supported and used
     * where appropriate.
     * <table border>
     * <p>
     * <tr>
     * <th>Attribute name</th>
     * <th>Attribute value</th>
     * </tr>
     * <tr>
     * <td>"Manufacturer Name"</td>
     * <td>3-character EISA manufacturer ID as a {@link java.lang.String}</code>
     * </td>
     * </tr>
     * <tr>
     * <td>"Product Code"</td>
     * <td>Vendor assigned product code as a {@link java.lang.Short}</td>
     * </tr>
     * <tr>
     * <td>"Serial Number"</td>
     * <td>Vendor assigned serial number as a {@link java.lang.Integer}</td>
     * </tr>
     * <tr>
     * <td>"Manufacture Week"</td>
     * <td>Week of manufacture as a {@link java.lang.Byte}</td>
     * </tr>
     * <tr>
     * <td>"Manufacture Year"</td>
     * <td>Year of manufacture (offset from 1990) as a {@link java.lang.Byte}</td>
     * </tr>
     * </table>
     *
     * @return a <code>Hashtable</code> describing known display attributes;
     *         <code>null</code> is returned if {@link #isDisplayConnected()}
     *         would return <code>false</code>
     */
    public Hashtable getDisplayAttributes();

    /**
     * Get the connection status of this video output port.
     * <p>
     * If the Host device is incapable of determining connection status (e.g.,
     * for {@link VideoOutputPort#AV_OUTPUT_PORT_TYPE_COMPONENT_VIDEO component
     * video}), then <code>false</code> SHALL be returned.
     *
     * @return <code>true</code> if a display is known to be connected;
     *         <code>false</code> otherwise
     */
    public boolean isDisplayConnected();

    /**
     * Get the protection status of content on this video output port.
     * <p>
     * This SHALL return <code>true</code> if both this port and the connected
     * display support content protection and content is protected;
     * <code>false</code> SHALL be returned otherwise.
     *
     * @return whether content is currently protected on this video output port
     */
    public boolean isContentProtected();

    /**
     * Get the aspect ratio of the display connected to this video output port.
     *
     * @return one of {@link VideoFormatControl#DAR_4_3},
     *         {@link VideoFormatControl#DAR_16_9}, or -1 if unknown
     */
    public int getDisplayAspectRatio();
}
