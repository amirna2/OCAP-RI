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

// TODO: replace "new QualifiedName" by "Property.newProperty" and eliminate
//       QualifiedName
// TODO: key() needs to be wrt namespace map
// TODO: key -> name? (elementName and attributeName are already used)
// TODO: add elementName() and attributeName() methods (wrt namespace map)

import java.util.HashMap; // only used for testing
import java.util.Map;
import org.apache.log4j.Logger;

/**
 * TODO
 */
public abstract class Property
{
    /**
     * Log4j logger.
     */
    private static final Logger log = Logger.getLogger(Property.class);

    /**
     * TODO
     */
    private final String key;

    /**
     * TODO
     */
    protected Property(String key)
    {
        assert key != null;

        this.key = key;
    }

    /**
     * Key test method.
     *
     * @param namespaceMap The namespace map with respect to which the key
     *                     is to be tested.
     * @param key          The key to test.
     */
    private static void test(Map namespaceMap, String key)
    {
        System.out.println(key);

        Property p = Property.newProperty(namespaceMap, key);

        System.out.println("  toString returns " + p);
    }

    /**
     * Main program, for testing.
     * <p>
     * Run like this:
     * <code>
         export SYS="$OCAPROOT/bin/$OCAPTC/env/sys"
         java -enableassertions -cp "$SYS/ocap-classes.jar;$SYS/support.jar" org.cablelabs.impl.ocap.hn.util.xml.miniDom.Property
     * </code>
     * <p>
     * TODO: deal with log4j error message
     *
     * @param args Command-line args; not used.
     */
    public static void main(String[] args)
    {
        test(null, "foo");
        test(null, "bar:foo");
        test(null, "foo@goo");
        test(null, "bar:foo@goo");
        test(null, "foo@car:goo");
        test(null, "bar:foo@car:goo");

        Map m = new HashMap();

        m.put("bar", "ns:bar");

        System.out.println();

        test(m, "foo");
        test(m, "bar:foo");
        test(m, "foo@goo");
        test(m, "bar:foo@goo");
        test(m, "foo@car:goo");
        test(m, "bar:foo@car:goo");

        m.put("", "ns:default");
        m.put("car", "ns:car");

        System.out.println();

        test(m, "foo");
        test(m, "bar:foo");
        test(m, "foo@goo");
        test(m, "bar:foo@goo");
        test(m, "foo@car:goo");
        test(m, "bar:foo@car:goo");
    }

    /**
     * TODO
     */
    public String key()
    {
        return key;
    }

    /**
     * TODO
     */
    public static final Property newProperty(Map namespaceMap, String key)
    {
        if (key == null)
        {
            throw new IllegalArgumentException("key is null");
        }

        assert key != null;

        Property result;

        int atSignPosition = key.indexOf('@');

        if (atSignPosition < 0)
        {
            String elementKey = key;

            assert elementKey != null;

            if (elementKey.length() == 0)
            {
                throw new IllegalArgumentException("empty element name in '" + key + "'");
            }

            assert elementKey.length() > 0;

            assert elementKey.indexOf('@') < 0;

            result = new IndependentProperty(namespaceMap, key, elementKey);
        }
        else
        {
            String elementKey = key.substring(0, atSignPosition);

            assert elementKey != null;

            if (elementKey.length() == 0)
            {
                throw new IllegalArgumentException("empty element name in '" + key + "'");
            }

            assert elementKey.length() > 0;

            assert elementKey.indexOf('@') < 0;

            String attributeKey = key.substring(atSignPosition + 1);

            assert attributeKey != null;

            if (attributeKey.length() == 0)
            {
                throw new IllegalArgumentException("empty attribute name in '" + key + "'");
            }

            assert attributeKey.length() > 0;

            if (attributeKey.indexOf('@') >= 0)
            {
                throw new IllegalArgumentException("two ampersands in '" + key + "'");
            }

            assert attributeKey.indexOf('@') < 0;

            result = new DependentProperty(namespaceMap, key, elementKey, attributeKey);
        }

        return result;
    }

    /**
     * TODO
     */
    private static final class IndependentProperty extends Property
    {
        /**
         * TODO
         */
        private final ExpandedName elementName;

        /**
         * TODO
         */
        public IndependentProperty(Map namespaceMap, String key, String elementKey)
        {
            super(key);

            assert elementKey != null && elementKey.length() > 0 && elementKey.indexOf('@') < 0;

            elementName = new ExpandedName(namespaceMap, elementKey, true);
        }

        /**
         * TODO
         */
        public boolean equals(Object obj)
        {
            if (! (obj instanceof IndependentProperty))
            {
                return false;
            }

            IndependentProperty that = (IndependentProperty) obj;

            return this.elementName.equals(that.elementName);
        }

