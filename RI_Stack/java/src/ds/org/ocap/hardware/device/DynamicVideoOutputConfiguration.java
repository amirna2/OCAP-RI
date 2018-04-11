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

package org.ocap.hardware.device;

import java.awt.Dimension;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

import org.dvb.media.VideoFormatControl;

/**
 * An instance of this class may be used to represent a dynamic selection of
 * video output configurations based upon specified input video. An instance of
 * <code>DynamicVideoOutputConfiguration</code> would mainly be used to allow
 * for the <code>HScreen</code> resolution and the video output port resolution
 * to closely match the resolution of the input video, generally with the
 * intention of letting the display monitor connected to the output port manage
 * aspect ratio conversions.
 * <p>
 * Such configurations are only valid for the current
 * {@link HostSettings#getMainVideoOutputPort main} video output port for a
 * given <code>HScreen</code>. If a video output port is not the current
 * <i>main</i> output port or ceases to be the <i>main</i>, then this
 * configuration setting SHALL be effectively ignored and output resolution
 * settings SHALL revert to an implementation-specific configuration.
 * <p>
 * Application of the input-to-output video resolution mapping described by an
 * instance of <code>DynamicVideoOutputConfiguration</code> MAY result in
 * configuration changes for the component <code>HScreenDevice</code>s of the
 * relevant <code>HScreen</code> as if the output resolution were selected via a
 * {@link FixedVideoOutputConfiguration static} configuration.
 * 
 * @author Aaron Kamenski
 * @author Alan Cossitt (implementation)
 */
public class DynamicVideoOutputConfiguration implements VideoOutputConfiguration
{
    /**
     * Constant representing any Standard Definition input video.
     */
    public static final VideoResolution INPUT_SD = new VideoResolution(null, VideoFormatControl.ASPECT_RATIO_UNKNOWN,
            0.0F, VideoResolution.SCANMODE_UNKNOWN);

    /**
     * Constant representing any High Definition input video.
     */
    public static final VideoResolution INPUT_HD = new VideoResolution(null, VideoFormatControl.ASPECT_RATIO_UNKNOWN,
            0.0F, VideoResolution.SCANMODE_UNKNOWN);

    private final Hashtable resolutionMap = new Hashtable();

    /**
     * Construct a new instance of <code>DynamicVideoOutputConfiguration</code>.
     * The newly created <code>DynamicVideoOutputConfiguration</code> contains
     * no mappings from input video to output video configuration.
     */
    public DynamicVideoOutputConfiguration()
    {

    }

    /**
     * Returns "Dynamic".
     * 
     * @return "Dynamic"
     * @see org.ocap.hardware.device.VideoOutputConfiguration#getName()
     */
    public String getName()
    {
        return "Dynamic";
    }

    /**
     * Add a desired input video resolution to output video resolution mapping.
     * If this configuration is
     * {@link VideoOutputSettings#setOutputConfiguration applied} successfully,
     * the video output port would use the given output resolution whenever the
     * main video input resolution matched the given input resolution.
     * <p>
     * The desired video output resolution is specified as an instance of
     * <code>FixedVideoOutputConfiguration</code>. Valid configurations are
     * those returned by
     * {@link VideoOutputSettings#getSupportedConfigurations()} for a given
     * video output port instance. This class does not guard against addition of
     * an invalid video resolution configuration. Instead, the instance of
     * <code>DynamicVideoOutputConfiguration</code> would be rejected by the
     * video output port.
     * <p>
     * For a given input resolution, wildcard values may be specified for some
     * attributes. The following table documents accepted wildcard (or
     * "don't care") values.
     * <table border>
     * <tr>
     * <th>Attribute</th>
     * <th>Wildcard value</th>
     * </tr>
     * <tr>
     * <td>{@link VideoResolution#getPixelResolution()}</td>
     * <td><code>null</code></td>
     * </tr>
     * <tr>
     * <td>{@link VideoResolution#getAspectRatio()}</td>
     * <td><code>{@link VideoFormatControl#ASPECT_RATIO_UNKNOWN}</code></td>
     * </tr>
     * <tr>
     * <td>{@link VideoResolution#getRate()}</td>
     * <td><code><= 0.0F</code></td>
     * </tr>
     * <tr>
     * <td>{@link VideoResolution#getScanMode()}</td>
     * <td><code>{@link VideoResolution#SCANMODE_UNKNOWN}</code></td>
     * </tr>
     * </table>
     * 
     * @param inputResolution
     *            The given input video resolution. May be an
     *            application-created instance of <code>VideoResolution</code>;
     *            or one of {@link #INPUT_SD} or {@link #INPUT_HD} may be
     *            specified to indicate a wildcard value covering all SD or HD
     *            resolutions.
     * @param outputResolution
     *            The desired output configuration that video of the given input
     *            resolution should be mapped.
     * 
     * @see #getOutputResolution
     */
    public void addOutputResolution(VideoResolution inputResolution, FixedVideoOutputConfiguration outputResolution)
    {
        resolutionMap.put(inputResolution, outputResolution);
    }

