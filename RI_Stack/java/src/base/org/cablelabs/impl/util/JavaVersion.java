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

package org.cablelabs.impl.util;

/**
 * Provides a collection of constants that indicate the current Java version.
 * This is better than checking properties such as java.version because they may
 * not be consistent.
 * <p>
 * The <code>static final</code> constants are determined at runtime, so any
 * operations based on them are required to be runtime operations.
 * <p>
 * Example usage follows:
 * 
 * <pre>
 * public Object getObjectImpl()
 * {
 *     if (JavaVersion.PJAVA_12)
 *        return new PJavaObjectImpl();
 *     else if (JavaVersion.PBP_10)
 *        return new PBPObjectImpl();
 *     else
 *        return new GenericObjectImpl();
 * </pre>
 * 
 * @author Aaron Kamienski
 */
public interface JavaVersion
{
    /**
     * Indicates PJava support. This is static and final but not inlinable.
     */
    boolean PJAVA_12 = JavaVersionImpl.PJAVA_12;

    /**
     * Indicates Java 2 (or greater) support. This is static and final but not
     * inlinable.
     */
    boolean JAVA_2 = JavaVersionImpl.JAVA_2;

    /**
     * Indicates PBP 1.0 (or greater) support. This is static and final but not
     * inlinable.
     */
    boolean PBP_10 = JavaVersionImpl.PBP_10;

    /**
     * This nested class is here just to give us a static initializer so that
     * the PJAVA_12, JAVA_2, and PBP_10 constants can be initialized at
     * run-time.
     */
    abstract class JavaVersionImpl
    {
        private static final boolean PJAVA_12;

        private static final boolean JAVA_2;

        private static final boolean PBP_10;

        private JavaVersionImpl()
        {
        }

        static
        {
            boolean temp;

            /*
             * Test for PJava. We used to test for the existence of the
             * com.sun.util.PTimer class, but that class is now OPTIONAL for
             * PBP, but deprecated. In addition to the clas actually tested
             * below, the following classes are also exclusive to PJava:
             * com.sun.awt.NoInputPreferred com.sun.awt.KeyboardInputPreferred
             * com.sun.awt.ActionInputPreferred
             * com.sun.awt.PositionalInputPreferred
             */
            try
            {
                Class.forName("com.sun.lang.UnsupportedOperationException");
                temp = true;
            }
            catch (Exception e)
            {
                temp = false;
            }
            PJAVA_12 = temp;

            /* Test for Java 1.2 or above */
            // unfortunately, will find PJava also
            // JAVA_2 = ((SecurityManager.class.getModifiers() &
            // Modifier.ABSTRACT) == 0);
            try
            {
                Class.forName("java.awt.ActiveEvent");
                temp = true;
            }
            catch (Exception e)
            {
                temp = false;
            }
            JAVA_2 = temp;

            /* Test for PBP 1.0 or above) */
            temp = false;
            if (JAVA_2)
            {
                try
                {
                    Class.forName("javax.microedition.xlet.Xlet");
                    temp = true;
                }
                catch (Exception e)
                {
                    temp = false;
                }
            }
            PBP_10 = temp;
        }
    }
}
