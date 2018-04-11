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

package org.cablelabs.lib.utils;

//Import OCAP packages.
import java.util.Date;

import javax.tv.service.SIManager;
import javax.tv.service.Service;
import javax.tv.service.selection.NormalContentEvent;
import javax.tv.service.selection.SelectionFailedEvent;
import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceContextEvent;
import javax.tv.service.selection.ServiceContentHandler;
import javax.tv.service.selection.ServiceMediaHandler;

import org.ocap.net.OcapLocator;

import org.cablelabs.test.autoxlet.Logger;
import org.cablelabs.test.autoxlet.XletLogger;

public class OcapTuner
{
    public OcapTuner(ServiceContext sc)
    {
        _serviceContext = sc;
        _log = new XletLogger();

        System.out.println("OcapTuner");
    }

    public OcapTuner(ServiceContext sc, Logger log)
    {
        if (log == null)
            _log = new XletLogger();
        else
            _log = log;

        _log.log(prefix() + "created");
        _serviceContext = sc;
    }

    public void tune(int frequency, int programNumber, int qam) throws org.davic.net.InvalidLocatorException,
            javax.tv.locator.InvalidLocatorException
    {
        _log.log(prefix() + "tune -- frequency: " + frequency + " programNumber: " + programNumber + " qam: " + qam);

        OcapLocator ocaploc = new OcapLocator(frequency, programNumber, qam);
        tune(getService(ocaploc));
    }

    public void tune(int sourceID) throws org.davic.net.InvalidLocatorException,
            javax.tv.locator.InvalidLocatorException
    {
        _log.log(prefix() + "tune -- SourceID: " + sourceID);

        OcapLocator ocaploc = new OcapLocator(sourceID);
        tune(getService(ocaploc));
    }

    public void tune(OcapLocator ocaploc) throws javax.tv.locator.InvalidLocatorException
    {
        _log.log(prefix() + "tune -- OcapLocator: " + ocaploc.toString());

        tune(getService(ocaploc));
    }

    public void tune(Service service)
    {
        _state = IDLE;
        if (service == null) return;

        _log.log(prefix() + "tune -- Service: " + ((OcapLocator) (service.getLocator())).toString());

        _state = TUNING;
        _tuningStart = System.currentTimeMillis();
        _serviceContext.select(service);
    }

    private static long startTime = new Date().getTime();

    private static long getElapsed()
    {
        return new Date().getTime() - startTime;
    }

    public static String format(long number, int digits)
    {
        // If number is too large to fit in the specified digits, just return
        // the entire number.
        long max = 10 ^ digits;
        if (number >= max) return Long.toString(number);

        // Build the string, digit by digit, starting from lowest to highest.
        StringBuffer sb = new StringBuffer(digits); // preallocate with required
                                                    // number of digits
        for (int i = 0; i < digits; ++i)
        {
            sb.insert(0, number % 10); // append 10's digit
            number /= 10; // divide by 10 to shift number right
        }
        return sb.toString();
    }

    public static String timestamp()
    {
        long now = getElapsed();
        long ms = now % 1000;
        long s = (now / 1000) % 60;
        long m = (now / (60 * 1000)) % 60;
        long h = now / (60 * 60 * 1000);

        return format(h, 2) + ":" + format(m, 2) + ":" + format(s, 2) + "." + format(ms, 3);
    }

    private static String prefix()
    {
        return "OcapTuner[" + timestamp() + "]: ";
    }

    public boolean tuneEventHandler(ServiceContextEvent event)
    {
        if (event == null) return false;

        String pfx = prefix();

        if (event instanceof NormalContentEvent)
        {
            _tuningStop = System.currentTimeMillis();

            _log.log("\n\n********************************************");
            _log.log(pfx + "Received ServiceContextEvent - " + "NormalContentEvent received from Service Context");
            _log.log("Successful Tune -- Time = " + getTuningTime() + "ms");
            _log.log("********************************************\n");

            _state = TUNED;
            return true;
        }

        if (event instanceof SelectionFailedEvent)
        {
            _log.log("\n\n********************************************");
            _log.log(pfx + "Received ServiceContextEvent - " + "SelectionFailedEvent (reason: "
                    + getReason(((SelectionFailedEvent) event).getReason()) + ") received from Service Context\n");
            _log.log("********************************************\n");

            _state = FAILED;
            return false;
        }

        _log.log("\n\n********************************************");
        _log.log(pfx + "Received ServiceContextEvent - " + "Unmatched event received from Service Context: " + event
                + "\n");
        _log.log("********************************************\n");

        return false;
    }

    public long getTuningTime()
    {
        return _tuningStop - _tuningStart;
    }

    public int getTuningState()
    {
        return _state;
    }

    public void stop()
    {
        ServiceContentHandler[] handlers = _serviceContext.getServiceContentHandlers();
        if (handlers.length > 0 && handlers[0] instanceof ServiceMediaHandler)
        {
            ((ServiceMediaHandler) handlers[0]).stop();
            _log.log("\n\n********************************************");
            _log.log(prefix() + " stop\n");
            _log.log("********************************************\n");
        }
    }

    public void play()
    {
        ServiceContentHandler[] handlers = _serviceContext.getServiceContentHandlers();
        if (handlers.length > 0 && handlers[0] instanceof ServiceMediaHandler)
        {
            ((ServiceMediaHandler) handlers[0]).start();
            _log.log("\n\n********************************************");
            _log.log(prefix() + " play\n");
            _log.log("********************************************\n");
        }
    }

    public ServiceContext getServiceContext()
    {
        return _serviceContext;
    }

    private Service getService(OcapLocator ocaploc) throws javax.tv.locator.InvalidLocatorException
    {
        SIManager siManager = (SIManager) SIManager.createInstance();

        Service service = siManager.getService(ocaploc);
        return service;
    }

    private String getReason(int reason)
    {
        switch (reason)
        {
            case SelectionFailedEvent.CA_REFUSAL:
                return "CA_REFUSAL";
            case SelectionFailedEvent.CONTENT_NOT_FOUND:
                return "CONTENT_NOT_FOUND";
            case SelectionFailedEvent.INSUFFICIENT_RESOURCES:
                return "INSUFFICIENT_RESOURCES";
            case SelectionFailedEvent.INTERRUPTED:
                return "INTERRUPTED";
            case SelectionFailedEvent.MISSING_HANDLER:
                return "MISSING_HANDLER";
            case SelectionFailedEvent.TUNING_FAILURE:
                return "TUNING_FAILURE";
            default:
                return "UNKNOWN";
        }
    }

    protected String getStateStr()
    {
        switch (_state)
        {
            case IDLE:
                return "IDLE";
            case TUNING:
                return "TUNING";
            case TUNED:
                return "TUNED";
            case FAILED:
                return "FAILED";
            default:
                return "UNKNOWN";
        }
    }

    public static final int IDLE = 0;
    public static final int TUNING = 1;
    public static final int TUNED = 2;
    public static final int FAILED = 3;

    private ServiceContext _serviceContext = null;

    private Logger _log = null;

    private long _tuningStart, _tuningStop = 0;

    private int _state = IDLE;
}
