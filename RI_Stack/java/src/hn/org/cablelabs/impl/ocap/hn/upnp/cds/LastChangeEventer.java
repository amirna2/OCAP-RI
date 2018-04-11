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

package org.cablelabs.impl.ocap.hn.upnp.cds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.tv.util.TVTimer;
import javax.tv.util.TVTimerScheduleFailedException;
import javax.tv.util.TVTimerSpec;
import javax.tv.util.TVTimerWentOffEvent;
import javax.tv.util.TVTimerWentOffListener;

import org.apache.log4j.Logger;

import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;

import org.ocap.hn.upnp.server.UPnPManagedStateVariable;

/**
 * LastChangeEventer - This class is used for generating events whenever the
 * LastChange state variable changes, subject to moderation at a fixed moderation
 * period.
 *
 * <p>
 * Elements are sorted (stably) by updateID before XML is generated.
 *
 * <p>
 * The caller is however responsible for guaranteeing that the ordering requirements
 * in section 2.3.8.2 of the CDS specification are satisfied, by calling the public
 * "announce" methods with updateIDs that are not inappropriate.
 *
 * <p>
 * It is not clear from that specification whether or not an stDone element
 * must appear after other elements with the same updateID. If so, that is
 * also the caller's responsibility.
 *
 * @version $Revision$
 */
public class LastChangeEventer implements TVTimerWentOffListener
{
    /**
     * Log4J logging facility.
     */
    private static final Logger log = Logger.getLogger(LastChangeEventer.class);

    private static final int MODERATION_PERIOD = 200; // milliseconds = 0.2
                                                      // seconds

    private TVTimerSpec eventTimerSpec = new TVTimerSpec();

    private final TVTimer eventTimer = TVTimer.getTimer();

    private AnnouncementList lastChange = new AnnouncementList();

    private boolean waitUntilModerationPeriodLapses;

    private final List m_stateVariables = new ArrayList();
    

    /**
     * Constructor.
     *
     * @param stateVariable The LastChange state variable.
     */
    public LastChangeEventer()
    {
        // this TimerSpec is non-repeating
        eventTimerSpec.setRepeat(false);

        // the delay time for this TimerSpec is set to the MODERATION_PERIOD
        eventTimerSpec.setDelayTime(MODERATION_PERIOD);

        // the LastChangeEventer registers itself for TVTimerWentOffEvents
        eventTimerSpec.addTVTimerWentOffListener(this);
    }
    
    public void registerVariable(UPnPManagedStateVariable stateVariable)
    {
        m_stateVariables.add(stateVariable);
    }

    /**
     * Announce an object addition.
     *
     * @param objID       The ID of the added object.
     * @param updateID    The current system update ID.
     * @param stUpdate    Whether or not this change is part of a subtree update.
     * @param objParentID The ID of the added object's parent.
     * @param objClass    The upnp:class of the added object.
     *
     * @throws IllegalArgumentException If objID, objParentID, or objClass is null
     *                                  or if updateID is not a 32-bit unsigned integer.
     */
    public synchronized void announceAddition(String objID, long updateID, boolean stUpdate, String objParentID, String objClass)
    {
        if (objID == null)
        {
            throw new IllegalArgumentException("objID is null");
        }

        if (objParentID == null)
        {
            throw new IllegalArgumentException("objParentID is null");
        }

        if (objClass == null)
        {
            throw new IllegalArgumentException("objClass is null");
        }

        if (updateID < 0 || updateID > 0xFFFFFFFFL)
        {
            throw new IllegalArgumentException("updateID " + updateID + " is not a 32-bit unsigned integer");
        }

        lastChange.add(new ObjectAddition(objID, updateID, stUpdate, objParentID, objClass));
        checkModeration();
    }

    /**
     * Announce an object deletion.
     *
     * @param objID       The ID of the deleted object.
     * @param updateID    The current system update ID.
     * @param stUpdate    Whether or not this change is part of a subtree update.
     *
     * @throws IllegalArgumentException If objID is null or if updateID is not
     *                                  a 32-bit unsigned integer.
     */
    public synchronized void announceDeletion(String objID, long updateID, boolean stUpdate)
    {
        if (objID == null)
        {
            throw new IllegalArgumentException("objID is null");
        }

        if (updateID < 0 || updateID > 0xFFFFFFFFL)
        {
            throw new IllegalArgumentException("updateID " + updateID + " is not a 32-bit unsigned integer");
        }

        lastChange.add(new ObjectDeletion(objID, updateID, stUpdate));
        checkModeration();
    }

    /**
     * Announce an object modification.
     *
     * @param objID       The ID of the modified object.
     * @param updateID    The current system update ID.
     * @param stUpdate    Whether or not this change is part of a subtree update.
     *
     * @throws IllegalArgumentException If objID is null or if updateID is not
     *                                  a 32-bit unsigned integer.
     */
    public synchronized void announceModification(String objID, long updateID, boolean stUpdate)
    {
        if (objID == null)
        {
            throw new IllegalArgumentException("objID is null");
        }

        if (updateID < 0 || updateID > 0xFFFFFFFFL)
        {
            throw new IllegalArgumentException("updateID " + updateID + " is not a 32-bit unsigned integer");
        }

        lastChange.add(new ObjectModification(objID, updateID, stUpdate));
        checkModeration();
    }

