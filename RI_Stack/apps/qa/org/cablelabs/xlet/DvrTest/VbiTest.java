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
 * Created on Feb 5, 2007
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.cablelabs.xlet.DvrTest;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.reflect.Array;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.tv.service.selection.ServiceContext;
import javax.tv.xlet.XletContext;

import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceProxy;
import org.ocap.media.VBIFilter;
import org.ocap.media.VBIFilterEvent;
import org.ocap.media.VBIFilterGroup;
import org.ocap.media.VBIFilterListener;

import org.cablelabs.lib.utils.ArgParser;

/**
 * @author Ryan
 * 
 *         TODO To change the template for this generated type comment go to
 *         Window - Preferences - Java - Code Style - Code Templates
 */
public class VbiTest extends DvrTest
{

    private static final String FILTER_GROUP = "VBIGroup_";

    private static final String FILTER_PARAMS = "VBIFilter_";

    private static final String START_PARAMS = "VBIStart_";

    private static final String DATA_PARAMS = "VBIData_";

    private static final String RESET_PARAMS = "VBIGroupResetVal_";

    private static final int ALL_FILTERS = 99;

    private static final long MAX_VARIANCE = 5000;

    private static final int MAX_GROUP = 2;

    private boolean filterAttach = false;

    private ArgParser configProp;

    private VBIFilterGroup[] vbiGroup = null;

    private Vector[] vbiFilters = null;

    private String m_rcName = "";

    VbiTest(Vector locators)
    {
        super(locators);
        vbiGroup = new VBIFilterGroup[MAX_GROUP];
        vbiFilters = new Vector[MAX_GROUP];
        for (int i = 0; i < MAX_GROUP; i++)
        {
            vbiFilters[i] = new Vector();
        }
    }

    /*
     * --------------------------------------------------------------------------
     * ----------------
     * 
     * @author Ryan
     * 
     * Global methods to be used in the test group
     * ------------------------------
     * ------------------------------------------------------------
     */

    /*
     * Setup the argument parser
     */
    public void setupArgs()
    {
        // Setup the Argument parser
        System.out.println("<<<<<<setupArgs() - Seting up properites object to read file");
        try
        {
            // Get path name of config file.
            ArgParser xlet_args = new ArgParser(
                    (String[]) (DVRTestRunnerXlet.getContext()).getXletProperty(XletContext.ARGS));
            String str_config_file_name = xlet_args.getStringArg("config_file");

            // Count DVR source ids in config file.
            FileInputStream fis = new FileInputStream(str_config_file_name);
            configProp = new ArgParser(fis);
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
            System.out.println("<<<<<<Config file was not found");
            m_failedReason = "<<<<<<Config file was not found";
            m_failed = TEST_FAILED;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            m_failedReason = "<<<<<<Exception occuring in setupArgs()";
            m_failed = TEST_FAILED;
        }
    }

    /*
     * Detach the filter group created
     */
    public void detachGroup(int group)
    {
        vbiGroup[group].detach();
        filterAttach = false;
        System.out.println("<<<<<<<<detachGroup() - Detached VBI Group");
    }

    /*
     * Method that stops just one filter int pramenter specified the Filter
     */
    public void stopVBIFilter(int group, int filterNum)
    {
        System.out.println("<<<<<< stopVBIFilter() - Stopping filter " + filterNum);
        VBIFilter filter = (VBIFilter) vbiFilters[group].elementAt(filterNum);
        // Stop the filter
        filter.stopFiltering();
    }

    /*
     * Enable one specific filter in the Vector list
     */
    public void startVBIFilter(int filterParam, int groupParam, int group)
    {
        int offset = 0;
        byte[] posFilterDef = null;
        byte[] posFilterMask = null;
        byte[] negFilterDef = null;
        byte[] negFilterMask = null;

        System.out.println("<<<<<< startVBIFilter - Starting filter " + filterParam + " for selected group "
                + groupParam);
        VBIFilter filter = (VBIFilter) vbiFilters[group].elementAt(filterParam);
        // Create specific App Data for the filter
        String AppData = "<<<<<Filter_" + groupParam + "_" + filterParam;
        System.out.println("<<<<< AppData created: " + "Filter_" + groupParam + "_" + filterParam);
        // Start the filter
        try
        {
            System.out.println("<<<<<< Start Filter specified");
            String filterArg = configProp.getStringArg(START_PARAMS + groupParam + "_" + filterParam);
            StringTokenizer st = new StringTokenizer(filterArg, ",");
            offset = Integer.parseInt((String) st.nextToken());
            posFilterDef = toBinArray((String) st.nextToken());
            negFilterDef = toBinArray((String) st.nextToken());
            posFilterMask = toBinArray((String) st.nextToken());
            negFilterMask = toBinArray((String) st.nextToken());
            filter.startFiltering(AppData, offset, posFilterDef, posFilterMask, negFilterDef, negFilterMask);
        }
        catch (Exception e)
        {
            System.out.println("<<<<<< No Start Filter parameters specified");
            filter.startFiltering(AppData);

        }
    }

    /*
     * From an event, discover the filter from where the event has originated
     * from Returns the integer in the Vector list where the VBI filter is
     * located
     */
    public int findFilterToEvent(VBIFilterEvent event)
    {
        // Get the appdata of the event
        System.out.println("<<<<<Retrieving app data");
        String appdata = (String) event.getAppData();
        StringTokenizer data = new StringTokenizer(appdata, "_");
        String param = data.nextToken();
        param = data.nextToken();
        param = data.nextToken();
        // Pull out the filter parameter from the appdata
        int filterParam = Integer.parseInt(param);
        System.out.println("<<<<<Event from filter " + filterParam);
        return filterParam;
    }

