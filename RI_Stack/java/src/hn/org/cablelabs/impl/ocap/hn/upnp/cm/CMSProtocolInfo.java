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

package org.cablelabs.impl.ocap.hn.upnp.cm;

import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.media.streaming.session.data.HNStreamProtocolInfo;
import org.cablelabs.impl.ocap.hn.NetManagerImpl;
import org.cablelabs.impl.ocap.hn.content.ContentContainerImpl;
import org.cablelabs.impl.ocap.hn.content.ContentEntryImpl;
import org.cablelabs.impl.ocap.hn.content.navigation.ContentListImpl;
import org.cablelabs.impl.ocap.hn.upnp.MediaServer;
import org.ocap.hn.ContentServerEvent;
import org.ocap.hn.ContentServerListener;
import org.ocap.hn.ContentServerNetModule;
import org.ocap.hn.NetModuleEvent;
import org.ocap.hn.NetModuleEventListener;
import org.ocap.hn.content.ContentEntry;

public class CMSProtocolInfo implements ContentServerListener, NetModuleEventListener
{
    private static final Logger log = Logger.getLogger(CMSProtocolInfo.class);

    // Singleton 
    private static CMSProtocolInfo m_instance;

    // Source Protocol Info for this CMS, 
    // keyed by ProfileID & Content Format and value is HNStreamProtocolInfo
    private static Map m_sourceProtocolInfoStrs = new HashMap();
    
    // Value to be use for CMS Source Protocol Info State Variable
    // This string is kept up to date as content is changed
    private static String m_sourceProtocolInfoStateVariableValue = "";

    // Sink Protocol Info for this CMS 
    // keyed by ProfileID & Content Format and value is HNStreamProtocolInfo
    private static Map m_sinkProtocolInfoStrs = new HashMap();

    // Value to be use for CMS Sink Protocol Info State Variable
    // This string is kept up to date as content is changed
    private static String m_sinkProtocolInfoStateVariableValue = "";
 
    /**
     * Returns instance of Connection Management protocol info
     */
    public static CMSProtocolInfo getInstance()
    {
        if (m_instance == null)
        {
            m_instance = new CMSProtocolInfo();
        }

        return m_instance;
    }
    
    /**
     * Listener method called when content changes in the CDS.
     * Update the source protocol info when this method is called.
     */
    public void contentUpdated(ContentServerEvent evt) 
    {
        if (log.isDebugEnabled())
        {
            log.debug("contentUpdated() - updating source protocol info");
        }
        
        // Spawn a thread to update protocol info 
        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        ccm.getSystemContext().runInContextAsync(new Runnable()
        {
            public void run()
            {
                if (log.isDebugEnabled())
                {
                    log.debug("contentUpdated() - thread started to update protocol info");
                }
                // Rebuild the supported source protocol
                buildSupportedSourceProtocolMap();        
                
                // Notify CMS its source protocol info state variable has been updated
                MediaServer.getInstance().getCMS().updateSourceProtocolInfoStateVariable();
                if (log.isDebugEnabled())
                {
                    log.debug("contentUpdated() - thread has completed");
                }
            }
        });
    }
    
    /**
     * NetManager net module event listener method used to be notified if CDS was not
     * discovered when instance was created.  This method allows instance to be notified when
     * local CDS is discovered so that ContentEventListener can be registered.
     */
    public void notify(NetModuleEvent event)
    {
        // Determine if CDS has been discovered yet
        ContentServerNetModule cds = NetManagerImpl.instance().getLocalCDS();
        
        // If local CDS has been discovered, register self as listener
        if (cds != null)
        {
            if (log.isInfoEnabled())
            {
                log.info("notify() - adding CMSProtocol as listener on CDS");
            }
            // Register as listener to be notified whenever content item is 
            // added, removed or changed in order to update     
            cds.addContentServerListener(this);      
            
            // Remove NetManager net module event listener
            NetManagerImpl.instance().removeNetModuleEventListener(this);
        }
        else
        {
            if (log.isInfoEnabled())
            {
                log.info("notify() - cds is still null");
            }
        }
    }
    
