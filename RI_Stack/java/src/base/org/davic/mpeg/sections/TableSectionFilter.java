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


import org.apache.log4j.Logger;
import org.davic.mpeg.NotAuthorizedException;

/**
 * This class defines a section filter operation optimized to capture entire
 * tables with minimum intervention required from the application. When
 * filtering is started, first one section matching the specified pattern will
 * be filtered. Once that section has been found, the last_section_number field
 * will be used to determine the number of Section objects required to hold the
 * entire table. This number of objects will be created and filtering re-started
 * to capture all the sections of the table. The SectionAvailableEvent will be
 * generated each time a Section is captured. The EndOfFilteringEvent will be
 * generated when the complete table has been captured.
 * 
 * The version_number of all sections of the table will be the same. If a
 * section is captured with a version_number that differs from the
 * version_number of the section first captured, a VersionChangeDetectedEvent
 * will be generated. The newly captured section will be ignored and filtering
 * will continue on the table with the version number of the first captured
 * section. Only one VersionChangeDetectedEvent will be sent per filtering
 * action.
 * 
 * Care should be taking in setting the filter parameters, a too restrictive
 * filter will never stop automatically and a too wide filter can produce
 * inconsistent results (e.g. filtering short sections using a
 * TableSectionFilter)
 * 
 * When the API detects a filtering situation where the filter parameters have
 * been incompletely defined, resulting in a blocking filter or a non MPEG-2
 * compliant result, an InCompleteFilteringEvent is sent and filtering is
 * stopped.
 * 
 * @version updated to DAVIC 1.3.1
 */
public class TableSectionFilter extends SectionFilter
{

    // Log4J Logger
    private static final Logger log = Logger.getLogger(TableSectionFilter.class.getName());

