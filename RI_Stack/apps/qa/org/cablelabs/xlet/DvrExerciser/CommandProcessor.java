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

import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Random;
import java.util.Vector;

import javax.tv.service.Service;

import org.ocap.ui.event.OCRcEvent;

/**
 * Process a list of commands to automate the behavior of DvrExerciser. This is
 * a singleton.
 * 
 * <pre>
 * Usage notes:
 * 1. Create an instance of this class.
 * 2. Call processCommandFile, specifying the name of the file containing
 *  a list of commands to be processed.
 * 3. Call <code>start()</code> to start the command processor.
 * 4. Commands will be processed until the vector of commands is exhausted,
 *  at which point the result of the test will be written to the log file.
 *  Note that the first failed command will result in the test stopping with
 *  a status of 'FAILED'.
 * 
 * Command file format:
 * The command file is expected to be a regular text file, where each line of
 *  text represents a command.  
 *  - Some commands will have arguments, while others will not.  
 *  - Commands and arguments are separated by white space.
 *  - Comment lines (those beginning with '#') and blank lines are ignored.
 *  - Lines with unknown commands will result in 'unknown' commands that
 *      are essentially no-ops
 *  
 * The following are the commands known to the command processor.  The items
 * in brackets identify required arguments for the command:
 * 
 * testName         [name of test being executed - string]
 * vkChannelUp
 * vkChannelDown
 * vkStop
 * vkLive
 * vkPlay
 * vkFastFwd
 * vkRewind
 * vkPause
 * vkNumber         [numeric key - integer]
 * 
 * 
 * 
 * deleteAllRecordings
 * createRecording
 * cdsPublishRecording
 * makeFirstRecordingCurrent
 * 
 * wait             [number of seconds to wait - integer]
 * channelUp
 * channelDown
 * recordAndWait    [number of seconds to record - integer]
 * setPlayRate      [play rate - float]
 * testPlayRate     [returns TRUE if current play rate equals this number - float]
 * randomTune       [dwell time - integer]    [run time - integer]
 * enableTsb        [enable state - string = "true" or "false"]
 * label            [name - string]
 * loop             [label name - string]     [loop count - integer]
 * 
 * An example command file might look something like:
 * 
 * <pre>
 * # channel up
 * channelUp
 * 
 * # wait for 5 seconds
 * wait 5
 * 
 * # channel down
 * channelDown
 * 
 * # record for 60 seconds
 * record 60
 * 
 * # play 'current' recording by simulating the pressing of key '4'
 * vkNumber 4
 * 
 * </pre>
 * 
 * @author andy
 * 
 */

public class CommandProcessor extends Thread
{
    // This vector will contain all of the commands extracted from the command
    // file. Once this vector is populated, the commands will sequentially
    // executed from this vector.
    private Vector m_vectCommands;

    private String commandFile;

    // the singleton instance of this class
    private static CommandProcessor m_instance;

    /*
     * Accessor method for the singleton instance of this class.
     * 
     * @return the singleton instance of this class.
     */
    public static CommandProcessor getInstance()
    {
        if (null == m_instance)
        {
            m_instance = new CommandProcessor();
        }
        return m_instance;
    }

    /**
     * Constructor
     */
    private CommandProcessor()
    {
        m_vectCommands = new Vector();
    }

    /**
     * Call this method to process (read in) commands from the command file.
     * Each command read from the command file will become a Command instance
     * and stored, in order, into the vector of commands.
     * 
     * @param commandFile
     *            a text file containing commands to be read.
     * 
     * @return <code>true</code> if the command file was successfully read in,
     *         <code>false</code> if an error occurred while reading the command
     *         file.
     */
    public boolean processCommandFile(String commandFile)
    {
        this.commandFile = commandFile;
        boolean bRetVal = false;
        BufferedReader br;
        int i;

        try
        {
            br = new BufferedReader(new FileReader(commandFile));

            // while the file has remaining lines of text...
            while (true == br.ready())
            {
                // ...get the next line, trimming any leading and trailing
                // whitespace chars
                String line = br.readLine();
                line = line.trim();

                // skip over blank lines and comment lines
                if ((0 != line.length()) && (false == line.startsWith("#")))
                {
                    // find the command name by searching for the first
                    // whitespace char following the command name
                    char[] chars = line.toCharArray();
                    for (i = 0; i < chars.length; i++)
                    {
                        if (true == Character.isWhitespace(chars[i]))
                        {
                            break;
                        }
                    }
                    // save the command name
                    String strCommand = new String(chars, 0, i);

                    // the remaining part of the line contains any arguments
                    // that may be associated with the command
                    String args = "";
                    if (i < chars.length)
                    {
                        args = new String(chars, i, chars.length - i).trim();
                    }

                    // have the command factory create the command and add
                    // it to the vector of commands
                    Command command = Command.commandFactory(strCommand, args);
                    m_vectCommands.add(command);
                    DvrExerciser.getInstance().logIt("Loading: " + command.toString());
                }
            }
            bRetVal = true;
            br.close();

            prepareLoops();
        }
        catch (FileNotFoundException fnfex)
        {
            fnfex.printStackTrace();
        }
        catch (IOException ioex)
        {
            ioex.printStackTrace();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }

        return bRetVal;
    }