    /**
     * Private constructor which builds list of supported protocols
     */
    protected CMSProtocolInfo()
    {
        // Create the supported sink protocol
        buildSupportedSinkProtocolMap();
        
        // Create the supported source protocol
        buildSupportedSourceProtocolMap();
        
        // Determine if CDS has been discovered yet
        ContentServerNetModule cds =  NetManagerImpl.instance().getLocalCDS();
        if (cds == null)
        {
            if (log.isInfoEnabled())
            {
                log.info("CMSProtocolInfo() - cds was null, adding listener to NetManager to be notified");
            }
            
            // Register as listener to determine when CDS is discovered
            NetManagerImpl.instance().addNetModuleEventListener(this);
        }
        else
        {
            if (log.isInfoEnabled())
            {
                log.info("CMSProtocolInfo() - cds discovered, adding listener to be notified of content changes");
            }
            
            // Register as listener to be notified whenever content item is 
            // added, removed or changed in order to update     
            cds.addContentServerListener(this);                  
        }
     }
    
    /**
     * Determine if player acting as sink can render content in supplied
     * protocol info format.
     * 
     * @param protocolInfo  determine if this protocol is supported
     * 
     * @return  true if this protocol can be rendered, false otherwise
     */
    public boolean isSinkProtocolSupported(HNStreamProtocolInfo protocolInfo)
    {
        // Get the profile id from protocol info
        String profileId = protocolInfo.getProfileId();
        String contentFormat = protocolInfo.getContentFormat();
        if ((profileId == null) || (profileId.equals("*")))
        {
            // No profile was specified, see if content format was specified
            if ((contentFormat == null) || (contentFormat.equals("*")))
            {
                return true;
            }
        }
        
        // Look for matching profile & mime type if supplied
        Iterator itr = m_sinkProtocolInfoStrs.values().iterator();
        while (itr.hasNext())
        {
            HNStreamProtocolInfo pi = (HNStreamProtocolInfo)itr.next();
            if ((profileId == null) || (profileId.equals("*")) || (profileId.equals(pi.getProfileId())))
            {
                // Profile id matches, check content format
                if ((contentFormat == null) || (contentFormat.equals("*")) || (contentFormat.equals(pi.getMimeType())))
                {
                    // *TODO* - add special case handling for DTCP
                    return true;
                }
            }
        }

        return false;
    }
    