    /**
     * Announce conclusion of a subtree update.
     *
     * @param objID       The ID of the container object that is the subtree root.
     * @param updateID    The current system update ID.
     *
     * @throws IllegalArgumentException If objID is null or if updateID is not
     *                                  a 32-bit unsigned integer.
     */
    public synchronized void announceSubtreeUpdateDone(String objID, long updateID)
    {
        if (objID == null)
        {
            throw new IllegalArgumentException("objID is null");
        }

        if (updateID < 0 || updateID > 0xFFFFFFFFL)
        {
            throw new IllegalArgumentException("updateID " + updateID + " is not a 32-bit unsigned integer");
        }

        lastChange.add(new SubtreeUpdateConclusion(objID, updateID));
        checkModeration();
    }

    public synchronized void timerWentOff(TVTimerWentOffEvent event)
    {
        /**
         * The moderation period has expired, so a LastChange
         * event needs to be sent to all interested listeners.
         */

        if (log.isDebugEnabled())
        {
            log.debug("timerWentOff - event received " + event);
        }

        generateEvent();

        waitUntilModerationPeriodLapses = false;
    }

    /**
     * Signal that a LastChange change
     * has occurred. If there have not been any LastChange
     * changes since the moderation period expired, an event is immediately sent
     * to all interested listeners. Then the moderation period is made active
     * and all additional LastChange changes are recorded until
     * the moderation period lapses.
     */
    private void checkModeration()
    {
        if (!waitUntilModerationPeriodLapses)
        {
            /**
             * The moderation period has lapsed so the LastChange event can
             * be generated and the listeners notified.
             */
            generateEvent();

            try
            {
                /**
                 * Start the TVTimerSpec to enforce the moderation period since
                 * a LastChange event was just sent to the appropriate
                 * listeners.
                 */
                eventTimerSpec = eventTimer.scheduleTimerSpec(eventTimerSpec);
            }
            catch (TVTimerScheduleFailedException exception)
            {
                if (log.isErrorEnabled())
                {
                    log.error("TVTimer failed to be scheduled", exception);
                }
            }

            /**
             * Setting this flag to true indicates that the moderation period is
             * currently being enforced and all values received during this
             * period need to be recorded. Once the moderation period expires an
             * event will be generated.
             */
            waitUntilModerationPeriodLapses = true;
        }
        else
        {
            /**
             * Do nothing since the moderation period is currently being
             * enforced and the the value has already been recorded.
             */

            // no op
        }
    }

    /**
     * Send the event to all interested listeners.
     */
    public void generateEvent()
    {
        if (log.isDebugEnabled())
        {
            log.debug("send value to all interested listeners");
        }

        /**
         * The service will create an event for the LastChange, and the
         * value will be sent out as part of the event.
         */
        lastChange.sort();
        final String value = lastChange.toString();

        getSystemContext().runInContextAsync(new Runnable()
        {
            public void run()
            {
                for(Iterator i = m_stateVariables.iterator(); i.hasNext();)
                {
                    ((UPnPManagedStateVariable)i.next()).setValue(value);
                }
            }
        });

        lastChange.reset();
    }

    private static CallerContext systemContext;

    private static CallerContext getSystemContext()
    {
        if (systemContext == null)
        {
            CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

            systemContext = ccm.getSystemContext();
        }

        return systemContext;
    }

    /**
     * The announcement list.
     */
    private static class AnnouncementList
    {
        private static final String VALUE_HEAD =
              "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<" + ContentDirectoryService.STATE_EVENT + " "
            + "    xmlns=\"urn:schemas-upnp-org:av:cds-event\" "
            + "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
            + "    xsi:schemaLocation=\"urn:schemas-upnp-org:av:cds-event http://www.upnp.org/schemas/av/cds-event.xsd\">\n"
        ;

        private static final String VALUE_TAIL =
              "</" + ContentDirectoryService.STATE_EVENT + ">\n"
        ;

        private final List/*<Announcement>*/ list = new ArrayList/*<Announcement>*/();

        private boolean reset;

        /**
         * Add an announcement to the list, first clearing the list if we've
         * been reset.
         */
        public void add(Announcement a)
        {
            if (reset)
            {
                list.clear();
                reset = false;
            }

            list.add(a);
        }

        /**
         * Reset, so we clear the list on the next add.
         */
        public void reset()
        {
            reset = true;
        }

        /**
         * Sort the list.
         */
        public void sort()
        {
            Collections.sort(list);
        }

