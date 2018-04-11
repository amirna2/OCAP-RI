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
 * ProgramAssociationTable.java
 */

package org.ocap.si;

/**
 * This interface represents an MPEG-2 Program Association Table (PAT).
 * <p>
 * </p>
 * For an Inband PAT, the getLocator() method defined in the SIElement interface
 * shall return an org.ocap.net.OcapLocator instance corresponding to one of the
 * following OCAP URL forms:
 * <p>
 * </p>
 * <tt>ocap://source_id</tt></br> <tt>ocap://f=frequency.program_number</tt>
 * <p>
 * </p>
 * The form returned must match the form of the OCAP URL passed to the previous
 * call to <tt>ProgramAssociationTableManager.retrieveInBand()</tt> or
 * <tt>ProgramAssociationTableManager.addInBandChangeListener()</tt>.
 * <p>
 * </p>
 * For an OOB PAT, the returned OcapLocator corresponds to the following OCAP
 * URL form:
 * <p>
 * </p>
 * <tt>ocap://oobfdc.program_number</tt>
 * <p>
 * </p>
 * The getServiceInformationType() method defined in the SIElement interface
 * shall return ServiceInformationType.UNKNOWN.
 */
public interface ProgramAssociationTable extends Table
{
    /**
     * Get the program loop in Program Association Table.
     *
     * @return The list of PATProgram which represents the program loop in
     *         Program Association Table.
     */
    public PATProgram[] getPrograms();

    /**
     * Get the identifier for this table.
     *
     * @return The table identifier.
     */
    public short getTableId();
}