    /**
     * From UPnP CMS Spec. 2.2.1 - Source Protocol Info 
     * 
     * This state variable contains a Comma-Separated Value (CSV) list of information on protocols this
     * ConnectionManager supports for �sourcing� (sending) data, in its current state. (The content of the 
     * CSV list can change over time...) Besides the traditional notion of the term �protocol�, the 
     * protocol-related information provided by the connection also contains other information such as 
     * supported content formats.  
     * 
     * During normal operation, a MediaServer SHOULD ensure that there is consistency between what is
     * reported in the SourceProtocolInfo state variable and all the res@protocolInfo properties of the 
     * items that populate the ContentDirectory; that is: at least all protocols that are used by any 
     * of the content items SHOULD be enumerated in the SourceProtocolInfo state variable. Additional 
     * protocols that are supported by the MediaServer but are not currently used by any of the content 
     * items MAY also be listed. Control points can use the SourceProtocolInfo CSV list to quickly find 
     * out what type of content this.
     * 
     * The source protocol info state variable is a comma separated list of content protocol info currently
     * available from this server.  If not content has been published to the CDS, the value of this 
     * variable will be an empty string;
     *
     * From DLNA 7.3.28 - CMS:GetProtocolInfo Rules
     * 
     * The ConnectionManager service of a UPnP AV MediaServer must list the union set of protocolInfo 
     * values supported by the device for protocolInfo values that share the same values in the first 
     * three fields and the same pn-param (DLNA.ORG_PN) value in the fourth field, but have different 
     * values in the additional parameters in the fourth field. This means that the ConnectionManager 
     * service must list only one protocolInfo value to represent all such profiles. 
     * 
     * The first three fields of the listed protocolInfo value must be identical to the first three fields
     * of the individual protocolInfo values. The pn-param (DLNA.ORG_PN) value in the fourth field must 
     * be identical to the pn-param (DLNA.ORG_PN) value in the fourth fields of these protocolInfo values.
     * 
     * From DLNA 7.3.28.5 - If a CSV (Comma Separated Value) list contained in a CMS:GetProtocolInfo
     * response has one or more embedded comma(s) in the individual substring entries of the CSV list,
     * then those embedded commas must be escaped as "\,".
     * 
     * From DLNA 7.3.28.6 - If a UPnP AV MediaServer lists additional parameters (besides the pn-param) 
     * in the 4th field of protocolInfo when combining multiple protocolInfo values as defined...
     * (snippets from DLNA spec included inline with code below).
     */ 
    private void buildSupportedSourceProtocolMap()
    {
        synchronized (m_sourceProtocolInfoStrs)
        {
            if (log.isDebugEnabled())
            {
                log.debug("buildSupportedSourceProtocolMap() - called with cnt: " +
                        m_sourceProtocolInfoStrs.size());
            }

            // Clear out map since rebuilding
            m_sourceProtocolInfoStrs.clear();
            
            // Build list of all possible profile IDs based on content items in CDS
            ContentContainerImpl root = MediaServer.getInstance().getCDS().getRootContainer();
            ContentListImpl contentList = root.getContentList();
            if (log.isDebugEnabled())
            {
                log.debug("buildSupportedSourceProtocolMap() - cds item cnt: " +
                        contentList.size());
            }
            for (int i = 0; i < contentList.size(); i++)
            {
                if (contentList.get(i) instanceof ContentEntry)
                {
                    HNStreamProtocolInfo piStrs[] = ((ContentEntryImpl)contentList.get(i)).getProtocolInfo();
                    if ((piStrs != null) && (piStrs.length > 0))
                    {
                        // Look at each protocol string associated with this content item
                        for (int j = 0; j < piStrs.length; j++)
                        {
                            HNStreamProtocolInfo protocolInfo = piStrs[j];
                            ProtocolKey key = new ProtocolKey(protocolInfo.getProfileId(), protocolInfo.getMimeType());
                            
                            // If protocol info not in map already, add it
                            if (!m_sourceProtocolInfoStrs.containsKey(key))
                            {
                                if (log.isDebugEnabled())
                                {
                                    log.debug("buildSupportedSourceProtocolMap() - no item found for key " + key.asString() + 
                                            " adding protocol info to map: " + protocolInfo.getAsString());
                                }
                                m_sourceProtocolInfoStrs.put(key, protocolInfo);
                            }
                            else
                            {
                                if (log.isDebugEnabled())
                                {
                                    log.debug("buildSupportedSourceProtocolMap() - already had protocol info in map: " +
                                            protocolInfo.getAsString());
                                }
                                boolean hasChanged = false;
                                
                                // Already in the map, but make sure the following DLNA requirements are met:
                                // For CMS:GetProtocolInfo SOURCE is the combined protocol info
                                
                                // Get the current combined protocol info value
                                HNStreamProtocolInfo curProtocolInfo = (HNStreamProtocolInfo)m_sourceProtocolInfoStrs.get(key);
                                
                                // Determine if value currently assigned in op params and if a new value is available
                                String curOpParam = curProtocolInfo.getOpParam();
                                String newOpParam = protocolInfo.getOpParam();
                                if ((curOpParam != null) || (newOpParam != null)) 
                                {
                                    hasChanged = true;
                                    curOpParam = combineOpParams(curOpParam, newOpParam);
                                }
                                
                                // Determine if playspeeds currently assigned need to be updated
                                float curPlayspeeds[] = curProtocolInfo.getPlayspeeds();
                                float newPlayspeeds[] = protocolInfo.getPlayspeeds();
                                
                                // Determine if both values have not been set already and if op params are different
                                if ((curPlayspeeds != null) || (newPlayspeeds != null))
                                {
                                    hasChanged = true;
                                    curPlayspeeds = combinePsParams(curPlayspeeds, newPlayspeeds);
                                }
                                
                                // Determine if flags currently assigned need to be updated
                                String curFlagsParam = curProtocolInfo.getFlagsParam();
                                String newFlagsParam = protocolInfo.getFlagsParam();
                                
                                // Determine if union potentially needs to be updated
                                if ((curFlagsParam != null) || (newFlagsParam != null))
                                {
                                    hasChanged = true;
                                    curFlagsParam = combineFlagsParams(curFlagsParam, newFlagsParam);
                                }
                                
                                // If anything has changed, create a new protocol info representing new union
                                if (hasChanged)
                                {
                                    // Remove old protocol info
                                    m_sourceProtocolInfoStrs.remove(key);
                                    
                                    // Create new protocol info with up to date current union values
                                    HNStreamProtocolInfo newProtocolInfo = new HNStreamProtocolInfo(curProtocolInfo.getProtocolType(), 
                                            curProtocolInfo.isLinkProtected(), curProtocolInfo.getProfileId(), curProtocolInfo.getContentFormat(), 
                                            curProtocolInfo.getMimeType(), curOpParam, curPlayspeeds, curFlagsParam, false);
                                    
                                    // Add new protocol info to map
                                    m_sourceProtocolInfoStrs.put(key, newProtocolInfo);                        

                                    if (log.isDebugEnabled())
                                    {
                                        log.debug("buildSupportedSourceProtocolMap() - adding new combined protocol info to map: " +
                                                newProtocolInfo.getAsString());
                                    }
                                }
                            }
                        }                   
                    }
                 }
            }
            
            // Update the string since values have changed
            StringBuffer sb = new StringBuffer(256);
            Iterator itr = m_sourceProtocolInfoStrs.values().iterator();
            int cnt = 0;
            while (itr.hasNext())
            {
                if (cnt > 0)
                {
                    // the comma separator for multiple protocol info strings is not escaped...
                    sb.append(",");                    
                }
                // however, the comma separators embedded in the ps-param value must be escaped.
                sb.append(((HNStreamProtocolInfo)itr.next()).getAsString(true));
                cnt++;
            }
            m_sourceProtocolInfoStateVariableValue = sb.toString();

            if (log.isInfoEnabled())
            {
                log.info("buildSupportedSourceProtocolMap() - completed with cnt: " +
                        m_sourceProtocolInfoStrs.size() + ", value: " + m_sourceProtocolInfoStateVariableValue);
            }
        }
    }
  
