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

/*
 * Created on Feb 23, 2007
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.cablelabs.xlet.PersistentStoragePriority;

import java.awt.Component;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.util.Enumeration;
import java.util.Vector;

import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import org.ocap.storage.AvailableStorageListener;
import org.ocap.storage.StorageManager;

import org.dvb.application.AppAttributes;
import org.dvb.application.AppID;
import org.dvb.application.AppProxy;
import org.dvb.application.AppsDatabase;
import org.dvb.application.CurrentServiceFilter;
import org.dvb.io.persistent.FileAttributes;
import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.HSceneTemplate;
import org.havi.ui.event.HRcEvent;

import org.cablelabs.lib.utils.ArgParser;
import org.cablelabs.lib.utils.VidTextBox;
import org.cablelabs.test.autoxlet.*;

/**
 * @author Ryan
 * 
 *         TODO To change the template for this generated type comment go to
 *         Window - Preferences - Java - Code Style - Code Templates
 */
public class PSGobblerXlet extends Component implements Xlet, KeyListener, Driveable, AvailableStorageListener
{

    static final String LOW_PRIORITY = "LOW";

    static final String MED_PRIORITY = "MEDIUM";

    static final String HIGH_PRIORITY = "HIGH";

    static final String NO_PRIORITY = "NONE";

    static final String CHUNK_SIZE = "Kbytes";

    static final String FILE_DELETE = "File_Delete";

    static final String MAX_DELETED = "MaxDeleted";

    static final String FILE_NAME = "Filename";

    static final String CHUNKS_WRITTEN = "ChunksWritten";

    static final String APP_NAME = "AppName";

    static final String CONFIG_FILE = "config_file";

    static final int WAIT_FOR_NEXT_START = 20000;

    // Flags for wipeClean() function call, which deletes all the personal files
    // and their directories if specified. Can also be set to delete all files
    // and
    // directories in the test area (not just those expected to be created).
    static final int CLEAN_FILES = 0; // No flags on just deletes expected files

    static final int CLEAN_DIRS = 1; // Set means to delete directories as well
                                     // as files

    static final int CLEAN_ALL = 2; // Set means to delete all files under the
                                    // test area

    static final String ROOTNAME = System.getProperty("dvb.persistent.root");

    AutoXletClient m_axc = null; // Auto Xlet client

    static Test m_test = null; // Current test function.s

    Monitor m_eventMonitor = null; // Monitor for AutoXlet

    Logger m_log = null; // Logger for AutoXlet

    XletContext m_ctx = null; // This xlet's context

    VidTextBox m_vbox = null;

    ArgParser config_args = null;

    byte m_buf[] = null; // Write buffer

    // A HAVi scene....Graphics for displaying results on the screen.
    HScene m_scene = null; // HScene

    // Applications used in the test
    Vector m_appEntries; // This is the vector used.

    // All files are going to be in dvbroot/oid/aid/xletname.dat
    Vector m_files; // Vector of files to be created.

    // All Directories of form dvbroot/oid/aid
    Vector m_dirs; // Vector of dirs to be created/deleted

    // Stored String name of application
    String AppName = null;

    // Flag for verifying if high water mark is reached
    boolean receivedHWM = false;

