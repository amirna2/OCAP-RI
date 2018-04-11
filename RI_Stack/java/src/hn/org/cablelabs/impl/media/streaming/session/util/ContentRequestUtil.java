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
package org.cablelabs.impl.media.streaming.session.util;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.cablelabs.impl.media.streaming.exception.HNStreamingException;
import org.cablelabs.impl.media.streaming.session.ContentRequest;
import org.cablelabs.impl.media.streaming.session.data.HNStreamProtocolInfo;

/**
 * @author Parthiban Balasubramanian
 * 
 */

public class ContentRequestUtil
{
    private static final Logger log = Logger.getLogger(ContentRequest.class);

    private ContentRequestUtil()
    {

    }

    /**
     * Returns an integer representing chunked encoding mode based on value
     * being passed
     * 
     * @param value
     *            - value to get back a chunk encoding mode integer
     * @return - Integer representing chunk encoding mode.
     */
    public static int convertChunkedEncodingModeString(String value)
    {
        if (ContentRequestConstant.HTTP_HEADER_GOP_STR.equals(value))
        {
            return ContentRequestConstant.HTTP_HEADER_CEM_GOP;
        }
        if (ContentRequestConstant.HTTP_HEADER_FRAME_STR.equals(value))
        {
            return ContentRequestConstant.HTTP_HEADER_CEM_FRAME;
        }
        if (ContentRequestConstant.HTTP_HEADER_OTHER_STR.equals(value))
        {
            return ContentRequestConstant.HTTP_HEADER_CEM_OTHER;
        }
        throw new IllegalArgumentException("Unknown chunked encoding value: " + value);

    }

    public static int convertFrameTypesInTrickModeString(String value)
    {
        // Note: HNP Spec says header values are case sensitive
        if (ContentRequestConstant.HTTP_HEADER_TRICK_MODE_FRAME_TYPE_I_STR.equals(value))
        {
            return ContentRequestConstant.HTTP_HEADER_TRICK_MODE_FRAME_TYPE_I;
        }
        if (ContentRequestConstant.HTTP_HEADER_TRICK_MODE_FRAME_TYPE_IP_STR.equals(value))
        {
            return ContentRequestConstant.HTTP_HEADER_TRICK_MODE_FRAME_TYPE_IP;
        }
        if (ContentRequestConstant.HTTP_HEADER_TRICK_MODE_FRAME_TYPE_ALL_STR.equals(value))
        {
            return ContentRequestConstant.HTTP_HEADER_TRICK_MODE_FRAME_TYPE_ALL;
        }
        throw new IllegalArgumentException("Unknown frame types in trick mode: " + value);

    }

    public static String formatFrameTypesInTrickMode(int value)
    {
        // Note: HNP Spec says header values are case sensitive
        if (value == ContentRequestConstant.HTTP_HEADER_TRICK_MODE_FRAME_TYPE_I)
        {
            return ContentRequestConstant.HTTP_HEADER_TRICK_MODE_FRAME_TYPE_I_STR;
        }
        if (value == ContentRequestConstant.HTTP_HEADER_TRICK_MODE_FRAME_TYPE_IP)
        {
            return ContentRequestConstant.HTTP_HEADER_TRICK_MODE_FRAME_TYPE_IP_STR;
        }
        if (value == ContentRequestConstant.HTTP_HEADER_TRICK_MODE_FRAME_TYPE_ALL)
        {
            return ContentRequestConstant.HTTP_HEADER_TRICK_MODE_FRAME_TYPE_ALL_STR;
        }
        throw new IllegalArgumentException("Unknown frame types in trick mode: " + value);
    }

    /**
     * Format string or return null if mediaTimeMS is -1 (for example, end time)
     * 
     * @param mediaTimeMS
     * @return NPT formatted string
     */
    public static String formatNPT(final long mediaTimeMS)
    {
        if (mediaTimeMS == -1)
        {
            return null;
        }
        String millis = Integer.toString((int) mediaTimeMS % 1000);
        final long time = mediaTimeMS / 1000;
        String seconds = Integer.toString((int) (time % 60));
        String minutes = Integer.toString((int) ((time % 3600) / 60));
        String hours = Integer.toString((int) (time / 3600));
        if (millis.length() == 1)
        {
            millis = "00" + millis;
        }
        else if (millis.length() == 2)
        {
            millis = "0" + millis;
        }
        if (seconds.length() < 2)
        {
            seconds = "0" + seconds;
        }
        if (minutes.length() < 2)
        {
            minutes = "0" + minutes;
        }
        // hours does not have to be 2 digits
        return hours + ":" + minutes + ":" + seconds + "." + millis;
    }