        /**
         * TODO
         */
        public int hashCode()
        {
            return elementName.hashCode();
        }

        /**
         * TODO
         */
        public String toString()
        {
            return elementName.toString();
        }
    }

    /**
     * TODO
     */
    private static final class DependentProperty extends Property
    {
        /**
         * TODO
         */
        private final ExpandedName elementName;

        /**
         * TODO
         */
        private final ExpandedName attributeName;

        /**
         * TODO
         */
        public DependentProperty(Map namespaceMap, String key, String elementKey, String attributeKey)
        {
            super(key);

            assert elementKey != null && elementKey.length() > 0 && elementKey.indexOf('@') < 0;
            assert attributeKey != null && attributeKey.length() > 0 && attributeKey.indexOf('@') < 0;

            elementName = new ExpandedName(namespaceMap, elementKey, true);
            attributeName = new ExpandedName(namespaceMap, attributeKey, false);
        }

        /**
         * TODO
         */
        public boolean equals(Object obj)
        {
            if (! (obj instanceof DependentProperty))
            {
                return false;
            }

            DependentProperty that = (DependentProperty) obj;

            return this.elementName.equals(that.elementName) && this.attributeName.equals(that.attributeName);
        }

        /**
         * TODO
         */
        public int hashCode()
        {
            return elementName.hashCode() + attributeName.hashCode();
        }

        /**
         * TODO
         */
        public String toString()
        {
            return elementName.toString() + "@" + attributeName.toString();
        }
    }

    // TODO: distinguish between two subclasses of ExpandedName, just as we
    //       distinguish between two subclasses of Property?

    /**
     * TODO
     */
    private static final class ExpandedName
    {
        /**
         * TODO
         */
        private final String namespaceName;

        /**
         * TODO
         */
        private final String localName;

        /**
         * TODO
         */
        public ExpandedName(Map namespaceMap, String key, boolean isElement)
        {
            assert key != null && key.length() > 0 && key.indexOf('@') < 0;

            int colonPosition = key.indexOf(':');

            if (colonPosition < 0)
            {
                namespaceName = isElement ? map(namespaceMap, "", key) : null;

                localName = key;

                assert localName != null && localName.length() > 0 && localName.indexOf('@') < 0;

                assert localName.indexOf(':') < 0;
            }
            else
            {
                String namespacePrefix = key.substring(0, colonPosition);

                assert namespacePrefix != null;

                if (namespacePrefix.length() == 0)
                {
                    throw new IllegalArgumentException("empty namespace prefix in '" + key + "'");
                }

                namespaceName = map(namespaceMap, namespacePrefix, key);

                localName = key.substring(colonPosition + 1);

                assert localName != null;

                if (localName.length() == 0)
                {
                    throw new IllegalArgumentException("empty local name in '" + key + "'");
                }

                assert localName.length() > 0;

                assert localName.indexOf('@') < 0;

                if (localName.indexOf(':') >= 0)
                {
                    throw new IllegalArgumentException("two colons in '" + key + "'");
                }

                assert localName.indexOf(':') < 0;
            }
        }

        /**
         * TODO
         */
        public boolean equals(Object obj)
        {
            if (! (obj instanceof ExpandedName))
            {
                return false;
            }

            ExpandedName that = (ExpandedName) obj;

            return equals(this.namespaceName, that.namespaceName) && this.localName.equals(that.localName);
        }

        /**
         * TODO
         */
        public int hashCode()
        {
            return hashCode(namespaceName) + localName.hashCode();
        }

        /**
         * TODO
         */
        public String toString()
        {
            return toString(namespaceName) + ":" + localName;
        }

        /**
         * TODO
         */
        private static boolean equals(String s, String t)
        {
            return s == null ? t == null : s.equals(t);
        }

        /**
         * TODO
         */
        private static int hashCode(String s)
        {
            return s == null ? 0 : s.hashCode();
        }

        /**
         * TODO
         */
        private static String map(Map namespaceMap, String namespacePrefix, String key)
        {
            assert namespacePrefix != null;

            assert namespacePrefix.indexOf('@') < 0;

            assert namespacePrefix.indexOf(':') < 0;

            if (namespaceMap == null)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Property.map: for key '" + key + "', namespace map is null");
                }

                return null;
            }

            assert namespaceMap != null;

            Object obj = namespaceMap.get(namespacePrefix);

            if (obj == null)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Property.map: for key '" + key + "', namespace prefix is undeclared");
                }

                return null;
            }

            assert obj != null;

            if (! (obj instanceof String))
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Property.map: for key '" + key + "', namespace prefix is incorrectly declared");
                }

                return null;
            }

            assert obj instanceof String;

            return (String) obj;
        }

        /**
         * TODO
         */
        private static String toString(String s)
        {
            return s == null ? "null" : "<" + s + ">";
        }
    }
}
