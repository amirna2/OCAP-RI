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
package org.cablelabs.xlet.TuneTest;

import java.awt.Component;
import java.util.Random;
import java.util.Vector;

import javax.tv.service.Service;

import org.cablelabs.lib.utils.OcapTuner;

public class RepeatTune extends Thread
{
    public RepeatTune(Vector serviceList, OcapTuner tuner, int index, boolean randomness, int minwaitTime,
            int maxwaitTime, int interval, String threadName)
    {
        super(threadName);
        _isRunning = true;
        _serviceList = serviceList;
        _tuner = tuner;
        _index = index;

        _randomwait = randomness; /* tune timing */
        _minwaitTime = minwaitTime; /* minimum wait time */
        _maxwaitTime = maxwaitTime;
        _rampInc = interval;

        tuneQueue = new Vector();
    }

    public RepeatTune(Vector serviceList, OcapTuner tuner, int index, boolean randomness, int minwaitTime,
            int maxwaitTime, int interval, Component visual, String threadName)
    {
        super(threadName);
        _isRunning = true;
        _serviceList = serviceList;
        _tuner = tuner;
        _index = index;

        _randomwait = randomness; /* tune timing */
        _minwaitTime = minwaitTime; /* minimum wait time */
        _maxwaitTime = maxwaitTime;
        _rampInc = interval;

        _visual = visual;

        tuneQueue = new Vector();
    }

    public synchronized boolean isRunning()
    {
        return _isRunning;
    }

    public void run()
    {
        long waitTime = _minwaitTime;

        System.out.println("\nTuneTest - Repeat tune thread starting\n");

        while (isRunning())
        {
            Random rand = new Random();

            // tune to a random channel
            _index = rand.nextInt() % _serviceList.size();
            if (_index < 0) _index *= -1;

            if (_rampup)
            {
                if (waitTime > _maxwaitTime)
                {
                    waitTime = _maxwaitTime;
                    _rampInc = -_rampInc;
                }
                else if (waitTime < _minwaitTime)
                {
                    waitTime = _minwaitTime;
                    _rampInc = -_rampInc;
                }
            }
            else
            {
                // waitTime = _minwaitTime;

                // Check for valid min wait time
                if (_minwaitTime > _maxwaitTime) waitTime = _minwaitTime = _maxwaitTime;

                if (_randomwait && _minwaitTime != _maxwaitTime)
                {
                    // Set wait time to be sometime between the specified min
                    // and the max
                    int wait = (rand.nextInt() % (_maxwaitTime - _minwaitTime));
                    if (wait < 0) wait *= -1;
                    waitTime = _minwaitTime + wait;
                }
            }

            System.out.println("*********************");
            System.out.println("Wait time is " + waitTime + " ms");
            System.out.println("*********************");

            _count++;

            Service service = (Service) _serviceList.elementAt(_index);

            tuneQueue.addElement(service);

            // Hide any channel display graphics that may have been displayed
            if (_visual != null) _visual.setVisible(false);

            _tuner.tune(service);

            // bail out if tune thread is stopping

            if (!isRunning())
            {
                break;
            }

            try
            {
                sleep(waitTime);
            }
            catch (InterruptedException e)
            {
            }

            // Calculate average tuning time
            _sum += _tuner.getTuningTime();

            // Don't count more than last 50 changes [[ THIS IS BRAIN DAMAGED ]]
            if (_count > 100)
            {
                _count = 0;
                _sum = 0;
            }
            if (_rampup) waitTime += _rampInc;
        }
        System.out.println("\nTuneTest - Repeat tune thread exiting\n");
    }

    public boolean isRandomTiming()
    {
        return _randomwait;
    }

    public void setRampup(boolean rampup)
    {
        _rampup = rampup;
    }

    public boolean isRampup()
    {
        return _rampup;
    }

    public synchronized void finish()
    {
        if (_isRunning)
        {
            _isRunning = false;
            System.out.println("\nTuneTest - Repeat tune finishing\n");
            if (_count > 0)
            {
                System.out.println("*************\n");
                System.out.println("Repeat TuneTest Average (ms)=" + (_sum / _count));
                System.out.println("*************\n");
            }
            try
            {
                sleep(500);
            }
            catch (InterruptedException e)
            {
            }
        }
    }

    public Vector getTuneQueue()
    {
        return tuneQueue;
    }

    private OcapTuner _tuner;

    private int _minwaitTime = 0;

    private int _maxwaitTime = 15000;

    private int _sum = 0;

    private int _count = 0;

    private int _index = 0;

    private Vector _serviceList;

    private boolean _isRunning = false;

    private boolean _randomwait = false;

    private boolean _rampup = false;

    private int _rampInc = 100;

    private Component _visual = null;

    private Vector tuneQueue;
}
