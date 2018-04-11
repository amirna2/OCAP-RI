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

package org.cablelabs.xlet.DvrExerciser;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.HashMap;

import org.cablelabs.lib.utils.SimpleCondition;
import org.havi.ui.HListElement;
import org.havi.ui.HListGroup;
import org.havi.ui.HVisible;
import org.havi.ui.event.HItemEvent;
import org.havi.ui.event.HItemListener;
import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.dvr.RecordingResourceUsage;
import org.ocap.dvr.TimeShiftBufferResourceUsage;
import org.ocap.hn.resource.NetResourceUsage;
import org.ocap.resource.ApplicationResourceUsage;
import org.ocap.resource.ResourceContentionHandler;
import org.ocap.resource.ResourceUsage;
import org.ocap.resource.SharedResourceUsage;
import org.ocap.service.ServiceContextResourceUsage;
import org.ocap.shared.dvr.LocatorRecordingSpec;
import org.ocap.shared.dvr.RecordingSpec;
import org.ocap.shared.dvr.ServiceContextRecordingSpec;
import org.ocap.shared.dvr.ServiceRecordingSpec;

public class InteractiveResourceContentionHandler implements ResourceContentionHandler
{
    private static final long serialVersionUID = 1L;
    
    private static DvrExerciser m_dvrExerciser;

    InteractiveResourceUsageSorter m_ruSorter;
    
    SimpleCondition m_contentionHandled = new SimpleCondition(false);
    
    public InteractiveResourceContentionHandler(final DvrExerciser dvre)
    {
        m_dvrExerciser = dvre;
        // m_dvrExerciser.logIt("InteractiveResourceContentionHandler constructing...");
    }
    
    /**
     * {@inheritDoc}
     */
    public ResourceUsage[] resolveResourceContention(final ResourceUsage newRequest, final ResourceUsage[] currentReservations)
    {
        m_dvrExerciser.logIt("InteractiveResourceContentionHandler: Entered ResourceContentionHandler...");
        try
        {
            EventQueue.invokeAndWait(new Runnable()
            {
                public void run()
                {
                    m_ruSorter = new InteractiveResourceUsageSorter(newRequest, currentReservations);
                    m_ruSorter.show();
                }
            });
        }
        catch (Exception e)
        {
            m_dvrExerciser.logIt("WARN: InteractiveResourceContentionHandler: Error delegating to EventQueue: " + e.getMessage());
        }
        
        // m_dvrExerciser.logIt("InteractiveResourceContentionHandler: Sorter started...");

        m_ruSorter.waitForDialogClose();
        m_dvrExerciser.logIt("InteractiveResourceContentionHandler: Exiting ResourceContentionHandler...");
        return m_ruSorter.getPrioritizedUsages();
    }

    /**
     * {@inheritDoc}
     */
    public void resourceContentionWarning(ResourceUsage newRequest, ResourceUsage[] currentReservations)
    {
        // TODO Auto-generated method stub
    }
    
    public static class InteractiveResourceUsageSorter implements HItemListener
    {
        private static final long serialVersionUID = 1L;

        private HashMap m_rus;
        
        SimpleCondition m_dialogDismissed;
        
        HListGroup m_hlist;
        
        final ResourceUsage m_newRequest;
        final ResourceUsage [] m_currentReservations;

        public InteractiveResourceUsageSorter( final ResourceUsage newRequest,
                                               final ResourceUsage [] currentReservations )
       {
            // m_dvrExerciser.logIt("InteractiveResourceUsageSorter constructing...");
            m_newRequest = newRequest;
            m_currentReservations = currentReservations;
            m_dialogDismissed = new SimpleCondition(false);
            m_rus = new HashMap(m_currentReservations.length+1);
       }
        
