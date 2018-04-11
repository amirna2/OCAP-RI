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
 * Created on Jul 13, 2006
 */
package org.cablelabs.impl.davic.mpeg.sections;

import org.davic.mpeg.sections.NoDataAvailableException;
import org.davic.mpeg.sections.Section;

/**
 * A <code>ProxySection</code> serves as a proxy for another instance of
 * <code>Section</code>. The use of a <code>ProxySection</code> allows for a
 * level of indirection, where a fixed <code>ProxySection</code> instance can be
 * referenced, with the <i>real</i> <code>Section</code> upon which it is based
 * changing over time.
 * <p>
 * The <i>real</i> <code>Section</code> can be set at
 * {@link #ProxySection(Section) construction} or via
 * {@link #setSection(Section)}. The <i>real</i> <code>Section</code> is
 * implicitly cleared via {@link #setEmpty}. The <i>real</I>
 * <code>Section</code> may be retrieved at any point via {@link #getSection}.
 * 
 * @author Aaron Kamienski
 */
public class ProxySection extends BasicSection
{

    /**
     * Creates an instance of <code>ProxySection</code> that wraps the given
     * <i>section</i>.
     * 
     * @param base
     *            the section decorated by this <code>ProxySection</code>; may
     *            be <code>null</code>
     * 
     * @see #getSection
     * @see #setSection
     */
    public ProxySection(Section section)
    {
        super();
        base = section;
    }

    /**
     * Creates an empty <code>ProxySection</code>.
     * 
     * Calling {@link #getSection} will return <code>null</code> until
     * {@link #setSection} is called with a non-<code>null</i> section.
     */
    public ProxySection()
    {
        this(null);
    }

    /**
     * Updates the section wrapped by this <code>ProxySection</code>. If
     * <i>section</i> is <code>null</code> all subsequent calls upon this
     * <code>Section</code> will fail (e.g., with
     * {@link NoDataAvailableException}) until <code>setSection()</code> is
     * invoked with a non-<code>null</code> <code>Section</code>.
     * 
     * @param section
     *            the new section or <code>null</code>
     */
    public void setSection(Section section)
    {
        base = section;
    }

    /**
     * Returns the currently wrapped <code>Section</code> or <code>null</code>.
     * A valud of <code>null</code> indicates that this <code>Section</code> is
     * empty (either because it was never full or it was explicitly emptied via
     * {@link #setSection} or {@link #setEmpty()}.
     * 
     * @return the currently wrapped <code>Section</code> or <code>null</code>
     */
    public Section getSection()
    {
        return base;
    }

    /**
     * Overrides {@link BasicSection#setEmpty()} forwarding the request to the
     * <i>base</i> {@link Section section}. Invokes {@link #setSection
     * setSection(null)} to forget the current <i>base</i>.
     */
    public void setEmpty()
    {
        Section tmp = base;

        setSection(null);
        try
        {
            tmp.setEmpty();
        }
        catch (NullPointerException e)
        {
            // Already empty
        }
    }

    /**
     * Implements {@link BasicSection#clone()}, always returning a new
     * <code>ProxySection</code> of a clone of the the original <i>base</i>
     * {@link Section section}.
     */
    public Object clone()
    {
        try
        {
            return new ProxySection((Section) base.clone());
        }
        catch (NullPointerException e)
        {
            return new ProxySection();
        }
    }

    /**
     * Overrides {@link BasicSection#getByteAt(int)} forwarding the request to
     * the <i>base</i> {@link Section section}.
     */
    public byte getByteAt(int index) throws NoDataAvailableException, IndexOutOfBoundsException
    {
        try
        {
            return base.getByteAt(index);
        }
        catch (NullPointerException e)
        {
            throw new NoDataAvailableException();
        }
    }

    /**
     * Overrides {@link BasicSection#getData()} forwarding the request to the
     * <i>base</i> {@link Section section}.
     */
    public byte[] getData() throws NoDataAvailableException
    {
        try
        {
            return base.getData();
        }
        catch (NullPointerException e)
        {
            throw new NoDataAvailableException();
        }
    }

    /**
     * Overrides {@link BasicSection#getData(int, int)} forwarding the request
     * to the <i>base</i> {@link Section section}.
     */
    public byte[] getData(int index, int length) throws NoDataAvailableException, IndexOutOfBoundsException
    {
        try
        {
            return base.getData(index, length);
        }
        catch (NullPointerException e)
        {
            throw new NoDataAvailableException();
        }
    }

    /**
     * Overrides {@link BasicSection#getFullStatus()} forwarding the request to
     * the <i>base</i> {@link Section section}.
     */
    public boolean getFullStatus()
    {
        try
        {
            return base.getFullStatus();
        }
        catch (NullPointerException e)
        {
            return false;
        }
    }

    /**
     * Overrides {@link BasicSection#getShortAt(int)} forwarding the request to
     * the <i>base</i> {@link Section section}.
     */
    protected short getShortAt(int index) throws NoDataAvailableException
    {
        try
        {
            try
            {
                BasicSection basic = (BasicSection) base;
                return basic.getShortAt(index);
            }
            catch (ClassCastException e)
            {
                byte[] data = base.getData(index, 2);
                return (short) (data[0] << 8 | data[1]);
            }
        }
        catch (NullPointerException e)
        {
            throw new NoDataAvailableException();
        }
    }

    private volatile Section base;
}