    /**
     * Build union representation of two supplied op param strings, based on the following:
     * 
     * DLNA 7.3.28.6 - op-param:
     *
     * If none of the individual protocolInfo values contain an op-param value, the
     * combined protocolInfo value must omit the op-param value. If the a-val of the opparam
     * in any of the individual protocolInfo values is "1", the a-val of the op-param in
     * the combined protocolInfo value must be "1". Otherwise, the a-val of the op-param in
     * the combined protocolInfo value must be "0". Similarly, if the b-val of the op-param in
     * any of the individual protocolInfo values is "1", the b-val of the op-param in the
     * combined protocolInfo value must be "1". Otherwise, the b-val of the op-param in the
     * combined protocolInfo value must be "0".
     *
     * @param curOpParam    current union of values
     * @param newOpParam    additional values to include in union
     * 
     * @return  op param which represents union of two supplied values
     */
    private String combineOpParams(String curOpParam, String newOpParam)
    {
        if (log.isDebugEnabled())
        {
            log.debug("combineOpParams() - called with cur: " +
                   curOpParam + ", new: " + newOpParam);
        }
        if (curOpParam == null)
        {
            curOpParam = newOpParam;
        }
        else // curOpParam != null & newOpParam != null
        {
            boolean setAVal = false;
            boolean setBVal = false;

            // Determine if a-val should be set
            if (((curOpParam != null) && (curOpParam.charAt(0) == '1')) || 
                ((newOpParam != null) && (newOpParam.charAt(0) == '1')))
            {
                setAVal = true; 
            }
            if (((curOpParam != null) && (curOpParam.charAt(1) == '1')) || 
                ((newOpParam != null) && (newOpParam.charAt(1) == '1')))
            {
                setBVal = true; 
            }
            StringBuffer sb = new StringBuffer(16);
            if (setAVal)
            {
                sb.append('1');
            }
            else
            {
                sb.append('0');                                    
            }
            if (setBVal)
            {
                sb.append('1');
            }
            else
            {
                sb.append('0');                                    
            }
            curOpParam = sb.toString();
        }
        
        return curOpParam;
    }
    
