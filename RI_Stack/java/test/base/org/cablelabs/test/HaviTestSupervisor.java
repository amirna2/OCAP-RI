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

package org.cablelabs.test;

import junit.framework.*;
import junit.textui.*;
import java.io.*;
import java.net.*;
import java.lang.reflect.Method;
import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

/**
 * @author Sumathi xlet wrapper to run Havitests
 */

public class HaviTestSupervisor implements Xlet
{
    private static final int VTE_VTM_DONE = 0x00010003;

    private String testClassName = null;

    /**
     * The Xlet context passed to initXlet(XletContext). Lock the instance
     * monitor before modifying.
     */
    public static XletContext haviTestXletContext = null;

    static class TestResultPrinter extends ResultPrinter
    {
        TestResultPrinter(PrintStream writer)
        {
            super(writer);
        }

        /*
         * Spoof printing time so the tests are deterministic
         */
        protected String elapsedTimeAsString(long runTime)
        {
            return "0";
        }
    }

    /**
     * The Xlet context passed to initXlet(XletContext). Lock the instance
     * monitor before modifying.
     */
    // private XletContext xletContext;

    //
    // Xlet implementation
    //

    /**
     * Signal this Xlet to initialize itself and enter the Paused state.
     * 
     * @param context
     *            The context for use by the Xlet discover information about its
     *            environment and to signal internal state changes.
     * @throws XletStateChangeException
     *             If the Xlet cannot be initialized.
     */
    public void initXlet(XletContext context) throws XletStateChangeException
    {
        System.out.println("Calling HaviTestSupervisor initXlet()");
        this.haviTestXletContext = context;
    }

    /**
     * Signal this Xlet to start providing service and enter the Active state.
     * 
     * If the TestSupervisor has not already started the test, it starts it
     * here. The test is started in a new thread.
     * 
     * @throws XletStateChangeException
     *             If the Xlet cannot be started.
     */
    public void startXlet() throws XletStateChangeException
    {
        System.out.println("Calling HaviTestSupervisor startXlet()");
        String[] app_args = (String[]) haviTestXletContext.getXletProperty(XletContext.ARGS);
        testClassName = app_args[0];
        runHaviTests();
    }

    /**
     * Signal this Xlet to stop providing service and enter the Paused state.
     */
    public void pauseXlet()
    {
    }

    /**
     * Signal this Xlet to terminate and enter the Destroyed state.
     * 
     * @param unconditional
     *            If unconditional is true when this method is called, requests
     *            by the Xlet to not enter the destroyed state will be ignored.
     * @throws XletStateChangeException
     *             If the Xlet wishes to continue to execute (Not enter the
     *             Destroyed state). This exception is ignored if unconditional
     *             is equal to true.
     */
    public void destroyXlet(boolean unconditional) throws XletStateChangeException
    {
    }

    /**
     * runHaviTests() - Run the test using TestRunner. My goal here is to invoke
     * suite() or suite(class) method in the test class file, extract the test
     * suite and run using TestRunner.doRun(). Incase the test class doesn't
     * have suite()/suite(class) method defined, then I create a new test suite
     * and run using TestRunner.doRun().
     * 
     * The reasons for not invoking "main" method of the test class are, 1. The
     * main() method may have call to system.exit(0) which will bring down the
     * VM. This may be okay when run as unit test. But we don't want this to
     * happen when running in VTE environment. 2. The main() method may have a
     * try-catch block. This may be okay when run as unit test. But for running
     * in VTE setup, we want the test wrapper to catch the exception and do
     * further processing. 3. By not using main(), I can run the test using a
     * separate ResultPrinter for all the test output. In this case I am using a
     * ByteArrayOutputStream() as the ResultPrinter. Data from this byte array
     * is sent to the server.
     */