    /**
     * Get the list of parameters provided in the URI
     * 
     * Note: we want to use our local URI, not the one in the HTTP request
     */
    public static Map getQueryParameters(final String uri)
    {
        HashMap queryParameters = new HashMap();
        if (uri == null)
        {
            return queryParameters;
        }
        int paramIdx = uri.indexOf('?');
        if (paramIdx < 0)
        {
            return queryParameters;
        }
        while (paramIdx > 0)
        {
            int eqIdx = uri.indexOf('=', (paramIdx + 1));
            String name = uri.substring(paramIdx + 1, eqIdx);
            int nextParamIdx = uri.indexOf('&', (eqIdx + 1));
            String value = uri.substring(eqIdx + 1, (0 < nextParamIdx) ? nextParamIdx : uri.length());
            queryParameters.put(name, value);
            paramIdx = nextParamIdx;
        }
        return queryParameters;
    }

    public static Boolean parseServerSidePacing(String value) throws HNStreamingException
    {
        if (value != null)
        {
            try
            {
                StringTokenizer tokenizer = new StringTokenizer(value.trim(), "=");
                // first token is pacing, second token is =, third is value
                if (ContentRequestConstant.PACING.equals(tokenizer.nextToken().trim()))
                {
                    boolean result;
                    String pacingString = tokenizer.nextToken();
                    if (ContentRequestConstant.PACING_YES.equals(pacingString) || ContentRequestConstant.PACING_NO.equals(pacingString))
                    {
                        result = ContentRequestConstant.PACING_YES.equals(pacingString);
                        if (log.isDebugEnabled())
                        {
                            log.debug("using serversidepacedstreaming header - value: " + result
                                    + ", invalid pacing string value: " + pacingString);
                        }
                        return new Boolean(result);
                    }
                    else
                    {
                        String errMsg = "Invalid header format for serversidepacedstreaming: " + value;
                        throw new HNStreamingException(errMsg);
                    }
                }
                else
                {
                    String errMsg = "Invalid header format for serversidepacedstreaming: " + value
                            + ", missing pacing string";
                    throw new HNStreamingException(errMsg);
                }
            }
            catch (NoSuchElementException nsee)
            {
                String errMsg = "Invalid header format for serversidepacedstreaming: " + value;
                throw new HNStreamingException(errMsg);
            }
        }
        return null;
    }

    public static Integer parseMaxFramesPerGOP(String value) throws HNStreamingException
    {
        if (value != null)
        {
            try
            {
                StringTokenizer tokenizer = new StringTokenizer(value.trim(), "=");
                // first token is frames, second token is =, third is value
                if (ContentRequestConstant.FRAMES.equals(tokenizer.nextToken().trim()))
                {
                    int result = Integer.parseInt(tokenizer.nextToken().trim());
                    if (log.isDebugEnabled())
                    {
                        log.debug("using maxframespergop header - value: " + result);
                    }
                    return new Integer(result);
                }
                else
                {
                    String errMsg = "invalid maxframespergop header: " + value;
                    throw new HNStreamingException(errMsg);
                }
            }
            catch (NumberFormatException e)
            {
                String errMsg = "invalid maxframespergop header: " + value;
                throw new HNStreamingException(errMsg);
            }
            catch (NoSuchElementException nsee)
            {
                String errMsg = "invalid maxframespergop header: " + value;
                throw new HNStreamingException(errMsg);
            }
        }
        return null;
    }

    public static Long parseMaxTrickModeBandwidth(String value) throws HNStreamingException
    {
        if (value != null)
        {
            try
            {
                StringTokenizer tokenizer = new StringTokenizer(value.trim(), "=");
                // first token is bandwidth, second token is =, third is value
                if (ContentRequestConstant.BANDWIDTH.equals(tokenizer.nextToken().trim()))
                {
                    long result = Long.parseLong(tokenizer.nextToken().trim());
                    if (log.isDebugEnabled())
                    {
                        log.debug("using maxtrickmodebandwidth header - value: " + result);
                    }
                    return new Long(result);
                }
                else
                {
                    String errMsg = "Invalid header format for maxtrickmodebandwidth: " + value;
                    throw new HNStreamingException(errMsg);
                }
            }
            catch (NumberFormatException e)
            {
                String errMsg = "Invalid header format for maxtrickmodebandwidth: " + value;
                throw new HNStreamingException(errMsg);
            }
            catch (NoSuchElementException nsee)
            {
                String errMsg = "Invalid header format for maxtrickmodebandwidth: " + value;
                throw new HNStreamingException(errMsg);
            }
        }
        return null;
    }