    /*
     * From an event, discover the group number associated from the config file
     * from where the event had constructed from. Returns the integer the
     * assocaited group #
     */
    public int findGroupNumToEvent(VBIFilterEvent event)
    {
        // Get the appdata of the event
        System.out.println("<<<<<Retrieving app data");
        String appdata = (String) event.getAppData();
        StringTokenizer data = new StringTokenizer(appdata, "_");
        String param = data.nextToken();
        param = data.nextToken();
        // Pull out the filter parameter from the appdata
        int filterParam = Integer.parseInt(param);
        System.out.println("<<<<<Event from filter " + filterParam);
        return filterParam;
    }

    /*
     * From an event, retrieves the string name of the Service Context source
     */
    public String findSourceToEvent(VBIFilterEvent event)
    {
        System.out.println("<<<<<<Event from source " + event.getSource().toString());
        return event.getSource().toString();
    }

    /*
     * Converts a Sring into a byte array
     */
    public byte[] toBinArray(String hexStr)
    {
        byte bArray[] = new byte[hexStr.length() / 2];
        for (int i = 0; i < bArray.length; i++)
        {
            bArray[i] = (byte) Integer.parseInt(hexStr.substring(2 * i, 2 * (i + 1)), 16);
            System.out.println("<<<<<<Returning byte[" + i + "] : " + bArray[i]);
        }
        return bArray;
    }

    /**
     * Retrieves data under the "VBIData_" header in String form, creates byte
     * arrays and deliver the byte arrays in Vector form.
     */
    private Vector getByteData(int groupParam, int filterParam)
    {
        Vector dataArray = null;
        String dataArg = null;
        try
        {
            dataArg = configProp.getStringArg(DATA_PARAMS + groupParam + "_" + filterParam);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            m_failedReason = "<<<<<<Exception on reading data at getByteData()";
            m_failed = TEST_FAILED;
        }
        StringTokenizer data = new StringTokenizer(dataArg, ",");
        dataArray = new Vector();

        // countTokens() cannot be reffered to directly
        int tokens = data.countTokens();
        System.out.println("Number of tokens: " + tokens);
        for (int i = 0; i < tokens; i++)
        {
            byte[] dataBytes = toBinArray(data.nextToken());
            System.out.println("<<<<<<<Data element " + i + " being parsed : " + dataBytes);
            dataArray.addElement(dataBytes);
        }
        return dataArray;
    }

    /**
     * Retrieves information from the config file used to reset the notification
     * methods to a specific value
     */
    public long[] getParamsForReset(int group)
    {
        String argsString = null;
        long[] args = new long[2];
        try
        {
            argsString = configProp.getStringArg(RESET_PARAMS + group);
        }
        catch (Exception e)
        {
            m_failed = TEST_FAILED;
            System.out.println("<<<<<<FAILED: Unable to find " + group);
            m_failedReason = "<<<<<<FAILED: Unable to find " + group;
            m_failed = TEST_FAILED;
            e.printStackTrace();
        }
        StringTokenizer st = new StringTokenizer(argsString, ",");

        // This argument will either be as value that represents a
        // an offset in scheduling a reset value or the maximum # of
        // data units that could be aquired
        args[0] = Long.parseLong((String) st.nextToken());
        System.out.println("getParamsforReset : offset or max " + args[0]);

        // This argument wiil represent the resent value
        args[1] = Long.parseLong((String) st.nextToken());
        System.out.println("getParamsForReset : reset value: " + args[1]);
        return args;
    }

    /**
     * Verifies that the current time passed is within the MAX_VARIANCE
     * discrepancy
     */
    public void verifyEventTime(long time)
    {
        long now = System.currentTimeMillis();
        long diff = Math.abs(time - now);
        if (diff > MAX_VARIANCE)
        {
            m_failed = TEST_FAILED;
            DVRTestRunnerXlet.log("<<<<FAILED :: verifyEventTime - time not within bounds ; was " + now + " should be "
                    + time);
            m_failedReason = "<<<<FAILED :: verifyEventTime - time not within bounds ; was " + now + " should be "
                    + time;
        }
    }

    /**
     * From one filter, dumps out the data, finds the number of complete units
     * from the given key that is passed into the method
     */
    public int getUnitsReceived(int filter, int group, byte key)
    {
        byte[] data = ((VBIFilter) vbiFilters[group].elementAt(filter)).getVBIData();
        int units = 0;
        for (int i = 0; i < data.length; i++)
        {
            if (data[i] == key)
            {
                ++units;
                System.out.println("!!!!!!Found unit " + units + "!!!!!");
            }
        }
        System.out.println(printBytes(data));
        return units;
    }

    /**
     * 
     * This function takes an array of bytes and creates a representation in
     * String form suitable for printing out to the console or to the screen
     * 
     * @param byteArray
     *            - byte arreay to be represented
     * @return - String output
     */
    public String printBytes(byte[] byteArray)
    {
        System.out.println("Returning bytes in string form for printing");
        StringBuffer byteString = new StringBuffer();
        for (int k = 0; k < byteArray.length; k++)
        {
            int data = byteArray[k] & 0xFF;
            byteString.append((Integer.toHexString(data) + " "));
        }
        return new String(byteString);
    }

    /*
     * --------------------------------------------------------------------------
     * -------------------
     * 
     * @author Ryan
     * 
     * Added VBI NotifyShell objects for automation
     * ------------------------------
     * ----------------------------------------------------------------
     */

    /*
     * 
     * @author Ryan
     * 
     * Creates a VBIFIlterGroup at the with the nuber of filters specified
     * within the config file, . Parameters for each filter is referenced from
     * the config file defined in the xlet context
     */
    public class createVBIFilterGroup extends EventScheduler.NotifyShell
    {

        private int m_filterGroupNum;

        private int m_group;

        createVBIFilterGroup(int filterGroupNum, int group, long time)
        {
            super(time);
            m_filterGroupNum = filterGroupNum;
            m_group = group;
        }

