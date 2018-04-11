/*
<p>This is not an official specification document, and usage is restricted.
</p>
<a name="notice"><strong><center>
NOTICE
</center></strong><br>
<br>

(c) 2005-2007 Sun Microsystems, Inc. All Rights Reserved.
<p>
Neither this file nor any files generated from it describe a complete
specification, and they may only be used as described below. For
example, no permission is given for you to incorporate this file, in
whole or in part, in an implementation of a Java specification.
<p>
Sun Microsystems Inc. owns the copyright in this file and it is provided
to you for informative, as opposed to normative, use. The file and any
files generated from it may be used to generate other informative
documentation, such as a unified set of documents of API signatures for
a platform that includes technologies expressed as Java APIs. The file
may also be used to produce "compilation stubs," which allow
applications to be compiled and validated for such platforms.
<p>
Any work generated from this file, such as unified javadocs or compiled
stub files, must be accompanied by this notice in its entirety.
<p>
This work corresponds to the API signatures of JSR 219: Foundation
Profile 1.1. In the event of a discrepency between this work and the
JSR 219 specification, which is available at
http://www.jcp.org/en/jsr/detail?id=219, the latter takes precedence.
*/


  


package javax.microedition.io;

import java.io.*;

/** 
 * This interface defines the capabilities that a datagram connection
 * must have.
 * <!--
 * <p>
 * Reminder: Since the CLDC Specification does not define any
 * actual network protocol implementations, the syntax for 
 * datagram addressing is not defined in the CLDC Specification.
 * Rather, syntax definition takes place at the level of J2ME
 * profiles such as MIDP.
 * <p>
 * In the sample implementation that is provided as part of the
 * CLDC reference implementation, the following addressing scheme
 * is used:
 * <p>
 * The parameter string describing the target of a connection
 * in the CLDC reference implementation takes the following form:
 * -->
 * The parameter string describing the target of a connection
 * takes the following form:
 *
 * <pre>
 * {protocol}://[{host}]:[{port}]
 * </pre>
 *
 * A datagram connection can be opened in a "client" mode or "server" mode.
 * If the "{host}" part is missing then the connection  is opened as 
 * a "server" (by "server", we mean that a client application initiates
 * communication). When the "{host}" part is specified, the connection
 * is opened as a "client".
 * <p>
 * Examples:
 * <p>
 *  A datagram connection for accepting datagrams<br>
 *  datagram://:1234<p>
 *  A datagram connection for sending to a server:<br>
 *  datagram://123.456.789.12:1234<p>
 *
 * Note that the port number in "server mode" (unspecified host name) is 
 * that of the receiving port. The port number in "client mode" (host name
 * specified) is that of the target port. The reply-to port in both cases
 * is never unspecified. In "server mode", the same port number is used for 
 * both receiving and sending. In "client mode", the reply-to port is 
 * always dynamically allocated.
 * <p>
 * Also note that the allocation of datagram objects is done in a
 * more abstract way than in Java 2 Standard Edition (J2SE).
 * Instead of providing a concrete <code>DatagramPacket</code> class, 
 * an abstract <code>Datagram</code> interface is provided.  This
 * is to allow a single platform to support several different datagram  
 * interfaces simultaneously.  Datagram objects must be allocated by 
 * calling the <code>newDatagram</code> methods of the 
 * <code>DatagramConnection</code> object. 
 * The resulting object is defined using another interface type 
 * called <code>javax.microedition.io.Datagram</code>.
 *
 * @author  Brian Modra
 * @author  Nik Shaylor
 * @version 12/17/01 (CLDC 1.1)
 * @since   CLDC 1.0
 */
public interface DatagramConnection extends Connection
{

    /** 
     * Get the maximum length a datagram can be.
     * Maximum length determines the maximum size
     * of the datagram that can be created using 
     * the <code>newDatagram</code> method, and the 
     * maximum size of the datagram that can be sent
     * or received.
     *
     * @return    The maximum length of a datagram.
     * @exception IOException  If an I/O error occurs.
     */
    public int getMaximumLength() throws IOException;

    /** 
     * Get the nominal length of a datagram.
     * Nominal length refers to the size of the 
     * datagram that is stored into the data buffer.
     * Nominal length may be equal or
     * less than the maximum length of the datagram.
     *
     * @return    The nominal length of a datagram.
     * @exception IOException  If an I/O error occurs.
     */
    public int getNominalLength() throws IOException;

