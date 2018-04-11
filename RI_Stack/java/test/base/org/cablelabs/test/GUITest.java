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

package org.cablelabs.test;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Point;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.zip.GZIPOutputStream;

import javax.tv.xlet.XletContext;

import junit.framework.TestCase;

import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;

/**
 * This extension to the JUnit <code>TestCase</code> provides additional support
 * necessary in both batch and interactive testing of GUI applications.
 * 
 * Static utility methods are added that allow one to control the mouse (e.g.,
 * {@link #mouseMove(int,int) mouseMove} or {@link #mouseClick() mouseClick})
 * and the keyboard (e.g., {@link #keyClick(int) keyClick}) as well as take
 * <i>snapshot</i> images of the components on the screen.
 * 
 * <p>
 * 
 * Interactive GUI testing is supported by way of visual inspection of
 * snapshots. The tester is required to verify the correctness of a snapshot.
 * 
 * Batch testing is supported by the comparison of a component snapshot with a
 * previously saved snapshot that was already validated by a tester. The saved
 * snapshot that new snapshots are compared against is generated during an
 * interactive test.
 * 
 * @author Aaron Kamienski
 * @author Sumathi - Modified to suite Vidiom Test Environment
 */
public abstract class GUITest extends TestCase
{
    private static final int VTE_VTM_VERIFY_SOUND = 0x303;

    public static final int VTE_VTM_VERIFY = 0x00010006;

    public static final int VTE_VTM_COMPARE_IMAGE = 0x00010007;

    public static final int VTE_VTM_VERIFY_START = 0x00010008;

    public static final int VTE_VTM_VERIFY_PACKET = 0x00010009;

    public static final int VERIFY_IMAGE = 1;

    public static final int COMPARE_IMAGE = 2;

    public static final int VERIFY_SOUND = 3;

    // private static Robot robot;

    // xait properties to be set by the xlet context

    private static boolean interactive;

    private static String className;

    private static int versionNumber;

    private static int imgId; // image id set from the xait property

    private static boolean compareImages = true;

    private static int imageId;

    private HScene scene = null;

    private java.awt.Graphics g = null;

    private java.awt.Color bgColor = null, fgColor = null;

    private java.awt.Rectangle rect;

    private DatagramSocket receiveUdpSocket;

    private static Object syncObj = new Object();

    protected void tearDown() throws Exception
    {
        if (scene != null)
        {
            scene.dispose();
        }
        if (g != null)
        {
            g.dispose();
        }
        super.tearDown();
    }

    private String targetIP = "127.0.0.1";

    /**
     * Create a <code>GUITest</code> TestCase.
     */
    public GUITest(String name)
    {
        super(name);
        imageId = 0;
    }

    public HScene getHScene()
    {
        if (scene == null)
        {
            HSceneFactory factory = HSceneFactory.getInstance();
            scene = factory.getDefaultHScene();
            g = scene.getGraphics();
            bgColor = scene.getBackground();
            fgColor = scene.getForeground();
            rect = scene.getBounds();
        }
        scene.setBackground(bgColor);
        scene.setForeground(fgColor);
        g.setColor(bgColor);
        g.fillRect(rect.x, rect.y, rect.width, rect.height);
        scene.paint(g);
        return scene;
    }

    /**
     * Initializes the <code>GUITest</code> framework.
     * 
     * <ul>
     * <li><i>interactive</i> property is initialized
     * </ul>
     * 
     * @see #createResourceDirectory()
     */
    public void setUp() throws Exception
    {
        try
        {
            String[] app_args = (String[]) HaviTestSupervisor.haviTestXletContext.getXletProperty(XletContext.ARGS);
            className = app_args[0];
            interactive = true;
            int i;
            for (i = 0; i < app_args.length; i++)
            {
                if (app_args[i].startsWith("VTM_RUNTEST_interactive=false")) break;
            }
            if (i != app_args.length)
            {
                interactive = false;
            }
            String s = (String) HaviTestSupervisor.haviTestXletContext.getXletProperty("app.1.application_version");
            versionNumber = Integer.parseInt(s);
        }
        catch (Throwable e)
        { // We are running as a JUnit test outside of Xlet context
            interactive = false;
            compareImages = false;
        }
    }

    /**
     * For more complex operations, use the <code>Robot</code> that is used by
     * this <code>GUITest</code> object.
     * 
     * @return the current <code>Robot</code> in use by this object
     */
    /*
     * public static Robot getRobot() { return robot; return null; }
     */

