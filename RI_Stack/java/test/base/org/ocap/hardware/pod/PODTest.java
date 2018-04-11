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

package org.ocap.hardware.pod;

import org.ocap.hardware.pod.HostParamHandler;

import junit.framework.*;

/**
 * Tests POD
 * 
 * @author A. Hoffman
 */
public class PODTest extends TestCase
{
    /**
     * Tests instantiation of a POD()
     */
    public void testConstructor()
    {
        POD pod = POD.getInstance();
        assertNotNull("Constructor failed to get an instance of POD", pod);
    }

    /**
     * Tests POD.isReady()
     */
    public void testPODisReady()
    {
        POD pod = POD.getInstance();
        assertTrue("POD failed to indicate a ready state", pod.isReady());
    }

    /**
     * Tests POD.getManufacturerID()
     */
    public void testPODgetManufacturerID()
    {
        POD pod = POD.getInstance();
        int mid = pod.getManufacturerID();

        assertTrue("POD failed to return non-zero manufacture identifier", 0 != mid);
        assertTrue("POD failed to return the same manufacture identifier twice", pod.getManufacturerID() == mid);
    }

    /**
     * Tests POD.getManufacturerID()
     */
    public void testPODgetVersionNumber()
    {
        POD pod = POD.getInstance();
        int version = pod.getVersionNumber();

        assertTrue("POD failed to return the same version number twice", pod.getVersionNumber() == version);
    }

    /**
     * Tests POD.getApplications()
     */
    public void testPODgetApplications()
    {
        POD pod = POD.getInstance();
        PODApplication[] apps = pod.getApplications();

        assertNotNull("getPODApplications returned a null object", apps);

        // Check for no applications on the POD.
        if (apps.length != 0)
        {
            for (int i = 0; i < apps.length; ++i)
            {
                assertTrue("POD array contains a null PODApplication object", apps[i] != null);
            }
        }
    }

    /**
     * Tests POD.getHostFeatures()
     */

    public void testPODgetHostFeatures()
    {
        POD pod = POD.getInstance();
        int[] hostFeatures = pod.getHostFeatureList();

        assertNotNull("getHostFeatures returned a null object", hostFeatures);
        assertTrue("POD returned and empty set of strings representing the Host Features", 0 != hostFeatures.length);
    }

    /**
     * Tests POD.getHostParam() and POD.updateHostParam() for all of the
     * supported features.
     */
    public void testPODHostParam()
    {
        POD pod = POD.getInstance();
        int[] hostFeatures = pod.getHostFeatureList();

        assertNotNull("getHostFeatures returned a null object", hostFeatures);
        assertTrue("POD returned and empty set of feature IDs", 0 != hostFeatures.length);

        // Now iterate through the supported host features and verify that the
        // parameter
        // values can be acquired and updated successfully...
        for (int i = 0; i < hostFeatures.length; ++i)
        {
            doGetAndUpdateCheck(hostFeatures[i]);
        }
    }