    /** 
     * Send a datagram.  The <code>Datagram</code> object includes
     * the information indicating the data to be sent, its length,
     * and the address of the receiver.  The method sends <code>length</code>
     * bytes starting at the current <code>offset</code> of the
     * <code>Datagram</code> object, where <code>length</code>
     * and <code>offset</code> are internal state variables 
     * of the <code>Datagram</code> object.
     *
     * <!-- GCFOP begin -->
     * <p>
     * If a security manager exists, a datagram cannot be sent
     * if the security manager's <code>checkConnect(host, port)</code>
     * method does not permit at connection to the remote host and port.
     * <!-- GCFOP end -->
     *
     * @param     dgram        A datagram.
     * @exception IOException  If an I/O error occurs.
     * @exception InterruptedIOException Timeout or interrupt occurred.
     *
     * <!-- GCFOP begin -->
     * @throws SecurityException If a security manager exists, and its
     * <code>checkConnect</code> method does not allow the operation.
     * <!-- GCFOP end -->
     */
    public void send(Datagram dgram) throws IOException;

    /** 
     * Receive a datagram.  When this method returns, the internal
     * buffer in the <code>Datagram</code> object is filled with
     * the data received, starting at the location determined by
     * the <code>offset</code> state variable, and the data is
     * ready to be read using the methods of the 
     * <code>DataInput</code> interface.
     * <p>
     * This method blocks until a datagram is received.  The internal
     * <code>length</code> state variable in the <code>Datagram</code>
     * object contains the length of the received datagram.  If the
     * received data is longer than the length of the internal buffer
     * minus offset, data is truncated.
     * <p>
     * This method does not change the internal <i>read/write</i> state
     * variable of the <code>Datagram</code> object. Use method 
     * <code>Datagram.reset</code> to change the pointer before
     * reading if necessary.
     *
     * <!-- GCFOP begin -->
     * <p>
     * If a security manager exists, a datagram cannot be received
     * if the security manager's <code>checkAccept(host, port)</code>
     * method does not permit a connection from the remote host and port.
     * <!-- GCFOP end -->
     *
     * @param     dgram        A datagram.
     * @exception IOException  If an I/O error occurs.
     * @exception InterruptedIOException Timeout or interrupt occurred.
     *
     */
    public void receive(Datagram dgram) throws IOException;

    /** 
     * Create a new datagram object.
     *
     * @param  size            The size of the buffer needed 
     *                         for the datagram
     * @return                 A new datagram
     * @exception IOException  If an I/O error occurs.
     * @exception IllegalArgumentException if the size is negative
     *                         or larger than the maximum size
     */
    public Datagram newDatagram(int size) throws IOException;

    /** 
     * Create a new datagram object.
     *
     * @param  size            The size of the buffer needed
     *                         for the datagram
     * @param  addr            The I/O address to which the datagram
     *                         will be sent, in the form
     *                         <code>"datagram://{host}:{port}"</code>.
     * @return                 A new datagram
     * @exception IOException  If an I/O error occurs.
     * @exception IllegalArgumentException if the size is negative or
     *                         larger than the maximum size, or if the
     *                         address parameter is invalid
     */
    public Datagram newDatagram(int size, String addr) throws IOException;

    /** 
     * Create a new datagram object.
     *
     * @param  buf             The buffer to be used for the datagram
     * @param  size            The size of the buffer needed
     *                         for the datagram
     * @return                 A new datagram
     * @exception IOException  If an I/O error occurs.
     * @exception IllegalArgumentException if the size is negative or
     *                         larger than the maximum size or the given
     *                         buffer's length, or if the buffer parameter 
     *                         is invalid
     */
    public Datagram newDatagram(byte[] buf, int size) throws IOException;

    /** 
     * Make a new datagram object.
     *
     * @param  buf             The buffer to be used for the datagram
     * @param  size            The size of the buffer needed
     *                         for the datagram
     * @param  addr            The I/O address to which the datagram
     *                         will be sent, in the form
     *                         <code>"datagram://{host}:{port}"</code>.
     * @return                 A new datagram
     * @exception IOException  If an I/O error occurs.
     * @exception IllegalArgumentException if the size is negative or
     *                         larger than the maximum size or the given
     *                         buffer's length, or if the address or 
     *                         buffer parameter is invalid
     */
    public Datagram newDatagram(byte[] buf, int size, String addr)
        throws IOException;
}