    /**
     * Moves the mouse pointer to the center of the given <code>Component</code>
     * . The operation is undefined if the given <code>Component</code> is not
     * currently on-screen and visible.
     * 
     * @param c
     *            the Component to move to
     */
    public static void mouseMove(Component c)
    {
        Point p = c.getLocationOnScreen();
        Dimension d = c.getSize();

        // robot.mouseMove(p.x + d.width / 2, p.y + d.height / 2);
    }

    /**
     * Moves the mouse point to the given coordinates, relative to the given
     * <code>Component</code>. The operation is undefined if the given
     * <code>Component</code> is not currently on-screen and visible.
     * 
     * @param c
     *            the Component to move to
     * @param x
     *            the x-coordinate, relative to <code>c</code>
     * @param y
     *            the y-coordinate, relative to <code>c</code>
     */
    public static void mouseMove(Component c, int x, int y)
    {
        Point p = c.getLocationOnScreen();

        // robot.mouseMove(p.x + x, p.y + y);
    }

    /**
     * Generates a mouse click (press and release) at the current mouse
     * location.
     */
    public static void mouseClick()
    {
        // robot.mousePress(MouseEvent.BUTTON1_MASK);
        // robot.mouseRelease(MouseEvent.BUTTON1_MASK);
    }

    /**
     * Generates a key click (press and release) of the given keycode.
     */
    public static void keyClick(int keyCode)
    {
        // robot.keyPress(keyCode);
        // robot.keyRelease(keyCode);
    }

    /**
     * Pause until all events have been cleared from the event queue.
     */
    public static void waitForIdle()
    {
        // robot.waitForIdle();
    }

    /**
     * Pause until the given amount of time (> 0 and <= 60000 ms) has elapsed.
     * 
     * @param ms
     *            delay time in milliseconds
     */
    public static void delay(int ms)
    {
        try
        {
            Thread.sleep(ms);
        }
        catch (InterruptedException exc)
        {

        }
    }

