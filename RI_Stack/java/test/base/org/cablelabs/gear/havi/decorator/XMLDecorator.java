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

package com.snaptwo.gear.havi.decorator;

import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import com.snaptwo.gear.data.Locator;
import archiver.XMLInputStream;

/**
 * <code>XMLDecorator</code> is an extension of {@link DecoratorLook} which is
 * capable of dealing with it's component decorator look chain in terms of XML
 * data.
 * 
 * An instance of <code>XMLDecorator</code> can be queried for it's contained
 * decorator chain represented by a XML string. Likewise, an instance can have
 * it's decorator chain set using a XML description of that chain.
 * 
 * @author Jay Tracy
 * @version $Id: XMLDecorator.java,v 1.2 2002/06/03 21:32:31 aaronk Exp $
 */
public class XMLDecorator extends DecoratorLook
{
    /**
     * Constructor to create a new <code>XMLDecorator</code> with no component
     * look.
     */
    public XMLDecorator()
    {
        super(null);
    }

    /**
     * Constructor to create a new <code>XMLDecorator</code> with the decorator
     * chain represented by the specified XML data.
     * 
     * @param xml
     *            The XML representation of the decorator chain to be set as the
     *            component look.
     * 
     * @see #setXMLDescription
     * @see #getXMLDescription
     */
    public XMLDecorator(String xml)
    {
        super(null);

        try
        {
            setXMLDescription(xml);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Constructor to create a new <code>XMLDecorator</code> with the decorator
     * chain represented by the XML data contained in the specified file.
     * 
     * @param locator
     *            The {@link com.snaptwo.gear.data.Locator Locator} object which
     *            specifies the file containing the XML description.
     * 
     * @see #setXMLLocator(Locator)
     * @see #getXMLLocator()
     */
    public XMLDecorator(Locator locator)
    {
        super(null);

        try
        {
            setXMLLocator(locator);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Returns an XML representation of the component decorator chain. This XML
     * data represents each <code>DecoratorLook</code> in the chain along with
     * any properties which have non-default values.
     * 
     * @return an XML representation of the component decorator chain.
     * 
     * @throws java.io.IOException
     */
    /*
     * public String getXMLDescription() throws IOException {
     * ByteArrayOutputStream out = new ByteArrayOutputStream();
     * 
     * XMLOutputStream encoder = new XMLOutputStream(new
     * BufferedOutputStream(out)); encoder.writeObject(getComponentLook());
     * encoder.close();
     * 
     * return(out.toString()); }
     */

    /**
     * Recreates a decorator chain based on the specified XML data, and sets
     * that chain as the component look.
     * 
     * @param description
     *            The XML description of the component decorator chain.
     * 
     * @throws java.io.IOException
     */
    public void setXMLDescription(String description) throws IOException
    {
        ByteArrayInputStream in = new ByteArrayInputStream(description.getBytes());
        setXMLDescription(in);
    }

    /**
     * Recreates a decorator chain based on the XML data from the specified
     * <code>InputStream</code>, and sets that chain as the component look.
     * 
     * @param in
     *            The <code>InputStream</code> containing the XML description of
     *            the component decorator chain.
     * 
     * @throws java.io.IOException
     */
    public void setXMLDescription(InputStream in) throws IOException
    {
        XMLInputStream decoder = null;
        org.havi.ui.HLook newLook = null;

        decoder = new XMLInputStream(in);
        newLook = (org.havi.ui.HLook) decoder.readObject();
        decoder.close();

        setComponentLook(newLook);
    }

    /**
     * Sets the locator property for a <code>XMLDecorator</code>. This property
     * holds a <code>Locator</code> object which specifies a file containing the
     * XML data for this <code>XMLDecorator</code>.
     * 
     * @param locator
     *            The <code>Locator</code> that specifies where the XML data can
     *            be found
     * 
     * @throws java.io.IOException
     */
    public void setXMLLocator(Locator locator) throws IOException
    {
        this.locator = locator;
        setXMLDescription(locator.getLocation().openStream());
    }

    /**
     * Retrieves the <code>Locator</code> which specifies the file containing
     * the XML description of the decorator chain.
     * 
     * @return The <code>Locator</code> that specifies where the XML data can be
     *         found
     */
    public Locator getXMLLocator()
    {
        return locator;
    }

    private Locator locator;
}