        public void ProcessCommand()
        {
            int filters = 0;
            int[] lineNum = new int[1];
            lineNum[0] = 0;
            int fieldData = 0;
            int dataFormat = 0;
            int unitLength = 0;
            int bufferSize = 0;
            String timeOut = null;
            String notifyDataUnits = null;
            String notifyTime = null;

            System.out.println("<<<<createVBIFilterGroup::ProcessCommand>>>>");

            // Call to refresh arguments
            setupArgs();

            try
            {
                // Get arguments in creating the filter group
                filters = configProp.getIntArg(FILTER_GROUP + m_filterGroupNum);
            }
            catch (Exception e)
            {
                System.out.println("<<<<<<FAILED: Unable to find " + FILTER_GROUP + m_filterGroupNum);
                m_failed = TEST_FAILED;
                m_failedReason = "<<<<<<FAILED: Unable to find " + FILTER_GROUP + m_filterGroupNum;
                e.printStackTrace();
            }

            // If a filter group is created, stop, detach, and clean if
            // necessary
            if (vbiGroup[m_group] != null)
            {
                System.out.println("<<<<<<VBI Group created, planning on disposing>>>>>");
                if (filterAttach)
                {
                    System.out.println("<<<<<<Detach the filter prior to disposal>>>>>>");
                    detachGroup(m_group);
                }
                System.out.println("<<<<<Removing filters>>>>>>");
                vbiFilters[m_group].removeAllElements();
                vbiGroup[m_group] = null;
            }

            System.out.println("<<<<<<<<<<Creating new filters>>>>>>>>>>");
            // Create the new filter group
            vbiGroup[m_group] = new VBIFilterGroup(filters);

            // Add group number to the list
            vbiFilters[m_group].addElement(new Integer(m_filterGroupNum));

            // Get arguments in creating the filters
            for (int i = 1; i < filters + 1; i++)
            {
                try
                {
                    System.out.println("<<<<<<<Retrieving data from file to contruct filter>>>>>>>");
                    String filterInfo = configProp.getStringArg(FILTER_PARAMS + m_filterGroupNum + "_" + i);
                    StringTokenizer st = new StringTokenizer(filterInfo, ",");
                    lineNum[0] = Integer.parseInt((String) st.nextToken());
                    fieldData = Integer.parseInt((String) st.nextToken());
                    dataFormat = Integer.parseInt((String) st.nextToken());
                    unitLength = Integer.parseInt((String) st.nextToken());
                    bufferSize = Integer.parseInt((String) st.nextToken());
                    timeOut = (String) st.nextToken();
                    notifyTime = (String) st.nextToken();
                    notifyDataUnits = (String) st.nextToken();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    m_failed = TEST_FAILED;
                    System.out.println("FAILED:Exception caught in reading filter data");
                    m_failedReason = "FAILED:Exception caught in reading filter data";
                }

                // Print out information
                System.out.println("<<<<<<line number : " + lineNum[0]);
                System.out.println("<<<<<<field data : " + fieldData);
                System.out.println("<<<<<<data format : " + dataFormat);
                System.out.println("<<<<<<data unit length in bits : " + unitLength);
                System.out.println("<<<<<<buffer size in bytes : " + bufferSize);
                System.out.println("<<<<<<timeout in milliseconds : " + timeOut);
                System.out.println("<<<<<<notification by data units : " + notifyDataUnits);
                System.out.println("<<<<<<notification by time : " + notifyTime);

                // Create the filter
                VBIFilter vbiFilter = vbiGroup[m_group].newVBIFilter(lineNum, fieldData, dataFormat, unitLength,
                        bufferSize);

                if (vbiFilter == null)
                {
                    m_failed = TEST_FAILED;
                    System.out.println("FAILED: Filter created is null");
                    m_failedReason = "FAILED: Filter created is null";
                }

                // Setup the notifications and timeouts
                if (notifyDataUnits.compareTo("NONE") != 0)
                {
                    vbiFilter.setNotificationByDataUnits(Integer.parseInt(notifyDataUnits));
                }
                if (notifyTime.compareTo("NONE") != 0)
                {
                    vbiFilter.setNotificationByTime(Long.parseLong(notifyTime));
                }
                if (timeOut.compareTo("NONE") != 0)
                {
                    vbiFilter.setTimeOut(Long.parseLong(timeOut));
                }

                // Add to the vbilist
                VBITestRunnerXlet.log("<<<<<Adding filter " + vbiFilter.toString() + " to vector index " + i);
                vbiFilters[m_group].addElement(vbiFilter);
            }
            System.out.println("<<<<<<<Vector list size : " + vbiFilters[m_group].size());

            System.out.println("<<<<createVBIFilterGroup::Done>>>>");
        }
    }

    /*
     * 
     * @author Ryan
     * 
     * Registers the listener for the refernece passed in of a
     * VBIFIlterListener. Defines in the constuctior is also which filter(s) the
     * listener(s) should be registered from.
     */
    public class registerVBIFilterListener extends EventScheduler.NotifyShell
    {

        private VBIFilterListener m_listener;

        private int m_filter;

        private int m_group;

        /**
         * @param time
         */
        registerVBIFilterListener(int filter, int group, VBIFilterListener listener, long time)
        {
            super(time);
            m_filter = filter;
            m_group = group;
            m_listener = listener;
        }

        public void ProcessCommand()
        {
            System.out.println("<<<<registerVBIFilterListener::ProcessCommand>>>>");
            try
            {
                if (m_filter == ALL_FILTERS)
                {
                    for (int i = 1; i < vbiFilters[m_group].size(); i++)
                    {
                        VBIFilter vbiFilter = (VBIFilter) vbiFilters[m_group].elementAt(i);
                        vbiFilter.addVBIFilterListener(m_listener);
                        System.out.println("<<<<<<adding listener to filter " + i);
                    }
                }
                else
                {
                    VBIFilter vbiFilter = (VBIFilter) vbiFilters[m_group].elementAt(m_filter);
                    vbiFilter.addVBIFilterListener(m_listener);
                    System.out.println("<<<<<<adding listener to filter " + m_filter);
                }
                System.out.println("<<<<registerVBIFilter::Done>>>>");
            }
            catch (Exception e)
            {
                e.printStackTrace();
                m_failed = TEST_FAILED;
                System.out.println("FAILED:Exception caught in registering VBI Filter Listener");
                m_failedReason = "FAILED:Exception caught in registering VBI Filter Listener";
            }
        }

    }