    /**
     * Get the output configuration that should be applied for the given input
     * resolution if this configuration were successfully set on a video output
     * port.
     * 
     * @param inputResolution
     *            The given input video resolution. May be an
     *            application-created instance of <code>VideoResolution</code>;
     *            or one of {@link #INPUT_SD} or {@link #INPUT_HD} may be
     *            specified to indicate a wildcard value covering all SD or HD
     *            resolutions.
     * @return The output video configuration mapped to by the given input
     *         resolution or <code>null</code> if no mapping is defined.
     * 
     * @see #addOutputResolution
     */
    public FixedVideoOutputConfiguration getOutputResolution(VideoResolution inputResolution)
    {
        Iterator outputResIter = resolutionMap.values().iterator();
        if (!outputResIter.hasNext()) return null;

        // special cases, lets look for INPUT_HD or INPUT_SD. In
        // OC-SP-OCAP1.0.2-080314 HD is defined as a video
        // resolution of greater than or equal to 1920x1080. // TODO_DS, Find if
        // HD/SD is defined anywhere
        if (inputResolution == INPUT_HD || inputResolution == INPUT_SD)
        {
        	//look for defined mapping first
        	if(this.resolutionMap.containsKey(inputResolution)){
        		return (FixedVideoOutputConfiguration) this.resolutionMap.get(inputResolution);
        	}
        	
            int width = 1920;
            int height = 1080;

            while (outputResIter.hasNext())
            {
                FixedVideoOutputConfiguration outputResolution = (FixedVideoOutputConfiguration) outputResIter.next();
                Dimension dim = outputResolution.getVideoResolution().getPixelResolution();
                if (inputResolution == INPUT_HD)
                {
                    if (dim.height >= height && dim.width >= width)
                    {
                        return outputResolution;
                    }
                }
                else
                {
                    if (dim.height < height && dim.width < width)
                    {
                        return outputResolution;
                    }
                }
            }
        }
        else
        {
            while (outputResIter.hasNext())
            {
                FixedVideoOutputConfiguration fixedOutputResolution = (FixedVideoOutputConfiguration) outputResIter.next();
                VideoResolution outputResolution = fixedOutputResolution.getVideoResolution();

                if (isMatch(inputResolution, outputResolution))
                {
                    return fixedOutputResolution;
                }
            }
        }
        return null;
    }

    private boolean isMatch(VideoResolution input, VideoResolution output)
    {
        // get the desired values (which can have wildcard values)
        int inAspectRatio = input.getAspectRatio(); // VideoFormatControl.ASPECT_RATIO_UNKNOWN
                                                    // == wildcard
        Dimension inPixel = input.getPixelResolution(); // null == wildcard
        float inRate = input.getRate(); // <= 0.0F == wildcard
        int inScanMode = input.getScanMode(); // VideoResolution.SCANMODE_UNKNOWN
                                              // == wildcard

        // if a wildcard (anything matches) OR there is an actual match, then
        // move to the next criteria,
        // otherwise this is not a match and return false

        if (inAspectRatio == VideoFormatControl.ASPECT_RATIO_UNKNOWN || inAspectRatio == output.getAspectRatio())
        {
            ; // match, go check the next condition
        }
        else
            return false;

        if (inPixel == null || (output.getPixelResolution() != null && inPixel.equals(output.getPixelResolution())))
        {
            ; // match, go check the next condition
        }
        else
            return false;

        if (inRate <= 0.0F || inRate == output.getRate())
        {
            ; // match, go check the next condition
        }
        else
            return false;

        if (inScanMode == VideoResolution.SCANMODE_UNKNOWN || inScanMode == output.getScanMode())
        {
            ; // match, go check the next condition
        }
        else
            return false;

        // all conditions match, return true
        return true;

    }

    /**
     * Get the set of input video resolutions for which a mapping to output
     * configuration is specified.
     * 
     * @return A non-<code>null</code> <code>Enumeration</code> of
     *         <code>VideoResolution</code> instances.
     */
    public Enumeration getInputResolutions()
    {
        return this.resolutionMap.keys();
    }
}