    public static Integer parseCurrentDecodePTS(String value) throws HNStreamingException
    {
        if (value != null)
        {
            try
            {
                StringTokenizer tokenizer = new StringTokenizer(value.trim(), "=");
                // first token is PTS, second token is =, third is value
                if (ContentRequestConstant.PTS.equals(tokenizer.nextToken().trim()))
                {
                    // not prefixed by 0x
                    int result = Integer.parseInt(tokenizer.nextToken().trim(), ContentRequestConstant.HEX_RADIX);
                    if (log.isDebugEnabled())
                    {
                        log.debug("using currentdecodepts header - value: " + result);
                    }
                    return new Integer(result);
                }
                else
                {
                    String errMsg = "invalid currentdecodepts header: " + value;
                    throw new HNStreamingException(errMsg);
                }
            }
            catch (NumberFormatException e)
            {
                String errMsg = "invalid currentdecodepts header: " + value;
                throw new HNStreamingException(errMsg);
            }
            catch (NoSuchElementException nsee)
            {
                String errMsg = "invalid currentdecodepts header: " + value;
                throw new HNStreamingException(errMsg);
            }
        }
        return null;
    }

    /**
     * Retrieve timeSeekRange. End value will be -1 if end was not present
     * 
     * @param value
     * @return
     * @throws HNStreamingException
     */
    public static Integer[] parseTimeSeekRangeDlnaOrg(String value) throws HNStreamingException
    {
        // The time seek range header can have two different formats
        // Either:
        // "ntp = 1*DIGIT["."1*3DIGIT]
        // ntp sec = 0.232, or 1 or 15 or 16.652 (leading at one or more digits,
        // optionally followed by decimal point and 3 digits)
        // OR
        // "npt=00:00:00.000" where format is HH:MM:SS.mmm (hours, minutes,
        // seconds, milliseconds)
        // 
        // Header can include an optional end time
        //
        int startMillis = -1;
        int endMillis = -1;

        if (value != null)
        {
            try
            {
                if (log.isDebugEnabled())
                {
                    log.debug("parseTimeSeekRangeDlnaOrg() - called with " + value);
                }

                // Create tokenizer with '=' as deliminator
                StringTokenizer tokenizer = new StringTokenizer(value.trim(), "=");

                // Get the first part of the string which should be 'npt'=
                String hdrValueStr = (String) (tokenizer.nextElement());
                if (ContentRequestConstant.NPT.equals(hdrValueStr.trim()))
                {
                    // Get the remaining portion of the string which is actual
                    // value
                    String valueStr = tokenizer.nextToken().trim();

                    // Get the start string and end string if specified,
                    // separated by -
                    tokenizer = new StringTokenizer(valueStr, "-");

                    // if the token count is one, then check if the mandatory
                    // start time is available
                    if (tokenizer.countTokens() == 1 && valueStr.indexOf("-") == 0)
                    {
                        String errMsg = "Invalid header value for TimeSeekRange.dlna.org, mandatory Start time value is missing in npt="
                                + valueStr;
                        throw new HNStreamingException(errMsg);
                    }

                    // Start string must be supplied
                    String startStr = tokenizer.nextToken().trim();

                    // Determine if there was an end string specified
                    String endStr = null;
                    if (tokenizer.hasMoreTokens())
                    {
                        endStr = tokenizer.nextToken().trim();
                    }

                    // If the string contains ':', then the format is
                    // npt=00:00:00.000-
                    if (startStr.indexOf(":") > -1)
                    {
                        // Parse out starting time in hour min sec format
                        startMillis = parseTimeSeekRangeHMS(startStr);
                    }
                    else
                    // Assume format is npt=0.0 or npt=0-
                    {
                        // Parse out starting time in secs format
                        startMillis = parseTimeSeekRangeSecs(startStr);
                    }

                    // If the end string is not null
                    if (endStr != null)
                    {
                        // if end string contains ':', then the format is
                        // npt=00:00:00.000
                        if (endStr.indexOf(":") > -1)
                        {
                            // Parse out starting time in hour min sec format
                            endMillis = parseTimeSeekRangeHMS(endStr);
                        }
                        else
                        // Assume format is npt=0.0 or npt=0
                        {
                            // Parse out starting time in secs format
                            endMillis = parseTimeSeekRangeSecs(endStr);
                        }
                    }
                }
                else
                {
                    String errMsg = "Invalid header format for timeseekrangedlnaorg, no npt included: " + value
                            + ", valueStr = " + hdrValueStr.trim();
                    throw new HNStreamingException(errMsg);
                }
            }
            catch (NumberFormatException nfe)
            {
                String errMsg = "Invalid header format for timeseekrangedlnaorg";
                throw new HNStreamingException(errMsg, nfe);
            }
            catch (NoSuchElementException nsee)
            {
                String errMsg = "Invalid header format for timeseekrangedlnaorg";
                throw new HNStreamingException(errMsg, nsee);
            }

            return new Integer[] { new Integer(startMillis), new Integer(endMillis) };
        }
        else
        {
            return null;
        }
    }