    /**
     * Tests POD.setHostParamHandler() and HostParamHandler.notifyUpdate().
     */
    public void testPODHostParamHandler()
    {
        POD pod = POD.getInstance();
        int[] hostFeatures = pod.getHostFeatureList();
        boolean result;

        if (hostFeatures == null || hostFeatures.length == 0) return;

        // Setup parameter handler that is called as a response to the "faked"
        // native pod updates. This handler will run on the system context.
        class TestHostParamHandler implements HostParamHandler
        {
            public boolean notifyUpdate(int featureID, byte[] value)
            {
                return doUpdateCheck(featureID, value);
            }
        }

        // Get the test update handler.
        TestHostParamHandler testHandler = new TestHostParamHandler();

        // Verify the installation of a handler works.
        pod.setHostParamHandler(testHandler);

        for (int i = 0; i < hostFeatures.length; ++i)
        {
            try
            {
                // Now begin actual testing of the invocation of the handler's
                // notifyUpdate method.
                switch (hostFeatures[i])
                {
                    case GF_RF_OUTPUT_CHANNEL:
                        // RF Output channel update test.
                        result = pod.updateHostParam(1, new byte[] { 4, 2 });
                        Thread.sleep(1000);
                        assertTrue("Update handler update of rf output channel failed", result);
                        break;

                    case GF_P_C_PIN:
                        // Parental control PIN update test.
                        result = pod.updateHostParam(2, new byte[] { 4, 1, 2, 3, 4 });
                        Thread.sleep(1000);
                        assertTrue("Update handler update of parental control pin failed", result);
                        break;

                    case GF_P_C_SETTINGS:
                        // Parental control settings update test.
                        result = pod.updateHostParam(3, new byte[] { 0, 0, 4, 0, 105, 5, 0, 106, 6, 0, 107, 7, 0, 108,
                                8 });
                        Thread.sleep(1000);
                        assertTrue("Update handler update of parental control settings failed", result);
                        break;

                    case GF_IPPV_PIN:
                        // IPPV PIN update test.
                        result = pod.updateHostParam(4,
                                new byte[] { 4, (byte) 201, (byte) 202, (byte) 203, (byte) 204 });
                        Thread.sleep(1000);
                        assertTrue("Update handler update of IPPV PIN failed", result);
                        break;

                    case GF_TIME_ZONE:
                        // Time zone update test.
                        result = pod.updateHostParam(5, new byte[] { 300 >> 8, 300 & 0xFF });
                        Thread.sleep(1000);
                        assertTrue("Update handler update of time zone failed", result);
                        break;

                    case GF_DAYLIGHT_SAVINGS:
                        // Daylight savings update test.
                        result = pod.updateHostParam(6, new byte[] { 2 });
                        Thread.sleep(1000);
                        assertTrue("Update handler update of daylight savings failed", result);
                        break;

                    case GF_AC_OUTLET:
                        // AC Outlet update test.
                        result = pod.updateHostParam(7, new byte[] { 2 });
                        Thread.sleep(1000);
                        assertTrue("Update handler update of AC outlet setting failed", result);
                        break;

                    case GF_LANGUAGE:
                        // Language update test.
                        result = pod.updateHostParam(8, new byte[] { 's', 'w', 'a' });
                        Thread.sleep(1000);
                        assertTrue("Update handler update of language setting failed", result);
                        break;

                    case GF_RATING_REGION:
                        // Rating region update test.
                        result = pod.updateHostParam(9, new byte[] { 2 });
                        Thread.sleep(1000);
                        assertTrue("Update handler update of ratings region failed", result);
                        break;

                    default:
                        break;
                }
            }
            catch (java.lang.InterruptedException ie)
            {
                fail("Thread sleep waiting for HostParamHandler update to finish failed");
            }
        }
    }