        /**
         * Constructor
         */
        public void show()
        {
            // m_dvrExerciser.logIt("InteractiveResourceUsageSorter.show...");
            
            m_hlist = new HListGroup();
            m_rus.clear();
            // establish some display characteristics
            m_hlist.setBackground(Color.YELLOW);
            m_hlist.setBackgroundMode(HVisible.BACKGROUND_FILL);
            m_hlist.setFont(new Font("Tiresias", Font.PLAIN, 15));
            m_hlist.setHorizontalAlignment(HListGroup.HALIGN_LEFT);

            // only allow a single selection
            m_hlist.setMultiSelection(false);

            m_hlist.addItem(new HListElement("              RESOURCE CONTENTION DETECTED"), 0);
            m_hlist.addItem(new HListElement("[Select a usage to move to the bottom - select here to use current order]"), 1);
            
            String label = "NEW REQUEST: " + stringForUsage(m_newRequest);
            m_hlist.addItem(new HListElement(label), 2);
            m_rus.put(label, m_newRequest);
            // m_dvrExerciser.logIt("Added item: " + label);

            for (int i = 0; i < m_currentReservations.length; i++)
            {
                label = "CURRENT USE " + (i+1) + ": " 
                        + stringForUsage(m_currentReservations[i]);
                m_hlist.addItem(new HListElement(label), i+3);
                m_rus.put(label, m_currentReservations[i]);
                m_dvrExerciser.logIt("Added item: " + label);
            }
            m_hlist.setCurrentItem(1);
            
            m_hlist.setBounds(10, 100, DvrExerciser.SCREEN_WIDTH - 100, DvrExerciser.SCREEN_HEIGHT - 75);
            m_hlist.setScrollPosition(0);
            
            m_hlist.addItemListener(this);

            m_dvrExerciser.add(m_hlist);
            m_dvrExerciser.popToFront(m_hlist);

            // ... have it process input
            m_hlist.setVisible(true);
            m_dvrExerciser.popToFront(m_hlist);
            m_hlist.setEnabled(true);
            m_hlist.setFocusable(true);

            m_hlist.requestFocus();
        } // END InteractiveResourceUsageSorter constructor

        public void waitForDialogClose()
        {
            // m_dvrExerciser.logIt("InteractiveResourceUsageSorter.waitForDialogClose()...");
            try
            {
                m_dialogDismissed.waitUntilTrue();
            }
            catch (InterruptedException e)
            {
                m_dvrExerciser.logIt("WARN: InteractiveResourceUsageSorter.waitForDialogClose() INTERRRUPTED: " + e.getMessage());
            }
        }

        /**
         * {@inheritDoc}
         */
        public void currentItemChanged(HItemEvent e)
        {
            HListElement changedElement = (HListElement)e.getItem();
            // m_dvrExerciser.logIt( "InteractiveResourceUsageSorter: Current item changed: eventID" 
            //                       + e.getID() + "/"
            //                       + ( (changedElement != null) 
            //                           ? changedElement.getLabel() 
            //                           : e.toString() ) );
        }

        /**
         * {@inheritDoc}
         */
        public void selectionChanged(HItemEvent e)
        {
            HListElement changedElement = (HListElement)e.getItem();
            // m_dvrExerciser.logIt( "InteractiveResourceUsageSorter: Selection changed: eventID " 
            //         + e.getID() + "/"
            //         + ( (changedElement != null) 
            //             ? changedElement.getLabel() 
            //             : e.toString() ) );
            
            if (e.getID() == HItemEvent.ITEM_SELECTED)
            {
                int selectionIndex = m_hlist.getSelectionIndices()[0];
                // m_dvrExerciser.logIt( "InteractiveResourceUsageSorter.selectionChanged: selectionIndex = " 
                //                       + selectionIndex ); 
                if (selectionIndex == 0)
                { // Ignore - we're using item 0 as a "title" 
                    return;
                }
                
                if (selectionIndex == 1)
                { // Index 1 indicates that we're done. 
                    // m_dvrExerciser.logIt("InteractiveResourceUsageSorter.selectionChanged: Sorting dialog dismissed...");
                    m_hlist.setVisible(false);
                    m_hlist.setEnabled(false);
                    m_dvrExerciser.remove(m_hlist);
                    m_dvrExerciser.m_scene.requestFocus();
                    m_dvrExerciser.m_scene.repaint();
                    m_dialogDismissed.setTrue();
                    return;
                }
                
                // Otherwise, we're just going to move things around...
                HListElement selectedElement = m_hlist.removeItem(selectionIndex);
                
                // m_dvrExerciser.logIt( "InteractiveResourceUsageSorter.selectionChanged: Removed item " 
                //                       + selectedElement ); 
                // m_dvrExerciser.logIt("InteractiveResourceUsageSorter.selectionChanged: Moving item #" 
                //                      + selectionIndex + " (" 
                //                      + selectedElement.getLabel() 
                //                      + ") to the top..." );
                m_hlist.addItem(selectedElement, m_hlist.getNumItems());
                m_hlist.setCurrentItem(1);
                m_hlist.repaint();
                m_dvrExerciser.m_scene.repaint();
            }
        } // END selectionChanged()
        
