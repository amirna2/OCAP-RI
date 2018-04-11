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


package org.cablelabs.impl.ocap.hn.util.xml.miniDom;

// TODO: replace this class by Property class.

/****************************************************
Uses of QualifiedName:

UPnPConstants
    definitions
MetadataNodeImpl
    namespaceName
    localPart
RecordscheduleDirectManual
    namespaceName
    localPart
various constructors throughout
****************************************************/


/**
 * A representation of an XML qualified name, consisting of a namespace name
 * and a local part. (See the XML namespace spec.)
 */
public final class QualifiedName
{
    /**
     * The namespace name.
     */
    private final String namespaceName;

    /**
     * The local part.
     */
    private final String localPart;

    /**
     * Construct a qualified name from a namespace name and a local part.
     *
     * @param namespaceName The namespace name.
     * @param localPart The local part.
     */
    public QualifiedName(String namespaceName, String localPart)
    {
        this.namespaceName = namespaceName;
        this.localPart = localPart;
    }

    /**
     * See if another object equals this qualified name.
     *
     * @param obj The other object (which should be a qualified name).
     *
     * @return True if the other object is a qualified name that equals
     *         this qualified name; else false.
     */
    public boolean equals(Object obj)
    {
        if (! (obj instanceof QualifiedName))
        {
            return false;
        }

        QualifiedName that = (QualifiedName) obj;

        return equals(this.namespaceName, that.namespaceName) && equals(this.localPart, that.localPart);
    }

    /**
     * Compute a hash code for this qualified name.
     *
     * @return The hash code.
     */
    public int hashCode()
    {
        return hashCode(namespaceName) + hashCode(localPart);
    }

    /**
     * Return the local part.
     *
     * @return The local part.
     */
    public String localPart()
    {
        return localPart;
    }

    /**
     * Return the namespace name.
     *
     * @return The namespace name.
     */
    public String namespaceName()
    {
        return namespaceName;
    }

    /**
     * Return a displayable version of the value of this qualified name,
     * for debugging.
     *
     * @return A displayable version of the value of this qualified name.
     */
    public String toString()
    {
        return "<" + toString(namespaceName) + ", " + toString(localPart) + ">";
    }

    /**
     * See if two String references either are both null or compare equal
     * to one another.
     *
     * @param s1 One String reference.
     * @param s2 The other String reference.
     *
     * @return True if the two are both null or compare equal to one another;
     *         else false.
     */
    private static boolean equals(String s1, String s2)
    {
        return s1 == null ? s2 == null : s1.equals(s2);
    }

    /**
     * Compute a hash code for a String reference.
     *
     * @param s The String reference.
     *
     * @return A hash code for it.
     */
    private static int hashCode(String s)
    {
        return s == null ? 0 : s.hashCode();
    }

    /**
     * Return a displayable version of a String reference.
     *
     * @param s The String reference.
     *
     * @return A displayable version of it.
     */
    private static String toString(String s)
    {
        return s == null ? "null" : "\"" + s + "\"";
    }
}