    public void runHaviTests()
    {
        junit.textui.TestRunner runner = null;
        Class testClass = null;
        TestSuite newSuite = null;
        ByteArrayOutputStream bResult = new ByteArrayOutputStream();
        PrintStream pout = new PrintStream(bResult);

        /*
         * Note: For some reason I am not able to get class.getDeclaredMethod()
         * to work. Instead I am using class.getDeclaredMethods(). Extract
         * suite()/suite(class) method out of the list of methods and invoke it
         * - Sumathi H.
         */
        runner = new junit.textui.TestRunner(new TestResultPrinter(pout));
        try
        {
            testClass = Class.forName(testClassName);
            Method[] methods = testClass.getDeclaredMethods();
            int i;
            for (i = 0; i < methods.length; i++)
            {
                // System.out.println("...........methods[i] = " +
                // methods[i].getName());
                // checking to see if the test has 'suite' method
                if (methods[i].getName().equals("suite"))
                {
                    // System.out.println("...........................run suite()");
                    // get the parameter types for this 'suite' method
                    Class[] clsArray = methods[i].getParameterTypes();
                    // if parameter types is null then invoke "suite()" method.
                    if (clsArray.length == 0)
                    {
                        // System.out.println("Invoke suite() method ");
                        newSuite = (TestSuite) methods[i].invoke(null, null);
                    }
                    else
                    {
                        // invoke "suite(class)" method with the test class
                        // object as parameter type.
                        // System.out.println("Invoke suite(Class) method ");
                        Object[] objArray = new Object[1];
                        objArray[0] = testClass;
                        newSuite = (TestSuite) methods[i].invoke(null, objArray);
                    }
                    break;
                }
            }
            if (i == methods.length)
            {
                // System.out.println("No suite() or suite(Class) method. create a new test suite");
                newSuite = new TestSuite(testClass);
            }
        }
        catch (ClassNotFoundException ce)
        {
            System.out.println("\n\nFailed to Launch Test: " + testClassName + " Exception: " + ce + "\n");
            ce.printStackTrace(pout);
            ce.printStackTrace();
            sendDoneMessage(bResult);
            return;
        }
        catch (SecurityException se)
        {
            System.out.println("\n\nFailed to Launch Test: " + testClassName + " Exception: " + se + "\n");
            se.printStackTrace(pout);
            se.printStackTrace();
            sendDoneMessage(bResult);
            return;
        }
        catch (Exception e)
        {
            System.out.println("\n\nFailed to invoke suite()/suite(class). Creating a test suite: " + testClassName
                    + " Exception: " + e + "\n");
            // Failed to invoke suite()/suite(class). Create a test suite
            // e.printStackTrace();
            newSuite = new TestSuite(testClass);
        }

        // Execute the test
        try
        {
            runner.doRun(newSuite);
        }
        catch (Exception e1)
        {
            e1.printStackTrace(pout);
            e1.printStackTrace();
        }
        System.out.println("\n\n*********************************** RESULTS ************************************");
        System.out.println("TESTNAME: " + testClassName);
        System.out.println(bResult.toString());
        System.out.println("********************************************************************************\n");
        sendDoneMessage(bResult);
    }

    /**
     * Construct VTE_VTM_DONE message and send it to VTE agent over a UDP
     * socket. The message structure is... VTE_VTM_DONE: // current test
     * execution is complete Message{ Uint32 message; Uint32 messageBodySize;
     * //number of bytes following this field Uint16 classNameSize; Byte
     * bytes[classNameSize]; Uint8 versionNumber; Uint8 result; Uint16
     * resultMessageSize; Byte bytes[messageSize]; }
     */
    private void sendDoneMessage(ByteArrayOutputStream bResult)
    {
        boolean result = false;
        byte[] errorMsg = null;
        String passedMsg = "Passed";
        byte[] message = null;

        String s = (String) HaviTestSupervisor.haviTestXletContext.getXletProperty("app.1.application_version");
        int versionNumber = Integer.parseInt(s);

        try
        {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            DataOutputStream dout = new DataOutputStream(bout);
            // Uint32 message
            dout.writeInt(VTE_VTM_DONE);
            // Uint32 messageBodySize; //number of bytes following this field
            dout.writeInt(0xFFFFFFFF);
            // Uint16 classNameSize;
            dout.writeShort(testClassName.length());
            // Byte bytes[classNameSize];
            dout.writeBytes(testClassName);
            // Uint8 versionNumber;
            dout.writeByte(versionNumber);
            errorMsg = bResult.toByteArray();
            if (errorMsg == null || errorMsg.length == 0)
                result = true;
            else
                result = false;
            // Uint8 result;
            dout.writeBoolean(result);
            if (result == true)
            {
                // Uint16 resultMessageSize
                dout.writeShort(passedMsg.getBytes().length);
                // Byte bytes[messageSize];
                dout.writeBytes(passedMsg);
            }
            else
            {
                // Uint16 resultMessageSize
                dout.writeShort(errorMsg.length);
                // Byte bytes[messageSize];
                dout.write(errorMsg);
            }

            // Uint32 messageBodySize; //number of bytes following this field
            int size = dout.size() - 8; // total size - (message_body_size bytes
                                        // + message bytes)
            message = bout.toByteArray();
            message[4] = (byte) (size >> 24);
            message[5] = (byte) (size >> 16);
            message[6] = (byte) (size >> 8);
            message[7] = (byte) (size);;
            dout.close();
        }
        catch (Exception e)
        {
            System.out.println("Error while creating done message - " + e.getMessage());
        }

        // Send the message to VTE agent over the UDP socket. This message is
        // received by MessageSender
        // and sent to the server.

        try
        {
            DatagramSocket udpSocket = new DatagramSocket();
            DatagramPacket packet = new DatagramPacket(message, message.length, InetAddress.getByName("127.0.0.1"),
                    5200);
            udpSocket.send(packet);
        }
        catch (Exception e)
        {
            System.out.println("Exception - Unable to send 'verify image' message to VTE agent - " + e.toString());
        }
    }
}