    /**
     * This function provides the support for testing acquisition of the Host
     * Feature Parameters using POD.getHostParam() and the setting of the
     * parameter values using POD.updateHostParam(). All values are checked for
     * sanity and restored to their original values after the updates.
     * 
     * @param feature
     *            is the string identifier of the feature to test.
     */
    private void doGetAndUpdateCheck(int featureID)
    {
        POD pod = POD.getInstance();

        // Determine feature and process according type...
        switch (featureID)
        {
            case GF_RF_OUTPUT_CHANNEL:
            {
                byte[] temp;

                // Get the current value.
                byte[] chnl = pod.getHostParam(featureID);

                if (chnl == null || (chnl[0] != (3) && chnl[0] != 4)) return;

                // Sanity check the value.
                assertTrue("RF output channel is not a reasonable value: " + chnl[0], chnl[0] == 3 || chnl[0] == 4);

                // Set it to a new channel value and UI disable flag.
                byte[] newChnl = { chnl[0] == 3 ? (byte) 4 : (byte) 3, 2 };
                assertTrue("RF output channel update failed.", pod.updateHostParam(featureID, newChnl));

                // Make sure it was reflected in subsystem.
                temp = pod.getHostParam(featureID);
                assertNotNull("Failed to acquire RF output channel", temp);
                assertEquals("RF output channel did not change", temp[0], newChnl[0]);

                // Restore original value.
                assertTrue("RF output channel restoration failed.", pod.updateHostParam(featureID, chnl));
                temp = pod.getHostParam(featureID);
                assertNotNull("Failed to acquire RF output channel", temp);
                assertEquals("RF output channel did not get restored", temp[0], chnl[0]);
            }
                break;

            case GF_P_C_PIN:
            {
                // Acquire the current PIN.
                byte[] pin = pod.getHostParam(featureID);

                // Make sure the pin was returned and makes sense.
                assertNotNull("PIN value not returned successfully", pin);
                assertTrue("PIN value not returned successfully", pin.length > 0 && pin.length < 256);

                // Set it to a new value.
                byte[] newPin = { 4, 8, 9, 8, 6 };
                assertTrue("PIN value could not be changed", pod.updateHostParam(featureID, newPin));

                // Check to make sure it was set.
                byte[] verifyPin = pod.getHostParam(featureID);
                assertNotNull("New PIN value not returned successfully", verifyPin);
                for (int i = 0; i < newPin.length; ++i)
                    assertEquals("New PIN value is not correct", newPin[i], verifyPin[i]);

                // Restore original PIN value.
                assertTrue("PIN value could not be restored", pod.updateHostParam(featureID, pin));
                verifyPin = pod.getHostParam(featureID);
                assertNotNull("PIN value not restored successfully", verifyPin);
                for (int i = 0; i < pin.length; ++i)
                    assertEquals("Restored PIN value is not correct", pin[i], verifyPin[i]);
            }
                break;

            case GF_P_C_SETTINGS:
            {
                // Acquire the current parental control channel settings.
                byte[] settings = pod.getHostParam(featureID);

                // Make sure the settings was returned and makes sense.
                assertNotNull("Parental control settings value not returned successfully", settings);

                if (settings.length > 0)
                {
                    // Set it to a new value.
                    byte[] newSettings = { 0, 0, 4, 0, 105, 5, 0, 106, 6, 0, 107, 7, 0, 108, 8 };
                    assertTrue("Parental control settings value could not be changed", pod.updateHostParam(featureID,
                            newSettings));

                    // Check to make sure it was set.
                    byte[] verifySettings = pod.getHostParam(featureID);
                    assertNotNull("New parental control settings value not returned successfully", verifySettings);
                    for (int i = 0; i < newSettings.length; ++i)
                        assertEquals("New parental control settings value is not correct", newSettings[i],
                                verifySettings[i]);

                    // Restore original SETTINGS value.
                    assertTrue("Parental control settings value could not be restored", pod.updateHostParam(featureID,
                            settings));
                    verifySettings = pod.getHostParam(featureID);
                    assertNotNull("Parental control settings value not restored successfully", verifySettings);
                    for (int i = 0; i < settings.length; ++i)
                        assertEquals("Restored arental control settings value is not correct", settings[i],
                                verifySettings[i]);
                }
            }
                break;

            case GF_IPPV_PIN:
            {
                // Acquire the current PIN.
                byte[] pin = pod.getHostParam(featureID);

                // Make sure the pin was returned and makes sense.
                assertNotNull("IPPV PIN value not returned successfully", pin);
                assertTrue("IPPV PIN value not returned successfully", pin.length > 0 && pin.length < 256);

                // Set it to a new value.
                byte[] newPin = { 4, 8, 9, 8, 6 };
                assertTrue("IPPV PIN value could not be changed", pod.updateHostParam(featureID, newPin));

                // Check to make sure it was set.
                byte[] verifyPin = pod.getHostParam(featureID);
                assertNotNull("New IPPV PIN value not returned successfully", verifyPin);
                for (int i = 0; i < newPin.length; ++i)
                    assertEquals("New IPPV PIN value is not correct", newPin[i], verifyPin[i]);

                // Restore original PIN value.
                assertTrue("IPPV PIN value could not be restored", pod.updateHostParam(featureID, pin));
                verifyPin = pod.getHostParam(featureID);
                assertNotNull("IPPV PIN value not restored successfully", verifyPin);
                for (int i = 0; i < pin.length; ++i)
                    assertEquals("Restored IPPV PIN value is not correct", pin[i], verifyPin[i]);
            }
                break;

            case GF_TIME_ZONE:
            {
                byte[] temp;
                int tzValue;
                byte[] newTz = { 0, 0 };

                // Get the current value.
                byte[] tz = pod.getHostParam(featureID);

                // Sanity check the value.
                assertNotNull("Failed to acquire time zone", tz);
                tzValue = tz[0] << 8 | tz[1];
                assertTrue("Time zone is not a reasonable value", tzValue >= 0 && tzValue <= (23 * 60));

                // Set it to a new value.
                tzValue = (tzValue == 360 ? 400 : 360);
                newTz[0] = (byte) (tzValue >> 8);
                newTz[1] = (byte) (tzValue & 0xFF);
                assertTrue("Time zone update failed.", pod.updateHostParam(featureID, newTz));

                // Make sure it was reflected in subsystem.
                temp = pod.getHostParam(featureID);
                assertNotNull("Failed to acquire time zone", temp);
                assertTrue("Time zone did not change", temp[0] == newTz[0] && temp[1] == newTz[1]);

                // Restore original value.
                assertTrue("Time zone restoration failed.", pod.updateHostParam(featureID, tz));
                temp = pod.getHostParam(featureID);
                assertNotNull("Failed to acquire time zone", temp);
                assertTrue("Time zone did not get restored", temp[0] == tz[0] && temp[1] == tz[1]);
            }
                break;

            case GF_DAYLIGHT_SAVINGS:
            {
                byte[] temp;

                // Get the current value.
                byte[] ds = pod.getHostParam(featureID);

                // Sanity check the value.
                assertNotNull("Failed to acquire daylight savings value", ds);

                // Set it to a new value.
                byte[] newDs = { ds[0] == 2 ? (byte) 1 : (byte) 2 };
                assertTrue("Daylight savings update failed.", pod.updateHostParam(featureID, newDs));

                // Make sure it was reflected in subsystem.
                temp = pod.getHostParam(featureID);
                assertNotNull("Failed to acquire daylight savings value", temp);
                assertEquals("Daylight savings did not change", temp[0], newDs[0]);

                // Restore original value.
                assertTrue("Daylight savings restoration failed.", pod.updateHostParam(featureID, ds));
                temp = pod.getHostParam(featureID);
                assertNotNull("Failed to acquire daylight savings value", temp);
                assertEquals("Daylight savings did not get restored", temp[0], ds[0]);
            }
                break;

            case GF_AC_OUTLET:
            {
                byte[] temp;

                // Get the current value.
                byte[] ac = pod.getHostParam(featureID);

                // Sanity check the value.
                assertNotNull("Failed to acquire AC outlet setting value", ac);

                // Set it to a new value.
                byte[] newAc = { ac[0] == 0 ? (byte) 1 : (byte) 0 };
                assertTrue("AC outlet update failed.", pod.updateHostParam(featureID, newAc));

                // Make sure it was reflected in subsystem.
                temp = pod.getHostParam(featureID);
                assertNotNull("Failed to acquire AC outlet setting value", temp);
                assertEquals("AC outlet setting did not change", temp[0], newAc[0]);

                // Restore original value.
                assertTrue("AC outlet setting restoration failed.", pod.updateHostParam(featureID, ac));
                temp = pod.getHostParam(featureID);
                assertNotNull("Failed to acquire AC outlet setting value", temp);
                assertEquals("AC outlet setting did not get restored", temp[0], ac[0]);
            }
                break;

            case GF_LANGUAGE:
            {
                byte[] temp;

                // Get the current value.
                byte[] lang = pod.getHostParam(featureID);

                // Sanity check the value.
                assertNotNull("Failed to acquire language setting value", lang);

                // Set it to a new value.
                byte[] newLang = { 's', 'w', 'a' };
                assertTrue("Language setting update failed.", pod.updateHostParam(featureID, newLang));

                // Make sure it was reflected in subsystem.
                temp = pod.getHostParam(featureID);
                assertNotNull("Failed to language setting value", temp);
                assertEquals("Failed to language setting value", temp.length, newLang.length);
                for (int i = 0; i < temp.length; ++i)
                    assertEquals("Language setting did not change", temp[i], newLang[i]);

                // Restore original value.
                assertTrue("Language setting restoration failed.", pod.updateHostParam(featureID, lang));
                temp = pod.getHostParam(featureID);
                assertNotNull("Failed to acquire language setting value", temp);
                assertEquals("Failed to language setting value", temp.length, lang.length);
                for (int i = 0; i < temp.length; ++i)
                    assertEquals("Language setting did not change", temp[i], lang[i]);
            }
                break;

            case GF_RATING_REGION:
            {
                byte[] temp;

                // Get the current value.
                byte[] region = pod.getHostParam(featureID);

                // Sanity check the value.
                assertNotNull("Failed to acquire ratings region", region);
                assertTrue("Ratings region is not a reasonable value", region[0] == 1 || region[0] == 2);

                // Set it to a new value.
                byte[] newRegion = { region[0] == 1 ? (byte) 2 : (byte) 1 };
                assertTrue("Ratings region update failed.", pod.updateHostParam(featureID, newRegion));

                // Make sure it was reflected in subsystem.
                temp = pod.getHostParam(featureID);
                assertNotNull("Failed to acquire ratings region", temp);
                assertEquals("Ratings region did not change", temp[0], newRegion[0]);

                // Restore original value.
                assertTrue("Ratings region restoration failed.", pod.updateHostParam(featureID, region));
                temp = pod.getHostParam(featureID);
                assertNotNull("Failed to acquire ratings region", temp);
                assertEquals("Ratings region did not get restored", temp[0], region[0]);
            }
                break;

            case GF_RESET_P_C_PIN:
            {
                byte[] reset = pod.getHostParam(featureID);

                // Sanity check the value.
                assertNotNull("Reset PC PIN failed to return a zero-length byte array", reset);
                assertEquals("Attempt to acquire reset parental control pin should have returned an empty array",
                        reset.length, 0);
            }
                break;

            case GF_CABLE_URLS:
            {
                byte[] urls = pod.getHostParam(featureID);

                // Sanity check the value.
                assertNotNull("Failed to acquire cable URLs", urls);
                assertTrue("Failed to acquire cable URLs", urls.length > 0);
            }
                break;

            case GF_EA_LOCATION:
            {
                byte[] eaLocation = pod.getHostParam(featureID);

                // Sanity check the value.
                assertNotNull("Failed to acquire EA location", eaLocation);
                assertTrue("Failed to acquire EA location", eaLocation.length > 0);
            }
        }
    }