        /**
         * Get the user-sorted list of usages
         */
        public ResourceUsage [] getPrioritizedUsages()
        {
            HListElement[] elements = m_hlist.getListContent();
            
            ResourceUsage orderedList[] = new ResourceUsage[elements.length-2];

            // The label order dictates the RU order...
            for (int i=0; i<orderedList.length; i++)
            {
                String curLabel = elements[i+2].getLabel();
                ResourceUsage ru = (ResourceUsage)m_rus.get(curLabel);
                orderedList[i] = ru;
            }
            
            return orderedList;
        }
        
        public static String stringForUsage(ResourceUsage ru)
        {
            if (ru instanceof ServiceContextResourceUsage)
            {
                ServiceContextResourceUsage scru = 
                    (ServiceContextResourceUsage)ru;
                return "ServiceContext (service: " 
                       + scru.getRequestedService().getLocator().toExternalForm()
                       + ')';
            }
            if (ru instanceof RecordingResourceUsage)
            {
                RecordingResourceUsage rru = 
                    (RecordingResourceUsage)ru;
                OcapRecordingRequest orr = (OcapRecordingRequest)
                                           rru.getRecordingRequest();
                RecordingSpec rs = orr.getRecordingSpec();
                
                String rsString;
                Date startTime = null;
                long duration = 0;
                if (rs instanceof LocatorRecordingSpec)
                {
                    LocatorRecordingSpec lrs = (LocatorRecordingSpec)rs;
                    rsString = "LocatorRecording " + lrs.getSource()[0];
                    startTime = lrs.getStartTime();
                    duration = lrs.getDuration();
                }
                else if (rs instanceof ServiceRecordingSpec)
                {
                    ServiceRecordingSpec srs = (ServiceRecordingSpec)rs;
                    rsString = "ServiceRecording " + srs.getSource().getLocator().toExternalForm();
                    startTime = srs.getStartTime();
                    duration = srs.getDuration();
                } 
                else if (rs instanceof ServiceContextRecordingSpec)
                {
                    ServiceContextRecordingSpec scrs = (ServiceContextRecordingSpec)rs;
                    rsString = "ServiceContextRecording " + scrs.getServiceContext().getService().getLocator().toExternalForm();
                    startTime = scrs.getStartTime();
                    duration = scrs.getDuration();
                } 
                else 
                {
                    rsString = rs.toString();
                } 

                long remainingSeconds = 0;
                if (startTime != null)
                {
                    remainingSeconds = 
                                       ( (startTime.getTime() + duration)
                                         - System.currentTimeMillis() ) / 1000;
                }
                return rsString + " (" + remainingSeconds + " seconds remaining)";
            } // END RecordingResourceUsage
            
            if (ru instanceof TimeShiftBufferResourceUsage)
            {
                TimeShiftBufferResourceUsage tsru = (TimeShiftBufferResourceUsage)ru;
                return "TSBResourceUsage (service " 
                       + tsru.getService().getLocator().toExternalForm() 
                       + ')';
            }
            if (ru instanceof ApplicationResourceUsage)
            {
                ApplicationResourceUsage aru = (ApplicationResourceUsage)ru;
                return "AppResourceUsage (appID " 
                       + aru.getAppID().toString()
                       + ')';
            }
            
            if (ru instanceof NetResourceUsage)
            {
                NetResourceUsage nru = (NetResourceUsage)ru;
                return "NetResourceUsage (from " 
                       + nru.getInetAddress()
                       + " for " + nru.getOcapLocator() +')';
            }
            
            if (ru instanceof SharedResourceUsage)
            {
                SharedResourceUsage sharedRU = (SharedResourceUsage) ru;
                ResourceUsage ruarray [] = sharedRU.getResourceUsages();
                StringBuffer sruSB = new StringBuffer("SharedResourceUsage");
                sruSB.append(" (" + ruarray.length + " elements)\n");
                
                for (int i=0; i<ruarray.length; i++)
                {
                    sruSB.append("  SUBUSE ")
                         .append(i+1)
                         .append(": ")
                         .append(stringForUsage(ruarray[i]))
                         .append('\n');
                }
                
                return sruSB.toString();
            }
            // Last resport...
            return ru.toString();
        } // END stringForUsage()
    } // END class ResourceUsageSorter
} // END class InteractiveResourceContentionHandler