    /**
     * Prepares loops for normal processing.
     * 
     * <pre>
     * The strategy for processing loops is:
     * - Each loop command must be associated with a label command 
     * - Loop commands can occur before or after label commands
     * - This method will rearrange the order of commands in the 
     *  command vector so that loop commands will always occur
     *  after their corresponding label commands to facilitate
     *  loop processing.
     * - Once this method has been called, the vector of commands
     *  is ready for processing.
     * </pre>
     * 
     */
    private void prepareLoops()
    {
        // for each command in the vector of commands...

        for (int loopIndex = 0; loopIndex < m_vectCommands.size(); loopIndex++)
        {
            // ...if it is a loop command...
            if (m_vectCommands.get(loopIndex) instanceof CommandLoop)
            {
                CommandLoop commandLoop = (CommandLoop) m_vectCommands.get(loopIndex);
                // ...find the associated label command
                for (int labelIndex = 0; labelIndex < m_vectCommands.size(); labelIndex++)
                {
                    Command labelTest = (Command) m_vectCommands.get(labelIndex);
                    if (labelTest instanceof CommandLabel)
                    {
                        CommandLabel commandLabel = (CommandLabel) labelTest;

                        // if the loop's label command matches this label...
                        if (true == commandLabel.getLabelName().equals(commandLoop.getLabelName()))
                        {
                            // ...then the correct label has been found
                            // if loop is before label
                            if (loopIndex < labelIndex)
                            {
                                // swap loop and label positions in vector
                                m_vectCommands.set(loopIndex, commandLabel);
                                m_vectCommands.set(labelIndex, commandLoop);

                                // set label index of loop command (which used
                                // to be the index of the loop command)
                                commandLoop.setLabelIndex(loopIndex);

                                // move the search index up to the new loop
                                // position
                                // Note that if nested loops are required, then
                                // this
                                // logic will no longer work.
                                loopIndex = labelIndex;
                            }
                            else
                            {
                                commandLoop.setLabelIndex(labelIndex);
                            }

                            // we are done with this loop command, so move on to
                            // the next
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * This is the thread which processes (executes) the vector of commands.
     */
    public void run()
    {
        DvrExerciser.getInstance().logIt("Starting Command File: " + this.commandFile);
        // assume success
        boolean bTestPassed = true;
        int testStep;

        boolean continueTestOnFail = false;


        // for each command in the vector...
        for (testStep = 0; testStep < m_vectCommands.size(); testStep++)
        {
            Command command = (Command) m_vectCommands.get(testStep);

            if (command instanceof CommandTestName)
            {
                continueTestOnFail = ((CommandTestName) command).getContinueOnFail();
            }

            // ...log it and...
            //DvrExerciser.getInstance().logIt("Executing: " + command.toString() + " (continue test on failed command? "+continueTestOnFail +")");
            DvrExerciser.getInstance().logIt("Executing Command: " + command.toString());

            // ...execute it
            if (false == command.execute())
            {
                DvrExerciser.getInstance().logIt(command.toString() +" Failed");
                // any failure will cause the command processor to stop
                // and will result in the logging of a test failure
                bTestPassed = false;
                 
                // IT_295: don't exit if user specified continueOnFail flag
                if (! continueTestOnFail)
                {
                    break;
                }
            }
            else
            {
                DvrExerciser.getInstance().logIt(command.toString() +" Passed");
            }

            // if this is a 'loop' command...
            if (command instanceof CommandLoop)
            {
                // get the index of the corresponding label command
                int testIndex = ((CommandLoop) command).getLabelIndex();

                // if the label command index is valid...
                if (-1 != testIndex)
                {
                    // ...update our test step index with it
                    testStep = testIndex;
                }
            }
        }

        // emit the result of the test to the log
        DvrExerciser.getInstance().logIt(
                "Result for " + this.commandFile + ": " + (true == bTestPassed ? "PASSED" : "FAILED"));
    }
}

/**
 * Defines commands that can be processed by the command processor.
 * 
 * 
 * @author andy
 * 
 */
abstract class Command
{
    // The following strings define the possible commands that can appear
    // in a command file.

    // test identification command
    public static String COMMAND_TEST_NAME = "testName";

    // commands to simulate user key presses
    public static String COMMAND_VK_CHANNEL_UP = "vkChannelUp";

    public static String COMMAND_VK_CHANNEL_DOWN = "vkChannelDown";

    public static String COMMAND_VK_STOP = "vkStop";

    public static String COMMAND_VK_LIVE = "vkLive";

    public static String COMMAND_VK_PLAY = "vkPlay";

    public static String COMMAND_VK_FAST_FWD = "vkFastFwd";

    public static String COMMAND_VK_REWIND = "vkRewind";

    public static String COMMAND_VK_PAUSE = "vkPause";

    public static String COMMAND_VK_NUMBER = "vkNumber";

    // operational commands
    public static String COMMAND_DELETE_ALL_RECORDINGS = "deleteAllRecordings";
    public static String COMMAND_CREATE_RECORDING = "createRecording";
    public static String COMMAND_CDS_PUBLISH_RECORDING = "cdsPublishRecording";
    public static String COMMAND_FIRST_RECORDING_CURRENT = "firstRecordingCurrent";

    public static String COMMAND_WAIT = "wait";

    public static String COMMAND_CHANNEL_UP = "channelUp";

    public static String COMMAND_CHANNEL_DOWN = "channelDown";

    public static String COMMAND_RECORD_AND_WAIT = "recordAndWait";

    public static String COMMAND_SET_PLAY_RATE = "setPlayRate";

    public static String COMMAND_TEST_PLAY_RATE = "testPlayRate";

    public static String COMMAND_RANDOM_TUNE = "randomTune";

    public static String COMMAND_ENABLE_TSB = "enableTsb";

    public static String COMMAND_LOG_TSB = "logTsb";

    // control commands
    public static String COMMAND_LABEL = "label";

    public static String COMMAND_LOOP = "loop";

    // Maintains the mapping of command strings to command object classes for
    // use by the command factory method.
    private static HashMap m_hmCommands = new HashMap();

    // populates the command map with the command strings and command classes
    static
    {
        m_hmCommands.put(COMMAND_TEST_NAME, CommandTestName.class);

        // emulate key press commands
        m_hmCommands.put(COMMAND_VK_CHANNEL_UP, CommandVkChannelUp.class);
        m_hmCommands.put(COMMAND_VK_CHANNEL_DOWN, CommandVkChannelDown.class);
        m_hmCommands.put(COMMAND_VK_STOP, CommandVkStop.class);
        m_hmCommands.put(COMMAND_VK_LIVE, CommandVkLive.class);
        m_hmCommands.put(COMMAND_VK_PLAY, CommandVkPlay.class);
        m_hmCommands.put(COMMAND_VK_FAST_FWD, CommandVkFastFwd.class);
        m_hmCommands.put(COMMAND_VK_REWIND, CommandVkRewind.class);
        m_hmCommands.put(COMMAND_VK_PAUSE, CommandVkPause.class);
        m_hmCommands.put(COMMAND_VK_NUMBER, CommandVkNumber.class);

        // operational commands
        m_hmCommands.put(COMMAND_DELETE_ALL_RECORDINGS, CommandDeleteAllRecordings.class);
        m_hmCommands.put(COMMAND_CREATE_RECORDING, CommandCreateRecording.class);
        m_hmCommands.put(COMMAND_CDS_PUBLISH_RECORDING, CommandCdsPublishRecording.class);
        m_hmCommands.put(COMMAND_FIRST_RECORDING_CURRENT, CommandFirstRecordingCurrent.class);
        m_hmCommands.put(COMMAND_WAIT, CommandWait.class);
        m_hmCommands.put(COMMAND_CHANNEL_UP, CommandChannelUp.class);
        m_hmCommands.put(COMMAND_CHANNEL_DOWN, CommandChannelDown.class);
        m_hmCommands.put(COMMAND_RECORD_AND_WAIT, CommandRecordAndWait.class);

        m_hmCommands.put(COMMAND_SET_PLAY_RATE, CommandSetPlayRate.class);
        m_hmCommands.put(COMMAND_TEST_PLAY_RATE, CommandTestPlayRate.class);

        m_hmCommands.put(COMMAND_RANDOM_TUNE, CommandRandomTune.class);
        m_hmCommands.put(COMMAND_ENABLE_TSB, CommandEnableTsb.class);
        m_hmCommands.put(COMMAND_LOG_TSB, CommandLogTsb.class);

        // control commands
        m_hmCommands.put(COMMAND_LABEL, CommandLabel.class);
        m_hmCommands.put(COMMAND_LOOP, CommandLoop.class);
    }

    /**
     * Factory for creating a command given a command string and argument
     * string.
     * 
     * @param command
     *            the name of the command to be created.
     * @param args
     *            contains any arguments that may be required by the command.
     *            Note that the formatting of the arguments are
     *            command-specific.
     * 
     * @return the created Command instance.
     */
    static Command commandFactory(String command, String args)
    {
        Command retVal = null;

        // Create a new command given its name
        try
        {
            Class klass = (Class) m_hmCommands.get(command);
            if (null != klass)
            {
                Class[] ca = new Class[1];
                ca[0] = String.class;

                Constructor constructor = klass.getConstructor(ca);

                Object[] objArgs = new Object[1];
                objArgs[0] = args;
                retVal = (Command) constructor.newInstance(objArgs);
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }

        // if the command couldn't be created...
        if (null == retVal)
        {
            // ...just return the default, unknown command
            retVal = new CommandUnknown(command);
        }

        return retVal;
    }

    // methods which must be implemented by extending classes
    public abstract boolean execute();

    public abstract String toString();
}

/**
 * Emits the test file name to the log output.
 */
class CommandTestName extends Command
{
    private String m_strTestName;
    private boolean m_continueOnFail = false;

    /**
     * Constructor for the <i>testName</i> command.
     * 
     * @param args
     *            a simple string (w/o whitespace) that should be the same as
     *            the test file name.
     */
    public CommandTestName(String args)
    {
        int i;

        // find test name string by searching for the first whitespace char
        char[] chars = args.toCharArray();
        for (i = 0; i < chars.length; i++)
        {
            if (true == Character.isWhitespace(chars[i]))
            {
                break;
            }
        }
        m_strTestName = new String(chars, 0, i);

        // now look for the continueOnFail flag
        String remainingArgs;
        if (i < chars.length)
        {
            remainingArgs = new String(chars, i, chars.length - i).trim();
            if (remainingArgs.equalsIgnoreCase("continueOnFail"))
            { 
                m_continueOnFail = true;
            } 
        }
    }

    public boolean execute()
    {
        return true;
    }

    public String toString()
    {
        return ("Test name = " + m_strTestName);
    }

    public boolean getContinueOnFail()
    {
        return m_continueOnFail;
    }
}

/**
 * Represents any commands read from the command file that aren't known to the
 * command processor.
 */
class CommandUnknown extends Command
{
    private String m_strCommand;

    /**
     * Constructor for the <i>unknown</i> command.
     * 
     * @param args
     *            in this case, args is expected to be the string that was read
     *            from the command file which was expected to be the name of a
     *            valid command.
     */
    public CommandUnknown(String args)
    {
        m_strCommand = args;
    }

    /**
     * Does nothing.
     * 
     * @return <code>true</code>.
     */
    public boolean execute()
    {
        return true;
    }

    public String toString()
    {
        return "Command: unknown (" + m_strCommand + ")";
    }
}

/**
 * Simulate a user 'channel up' key press on the remote control.
 * 
 */
class CommandVkChannelUp extends Command
{
    private KeyEvent m_keyEvent;

    /**
     * Constructor for the <i>vkChannelUp</i> key command.
     * 
     * @param args
     *            ignored
     */
    public CommandVkChannelUp(String args)
    {
        m_keyEvent = new OCRcEvent(DvrExerciser.getInstance(), // souce
                OCRcEvent.KEY_RELEASED, // id
                (long) 0, // when
                0, // modifiers
                OCRcEvent.VK_CHANNEL_UP, // keyCode
                (char) 0); // keyChar
    }

    /**
     * Simulates selection of the channel up remote control key.
     * 
     * @return true;
     */
    public boolean execute()
    {
        DvrExerciser.getInstance().keyReleased(m_keyEvent);

        return true;
    }

    public String toString()
    {
        return "Command: " + COMMAND_VK_CHANNEL_UP;
    }
}

/**
 * Simulate a user 'channel down' key press on the remote control.
 * 
 */
class CommandVkChannelDown extends Command
{
    private KeyEvent m_keyEvent;

    /**
     * Constructor for the <i>vkChannelDown</i> key command.
     * 
     * @param args
     *            ignored
     */
    public CommandVkChannelDown(String args)
    {
        m_keyEvent = new OCRcEvent(DvrExerciser.getInstance(), // source
                OCRcEvent.KEY_RELEASED, // id
                (long) 0, // when
                0, // modifiers
                OCRcEvent.VK_CHANNEL_DOWN, // keyCode
                (char) 0); // keyChar
    }

    /**
     * Simulates selection of the channdl down remote control key.
     * 
     * @return <code>true</code>.
     */
    public boolean execute()
    {
        DvrExerciser.getInstance().keyReleased(m_keyEvent);

        return true;
    }

    public String toString()
    {
        return "Command: " + COMMAND_VK_CHANNEL_DOWN;
    }
}

/**
 * Simulates a user 'stop' key press on the remote control.
 * 
 */
class CommandVkStop extends Command
{
    private KeyEvent m_keyEvent;

    /**
     * Constructor for the <i>vkStop</i> key command.
     * 
     * @param args
     *            ignored
     */
    public CommandVkStop(String args)
    {
        m_keyEvent = new OCRcEvent(DvrExerciser.getInstance(), // source
                OCRcEvent.KEY_RELEASED, // id
                (long) 0, // when
                0, // modifiers
                OCRcEvent.VK_STOP, // keyCode
                (char) 0); // keyChar
    }

    /**
     * Simulates selection of the stop remote control key.
     * 
     * @return <code>true</code>.
     */
    public boolean execute()
    {
        DvrExerciser.getInstance().keyReleased(m_keyEvent);

        return true;
    }

    public String toString()
    {
        return "Command: " + COMMAND_VK_STOP;
    }
}

/**
 * Simulates a user 'live' key press on the remote control.
 * 
 */
class CommandVkLive extends Command
{
    private KeyEvent m_keyEvent;

    /**
     * Constructor for the <i>vkLive</i> key command.
     * 
     * @param args
     *            ignored
     */
    public CommandVkLive(String args)
    {
        m_keyEvent = new OCRcEvent(DvrExerciser.getInstance(), // source
                OCRcEvent.KEY_RELEASED, // id
                (long) 0, // when
                0, // modifiers
                OCRcEvent.VK_LIVE, // keyCode
                (char) 0); // keyChar
    }

    /**
     * Simulates selection of the live remote control key.
     * 
     * @return <code>true</code>.
     */
    public boolean execute()
    {
        DvrExerciser.getInstance().keyReleased(m_keyEvent);

        return true;
    }

    public String toString()
    {
        return "Command: " + COMMAND_VK_LIVE;
    }
}

/**
 * Simulates a user 'play' key press.
 * 
 */
class CommandVkPlay extends Command
{
    private KeyEvent m_keyEvent;

    /**
     * Constructor for the <i>vkPlay</i> key command.
     * 
     * @param args
     *            ignored
     */
    public CommandVkPlay(String args)
    {
        m_keyEvent = new OCRcEvent(DvrExerciser.getInstance(), // source
                OCRcEvent.KEY_RELEASED, // id
                (long) 0, // when
                0, // modifiers
                OCRcEvent.VK_PLAY, // keyCode
                (char) 0); // keyChar
    }

    /**
     * Simulates selection of the play remote control key.
     * 
     * @return <code>true</code>.
     */
    public boolean execute()
    {
        DvrExerciser.getInstance().keyReleased(m_keyEvent);

        return true;
    }

    public String toString()
    {
        return "Command: " + COMMAND_VK_PLAY;
    }
}

/**
 * Simulates a user 'fast forward' key on the remote control.
 * 
 */
class CommandVkFastFwd extends Command
{
    private KeyEvent m_keyEvent;

    /**
     * Constructor for the <i>vkFastFwd</i> command.
     * 
     * @param args
     *            ignored.
     */
    public CommandVkFastFwd(String args)
    {
        m_keyEvent = new OCRcEvent(DvrExerciser.getInstance(), // source
                OCRcEvent.KEY_RELEASED, // id
                (long) 0, // when
                0, // modifiers
                OCRcEvent.VK_FAST_FWD, // keyCode
                (char) 0); // keyChar
    }

    /**
     * Simulates selection of the fast forward remote control key.
     * 
     * @return <code>true</code>.
     */
    public boolean execute()
    {
        DvrExerciser.getInstance().keyReleased(m_keyEvent);

        return true;
    }

    public String toString()
    {
        return "Command: " + COMMAND_VK_FAST_FWD;
    }
}

/**
 * Simulates the pressing of the '<<' key on the remote control.
 */
class CommandVkRewind extends Command
{
    private KeyEvent m_keyEvent;

    /**
     * Constructor for the <i>vkRewind</i> command.
     * 
     * @param args
     *            ignored.
     */
    public CommandVkRewind(String args)
    {
        m_keyEvent = new OCRcEvent(DvrExerciser.getInstance(), // source
                OCRcEvent.KEY_RELEASED, // id
                (long) 0, // when
                0, // modifiers
                OCRcEvent.VK_REWIND, // keyCode
                (char) 0); // keyChar
    }

    /**
     * Simulates selection of the rewind remote control key.
     * 
     * @return <code>true</code>.
     */
    public boolean execute()
    {
        DvrExerciser.getInstance().keyReleased(m_keyEvent);

        return true;
    }

    public String toString()
    {
        return "Command: " + COMMAND_VK_REWIND;
    }
}

/**
 * Simulates the pressing of the 'paused' key on the remote control.
 */
class CommandVkPause extends Command
{
    private KeyEvent m_keyEvent;

    /**
     * Constructor for the <i>vkPause</i> command.
     * 
     * @param args
     *            ignored.
     */
    public CommandVkPause(String args)
    {
        m_keyEvent = new OCRcEvent(DvrExerciser.getInstance(), // source
                OCRcEvent.KEY_RELEASED, // id
                (long) 0, // when
                0, // modifiers
                OCRcEvent.VK_PAUSE, // keyCode
                (char) 0); // keyChar
    }

    /**
     * Simulates selection of the pause remote control key.
     * 
     * @return <code>true</code>.
     */
    public boolean execute()
    {
        DvrExerciser.getInstance().keyReleased(m_keyEvent);

        return true;
    }

    public String toString()
    {
        return "Command: " + COMMAND_VK_PAUSE;
    }
}

/**
 * Simulates the pressing of a (specified) number key on the remote control.
 */
class CommandVkNumber extends Command
{
    private KeyEvent m_keyEvent;

    /**
     * Constructor for the <i>vkNumber</i> command.
     * 
     * @param args
     *            expected to be the string representation of an integer value
     *            from '0' to '9', inclusive.
     */
    public CommandVkNumber(String args)
    {
        int keyNumber = OCRcEvent.VK_0 + Integer.parseInt(args);

        m_keyEvent = new OCRcEvent(DvrExerciser.getInstance(), // source
                OCRcEvent.KEY_RELEASED, // id
                (long) 0, // when
                0, // modifiers
                keyNumber, // keyCode
                (char) 0); // keyChar
    }

    /**
     * Simulates selection of the specified remote control number key.
     * 
     * @return <code>true</code>.
     */
    public boolean execute()
    {
        DvrExerciser.getInstance().keyReleased(m_keyEvent);

        return true;
    }

    public String toString()
    {
        return "Command: " + COMMAND_VK_NUMBER + " " + (m_keyEvent.getKeyCode() - OCRcEvent.VK_0);
    }
}

//
// Operational commands
//

/**
 * Waits for a specified number of seconds.
 */
class CommandWait extends Command
{
    private int m_time;

    /**
     * Constructor for the <i>wait</i> command.
     * 
     * @param args
     *            expected to contain the string representation of an integer
     *            value specifying the amount of time in seconds to wait.
     */
    public CommandWait(String args)
    {
        // the wait command expects a time argument
        m_time = Integer.parseInt(args);
    }

    /**
     * Hangs on to this thread for the number of seconds specified to the
     * constructor.
     * 
     * @return <code>true</code> unless an <code>InterruptedException</code>
     *         occurs.
     */
    public boolean execute()
    {
        boolean bRetVal = false;
        try
        {
            Thread.sleep(m_time * 1000);

            bRetVal = true;
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }

        return bRetVal;
    }

    public String toString()
    {
        return "Command: " + COMMAND_WAIT + ", timeout = " + m_time;
    }
}

/**
 * Selects the next highest channel 'known' to the application.
 * 
 */
class CommandChannelUp extends Command
{
    public CommandChannelUp(String args)
    {
    }

    /**
     * Tunes to the next highest channel 'known' to the application.
     * 
     * @return <code>true</code> if the tune succeeds, <code>false</code>
     *         otherwise.
     */
    public boolean execute()
    {
        boolean bRetVal = false;

        Service service = DvrExerciser.getInstance().getLiveContent().getNextService();
        if (null != service)
        {
            if (DvrExerciser.getInstance().isDvrEnabled())
            {
                bRetVal = DvrTest.getInstance().doTuneLive(service);                
            }
            else
            {
                bRetVal = DvrExerciser.getInstance().getNonDvrTest().doTuneLive(service);
            }
        }
        return bRetVal;
    }

    public String toString()
    {
        return "Command: " + COMMAND_CHANNEL_UP;
    }
}

/**
 * Selects the next lowest channel 'known' to the application.
 * 
 */
class CommandChannelDown extends Command
{
    public CommandChannelDown(String args)
    {
    }

    /**
     * Tunes to the next lowest channel 'known' to the application.
     * 
     * @return <code>true</code> if the tune succeeds, <code>false</code>
     *         otherwise.
     */
    public boolean execute()
    {
        boolean bRetVal = false;

        Service service = DvrExerciser.getInstance().getLiveContent().getPreviousService();
        if (null != service)
        {
            if (DvrExerciser.getInstance().isDvrEnabled())
            {
                bRetVal = DvrTest.getInstance().doTuneLive(service);                
            }
            else
            {
                bRetVal = DvrExerciser.getInstance().getNonDvrTest().doTuneLive(service);
            }
        }
        return bRetVal;
    }

    public String toString()
    {
        return "Command: " + COMMAND_CHANNEL_DOWN;
    }
}

/**
 * Randomly tune to various 'live' services, dwelling on each for a specified
 * <i>dwellTime</i>, allowing the operation to continue for a specified
 * <i>runTime</i>.
 * 
 */
class CommandRandomTune extends Command
{
    Random m_random;

    int m_dwellTime;

    int m_runTime;

    /**
     * Constructor for the <i>randomTune</i> command.
     * 
     * @param args
     *            is expected to contain two arguments. The first is the
     *            <i>dwellTime</i>, or the amount of time to remain tuned to a
     *            service. The <i>runTime</i>, or the amount of time to randomly
     *            tune various services. Both parameters are expected to be
     *            parsable as integers.
     */
    public CommandRandomTune(String args)
    {
        int i;

        m_random = new Random();

        // find dwell time by searching for the first whitespace char
        char[] chars = args.toCharArray();
        for (i = 0; i < chars.length; i++)
        {
            if (true == Character.isWhitespace(chars[i]))
            {
                break;
            }
        }
        String strDwellTime = new String(chars, 0, i);
        m_dwellTime = Integer.parseInt(strDwellTime);

        // now look for the run time
        String strRunTime;
        if (i < chars.length)
        {
            strRunTime = new String(chars, i, chars.length - i).trim();
            m_runTime = Integer.parseInt(strRunTime);
        }
    }

    /**
     * This method will hang on to the execution thread for <i>runTime</i>,
     * tuining to randomly selected services for <i>dwellTime</i>.
     * 
     * @return <code>true</code> if all tunes succeed, <code>false</code> if any
     *         tune fails.
     */
    public boolean execute()
    {
        boolean bRetVal = true;

        try
        {
            do
            {
                Service service = null;

                // perform random tune by scrolling up through the available
                // live services
                // some random number of times between 0 and 10
                int count = m_random.nextInt(10);
                do
                {
                    service = DvrExerciser.getInstance().getLiveContent().getNextService();
                }
                while (0 < --count);

                // perform the tune
                if (null != service)
                {
                    if (DvrExerciser.getInstance().isDvrEnabled())
                    {
                        bRetVal = DvrTest.getInstance().doTuneLive(service);                
                    }
                    else
                    {
                        bRetVal = DvrExerciser.getInstance().getNonDvrTest().doTuneLive(service);
                    }
                }

                // wait for dwellTime
                for (int i = m_dwellTime; 0 < i; --i)
                {
                    // 1-second sleep
                    Thread.sleep(1000);

                    // update the run time
                    --m_runTime;
                }
            }
            while ((true == bRetVal) && (0 < m_runTime));
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }

        return bRetVal;
    }

    public String toString()
    {
        return "Command: " + COMMAND_RANDOM_TUNE + ", dwellTime = " + m_dwellTime + ", runTime = " + m_runTime;
    }
}

/**
 * Enables or disables TSB functionality as specified by the 'enable' parameter.
 * 
 */
class CommandEnableTsb extends Command
{
    private boolean m_bSetting;

    /**
     * Constructor for the <i>enableTsb</i> command.
     * 
     * @param args
     *            expected to contain the string value <code>true</code> or
     *            <code>false</code>, to enable or disable the TSB,
     *            respectively.
     */
    public CommandEnableTsb(String args)
    {
        m_bSetting = Boolean.valueOf(args).booleanValue();
    }

    /**
     * Enables/disables the TSB using the value specified to the constructor.
     */
    public boolean execute()
    {
        if (DvrExerciser.getInstance().isDvrEnabled())
        {
            DvrTest.getInstance().enableTsb(m_bSetting);
        }
        return true;
    }

    public String toString()
    {
        return "Command: " + COMMAND_ENABLE_TSB + ", setting = " + m_bSetting;
    }
}

/**
 * Enables or disables TSB functionality as specified by the 'enable' parameter.
 * 
 */
class CommandLogTsb extends Command
{
    private boolean m_bSetting;

    /**
     * Constructor for the <i>enableTsb</i> command.
     * 
     * @param args
     *            expected to contain the string value <code>true</code> or
     *            <code>false</code>, to enable or disable the TSB,
     *            respectively.
     */
    public CommandLogTsb(String args)
    {
        m_bSetting = Boolean.valueOf(args).booleanValue();
    }

    /**
     * Enables/disables the TSB using the value specified to the constructor.
     */
    public boolean execute()
    {
        if (DvrExerciser.getInstance().isDvrEnabled())
        {
            DvrTest.getInstance().enableTsbLogging(m_bSetting);
        }

        return true;
    }

    public String toString()
    {
        return "Command: " + COMMAND_LOG_TSB + ", setting = " + m_bSetting;
    }
}

/**
 * Requests a recording of the current service, and waits a specified amount of
 * time in seconds for the recording to complete.
 * 
 */
class CommandRecordAndWait extends Command
{
    private int m_time;

    /**
     * Constructor for the <i>recordAndWait</i> command.
     * 
     * @param args
     *            expected to contain a string representation of an integer
     *            value specifying the number of seconds to wait for the
     *            recording to complete.
     */
    public CommandRecordAndWait(String args)
    {
        // the wait command expects a time argument (seconds)
        m_time = Integer.parseInt(args);
    }

    /**
     * Requests a recording, and waits for it to complete.
     * 
     * @return <code>true</code> if the recording completed successfully,
     *         <code>false</code>, otherwise.
     */
    public boolean execute()
    {
        boolean bRetVal = false;
        if (DvrExerciser.getInstance().isDvrEnabled())
        {
            // make the timed recording
            bRetVal = DvrTest.getInstance().doRecording(m_time, true, 0);

            // if the recording was successful...
            if (true == bRetVal)
            {
                // ...wait for it to complete - the return value should indicate
                // whether
                // or not the recording completed successfully
                bRetVal = DvrTest.getInstance().waitForRecordingToComplete(m_time);
            }            
        }

        return bRetVal;
    }

    public String toString()
    {
        return "Command: " + COMMAND_RECORD_AND_WAIT + ", recording time = " + m_time + " sec";
    }
}

/**
 * Deletes all recordings.
 * 
 */
class CommandDeleteAllRecordings extends Command
{
    /**
     * Constructor for the <i>deleteAllRecordings</i> command.
     * 
     * @param args
     *            ignored for this command.
     */
    public CommandDeleteAllRecordings(String args)
    {
    }

    /**
     * Requests the deletion of all recordings.
     * 
     * @return the result of the delete all recordings command.
     */
    public boolean execute()
    {
        if (DvrExerciser.getInstance().isDvrEnabled())
        {
            return DvrTest.getInstance().doDeleteAllRecordings();
        }
        return true;
    }

    public String toString()
    {
        return "Command: " + COMMAND_DELETE_ALL_RECORDINGS;
    }
}

/**
// * Create ? second recording.
 * 
 */
class CommandCreateRecording extends Command
{
    /**
     * Constructor for the <i>deleteAllRecordings</i> command.
     * 
     * @param args
     *            ignored for this command.
     */
    public CommandCreateRecording(String args)
    {
    }

    /**
     * Requests the deletion of all recordings.
     * 
     * @return the result of the delete all recordings command.
     */
    public boolean execute()
    {
        if (DvrExerciser.getInstance().isDvrEnabled())
        {
            // Create a 10 second recording
            return DvrTest.getInstance().doRecording(10, false, 0);
        }
        return true;
    }

    public String toString()
    {
        return "Command: " + COMMAND_CREATE_RECORDING;
    }
}

/**
 * Publish current recording to CDS.
 * 
 */
class CommandCdsPublishRecording extends Command
{
    /**
     * Constructor for the <i>deleteAllRecordings</i> command.
     * 
     * @param args
     *            ignored for this command.
     */
    public CommandCdsPublishRecording(String args)
    {
    }

    /**
     * Requests publishing of current recording.
     * 
     * @return the result of the publish recording command.
     */
    public boolean execute()
    {
        if (DvrExerciser.getInstance().isDvrEnabled())
        {
            String result = DvrExerciser.getInstance().hnPublishRec();
            if (!result.equals("pass"))
            {
                return false;
            }
            DvrExerciser.getInstance().logIt("The recording has been published to the CDS.");
        }
        return true;
    }

    public String toString()
    {
        return "Command: " + COMMAND_CDS_PUBLISH_RECORDING;
    }
}

/**
//* Make the first recording found the current recording.
* 
*/
class CommandFirstRecordingCurrent extends Command
{
  /**
   * Constructor for the <i>deleteAllRecordings</i> command.
   * 
   * @param args
   *            ignored for this command.
   */
  public CommandFirstRecordingCurrent(String args)
  {
  }

  /**
   * Requests the deletion of all recordings.
   * 
   * @return the result of the delete all recordings command.
   */
  public boolean execute()
  {
      if (DvrExerciser.getInstance().isDvrEnabled())
      {
          // Make the first recording found the current recording
          DvrTest.getInstance().setDefaultCurrentRecording();
      }
      return true;
  }

  public String toString()
  {
      return "Command: " + COMMAND_FIRST_RECORDING_CURRENT;
  }
}

/**
 * Sets the current play rate for live and recorded services to a specified
 * floating point value.
 * 
 */
class CommandSetPlayRate extends Command
{
    private float m_rate;

    /**
     * Constructor for the <i>setPlayRate</i> command.
     * 
     * @param args
     *            a string representation of the specified play rate. Note that
     *            this string must be parsable as a floating point value.
     */
    public CommandSetPlayRate(String args)
    {
        m_rate = Float.parseFloat(args);
    }

    /**
     * Sets the play rate to that specified in the constructor.
     */
    public boolean execute()
    {
        DvrTest.getInstance().setPlaybackRate(m_rate);
        return true;
    }

    public String toString()
    {
        return "Command: " + COMMAND_SET_PLAY_RATE + " rate = " + m_rate;
    }
}

/**
 * Tests that the current play rate matches a specified value.
 * 
 */
class CommandTestPlayRate extends Command
{
    private float m_rate;

    /**
     * Constructor for the <i>testPlaybackRate</i> command.
     * 
     * @param args
     *            a string representation of the expected play rate. Note that
     *            this string must be parsable as a floating point value.
     */
    public CommandTestPlayRate(String args)
    {
        m_rate = Float.parseFloat(args);
    }

    /**
     * Retrieves the current play rate and compares it to the expected play
     * rate.
     * 
     * @return <code>true</code> if the play rate matches the expected value,
     *         <code>false</code> otherwise.
     */
    public boolean execute()
    {
        boolean bRetVal = false;

        float testRate = DvrTest.getInstance().getPlaybackRate();
        if (testRate == m_rate)
        {
            bRetVal = true;
        }
        return bRetVal;
    }

    public String toString()
    {
        return "Command: " + COMMAND_TEST_PLAY_RATE + " rate = " + m_rate;
    }
}

/**
 * In association with a <i>loop</i> command, the <i>label</i> command is used
 * identify a block of commands that will be executed within a loop.
 */
class CommandLabel extends Command
{
    private String m_labelName;

    /**
     * Constructor for the <i>label</i> command.
     * 
     * @param args
     *            in this case, args is expected to be a string defining the
     *            name for this label.
     */
    public CommandLabel(String args)
    {
        m_labelName = args;
    }

    public String getLabelName()
    {
        return m_labelName;
    }

    public boolean execute()
    {
        return true;
    }

    public String toString()
    {
        return "Command: " + COMMAND_LABEL + " name = " + m_labelName;
    }
}

/**
 * Implements a 'looping' command. A <i>loop</i> command must be associated with
 * a <i>label</i> command. All commands between the loop and label commands will
 * be executed the number of times specified to the loop command.
 * 
 * Note: In order for the loop command to work, the vector of commands must be
 * pre-processed so that the index of the label command with which the loop
 * command is associated is written into the loop command.
 * 
 */
class CommandLoop extends Command
{
    private String m_labelName;

    private int m_count;

    private int m_labelIndex;

    /**
     * Constructor for the <i>loop</i> command.
     * 
     * @param args
     *            is expected to contain two arguments. The first is the name of
     *            the label command with which this command is to be associated.
     *            The second is an integer count of the number of times the
     *            commands in the loop will be executed.
     */
    public CommandLoop(String args)
    {
        int i;

        // find label name by searching for the first whitespace char
        char[] chars = args.toCharArray();
        for (i = 0; i < chars.length; i++)
        {
            if (true == Character.isWhitespace(chars[i]))
            {
                break;
            }
        }
        m_labelName = new String(chars, 0, i);

        // now look for the count
        String strCount;
        if (i < chars.length)
        {
            strCount = new String(chars, i, chars.length - i).trim();
            m_count = Integer.parseInt(strCount);
        }
        m_labelIndex = -1;
    }

    /**
     * Mutator to set the index of the label command associated with this loop
     * command.
     * 
     * @param index
     *            the index of the label command in the vector of commands.
     */
    public void setLabelIndex(int index)
    {
        m_labelIndex = index;
    }

    /**
     * Accessor method allowing the command processor to obtain the index of
     * label command associated with this loop command.
     * 
     * @return the index of the label command associated with this loop command.
     */
    public int getLabelIndex()
    {
        return m_labelIndex;
    }

    public String getLabelName()
    {
        return m_labelName;
    }

    public boolean execute()
    {
        // update the loop count until...
        if (0 >= --m_count)
        {
            // ...it is exhausted
            m_labelIndex = -1;
        }

        // always return true
        return true;
    }

    public String toString()
    {
        return "Command: " + COMMAND_LOOP + " labelName = " + m_labelName + ", count = " + m_count;
    }
}