    public static int parseTimeSeekRangeHMS(String hmsStr)
    {
        if (log.isDebugEnabled())
        {
            log.debug("parseTimeSeekRangeHMS() - called with " + hmsStr);
        }
        int parsedMillis = -1;
        StringTokenizer tokenizer = new StringTokenizer(hmsStr, ":");
        int hours = Integer.parseInt(tokenizer.nextToken().trim());

        int mins = 0;
        if (tokenizer.hasMoreTokens())
        {
            mins = Integer.parseInt(tokenizer.nextToken().trim());
        }

        String thirdStr = tokenizer.nextToken().trim();
        tokenizer = new StringTokenizer(thirdStr, ".");
        // If milli second is three digit then take the value as
        // it is. Else add appropriate number of zeros after the current value.
        // This fix is to avoid scenario where the start time is sent in the
        // request as 54.84 and the resultant parsedMillis is formed as 54.084
        // instead of 54.840
        String secs = "0";
        if (tokenizer.hasMoreTokens())
        {
            secs = tokenizer.nextToken().trim();
        }

        String msecs = "000";
        if (tokenizer.hasMoreTokens())
        {
            String msStr = tokenizer.nextToken().trim();
            StringTokenizer newTokenizer = new StringTokenizer(msStr, "-");
            msecs = newTokenizer.nextToken().trim();
        }

        int secsWithMills = (int) (Float.parseFloat(secs.concat(".").concat(msecs)) * 1000);
        parsedMillis = (hours * 60 * 60 * 1000) + (mins * 60 * 1000) + secsWithMills;
        if (log.isDebugEnabled())
        {
            log.debug("parseTimeSeekRangeHMS() - string: " + hmsStr + ", millis: " + parsedMillis);
        }

        return parsedMillis;
    }

    public static int parseTimeSeekRangeSecs(String secsStr)
    {
        if (log.isDebugEnabled())
        {
            log.debug("parseTimeSeekRangeSecs() - called with " + secsStr);
        }
        int parsedMillis = -1;
        String secondsStr = "0";
        String millisStr = "000";
        StringTokenizer tokenizer = new StringTokenizer(secsStr, ".");
        if (tokenizer.hasMoreElements())
        {
            secondsStr = tokenizer.nextToken().trim();
            if (tokenizer.hasMoreTokens())
            {
                String msStr = tokenizer.nextToken().trim();
                StringTokenizer newTokenizer = new StringTokenizer(msStr, "-");
                millisStr = newTokenizer.nextToken().trim();
            }
        }
        // If milli second is three digit then take the value as
        // it is. Else add appropriate number of zeros after the current value.
        // This fix is to avoid scenario where the start time is sent in the
        // request as 54.84 and the resultant parsedMillis is formed as 54.084
        // instead of 54.840
        parsedMillis = (int) (Float.parseFloat(secondsStr.concat(".").concat(millisStr)) * 1000);
        if (log.isDebugEnabled())
        {
            log.debug("parseTimeSeekRangeSecs() - millis: " + parsedMillis);
        }
        return parsedMillis;
    }

