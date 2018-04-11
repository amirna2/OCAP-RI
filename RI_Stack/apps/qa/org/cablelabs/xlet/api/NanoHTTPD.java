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

package org.cablelabs.xlet.api;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * A simple, tiny, nicely embeddable HTTP 1.0 server in Java
 * 
 * <p>
 * NanoHTTPD version 1.02, Copyright &copy; 2001,2005 Jarno Elonen
 * (elonen@iki.fi, http://iki.fi/elonen/)
 * 
 * <p>
 * <b>Features & limitations: </b>
 * <ul>
 * 
 * <li>Only one Java file</li>
 * <li>Java 1.1 compatible</li>
 * <li>Released as open source, Modified BSD licence</li>
 * <li>No fixed config files, logging, authorization etc. (Implement yourself if
 * you need them.)</li>
 * <li>Supports parameter parsing of GET and POST methods</li>
 * <li>Supports both dynamic content and file serving</li>
 * <li>Never caches anything</li>
 * <li>Doesn't limit bandwidth, request time or simultaneous connections</li>
 * <li>Default code serves files and shows all HTTP parameters and headers</li>
 * <li>File server supports directory listing, index.html and index.htm</li>
 * <li>File server does the 301 redirection trick for directories without '/'</li>
 * <li>File server supports simple skipping for files (continue download)</li>
 * <li>File server uses current directory as a web root</li>
 * <li>File server serves also very long files without memory overhead</li>
 * <li>Contains a built-in list of most common mime types</li>
 * 
 * </ul>
 * 
 * <p>
 * <b>Ways to use: </b>
 * <ul>
 * 
 * <li>Run as a standalone app, serves files from current directory and shows
 * requests</li>
 * <li>Subclass serve() and embed to your own program</li>
 * <li>Call serveFile() from serve() with your own base directory</li>
 * 
 * </ul>
 * 
 * See the end of the source file for distribution license (Modified BSD
 * licence)
 */
public class NanoHTTPD
{
    // ==================================================
    // API parts
    // ==================================================

    /**
     * Override this to customize the server.
     * <p>
     * 
     * (By default, this delegates to serveFile() and allows directory listing.)
     * 
     * @parm uri Percent-decoded URI without parameters, for example
     *       "/index.cgi"
     * @parm method "GET", "POST" etc.
     * @parm parms Parsed, percent decoded parameters from URI and, in case of
     *       POST, data.
     * @parm header Header entries, percent decoded
     * @return HTTP response, see class Response for details
     */
    public Response serve(String uri, String method, Properties header, Properties parms)
    {
        if (verbose)
        {
            System.out.println(method + " '" + uri + "' ");

            Enumeration e = header.propertyNames();
            while (e.hasMoreElements())
            {
                String value = (String) e.nextElement();
                System.out.println("  HDR: '" + value + "' = '" + header.getProperty(value) + "'");
            }
            e = parms.propertyNames();
            while (e.hasMoreElements())
            {
                String value = (String) e.nextElement();
                System.out.println("  PRM: '" + value + "' = '" + parms.getProperty(value) + "'");
            }
        }

        return serveFile(uri, header, myFileDir, true);
    }

    /**
     * HTTP response. Return one of these from serve().
     */
    public class Response
    {
        /**
         * Default constructor: response = HTTP_OK, data = mime = 'null'
         */
        public Response()
        {
            this.status = HTTP_OK;
        }

        /**
         * Basic constructor.
         */
        public Response(String status, String mimeType, InputStream data)
        {
            this.status = status;
            this.mimeType = mimeType;
            this.data = data;
        }

        /**
         * Convenience method that makes an InputStream out of given text.
         */
        public Response(String status, String mimeType, String txt)
        {
            this.status = status;
            this.mimeType = mimeType;
            this.data = new ByteArrayInputStream(txt.getBytes());
        }

        /**
         * Adds given line to the header.
         */
        public void addHeader(String name, String value)
        {
            header.put(name, value);
        }

        /**
         * HTTP status code after processing, e.g. "200 OK", HTTP_OK
         */
        public String status;

        /**
         * MIME type of content, e.g. "text/html"
         */
        public String mimeType;

        /**
         * Data of the response, may be null.
         */
        public InputStream data;

        /**
         * Headers for the HTTP response. Use addHeader() to add lines.
         */
        public Properties header = new Properties();
    }

    /**
     * Some HTTP response status codes
     */
    public static final String HTTP_OK = "200 OK", HTTP_REDIRECT = "301 Moved Permanently",
            HTTP_FORBIDDEN = "403 Forbidden", HTTP_NOTFOUND = "404 Not Found", HTTP_BADREQUEST = "400 Bad Request",
            HTTP_INTERNALERROR = "500 Internal Server Error", HTTP_NOTIMPLEMENTED = "501 Not Implemented";

    /**
     * Common mime types for dynamic content
     */
    public static final String MIME_PLAINTEXT = "text/plain", MIME_HTML = "text/html",
            MIME_DEFAULT_BINARY = "application/octet-stream";

    // ==================================================
    // Socket & server code
    // ==================================================

    /**
     * Starts a HTTP server to given port.
     * <p>
     * Throws an IOException if the socket is already in use
     */
    public NanoHTTPD(int port, File base) throws IOException
    {
        myTcpPort = port;
        myFileDir = base;

        final ServerSocket ss = new ServerSocket(myTcpPort);
        myThread = new Thread(new Runnable()
        {
            public void run()
            {
                try
                {
                    System.out.println("NanoHTTPD waiting for connections");
                    while (!isShutdown)
                    {
                        new HTTPSession(ss.accept());
                    }
                }
                catch (IOException ioe)
                {
                    ioe.printStackTrace();
                }
            }
        });
        myThread.setDaemon(true);
        System.out.println("NanoHTTPD: starting server thread");
        myThread.start();
    }

    public int getMyPort()
    {
        return myTcpPort;
    }

    public URL getMyURL() throws IOException
    {
        return new URL("http", InetAddress.getLocalHost().getHostAddress(), getMyPort(), "");
    }

    public void shutdown()
    {
        myThread.interrupt();
        isShutdown = true;
    }

    public void setVerbose(boolean verbose)
    {
        this.verbose = verbose;
    }

    /**
     * Starts as a standalone file server and waits for Enter.
     */
    public static void main(String[] args)
    {
        System.out.println("NanoHTTPD 1.02 (C) 2001,2005 Jarno Elonen\n"
                + "(Command line options: [port] [--licence])\n");

        // Show licence if requested
        int lopt = -1;
        for (int i = 0; i < args.length; ++i)
            if (args[i].toLowerCase().endsWith("licence"))
            {
                lopt = i;
                System.out.println(LICENCE + "\n");
            }

        // Change port if requested
        int port = 80;
        if (args.length > 0 && lopt != 0) port = Integer.parseInt(args[0]);

        if (args.length > 1 && args[1].toLowerCase().endsWith("licence")) System.out.println(LICENCE + "\n");

        NanoHTTPD nh = null;
        try
        {
            nh = new NanoHTTPD(port, new File("."));
        }
        catch (IOException ioe)
        {
            System.err.println("Couldn't start server:\n" + ioe);
            System.exit(-1);
        }

        System.out.println("Now serving files in port " + port + " from \"" + new File("").getAbsolutePath() + "\"");
        System.out.println("Hit Enter to stop.\n");

        try
        {
            System.in.read();
        }
        catch (Throwable t)
        { /* empty */
        }
    }

    /**
     * Handles one session, i.e. parses the HTTP request and returns the
     * response.
     */
    private class HTTPSession implements Runnable
    {
        public HTTPSession(Socket s)
        {
            mySocket = s;
            Thread t = new Thread(this);
            t.setDaemon(true);
            t.start();
        }

        public void run()
        {
            try
            {
                InputStream is = mySocket.getInputStream();
                if (is == null) return;
                BufferedReader in = new BufferedReader(new InputStreamReader(is));

                // Read the request line
                String input = in.readLine();
                System.out.println("NanoHTTPD: Request received - " + input);
                StringTokenizer st = new StringTokenizer(input);
                if (!st.hasMoreTokens())
                {
                    System.out.println("NanoHTTPD: Bad Request");
                    sendError(HTTP_BADREQUEST, "BAD REQUEST: Syntax error. Usage: GET /example/file.html");
                }

                String method = st.nextToken();

                if (!st.hasMoreTokens())
                {
                    System.out.println("NanoHTTPD: Bad Request");
                    sendError(HTTP_BADREQUEST, "BAD REQUEST: Missing URI. Usage: GET /example/file.html");
                }

                String uri = decodePercent(st.nextToken());

                // Decode parameters from the URI
                Properties parms = new Properties();
                int qmi = uri.indexOf('?');
                if (qmi >= 0)
                {
                    decodeParms(uri.substring(qmi + 1), parms);
                    uri = decodePercent(uri.substring(0, qmi));
                }

                // If there's another token, it's protocol version,
                // followed by HTTP headers. Ignore version but parse headers.
                Properties header = new Properties();
                if (st.hasMoreTokens())
                {
                    String line = in.readLine();
                    while (line.trim().length() > 0)
                    {
                        int p = line.indexOf(':');
                        header.put(line.substring(0, p).trim(), line.substring(p + 1).trim());
                        line = in.readLine();
                    }
                }

                // If the method is POST, there may be parameters
                // in data section, too, read another line:
                if (method.equalsIgnoreCase("POST")) decodeParms(in.readLine(), parms);

                // Ok, now do the serve()
                Response r = serve(uri, method, header, parms);
                if (r == null)
                {
                    System.out.println("NanoHTTPD: Internal Error, null response");
                    sendError(HTTP_INTERNALERROR, "SERVER INTERNAL ERROR: Serve() returned a null response.");
                }
                else
                {
                    System.out.println("NanoHTTPD: Sending Response " + r.status);
                    sendResponse(r.status, r.mimeType, r.header, r.data);
                }

                in.close();
            }
            catch (IOException ioe)
            {
                try
                {
                    sendError(HTTP_INTERNALERROR, "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage());
                }
                catch (Throwable t)
                { /* empty */
                }
            }
            catch (InterruptedException ie)
            {
                // Thrown by sendError, ignore and exit the thread.
            }
            finally
            {
                try
                {
                    mySocket.close();
                }
                catch (IOException e)
                {
                    /* Ignore */
                }
            }
        }

        /**
         * Decodes the percent encoding scheme. <br/>
         * For example: "an+example%20string" -> "an example string"
         */
        private String decodePercent(String str) throws InterruptedException
        {
            try
            {
                StringBuffer sb = new StringBuffer();
                for (int i = 0; i < str.length(); i++)
                {
                    char c = str.charAt(i);
                    switch (c)
                    {
                        case '+':
                            sb.append(' ');
                            break;
                        case '%':
                            sb.append((char) Integer.parseInt(str.substring(i + 1, i + 3), 16));
                            i += 2;
                            break;
                        default:
                            sb.append(c);
                            break;
                    }
                }
                return new String(sb.toString().getBytes());
            }
            catch (Exception e)
            {
                sendError(HTTP_BADREQUEST, "BAD REQUEST: Bad percent-encoding.");
                return null;
            }
        }

        /**
         * Decodes parameters in percent-encoded URI-format ( e.g.
         * "name=Jack%20Daniels&pass=Single%20Malt" ) and adds them to given
         * Properties.
         */
        private void decodeParms(String parms, Properties p) throws InterruptedException
        {
            if (parms == null) return;

            StringTokenizer st = new StringTokenizer(parms, "&");
            while (st.hasMoreTokens())
            {
                String e = st.nextToken();
                int sep = e.indexOf('=');
                if (sep >= 0) p.put(decodePercent(e.substring(0, sep)).trim(), decodePercent(e.substring(sep + 1)));
            }
        }

        /**
         * Returns an error message as a HTTP response and throws
         * InterruptedException to stop furhter request processing.
         */
        private void sendError(String status, String msg) throws InterruptedException
        {
            sendResponse(status, MIME_PLAINTEXT, null, new ByteArrayInputStream(msg.getBytes()));
            throw new InterruptedException();
        }

        /**
         * Sends given response to the socket.
         */
        private void sendResponse(String status, String mime, Properties header, InputStream data)
        {
            try
            {
                if (status == null) throw new Error("sendResponse(): Status can't be null.");

                OutputStream out = mySocket.getOutputStream();
                PrintWriter pw = new PrintWriter(out);
                pw.print("HTTP/1.0 " + status + " \r\n");

                if (mime != null) pw.print("Content-Type: " + mime + "\r\n");

                // if ( header == null || header.getProperty( "Date" ) == null )
                // pw.print( "Date: " + gmtFrmt.format( new Date()) + "\r\n");

                if (header != null)
                {
                    Enumeration e = header.keys();
                    while (e.hasMoreElements())
                    {
                        String key = (String) e.nextElement();
                        String value = header.getProperty(key);
                        pw.print(key + ": " + value + "\r\n");
                    }
                }

                pw.print("\r\n");
                pw.flush();

                if (data != null)
                {
                    byte[] buff = new byte[2048];
                    while (true)
                    {
                        int read = data.read(buff, 0, 2048);
                        if (read <= 0) break;
                        out.write(buff, 0, read);
                    }
                }
                out.flush();
                out.close();
                if (data != null) data.close();
            }
            catch (IOException ioe)
            {
                // Couldn't write? No can do.
                try
                {
                    mySocket.close();
                }
                catch (Throwable t)
                { /* empty */
                }
            }
        }

        private Socket mySocket;
        // private BufferedReader myIn;
    }

    /**
     * URL-encodes everything between "/"-characters. Encodes spaces as '%20'
     * instead of '+'.
     */
    private String encodeUri(String uri)
    {
        String newUri = "";
        StringTokenizer st = new StringTokenizer(uri, "/ ", true);
        while (st.hasMoreTokens())
        {
            String tok = st.nextToken();
            if (tok.equals("/"))
                newUri += "/";
            else if (tok.equals(" "))
                newUri += "%20";
            else
                newUri += URLEncoder.encode(tok);
        }
        return newUri;
    }

    private int myTcpPort;

    private File myFileDir;

    private Thread myThread;

    private boolean isShutdown;

    protected boolean verbose = true;

    // ==================================================
    // File server code
    // ==================================================

    /**
     * Serves file from homeDir and its' subdirectories (only). Uses only URI,
     * ignores all headers and HTTP parameters.
     */
    public Response serveFile(String uri, Properties header, File homeDir, boolean allowDirectoryListing)
    {
        // Make sure we won't die of an exception later
        if (!homeDir.isDirectory())
            return new Response(HTTP_INTERNALERROR, MIME_PLAINTEXT,
                    "INTERNAL ERRROR: serveFile(): given homeDir is not a directory.");

        // Remove URL arguments
        uri = uri.trim().replace(File.separatorChar, '/');
        if (uri.indexOf('?') >= 0) uri = uri.substring(0, uri.indexOf('?'));

        // Prohibit getting out of current directory
        if (uri.startsWith("..") || uri.endsWith("..") || uri.indexOf("../") >= 0)
            return new Response(HTTP_FORBIDDEN, MIME_PLAINTEXT, "FORBIDDEN: Won't serve ../ for security reasons.");

        File f = new File(homeDir, uri);
        if (!f.exists())
        {
            boolean hashfile;

            // String filename = uri.trim().replace('/',File.separatorChar);
            String filename = uri.trim();
            String directory = filename.substring(0, filename.lastIndexOf('/'));
            File file = new File(myFileDir, uri.trim().replace('/', File.separatorChar));
            System.out.println("Generating for " + directory);
            File dir = new File(myFileDir, directory);
            // Fake hashfile/.dir (if one didn't exist)
            if ((hashfile = uri.endsWith("hashfile")) || uri.endsWith(".dir"))
            {
                try
                {

                    byte[] hash = hashfile ? hashfile(dir) : dotdir(dir);

                    Response r2 = new Response(HTTP_OK, MIME_DEFAULT_BINARY, new ByteArrayInputStream(hash));
                    r2.addHeader("Content-length", "" + hash.length);
                    return r2;
                }
                catch (IOException ioe)
                {
                    ioe.printStackTrace();
                    return new Response(HTTP_FORBIDDEN, MIME_PLAINTEXT, "FORBIDDEN: Generating hash or .dir failed.");
                }
            }
            else
            {
                return new Response(HTTP_NOTFOUND, MIME_PLAINTEXT, "Error 404, file not found.");
            }
        }

        // List the directory, if necessary
        if (f.isDirectory())
        {
            // Browsers get confused without '/' after the
            // directory, send a redirect.
            if (!uri.endsWith("/"))
            {
                uri += "/";
                Response r = new Response(HTTP_REDIRECT, MIME_HTML, "<html><body>Redirected: <a href=\"" + uri + "\">"
                        + uri + "</a></body></html>");
                r.addHeader("Location", uri);
                return r;
            }

            // First try index.html and index.htm
            if (new File(f, "index.html").exists())
                f = new File(homeDir, uri + "/index.html");
            else if (new File(f, "index.htm").exists())
                f = new File(homeDir, uri + "/index.htm");

            // No index file, list the directory
            else if (allowDirectoryListing)
            {
                String[] files = f.list();
                String msg = "<html><body><h1>Directory " + uri + "</h1><br/>";

                if (uri.length() > 1)
                {
                    String u = uri.substring(0, uri.length() - 1);
                    int slash = u.lastIndexOf('/');
                    if (slash >= 0 && slash < u.length())
                        msg += "<b><a href=\"" + uri.substring(0, slash + 1) + "\">..</a></b><br/>";
                }

                for (int i = 0; i < files.length; ++i)
                {
                    File curFile = new File(f, files[i]);
                    boolean dir = curFile.isDirectory();
                    if (dir)
                    {
                        msg += "<b>";
                        files[i] += "/";
                    }

                    msg += "<a href=\"" + encodeUri(uri + files[i]) + "\">" + files[i] + "</a>";

                    // Show file size
                    if (curFile.isFile())
                    {
                        long len = curFile.length();
                        msg += " &nbsp;<font size=2>(";
                        if (len < 1024)
                            msg += curFile.length() + " bytes";
                        else if (len < 1024 * 1024)
                            msg += curFile.length() / 1024 + "." + (curFile.length() % 1024 / 10 % 100) + " KB";
                        else
                            msg += curFile.length() / (1024 * 1024) + "." + curFile.length() % (1024 * 1024) / 10 % 100
                                    + " MB";

                        msg += ")</font>";
                    }
                    msg += "<br/>";
                    if (dir) msg += "</b>";
                }
                return new Response(HTTP_OK, MIME_HTML, msg);
            }
            else
            {
                return new Response(HTTP_FORBIDDEN, MIME_PLAINTEXT, "FORBIDDEN: No directory listing.");
            }
        }

        // Get MIME type from file name extension, if possible
        String mime = null;
        int dot = uri.lastIndexOf('.');
        if (dot >= 0) mime = (String) theMimeTypes.get(uri.substring(dot + 1).toLowerCase());
        if (mime == null) mime = MIME_DEFAULT_BINARY;

        try
        {
            // Support (simple) skipping:
            long startFrom = 0;
            String range = header.getProperty("Range");
            if (range != null)
            {
                if (range.startsWith("bytes="))
                {
                    range = range.substring("bytes=".length());
                    int minus = range.indexOf('-');
                    if (minus > 0) range = range.substring(0, minus);
                    try
                    {
                        startFrom = Long.parseLong(range);
                    }
                    catch (NumberFormatException nfe)
                    { /* empty */
                    }
                }
            }

            FileInputStream fis = new FileInputStream(f);
            fis.skip(startFrom);
            Response r = new Response(HTTP_OK, mime, fis);
            r.addHeader("Content-length", "" + (f.length() - startFrom));
            r.addHeader("Content-range", "" + startFrom + "-" + (f.length() - 1) + "/" + f.length());
            return r;
        }
        catch (IOException ioe)
        {
            return new Response(HTTP_FORBIDDEN, MIME_PLAINTEXT, "FORBIDDEN: Reading file failed.");
        }
    }

    /**
     * <pre>
     * Hashfile () {
     *   digest_count 16 uimsbf
     *   for( i=0; i<digest_count; i++ ) {
     *     digest_type 8 uimsbf
     *     name_count 16 uimsbf
     *     for( j=0; j<name_count; j++ ) {
     *       name_length 8 uimsbf
     *       for( k=0; k<name_length; k++ ) {
     *         name_byte 8 bslbf
     *       }
     *     }
     *     for( j=0; j<digest_length; j++ ) {
     *       digest_byte 8 bslbf
     *     }
     *   }
     * }
     * </pre>
     * 
     * @param dir
     * @return byte array containing hashfile
     */
    private static byte[] hashfile(File dir) throws IOException
    {
        final boolean DEBUG = false;
        String[] files = dir.list();
        if (files == null) throw new IOException("Cannot access " + dir);

        if (DEBUG) System.out.println("Generating hashfile: " + dir);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream hashfile = new DataOutputStream(bos);

        // digest_count 16 uimsbf
        hashfile.writeShort((short) files.length);
        for (int i = 0; i < files.length; ++i)
        {
            if (DEBUG) System.out.println("\t" + files[i]);

            // digest_type 8 uimsbf
            hashfile.writeByte((byte) 0);

            // name_count 16 uimsbf
            hashfile.writeShort((short) 1);

            // name_length 8 uimsbf
            byte[] name = files[i].getBytes();
            hashfile.writeByte((byte) name.length);
            // name_byte 8 bslbf
            hashfile.write(name);
        }
        hashfile.flush();

        return bos.toByteArray();
    }

    private static byte[] dotdir(File dir) throws IOException
    {
        final boolean DEBUG = true;
        String[] files = dir.list();
        if (!dir.isDirectory() || files == null) throw new IOException("Cannot access " + dir);

        if (DEBUG) System.out.println("Generating .dir: " + dir);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PrintStream dotdir = new PrintStream(bos);

        for (int i = 0; i < files.length; ++i)
        {
            if (DEBUG) System.out.println("\t" + files[i]);
            dotdir.println(files[i]);
        }
        dotdir.flush();

        return bos.toByteArray();
    }

    /**
     * Hashtable mapping (String)FILENAME_EXTENSION -> (String)MIME_TYPE
     */
    private static Hashtable theMimeTypes = new Hashtable();
    static
    {
        StringTokenizer st = new StringTokenizer("htm        text/html " + "html       text/html "
                + "txt        text/plain " + "asc        text/plain " + "gif        image/gif "
                + "jpg        image/jpeg " + "jpeg       image/jpeg " + "png        image/png "
                + "mp3        audio/mpeg " + "m3u        audio/mpeg-url " + "pdf        application/pdf "
                + "doc        application/msword " + "ogg        application/x-ogg "
                + "zip        application/octet-stream " + "exe        application/octet-stream "
                + "class      application/octet-stream ");
        while (st.hasMoreTokens())
            theMimeTypes.put(st.nextToken(), st.nextToken());
    }

    // /**
    // * GMT date formatter
    // */
    // private static java.text.SimpleDateFormat gmtFrmt;
    // static
    // {
    // gmtFrmt = new java.text.SimpleDateFormat( "E, d MMM yyyy HH:mm:ss 'GMT'",
    // Locale.US);
    // gmtFrmt.setTimeZone(TimeZone.getTimeZone("GMT"));
    // }

    /**
     * The distribution licence
     */
    private static final String LICENCE = "Copyright (C) 2001,2005 by Jarno Elonen <elonen@iki.fi>\n" + "\n"
            + "Redistribution and use in source and binary forms, with or without\n"
            + "modification, are permitted provided that the following conditions\n" + "are met:\n" + "\n"
            + "Redistributions of source code must retain the above copyright notice,\n"
            + "this list of conditions and the following disclaimer. Redistributions in\n"
            + "binary form must reproduce the above copyright notice, this list of\n"
            + "conditions and the following disclaimer in the documentation and/or other\n"
            + "materials provided with the distribution. The name of the author may not\n"
            + "be used to endorse or promote products derived from this software without\n"
            + "specific prior written permission. \n" + " \n"
            + "THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR\n"
            + "IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES\n"
            + "OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.\n"
            + "IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,\n"
            + "INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT\n"
            + "NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,\n"
            + "DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY\n"
            + "THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT\n"
            + "(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE\n"
            + "OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.";
}