    /**
     * Build union representation of two supplied playspeed arrays for ps param, based on the following:
     * 
     * DLNA 7.3.28.6 - ps-param:
     *
     * If none of the individual protocolInfo values contain a ps-param value, the
     * combined protocolInfo value must omit the ps-param value. Otherwise, the ps-param
     * value of the combined protofocolInfo value must be a comma-separated list of all the
     * distinct ps-param values of the individual protocolInfo values.
     * 
     * @param curPlayspeeds     current union of values
     * @param newPlayspeeds     additional values to include in union
     * 
     * @return ps param which represents union of two supplied values
     */
    private float[] combinePsParams(float curPlayspeeds[], float newPlayspeeds[])
    {
        if (log.isDebugEnabled())
        {
            log.debug("combinePsParams() - called");
        }
        // Create union representation of playspeeds
        if (curPlayspeeds == null)
        {
            return newPlayspeeds;
        }
        else 
        {
            // Create vector which includes all current playspeeds
            Vector speeds = new Vector();
            for (int cIdx = 0; cIdx < curPlayspeeds.length; cIdx++)
            {
                speeds.add(new Float(curPlayspeeds[cIdx]));
            }

            // Add any additional playspeeds that should be added
            for (int nIdx = 0;  nIdx < newPlayspeeds.length; nIdx++)
            {
                boolean found = false;

                // Determine if this playspeed needs to be added to list
                for (int cIdx = 0; cIdx < curPlayspeeds.length; cIdx++)
                {
                    if (newPlayspeeds[nIdx] == curPlayspeeds[cIdx])
                    {
                        // Found the playspeed in list already, no need to add
                        found = true;
                        break;
                    }
                }

                if (!found)
                {
                    // Add to list of playspeeds 
                    speeds.add(new Float(newPlayspeeds[nIdx]));
                }
            }

            // Use vector to create current array playspeeds
            if (speeds.size() > 0)
            {
                curPlayspeeds = new float[speeds.size()];
                for (int cIdx = 0; cIdx < curPlayspeeds.length; cIdx++)
                {
                    curPlayspeeds[cIdx] = ((Float)speeds.elementAt(cIdx)).floatValue();
                }
                return curPlayspeeds;                
            }
        }
        return null;
    }
    
    /**
     * Build union representation of two supplied flags param strings, based on the following:
     * 
     * DLNA 7.3.28.6 - flags-param:
     *
     * If none of the individual protocolInfo values contain a flags-param value,
     * the combined protocolInfo value must omit the flags-param value. For each bit in the
     * flags-param value, if the corresponding bit in any of the individual protocolInfo values
     * is "1", that bit in the combined protocolInfo value must be "1". Otherwise, that bit must
     * be "0".
     * 
     * @param curFlagsParam     current union of values
     * @param newFlagsParam     additional values to include in union
     * 
     * @return flags param which represents union of two supplied values
     */
    private String combineFlagsParams(String curFlagsParam, String newFlagsParam)
    {
        if (log.isDebugEnabled())
        {
            log.debug("combineFlagsParams() - called with cur: " +
                   curFlagsParam + ", new: " + newFlagsParam);
        }
        // Create union representation of flags
        if (curFlagsParam == null)
        {
            curFlagsParam = newFlagsParam;
        }
        else
        {
            // Look for differences in each flag, creating union, OR'ing values together
            long flags = 0;
            for (int fIdx = 0; fIdx < HNStreamProtocolInfo.DLNA_FLAGS.length; fIdx++)
            {
                if ((HNStreamProtocolInfo.isFlagSet(curFlagsParam, HNStreamProtocolInfo.DLNA_FLAGS[fIdx])) ||
                        (HNStreamProtocolInfo.isFlagSet(newFlagsParam, HNStreamProtocolInfo.DLNA_FLAGS[fIdx])))
                {
                    flags = flags | HNStreamProtocolInfo.DLNA_FLAGS[fIdx];
                }
            }

            // Assign string representation to current value
            curFlagsParam = HNStreamProtocolInfo.generateFlags(flags);
        }
        
        return curFlagsParam;
    }
    
