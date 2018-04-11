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

package org.dvb.dsmcc;

import junit.framework.*;

/**
 * Tests the org.dvb.dsmcc package.
 */
public class DsmccTest extends TestCase
{
    public DsmccTest(String name)
    {
        super(name);
    }

    protected void setUp() throws Exception
    {
        super.setUp();
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    public static void main(String[] args)
    {
        try
        {
            junit.textui.TestRunner.run(suite());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        System.exit(0);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite();
        suite.addTest(DSMCCStreamTest.suite());
        suite.addTest(DSMCCStreamEventTest.suite());
        suite.addTest(InvalidFormatEventTest.suite());
        suite.addTest(InvalidPathnameEventTest.suite());
        suite.addTest(LoadingAbortedEventTest.suite());
        suite.addTest(MPEGDeliveryErrorEventTest.suite());
        suite.addTest(NotEntitledEventTest.suite());
        suite.addTest(ServerDeliveryErrorEventTest.suite());
        suite.addTest(ServiceXFRErrorEventTest.suite());
        suite.addTest(SuccessEventTest.suite());
        suite.addTest(NPTRateChangeEventTest.suite());
        suite.addTest(NPTDiscontinuityEventTest.suite());
        suite.addTest(NPTPresentEventTest.suite());
        suite.addTest(NPTRemovedEventTest.suite());
        suite.addTest(ObjectChangeEventTest.suite());
        suite.addTest(StreamEventTest.suite());
        suite.addTest(DSMCCObjectTest.suite());
        suite.addTest(NPTRateTest.suite());
        suite.addTest(ServiceDomainTest.suite());
        suite.addTest(ServiceXFRReferenceTest.suite());
        suite.addTest(DSMCCExceptionTest.suite());
        suite.addTest(IllegalObjectTypeExceptionTest.suite());
        suite.addTest(InsufficientResourcesExceptionTest.suite());
        suite.addTest(InvalidAddressExceptionTest.suite());
        suite.addTest(InvalidFormatExceptionTest.suite());
        suite.addTest(InvalidPathNameExceptionTest.suite());
        suite.addTest(MPEGDeliveryExceptionTest.suite());
        suite.addTest(NotEntitledExceptionTest.suite());
        suite.addTest(NothingToAbortExceptionTest.suite());
        suite.addTest(NotLoadedExceptionTest.suite());
        suite.addTest(ServerDeliveryExceptionTest.suite());
        suite.addTest(ServiceXFRExceptionTest.suite());
        suite.addTest(UnknownEventExceptionTest.suite());

        return suite;
    }

}