    /**
     * Pause until the given component has been repainted. It is not guaranteed
     * that a repaint will occur (in particular, if the component is not
     * visible, no repaint will occur).
     * 
     * @param c
     *            the component to repaint and then wait for
     */
    public static void waitForRepaint(Component c)
    {
        System.out.println("in waitForRepaint");
        c.repaint();
        // robot.waitForIdle();

        // This is a complete hack. If we don't loop, then we get
        // some of the complete images, but not all.
        for (int x = 0; x < 300; x++)
        {
            try
            {
                EventQueue.invokeAndWait(new Runnable()
                {
                    public void run()
                    {
                    }
                });
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    /**
     * Pause until the given component has been focused. It is not guaranteed
     * that a focus gain event will occur (in particular, if the component is
     * not visible, no focusing will occur).
     * 
     * @param c
     *            the component to request focus and then wait for
     */
    public static void waitForFocus(Component c)
    {
        c.requestFocus();
        // robot.waitForIdle();
    }

    /**
     * Returns a snapshot of the given <code>Component</code>. This actually
     * gets the pixel info - pixels currently on-screen that make up the given
     * <code>Component</code> The operation is undefined if the given
     * <code>Component</code> is not currently on-screen and visible.
     * 
     * @param c
     *            the <code>Component</code> whose bounds should be used in
     *            creating a snaphot.
     * @return a compressed (gzip) byte array representing a snapshot of the
     *         given <code>Component</code>
     */
    public static byte[] getSnapshot(Component c)
    {
        Point p = c.getLocationOnScreen();
        Dimension d = c.getSize();
        byte[] snapshot = ScreenCapture.capture(c);
        if (snapshot == null)
        {
            fail("Error in capturing the image");
        }

        System.out.println("*** SIZE=" + snapshot.length);

        int i;
        for (i = 2; i < snapshot.length; i++)
        {
            if (snapshot[i] != 0) break;
        }
        if (i == snapshot.length)
            System.out.println("NULL Image");
        else
            System.out.println("Non-Null Image");

        byte[] compressedByteArray = null;
        try
        {
            System.out.println("**SNAPSHOT bitsperpixel=" + snapshot[0] + " colorFormat=" + snapshot[1]);
            // System.out.println("Compressing message of size: " +
            // snapshot.length);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            GZIPOutputStream gos = new GZIPOutputStream(baos);
            gos.write(snapshot, 0, snapshot.length);
            gos.finish();
            gos.close();
            compressedByteArray = baos.toByteArray();
            System.out.println("Compressed message to size: " + compressedByteArray.length);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Error in compressing the image");
        }
        return compressedByteArray;
    }

    /**
     * Assert that the given <i>snapshot</i> image is identical to the
     * <code>Image</code> stored at the given location.
     * 
     * @param message
     *            the message used if comparison fails
     * @param questions
     *            an array of <code>String</code> messages that constitute
     *            questions regarding the validity of the image in question.
     * @param snapshot
     *            the current snapshot to compare
     * @param width
     *            - width of image
     * @param height
     *            - height of image
     * @param rezName
     *            - IGNORE THIS.
     */
    public void assertImage(String message, String[] questions, byte[] snapshot, int width, int height, String resource)
    {
        String errorMsg = null;
        width = 640; // hardcoded for now. use gfx API to get the screen
                     // resolution
        height = 480;
        imageId++;
        if (compareImages)
        {
            if (interactive == false)
            {
                // non-interactive - subsequent runs of the test where the image
                // is compared
                // with the image obtained from the first run.
                int portNumber = sendCompareImageMessage(snapshot, width, height);
                if (waitForResult(portNumber, COMPARE_IMAGE) == 1)
                    return; // test passed, continue.
                else
                    fail(message);
                fail(message);
                // test failed. abort the test, send error message for logging.
            }
            else
            {
                // interactive - first run of the test. image to be verified on
                // the server
                int portNumber = sendVerifyImageMessage(snapshot, width, height, questions);
                int result = waitForResult(portNumber, VERIFY_IMAGE);
                if (result == -1)
                    return; // no error in image, image saved on server
                else
                {
                    // error in image. abort the test. send the error message
                    // for logging.
                    imageId--;
                    fail(questions[result]);
                }
            }
        }
        else
        {
            // fail("image comparison is not available.");
        }
    }

    /**
     * Assert the given questions. The user should not reply "Fail?" to any of
     * them.
     * 
     * @param questions
     *            an array of <code>String</code> messages that constitute
     *            questions regarding the validity of the image in question.
     */
    public void assertQuestions(String message, String[] questions)
    {
        if (interactive)
        {
            int portNumber = sendSoundTestQuestions(questions);
            int result = waitForResult(portNumber, VERIFY_SOUND);
            if (result == -1)
                return; // no error
            else
                fail(message);
        }
        // error. abort the test. send the error message for logging.
    }

    /**
     * Construct comapre image message VTE_VTM_COMPARE_IMAGE: // current running
     * tests screen captures Message{ Uint32 message; Uint32 messageBodySize;
     * //number of bytes following this field Uint16 classNameSize; Byte
     * bytes[classNameSize]; Uint8 versionNumber; Uint8 imageId; Uint32
     * portNumber; Uint32 imageSize; Byte bytes[imageSize]; }
     */

    private int sendCompareImageMessage(byte[] snapshot, int width, int height)
    {
        int portNumber = 0;
        try
        {
            receiveUdpSocket = new DatagramSocket();
            portNumber = receiveUdpSocket.getLocalPort();
        }
        catch (Exception e)
        {
            fail("Caught an exception while opening the receiving socket - " + e.toString());
        }

        byte[] message = null;
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(bout);
        try
        {
            // Uint32 message
            dout.writeInt(VTE_VTM_COMPARE_IMAGE);
            // Uint32 messageBodySize;
            // overwritten at the end after computing the exact size
            dout.writeInt(0xFFFFFFFF);
            // Uint16 classNameSize;
            dout.writeShort(className.length());
            // Byte bytes[classNameSize];
            dout.writeBytes(className);
            // Uint8 versionNumber;
            dout.writeByte(versionNumber);
            // Uint8 imageId;
            dout.writeByte(imageId);
            // Uint32 portNumber;
            dout.writeInt(portNumber);
            // Uint32 imageSize;
            dout.writeInt(snapshot.length + 4);
            // Byte bytes[imageSize];
            // image length + width (2 bytes)+ height(2 bytes)
            dout.writeShort(width); // width of image
            dout.writeShort(height); // height of image
            dout.write(snapshot);
            message = bout.toByteArray();
            // compute the actual size and overwrite me portNumber =
            // udpSocket.getPort();ssageBodySize field.
            int size = dout.size() - 8;
            // Uint32 messageBodySize;
            // total size - (message_body_size bytes + message bytes)
            message[4] = (byte) (size >> 24);
            message[5] = (byte) (size >> 16);
            message[6] = (byte) (size >> 8);
            message[7] = (byte) (size);
            dout.close();
        }
        catch (Exception e)
        {
            fail("Error while creating compare image message");
        }

        // Send the message to VTE agent over the UDP socket. This message is
        // received by MessageSender
        // and sent to the server.

        try
        {
            DatagramSocket udpSocket = new DatagramSocket();
            DatagramPacket packet = new DatagramPacket(message, message.length, InetAddress.getByName(targetIP), 5200);
            udpSocket.send(packet);
        }
        catch (Exception e)
        {
            fail("Exception - Unable to send 'verify image' message to VTE agent - " + e.toString());
        }
        return portNumber;
    }

    /**
     * Construct verify sound message VTE_VTM_VERIFY_SOUND: // current running
     * tests screen captures Message{ Uint32 message; Uint32 message BodySize;
     * //number of bytes following this field Uint16 classNameSize; Byte
     * bytes[classNameSize]; Uint8 versionNumber; Uint32 portNumber; Uint8
     * noOfQuestions; For(i=0;i< noOfQuestions;i++) { Uint16 questionSize; Byte
     * question[questionSize]; } }
     */

    private int sendSoundTestQuestions(String[] questions)
    {
        int portNumber = 0;
        try
        {
            receiveUdpSocket = new DatagramSocket();
            portNumber = receiveUdpSocket.getLocalPort();
        }
        catch (Exception e)
        {
            fail("Caught an exception while opening the receiving socket - " + e.toString());
        }

        byte[] message = null;
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(bout);
        try
        {
            dout.writeInt(VTE_VTM_VERIFY_SOUND);
            dout.writeInt(0xFFFFFFFF);
            // overwritten at the end after computing the exact size
            dout.writeShort(className.length());
            dout.writeBytes(className);
            dout.writeByte(versionNumber);
            dout.writeInt(portNumber);
            if (questions != null)
            {
                dout.writeByte(questions.length); // number of questions
                for (int i = 0; i < questions.length; i++)
                {
                    dout.writeShort(questions[i].length()); // question length
                    dout.writeBytes(questions[i]); // the question itself
                }
            }
            else
            {
                dout.writeByte(0); // number of questions = 0
            }
            message = bout.toByteArray();
            // compute the actual size and overwrite messageBodySize field.
            int size = dout.size() - 8;
            // total size - (message_body_size bytes + message bytes)
            message[4] = (byte) (size >> 24);
            message[5] = (byte) (size >> 16);
            message[6] = (byte) (size >> 8);
            message[7] = (byte) (size);
            dout.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Error while creating verify image message");
        }

        // Send the message to VTE agent over the UDP socket. This message is
        // received by MessageSender
        // and sent to the server.

        try
        {
            DatagramSocket udpSocket = new DatagramSocket();
            DatagramPacket packet = new DatagramPacket(message, message.length, InetAddress.getByName(targetIP), 5200);
            udpSocket.send(packet);
        }
        catch (Exception e)
        {
            fail("Exception - Unable to send sound test questions to VTE agent - " + e.toString());
        }
        return portNumber;
    }

    /**
     * Construct verify image message VTE_VTM_VERIFY: // current running tests
     * screen captures Message{ Uint32 message; Uint32 messageBodySize; //number
     * of bytes following this field Uint16 classNameSize; Byte
     * bytes[classNameSize]; Uint8 versionNumber; Uint8 imageId; Uint32
     * portNumber Byte bytes[imageSize]; Uint8 noOfQuestions; For(i=0;i<
     * noOfQuestions;i++) { Uint16 questionSize; Byte question[questionSize]; }
     * }
     */
    private final int PARTIAL_THRESHOLD = 1000000;

    private int sendVerifyImageMessage(byte[] snapshot, int width, int height, String[] questions)
    {
        int portNumber = 0;

        try
        {
            receiveUdpSocket = new DatagramSocket();
            portNumber = receiveUdpSocket.getLocalPort();
        }
        catch (Exception e)
        {
            fail("Caught an exception while opening the receiving socket - " + e.toString());
        }

        int total_image_bytes = snapshot.length + 4;
        int dataleft = total_image_bytes;
        int byte_offset = 0;

        while (dataleft > 0)
        {
            int command = VTE_VTM_VERIFY;
            int image_bytes_towrite = 0;
            byte[] message = null;
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            DataOutputStream dout = new DataOutputStream(bout);
            try
            {
                if ((dataleft == total_image_bytes) && (dataleft <= PARTIAL_THRESHOLD))
                {
                    // Uint32 message;
                    dout.writeInt(VTE_VTM_VERIFY);
                }
                else if (dataleft == total_image_bytes)
                {
                    dout.writeInt(VTE_VTM_VERIFY_START);
                    command = VTE_VTM_VERIFY_START;
                }
                else
                {
                    dout.writeInt(VTE_VTM_VERIFY_PACKET);
                    command = VTE_VTM_VERIFY_PACKET;
                }

                System.out.println("**Command = " + command);

                // Uint32 messageBodySize;
                // overwritten at the end after computing the exact size
                dout.writeInt(0xFFFFFFFF);
                // Uint16 classNameSize;
                dout.writeShort(className.length());
                // Byte bytes[classNameSize];
                dout.writeBytes(className);
                // Uint8 versionNumber;
                dout.writeByte(versionNumber);
                // Uint8 imageId;
                dout.writeByte(imageId);
                dout.writeInt(portNumber);
                // image length + width (2 bytes)+ height(2 bytes)
                // Uint16 imageSize;
                dout.writeInt(snapshot.length + 4);

                switch (command)
                {
                    case VTE_VTM_VERIFY_PACKET:
                        image_bytes_towrite += Math.min(dataleft, PARTIAL_THRESHOLD);
                        // Uint32 offset;
                        dout.writeInt(byte_offset);
                        // Uint32 image_bytes_in_packet;
                        dout.writeInt(image_bytes_towrite);
                        // Uint32 complete
                        dout.writeInt((dataleft - image_bytes_towrite) == 0 ? 1 : 0);

                        dout.write(snapshot, byte_offset, image_bytes_towrite);
                        byte_offset += image_bytes_towrite;
                        dataleft -= image_bytes_towrite;
                        break;
                    case VTE_VTM_VERIFY_START:
                        dataleft -= 4;
                        image_bytes_towrite += Math.min(dataleft, PARTIAL_THRESHOLD);

                        // Uint32 image_bytes_in_packet;
                        dout.writeInt(image_bytes_towrite);

                        dout.writeShort(width); // width of image
                        dout.writeShort(height); // height of image

                        dout.write(snapshot, byte_offset, image_bytes_towrite);
                        byte_offset += image_bytes_towrite;
                        dataleft -= image_bytes_towrite;
                        break;
                    case VTE_VTM_VERIFY:
                        dataleft = 0;
                        dout.writeShort(width); // width of image
                        dout.writeShort(height); // height of image
                        dout.write(snapshot);
                        break;
                }

                if (command != VTE_VTM_VERIFY_PACKET)
                {
                    if (questions != null)
                    {
                        // Uint8 noOfQuestions;
                        dout.writeByte(questions.length); // number of questions
                        for (int i = 0; i < questions.length; i++)
                        {
                            // Uint16 questionSize;
                            dout.writeShort(questions[i].length()); // question
                                                                    // length
                            // Byte question[questionSize];
                            dout.writeBytes(questions[i]); // the question
                                                           // itself
                        }
                    }
                    else
                    {
                        // Uint8 noOfQuestions;
                        dout.writeByte(0); // number of questions = 0
                    }
                }
                message = bout.toByteArray();
                // compute the actual size and overwrite messageBodySize field.
                int size = dout.size() - 8;
                // Uint32 messageBodySize;
                // total size - (message_body_size bytes + message bytes)
                message[4] = (byte) (size >> 24);
                message[5] = (byte) (size >> 16);
                message[6] = (byte) (size >> 8);
                message[7] = (byte) (size);
                dout.close();
            }
            catch (Exception e)
            {
                fail("Error while creating verify partial image message");
            }

            // Send the message to VTE agent over the UDP socket. This message
            // is received by MessageSender
            // and sent to the server.
            try
            {
                System.out.println("**Message size = " + message.length);
                DatagramSocket udpSocket = new DatagramSocket();
                DatagramPacket packet = new DatagramPacket(message, message.length, InetAddress.getByName(targetIP),
                        5200);
                udpSocket.send(packet);
                System.out.println("......Sending verify message\n");
            }
            catch (Exception e)
            {
                System.out.println("Error Sending message: " + e.getMessage());
                fail("Exception - Unable to send 'verify image' message to VTE agent - " + e.toString());
            }
        }
        return portNumber;
    }

    /**
     * Wait for result message. VTE receives the VTM_VTE_COMPARE_IMAGE_RESULT
     * message from the server and MessageSender redirects it here.
     * 
     * @param pnum
     *            - portnumber
     * @param id
     *            - indicates the request type (VERIFY_IMAGE, COMAPRE_IMAGE,
     *            VERIFY_SOUND)
     */
    private int waitForResult(int pnum, int id)
    {
        byte[] result = new byte[2];
        try
        {
            boolean quit = false;
            DatagramPacket receivePacket = new DatagramPacket(result, result.length);
            receiveUdpSocket.receive(receivePacket);
        }
        catch (Exception e)
        {
            fail("Caught an exception while receiving the result from server - " + e.toString());
        }
        switch (id)
        {
            case VERIFY_IMAGE:
                if (result[0] == 1) return -1;
                return result[1];
            case COMPARE_IMAGE:
            case VERIFY_SOUND:
                return result[0];
        }
        return 1;
    }
}
