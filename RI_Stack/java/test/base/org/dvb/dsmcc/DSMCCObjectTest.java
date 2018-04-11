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

import java.io.File;
import java.util.Vector;

import junit.framework.*;

import org.cablelabs.impl.manager.Manager;
import org.cablelabs.impl.util.MPEEnv;

/**
 * Tests the org.dvb.dsmcc.DSMCCObject class.
 */
public class DSMCCObjectTest extends TestCase
{
    private Manager fsysMgr = null;

    private Manager edMgr = null;

    private static final String dir1 = "evm/lib/security";

    private static final String file1 = "java.policy";

    private static final String path1 = "evm/lib/security/java.policy";

    private String TESTDIR;

    public DSMCCObjectTest(String name)
    {
        super(name);
    }

    public static void main(String[] args)
    {
        try
        {
            junit.textui.TestRunner.run(suite(args));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        System.exit(0);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(DSMCCObjectTest.class);
        return suite;
    }

    public static Test suite(String tests[])
    {
        if (tests == null || tests.length == 0)
            return suite();
        else
        {
            TestSuite suite = new TestSuite(DSMCCObjectTest.class.getName());
            for (int i = 0; i < tests.length; ++i)
                suite.addTest(new DSMCCObjectTest(tests[i]));
            return suite;
        }
    }

    protected void setUp() throws Exception
    {
        super.setUp();

        String ocap = MPEEnv.getEnv("OCAP.persistent.root");
        assertNotNull("Error! Need a place to write files", ocap);
        TESTDIR = ocap + "/junit";
        File testDir = new File(TESTDIR);
        if (!testDir.exists()) testDir.mkdirs();
    }

    protected void tearDown() throws Exception
    {
        if (fsysMgr != null)
        {
            fsysMgr.destroy();
            edMgr.destroy();
        }

        cleanDir(new File(TESTDIR));
        super.tearDown();
    }

    private void cleanDir(File dir)
    {
        if (dir.exists() == false) return;

        String[] contents = dir.list();
        for (int i = 0; i < contents.length; ++i)
        {
            File target;
            try
            {
                target = new File(dir.getCanonicalFile() + "/" + contents[i]);
            }
            catch (Exception e)
            {
                continue;
            }

            if (target.isDirectory()) cleanDir(target);
            target.delete();
        }
    }

    private class myLoadingEventListener implements AsynchronousLoadingEventListener
    {
        public boolean eventReceived;

        public boolean successEventReceived;

        public boolean loadingAbortedReceived;

        Vector eventList = new Vector();

        public myLoadingEventListener()
        {
            eventReceived = false;
            return;
        }

        public synchronized void receiveEvent(AsynchronousLoadingEvent e)
        {
            eventReceived = true;
            eventList.addElement(e);
            notifyAll();
        }

        public AsynchronousLoadingEvent checkEvent()
        {
            if (eventList.size() > 0)
                return (AsynchronousLoadingEvent) eventList.elementAt(0);
            else
                return null;
        }

        public void waitEvent(long millisec) throws InterruptedException
        {
            int count = eventList.size();
            if (count <= 0)
            {
                wait(millisec);
            }
            processEvent();
        }

        public void resetAll()
        {
            reset();
            eventList.removeAllElements();
        }

        public void reset()
        {
            eventReceived = false;
            successEventReceived = false;
            loadingAbortedReceived = false;
        }

        private void processEvent()
        {
            Object e = null;

            synchronized (eventList)
            {
                if (eventList.size() > 0)
                {
                    e = eventList.elementAt(0);
                    eventList.removeElementAt(0);
                }
            }

            if (e != null)
            {
                if (e instanceof SuccessEvent)
                {
                    successEventReceived = true;
                }
                else if (e instanceof LoadingAbortedEvent)
                {
                    loadingAbortedReceived = true;
                }
                else
                {
                    fail("Unexpected event received = " + e);
                }
            }
        }
    }

    private class myObjectChangeEventListener implements ObjectChangeEventListener
    {
        private ObjectChangeEvent eventReceived;

        public myObjectChangeEventListener()
        {
            eventReceived = null;
            return;
        }

        public void receiveObjectChangeEvent(ObjectChangeEvent e)
        {
            eventReceived = e;
            return;
        }

        public ObjectChangeEvent checkEvent()
        {
            ObjectChangeEvent returnEvent = eventReceived;
            eventReceived = null;
            return returnEvent;
        }

    }

    public void testConstructor()
    {
        DSMCCObject obj1 = new DSMCCObject(dir1);
        assertNotNull("DSMCCObject(path) object wasn't instantiated", obj1);

        DSMCCObject obj2 = new DSMCCObject(dir1, file1);
        assertNotNull("DSMCCObject(path,name) object wasn't instantiated", obj2);

        DSMCCObject obj3 = new DSMCCObject(obj1, file1);
        assertNotNull("DSMCCObject(DSMCCObject,file) object wasn't instantiated", obj3);
        assertEquals("DSMCCObject(path,name) path should be equal to DSMCCObject(DSMCCObject,file) path",
                obj2.getPath(), obj3.getPath());
    }

    public void testSyncLoad() throws Exception
    {
        DSMCCObject obj = new DSMCCObject(path1);

        obj.synchronousLoad();
        assertTrue("DSMCCObject should be loaded after synchronousLoad()", obj.isLoaded());

        obj.unload();
        assertFalse("DSMCCObject should not be loaded after unload()", obj.isLoaded());
    }

    public void testAsyncLoad() throws Exception
    {
        DSMCCObject obj = new DSMCCObject(path1);
        myLoadingEventListener loadingListener = new myLoadingEventListener();

        assertFalse("DSMCCObject should not yet be loaded before asynchronousLoad()", obj.isLoaded());

        obj.asynchronousLoad(loadingListener);
        AsynchronousLoadingEvent receivedEvent = null;
        for (int i = 0; i < 100; i++)
        {
            receivedEvent = loadingListener.checkEvent();
            if (receivedEvent != null)
            {
                break;
            }
            java.lang.Thread.sleep(100);
        }
        assertTrue("DSMCCObject.asynchronousLoad() didn't send event", receivedEvent != null);
        assertTrue("DSMCCObject should be loaded after asynchronousLoad()", obj.isLoaded());

        obj.unload();
        assertFalse("DSMCCObject should not be loaded after unload()", obj.isLoaded());
    }

    public void testLoadDirectoryEntry() throws Exception
    {
        DSMCCObject obj = new DSMCCObject(path1);
        myLoadingEventListener loadingListener = new myLoadingEventListener();

        obj.loadDirectoryEntry(loadingListener);
        AsynchronousLoadingEvent receivedEvent = null;
        for (int i = 0; i < 100; i++)
        {
            receivedEvent = loadingListener.checkEvent();
            if (receivedEvent != null)
            {
                break;
            }
            java.lang.Thread.sleep(100);
        }
        assertTrue("DSMCCObject.loadDirectoryEntry() didn't send event", receivedEvent != null);
    }

    public void testAbort() throws Exception
    {
        DSMCCObject obj = new DSMCCObject(path1);
        myLoadingEventListener loadingListener = new myLoadingEventListener();
        boolean caughtRightException = false;

        try
        {
            obj.abort();
        }
        catch (NothingToAbortException e)
        {
            caughtRightException = true;
        }
        assertTrue("DSMCCObject.abort() without an outstanding load should have caused exception", caughtRightException);

        // This load should be immediate, so again no exception should be thrown
        // on the abort.
        caughtRightException = false;
        obj.asynchronousLoad(loadingListener);
        try
        {
            obj.abort();
        }
        catch (NothingToAbortException e)
        {
            caughtRightException = true;
            // fail("DSMCCObject.abort() with an outstanding load should not have caused exception");
        }
        assertTrue("DSMCCObject.abort() with immediate load should have caused exception", caughtRightException);
        // AsynchronousLoadingEvent receivedEvent =
        // loadingListener.checkEvent();
        // assertTrue("DSMCCObject.asynchronousLoad() should have sent out LoadingAbortedEvent on abort()",
        // (receivedEvent instanceof LoadingAbortedEvent) );
        // assertFalse("DSMCCObject should not be loaded after aborted load",
        // obj.isLoaded() );
    }

    public void x_testObjectChangeListeners() throws Exception
    {
        DSMCCObject obj = new DSMCCObject(path1);
        myObjectChangeEventListener changeListener = new myObjectChangeEventListener();

        obj.addObjectChangeEventListener(changeListener);

        // wait for event to come in before object is loaded
        ObjectChangeEvent receivedEvent = null;
        for (int i = 0; i < 100; i++)
        {
            receivedEvent = changeListener.checkEvent();
            if (receivedEvent != null)
            {
                break;
            }
            java.lang.Thread.sleep(100);
        }
        assertFalse("DSMCCObject.addObjectChangeEventListener() sent out event on non-loaded object",
                receivedEvent != null);

        obj.synchronousLoad();
        assertTrue("DSMCCObject should be loaded after synchronousLoad()", obj.isLoaded());

        // wait for first event to come in
        receivedEvent = null;
        for (int i = 0; i < 100; i++)
        {
            receivedEvent = changeListener.checkEvent();
            if (receivedEvent != null)
            {
                break;
            }
            java.lang.Thread.sleep(100);
        }
        assertTrue("DSMCCObject.addObjectChangeEventListener() didn't send 1st event", receivedEvent != null);

        // wait for another event to come in
        receivedEvent = null;
        for (int i = 0; i < 100; i++)
        {
            receivedEvent = changeListener.checkEvent();
            if (receivedEvent != null)
            {
                break;
            }
            java.lang.Thread.sleep(100);
        }
        assertTrue("DSMCCObject.addObjectChangeEventListener() didn't send 2nd event", receivedEvent != null);

        obj.removeObjectChangeEventListener(changeListener);
        changeListener.checkEvent(); // eat any outstanding recieved event

        // wait for another event to come in
        receivedEvent = null;
        for (int i = 0; i < 100; i++)
        {
            receivedEvent = changeListener.checkEvent();
            if (receivedEvent != null)
            {
                break;
            }
            java.lang.Thread.sleep(100);
        }
        assertFalse("DSMCCObject.addObjectChangeEventListener() sent event after listener was removed",
                receivedEvent != null);

        obj.unload();
        assertFalse("DSMCCObject should not be loaded after unload()", obj.isLoaded());
    }

    public void testURL() throws Exception
    {
        DSMCCObject obj = new DSMCCObject(path1);

        assertNull("DSMCCObject.getURL() should be null when object isn't loaded", obj.getURL());

        obj.synchronousLoad();
        assertNotNull("DSMCCObject.getURL() should not be null when object is loaded", obj.getURL());
        obj.unload();
    }

    public void testRetrievalMode()
    {
        DSMCCObject obj = new DSMCCObject("/TestServer1/testfile1");

        obj.setRetrievalMode(DSMCCObject.FROM_CACHE);
        assertEquals("DSMCCObject.setRetrievalMode(FROM_CACHE) didn't work", obj.getRetrievalMode(),
                DSMCCObject.FROM_CACHE);

        obj.setRetrievalMode(DSMCCObject.FROM_CACHE_OR_STREAM);
        assertEquals("DSMCCObject.setRetrievalMode(FROM_CACHE_OR_STREAM) didn't work", obj.getRetrievalMode(),
                DSMCCObject.FROM_CACHE_OR_STREAM);

        obj.setRetrievalMode(DSMCCObject.FROM_STREAM_ONLY);
        assertEquals("DSMCCObject.setRetrievalMode(FROM_STREAM_ONLY) didn't work", obj.getRetrievalMode(),
                DSMCCObject.FROM_STREAM_ONLY);
    }

    public void testStaticPrefetch()
    {
        // NOTE: will always be 'false' (still a valid response)
        // until we really implement cache prefetching within MPE

        byte priority = 1;

        assertFalse("Wow, prefetching is implemented now!", DSMCCObject.prefetch(path1, priority));

        DSMCCObject obj1 = new DSMCCObject(dir1);
        assertFalse("Wow, prefetching is implemented now!", DSMCCObject.prefetch(obj1, file1, priority));
    }

    // test using a DSMCCObject with a non-object carousel file.
    /**
     * @todo disabled per 5128
     */
    public void xxxxtestStoredFile() throws Exception
    {
        String filePath = TESTDIR + "/testFile.txt";
        String filePath2 = TESTDIR + "/doesNotExist.txt";
        File file = new File(filePath);
        File file2 = new File(filePath2);
        File dir = new File(TESTDIR);
        file.createNewFile();
        DSMCCObject obj = new DSMCCObject(file.getPath());
        DSMCCObject obj2 = new DSMCCObject(file2.getPath());
        DSMCCObject dirObj = new DSMCCObject(dir.getPath());

        // tests OCAP 13.3.7.2 (using a DSMCCObject to reference stored files.)
        // abort() will always throw NothingToAbortException
        try
        {
            obj.abort();
            fail("Expected NothingToAbortException to be thrown!");
        }
        catch (NothingToAbortException e)
        {
            // pass
        }

        // TODO: try loading and verify that NothingToAbort is thrown.

        // verify file is not loaded
        assertFalse("Expected the file to not be loaded", obj.isLoaded());
        // async load should succeed immediately or fail with
        // InvalidPathNameException
        myLoadingEventListener listener = new myLoadingEventListener();
        System.out.println("Going to do AsyncLoad");
        synchronized (listener)
        {
            obj.asynchronousLoad(listener);
            listener.waitEvent(1000);
        }
        assertTrue("Expected to receive an event", listener.eventReceived);
        assertTrue("Expected to receive a SuccessEvent", listener.successEventReceived);
        assertTrue("Expected the file to be loaded", obj.isLoaded());
        // unload the file and test isLoaded()
        obj.unload();
        assertFalse("Expected the file to be unloaded", obj.isLoaded());
        // unload() should not have removed the file
        assertTrue("Expected file " + file.getPath() + " to still exist", file.exists());

        System.out.println("Going to do AsyncLoad2");
        synchronized (listener)
        {
            listener.reset();
            try
            {
                obj2.asynchronousLoad(listener);
                fail("Expected InvalidPathNameException to be thrown");
            }
            catch (InvalidPathNameException e)
            {
                // pass
            }
            listener.waitEvent(1000);
        }
        assertFalse("Did not expect to receive an event", listener.eventReceived);
        assertFalse("Did not expect to receive a SuccessEvent", listener.successEventReceived);
        assertFalse("Did not expect to receive a LoadingAbortedEvent", listener.loadingAbortedReceived);

        // isObjectKindKnown shall always return true
        assertTrue("expected isObjectKindKnown() to be true", obj.isObjectKindKnown());

        // isStream should return false
        System.out.println("Going to do isStream");
        assertFalse("Expected isStream() to return false", obj.isStream());
        assertFalse("Expected isStream() to return false", obj2.isStream());
        assertFalse("Expected isStream() to return false", dirObj.isStream());

        // isStreamEvent should return false
        System.out.println("Going to do isStreamEvent");
        assertFalse("Expected isStreamEvent() to return false", obj.isStreamEvent());
        assertFalse("Expected isStreamEvent() to return false", obj2.isStreamEvent());
        assertFalse("Expected isStreamEvent() to return false", dirObj.isStreamEvent());

        // loadDirectoryEntry shall succeed immediately with SuccessEvent
        synchronized (listener)
        {
            listener.reset();
            obj.loadDirectoryEntry(listener);
            listener.waitEvent(1000);
        }
        assertTrue("Expected to receive an event", listener.eventReceived);
        assertTrue("Expected to receive a SuccessEvent", listener.successEventReceived);

        // synchronized(listener)
        // {
        // listener.reset();
        // try
        // {
        // new DSMCCObject(TESTDIR
        // +"/somedir/blah.txt").loadDirectoryEntry(listener);
        // fail("Expected InvalidPathNameException to be thrown");
        // }
        // catch (InvalidPathNameException e)
        // {
        // // pass
        // }
        // listener.waitEvent(1000);
        // }
        // assertFalse("Did not expect to receive an event",
        // listener.eventReceived);
        // assertFalse("Did not expect to receive a SuccessEvent",
        // listener.successEventReceived);

        // prefetch shall return false
        System.out.println("Going to do prefetch");
        assertFalse("Expected prefetch to return false", DSMCCObject.prefetch(dirObj, "file.txt", Byte.MAX_VALUE));
        assertFalse("Expected prefetch to return false", DSMCCObject.prefetch(dirObj, "doesNotExist.txt",
                Byte.MAX_VALUE));
        assertFalse("Expected prefetch to return false", DSMCCObject.prefetch(filePath, Byte.MAX_VALUE));
        assertFalse("Expected prefetch to return false", DSMCCObject.prefetch(filePath2, Byte.MAX_VALUE));

        // synchronousLoad should succeed immediately or fail with an exception
        // verify file is not loaded
        assertFalse("Expected the file to not be loaded", obj.isLoaded());
        System.out.println("Going to do the synchronous load");
        try
        {
            obj.synchronousLoad();
        }
        catch (Exception e)
        {
            fail("Did not expect an exception to be thrown");
        }
        assertTrue("Expected the file to be loaded", obj.isLoaded());
        // unload the file and test isLoaded()
        obj.unload();
        assertFalse("Expected the file to be unloaded", obj.isLoaded());
        // unload() should not have removed the file
        assertTrue("Expected file " + file.getPath() + " to still exist", file.exists());

        System.out.println("Going to do the synchronous load2");
        try
        {
            obj2.synchronousLoad();
            fail("Expected InvalidPathNameException to be thrown");
        }
        catch (InvalidPathNameException e)
        {
            // pass
        }
        assertFalse("Expected the file to not be loaded", obj2.isLoaded());

        // TODO: getURL() returns a URL identifying the stored file

    }
}
