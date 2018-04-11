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
 * ProgramMapTableManager.java
 */

package org.ocap.si;

import org.cablelabs.impl.ocap.si.ProgramMapTableManagerImpl;

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
public abstract class ProgramMapTableManager
{
    private static ProgramMapTableManagerImpl theProgramMapTableManager = null;

    /**
     * For Singleton behavior
     */
    protected ProgramMapTableManager()
    {
    }

    /**
     * Get an instance of the Program Map Table Manager.
     *
     * @return The ProgramMapTableManager instance.
     */
    public static ProgramMapTableManager getInstance()
    {
        /*
         * Actual implementation is vendor dependent.
         */
        if (theProgramMapTableManager == null)
            theProgramMapTableManager = new org.cablelabs.impl.ocap.si.ProgramMapTableManagerImpl();

        return theProgramMapTableManager;
    }

    /**
     * Add a TableChangeListener object that will be notified when the inband
     * PMT changes. If the specified TableChangeListener object is already
     * added, no action is performed.
     * <tt>javax.tv.service.SIRequestFailureType.DATA_UNAVAILABLE</tt>
     * is returned via <tt>SIRequestor.notifyFailure()</tt>
     * if the locator refers to an analog service.
     *
     * @param listener
     *            A TableChangeListener object to be notified when the inband
     *            PMT changes.
     *
     * @param locator
     *            A locator to specify a virtual channel carrying the inband
     *            PMTs. Should correspond to one of the following OCAP URL
     *            forms: ocap://source_id, ocap://f=frequency.program_number
     *
     */
    public abstract void addInBandChangeListener(TableChangeListener listener, javax.tv.locator.Locator locator);

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
    public abstract void addOutOfBandChangeListener(TableChangeListener listener, int programNumber);

    /**
     * Remove the TableChangedListener object for the inband PMT.
     *
     * @param listener
     *            The TableChangeListener object to be removed.
     */
    public abstract void removeInBandChangeListener(TableChangeListener listener);

    /**
     * Remove the TableChangedListener object for the OOB PMT.
     *
     * @param listener
     *            The TableChangeListener object to be removed.
     */
    public abstract void removeOutOfBandChangeListener(TableChangeListener listener);

    /**
     * <P>
     * Retrieve a PMT from the in-band channel (transport stream) identified by
     * the Locator parameter.
     * <P>
     * </P>
     * The OCAP implementation does not automatically tune to the transport
     * stream specified by the Locator. Hence, the calling application must tune
     * to the corresponding transport stream before calling this method.
     * <P>
     * </P>
     * The attempt to retrieve a PMT stops silently and permanently when the
     * network interface starts tuning to another transport stream. In this
     * case, the registered <tt>SIRequestor.notifyFailure()</tt> method is
     * invoked with a failure type of
     * <tt>javax.tv.service.SIRequestFailureType.DATA_UNAVAILABLE</tt>. </P>
     * <P>
     * It is not guaranteed that the transport stream specified by the Locator
     * is still tuned when the method of the SIRequestor is called back.
     * <P>
     * </P>
     * Note: If an application has added a listener via the
     * <tt>addInBandChangeListener()</tt> method, the
     * <tt>TableChangeListener.notifyChange()</tt> method is called when the
     * specified PMT is updated. In this case, the PMT returned to the
     * <tt>SIRequestor</tt> registered with this method may have expired. </P>
     *
     * <tt>javax.tv.service.SIRequestFailureType.DATA_UNAVAILABLE</tt>
     * is returned via <tt>SIRequestor.notifyFailure()</tt>
     * if the locator refers to an analog service.
     *
     * @param requestor
     *            The SIRequestor object to be called back with the retrieval
     *            result, when the PMT is discovered.
     *
     * @param locator
     *            A locator to specify a virtual channel carrying the inband
     *            PMTs. Should correspond to one of the following OCAP URL
     *            forms: ocap://source_id, ocap://f=frequency.program_number
     *
     * @return The SIRequest object that identifies this asynchronous retrieval
     *         request and allows the request to be cancelled.
     */
    public abstract javax.tv.service.SIRequest retrieveInBand(javax.tv.service.SIRequestor requestor,
            javax.tv.locator.Locator locator);

    /**
     * Retrieve the PMT from the out-of-band channel. If there is no OOB PMT the
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
    public abstract javax.tv.service.SIRequest retrieveOutOfBand(javax.tv.service.SIRequestor requestor,
            int programNumber);

}