    /*
     * Creates list of protocols this player is capable of supporting
     */
    private void buildSupportedSinkProtocolMap()
    {
        // *TODO* - need to update this when ability to query platform is available
        // Clear out map since rebuilding
        m_sinkProtocolInfoStrs.clear();
        
        // Add the one and only supported profile for the RI
        // *TODO* - add actual info when it can be retrieved from platform
        // *TODO* - make sure that DTCP_ types are reported as well: DLNA [8.7.2.3]
        HNStreamProtocolInfo protocolInfo = new HNStreamProtocolInfo(
                "http-get:*:" + MIME_TYPE_VIDEO_MPEG + ":DLNA.ORG_PN=MPEG_TS_SD_NA_ISO");
        
        if (log.isInfoEnabled())
        {
            log.info("buildSupportedSinkProtocolMap() = added protocol info: " + protocolInfo.getAsString());
        }
        m_sinkProtocolInfoStrs.put(new ProtocolKey(protocolInfo.getProfileId(), protocolInfo.getMimeType()),
                                    protocolInfo);
        
        // OCORI-2658: HNP 2.0 I07 [5.4] implies that an HNIMP can be a "DMS and/or DMP" DLNA device
        // class. The RI exemplifies this use case by implementing both device classes. Note that it
        // does not implement the "DMR" DLNA device class. Also, there is not necessarily a 1:1
        // correspondence between DLNA devices classes and UPnP device types.
        //
        // Per DLNA [7.3.11] (October, 2006), a DMS device class implements a UPnP AV MediaServer
        // device that must have a ContentDirectory service and a ConnectionManager service; thus
        // it can respond to a CMS:GetProtocolInfo action. UPnP ConnectionManager:2 Service Template
        // Version 1.01 [1.1], [2.5.1], and [2.5.1] tie the MediaServer device type with "sourcing"
        // of data and with the SourceProtocolInfo state variable. [2.2.2] and [2.4.1] state that if
        // the device does not support "sinking" data, the SinkProtocolInfo state variable must be
        // set to the empty string. Since a UPnP MediaServer "sources" data and does not "sink" data,
        // the SinkProtocolInfo state variable is set to an empty string.
        //
        // Per DLNA [7.3.2] (October, 2006), a DMP device class only implements a UPnP AV Media
        // Server control point and no other service (e.g., ConnectionManager). Therefore it cannot
        // respond to a CMS:GetProtocolInfo action. Hence the SinkProtcolInfo state variable remains
        // an empty string.
        //
        // This allows the RI to get past the UCTT AV-CM:1-1.1 GetProtocolInfo() action [MANDATORY] 
        // "Sink parameter must be empty for MediaServer" test.
        m_sinkProtocolInfoStateVariableValue = "";
        
        // OCSPEC-324: The following code block would be needed only if the RI implemented a UPnP 
        // sub-device of type MediaRenderer. A MediaRenderer implements a Connection Manager that
        // responds to a CMS:GetProtocolInfo action with an empty SourceProtocolInfo string and a
        // populated SinkProtocolInfo string. This implies separate ConnectionManager services for
        // MediaServer and MediaRenderer UPnP device types.
//        // Update the sink protocol info string
//        StringBuffer sb = new StringBuffer(256);
//        Iterator itr = m_sinkProtocolInfoStrs.values().iterator();
//        int cnt = 0;
//        while (itr.hasNext())
//        {
//            if (cnt > 0)
//            {
//                // the comma separator for multiple protocol info strings is not escaped...
//                sb.append(",");                    
//            }
//            
//            // however, the comma separators embedded in the ps-param value must be escaped.
//            sb.append(((HNStreamProtocolInfo)itr.next()).getAsString(true));
//            cnt++;
//        }
//        m_sinkProtocolInfoStateVariableValue = sb.toString();        
    }
    
    /**
     * Returns the value of CMS:GetProtocolInfo - Source state variable for UPnP CMS
     * 
     * @return  value of state variable
     */
    public String getSourceProtocolStateVariableValue()
    {
        return m_sourceProtocolInfoStateVariableValue;
    }

    /**
     * Returns the value of CMS:GetProtocolInfo - Sink state variable for UPnP CMS
     * 
     * @return  value of state variable
     */
    public String getSinkProtocolStateVariableValue()
    {
         return m_sinkProtocolInfoStateVariableValue;
    }
    
    /**
     * Used as key when storing protocol info in maps.
     */
    private class ProtocolKey
    {
        String m_profileId; 
        String m_mimeType;
        
        ProtocolKey(String profileId, String mimeType)
        {
            if ((profileId == null) || (mimeType == null))
            {
                throw new InvalidParameterException();
            }
            m_profileId = profileId;
            m_mimeType = mimeType;
        }
        
        public boolean equals(Object obj)
        {
            if (!(obj instanceof ProtocolKey))
            {
                return false;
            }

            ProtocolKey that = (ProtocolKey)obj;
            boolean equals = false;
            if ((this.m_profileId.equals(that.m_profileId)) && (this.m_mimeType.equals(that.m_mimeType)))
            {
                equals = true;
            }
            if (log.isDebugEnabled())
            {
                log.debug("ProtocolKey.equals() - this p: " + this.m_profileId + ", that p: " + that.m_profileId +
                        ", this m: " + this.m_mimeType + ", that m: " + that.m_mimeType + ", returning: " + equals);
            }
            return equals;
        }