    /*
     * 
     * @author Ryan
     * 
     * Removes the listener from the refernece passed in of a VBIFIlterListener.
     * Defines in the constuctior is also which filter to remove the listenre
     * from.
     */
    public class removeVBIFilterListener extends EventScheduler.NotifyShell
    {

        private VBIFilterListener m_listener;

        private int m_filter;

        private int m_group;

        /**
         * @param time
         */
        removeVBIFilterListener(int filter, int group, VBIFilterListener listener, long time)
        {
            super(time);
            m_filter = filter;
            m_group = group;
            m_listener = listener;
        }

        public void ProcessCommand()
        {
            System.out.println("<<<<removeVBIFilter::ProcessCommand>>>>");
            try
            {
                if (m_filter == ALL_FILTERS)
                {
                    for (int i = 1; i < vbiFilters[m_group].size(); i++)
                    {
                        VBIFilter vbiFilter = (VBIFilter) vbiFilters[m_group].elementAt(i);
                        vbiFilter.removeVBIFilterListener(m_listener);
                        System.out.println("<<<<<<removing listener to filter " + i);
                    }
                }
                else
                {
                    VBIFilter vbiFilter = (VBIFilter) vbiFilters[m_group].elementAt(m_filter);
                    vbiFilter.removeVBIFilterListener(m_listener);
                    System.out.println("<<<<<<removing listener to filter " + m_filter);
                }
                System.out.println("<<<<removeVBIFilter::Done>>>>");
            }
            catch (Exception e)
            {
                e.printStackTrace();
                m_failed = TEST_FAILED;
                System.out.println("FAILED:Exception caught in removing VBI Filter Listener");
                m_failedReason = "FAILED:Exception caught in removing VBI Filter Listener";
            }
        }
    }

    /*
     * 
     * @author Ryan
     * 
     * Start up all or specific filters. Argument passed in determines which
     * filters are to start up. Parameters in config file used to set filtering
     * definitions at start
     */
    public class startVBIFilters extends EventScheduler.NotifyShell
    {

        private int m_filter;

        private int m_group;

        /**
         * @param time
         */
        startVBIFilters(int filter, int group, long time)
        {
            super(time);
            m_filter = filter;
            m_group = group;
        }

        public void ProcessCommand()
        {
            System.out.println("<<<<startVBIFilter::ProcessCommand>>>>");
            try
            {
                int selectedGroup = ((Integer) vbiFilters[m_group].elementAt(0)).intValue();
                if (m_filter == ALL_FILTERS)
                {
                    System.out.println("<<<<<<Start Filters for filter group " + selectedGroup);
                    // Go through one filter at a time
                    for (int i = 1; i < vbiFilters[m_group].size(); i++)
                    {
                        // Start the filters
                        startVBIFilter(i, selectedGroup, m_group);
                    }
                }
                // if within range of the vbiFilters array
                else if (m_filter > 0 || (m_filter < vbiFilters[m_group].size()))
                {
                    startVBIFilter(m_filter, selectedGroup, m_group);
                }
                // if not, notify that m_filter is not in range
                else
                {
                    System.out.println("!!!!!!!!!!m_filter not in range");
                }
                System.out.println("<<<<startVBIFilter::Done>>>>");
            }
            catch (Exception e)
            {
                e.printStackTrace();
                m_failed = TEST_FAILED;
                System.out.println("FAILED:Exception caught in starting VBI Filters");
                m_failedReason = "FAILED:Exception caught in starting VBI Filters";
            }
        }

    }

    /*
     * 
     * @author Ryan
     * 
     * Stopping all or specified VBIFilter. Argument passed in determines what
     * filter(s) is/are to be stopped.
     */
    public class stopVBIFilters extends EventScheduler.NotifyShell
    {

        private int m_filter;

        private int m_group;

        /**
         * @param time
         */
        stopVBIFilters(int filter, int group, long time)
        {
            super(time);
            m_filter = filter;
            m_group = group;
        }

        public void ProcessCommand()
        {
            System.out.println("<<<<stopVBIFilter::ProcessCommand>>>>");
            try
            {
                if (m_filter == ALL_FILTERS)
                {
                    System.out.println("<<<<<< Stopping filters");
                    // Go though the list of filters
                    for (int i = 1; i < vbiFilters[m_group].size(); i++)
                    {
                        // Stop the filter
                        stopVBIFilter(m_group, i);
                    }
                }
                else if (m_filter > 0 || (m_filter < vbiFilters[m_group].size()))
                {
                    stopVBIFilter(m_group, m_filter);
                }
                // if not, notify that m_filter is not in range
                else
                {
                    System.out.println("!!!!!!!!!!m_filter not in range");
                }
                System.out.println("<<<<stopVBIFilter::Done>>>>");
            }
            catch (Exception e)
            {
                e.printStackTrace();
                m_failed = TEST_FAILED;
                System.out.println("FAILED:Exception caught in stopping VBI Filters");
                m_failedReason = "FAILED:Exception caught in stopping VBI Filters";
            }
        }
    }

    /*
     * 
     * @author Ryan
     * 
     * Attaching the filter group created to the service Context defined with
     * DVRTestRunner. Depending on the contructor used, a differnt Resource
     * client can be used that the the Resource Client defined here. For
     * negative testing, the constructor can be defined to not have a Service
     * Context attached or reterieve by string name.
     */
    public class attachFilterGroup extends EventScheduler.NotifyShell
    {

        private ResourceClient m_rc;

        private boolean m_useSC = false;