    /**
     * Parse the range..returns -1 for second array if end was not present.
     * 
     * @param value
     * @return
     * @throws HNStreamingException
     */
    public static long[] parseRange(String value) throws HNStreamingException
    {
        long[] bytePos = new long[2]; // example: "bytes=15669980-20669980 or
        // bytes=15669980-
        bytePos[0] = -1L;
        bytePos[1] = -1L;
        if (value != null)
        {
            try
            {
                StringTokenizer tokenizer = new StringTokenizer(value.trim(), "=");
                String firstStr = (String) (tokenizer.nextElement());
                if (ContentRequestConstant.BYTES.equals(firstStr.trim()))
                {
                    // get start byte position
                    String bytePosStr = tokenizer.nextToken().trim();
                    StringTokenizer newTokenizer = new StringTokenizer(bytePosStr, "-");
                    long parsedValue = Long.parseLong(newTokenizer.nextToken().trim());
                    if (bytePosStr.startsWith("-"))
                    {
                        bytePos[1] = parsedValue;
                    }
                    else
                    {
                        bytePos[0] = parsedValue;
                        if (newTokenizer.hasMoreTokens())
                        {
                            bytePos[1] = Long.parseLong(newTokenizer.nextToken().trim());
                        }
                    }

                    if (log.isInfoEnabled())
                    {
                        log.info("using start byte range header - bytes: " + bytePos[0]);
                    }
                    if (log.isInfoEnabled())
                    {
                        log.info("using end byte range header - bytes: " + bytePos[1]);
                    }
                }
                else
                {
                    String errMsg = "Invalid header format for ranged: " + value + ", firstStr = " + firstStr.trim();
                    throw new HNStreamingException(errMsg);
                }
            }
            catch (NumberFormatException nfe)
            {
                String errMsg = "Invalid header format for ranged: " + value;
                throw new HNStreamingException(errMsg);
            }
            catch (NoSuchElementException nsee)
            {
                String errMsg = "Invalid header format for ranged: " + value;
                throw new HNStreamingException(errMsg);
            }
        }
        else
        {
            return null;
        }
        return bytePos;
    }

