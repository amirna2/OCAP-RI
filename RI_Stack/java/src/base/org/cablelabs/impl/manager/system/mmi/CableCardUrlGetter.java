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

package org.cablelabs.impl.manager.system.mmi;

import org.cablelabs.impl.manager.system.APDUReader;
import org.cablelabs.impl.manager.system.APDUWriter;
import org.cablelabs.impl.manager.system.APDUReader.APDUReadException;
import org.cablelabs.impl.manager.system.html.UrlGetter;
import org.cablelabs.impl.manager.system.html.UrlGetterListener;
import org.ocap.system.SystemModule;
import org.ocap.system.SystemModuleHandler;

public class CableCardUrlGetter implements UrlGetter, SystemModuleHandler {
    private static CableCardUrlGetter instance = null;
    private static int instanceCount = 0;
    
    private static final int TAG_SERVER_QUERY = 0x9F8022;
    private static final int TAG_SERVER_REPLY = 0x9F8023;
    
    private static final int FILE_STATUS_OK = 0;

    private final CyclicCounterMap pendingServerQueries = new CyclicCounterMap();
    
    private SystemModule systemModule;


    /** 
     * Returns a CableCardUrlGetter and updates internal state to
     * indicate that a session is in progress.  The session must
     * be closed with releaseSession() once the CableCardUrlGetter
     * is no longer being used.
     */
    public synchronized static CableCardUrlGetter getSession()
    {
        instanceCount++;
        
        if(instance == null)
        {
            instance = new CableCardUrlGetter();
        }
        
        return instance;
    }

    /**
     * Releases the CableCardUrlGetter session held by calling
     * getSession().
     */
    public void releaseSession()
    {
        instanceCount--;
    }
    
    public void getUrl(String url, UrlGetterListener listener)
    {
        int transaction = pendingServerQueries.getNumber();
        if (transaction < 0)
        {
            // All transaction numbers are in use.
            listener.fileDownloadFailed(UrlGetterListener.REASON_ERROR);
        }
        else
        {
            pendingServerQueries.put(transaction, new ServerQuery(listener));
            
            byte[] headers = new byte[0];
            
            byte[] apdu = new APDUWriter().putInt(transaction, 1)
                    .putInt(headers.length, 2)
                    .putBytes(headers)
                    .putInt(url.length(), 2)
                    .putBytes(url.getBytes())
                    .getData();

            sendAPDU(TAG_SERVER_QUERY, apdu);
        }        
    }

    
    public void ready(SystemModule systemModule)
    {
        this.systemModule = systemModule;
    }

    public void notifyUnregister()
    {
        systemModule = null;
    }

    public void receiveAPDU(int apduTag, int lengthField, byte[] dataByte)
    {
        if (TAG_SERVER_REPLY == apduTag)
        {
            try
            {
                APDUReader reader = new APDUReader(dataByte);
                int transactionNumber = reader.getInt(1);
                int fileStatus = reader.getInt(1);
                int headerLength = reader.getInt(2);
                byte[] headers = reader.getBytes(headerLength);
                int fileLength = reader.getInt(2);
                byte[] file = reader.getBytes(fileLength);
    
                handleServerReply(transactionNumber, fileStatus, headers, file);
            }
            catch(APDUReadException e)
            {
                /* TODO: handle it! */
            }
        }
    }

    public void sendAPDUFailed(int apduTag, byte[] dataByte)
    {
        // If a server query fails, notify the requestor.
        if (apduTag == TAG_SERVER_QUERY)
        {
            APDUReader reader = new APDUReader(dataByte);
            try
            {
                int transactionNumber = reader.getInt(1);

                ServerQuery query = (ServerQuery) pendingServerQueries.remove(transactionNumber);

                if (null != query)
                {
                    query.listener.fileDownloadFailed(UrlGetterListener.REASON_ERROR);
                }
            }
            catch (APDUReader.APDUReadException e)
            {
            }
        }
    }
    

    /**
     * Handle an incoming server_reply APDU.
     * 
     * @param transactionNumber
     *            transaction number for reply
     * 
     * @param fileStatus
     *            reply status
     * 
     * @param headers
     *            HTTP headers received with reply
     * 
     * @param file
     *            data received in reply
     * 
     */
    private void handleServerReply(int transactionNumber, int fileStatus, byte[] headers, byte[] file)
    {
        ServerQuery query = (ServerQuery) pendingServerQueries.remove(transactionNumber);

        if (null != query)
        {
            if(fileStatus == FILE_STATUS_OK)
            {
                query.listener.fileDownloadComplete(new String(file));
            }
            else
            {
                query.listener.fileDownloadFailed(fileStatus);
            }
        }
    }
    
    private void sendAPDU(int apduTag, byte[] APDU)
    {
        if (systemModule != null)
        {
            systemModule.sendAPDU(apduTag, APDU);
        }
    }
    
    /**
     * Simple container class for server queries.
     */
    private static class ServerQuery
    {
        public final UrlGetterListener listener;

        public ServerQuery(UrlGetterListener listener)
        {
            this.listener = listener;
        }
    }
}
