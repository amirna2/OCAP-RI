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

package org.cablelabs.impl.davic.mpeg.sections;

import org.davic.mpeg.sections.NoDataAvailableException;
import org.davic.mpeg.sections.Section;

/**
 * This class describes a single section as filtered from an MPEG transport
 * stream.
 *
 * A cloned Section object is a new and separate object. It is unaffected by
 * changes in the state of the original Section object or restarting of the
 * SectionFilter the source Section object originated from. The clone method
 * must be implemented without declaring exceptions.
 *
 * @version updated to DAVIC 1.3.1
 */
public class BasicSection extends Section
{

    // here to stop javadoc generating a constructor
    public BasicSection(byte[] buffer)
    {
        super(buffer);
        this.cache = buffer;
    }

    public BasicSection()
    {
        super();
    }

    /**
     * This method returns all data from the filtered section in the Section
     * object, including the section header. Each call to this method results in
     * a new a copy of the section data.
     *
     * @exception NoDataAvailableException
     *                if no valid data is available.
     */
    public byte[] getData() throws NoDataAvailableException
    {
        try
        {
            byte[] cache = this.cache;
            byte[] copy = new byte[cache.length];

            System.arraycopy(cache, 0, copy, 0, cache.length);

            return copy;
        }
        catch (NullPointerException e)
        {
            throw new NoDataAvailableException();
        }
    }

    /**
     * This method returns the specified part of the filtered data. Each call to
     * this method results in a new a copy of the section data.
     *
     * @param index
     *            defines within the filtered section the index of the first
     *            byte of the data to be retrieved. The first byte of the
     *            section (the table_id field) has index 1.
     * @param length
     *            defines the number of consecutive bytes from the filtered
     *            section to be retrieved.
     * @exception NoDataAvailableException
     *                if no valid data is available.
     * @exception java.lang.IndexOutOfBoundsException
     *                if any part of the filtered data requested would be
     *                outside the range of data in the section.
     */
    public byte[] getData(int index, int length) throws NoDataAvailableException, java.lang.IndexOutOfBoundsException
    {
        if (length < 0) throw new IndexOutOfBoundsException();

        try
        {
            byte[] cache = this.cache;
            byte[] copy = new byte[length];

            System.arraycopy(cache, index - 1, copy, 0, length);

            return copy;
        }
        catch (NullPointerException e)
        {
            throw new NoDataAvailableException();
        }
    }

    /**
     * This method returns one byte from the filtered data.
     *
     * @param index
     *            defines within the filtered section the index of the byte to
     *            be retrieved. The first byte of the section (the table_id
     *            field) has index 1.
     * @exception NoDataAvailableException
     *                if no valid data is available.
     * @exception java.lang.IndexOutOfBoundsException
     *                if the byte requested would be outside the range of data
     *                in the section.
     */
    public byte getByteAt(int index) throws NoDataAvailableException, java.lang.IndexOutOfBoundsException
    {
        try
        {
            // table_id field has index 1, but is first byte
            return cache[index - 1];
        }
        catch (NullPointerException e)
        {
            throw new NoDataAvailableException();
        }
    }

    /**
     * This method returns the value of the corresponding field from an MPEG-2
     * section header.
     *
     * @exception NoDataAvailableException
     *                thrown if no valid data is available
     */
    public int table_id() throws NoDataAvailableException
    {
        return getByteAt(1) & 0xFF;
    }

    /**
     * This method returns the value of the corresponding field from an MPEG-2
     * section header.
     *
     * @exception NoDataAvailableException
     *                thrown if no valid data is available
     */
    public boolean section_syntax_indicator() throws NoDataAvailableException
    {
        return (getByteAt(2) & 0x80) != 0;
    }

    /**
     * This method returns the value of the corresponding field from an MPEG-2
     * section header.
     *
     * @exception NoDataAvailableException
     *                thrown if no valid data is available
     */
    public boolean private_indicator() throws NoDataAvailableException
    {
        return (getByteAt(2) & 0x40) != 0;
    }