        public int hashCode()
        {
            return m_profileId.hashCode() + m_mimeType.hashCode();
        }
        
        public String asString()
        {
            return "proifileId = " + m_profileId + ", m_mimeType = " + m_mimeType;
        }
    }

    // *TODO* - get rid of this when player side HN MPEOS API changes are made
    public static int findContentTypeForString(String contentType)
    {
        String temp = contentType.trim();
        
        // *TODO* - need more sophisticated ways to support multiple types
        // Determine if there are multiple types supplied
        int idx = -1;
        if ((idx = contentType.indexOf(";")) > -1)
        {
            temp = contentType.substring(0, idx);
        }
        
        if (temp.indexOf(MIME_TYPE_VIDEO_VND_DLNA_MPEG_TTS) > -1)
        {
            return CONTENT_MEDIA_TYPE_VIDEO_VND_DLNA_MPEG_TTS;
        }
        else if (temp.indexOf(MIME_TYPE_VIDEO_MPEG) > -1)
        {
            return CONTENT_MEDIA_TYPE_VIDEO_MPEG;
        }
        else if (temp.indexOf(MIME_TYPE_AUDIO_L16) > -1)
        {
            return CONTENT_MEDIA_TYPE_AUDIO_L16;
        }
        else if (temp.indexOf(MIME_TYPE_AUDIO_VND_DOLBY_DD_RAW) > -1)
        {
            return CONTENT_MEDIA_TYPE_AUDIO_VND_DOLBY_DD_RAW;
        }
        else if (temp.indexOf(MIME_TYPE_AUDIO_VND_DLNA_ADTS) > -1)
        {
            return CONTENT_MEDIA_TYPE_AUDIO_VND_DLNA_ADTS;
        }
        else if (temp.indexOf(MIME_TYPE_AUDIO_MP4) > -1)
        {
            return CONTENT_MEDIA_TYPE_AUDIO_MP4;
        }
        else if (temp.indexOf(MIME_TYPE_AUDIO_3GPP) > -1)
        {
            return CONTENT_MEDIA_TYPE_AUDIO_3GPP;
        }
        else if (temp.indexOf(MIME_TYPE_AUDIO_MPEG) > -1)
        {
            return CONTENT_MEDIA_TYPE_AUDIO_MPEG;
        }
        else
        {
            return CONTENT_MEDIA_TYPE_NONE;
        }
    }
    private static final String MIME_TYPE_VIDEO_MPEG = "video/mpeg";
    private static final String MIME_TYPE_VIDEO_VND_DLNA_MPEG_TTS = "video/vnd.dlna.mpeg-tts";
    private static final String MIME_TYPE_AUDIO_L16 = "audio/L16";
    private static final String MIME_TYPE_AUDIO_VND_DOLBY_DD_RAW = "audio/vnd.dolby.dd-raw";
    private static final String MIME_TYPE_AUDIO_VND_DLNA_ADTS = "audio/vnd.dlna.adts";
    private static final String MIME_TYPE_AUDIO_MP4 = "audio/mp4";
    private static final String MIME_TYPE_AUDIO_3GPP = "audio/3gpp";
    private static final String MIME_TYPE_AUDIO_MPEG = "audio/mpeg";
    private static final int CONTENT_MEDIA_TYPE_VIDEO_VND_DLNA_MPEG_TTS = 1;
    private static final int CONTENT_MEDIA_TYPE_VIDEO_MPEG = 2;
    private static final int CONTENT_MEDIA_TYPE_AUDIO_L16 = 3;
    private static final int CONTENT_MEDIA_TYPE_AUDIO_VND_DOLBY_DD_RAW = 4;
    private static final int CONTENT_MEDIA_TYPE_AUDIO_VND_DLNA_ADTS = 5;
    private static final int CONTENT_MEDIA_TYPE_AUDIO_MP4 = 6;
    private static final int CONTENT_MEDIA_TYPE_AUDIO_3GPP = 7;
    private static final int CONTENT_MEDIA_TYPE_AUDIO_MPEG = 8;
    private static final int CONTENT_MEDIA_TYPE_NONE = 0xFFFF;
}