    public static Float parsePlaySpeedDlnaOrg(String value) throws HNStreamingException
    {
        // header example: speed=1 1/2
        // examples: 1, 1/2, 2, -1, 1/10 (string representation of a rational
        // fraction)
        // won't be prefixed with a +, only a - if it's negative
        // may or may not have a fractional value..if it is, the whole number is
        // separated from the fraction by a space
        if (value != null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("getPlayspeedDlnaorg header - str value: " + value);
            }
            try
            {
                StringTokenizer tokenizer = new StringTokenizer(value, "=");
                if (ContentRequestConstant.SPEED.equals(tokenizer.nextToken().trim()))
                {
                    return HNStreamProtocolInfo.fractionToFloat(tokenizer.nextToken().trim());
                }
                else
                {
                    String errMsg = "Invalid header format for playspeeddlnaorg: " + value + ", missing speed string";
                    throw new HNStreamingException(errMsg);
                }
            }
            catch (NumberFormatException nfe)
            {
                String errMsg = "Invalid header format for playspeeddlnaorg: " + value;
                throw new HNStreamingException(errMsg);
            }
            catch (NoSuchElementException nsee)
            {
                String errMsg = "Invalid header format for playspeeddlnaorg: " + value;
                throw new HNStreamingException(errMsg);
            }
        }
        return null;
    }

    public static Integer parseMaxGOPsPerChunk(String value) throws HNStreamingException
    {
        if (value != null)
        {
            try
            {
                StringTokenizer tokenizer = new StringTokenizer(value.trim(), "=");
                // first token is gops, second token is =, third is value
                if (ContentRequestConstant.GOPS.equals(tokenizer.nextToken().trim()))
                {
                    int result = Integer.parseInt(tokenizer.nextToken().trim());
                    if (log.isDebugEnabled())
                    {
                        log.debug("using maxgopsperchunk header - value: " + result);
                    }
                    return new Integer(result);
                }
                else
                {
                    String errMsg = "Invalid header format for maxgopsperchunk: " + value;
                    throw new HNStreamingException(errMsg);
                }
            }
            catch (NumberFormatException nfe)
            {
                String errMsg = "Invalid header format for maxgopsperchunk: " + value;
                throw new HNStreamingException(errMsg);
            }
            catch (NoSuchElementException nsee)
            {
                String errMsg = "Invalid header format for maxgopsperchunk: " + value;
                throw new HNStreamingException(errMsg);
            }
        }
        else
        {
            return null;
        }
    }

    public static Integer parseFrameTypesInTrickMode(String value) throws HNStreamingException
    {
        if (value != null)
        {
            try
            {
                StringTokenizer tokenizer = new StringTokenizer(value.trim(), "=");
                // first token is frames, second token is =, third is value
                if (ContentRequestConstant.FRAMES.equals(tokenizer.nextToken().trim()))
                {
                    String result = tokenizer.nextToken().trim();
                    if (log.isDebugEnabled())
                    {
                        log.debug("using frametypesintrickmode header - value: " + result);
                    }
                    return new Integer(ContentRequestUtil.convertFrameTypesInTrickModeString(result));
                }
                else
                {
                    String errMsg = "Invalid header format for frametypesintrickmode: " + value;
                    throw new HNStreamingException(errMsg);
                }
            }
            catch (NoSuchElementException nsee)
            {
                String errMsg = "Invalid header format for frametypesintrickmode: " + value;
                throw new HNStreamingException(errMsg);
            }
        }
        else
        {
            return null;
        }
    }

    public static Integer parseChunkedEncodingMode(String value) throws HNStreamingException
    {
        if (value != null)
        {
            try
            {
                String result = null;
                StringTokenizer tokenizer = new StringTokenizer(value.trim(), "=");
                // first token is chunk, second token is =, third is value
                if (ContentRequestConstant.CHUNK.equals(tokenizer.nextToken().trim()))
                {
                    result = tokenizer.nextToken().trim();
                    if (log.isDebugEnabled())
                    {
                        log.debug("using chunkedencodingmode header - value: " + result);
                    }
                    return new Integer(ContentRequestUtil.convertChunkedEncodingModeString(result));
                }
                else
                {
                    String errMsg = "Invalid header format for chunkedencodingmode: " + result;
                    throw new HNStreamingException(errMsg);
                }
            }
            catch (NoSuchElementException nsee)
            {
                String errMsg = "Invalid header format for chunkedencodingmode";
                throw new HNStreamingException(errMsg, nsee);
            }
        }
        else
        {
            return null;
        }
    }

    public static boolean parseContentFeaturesDlnaOrg(String value) throws HNStreamingException
    {
        boolean requestedFeatures = false;
        if (value != null)
        {
            StringTokenizer tokenizer = new StringTokenizer(value.trim(), "=");

            // First token is value and has to be equal to 1 when included as
            // part of an incoming HTTP request. The 1 indicates include the
            // content features header in the response
            if (!"1".equals(tokenizer.nextToken().trim()))
            {
                String errMsg = "Invalid header value for contentFeatures: " + value;
                throw new HNStreamingException(errMsg);
            }

            // Supply the content features string to be used in the response
            // which should be the same as the 4th field in res@protocolInfo of
            // the content item

            // Set flag when formulating response to indicate content features
            // should be included
            requestedFeatures = true;
        }
        return requestedFeatures;
    }

    public static boolean parseAvailableSeekRangeDlnaOrg(String value) throws HNStreamingException
    {
        boolean requestedRange = false;
        if (value != null)
        {
            StringTokenizer tokenizer = new StringTokenizer(value.trim(), "=");

            // First token is value and has to be equal to 1 when included as
            // part of an incoming HTTP request. The 1 indicates include the
            // content features header in the response
            if (!"1".equals(tokenizer.nextToken().trim()))
            {
                String errMsg = "Invalid header value for contentFeatures: " + value;
                throw new HNStreamingException(errMsg);
            }

            // Set flag when formulating response to indicate available seek
            // range should be included
            requestedRange = true;
        }
        return requestedRange;
    }

    public static long getAlignedStartByte(final long startByte)
    {
        long tempStartByte;
        final long startAlignOffset = startByte % ContentRequestConstant.TS_PACKET_SIZE;
        if (startAlignOffset != 0)
        {
            tempStartByte = startByte - startAlignOffset;
            if (tempStartByte < 0)
            {
                tempStartByte = 0;
            }
        }
        else
        {
            tempStartByte = startByte;
        }
        return tempStartByte;
    }

    public static long getAlignedEndByte(final long startByte, final long endByte)
    {
        long tempEndByte;
        if (endByte > 0)
        {
            final long totalLength = endByte - startByte + 1;
            final long add = ContentRequestConstant.TS_PACKET_SIZE
                    - (totalLength % ContentRequestConstant.TS_PACKET_SIZE);
            tempEndByte = endByte + add;
        }
        else
        {
            tempEndByte = endByte;
        }
        return tempEndByte;
    }
}