        private boolean m_useRC = false;

        private String m_scName = null;

        private int m_group = 0;

        attachFilterGroup(boolean useRC, boolean useSC, long time)
        {
            super(time);
            m_useRC = useRC;
            m_useSC = useSC;
            m_rc = new DefaultResourceClient();
            m_scName = null;
        }

        attachFilterGroup(boolean useRC, boolean useSC, ResourceClient rc, long time)
        {
            super(time);
            m_useRC = useRC;
            m_useSC = useSC;
            m_rc = rc;
            m_scName = null;
        }

        attachFilterGroup(boolean useRC, String SCname, int group, long time)
        {
            super(time);
            m_useRC = useRC;
            m_useSC = false;
            m_rc = new DefaultResourceClient();
            m_scName = SCname;
            m_group = group;
        }

        public void ProcessCommand()
        {
            //
            // Attach the filter group created
            //
            ServiceContext serviceContext = null;
            ResourceClient resourceClient = null;
            System.out.println("<<<<attachFilterGroup::ProcessCommand>>>>");
            try
            {
                // Get the argumnets of attaching the filter group to the
                // Service Context
                if (m_useSC)
                {
                    System.out.println("<<<<<Using Service Context in DVRTestRunner");
                    serviceContext = m_serviceContext;
                }
                else if (m_scName != null)
                {
                    serviceContext = (ServiceContext) findObject(m_scName);
                }
                if (m_useRC)
                {
                    System.out.println("<<<<<Using DefaultResourceClient as Resource Client");
                    resourceClient = m_rc;
                    // Store away string name of resource client
                    m_rcName = m_rc.toString();
                }
                vbiGroup[m_group].attach(serviceContext, resourceClient, null);
                System.out.println("<<<<attachFilterGroup::Done>>>>");
            }
            catch (Exception e)
            {
                e.printStackTrace();
                m_failed = TEST_FAILED;
                System.out.println("FAILED:Exception caught in stopping VBI Filters");
                m_failedReason = "FAILED:Exception caught in stopping VBI Filters";
            }
        }
    }

    /*
     * 
     * @author Ryan
     * 
     * Detach the filter group from its Service Context
     */
    public class detachFilterGroup extends EventScheduler.NotifyShell
    {

        private int m_group;

        detachFilterGroup(int group, long time)
        {
            super(time);
            m_group = group;
            // TODO Auto-generated constructor stub
        }

        public void ProcessCommand()
        {
            System.out.println("<<<<detachFilterGroup::ProcessCommand>>>>");
            detachGroup(m_group);
            System.out.println("<<<<detachFilterGroup::Done>>>>");
        }

    }

    /*
     * 
     * @author Ryan
     * 
     * Dumps the data out of one filter and verifes if it matches any of the
     * strings in the string arrys passed through the constructor. Filter number
     * is defined in config file.
     */
    public class compareData extends EventScheduler.NotifyShell
    {

        private Vector m_data;

        private int m_filter;

        private int m_group;

        private int m_filterGroupNum;

        /**
         * @param time
         */
        compareData(int filter, int group, Vector data, long time)
        {
            super(time);
            m_data = data;
            m_filter = filter;
            m_group = group;
            m_filterGroupNum = 0;

        }

        compareData(int filter, int group, int filterGroupNum, long time)
        {
            super(time);
            m_data = null;
            m_filter = filter;
            m_group = group;
            m_filterGroupNum = filterGroupNum;
        }

        public void ProcessCommand()
        {

            System.out.println("<<<<compareData::ProcessCommand>>>>");

            // if data passed in is null then look to the config file
            if (m_data == null)
            {
                m_data = getByteData(m_filterGroupNum, m_filter);
            }

            // Go through one filter at a time
            System.out.println("<<<<<<Dumping out data from each filter");
            if (m_filter > 0 || (m_filter < vbiFilters[m_group].size()))
            {
                // Dunp the buffers
                System.out.println("<<<<<<Data dump of filter " + m_filter);
                VBIFilter vbiFilter = (VBIFilter) vbiFilters[m_group].elementAt(m_filter);
                byte[] vbiData = vbiFilter.getVBIData();
                // Set test to failed
                DVRTestRunnerXlet.log("<<<<<compareData::Setting to FAILED until match is found");
                m_failed = TEST_FAILED;
                for (int j = 0; j < m_data.size(); j++)
                {
                    // load in nth byte array
                    byte[] byteData = (byte[]) m_data.elementAt(j);
                    boolean match = true;
                    // do a byte for byte check
                    for (int i = 0; i < byteData.length; i++)
                    {
                        if (byteData[i] != vbiData[i])
                        {
                            match = false;
                        }
                    }
                    // Verify the rest of the buffer is null or has a partial
                    for (int i = byteData.length; i < vbiData.length; i++)
                    {
                        if (vbiData[i] != 0x00)
                        {
                            if (vbiData[i] != byteData[(i % byteData.length)])
                            {
                                match = false;
                            }
                        }
                    }
                    // if a fualt was not found in the check, test passes
                    if (match)
                    {
                        DVRTestRunnerXlet.log("<<<<<compareData::PASSED - Match found");
                        m_failed = TEST_PASSED;
                        break;
                    }
                }
                // Throw it out to the terminal and to UDP Logger
                DVRTestRunnerXlet.log("Data:" + (new String(vbiData)) + "\nByte Array:" + (printBytes(vbiData)));
            }
            System.out.println("<<<<compareData::Done>>>>");
        }
    }

    /*
     * 
     * @author Ryan
     * 
     * Dumps the data out of one filter and verifes if it matches any of the
     * strings in the string arrys passed through the constructor. Filter number
     * is defined in config file.
     */
    public class checkCRC extends EventScheduler.NotifyShell
    {
        private int m_filter;

        private int m_group;

        /**
         * @param time
         */
        checkCRC(int filter, int group, long time)
        {
            super(time);
            m_filter = filter;
            m_group = group;
        }