    /**
     * This function provides the support for testing the asynchronous
     * invocation of the installed Host Parameter Update Handler.
     * 
     * @param featureID
     *            is the identifier of the feature being updated.
     * @param value
     *            is a byte array containing the associated update value.
     */
    private boolean doUpdateCheck(int featureID, byte[] value)
    {
        // Determine feature and process according type...
        switch (featureID)
        {
            case GF_RF_OUTPUT_CHANNEL:
            {
                // Verify byte array.
                if (value.length != 2) return false;

                // Test is ok if value is 4.
                return (value[0] == 4);
            }

            case GF_P_C_PIN:
            {
                // Verify byte array
                if ((value.length != 5) || (value[0] != 4)) return false;

                for (int i = 1; i < 5; ++i)
                {
                    if (i != value[i]) // match 1-4
                        return false;
                }
                return true; // All values matched what was expected.
            }

            case GF_P_C_SETTINGS:
            {
                // Verify byte array.
                if ((value.length != 15) || (value[2] != 4)) return false;

                for (int i = 3; i < value.length; ++i)
                {
                    if ((i % 3) == 0)
                    {
                        if (value[i] != 0) return false;
                    }
                    else if (!((value[i] >= 5 && value[i] <= 8) || (value[i] >= 105 && value[i] <= 108))) return false;
                }
                return true; // All values matched what was expected.
            }

            case GF_IPPV_PIN:
            {
                // Verify byte array.
                if (value.length != 5 || value[0] != 4) return false;

                for (int i = 1; i < value.length; ++i)
                    if (value[i] != (byte) (i + 200)) return false;
                return true;
            }

            case GF_TIME_ZONE:
            {
                // Verify byte array
                if (value.length != 2) return false;

                return ((value[0] == (byte) (300 >> 8)) && (value[1] == (byte) (300 & 0xFF)));
            }

            case GF_DAYLIGHT_SAVINGS:
            {
                // Verify byte array.
                if (value.length != 1) return false;
                return (value[0] == 2);
            }

            case GF_AC_OUTLET:
            {
                // Verify byte array.
                if (value.length != 1) return false;
                return (value[0] == 2);
            }

            case GF_LANGUAGE:
            {
                // Verify byte array.
                if (value.length != 3) return false;
                return (value[0] == 's' && value[1] == 'w' && value[2] == 'a');
            }

            case GF_RATING_REGION:
            {
                // Verify byte array.
                if (value.length != 1) return false;

                // Rating region value shoulbe be 2. */
                return (value[0] == 2);
            }
            default:
                return false;
        }
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
        TestSuite suite = new TestSuite(PODTest.class);
        return suite;
    }

    // Generic feature IDs from SCTE28 Table 8.12-B.
    private static final int GF_RF_OUTPUT_CHANNEL = 1;

    private static final int GF_P_C_PIN = 2;

    private static final int GF_P_C_SETTINGS = 3;

    private static final int GF_IPPV_PIN = 4;

    private static final int GF_TIME_ZONE = 5;

    private static final int GF_DAYLIGHT_SAVINGS = 6;

    private static final int GF_AC_OUTLET = 7;

    private static final int GF_LANGUAGE = 8;

    private static final int GF_RATING_REGION = 9;

    private static final int GF_RESET_P_C_PIN = 10;

    private static final int GF_CABLE_URLS = 11;

    private static final int GF_EA_LOCATION = 12;
}
