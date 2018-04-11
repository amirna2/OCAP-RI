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

package org.cablelabs.impl.ocap.si;

import javax.tv.locator.Locator;
import javax.tv.service.SIRequest;
import javax.tv.service.SIRequestor;
import javax.tv.service.Service;

import org.ocap.net.OcapLocator;
import org.ocap.si.ProgramMapTableManager;
import org.ocap.si.TableChangeListener;

import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ServiceManager;
import org.cablelabs.impl.service.ServiceExt;

/**
 * The Program Map Table (PMT) manager is used to discover and listen for MPEG-2
 * PMTs. To retrieve the PMT, an application add the TableChangeListener to the
 * ProgramMapTableManager via the addInBandChangeListener() or the
 * addOutOfBandChangeListener(), and call the retrieveInBand() or the
 * retrieveOutOfBand(). If PMT has changed, ProgramMapTableManager call
 * TableChangeListener.NotifyChange() to notify it. The application must get
 * updated ProgramMapTable object via the SIChangeEvent.getSIElement() to keep
 * the PMT table fresh when the PMT change is notified, i.e., ProgramMapTable
 * object is not updated automatically.
 */
public class ProgramMapTableManagerImpl extends ProgramMapTableManager implements TableManagerExt
{
    /**
     * Create an instance of the Program Map Table Manager.
     * 
     * @return A ProgramMapTableManager instance.
     */
    public ProgramMapTableManagerImpl()
    {
        // Get ServiceManager and register for PAT change notifications
        ServiceManager serviceManager = (ServiceManager) ManagerManager.getInstance(ServiceManager.class);
        tableChangeManager = new PMTTableChangeManager(serviceManager.getSICache());
    }

    /**
     * Add a TableChangeListener object that will be notified when the inband
     * PMT changes. If the specified TableChangeListener object is already
     * added, no action is performed.
     * 
     * @param listener
     *            A TableChangeListener object to be notified when the inband
     *            PMT changes.
     * 
     * @param locator
     *            A locator to specify a virtual channel carrying the inband
     *            PMTs. Should correspond to one of the following OCAP URL
     *            forms: ocap://source_id, ocap://f=frequency.program_number
     */
    public void addInBandChangeListener(TableChangeListener listener, Locator locator)
    {
        if (listener == null) return;

        // Validate Locator
        if (locator instanceof OcapLocator)
        {
            // A valid in-band PMT must specify either a SourceID or a
            // Frequency/Program#
            OcapLocator ol = (OcapLocator) locator;
            if (ol.getSourceID() != -1 || (ol.getFrequency() != -1 && ol.getProgramNumber() != -1))
            {
                tableChangeManager.addChangeListener(ol.getSourceID(), ol.getFrequency(), ol.getProgramNumber(),
                        listener);
            }
        }
    }

    public void addTableChangeListener(TableChangeListener listener, javax.tv.service.Service s, int priority)
    {
        if (listener == null) return;        
        tableChangeManager.addChangeListener(s, listener, priority);       
    }
    
    /**
     * Register an in-band <code>TableChangeListener</code> to the service
     * described by the given locator. Multiple listeners registered to the same
     * service will be notified in decreasing priority order
     * 
     * @param listener
     *            the listener to be notified of in-band table changes
     * @param locator
     *            the in-band service of interest
     * @param priority
     *            the priority of this listener
     */
    public void addInBandChangeListener(TableChangeListener listener, Locator locator, int priority)
    {
        if (listener == null) return;

        // Validate Locator
        if (locator instanceof OcapLocator)
        {
            // A valid in-band PMT must specify either a SourceID or a
            // Frequency/Program#
            OcapLocator ol = (OcapLocator) locator;
            if (ol.getSourceID() != -1 || (ol.getFrequency() != -1 && ol.getProgramNumber() != -1))
            {
                tableChangeManager.addChangeListenerWithPriority(ol.getSourceID(), ol.getFrequency(),
                        ol.getProgramNumber(), listener, priority);
            }
        }
    }