        public void ProcessCommand()
        {
            System.out.println("<<<<checkCRC::ProcessCommand>>>>");

            // temp value to calculating CRC
            byte sum = 0;
            // Go through one filter at a time
            System.out.println("<<<<<<Dumping out data from each filter");
            if (m_filter > 0 || (m_filter < vbiFilters[m_group].size()))
            {
                // Dunp the buffers
                System.out.println("<<<<<<Data dump of filter " + m_filter);
                VBIFilter vbiFilter = (VBIFilter) vbiFilters[m_group].elementAt(m_filter);
                byte[] vbiData = vbiFilter.getVBIData();
                // Sum the values in the data unit and see if it equals 0
                for (int i = 0; i < (vbiData.length); i++)
                {
                    sum += vbiData[i];
                }
                sum &= 0x7F;
                // If so we have a winner.
                if (sum != 0)
                {
                    m_failed = TEST_FAILED;
                    DVRTestRunnerXlet.log("<<<<TEST FAILED: Checksum does not equal 0");
                }
                else
                {
                    DVRTestRunnerXlet.log("<<<<TEST PASSED: Checksum does equal 0");
                }
                // Throw it out to the terminal and to UDP Logger
                System.out.println("Byte Array:" + (printBytes(vbiData)));
            }
            System.out.println("<<<<checkCRC::Done>>>>");
        }
    }

    /*
     * 
     * @author Ryan
     * 
     * Goes through and checks out the the data retrieved from the getter
     * methods of the VBIGroup object
     */
    public class checkVBIGroupInfo extends EventScheduler.NotifyShell
    {

        private int m_filterGroupNum;

        private int m_group;

        private int m_filter;

        /**
         * @param time
         */
        checkVBIGroupInfo(int filterGroupNum, int filter, int group, long time)
        {
            super(time);
            m_group = group;
            m_filter = filter;
            m_filterGroupNum = filterGroupNum;
        }

        public void ProcessCommand()
        {
            System.out.println("<<<<checkVBIGroupInfo::ProcessCommand>>>>");
            int[] lineNum = new int[1];
            lineNum[0] = 0;
            int dataFormat = 0;
            System.out.println("<<<<checkVBIGroupInfo::VBI Info dump>>>");

            // check to see if the names of the instances match
            if (m_rcName.compareTo(vbiGroup[m_group].getClient().toString()) != 0)
            {
                m_failed = TEST_FAILED;
                // generate the Resource Client by string name
                DVRTestRunnerXlet.log("<<<<checkVBIGroupInfo::TEST FAILED - Resource Client :: getClient() "
                        + vbiGroup[m_group].getClient().toString() + " vs. stored " + m_rcName);
                m_failedReason = "checkVBIGroupInfo::TEST FAILED - Resource Client :: getClient() "
                        + vbiGroup[m_group].getClient().toString() + " vs. stored " + m_rcName;
            }

            if (m_serviceContext != vbiGroup[m_group].getServiceContext())
            {
                m_failed = TEST_FAILED;
                // generate the Service Context by string name
                DVRTestRunnerXlet.log("<<<<<checkVBIGroupInfo::TEST FAILED - Service Context"
                        + vbiGroup[m_group].getServiceContext().toString());
                m_failedReason = "checkVBIGroupInfo::TEST FAILED - Service Context"
                        + vbiGroup[m_group].getServiceContext().toString();
            }

            System.out.println("<<<<<<<Retrieving data from file to contruct filter>>>>>>>");
            String filterInfo = null;
            try
            {
                filterInfo = configProp.getStringArg(FILTER_PARAMS + m_filterGroupNum + "_" + m_filter);
            }
            catch (Exception e)
            {
                m_failed = TEST_FAILED;
                m_failedReason = "checkVBIGroupInfo::Exception getting thrown " + e.toString();
                e.printStackTrace();
            }
            StringTokenizer st = new StringTokenizer(filterInfo, ",");
            lineNum[0] = Integer.parseInt((String) st.nextToken());
            st.nextToken();
            dataFormat = Integer.parseInt((String) st.nextToken());

            try
            {
                // generate a status if field mixed filtering is supported
                // with the current VBIFilterGroup with the given line numbers
                // and data format specified in the config file
                if (vbiGroup[m_group].getSeparatedFilteringCapability(lineNum, dataFormat))
                {
                    DVRTestRunnerXlet.log("getMixedFilteringCapability :: YES");
                }
                else
                {
                    DVRTestRunnerXlet.log("getMixedFilteringCapability :: NO");
                }
                // generate a status if field separated filtering is supported
                // with the current VBIFilterGroup with the given line numbers
                // and data format specified in the config file
                if (vbiGroup[m_group].getMixedFilteringCapability(lineNum, dataFormat))
                {
                    DVRTestRunnerXlet.log("\n getSeparatedFilteringCapability :: YES");
                }
                else
                {
                    DVRTestRunnerXlet.log("\n getSeparatedFilteringCapability :: NO");
                }
                // Show host support for SCTE20 and 21 compatibility
                if (vbiGroup[m_group].getSCTE20Capability())
                {
                    DVRTestRunnerXlet.log("\n getSCTE20Capability :: YES");
                }
                else
                {
                    DVRTestRunnerXlet.log("\n getSCTE20Capability :: NO");
                }
                if (vbiGroup[m_group].getSCTE21Capability())
                {
                    DVRTestRunnerXlet.log("\n getSCTE21Capability :: YES");
                }
                else
                {
                    DVRTestRunnerXlet.log("getSCTE21Capability :: NO");
                }
            }
            catch (Exception e)
            {
                m_failed = TEST_FAILED;
                m_failedReason = "checkVBIGroupInfo::Exception getting thrown " + e.toString();
                e.printStackTrace();
            }
            System.out.println("<<<<checkVBIGroupInfo::Done>>>>");
        }
    }

