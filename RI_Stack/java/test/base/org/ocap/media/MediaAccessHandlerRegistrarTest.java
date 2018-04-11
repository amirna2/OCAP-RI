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

package org.ocap.media;

import junit.framework.TestCase;

import org.ocap.media.CannedMediaAccessHandler;
import org.ocap.media.MediaAccessHandlerRegistrar;

import org.cablelabs.test.ProxySecurityManager;
import org.cablelabs.test.ProxySecurityManager.DummySecurityManager;

public class MediaAccessHandlerRegistrarTest extends TestCase
{
    private MediaAccessHandlerRegistrar registrar;

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(MediaAccessHandlerRegistrarTest.class);
        System.exit(0);
    }

    protected void setUp() throws Exception
    {
        super.setUp();
        registrar = MediaAccessHandlerRegistrar.getInstance();
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    public void testGetInstanceNotNull()
    {
        assertTrue(registrar != null);
    }

    private void pushDummySecurityManager()
    {
        DummySecurityManager sm = new DummySecurityManager("mediaAccess");
        ProxySecurityManager.install();
        ProxySecurityManager.push(sm);
    }

    private void popDummySecurityManager()
    {
        ProxySecurityManager.pop();
    }

    /*
     * SecurityException (Permissions) Tests
     */

    public void testRegisterMediaAccessHandler_SecurityException()
    {
        pushDummySecurityManager();
        try
        {
            registrar.registerMediaAccessHandler(new CannedMediaAccessHandler());
            fail("Expected SecurityException");
        }
        catch (SecurityException x)
        {
            // expected outcome
        }
        finally
        {
            popDummySecurityManager();
        }
    }
    /*
     * public void testRemoveServiceBlocking_SecurityException() {
     * pushDummySecurityManager(); try { registrar.removeServiceBlocking(new
     * BlockedService[0]); fail("Expected SecurityException"); }
     * catch(SecurityException x) { //expected outcome } finally {
     * popDummySecurityManager(); } }
     * 
     * public void testSetNotifyCondition_SecurityException() {
     * pushDummySecurityManager(); try { registrar.setNotifyCondition(false);
     * fail("Expected SecurityException"); } catch(SecurityException x) {
     * //expected outcome } finally { popDummySecurityManager(); } }
     * 
     * public void testSetNotRatedSignaledBlocking_SecurityException() {
     * pushDummySecurityManager(); try {
     * registrar.setNotRatedSignaledBlocking(ParentalControlRatings.TV_NONE,
     * false); fail("Expected SecurityException"); } catch(SecurityException x)
     * { //expected outcome } finally { popDummySecurityManager(); } }
     * 
     * public void testSetServiceBlocking_SecurityException() {
     * pushDummySecurityManager(); try { registrar.setServiceBlocking(new
     * BlockedService[0]); fail("Expected SecurityException"); }
     * catch(SecurityException x) { //expected outcome } finally {
     * popDummySecurityManager(); } }
     * 
     * public void testSetServiceBlockingOverride_SecurityException() {
     * pushDummySecurityManager(); try { registrar.setServiceBlockingOverride(0,
     * 0, false); fail("Expected SecurityException"); } catch(SecurityException
     * x) { //expected outcome } finally { popDummySecurityManager(); } }
     * 
     * public void testSetSignaledBlocking_SecurityException() {
     * pushDummySecurityManager(); try {
     * registrar.setSignaledBlocking(ParentalControlRatings.MPAA_G);
     * fail("Expected SecurityException"); } catch(SecurityException x) {
     * //expected outcome } finally { popDummySecurityManager(); } }
     * 
     * public void testSetSignaledBlockingOverride_SecurityException() {
     * pushDummySecurityManager(); try {
     * registrar.setSignaledBlockingOverride(false);
     * fail("Expected SecurityException"); } catch(SecurityException x) {
     * //expected outcome } finally { popDummySecurityManager(); } }
     */

    /*
     * IllegalArgumentException Tests
     */
    /*
     * public void testSetSignaledBlocking_IllegalArgumentException() { int[]
     * bogusRatings = { -1, 0x40, 0xFF, 0x10000,
     * ParentalControlRatings.TV_MA_L_S_V + 1, ParentalControlRatings.MPAA_G -
     * 1, ParentalControlRatings.MPAA_X - 1,
     * ParentalControlRatings.MPAA_NOT_RATED + 0x100,
     * ParentalControlRatings.MPAA_G + ParentalControlRatings.TV_MA_L_S_V + 1 };
     * for (int i = 0; i < bogusRatings.length; ++i) { int rating =
     * bogusRatings[i]; try { registrar.setSignaledBlocking(rating);
     * fail("Excpected IllegalArgumentException for rating #"+i+": "+rating); }
     * catch(IllegalArgumentException x) { // expected outcome } } }
     * 
     * public void testSetNotRatedSignaledBlocking_IllegalArgumentException() {
     * try { registrar.setNotRatedSignaledBlocking(ParentalControlRatings.TV_14,
     * false); fail("Expected IllegalArgumentException"); }
     * catch(IllegalArgumentException x) { // expected outcome }
     * 
     * try {
     * registrar.setNotRatedSignaledBlocking(ParentalControlRatings.MPAA_G,
     * false); fail("Expected IllegalArgumentException"); }
     * catch(IllegalArgumentException x) { // expected outcome } }
     * 
     * public void testSetServiceBlocking_IllegalArgumentException() { try {
     * registrar.setServiceBlocking(null);
     * fail("Expected IllegalArgumentException"); }
     * catch(IllegalArgumentException x) { // expected outcome } }
     * 
     * public void testRemoveServiceBlocking_IllegalArgumentException() { try {
     * registrar.removeServiceBlocking(null);
     * fail("Expected IllegalArgumentException"); }
     * catch(IllegalArgumentException x) { // expected outcome } }
     * 
     * public void testGetSignaledBlockingDefaultValue() {
     * ProxySecurityManager.install(); ProxySecurityManager.push(new
     * ProxySecurityManager.NullSecurityManager()); try { // check the initial
     * value assertEquals(registrar.getSignaledBlocking(),
     * (ParentalControlRatings.TV_NONE | ParentalControlRatings.MPAA_NA)); }
     * finally { ProxySecurityManager.pop(); } }
     * 
     * public void testSetGetSignaledBlocking() {
     * ProxySecurityManager.install(); ProxySecurityManager.push(new
     * ProxySecurityManager.NullSecurityManager()); try {
     * registrar.setSignaledBlocking(ParentalControlRatings.TV_G |
     * ParentalControlRatings.MPAA_NC_17);
     * assertEquals(registrar.getSignaledBlocking(),
     * (ParentalControlRatings.TV_G | ParentalControlRatings.MPAA_NC_17));
     * 
     * registrar.setSignaledBlocking(ParentalControlRatings.TV_PG);
     * assertEquals(registrar.getSignaledBlocking(),
     * (ParentalControlRatings.TV_PG | ParentalControlRatings.MPAA_NC_17));
     * 
     * registrar.setSignaledBlocking(ParentalControlRatings.MPAA_R);
     * assertEquals(registrar.getSignaledBlocking(),
     * (ParentalControlRatings.TV_PG | ParentalControlRatings.MPAA_R)); }
     * finally { ProxySecurityManager.pop(); } }
     * 
     * public void testSetGetSignaledBlockingOverride() {
     * ProxySecurityManager.install(); ProxySecurityManager.push(new
     * ProxySecurityManager.NullSecurityManager()); try { boolean testValue =
     * false; registrar.setSignaledBlockingOverride(testValue);
     * assertTrue(testValue == registrar.getSignaledBlockingOverride());
     * 
     * testValue = true; registrar.setSignaledBlockingOverride(testValue);
     * assertTrue(testValue == registrar.getSignaledBlockingOverride()); }
     * finally { ProxySecurityManager.pop(); } }
     * 
     * public void testSetGetNotifyCondition() { ProxySecurityManager.install();
     * ProxySecurityManager.push(new
     * ProxySecurityManager.NullSecurityManager()); try { boolean testValue =
     * false; registrar.setNotifyCondition(testValue); assertTrue(testValue ==
     * registrar.getNotifyCondition());
     * 
     * testValue = true; registrar.setNotifyCondition(testValue);
     * assertTrue(testValue == registrar.getNotifyCondition()); } finally {
     * ProxySecurityManager.pop(); } }
     * 
     * public void testSetGetServiceBlockingOverride() {
     * ProxySecurityManager.install(); ProxySecurityManager.push(new
     * ProxySecurityManager.NullSecurityManager()); try { // // block 2 - 5 //
     * registrar.setServiceBlockingOverride(2, 5, true);
     * assertTrue(registrar.getServiceBlockingOverride(2));
     * assertTrue(registrar.getServiceBlockingOverride(5));
     * assertTrue(!registrar.getServiceBlockingOverride(1));
     * assertTrue(!registrar.getServiceBlockingOverride(6));
     * 
     * // // unblock 3 - 4 // // Currently, it is expected that passing in false
     * for // setServiceBlockingOverride will unblock all ratings //
     * registrar.setServiceBlockingOverride(3, 4, false);
     * assertTrue(!registrar.getServiceBlockingOverride(2));
     * assertTrue(!registrar.getServiceBlockingOverride(5));
     * assertTrue(!registrar.getServiceBlockingOverride(3));
     * assertTrue(!registrar.getServiceBlockingOverride(4));
     * 
     * } finally { ProxySecurityManager.pop(); } }
     */
}
