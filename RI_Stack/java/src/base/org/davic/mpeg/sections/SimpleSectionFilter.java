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
package org.davic.mpeg.sections;

/**
 * This class defines a simple section filter intended to be used to capture a
 * single section once only. When a section matching the specified filter
 * pattern is found, SimpleSectionFilter objects will stop filtering as if the
 * stopFiltering method had been called.
 * 
 * @version updated to DAVIC 1.3.1
 */
public class SimpleSectionFilter extends org.davic.mpeg.sections.SectionFilter
{
    /**
     * This method retrieves a Section object describing the last MPEG-2 section
     * which matched the specified filter characteristics. If the
     * SimpleSectionFilter object is currently filtering, this method will block
     * until filtering stops.
     * 
     * Repeated calls to this method will return the same Section object,
     * provided that no new calls to startFiltering have been made in the
     * interim. Each time a new filtering operation is started, a new Section
     * object will be created. All references except any in the application to
     * the previous Section object will be removed. All data accessing methods
     * on the previous Section object will throw a NoDataAvailableException.
     * 
     * @exception FilteringInterruptedException
     *                if filtering stops before a matching section is found
     */
    public Section getSection() throws FilteringInterruptedException
    {
        if (lastSimpleSection != null)
        {
            // null out the previous section data
            lastSimpleSection.setEmpty();
            lastSimpleSection = null;
        }

        // Blocking is allowed if we are currently filtering and the section
        // has not yet been returned.
        if (isRunning() && !haveReceivedSection)
        {
            synchronized (sectionMonitor)
            {
                try
                {
                    // We'll wait until we are interrupted, a section comes in,
                    // or the filter is stopped.
					// Added for findbugs issues fix - start
					// While loop added to wait on the correct condition
                	while(!m_completedFiltering)
                	{
                    sectionMonitor.wait();
                }
					// Added for findbugs issues fix - end
                }
                catch (InterruptedException e)
                {
                }

                // TODO (TomH) We need to throw this exception if filtering
                // was interrupted before any data was filtered. This could
                // be because the filter was cancelled.
                if (haveReceivedSection == false) throw new FilteringInterruptedException();
            }
        }

        lastSimpleSection = simpleSection;
        return simpleSection;
    }

    SimpleSectionFilter(SectionFilterGroup group, int sectionSize)
    {
        super(group, FILTER_ONE_SHOT, sectionSize);
    }

    // Description copied from SectionFilter
    protected void handleStart()
    {
        // Invalidate any section from a previous filter operation and
        // remove OUR reference to this objects. Some Xlet may still
        // have a reference to the Section object, but it won't return
        // data now.
        synchronized (sectionMonitor)
        {
            if (simpleSection != null)
            {
                simpleSection.setEmpty();
                simpleSection = null;
            }
            haveReceivedSection = false;
        }
    }

    // Description copied from SectionFilter
    protected void handleStop()
    {
        synchronized (sectionMonitor)
        {
			// Added for findbugs issues fix - start
        	m_completedFiltering = true;
			// Added for findbugs issues fix - end
            sectionMonitor.notifyAll();
        }
    }

    // Description copied from SectionFilter
    protected void handleSection(Section nativeSection, Object appData)
    {

        synchronized (sectionMonitor)
        {
            simpleSection = nativeSection;
            haveReceivedSection = true;
        }

        // This will notify any thread waiting in getSection()
        stopFiltering();

        // Notify any listeners that we have received a section
        notifySectionFilterListener(new SectionAvailableEvent(this, appData));
    }

    private Section simpleSection = null;

    private Section lastSimpleSection = null;

    private boolean haveReceivedSection = false;

    private Object sectionMonitor = new Object();
    
    // Added for findbugs issues fix - start
    private boolean m_completedFiltering = false;
	// Added for findbugs issues fix - end

}