    /*
     * 
     * @author Ryan
     * 
     * Verifies that the amount of data in the buffer is of the correct legnth
     * This requires that the buffer of the filter contains one and only one
     * data unit.
     */

    public class checkUnitLength extends EventScheduler.NotifyShell
    {

        private int m_filterGroupNum;

        private int m_group;

        private int m_filter;

        /**
         * @param time
         */
        checkUnitLength(int filter, int group, int filterGroupNum, long time)
        {
            super(time);
            m_group = group;
            m_filterGroupNum = filterGroupNum;
            m_filter = filter;
        }

        public void ProcessCommand()
        {
            if (m_filter > 0 || (m_filter < vbiFilters[m_group].size()))
            {
                // Dunp the buffers
                System.out.println("<<<<<<Data dump of filter " + m_filter);
                VBIFilter vbiFilter = (VBIFilter) vbiFilters[m_group].elementAt(m_filter);
                byte[] vbiData = vbiFilter.getVBIData();

                // Get the buffer specs from the config file
                System.out.println("<<<<<<<Retrieving data from file to verify data");
                String filterInfo = null;
                try
                {
                    filterInfo = configProp.getStringArg(FILTER_PARAMS + m_filterGroupNum + "_" + m_filter);
                }
                catch (Exception e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                StringTokenizer st = new StringTokenizer(filterInfo, ",");
                st.nextToken();
                st.nextToken();
                int dataFormat = Integer.parseInt((String) st.nextToken());
                int unitLength = Integer.parseInt((String) st.nextToken());
                System.out.println("<<<<<<<Unit length is " + unitLength);

                // When VBI data format is not defined
                if (dataFormat == VBIFilterGroup.VBI_DATA_FORMAT_UNKNOWN)
                {
                    System.out.println("Field is an UNKNOWN field");
                    if ((vbiData.length * 8) != unitLength)
                    {
                        DVRTestRunnerXlet.log("<<<<<CheckNullData : FAILED - Length mismatch : length is "
                                + (vbiData.length * 8) + " should be " + unitLength + " >>>>");
                        m_failedReason = "CheckNullData : FAILED - Length mismatch : length is " + vbiData.length
                                + " should be " + unitLength;
                        m_failed = TEST_FAILED;
                    }
                }

                // When VBI data format is defined
                else
                {
                    System.out.println("Field is an KNOWN field");
                    if ((vbiData.length * 8) == unitLength)
                    {
                        DVRTestRunnerXlet.log("<<<<<CheckNullData : FAILED - Length exclusion : length should not be "
                                + (vbiData.length * 8) + " >>>>");
                        m_failedReason = "CheckNullData : FAILED - Length exclusion : length should not be "
                                + vbiData.length;
                        m_failed = TEST_FAILED;
                    }
                }
                DVRTestRunnerXlet.log("<<<<Data from filter " + m_filter + " : " + (new String(vbiData))
                        + "\nByte Array:" + (printBytes(vbiData)));
            }
        }

    }

    /*
     * 
     * @author Ryan
     * 
     * Checks for null data in a specific filter. Prior to the ProcessCommand()
     * the buffer shall be completely full with data. This shall only work work
     * with CC data only.
     */

    public class checkNullData extends EventScheduler.NotifyShell
    {

        private int m_filter;

        private int m_filterGroupNum;

        private int m_group;

        /**
         * @param time
         * @param i
         */
        checkNullData(int filter, int group, int filterGroupNum, long time)
        {
            super(time);
            m_filter = filter;
            m_group = group;
            m_filterGroupNum = filterGroupNum;
            // TODO Auto-generated constructor stub
        }

        public void ProcessCommand()
        {
            if (m_filter > 0 || (m_filter < vbiFilters[m_group].size()))
            {
                // Dunp the buffers
                System.out.println("<<<<<<Data dump of filter " + m_filter);
                VBIFilter vbiFilter = (VBIFilter) vbiFilters[m_group].elementAt(m_filter);
                byte[] vbiData = vbiFilter.getVBIData();

                // Check buffer if count matches
                String filterInfo = null;
                try
                {
                    filterInfo = configProp.getStringArg(FILTER_PARAMS + m_filterGroupNum + "_" + m_filter);
                }
                catch (Exception e)
                {
                    DVRTestRunnerXlet.log("<<<<<CheckNullData :Exception thrown");
                    m_failedReason = "CheckNullData : Exception thrown";
                    m_failed = TEST_FAILED;
                    e.printStackTrace();
                }
                StringTokenizer st = new StringTokenizer(filterInfo, ",");
                st.nextToken();
                st.nextToken();
                st.nextToken();
                st.nextToken();
                int bufferSize = Integer.parseInt((String) st.nextToken());
                if (vbiData.length != bufferSize)
                {
                    DVRTestRunnerXlet.log("<<<<<CheckNullData : Buffer was not completely full>>>>");
                    m_failedReason = "CheckNullData : Buffer was not completely full";
                    m_failed = TEST_FAILED;
                    return;
                }
                // for even bytes check to see if it is not 00
                for (int j = 0; j < vbiData.length; j++)
                {
                    if ((vbiData[j] == 0x00) && (j % 2 == 0))
                    {
                        DVRTestRunnerXlet.log("<<<<<CheckNullData : FAILED - Null detected!!!>>>>");
                        m_failedReason = "CheckNullData : Null detected in null checking of data";
                        m_failed = TEST_FAILED;
                    }
                }
                DVRTestRunnerXlet.log("Data " + m_filter + " : " + (new String(vbiData)) + "\nByte Array:"
                        + (printBytes(vbiData)));
            }
        }

    }

    /*
     * 
     * @author Ryan
     * 
     * TODO To change the template for this generated type comment go to Window
     * - Preferences - Java - Code Style - Code Templates
     */
    public class resetTimeNotification extends EventScheduler.NotifyShell
    {

        private long m_newTime;

        private int m_filter;

        private int m_group;