    /**
     * This method returns the value of the corresponding field from an MPEG-2
     * section header.
     *
     * @exception NoDataAvailableException
     *                thrown if no valid data is available
     */
    public int section_length() throws NoDataAvailableException
    {
        return getShortAt(2) & 0xFFF;
    }

    /**
     * This method returns the value of the corresponding field from an MPEG-2
     * section header.
     *
     * @exception NoDataAvailableException
     *                thrown if no valid data is available
     */
    public int table_id_extension() throws NoDataAvailableException
    {
        return getUShortAt(4);
        // if(section_syntax_indicator()==false)
        // throw new NoDataAvailableException("The Section doesn't have
        // table_Id_extension");
    }

    /**
     * This method returns the value of the corresponding field from an MPEG-2
     * section header.
     *
     * @exception NoDataAvailableException
     *                thrown if no valid data is available
     */
    public short version_number() throws NoDataAvailableException
    {
        return (short) ((getByteAt(6) >> 1) & 0x1F);

        // if(section_syntax_indicator()==false)
        // throw new NoDataAvailableException("The Section doesn't have Version
        // number");
    }

    /**
     * This method returns the value of the corresponding field from an MPEG-2
     * section header.
     *
     * @exception NoDataAvailableException
     *                thrown if no valid data is available
     */
    public boolean current_next_indicator() throws NoDataAvailableException
    {
        return (getByteAt(6) & 1) != 0;
        // if(section_syntax_indicator()==false)
        // throw new NoDataAvailableException("The Section doesn't have
        // current_next_indicator");
    }

    /**
     * This method returns the value of the corresponding field from an MPEG-2
     * section header.
     *
     * @exception NoDataAvailableException
     *                thrown if no valid data is available
     */
    public int section_number() throws NoDataAvailableException
    {
        return getUByteAt(7);
        // if(section_syntax_indicator()==false)
        // throw new NoDataAvailableException("The Section doesn't have
        // section_number");
    }

    /**
     * This method returns the value of the corresponding field from an MPEG-2
     * section header.
     *
     * @exception NoDataAvailableException
     *                thrown if no valid data is available
     */
    public int last_section_number() throws NoDataAvailableException
    {
        return getUByteAt(8);
        // if(section_syntax_indicator()==false)
        // throw new NoDataAvailableException("The Section doesn't have
        // last_section_number");
    }

    /**
     * This method reads whether a Section object contains valid data.
     *
     * @return true when the Section object contains valid data otherwise false
     */
    public boolean getFullStatus()
    {
        try
        {
            byte[] cache = this.cache;
            return cache != null && cache.length == section_length() + 3;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    /**
     * This method sets a Section object such that any data contained within it
     * is no longer valid. This is intended to be used with RingSectionFilters
     * to indicate that the particular object can be re-used.
     */
    public void setEmpty()
    {
        // Obviously, would have more meaning for RingSectionFilters
        cache = null;
    }

    /**
     * Create a copy of this Section object. A cloned Section object is a new
     * and separate object. It is unaffected by changes in the state of the
     * original Section object or restarting of the SectionFilter the source
     * Section object originated from.
     *
     */
    public Object clone()
    {
        //findbugs detected - clone needs to call super.clone()
        BasicSection returnThis = (BasicSection)super.clone();
        returnThis.cache = this.cache;
        return returnThis;
    }

    /**
     * Accesses a short at the given index.
     *
     * @param index
     * @return a short at the given index
     */
    protected short getShortAt(int index) throws NoDataAvailableException
    {
        return (short) ((getByteAt(index) << 8) | getUByteAt(index + 1));
    }

    /**
     * Accesses an unsigned byte at the given index.
     *
     * @param index
     * @return an unsigned at the given index
     */
    int getUByteAt(int index) throws NoDataAvailableException
    {
        return getByteAt(index) & 0xFF;
    }

    /**
     * Accesses an unsigned short at the given index.
     *
     * @param index
     * @return an unsigned short at the given index
     */
    int getUShortAt(int index) throws NoDataAvailableException
    {
        return getShortAt(index) & 0xFFFF;
    }

    /**
     * Cache of byte array that make up this section.
     */
    protected volatile byte[] cache;
}
