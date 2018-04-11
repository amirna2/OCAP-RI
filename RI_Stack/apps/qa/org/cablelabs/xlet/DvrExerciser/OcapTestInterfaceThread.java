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

package org.cablelabs.xlet.DvrExerciser;

import org.ocap.test.OCAPTest;
import java.lang.reflect.*;
import java.util.ArrayList;

public class OcapTestInterfaceThread extends Thread
{
    protected DvrExerciser m_dvrExerciser;

    public OcapTestInterfaceThread(DvrExerciser dvrExerciser)
    {
        m_dvrExerciser = dvrExerciser;
    }

    public void run()
    {
        m_dvrExerciser.logIt("OcapTestInterfaceThread: run...");
        while (true)
        {
            try
            {
                byte[] request = OCAPTest.receive();

                String reply = processRequest(new String(request));

                OCAPTest.send(reply.getBytes());
            }
            catch (Exception ex)
            {
                m_dvrExerciser.logIt("OcapTestInterfaceThread: ERROR: " + ex.getMessage());
                ex.printStackTrace();
                try
                {
                    // OCAPTest.send(new String("ERROR: " +
                    // ex.getMessage()).getBytes());
                    OCAPTest.send(("ERROR: " + ex.getMessage()).getBytes());
                }
                catch (Exception exx)
                {
                    // discard
                    m_dvrExerciser.logIt("Unable to send ERROR message!");
                }
            }
        }
    }

    protected String processRequest(String request) throws Exception
    {
        // parse the request, find the DVRExercisor method to call, and then
        // call it

        String[] pieces = parseRequest(request);
        String methodName = pieces[0];

        // get method name
        Class c = m_dvrExerciser.getClass();
        Method[] methods = c.getDeclaredMethods();
        Method myMethod = null;

        for (int i = 0; i < methods.length; i++)
        {

            if (methods[i].getName().equals(methodName))
            {
                Class[] paramTypes = methods[i].getParameterTypes();
                if (paramTypes.length == pieces.length - 1)
                {
                    myMethod = methods[i];
                    break;
                }
            }
        }

        if (myMethod == null)
        {
            throw new Exception("Method " + methodName + " with " + (pieces.length - 1) + " parameters not found");
        }

        Class[] paramTypes = myMethod.getParameterTypes();
        // unused? Class returnType = myMethod.getReturnType();

        Object args[] = new Object[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++)
        {
            // fill in param objects here

            String paramString = pieces[i + 1];
            if (paramTypes[i] == String.class)
            {
                args[i] = paramString;
            }
            else if (paramTypes[i] == Integer.class)
            {
                args[i] = Integer.valueOf(paramString);
            }
            else if (paramTypes[i] == Float.class)
            {
                args[i] = Float.valueOf(paramString);
            }
            else if (paramTypes[i] == Boolean.class)
            {
                args[i] = Boolean.valueOf(paramString);
            }
            else
            {
                throw new Exception("Unsupported parameter class " + paramTypes[i].getName() + " in method " + methodName);
            }
        }

        // call the method
        Object returnVar = myMethod.invoke(m_dvrExerciser, args);

        if (returnVar != null)
        {
            return (String)returnVar;
        }
        return "";
    }

    protected String[] parseRequest(String request)
    {
        String delimiter = "|";

        ArrayList parsedPieces = new ArrayList();

        int startIndex = 0;
        while (true)
        {
            int tempIndex = request.indexOf(delimiter, startIndex);
            if (tempIndex < 0)
            {
                if (startIndex <= request.length() - 1)
                {
                    String piece = request.substring(startIndex, request.length());
                    parsedPieces.add(piece);
                }
                break;
            }

            String piece = request.substring(startIndex, tempIndex);
            parsedPieces.add(piece);

            startIndex = tempIndex + delimiter.length();
        }

        return (String[]) parsedPieces.toArray(new String[0]);
    }
}
