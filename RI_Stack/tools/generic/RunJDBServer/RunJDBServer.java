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

import java.io.BufferedReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

public class RunJDBServer
{
    private final static int SO_TIMEOUT_MS = 30000;
    private static final int defaultListenPort = 8081;
    private static final String jdbInvocation = "jdb -connect com.sun.jdi.SocketAttach:hostname=127.0.0.1,port=8000";
    private static final String jdbCommands = "suspend\nwhere all\nthreadlocks all\nresume\nexit\n";

    private static void printUsage()
    {
        System.out.println("RunJDBServer usage:");
        System.out.println("RunJDBServer <listenPort>");
        System.out.println("Where listenPort is the port to listen for TCP connections");
        System.out.println("RunJDBServer will execute the following when a connection is opened:");
        System.out.println(jdbInvocation);
    }
    public static void main(String[] args)
    {
        System.out.println("RunJDBServer: Startup.");
        
        int listenPort;
        if (args.length == 0)
        {
            System.out.println("Using default listen port: " + defaultListenPort);
            listenPort = defaultListenPort;
        }
        else if (args.length == 1)
        {
            try
            {
                listenPort = Integer.parseInt(args[0]);
            }
            catch (NumberFormatException nfe)
            {
                System.out.println("Bad port number: " + args[0]);
                printUsage();
                return;
            }
        }
        else
        {
            printUsage();
            return;
        }
        
        try
        {
            ServerSocket serverSocket = new ServerSocket(listenPort, 50, InetAddress.getLocalHost());
            Thread listenerThread = new Thread(new SocketListener(serverSocket));
            listenerThread.start();
            synchronized(listenerThread)
            {
                System.out.println("Main thread waiting.");
                listenerThread.wait();
            }
        }
        catch (Exception e)
        {
            System.out.println("Error starting JDB");
            e.printStackTrace();
        }
    }

    /**
     * Inner class to accept socket connections and perform thread dumps
     * 
     * @author craig
     */
    public static class SocketListener implements Runnable
    {
        private final ServerSocket m_serverSocket;
        private boolean running = true;
        
        public SocketListener(ServerSocket socket)
        {
            m_serverSocket = socket;
        }
        
        public void run()
        {
            try
            {
                while(running)
                {
                    System.out.println("");
                    System.out.println( "Waiting for connection on " 
                            + m_serverSocket.getInetAddress() + ":" 
                            + m_serverSocket.getLocalPort());
                    
                    Socket socket = m_serverSocket.accept();
                    socket.setSoTimeout(SO_TIMEOUT_MS);
                    
                    Date now = new Date();
                    System.out.println("Accepted connection from " + socket.getInetAddress()
                                       + " at " + now);
                    runJDBAndCollectBacktraces();
                    socket.close();
                 }
            }
            catch (IOException e)
            {
                System.out.println("Socket IOException occurred.");
                e.printStackTrace();
            }
        }
        
        public void stop()
        {
            running = false;
            this.notifyAll();
        }
        
        int runJDBAndCollectBacktraces()
        {
            java.lang.Runtime runtime = java.lang.Runtime.getRuntime();
            Process jdbProcess;
            
            java.io.BufferedWriter jdbWriter;
            try
            {
                System.out.println("Running command: " + jdbInvocation);
                jdbProcess = runtime.exec(jdbInvocation);
                System.out.println("Exec completed (JDB process " + jdbProcess + ')');
                
                jdbWriter = new java.io.BufferedWriter(new java.io.OutputStreamWriter(jdbProcess.getOutputStream()));
            }
            catch (IOException e)
            {
                System.out.println("Error starting JDB");
                e.printStackTrace();
                return 2;
            }
            
            Thread outReaderThread = new Thread(new ReadAndPrintThread(jdbProcess.getInputStream(), "jdb output: "));
            Thread errReaderThread = new Thread(new ReadAndPrintThread(jdbProcess.getErrorStream(), "jdb error: "));
            outReaderThread.start();
            errReaderThread.start();
            
            try
            {
                Thread.sleep(200);
                System.out.println("main thread: Issuing command");
                jdbWriter.write(jdbCommands);
                jdbWriter.flush();
                System.out.println("main thread: done");
            }
            catch (IOException e)
            {
                System.out.println("Error writing command: " + e.getMessage());
            }
            catch (InterruptedException e)
            {
                System.out.println("Error sleeping: " + e.getMessage());
            }
            
//            jdbProcess.destroy(); // This should cause the reader/writer threads to complete...
            return 0;
        } // END dumpOutstandingTaskBacktraces()
    } // END class SocketListener

    static public class ReadAndPrintThread implements Runnable
    {
        private final BufferedReader bufReader;
        private String linePrefix;
        private boolean keepReading;
        
        ReadAndPrintThread(java.io.InputStream is, String linePrefix)
        {
            this.bufReader = new java.io.BufferedReader(new java.io.InputStreamReader(is));
            this.linePrefix = linePrefix;
            this.keepReading = true;
        }

        public void run()
        {
            System.out.println(linePrefix + "started");
            try
            {
                String nextLine;
                while (keepReading && ((nextLine = bufReader.readLine()) != null))
                {
                    System.out.println(linePrefix + nextLine);
                }
            }
            catch (IOException e)
            {
                System.out.println(linePrefix + "Error reading from input: " + e.getMessage());
            }
            System.out.println(linePrefix + "completed");
        }
        
        public void stopReading()
        {
            this.keepReading = false;
        }
    } // END class ReadAndPrintThread
}
