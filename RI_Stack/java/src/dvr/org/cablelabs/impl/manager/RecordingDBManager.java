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

package org.cablelabs.impl.manager;

import org.cablelabs.impl.recording.RecordingInfoNode;
import org.cablelabs.impl.recording.RecordingInfoTree;
import org.cablelabs.impl.recording.RecordingInfo;
import org.cablelabs.impl.recording.RecordingInfo2;

import java.io.IOException;
import java.util.Vector;

/**
 * An instance of a <code>RecordingDB</code> manager can be used to manage DVR
 * recording meta-data in persistent storage.
 * <p>
 * The actual representation of the meta-data in persistent storage, as well as
 * the location of said data, is implementation-specific. Some potential options
 * for implementations include:
 * <ul>
 * <li>Build on built-in Java Serialization mechanisms.
 * <li>Use a custom format.
 * <li>Maintain records in a single file or multiple files.
 * <li>Use compression.
 * <li>Use error detection mechanisms.
 * </ul>
 * 
 * @author Aaron Kamienski
 * 
 * @see RecordingInfo
 */
public interface RecordingDBManager extends Manager
{
    /**
     * Factory method used to allocate a new <code>RecordingInfo2</code> object.
     * This method manages the {@link RecordingInfoNode#uniqueId uniqueIds} that
     * are assigned to each <code>RecordingInfoNode</code> instance.
     * 
     * @return a new instance of <code>RecordingInfo2</code> with a pre-assigned
     *         <i>uniqueId</i>
     */
    public RecordingInfo2 newRecord();

    /**
     * Factory method used to allocate a new <code>RecordingInfo2</code> object
     * from an existing RecordingInfo and uniqueID.
     * 
     * @return a new instance of <code>RecordingInfo2</code> with the given
     *         <i>uniqueId</i> fully initialzed according to the supplied
     *         RecordingInfo.
     */
    public RecordingInfo2 newRecord(RecordingInfo ri, long uniqueID);

    /**
     * Factory method used to allocate a new <code>RecordingInfoTree</code>
     * object. This method manages the {@link RecordingInfoNode#uniqueId
     * uniqueIds} that are assigned to each <code>RecordingNode</code>.
     * 
     * @return a new instance of <code>RecordingInfoTree</code> with a
     *         pre-assigned <i>uniqueId</i>
     */
    public RecordingInfoTree newRecordTree();

    /**
     * Saves the given <code>RecordigInfo</code> to persistent storage.
     * 
     * @param info
     *            the <code>RecordingInfoNode</code> to save to storage
     * 
     * @throws IOException
     *             if there is a problem writing the record
     */
    public void saveRecord(RecordingInfoNode info) throws IOException;

    /**
     * Saves the given <code>RecordingInfoNode</code> in persistent storage,
     * potentially optimized to only update the persistent representation per
     * the <i>updateFlag</i>.
     * <p>
     * The <i>updateFlag</i> parameter is expected to be a sum of any of the
     * following values: {@link #ALL}, {@link #APP_ID}, {@link #EXPIRATION_DATE}, {@link #FAP}, {@link #PRIORITY}, {@link #BIT_RATE}, {@link #DURATION},
     * {@link #STATE}, {@link #SERVICE_LOCATOR}, {@link #SERVICE_NAME}.
     * {@link #RECORDING_ID}, (@link #FAILED_EXCEPTION_REASON},
     * <p>
     * Note that this method allows for update optimizations, but it is not
     * required to operate any differently than
     * {@link #saveRecord(RecordingInfoNode)}.
     * 
     * @param info
     *            the <code>RecordingInfoNode</code> to save to storage
     * @param updateFlag
     *            specifies the field or fields that have been updated
     * 
     * @throws IOException
     *             if there is a problem writing the record
     * @throws IllegalArgumentException
     *             if <i>updateFlag</i> doesn't specify known fields
     */
    public void saveRecord(RecordingInfoNode info, int updateFlag) throws IOException;

    /**
     * Saves the given <code>RecordingInfoTree</code> into persistent storage.
     * If <code>recurse</code> is true then the entire tree is recursed and
     * saved to persistent storage.
     * 
     * @param tree
     * @param recurse
     * @throws IOException
     */
    // TODO: Add this if we need it. Not seen as necessary at this point.
    // public synchronized void saveRecord(RecordingInfoTree tree, boolean
    // recurse)
    // throws IOException;

    /**
     * Deletes the given <code>RecordingInfoNode</code> from persistent storage.
     * A subsequent call to {@link #loadRecords} will not return an instance of
     * the given record. A subsequent call to {@link #saveRecord} can be used to
     * save the record to persistent storage again.
     * <p>
     * This operation fails silently if no representation of the given record
     * exists in persistent storage.
     * 
     * @param info
     *            the record to delete from persistent storage
     */
    public void deleteRecord(RecordingInfoNode info);