    /**
     * Initializes the OCAP Xlet. Note that even though we could theoretically
     * do a lot here, we don't. We wait until startXlet before running the test.
     * All we do here is mount the file system and get test arguments set up. We
     * also set up AutoClient stuff.
     * 
     * @param XletContext
     *            The context for this Xlet is passed in.
     * 
     * @throws XletStateChangeException
     *             If something goes wrong, then an XletStateChangeException is
     *             sent so that the runtime system knows that the Xlet can't be
     *             initialised.
     */
    public void initXlet(XletContext ctx) throws XletStateChangeException
    {
        m_appEntries = new Vector(); // Loaded apps
        m_files = new Vector(); // Files to be created.
        m_dirs = new Vector(); // Dirs to be used/created
        m_axc = new AutoXletClient(this, ctx); // Set up Auto Xlet Client
        m_test = m_axc.getTest();
        if (m_axc.isConnected())
        {
            m_log = m_axc.getLogger();
        }
        else
        {
            m_log = new XletLogger();
        }
        System.out.println("PSTestXlet.initXlet()");
        m_ctx = ctx;

        // Get Argument
        try
        {
            ArgParser args = new ArgParser((String[]) m_ctx.getXletProperty(XletContext.ARGS));
            if (args == null)
            {
                System.out.println("No arguments specified...cannot run test");
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            m_log.log("Failed to get hostapp parameters: " + e.getMessage());
            throw new XletStateChangeException("Failed to get hostapp parameters");
        }

        // We are creating a subdirectory just to make cleanup easier.
        // test directory is under dvbroot/oid/aid/PSTestXlet/....
        File f = new File(ROOTNAME);

        // Get parameters from config file
        try
        {
            ArgParser xlet_args = new ArgParser((String[]) m_ctx.getXletProperty(XletContext.ARGS));
            String str_config_file_name = xlet_args.getStringArg(CONFIG_FILE);
            FileInputStream fis_read = new FileInputStream(str_config_file_name);
            config_args = new ArgParser(fis_read);
            AppID id = getID();
            String aid = Integer.toHexString(id.getAID());
            AppName = config_args.getStringArg(APP_NAME + aid);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        // Fill the write buffer for usespacetest.
        m_buf = new byte[1024];
        for (int i = 0; i < 1024; ++i)
        {
            m_buf[i] = 1;
        }

        // Now create the initial HScene to display results on the screen.
        m_scene = HSceneFactory.getInstance().getBestScene(new HSceneTemplate());
        m_scene.setSize(640, 480);
        m_scene.add(this);
        m_vbox = new VidTextBox(50, 40, 540, 400, 14, 5000);
        m_scene.add(m_vbox);
        this.setVisible(true);
        m_scene.addKeyListener(this);
        m_scene.addKeyListener(m_vbox);

        try
        {
            writeMsg(AppName + " :");
        }
        catch (Exception e)
        {
            throw new XletStateChangeException("Failed to render HScene");
        }

        // We set up the applications as specified in the hostapps.properties
        // file. We filter only the ones in our group and exclude this xlet as
        // this is the controlling xlet.
        appSetup(); // Set up applications in vector of them.

        // For autoxlet purposes
        m_eventMonitor = new Monitor();

        System.out.println("Exited InitXlet()\n");
    }

    public void startXlet()
    {

        m_scene.show();
        m_scene.requestFocus();
        System.out.println("Persistent Storage Test starting");
        displayMenuOptions();
    }

    // Prints out the test options
    private void displayMenuOptions()
    {
        writeMsg("Press for the following options--------");
        writeMsg("1 to Clean All");
        writeMsg("2 to Write Files");
        writeMsg("3 to Kill Apps");
        writeMsg("4 to Start Gobbler");
        writeMsg("5 to Print List of Files");
        writeMsg("6 to Print List of Dirs");
        writeMsg("7 to Kill All Apps");
        writeMsg("INFO to Display Menu Options");
    }

    // Kepresses defined
    public void keyPressed(java.awt.event.KeyEvent e)
    {
        int key = e.getKeyCode();

        switch (key)
        {
            case HRcEvent.VK_1:
                // Wipe the STB clear of storage files
                System.out.println("Executing Clean All");
                wipeClean(CLEAN_ALL);
                writeMsg("Executing Clean All: Complete");
                notifyTestComplete(0, "Clean Done");
                break;
            case HRcEvent.VK_2:
                // Callout xlets to write files to disk
                System.out.println("Executing Write Files");
                StorageManager sm = StorageManager.getInstance();
                sm.addAvailableStorageListener(this, 90);
                receivedHWM = false;
                startXletWriters("PSDead");
                startXletWriters("PSLive");
                // Check if High Water Mark is reached
                checkHWM();
                writeMsg("Executing Write Files: Complete");
                sm.removeAvailableStorageListener(this);
                break;
            case HRcEvent.VK_3:
                // Kill dead xlets
                System.out.println("Executing Kill Apps");
                destroyXletWriters("PSDead");
                writeMsg("Executing Kill Apps: Complete");
                notifyTestComplete(0, "Dead Apps Destroyed Done");
                break;
            case HRcEvent.VK_4:
                // Start Gobbler Xlet
                System.out.println("Executing Gobbler App");
                executeGobbler();
                writeMsg("Executing Gobbler App: Complete");
                break;
            case HRcEvent.VK_5:
                // Print out the files to be generated
                printFileList();
                notifyTestComplete(0, "List Print Done");
                break;
            case HRcEvent.VK_6:
                // Print out the directories to be created
                printDirList();
                notifyTestComplete(0, "Dir Print Done");
                break;
            case HRcEvent.VK_7:
                // Kills all the writer applications
                System.out.println("Executing Kill All Apps");
                destroyXletWriters("PSDead");
                destroyXletWriters("PSLive");
                writeMsg("Executing Kill All Apps : Complete");
                notifyTestComplete(0, "All Apps Destroyed Done");
                break;
            case HRcEvent.VK_8:
                // Executes the test run
                DoAll();
                break;
            case HRcEvent.VK_INFO:
                // Calls to display the menu options
                displayMenuOptions();
                notifyTestComplete(0, "Menu Options Done");
                break;
            default:
                notifyTestComplete(0, "Did Nothing");
                break;
        }
    }

    // Runs the test sequence
    private void DoAll()
    {
        // Kill all write apps
        System.out.println("Executing Kill All Apps");
        destroyXletWriters("PSDead");
        destroyXletWriters("PSLive");
        writeMsg("Executing Kill All Apps : Complete");

        // Clear out all the files in the usr directory
        System.out.println("Executing Clean All");
        wipeClean(CLEAN_ALL);
        writeMsg("Executing Clean All: Complete");

        // Write files from different apps into the usr dir
        System.out.println("Executing Write Files");
        StorageManager sm = StorageManager.getInstance();
        sm.addAvailableStorageListener(this, 90);
        receivedHWM = false;
        startXletWriters("PSDead");
        startXletWriters("PSLive");
        checkHWM();
        writeMsg("Executing Write Files: Complete");
        sm.removeAvailableStorageListener(this);

        // Kill off dead applications
        System.out.println("Executing Kill Apps");
        destroyXletWriters("PSDead");
        writeMsg("Executing Kill Apps: Complete");

        // Start writing the large file
        System.out.println("Executing Gobbler App");
        executeGobbler();
        writeMsg("Executing Gobbler App: Complete");
    }

    // Check done if the high water mark was received. Notifies automation that
    // the check is complete
    private void checkHWM()
    {
        if (receivedHWM)
        {
            writeMsg("PASSED: High Water Mark received");
            notifyTestComplete(0, "High Water Mark received");
        }
        else
        {
            writeMsg("FAILED: High Water Mark not received");
            notifyTestComplete(1, "High Water Mark not received");
        }
    }

    // Prints out the list of files to be created
    private void printFileList()
    {
        writeMsg("Printing out order of files");
        for (int i = 0; i < m_files.size(); ++i)
        {
            writeMsg((String) m_files.elementAt(i));
        }
        writeMsg("Printing File List complete");
    }

    // Prints out the list of dir to be created
    private void printDirList()
    {
        writeMsg("Printing out order of file directories");
        for (int i = 0; i < m_dirs.size(); ++i)
        {
            writeMsg((String) m_dirs.elementAt(i));
        }
        writeMsg("Printing Directory List complete");
    }

    // Returns current ID of this xlet
    private AppID getID()
    {
        int aid, oid;
        String str;

        str = (String) m_ctx.getXletProperty("dvb.org.id");
        oid = (int) Long.parseLong(str, 16);
        str = (String) m_ctx.getXletProperty("dvb.app.id");
        aid = Integer.parseInt(str, 16);

        return new AppID(oid, aid);
    } // getID() function

    // Start the xlets that will exit after creating some files.
    public void startXletWriters(String appname)
    {
        for (int i = 0; i < m_appEntries.size(); ++i)
        {
            AppEntry entry = (AppEntry) m_appEntries.elementAt(i);
            String name = entry.getAppName(); // Get App's name
            if (name.startsWith(appname)) // Does it start with nme passed in
            {
                System.out.println("Starting App: " + name);
                entry.start();// Run Forest Run!
                try
                {
                    Thread.sleep(WAIT_FOR_NEXT_START); // Wait some time prior
                                                       // to the start of
                }
                catch (Exception e)
                {
                    System.out.println("Thread not sleeping ");
                }
            }
        }
    } // startDeadXlets() function

    // Destroys xlets that start with the supplied string in the constructor
    public void destroyXletWriters(String appname)
    {
        for (int i = 0; i < m_appEntries.size(); ++i)
        {
            AppEntry entry = (AppEntry) m_appEntries.elementAt(i);
            String name = entry.getAppName(); // Get App's name
            if (name.startsWith(appname)) // Is a deadxlet Xlet
            {
                System.out.println("Destroying App: " + name);
                entry.stop(); // Die Forest Die!
            }
        }
    }

    // Main test method that writes out chunks of data and verified the files
    // that are being
    // deleted.
    public void executeGobbler()
    {
        AppID id = getID();
        String oid = Integer.toHexString(id.getOID());
        String aid = Integer.toHexString(id.getAID());
        try
        {
            // Get the byte size, number of chunks to write, and the maximum
            // files that are to be deleted
            int numbytes = config_args.getIntArg(CHUNK_SIZE);
            int maxDeleted = config_args.getIntArg(MAX_DELETED + aid);
            int maxWrites = config_args.getIntArg(CHUNKS_WRITTEN + aid);
            String fileName = config_args.getStringArg(FILE_NAME + aid);

            // Creating file
            String fpath = ROOTNAME + "/" + oid + "/" + aid + "/" + fileName;
            System.out.println("Opening " + fpath);
            File usefile = new File(fpath);
            FileOutputStream ufos = new FileOutputStream(usefile);

            // Verify all the files form other xlets are there, no exceptions
            // should be thrown
            boolean Result = checkFiles(0, false);

            // If not, return and message out
            if (!Result)
            {
                writeMsg("FAILED: Failed to find all the files");
                m_log.log("FAILED: Failed to find all the files");
                return;
            }

            // Start writing chunks of data out, i is the number of deletions
            for (int i = 1; i <= maxWrites; i++)
            {
                boolean check = false;
                System.out.println("Writing out chunk " + i + " out of " + maxWrites);
                for (int j = 0; j < numbytes; j++)
                {
                    try
                    {
                        ufos.write(m_buf, 0, 1024);
                    }
                    catch (IOException e)
                    { // if an IO Exception, this may be acceptable
                        writeMsg("IOException has occured");
                    }
                    catch (Exception e)
                    { // priont out the exception if one has occured
                        e.printStackTrace();
                    }
                }
                // Verify that the specific files are deleted in the array
                if (i <= maxDeleted)
                { // specify the proper number of deletions
                    check = checkFiles(i, false);
                }
                else
                {
                    check = checkFiles(maxDeleted, true);
                }
                if (!check)
                { // if the check fails, send a notification
                    writeMsg("FAILED: File check failed on write " + i);
                    m_log.log("FAILED: File check failed on write " + i);
                }
                Result = check & Result; // if any test is 0, fail out
            }
            // Send results
            if (Result)
            {
                writeMsg("PASSED: Gobbler sucessfully completed");
                notifyTestComplete(0, "Gobbler sucessfully completed");
            }
            else
            {
                writeMsg("FAILED: Gobbler failed to complete properly");
                notifyTestComplete(1, "Gobbler failed to complete properly");
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /*
     * Check the files in the vector list with the actual files in the itfs
     * partition
     * 
     * @param acceptException - allows for IO Exception to occur, this may hapen
     * when creting a new file in a
     * 
     * @param deletedFiles - files deleted from the array, the files shall be
     * deleted from the begining of the array up to the number of files checked
     * is equal
     */
    private boolean checkFiles(int deletedFiles, boolean acceptException)
    {
        boolean fileFound = true; // remains true if purge order is true.
        int deleteCount = 0;
        try
        {
            System.out.println("Number of files that are to be deleted " + deletedFiles);
            for (int i = 0; i < m_files.size(); i++)
            {
                String str = (String) m_files.elementAt(i); // Get file
                File f = new File(str); // Create file
                if (f.exists()) // If it exists, print it.
                {
                    if (deleteCount == deletedFiles)
                    { // all the files lower in priority have been purged
                        System.out.println("Found file :" + str);
                        fileFound = true;
                        String fs = new Integer((int) f.length()).toString(); // Get
                                                                              // File
                                                                              // size
                        System.out.println(str + "    size = " + fs); // Print
                                                                      // file
                                                                      // and
                                                                      // size
                        str = f.getName(); // Get just the short filename
                    }
                    else if (deleteCount < deletedFiles)
                    {// all the files lower priority have not been deleted
                        fileFound = false;
                        writeMsg("FAILED: Unexpected found file " + str);
                        m_log.log("FAILED: Unexpected found file " + str);
                        return fileFound;
                    }
                    else
                    { // Should not get to this condition, but if so there is a
                      // test bug
                        writeMsg("Test error");
                        m_log.log("Test error");
                        return false;
                    }
                }
                else
                {
                    if (deleteCount < deletedFiles)
                    { // all the files lower priority have not been deleted
                        System.out.println("Expected deleted file :" + str); // Validate
                                                                             // the
                                                                             // check
                                                                             // is
                                                                             // correct
                        deleteCount += 1; // increment
                        fileFound = true;
                    }
                    else if (deleteCount == deletedFiles)
                    { // if all the files of higher or equal priority are left
                        fileFound = false;
                        writeMsg("FAILED: Unexpected deleted file or unable to reference" + str);
                        m_log.log("FAILED: Unexpected deleted file or unable to reference" + str);
                        return fileFound;
                    }
                    else
                    { // Should not get to this condition, but if so there is a
                      // test bug
                        writeMsg("Test error");
                        m_log.log("Test error");
                        return false;
                    }
                }
            } // for(....
            System.out.println("--------------------------------------");
            return fileFound;
        } // try........
        catch (Exception e)
        {
            e.printStackTrace();
            writeMsg("Exception other than IO exception thrown");
            m_log.log("Exception other than IO exception thrown");
            fileFound = false;
        }
        return false;
    }

    // Function creates vector of applications not including this one.
    public void appSetup()
    {
        AppsDatabase db = AppsDatabase.getAppsDatabase(); // Get Apps DB
        // This is how we filter to keep only apps in this service group
        // The weird boolean part will also keep this main test app out of
        // the enumeration.
        Enumeration enm = db.getAppAttributes(new CurrentServiceFilter()
        {
            public boolean accept(AppID appid)
            {
                return !appid.equals(getID());
            }
        });
        // Iterate through all these apps
        while (enm.hasMoreElements())
        {
            // Get next app
            AppAttributes attrib = (AppAttributes) enm.nextElement();
            // Get the proxy
            AppProxy proxy = db.getAppProxy(attrib.getIdentifier());
            // Finally construct an AppEntry, so we can control it
            AppEntry tentry = new AppEntry(attrib, proxy);
            m_appEntries.addElement(tentry);
            // Now add full pathname of test files to vector list
            String name = tentry.getAppName(); // Get name of xlet
            if (name.startsWith("PSLive") || name.startsWith("PSDead"))
            {
                // Create the directories
                AppID taid = tentry.getID();
                String dir = ROOTNAME + "/" + Integer.toHexString(taid.getOID()) // Create
                                                                                 // Pathname
                        + "/" + Integer.toHexString(taid.getAID());
                m_dirs.addElement(dir);

                // Create the files
                for (int j = 0; j < 3; ++j)
                {
                    String priority = null;
                    switch (j)
                    {
                        case 0:
                            priority = LOW_PRIORITY;
                            break;
                        case 1:
                            priority = MED_PRIORITY;
                            break;
                        case 2:
                            priority = HIGH_PRIORITY;
                            break;
                    }
                    String path = dir + "/" + name + "_" + priority + ".dat"; // Now
                                                                              // Create
                                                                              // filename
                    m_files.addElement(path); // Add pathname to vector list
                }
            }
        } // Add the test file that is going to be created
        AppID taid = getID();
        String dir = ROOTNAME + "/" + Integer.toHexString(taid.getOID()) + "/" // Create
                                                                               // Pathname
                + Integer.toHexString(taid.getAID());
        m_dirs.addElement(dir);
        try
        {
            dir = dir + "/" + (config_args.getStringArg(FILE_NAME + Integer.toHexString(taid.getAID())));
            m_files.addElement(dir);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    // Cleanup after and before the test. See CLEAN_ definitions in variable
    // members.
    public void wipeClean(int flags)
    {
        System.out.println("+---------------------------------------+");
        System.out.println("*** Cleaning out dvbroot/*");
        if ((flags & CLEAN_ALL) != 0) // If to clean out all files
        {
            try
            {
                wipeall(ROOTNAME, flags); // Delete all the files.
            }
            catch (Exception e) // and catch any exceptions
            {
                e.printStackTrace();
            }
            return; // Exit here....done.
        }

        // CLEAN_ALL not set, so first, delete all the files creatable.
        for (int i = 0; i < m_files.size(); ++i)
        {
            File f = new File((String) m_files.elementAt(i)); // Get file
            try
            {
                System.out.println("File " + (String) m_files.elementAt(i) + " deleted");
                f.delete(); // Delete it
            }
            catch (Exception e)
            {
                e.printStackTrace(); // TODO:- Delete this line (shouldn't cause
                                     // an error)
            }
        } // for...

        if ((flags & CLEAN_DIRS) == 0) // If directory delete flag not set, exit
            return;

        for (int i = 0; i < m_dirs.size(); ++i)
        {
            File f = new File((String) m_dirs.elementAt(i)); // Get Dir
            try
            {
                System.out.println("Directory " + (String) m_dirs.elementAt(i) + "/ deleted!");
                f.delete();
            }
            catch (Exception e)
            {
                e.printStackTrace(); // TODO:- Delete this line (shouldn't cause
                                     // an error
            }
        } // for....
    } // wipeClean(........

    // This is a seperate function as it has to be called recursively to clean
    // out
    // a directory of all files (and directories if the flag is set). Flags is
    // as in
    // wipeClean function. path is the current directory under which to delete
    // the files
    // recursively.
    public void wipeall(String path, int flags)
    {
        try
        {
            File f = new File(path); // Top directory
            if (f.exists() == false) // Make sure it exists
            {
                return;
            }
            String[] strlist = f.list(); // Get all files/dirs
            for (int i = 0; i < strlist.length; ++i) // Iterate through all the
                                                     // files/dir
            {
                String curpath = strlist[i]; // Current file/dir
                curpath = path + "/" + curpath;
                File curfile = new File(curpath);
                if (!curfile.isDirectory()) // if it's not a directory
                {
                    System.out.println("File " + curfile.getAbsolutePath() + " deleted");
                    if (!curfile.delete()) // Delete it
                    {
                        System.out.println("Could not delete file " + curfile.getAbsolutePath());
                    }
                }
                else
                // Else is directory
                {
                    wipeall(curpath, flags); // So iterate until not.
                    if (((flags & CLEAN_DIRS) != 0) // If flag set
                            && !curpath.equals(ROOTNAME)) // and not top
                                                          // directory
                    {
                        System.out.println("Directory " + curfile.getAbsolutePath() + "/ deleted");
                        if (!curfile.delete())
                        {
                            System.out.println("Could not delete directory " + curfile.getAbsolutePath());
                        }
                    } // if (((flags....
                }
            } // for(...
        } // try....
        catch (Exception e) // Print any exceptions received.
        {
            e.printStackTrace();
        }
    } // wipeall(....

    public void notifyTestComplete(int result, String reason)
    {
        String testResult = "PASSED";
        if (result != 0)
        {
            testResult = "FAILED: " + reason;
        }
        m_log.log("Test <" + AppName + "> completed; result=" + testResult);
        m_test.assertTrue("Test <" + AppName + "> failed:" + reason, result == 0);
        m_eventMonitor.notifyReady();
    }

    /*
     * For AutoXlet automation framework
     */
    public void dispatchEvent(KeyEvent e, boolean useMonitor, int timeout)
    {
        if (useMonitor)
        {
            m_eventMonitor.setTimeout(timeout);
            synchronized (m_eventMonitor)
            {
                keyPressed(e);
                m_eventMonitor.waitForReady();
            }
        }
        else
        {
            keyPressed(e);
        }
    }

    public void writeMsg(String msg)
    {
        System.out.println(msg);
        m_vbox.write(msg);
    }

    public void notifyHighWaterMarkReached()
    {
        m_vbox.write("Notification of High Water Mark received.");
        receivedHWM = true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.tv.xlet.Xlet#pauseXlet()
     */
    public void pauseXlet()
    {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.tv.xlet.Xlet#destroyXlet(boolean)
     */
    public void destroyXlet(boolean arg0) throws XletStateChangeException
    {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent)
     */
    public void keyTyped(KeyEvent arg0)
    {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent)
     */
    public void keyReleased(KeyEvent arg0)
    {
        // TODO Auto-generated method stub

    }
}