    /**
     * This method returns an array of Section objects corresponding to the
     * sections of the table. The sections in the array will be ordered
     * according to their section_number. Any sections which have not yet been
     * filtered from the source will have the corresponding entry in the array
     * set to null. If no sections have been filtered then this method will
     * block until at least one section is available or filtering stops.
     * 
     * Repeated calls to this method will return the same array, provided that
     * no new calls to startFiltering have been made in the interim. Each time a
     * new filtering operation is started, a new array of Section objects will
     * be created. All references except any in the application to the previous
     * array and Section objects will be removed. All data accessing methods on
     * the previous Section objects will throw a NoDataAvailableException.
     * 
     * @exception FilteringInterruptedException
     *                if filtering stops before one section is available
     */
    public Section[] getSections() throws FilteringInterruptedException
    {
        if (lastTableSections != null)
        {
            // null out the previous section data
            for (int i = 0; i < lastTableSections.length; i++)
                lastTableSections[i].setEmpty();
            lastTableSections = null;
        }

        // Blocking is allowed if we are currently filtering and no sections
        // have yet been returned.
        if (isRunning() && !haveReceivedSections)
        {
            synchronized (sectionMonitor)
            {
                try
                {
                    // We'll wait until we are interrupted, a section comes in,
                    // or the filter is stopped.
					// Added for findbugs issues fix - start
					// condition added in the while to wait on the correct condition
                	while(!m_completed)
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
                if (haveReceivedSections == false) throw new FilteringInterruptedException();
            }
        }

        lastTableSections = tableSections;
        return tableSections;
    }

    /**
     * Creates a new <code>TableSectionFilter</code>
     * 
     * @param group
     *            the <code>SectionFilterGroup</code> to which this section
     *            filter is a member
     * @param sectionSize
     *            the maximum section size in bytes
     */
    TableSectionFilter(SectionFilterGroup group, int sectionSize)
    {
        super(group, FILTER_RUN_TILL_CANCELED, sectionSize);
    }

    // Description copied from SectionFilter
    void handleStart()
    {
        // Release our interest in the sections previously matched
        // Note: References to the sections may live on beyond the life
        // of the filter that produced them.
        synchronized (sectionMonitor)
        {
            tableSections = null;
            haveReceivedSections = false;
            sentVersionChangeEvent = false;
        }
    }

    // Description copied from SectionFilter
    void handleStop()
    {
        synchronized (sectionMonitor)
        {
			// Added for findbugs issues fix - start
        	m_completed = true;
			// Added for findbugs issues fix - end
            sectionMonitor.notifyAll();
        }
    }

    // Description copied from SectionFilter
    void handleSection(Section nativeSection, Object appData)
    {
        // Create a section
        Section sec = nativeSection;

        synchronized (sectionMonitor)
        {
            try
            {
            	int sectionNumber;
            	
                if(!sec.section_syntax_indicator())
                { 
                	// If section_syntac_indicator is false
                	// There is only one section
                	// Private data bytes immediately follow the
                	// private_section_length field
                    // Allocate the table sections array 
                	sectionNumber = 0;
                	
                    if (tableSections == null)
                    {
                        tableSections = new Section[1];
                        tableSections[0] = null;

                        sectionCounter = 0;
                        numSections = 1;
                        if (log.isDebugEnabled())
                        {
                            log.debug("TableSectionFilter::handleSection tableVersion: " + tableVersion
                                    + " sectionCounter: " + sectionCounter + " numSections: " + numSections);
                        }
                    }                	
                }
                else
                {                                
	                sectionNumber = sec.section_number();
	
                    if (log.isDebugEnabled())
                    {
                        log.debug("TableSectionFilter: handleSection sec.section_number(): " + sectionNumber
                                  + " sec.version_number(): " + sec.version_number());
                    }
	
	                // Allocate the table sections array using the size found
	                // in the section itself.
	                if (tableSections == null)
	                {
	                    tableSections = new Section[sec.last_section_number() + 1];
	                    for (int i = 0; i <= sec.last_section_number(); i++)
	                        tableSections[i] = null;
	
	                    // Save off the version number for comparison against
	                    // future sections we receive.
	                    tableVersion = sec.version_number();
	                    sectionCounter = 0;
	                    numSections = sec.last_section_number() + 1;
                        if (log.isDebugEnabled())
                        {
                            log.debug("TableSectionFilter::handleSection tableVersion: " + tableVersion
                                      + " sectionCounter: " + sectionCounter + " numSections: " + numSections);
	                    }
	                }
	                
	                if(sectionNumber > numSections)
	                {
                        if (log.isErrorEnabled())
                        {
                            log.error("TableSectionFilter::handleSection - Error occured, section number (" 
                            		  + sectionNumber 
                            		  + ")" 
                            		  + " may not be greater than the number of sections (" 
                            		  + numSections 
                            		  + ")" + " - Ignoring section...");           
                    }
	                    return;
	                }
	                
	                // Check if the table version has changed
	                if (tableVersion != sec.version_number())
	                {
	                    // If the version number has changed then generate a version
	                    // change event
	                    // discard this section and continue filtering

                        if (log.isDebugEnabled())
                        {
                            log.debug("TableSectionFilter::handleSection posting a VersionChangeDetectedEvent..."
                                      + " tableVersion: " + tableVersion + " sec.version_number(): " + sec.version_number());
                        }
	                    // Send VersionChangeDetectedEvent only once
	                    if (!sentVersionChangeEvent)
	                    {
	                        notifySectionFilterListener(new VersionChangeDetectedEvent(this, appData, tableVersion,
	                                sec.version_number()));
	                        sentVersionChangeEvent = true;
	                    }
	                    return;
	                }  	                
                }           

                // Store this section in our array
                // need to verify this here.
                if (sec != null && tableSections[sectionNumber] == null)
                {
                    tableSections[sectionNumber] = sec;

                    sectionCounter++;
                    haveReceivedSections = true;

                    // Signal the sectionMonitor in case someone is blocking on
                    // the getSections method.
					// Added for findbugs issues fix - start
                    m_completed = true;
					// Added for findbugs issues fix - end
                    sectionMonitor.notifyAll();

                    if (log.isDebugEnabled())
                    {
                        log.debug("TableSectionFilter::handleSection calling notifySectionFilterListener-SectionAvailableEvent for section "
                                + sectionNumber);
                    }
                    // If the table is not complete, just alert listeners that a
                    // new
                    // section is available
                    notifySectionFilterListener(new SectionAvailableEvent(this, appData));
                }
            }
            catch (NoDataAvailableException e)
            {
                return;
            }

        }
        if (log.isDebugEnabled())
        {
            log.debug("TableSectionFilter::sectionCounter: " + sectionCounter + " numSections: " + numSections);
        }

        // Check if Table is complete!

        if (sectionCounter >= numSections)
        {
            stopFiltering();
            if (log.isDebugEnabled())
            {
                log.debug("TableSectionFilter: posting a EndOfFilteringEvent...");
            }
            notifySectionFilterListener(new EndOfFilteringEvent(this, appData));
        }
    }

    // override to throw new IllegalFilterDefinitionException if called by a
    // TableSectionFilter

    public void startFiltering(Object appData, int pid) throws FilterResourceException, NotAuthorizedException,
            IllegalFilterDefinitionException, ConnectionLostException
    {
        throw new IllegalFilterDefinitionException();
    }

    private short tableVersion = -1;

    private boolean haveReceivedSections = false;

    private boolean sentVersionChangeEvent = false;

    private Object sectionMonitor = new Object();

    private Section[] tableSections = null;

    private Section[] lastTableSections = null;

    /** Counts sections sent; when matches size, throw EOF Event. */
    private int sectionCounter;

    // ** Count of total sections. Here so code only has to get it once. */
    private int numSections;
	
	// Added for findbugs issues fix - start
    private boolean m_completed = false;
	// Added for findbugs issues fix - end
}