        /**
         * Return the XML representation of this announcement list.
         *
         * @return The XML representation.
         */
        public String toString()
        {
            StringBuffer sb = new StringBuffer();

            sb.append(VALUE_HEAD);

            for (Iterator i = list.iterator(); i.hasNext(); )
            {
                Announcement a = (Announcement) i.next();
                sb.append(a);
            }

            sb.append(VALUE_TAIL);

            return sb.toString();
        }
    }

    /**
     * Abstract superclass of all announcements.
     */
    private abstract static class Announcement implements Comparable
    {
        protected final String elementName;
        protected final String objectID;
        protected final long updateID;

        /**
         * Constructor.
         *
         * @param elementName The element name.
         * @param objectID The object ID.
         * @param updateID The update ID.
         */
        public Announcement(String elementName, String objectID, long updateID)
        {
            this.elementName = elementName;
            this.objectID = objectID;
            this.updateID = updateID;
        }

        /**
         * Compares this object with the specified object for order.
         *
         * @param o The object to be compared.
         */
        public int compareTo(Object o)
        {
            Announcement that = (Announcement) o;

            return this.updateID < that.updateID ? -1 : this.updateID == that.updateID ? 0 : 1;
        }

        /**
         * Return the XML representation of this announcement.
         *
         * @return The XML representation.
         */
        public String toString()
        {
            return
                  "  "
                + "<" + elementName
                    + stringGuts()
                + "/>\n";
        }

        /**
         * Return the internal part of the XML representation of this announcement.
         *
         * @return The internal part of the XML representation.
         */
        protected String stringGuts()
        {
            return
                  " " + ContentDirectoryService.OBJ_ID        + "='" + objectID      + "'"
                + " " + ContentDirectoryService.UPDATE_ID     + "='" + updateID      + "'";
        }
    }

    /**
     * Abstract superclass of all object change announcements.
     */
    private abstract static class ObjectChange extends Announcement
    {
        protected final boolean subtreeUpdate;

        /**
         * Constructor.
         *
         * @param elementName The element name.
         * @param objectID The object ID.
         * @param updateID The update ID.
         * @param subtreeUpdate The subtree update indication.
         */
        public ObjectChange(String elementName, String objectID, long updateID, boolean subtreeUpdate)
        {
            super(elementName, objectID, updateID);
            this.subtreeUpdate = subtreeUpdate;
        }

        /**
         * Return the internal part of the XML representation of this announcement.
         *
         * @return The internal part of the XML representation.
         */
        protected String stringGuts()
        {
            return
                  super.stringGuts()
                + " " + ContentDirectoryService.ST_UPDATE     + "='" + subtreeUpdate + "'";
        }
    }

    /**
     * An object addition announcement.
     */
    private static class ObjectAddition extends ObjectChange
    {
        private final String parentID;
        private final String objectClass;

        /**
         * Constructor.
         *
         * @param objectID The object ID.
         * @param updateID The update ID.
         * @param subtreeUpdate The subtree update indication.
         * @param parentID The parent ID.
         * @param objectClass The object class.
         */
        public ObjectAddition(String objectID, long updateID, boolean subtreeUpdate, String parentID, String objectClass)
        {
            super(ContentDirectoryService.OBJ_ADD, objectID, updateID, subtreeUpdate);
            this.parentID = parentID;
            this.objectClass = objectClass;
        }

        /**
         * Return the internal part of the XML representation of this announcement.
         *
         * @return The internal part of the XML representation.
         */
        protected String stringGuts()
        {
            return
                  super.stringGuts()
                + " " + ContentDirectoryService.OBJ_PARENT_ID + "='" + parentID      + "'"
                + " " + ContentDirectoryService.OBJ_CLASS     + "='" + objectClass   + "'";
        }
    }

    /**
     * An object deletion announcement.
     */
    private static class ObjectDeletion extends ObjectChange
    {
        /**
         * Constructor.
         *
         * @param objectID The object ID.
         * @param updateID The update ID.
         * @param subtreeUpdate The subtree update indication.
         */
        public ObjectDeletion(String objectID, long updateID, boolean subtreeUpdate)
        {
            super(ContentDirectoryService.OBJ_DEL, objectID, updateID, subtreeUpdate);
        }
    }

    /**
     * An object modification announcement.
     */
    private static class ObjectModification extends ObjectChange
    {
        /**
         * Constructor.
         *
         * @param objectID The object ID.
         * @param updateID The update ID.
         * @param subtreeUpdate The subtree update indication.
         */
        public ObjectModification(String objectID, long updateID, boolean subtreeUpdate)
        {
            super(ContentDirectoryService.OBJ_MOD, objectID, updateID, subtreeUpdate);
        }
    }

    /**
     * A subtree update conclusion announcement.
     */
    private static class SubtreeUpdateConclusion extends Announcement
    {
        /**
         * Constructor.
         *
         * @param objectID The object ID.
         * @param updateID The update ID.
         */
        public SubtreeUpdateConclusion(String objectID, long updateID)
        {
            super(ContentDirectoryService.ST_DONE, objectID, updateID);
        }
    }
}