    /**
     * Add a TableChangeListener object that will be notified when the
     * out-of-band PMT changes. If the specified TableChangeListener object is
     * already added, no action is performed.
     * 
     * @param listener
     *            A TableChangeListener object to be notified when the
     *            out-of-band PMT changes.
     * 
     * @param programNumber
     *            A program number of the PMT from the corresponding PAT.
     */
    public void addOutOfBandChangeListener(TableChangeListener listener, int programNumber)
    {
        if (listener == null || programNumber < 0) return;

        tableChangeManager.addChangeListener(-1, -1, programNumber, listener);
    }

    /**
     * Remove the TableChangeListener object for the inband PMT.
     * 
     * @param listener
     *            The TableChangeListener object to be removed.
     */
    public void removeInBandChangeListener(TableChangeListener listener)
    {
        tableChangeManager.removeChangeListener(listener);
    }

    /**
     * Remove the TableChangeListener object for the OOB PMT.
     * 
     * @param listener
     *            The TableChangeListener object to be removed.
     */
    public void removeOutOfBandChangeListener(TableChangeListener listener)
    {
        tableChangeManager.removeChangeListener(listener);
    }

    /**
     * Retrieve the PMTs from the in-band channel (transport stream) identified
     * by the Locator parameter. If the channel specified by the Locator is not
     * currently tuned, the system will place the PMT retrieve request in a
     * queue until it is. </P>
     * <P>
     * It is not guaranteed that the channel (transport stream) specified by the
     * Locator is still tuned when the method of the SIRequestor is called back.
     * Use a table listener to verify the validity of the PMT.
     * </P>
     * 
     * @param requestor
     *            The SIRequestor object to be called back with the retrieval
     *            result, when the PMT is discovered.
     * 
     * @param locator
     *            A locator to specify a virtual channel carrying the inband
     *            PMTs.
     * 
     * @return The SIRequest object that identifies this asynchronous retrieval
     *         request and allows the request to be cancelled.
     */
    public SIRequest retrieveInBand(SIRequestor requestor, Locator locator)
    {
        // Get ServiceManager and register for PMT change notifications
        ServiceManager serviceManager = (ServiceManager) ManagerManager.getInstance(ServiceManager.class);

        return serviceManager.getSICache().retrieveProgramMapTable(locator, requestor);
    }

    /**
     * Retrieve the PMT from the out-of-band channel. If there is no OOB PMT or
     * the OCAP implementation does not support retrieving a PMT via OOB, the
     * SIRequestor.notifyFailure method will be called with a failure type of
     * javax.tv.service.SIRequestFailureType.DATA_UNAVAILABLE.
     * 
     * @param requestor
     *            The SIRequestor object to be called back with the retrieval
     *            result.
     * 
     * @param programNumber
     *            A program number of the PMT from the corresponding PAT.
     * 
     * @return The SIRequest object that identifies this asynchronous retrieval
     *         request and allows the request to be cancelled.
     */
    public SIRequest retrieveOutOfBand(SIRequestor requestor, int programNumber)
    {
        // Get ServiceManager and register for PAT change notifications
        ServiceManager serviceManager = (ServiceManager) ManagerManager.getInstance(ServiceManager.class);

        // Create a locator representing the OOBFDC with the specified
        // program number
        OcapLocator ol = null;
        try
        {
            ol = new OcapLocator(-1, programNumber, -1);
        }
        catch (org.davic.net.InvalidLocatorException e1)
        {
        }

        return serviceManager.getSICache().retrieveProgramMapTable(ol, requestor);
    }
    
    /**
     * Retrieve the PMT for the given Service. If there is no PMT or
     * the OCAP implementation does not support retrieving PMT, the
     * SIRequestor.notifyFailure method will be called with a failure type of
     * javax.tv.service.SIRequestFailureType.DATA_UNAVAILABLE.
     * 
     * @param requestor
     *            The SIRequestor object to be called back with the retrieval
     *            result.
     * 
     * @param service
     *            Service for which the PMT needs to be retrieved.
     * 
     * @return The SIRequest object that identifies this asynchronous retrieval
     *         request and allows the request to be cancelled.
     */
    public SIRequest retrievePMT(SIRequestor requestor, Service service)
    {
        // Get ServiceManager and register for PAT change notifications
        ServiceManager serviceManager = (ServiceManager) ManagerManager.getInstance(ServiceManager.class);

        return serviceManager.getSICache().retrieveProgramMapTable(service, requestor);
    }
    
    private TableChangeManager tableChangeManager;

}