    /**
     * Deletes the given <code>RecordingInfoTree</code> from persistent storage.
     * A subsequent call to {@link #loadRecords} will not return an instance of
     * the given record. A subsequent call to {@link #saveRecord} can be used to
     * save the record to persistent storage again.
     * <p>
     * This operation fails silently if no representation of the given record
     * exists in persistent storage.
     * 
     * @param info
     *            the record to delete from persistent storage
     * @param recurse
     *            if <code>true</code> then all children are recursively
     *            deleted; if <code>false</code> then all existing children will
     *            be orphaned
     */
    public void deleteRecord(RecordingInfoTree tree, boolean recurse);

    /**
     * Loads any and all records from persistent storage.
     * <p>
     * The <code>Vector</code> directly contains only <i>root</i>
     * <code>RecordingInfoNode</code>s (i.e., <i>root</i>
     * <code>RecordingInfo</code> and <code>RecordingInfoTree</code> objects).
     * <p>
     * Note that this may allow the creation of multiple
     * <code>RecordingInfoNode</code> objects in memory with duplicate
     * information (including identical {@link RecordingInfoNode#uniqueId
     * uniqueId}'s), if called more than once. It is the responsibility of the
     * caller to manage such conditions.
     * 
     * @return a vector containing zero or more <code>RecordingInfoNode</code>
     *         instances
     */
    public Vector loadRecords();

    /**
     * Value to be used with {@link #saveRecord(RecordingInfoNode, int)} to
     * specify that all fiels are to be updated.
     */
    public static final int ALL = 0xFFFFFFFF;

    /**
     * Value to be used with {@link #saveRecord(RecordingInfoNode, int)} to
     * specify that the {@link RecordingInfoNode#appId} field is to be updated.
     */
    public static final int APP_ID = 1 << 1;

    /**
     * Value to be used with {@link #saveRecord(RecordingInfoNode, int)} to
     * specify that the {@link RecordingInfo#expirationDate} field is to be
     * updated.
     */
    public static final int EXPIRATION_DATE = 1 << 2;

    /**
     * Value to be used with {@link #saveRecord(RecordingInfoNode, int)} to
     * specify that the {@link RecordingInfo#fap} field is to be updated.
     */
    public static final int FAP = 1 << 3;

    /**
     * Value to be used with {@link #saveRecord(RecordingInfoNode, int)} to
     * specify that the {@link RecordingInfo#priority} field is to be updated.
     */
    public static final int PRIORITY = 1 << 4;

    /**
     * Value to be used with {@link #saveRecord(RecordingInfoNode, int)} to
     * specify that the {@link RecordingInfo#bitRate} field is to be updated.
     */
    public static final int BIT_RATE = 1 << 5;

    /**
     * Value to be used with {@link #saveRecord(RecordingInfoNode, int)} to
     * specify that the {@link RecordingInfo#duration} field is to be updated.
     */
    public static final int DURATION = 1 << 6;

    /**
     * Value to be used with {@link #saveRecord(RecordingInfoNode, int)} to
     * specify that the {@link RecordingInfoNode#state} field is to be updated.
     */
    public static final int STATE = 1 << 7;

    /**
     * Value to be used with {@link #saveRecord(RecordingInfoNode, int)} to
     * specify that the {@link RecordingInfo#servieLocator} field is to be
     * updated.
     */
    public static final int SERVICE_LOCATOR = 1 << 8;

    /**
     * Value to be used with {@link #saveRecord(RecordingInfoNode, int)} to
     * specify that the {@link RecordingInfo#serviceName} field is to be
     * updated.
     */
    public static final int SERVICE_NAME = 1 << 9;

    /**
     * Value to be used with {@link #saveRecord(RecordingInfoNode, int)} to
     * specify that the {@link RecordingInfo#recordingId} field is to be
     * updated.
     */
    public static final int RECORDING_ID = 1 << 10;

    /**
     * Value to be used with {@link #saveRecord(RecordingInfoNode, int)} to
     * specify that the {@link RecordingInfo#organization} field is to be
     * updated.
     */
    public static final int ORGANIZATION = 1 << 11;

    /**
     * Value to be used with {@link #saveRecord(RecordingInfoNode, int)} to
     * specify that the {@link RecordingInfo#mediaTime} field is to be updated.
     */
    public static final int MEDIA_TIME = 1 << 12;

    /**
     * Value to be used with {@link #saveRecord(RecordingInfoNode, int)} to
     * specify that the {@link RecordingInfo#} field is to be updated.
     */
    public static final int DELETION_DETAILS = 1 << 13;

    /**
     * Value to be used with {@link #saveRecord(RecordingInfoNode, int)} to
     * specify that the {@link RecordingInfoNode#appDataTable} field is to be
     * updated.
     */
    public static final int APP_DATA_TABLE = 1 << 14;

    /**
     * Value to be used with {@link #saveRecord(RecordingInfoNode, int)} to
     * specify that the {@link RecordingInfoNode#appDataTable} field is to be
     * updated.
     */
    public static final int FAILED_EXCEPTION_REASON = 1 << 15;

}