        /**
         * @param time
         */
        resetTimeNotification(int filter, int group, long newTime, long time)
        {
            super(time);
            m_newTime = newTime;
            m_filter = filter;
            m_group = group;
        }

        public void ProcessCommand()
        {
            System.out.println("<<<<resetTimeNotification::Done>>>>");
            ((VBIFilter) vbiFilters[m_group].elementAt(m_filter)).setNotificationByTime(m_newTime);
            System.out.println("<<<<resetTimeNotification::Done>>>>");
        }
    }

    /*
     * 
     * @author Ryan
     * 
     * TODO To change the template for this generated type comment go to Window
     * - Preferences - Java - Code Style - Code Templates
     */
    public class resetDataNotification extends EventScheduler.NotifyShell
    {

        private int m_newDataNum;

        private int m_filter;

        private int m_group;

        /**
         * @param time
         */
        resetDataNotification(int filter, int group, int newDataNum, long time)
        {
            super(time);
            m_newDataNum = newDataNum;
            m_filter = filter;
            m_group = group;
        }

        public void ProcessCommand()
        {
            System.out.println("<<<<resetDataNotification::Process Command>>>>");
            System.out.println("New data unit threshold : " + m_newDataNum);
            ((VBIFilter) vbiFilters[m_group].elementAt(m_filter)).setNotificationByDataUnits(m_newDataNum);
            System.out.println("<<<<resetDataNotification::Done>>>>");
        }
    }

    /*
     * 
     * @author Ryan
     * 
     * TODO To change the template for this generated type comment go to Window
     * - Preferences - Java - Code Style - Code Templates
     */
    public class resetTimeout extends EventScheduler.NotifyShell
    {

        private long m_newTimeout;

        private int m_filter;

        private int m_group;

        /**
         * @param time
         */
        resetTimeout(int filter, int group, long newTimeout, long time)
        {
            super(time);
            m_newTimeout = newTimeout;
            m_filter = filter;
            m_group = group;
        }

        public void ProcessCommand()
        {
            System.out.println("<<<<resetTimeout::ProcessCommand>>>>");
            ((VBIFilter) vbiFilters[m_group].elementAt(m_filter)).setTimeOut(m_newTimeout);
            System.out.println("<<<<resetTimeout::Done>>>>");
        }
    }

    /*
     * 
     * @author Ryan
     * 
     * Clears all the buffers or selected buffers of the VBIFilters. Argument
     * defined in the contructor deterimines what buffer(s) are flushed.
     */
    public class clearBuffers extends EventScheduler.NotifyShell
    {

        private int m_buffer;

        private int m_group;

        /**
         * @param time
         */
        clearBuffers(int buffer, int group, long time)
        {
            super(time);
            m_buffer = buffer;
            m_group = group;
        }

        public void ProcessCommand()
        {
            // Go through one filter at a time
            System.out.println("<<<<<<<<Clearing the buffers");
            if (m_buffer == ALL_FILTERS)
            {
                for (int i = 1; i < vbiFilters[m_group].size(); i++)
                {
                    // Clear the buffer
                    System.out.println("<<<<<<<Clearing the buffer of filter " + i);
                    VBIFilter vbiFilter = (VBIFilter) vbiFilters[m_group].elementAt(i);
                    vbiFilter.clearBuffer();
                    // Check to see if the buffer is truely cleared
                    byte[] data = vbiFilter.getVBIData();
                    for (int j = 0; j < data.length; j++)
                    {
                        if (data[j] != 0)
                        {
                            m_failed = TEST_FAILED;
                            DVRTestRunnerXlet.log("<<<<Clear Buffers : FAILED  - Buffer " + j
                                    + " has content in buffer");
                            m_failedReason = "<<<<Clear Buffers : FAILED  - Buffer " + j + " has content in buffer";
                        }
                    }
                }
            }
            else if (m_buffer > 0 || (m_buffer < vbiFilters[m_group].size()))
            {
                System.out.println("<<<<<<<Clearing the buffer of filter " + m_buffer);
                VBIFilter vbiFilter = (VBIFilter) vbiFilters[m_group].elementAt(m_buffer);
                vbiFilter.clearBuffer();
                // Check to see if the buffer is truely cleared
                byte[] data = vbiFilter.getVBIData();
                for (int j = 0; j < data.length; j++)
                {
                    if (data[j] != 0)
                    {
                        m_failed = TEST_FAILED;
                        DVRTestRunnerXlet.log("<<<<Clear Buffers : FAILED  - Buffer " + j + " has content in buffer");
                        m_failedReason = "<<<<Clear Buffers : FAILED  - Buffer " + j + " has content in buffer";
                    }
                }
                if (m_failed == TEST_FAILED)
                {
                    DVRTestRunnerXlet.log("Byte Array:" + (printBytes(data)));
                }
            }
            // if not, notify that m_filter is not in range
            else
            {
                System.out.println("<<<<<m_filter not in range");
            }
        }

    }

    /**
     * @author Ryan
     * 
     *         
     *         ------------------------------------------------------------------
     *         --------------------------
     * 
     *         Resource Clien object to handle Resource Client calls
     * 
     *         
     *         ------------------------------------------------------------------
     *         --------------------------
     * 
     */

    /*
     * 
     * @author Ryan
     * 
     * Default Resource Client designed to notify the Application of Resource
     * changes within the stack
     */
    public class DefaultResourceClient implements ResourceClient
    {

        public boolean requestRelease(ResourceProxy arg0, Object arg1)
        {
            System.out.println("ResourceClient.requestRelease() called with " + arg0.toString() + "; retuning false");
            return false;
        }

        public void release(ResourceProxy arg0)
        {
            System.out.println("ResourceClient.release() called with " + arg0.toString());
        }

        public void notifyRelease(ResourceProxy arg0)
        {
            System.out.println("ResourceClient.notifyRelease() called with " + arg0.toString());
        }
    }
}
