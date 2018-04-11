/*Copyright (c) 2004-2006, Satoshi Konno Copyright (c) 2005-2006,
Nokia Corporation Copyright (c) 2005-2006, Theo Beisch Collectively
the Copyright Owners All rights reserved
 */
package org.cablelabs.impl.ocap.hn.upnp.cds;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.StringTokenizer;
import org.apache.xerces.utils.regex.RegularExpression;
import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;

import org.dvb.application.AppID;
import org.ocap.storage.ExtendedFileAccessPermissions;

/**
 * set utilities used by home networking
 *
 * @author Michael Jastad
 * @version $Revision$
 *
 * @see
 */
public class Utils
{
    public static final String HEX_CHARS = "0123456789abcdef";
    
    private static final Logger log = Logger.getLogger(Utils.class);

    /**
     * used to determine if a string value is a digit.
     *
     * @param The
     *            string value to be verified.
     *
     * @return True if the string value represents a digit.
     */
    public static boolean isDigit(String arg)
    {
        boolean status = true;
        char[] charArray = null;

        if (arg != null)
        {
            charArray = arg.toCharArray();
        }

        if (charArray != null)
        {
            for (int i = 0; i < charArray.length; ++i)
            {
                if (Character.isDigit(charArray[i]))
                {
                    continue;
                }
                else
                {
                    status = false;

                    break;
                }
            }
        }

        return status;
    }

    /**
     * converts a string to an int
     *
     * @param s
     *            String to be converted
     *
     * @return int
     */
    public static int toInt(String s)
    {
        int ival = -1;

        if (isDigit(s))
        {
            ival = new Integer(s).intValue();
        }

        return ival;
    }

    /**
     * converts a string to a long
     *
     * @param s
     *            String to be converted
     *
     * @return long value
     */
    public static long toLong(String s)
    {
        long lval = -1;

        if (isDigit(s))
        {
            lval = new Long(s).longValue();
        }

        return lval;
    }

    /**
     * Reads an InputStream into a String
     * @param is
     * @return String read from InputStream
     * @throws IOException
     */
    public static String toString(InputStream is) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
        byte[] buffer = new byte[1024];
        int i;
        while ((i = is.read(buffer)) != -1)
        {
            baos.write(buffer, 0, i);
        }
        byte[] response = baos.toByteArray();

        return new String(response);
    }

    /**
     * Splits a string across a specified key.
     *
     * @param s
     *            String to be split
     * @param k
     *            Key used a a token to parse
     *
     * @return String[]
     */
    public static String[] split(String s, String key)
    {
        Vector strings = new Vector();

        if ((s != null) && (key != null))
        {
            int idx = 0;
            while (s.length() > 0)
            {

                idx = s.indexOf(key);

                if (idx > 0)
                {
                    strings.add(s.substring(0, idx));
                    s = s.substring(idx + 1, s.length());
                }
                else
                {
                    strings.add(s.substring(0, s.length()));
                    s = "";
                }
            }
        }

        return (String[]) strings.toArray(new String[strings.size()]);
    }

    /**
     * Splits a string that contains XMLFragments. This method is added to
     * overcome to the deficiency of Utils.split(s,key) where the method fails
     * to escape "," in the xml fragment value. e.g. <dc:title>Planes, Trains &
     * Automobiles</dc:title>,<dc:date>Bad date</dc:date> must be split as 2 xml
     * fragments but Utils.split splits it into three items.
     *
     * @param s
     *            String to be split
     * @param key
     *            Key used a a token to parse the xml string
     * @param escapeString
     *            escapeString used as a token to look for at the start of each
     *            split items
     * @return String[]
     */
    public static String[] splitXMLFragment(String s, String key, String escapeString)
    {
        // If the string is empty then send an array with only one empty value.
        if (s.trim().length() == 0)
        {
            return new String[] { "" };
        }
        // This splits the complete string based on key.
        String[] intialSplitResult = split(s, key);
        Vector finalSplitList = new Vector();
        try
        {
            for (int i = 0; i < intialSplitResult.length; i++)
            {
                // Checks if each of the split items starts with escapeString
                // value. If it does start with escapeString or if it is an
                // empty value then simply add the split items to the Vector
                // else combines the value with the previous split value.
                if (intialSplitResult[i].startsWith(escapeString) || intialSplitResult[i].trim().length() == 0)
                {
                    finalSplitList.add(intialSplitResult[i]);
                }
                else
                {
                    // Concat previous and the current items into one and
                    // Include "," back between the split string.
                    finalSplitList.set(i - 1, (intialSplitResult[i - 1].concat(",")).concat(intialSplitResult[i]));
                }
            }
        }
        catch (Exception e)
        {
            if (log.isErrorEnabled())
            {
                log.error("Problems parsing string " + s + ", with token " + key + " and escapeString " + escapeString);
            }
            return null;
        }
        return (String[]) finalSplitList.toArray(new String[finalSplitList.size()]);
    }

    /**
     * DOCUMENT ME!
     *
     * @param value
     *            DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public final static boolean hasData(String value)
    {
        if (value == null)
        {
            return false;
        }

        if (value.length() <= 0)
        {
            return false;
        }

        return true;
    }

    /**
     * DOCUMENT ME!
     *
     * @param str
     *            DOCUMENT ME!
     * @param chars
     *            DOCUMENT ME!
     * @param startIdx
     *            DOCUMENT ME!
     * @param endIdx
     *            DOCUMENT ME!
     * @param offset
     *            DOCUMENT ME!
     * @param isEqual
     *            DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public final static int findOf(String str, String chars, int startIdx, int endIdx, int offset, boolean isEqual)
    {
        if (offset == 0)
        {
            return -1;
        }

        int charCnt = chars.length();
        int idx = startIdx;

        while (true)
        {
            if (0 < offset)
            {
                if (endIdx < idx)
                {
                    break;
                }
            }
            else
            {
                if (idx < endIdx)
                {
                    break;
                }
            }

            char strc = str.charAt(idx);
            int noEqualCnt = 0;

            for (int n = 0; n < charCnt; n++)
            {
                char charc = chars.charAt(n);

                if (isEqual == true)
                {
                    if (strc == charc)
                    {
                        return idx;
                    }
                }
                else
                {
                    if (strc != charc)
                    {
                        noEqualCnt++;
                    }

                    if (noEqualCnt == charCnt)
                    {
                        return idx;
                    }
                }
            }

            idx += offset;
        }

        return -1;
    }

    /**
     * DOCUMENT ME!
     *
     * @param str
     *            DOCUMENT ME!
     * @param chars
     *            DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public final static int findFirstOf(String str, String chars)
    {
        return findOf(str, chars, 0, (str.length() - 1), 1, true);
    }

    /**
     * DOCUMENT ME!
     *
     * @param str
     *            DOCUMENT ME!
     * @param chars
     *            DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public final static int findFirstNotOf(String str, String chars)
    {
        return findOf(str, chars, 0, (str.length() - 1), 1, false);
    }

    /**
     * DOCUMENT ME!
     *
     * @param str
     *            DOCUMENT ME!
     * @param chars
     *            DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public final static int findLastOf(String str, String chars)
    {
        return findOf(str, chars, (str.length() - 1), 0, -1, true);
    }

    /**
     * DOCUMENT ME!
     *
     * @param str
     *            DOCUMENT ME!
     * @param chars
     *            DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public final static int findLastNotOf(String str, String chars)
    {
        return findOf(str, chars, (str.length() - 1), 0, -1, false);
    }

    /**
     * DOCUMENT ME!
     *
     * @param trimStr
     *            DOCUMENT ME!
     * @param trimChars
     *            DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public final static String trim(String trimStr, String trimChars)
    {
        int spIdx = findFirstNotOf(trimStr, trimChars);

        if (spIdx < 0)
        {
            String buf = trimStr;

            return buf;
        }

        String trimStr2 = trimStr.substring(spIdx, trimStr.length());
        spIdx = findLastNotOf(trimStr2, trimChars);

        if (spIdx < 0)
        {
            String buf = trimStr2;

            return buf;
        }

        String buf = trimStr2.substring(0, spIdx + 1);

        return buf;
    }

    /**
     * Formats a unix timestamp to a string compliant with ISO 8601. ex.
     * 2004-01-23T12:15:23
     *
     * @param time in unix time
     *
     * @return ISO8601 formatted date / time
     */
    public final static String formatDateISO8601(long time)
    {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        return format.format(new Date(time));
    }


    public final static Date fromDateISO8601(String dateStr)
    {
        // At this point, the String dateStr has already been validated, so
        // we can assume it's a valid ISO8601 date, time, or dateTime

        Date date = null;

        // ISO8601 dates can be in the following formats: YYYYMMDD,YYYY-MM-DD, YYYY-MM,
        // YYYY, YY, YYYYJJJ, YYYY-JJJ, YYYYWwwD, YYYY-Www-D, YYYY-Www, and YYYYWww
        String[] dateRegexPatterns = getDateRegexPatterns();

        // Parse date values
        RegularExpression regEx = new RegularExpression(dateRegexPatterns[0]);
        for (int i = 0; i < dateRegexPatterns.length; i++)
        {
            regEx.setPattern(dateRegexPatterns[i]);
            if (regEx.matches(dateStr))
            {
                return parseISO8601Date(dateStr, i);
            }
        }

        //Parse dateTime values
        String[] dateTimeRegexPatterns = getDateTimeRegexPatterns();
        for (int i = 0; i < dateTimeRegexPatterns.length; i++)
        {
            regEx.setPattern(dateTimeRegexPatterns[i]);
            if (regEx.matches(dateStr))
            {
                return parseISO8601DateTime(dateStr);
            }
        }
        return date;
    }


    /**
     * Formats a unix timestamp to a string compliant UPnP
     * res@duration format ex. 01:23:45.097
     *
     * Format is defined as H+:MM:SS[.F+] which in plain english is
     * (1 or more hour digits):(exactly 2 minute digits):(exactly 2
     * seconds digits) optionally followed by decimal point and any
     * number of digits (including none) to indicate fractional
     * seconds. The decimal point MUST be omitted if there are no
     * fractional seconds.
     *
     * There is also an optional to express the fractional seconds
     * as .F0/F1 (for example .1/2).
     *
     * This implementation chooses to represent the fraction as 3
     * digit decimal (if non-zero).
     *
     * @param time
     *            in unix time
     * @return UPnP res@duration formatted duration
     */
    public final static String formatDateResDuration(long time)
    {
        long totalsecs = (time / 1000);
        long totalmins = (totalsecs / 60);
        long hours = (totalmins / 60);

        long mins = totalmins - (hours * 60);
        long secs = totalsecs - (hours * 3600) - (mins * 60);
        long millisecs = time % 1000;

        StringBuffer sb = new StringBuffer().append(hours).append(':').append((int)(mins/10)).append(mins%10).append(':').append((int)(secs/10)).append(secs%10);
        if (millisecs != 0)
        {
            sb.append(".");
            if (millisecs < 10)
            {
                sb.append("00").append(millisecs);
            }
            else if (millisecs < 100)
            {
                sb.append("0").append(millisecs);
            }
            else
            {
                sb.append(millisecs);
            }
        }
        return sb.toString();
    }

    /**
     * Formats a string compliant with UPnP res@duration format ex.
     * 01:23:45.097 or 01:23:45.2/3 into milliseconds
     *
     * Format is defined as H+:MM:SS[.F+] which in plain english is
     * (1 or more hour digits):(exactly 2 minute digits):(exactly 2
     * seconds digits) optionally followed by decimal point and any
     * number of digits (including none) to indicate fractional
     * seconds. The decimal point MUST be omitted if there are no
     * fractional seconds.
     *
     * There is also an optional to express the fractional seconds
     * as .F0/F1 (for example .1/2).
     *
     * This implementation chooses to represent the fraction as 3
     * digit decimal (if non-zero).
     *
     * @param resDuration string duration according to formats
     *            above
     * @return long duration in milliseconds, -1 if unable to parse
     */
    public final static long parseDateResDuration(String resDuration)
    {
        long duration = -1;
        long hours = -1;
        long minutes = -1;
        long seconds = -1;
        long millis = 0;

        try
        {
            //findbugs detected - use equals rather than == when comparing strings.
            if ((resDuration != null) && (!resDuration.equals("")))
            {
                int firstColon = resDuration.indexOf(":",0);
                if (firstColon != -1)
                {
                    int secondColon = resDuration.indexOf(":",firstColon+1);
                    if (secondColon != -1)
                    {
                        int decimal = resDuration.indexOf(".", secondColon+1);

                        hours = Integer.parseInt(resDuration.substring(0,firstColon),10);
                        minutes = Integer.parseInt(resDuration.substring(firstColon+1,secondColon),10);
                        if (decimal != -1)
                        {
                            seconds = Integer.parseInt(resDuration.substring(secondColon+1, decimal),10);
                            String frac = resDuration.substring(decimal+1);
                            int divisor = frac.indexOf("/");
                            if (divisor != -1)
                            {
                                int f0 = Integer.parseInt(frac.substring(0,divisor),10);
                                int f1 = Integer.parseInt(frac.substring(divisor+1),10);

                                millis = (int)(f0 * 1000) / f1;
                            }
                            else
                            {
                                /* Convert the first three digits of the fractional to ms */
                                int fracDigits = frac.length();
                                int mult = 100;
                                for (int i = 0; ((i < 3) && (i < fracDigits)); i++)
                                {
                                    millis += ((int)(frac.charAt(i) - '0'))* mult;
                                    mult /= 10;
                                }
                            } /* endif divisor-based fractional seconds */


                        }
                        else
                        {
                            seconds = Integer.parseInt(resDuration.substring(secondColon+1),10);

                        } /* endif fractional seconds sent */

                        if ((hours != -1) && (minutes != -1) && (seconds != -1))
                        {
                            duration = millis + (seconds * 1000) + (minutes * 60000) + (hours * 3600000);
                        }

                    }/* endif second colon present */
                } /* endif first colon present */
            } /* endif passed string not NULL and not empty */

        } catch (Exception e)
        {
        }
        return duration;
    }

    /**
     * Formats a unix timestamp to a string compliant with UPnP srs
     * duration format ex. P1D12:42:54
     *
     * @param time
     *            in unix time
     * @return UPnP res@duration formatted duration
     */
    public final static String formatDateScheduledDuration(long time)
    {
        long totalsecs = (time / 1000);
        long totalmins = (totalsecs / 60);
        long totalhours = (totalmins / 60);
        long days = (totalhours / 24);

        long hours = totalhours - (days * 24);
        long mins = totalmins - (totalhours * 60);
        long secs = totalsecs - (totalmins * 60);

        StringBuffer sb = new StringBuffer().append("P");
        if (days != 0)
        {
            sb.append(days % 10).append("D");
        }
        sb.append((int)(hours/10)).append((int)(hours%10)).append(':');
        sb.append((int)(mins/10)).append(mins%10).append(':');
        sb.append((int)(secs/10)).append(secs%10);

        return sb.toString();
    }
    /**
     * Parses a string compliant with the UPnP SRS scheduledDuration
     * ex. P1D12:42:54 into a number of milliseconds
     *
     * @param srsDuration string duration according to formats
     *            above
     * @return long duration in milliseconds, -1 if unable to parse
     */
    public final static long parseDateScheduledDuration(String srsDuration)
    {
        long duration = -1;
        long days = 0;
        long hours = -1;
        long minutes = -1;
        long seconds = -1;
        int timeOffset = 1;

        try
        {
            //findbugs detected - use equals rather than == when comparing strings.
            if ((srsDuration != null) && (!srsDuration.equals("")) && (srsDuration.charAt(0) == 'P'))
            {
                if (srsDuration.length() >= 11)
                {
                    int dOffset = srsDuration.indexOf("D",0);
                    if (dOffset != -1)
                    {
                        days = Integer.parseInt(srsDuration.substring(1,dOffset),10);
                        timeOffset = dOffset+1;
                    }
                }

                int firstColon = srsDuration.indexOf(":",timeOffset);
                if (firstColon != -1)
                {
                    int secondColon = srsDuration.indexOf(":",firstColon+1);
                    if (secondColon != -1)
                    {
                        hours = Integer.parseInt(srsDuration.substring(timeOffset,firstColon),10);
                        minutes = Integer.parseInt(srsDuration.substring(firstColon+1,secondColon),10);
                        seconds = Integer.parseInt(srsDuration.substring(secondColon+1),10);

                        if ((hours != -1) && (minutes != -1) && (seconds != -1))
                        {
                            duration = (seconds * 1000)  +
                            (minutes * 60000) +
                            (hours * 3600000) +
                            (days * 86400000);
                        }

                    }/* endif second colon present */
                } /* endif first colon present */
            } /* endif passed string not NULL and not empty */

        } catch (Exception e)
        {
        }
        return duration;
    }

    /**
     * Returns the AppID of the current context
     * @return appID of the current context
     */
    public static AppID getCallerAppID()
    {
        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

        if (ccm == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Utils.getCallerAppID: can't find CallerContextManager.");
            }

            return null;
        }

        return (AppID) (ccm.getCurrentContext().get(CallerContext.APP_ID));
    }

    /**
     * Converts a valid EFAP to a formatted CSV string. Valid formats are
     * defined by the HN-OC HPN 2.0 specification.
     *
     * @param efap The EFAP; may not be null.
     *
     * @return String A CSV representing the ExtendedFileAccessPermissions.
     */
    public static String toCSV(ExtendedFileAccessPermissions efap)
    {
        assert efap != null;

        StringBuffer sb = new StringBuffer();
        // ///////////// WORLD /////////////////
        sb.append("w=,");
        sb.append(efap.hasReadWorldAccessRight() ? "1," : "0,");
        sb.append(efap.hasWriteWorldAccessRight() ? "1," : "0,");

        // /////////// ORGANIZATION ///////////////
        sb.append("o=0x0,");
        sb.append(efap.hasReadOrganisationAccessRight() ? "1," : "0,");
        sb.append(efap.hasWriteOrganisationAccessRight() ? "1," : "0,");

        // /////////// APPLICATION ////////////////
        sb.append("a=0x0,");
        sb.append(efap.hasReadApplicationAccessRight() ? "1," : "0,");
        sb.append(efap.hasWriteApplicationAccessRight() ? "1" : "0");

        /*
         * Need to collect other organizations along with their rights so
         * they can be serialized into a string of proper format. r=<hex
         * id>,r,w where r/w are 0 or 1
         *
         * Building a map of Integer -> boolean[2] where boolean[0] = read
         * boolean[1] = write
         */

        Map otherOrgs = new HashMap();
        int[] readOrgs = efap.getReadAccessOrganizationIds();
        int[] writeOrgs = efap.getWriteAccessOrganizationIds();
        if (readOrgs != null)
        {
            for (int i = 0; i < readOrgs.length; i++)
            {
                Integer key = new Integer(readOrgs[i]);
                boolean[] rights = (boolean[]) otherOrgs.get(key);
                if (rights == null)
                {
                    rights = new boolean[2];
                    otherOrgs.put(key, rights);
                }
                rights[0] = true;
            }
        }

        if (writeOrgs != null)
        {
            for (int i = 0; i < writeOrgs.length; i++)
            {
                Integer key = new Integer(writeOrgs[i]);
                boolean[] rights = (boolean[]) otherOrgs.get(key);
                if (rights == null)
                {
                    rights = new boolean[2];
                    otherOrgs.put(key, rights);
                }
                rights[1] = true;
            }
        }

        /*
         * This is where we serialize out the other organization ids and
         * rights
         */
        Iterator i = otherOrgs.keySet().iterator();
        while (i.hasNext())
        {
            Integer key = (Integer) i.next();
            String hexId = Integer.toHexString(key.intValue());
            sb.append(",r=0x").append(hexId).append(",");
            sb.append(((boolean[]) otherOrgs.get(key))[0] ? "1," : "0,");
            sb.append(((boolean[]) otherOrgs.get(key))[1] ? "1" : "0");
        }

        return sb.toString();
    }

    /**
     * Converts a valid formatted CSV string to an EFAP. Valid formats are
     * defined by the HN-OC HPN 2.0 specification.
     *
     * @param efapCSV The string; must not be null.
     *
     * @return ExtendedFileAccessPermissions based on the content of the CSV.
     */
    public static ExtendedFileAccessPermissions toEfap(String efapCSV)
    {
        assert efapCSV != null;

        ExtendedFileAccessPermissions eFap;

        String[] permissions = split(efapCSV, ",");

        if ((permissions != null) && (permissions.length >= 9))
        {
            boolean wRead =  permissions[1].equals("1"); // world read
            boolean wWrite = permissions[2].equals("1"); // world write
            boolean oRead =  permissions[4].equals("1"); // organization read
            boolean oWrite = permissions[5].equals("1"); // organization write
            boolean aRead =  permissions[7].equals("1"); // application read
            boolean aWrite = permissions[8].equals("1"); // application write

            // Parse out the other organization ids
            List readOrgs = new ArrayList();
            List writeOrgs = new ArrayList();

            for (int i = 9; i < permissions.length;)
            {
                if (permissions.length >= (i + 2)) // check parsed string has enough elements
                {
                    // Careful, i being incremented throughout the loop
                    String hexString = permissions[i].substring(permissions[i++].indexOf("=") + 1);
                    Integer id = Integer.decode(hexString);

                    if (permissions[i++].equals("1"))
                    {
                        readOrgs.add(id);
                    }
                    if (permissions[i++].equals("1"))
                    {
                        writeOrgs.add(id);
                    }
                }
            }

            // ExtendedFileAccessPermissions wants an int[]
            int[] readOrgInts = readOrgs.size() > 0 ? new int[readOrgs.size()] : null;
            int[] writeOrgInts = writeOrgs.size() > 0 ? new int[writeOrgs.size()] : null;

            for (int i = 0; i < readOrgs.size(); i++)
            {
                readOrgInts[i] = ((Integer) readOrgs.get(i)).intValue();
            }

            for (int i = 0; i < writeOrgs.size(); i++)
            {
                writeOrgInts[i] = ((Integer) writeOrgs.get(i)).intValue();
            }

            eFap = new ExtendedFileAccessPermissions(wRead, wWrite, oRead, oWrite, aRead, aWrite, readOrgInts,
                    writeOrgInts);
        }
        else
        {
            eFap = null;
        }

        return eFap;
    }

    /**
     * Converts a XML escaped string to string.
     *
     * @return unescaped XML string.
     */
    public static final String fromXMLEscaped(String str)
    {
        StringBuffer results = new StringBuffer();
        for (int i = 0; i < str.length(); i++)
        {
            char c = str.charAt(i);
            StringBuffer sb = new StringBuffer();
            if(c == '&')
            {
                boolean finished = false;
                while((!finished) && (i < str.length()-1))
                {
                    i++;
                    c = str.charAt(i);
                    if(c == ';')
                    {
                        finished = true;
                        break;
                    }
                    sb.append(c);
                }
                String entity = sb.toString();

                if(entity.equals("amp"))
                {
                    results.append("&");
                }
                else if(entity.equals("lt"))
                {
                    results.append("<");
                }
                else if(entity.equals("gt"))
                {
                    results.append(">");
                }
                else if(entity.equals("quot"))
                {
                    results.append("\"");
                }
                else if(entity.equals("apos"))
                {
                    results.append("\'");
                }
                /*
                 * TODO: determine if de-escaping these entities is needed
                else if(entity.startsWith("#x"))
                {
                    String hex = entity.substring(2, entity.length());
                    int b = Integer.parseInt(hex, 16);
                    results.append((char)b);
                }
                 */
            }
            else
            {
                results.append(c);
            }

        }
        return results.toString();
    }


    /**
     * Converts a string into an XML escaped string.
     *
     * @return XML escaped string.
     */
    public static final String toXMLEscaped(String str)
    {
        StringBuffer sb = new StringBuffer();
        char ch;
        for (int i = 0; i < str.length(); i++)
        {
            ch = str.charAt(i);
            switch (ch)
            {
            /* XML Predefined entities per XML section 4.6 */
            case '&':
                sb.append("&amp;");
                break;
            case '<':
                sb.append("&lt;");
                break;
            case '>':
                sb.append("&gt;");
                break;
            case '\"':
                sb.append("&quot;");
                break;
            case '\'':
                sb.append("&apos;");
                break;
            case 0x0A:
                sb.append(ch);
                break;
            case 0x0D: // TODO : determine if newline escaping is required.
                /*
                 * Convert 0x0D into 0x0A and 0x0D0A into 0x0A per XML
                 * section 2.11
                 */
                sb.append((char) 0x0A);

                /* discard any immediately following 0x0A */
                if (i < str.length() + 1)
                {
                    ch = str.charAt(i + 1);
                    if (ch == 0x0A)
                    {
                        i++;
                    }
                }
                break;
            default:
                /*
                 * TODO : determine if appropriate level of escaping includes this range.  XML spec seems to allow.
                 *
                 * Non-printables to hex escaped - following URL practice of
                 * hex
                    if ((ch < ' ') || (ch > 127))
                    {
                        sb.append("&#x");
                        sb.append(Integer.toHexString(ch));
                        sb.append(';');
                    }
                    else
                 */
            {
                sb.append(ch);
            }
            break;
            }
        }

        return sb.toString();
    }

    /**
     * Takes one CSV list and makes sure it is a proper sub-set of another CSV list and
     * that all requested values have proper sort modifiers.
     *
     * @param supported a CSV list of supported values
     * @param requested a CSV list of requested values
     * @return true if the values check out, false if there is an issue.
     */
    public static boolean checkSortCriteria(String supportedCSV, String requestedCSV)
    {
        String[] supportedArray = split(supportedCSV, ",");
        String[] requestedArray = split(requestedCSV, ",");

        // Set to true for first pass.
        boolean match = true;

        for(int i = 0; i < requestedArray.length; i++)
        {
            // Previous pass did not find a match
            if(match == false)
            {
                return false;
            }

            // Only set to true if match is found
            match = false;

            // Check for some prereqs.  Requested value needs at least a modifier
            if(requestedArray[i] == null || requestedArray[i].length() < 1)
            {
                return false;
            }

            // Check that requested value starts with sort modifier UPnP ContentDirectory V3 2.3.16
            if(!(requestedArray[i].startsWith("+") || requestedArray[i].startsWith("-")))
            {
                return false;
            }

            // Strip off modifier
            String requestedValue = requestedArray[i].substring(1, requestedArray[i].length());

            for(int x = 0; x < supportedArray.length; x++)
            {
                if(requestedValue.equals(supportedArray[x]))
                {
                    // Found a match, go to next value or if last value
                    // it will fall through and return true.
                    match = true;
                    break;
                }
            }
        }

        return match;
    }

    /**
     * Compares dataType argument with floating point data types defined in UPnP Device
     * Architecture 1.0.
     *
     * @param dataType - value to be compared.
     * @return true if dataType is a floating point data type.
     */
    public static boolean isUDAFloat(String dataType) {
        if ("r4".equals(dataType)
                || "r8".equals(dataType)
                || "number".equals(dataType)
                || "fixed.14.4".equals(dataType)
                || "float".equals(dataType))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * Compares dataType argument with integer data types defined in UPnP Device
     * Architecture 1.0.
     *
     * @param dataType - value to be compared.
     * @return true if dataType is a integer data type.
     */
    public static boolean isUDAInt(String dataType) {
        if ("ui1".equals(dataType)
                || "ui2".equals(dataType)
                || "ui4".equals(dataType)
                || "i1".equals(dataType)
                || "i2".equals(dataType)
                || "i4".equals(dataType)
                || "int".equals(dataType))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * Compares dataType argument with date/time/dateTime data types defined in UPnP Device
     * Architecture 1.0.
     *
     * @param dataType - value to be compared.
     * @return true if dataType is a integer data type.
     */
    public static boolean isUDAIso8601(String dataType) {
        if ("date".equals(dataType)
                || "dateTime".equals(dataType)
                || "dateTime.tz".equals(dataType)
                || "time".equals(dataType)
                || "time.tz".equals(dataType))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * Compares dataType argument with valid state variable dataTypes defined in UPnP Device
     * Architecture 1.0.
     *
     * @param dataType - value to be compared.
     * @return true if dataType is a valid UDA data type.
     */
    public static boolean isUDADataType(String dataType) {
        if (isUDAIso8601(dataType)
                || isUDAInt(dataType)
                || isUDAFloat(dataType)
                || "char".equals(dataType)
                || "string".equals(dataType)
                || "boolean".equals(dataType)
                || "bin.base64".equals(dataType)
                || "bin.hex".equals(dataType)
                || "uri".equals(dataType)
                || "uuid".equals(dataType))
        {
            return true;
        }
        else
        {
            if (log.isErrorEnabled())
            {
                log.error("The SCPD dataType is invalid: " + dataType);
            }
            return false;
        }
    }

    /**
     * Compare value argument with allowedValue list as defined in UPnP Device
     * Architecture 1.0.
     *
     * @param value - value to be compared.
     * @param allowedValueList -
     * @return true if dataType is a integer data type.
     */
    public static boolean validateUDAStringValue(String value, String[] allowedValueList)
    {
        if (allowedValueList.length > 0)
        {
            boolean isAllowed = false;
            for (int x = 0; x < allowedValueList.length; x++)
            {
                if (allowedValueList[x].equals(value))
                {
                    isAllowed = true;
                    break;
                }
            }
            if (!isAllowed)
            {
                if (log.isErrorEnabled())
                {
                    log.error("value not in allowedValueList.");
                }
                return false;
            }

        }

        return true;
    }
    
    /**
     * Hash data into a hex string using an algorithm
     * @param data the data to hash
     * @param algorithm the algorithm to use
     * @return returns the hex value of the hash or null if there is an issue hashing
     */
    public static String hashToHex(byte[] data, String algorithm)
    {
        String result = null;
        
        try
        {
            MessageDigest dig = MessageDigest.getInstance(algorithm);
            byte[] digest = dig.digest(data);
            
            StringBuffer sb = new StringBuffer(2 * digest.length);
            for (int i = 0; i < digest.length; i++)
            {
                sb.append(HEX_CHARS.charAt((digest[i] & 0xF0) >> 4));
                sb.append(HEX_CHARS.charAt((digest[i] & 0x0F)));
            }
            result = sb.toString();
        }
        catch (NoSuchAlgorithmException e)
        {
            if(log.isWarnEnabled())
            {
                log.warn("Message Digest MD5 not available.  Multiple UDNs will be disabled", e);
            }
        }        
        return result;
    }

    /**
     * Validate value argument against integer or float data types defined in UPnP Device
     * Architecture 1.0.
     *
     * @param dataType - value to be compared.
     * @return true if dataType is a integer data type.
     */
    public static boolean validateUDANumericValue(String dataType, String value, String minValue, String maxValue)
    {
        if (isUDAInt(dataType))
        {
            long longVal;
            if (value.startsWith("+"))
            {
                value = value.substring(1, value.length());
            }
            try
            {
                longVal = Long.parseLong(value);
                if ("ui1".equals(dataType) && (longVal < 0 || longVal > 255)
                        || "ui2".equals(dataType) && (longVal < 0 || longVal > 65535)
                        || "ui4".equals(dataType) && (longVal < 0 || longVal > 4294967295d)
                        || "i1".equals(dataType) && (longVal < -128 || longVal > 127)
                        || "i2".equals(dataType) && (longVal < -32768   || longVal > 32767)
                        || "i4".equals(dataType) && (longVal < -2147483648 || longVal > 2147483647))
                {
                    if (log.isErrorEnabled())
                    {
                        log.error("UDA UDA dataType validation failed: " + dataType + " value out of range: " + value);
                    }
                    return false;
                }


            }
            catch (NumberFormatException e)
            {
                if (log.isErrorEnabled())
                {
                    log.error("UDA dataType validation failed: " + dataType + " could not be parsed from: " + value);
                }
                return false;
            }
            if (minValue != null && !"".equals(minValue))
            {
                try
                {
                    int minVal = Integer.parseInt(minValue);
                    int maxVal = Integer.parseInt(maxValue);
                    if (longVal < minVal || longVal > maxVal)
                    {
                        if (log.isErrorEnabled())
                        {
                            log.error("UDA dataType validation failed: " + dataType + " value out of range based on min/max value defined in state variable table: " + value);
                        }
                        return false;
                    }
                }
                catch (NumberFormatException e)
                {
                    // TODO : what kind of exception should be thrown in this case?
                    //What happens if the state variable being validated is from an SCPD recieved
                    //from a remote device and that SCPD has an invalid min or max value?  Should
                    //it have gotten this far? Should the local implementation reject the SCPD
                    //when it is first being processed?
                    throw e;
                }
            }
        }
        else if (isUDAFloat(dataType))
        {
            double doubleVal;
            try
            {
                doubleVal = Double.parseDouble(value);
                if (value.indexOf(".") < 0
                        //Note if 'value' is too close to 0 (e.g. value = -1.94065645841247E-324)
                        //then parseDouble(value) will return 0.0 so we will reject all 0.0 values.
                        || doubleVal == 0
                        || "r4".equals(dataType) && (doubleVal < 1.17549435E-38 || doubleVal > 3.40282347E+38)
                        //Note: UPnP Device Architecture 1.0 (revision July 20, 2006) states that
                        //the max & min value an r8 data type can be is +/- 1.79769313486232E308.
                        //Java only allows up to +/- 1.7976931348623157E308 for a double
                        || "r8".equals(dataType) && ((doubleVal > -4.94065645841247E-324 && doubleVal < 0) || doubleVal < -1.7976931348623157E308)
                        || "r8".equals(dataType) && ((doubleVal < 4.94065645841247E-324 && doubleVal > 0) || doubleVal > 1.7976931348623157E308)
                        || "number".equals(dataType) && ((doubleVal > -4.94065645841247E-324 && doubleVal < 0) || doubleVal < -1.7976931348623157E308)
                        || "number".equals(dataType) && ((doubleVal < 4.94065645841247E-324 && doubleVal > 0) || doubleVal > 1.7976931348623157E308)
                        || "fixed.14.4".equals(dataType) && ((doubleVal > -4.94065645841247E-324 && doubleVal < 0) || doubleVal < -1.7976931348623157E308)
                        || "fixed.14.4".equals(dataType) && ((doubleVal < 4.94065645841247E-324 && doubleVal > 0) || doubleVal > 1.7976931348623157E308))
                {
                    if (log.isErrorEnabled())
                    {
                        log.error("UDA dataType validation failed: " + dataType + " value out of range: " + value);
                    }
                    return false;
                }
            }
            catch (NumberFormatException e)
            {
                if (log.isErrorEnabled())
                {
                    log.error("UDA dataType validation failed: " + dataType + " could not be parsed from: " + value);
                }
                return false;
            }

            //Special validation for fixed.14.4 dataType
            if ("fixed.14.4".equals(dataType))
            {
                if (value.indexOf("E") < 0)
                {
                    if (log.isErrorEnabled())
                    {
                        log.error("UDA dataType validation failed: " + dataType + " with no E for exponent value: " + value);
                    }
                    return false;
                }
                StringTokenizer tokenizer = new StringTokenizer(value, ".");
                String token = tokenizer.nextToken();
                if (token.length() > 14)
                {
                    if (log.isErrorEnabled())
                    {
                        log.error("UDA dataType validation failed: " + dataType + " with more than 14 digits to the left of the decimal: " + value);
                    }
                    return false;
                }
                token = tokenizer.nextToken();
                if (token.indexOf("E") >= 0)
                {
                    tokenizer = new StringTokenizer(token, "E");
                }
                token = tokenizer.nextToken();
                if (token.length() > 4)
                {
                    if (log.isErrorEnabled())
                    {
                        log.error("UDA dataType validation failed: " + dataType + " with more than 4 digits to the right of the decimal: " + value);
                    }
                    return false;
                }
            }

            //Validate value against min & max value if they have been set.
            if (minValue != null && !"".equals(minValue))
            {
                try
                {
                    float minVal = Float.parseFloat(minValue);
                    float maxVal = Float.parseFloat(maxValue);
                    if (doubleVal < minVal || doubleVal > maxVal)
                    {
                        if (log.isErrorEnabled())
                        {
                            log.error("UDA dataType validation failed: " + dataType + " value out of range based on min/max value defined in state variable table: " + value);
                        }
                        return false;
                    }
                }
                catch (NumberFormatException e)
                {
                    // TODO : what kind of exception should be thrown in this case?
                    //What happens if the state variable being validated is from an SCPD recieved
                    //from a remote device and that SCPD has an invalid min or max value?  Should
                    //it have gotten this far? Should the local implementation reject the SCPD
                    //when it is first being processed?
                    throw e;
                }
            }
        }
        return true;
    }

    /**
     * Validate value argument against char, boolean, data types defined in UPnP Device
     * Architecture 1.0.
     *
     * @param dataType - value to be compared.
     * @return true if dataType is a integer data type.
     */
    public static boolean validateUDAValue(String dataType, String value)
    {
        if("char".equals(dataType))
        {
            //TODO : how do we validate a unicode character?  Should the string value be in hex?
            //         Can it be in in  java notation (e.g. '\u0037')
            /*if (value.indexOf("\\u") == 0)
            {
                if (value.length() > 6)
                {
                    log.error(" value was more then one char: " + value);
                    return false;
                }
            }
            else if (value.length() > 1)
            {
                log.error("value was more then one char: " + value);
                return false;
            }
             */
        }
        if("boolean".equals(dataType))
        {
            if (!(value.equals("1")
                    || value.equals("0")
                    || value.equals("true")
                    || value.equals("false")
                    || value.equals("yes")
                    || value.equals("no")))
            {
                if (log.isErrorEnabled())
                {
                    log.error("UDA dataType validation failed: invalid boolen value: " + value);
                }
                return false;
            }

        }
        else if(isUDAIso8601(dataType))
        {
            if (!validateIso8601(dataType, value))
            {
                return false;
            }
        }
        else
        {
            if (log.isErrorEnabled())
            {
                log.error("Unsupported dataType: " + dataType);
            }
            return false;
        }
        return true;
    }

    private static boolean validateIso8601(String dataType, String value)
    {

        if ("date".equals(dataType))
        {
            return validateIso8601Date(value);
        }
        else if ("dateTime".equals(dataType))
        {
            return validateIso8601DateTime(value);
        }
        else if ("dateTime.tz".equals(dataType))
        {
            return validateDateTimeWithTimeZone(value);
        }
        else if ("time".equals(dataType))
        {
            return validateIso8601Time(value);
        }
        else if ("time.tz".equals(dataType))
        {
            return validateIso8601TimeWithTimeZone(value);
        }
        return false;
    }

    // Implementation of ISO8601 date/time data type validation is on hold until it's decided if
    // the Joda api will be used for validation (See OCORI-3219).
    private static boolean validateIso8601Date(String date)
    {
        // Valid ISO8601 dates can start with a leading '+' or '-' and one or more 0s
        if (date.startsWith("+") || date.startsWith("-"))
        {
            date = date.substring(1);
            if (date.length() > 0 && date.charAt(0) != '0')
            {
                if (log.isErrorEnabled())
                {
                    log.error("UDA dataType validation failed: date " +
                            "value had invalid ISO8601 format: " + date);
                }
                return false;
            }
            while (date.length() > 0 && date.charAt(0) == '0')
            {
                date = date.substring(1);
            }
        }

        // ISO8601 dates can be in the following formats: YYYYMMDD,YYYY-MM-DD, YYYY-MM,
        // YYYY, YY, YYYYJJJ, YYYY-JJJ, YYYYWwwD, YYYY-Www-D, YYYY-Www, and YYYYWww
        String pattern1 = "^\\d\\d\\d\\d\\d\\d\\d\\d$";
        String pattern2 = "^\\d\\d\\d\\d-\\d\\d-\\d\\d$";
        String pattern3 = "^\\d\\d\\d\\d-\\d\\d$";
        String pattern4 = "^\\d\\d\\d\\d$";
        String pattern5 = "^\\d\\d$";
        String pattern6 = "^\\d\\d\\d\\d\\d\\d\\d$";
        String pattern7 = "^\\d\\d\\d\\d-\\d\\d\\d$";
        String pattern8 = "^\\d\\d\\d\\dW\\d\\d\\d$";;
        String pattern9 = "^\\d\\d\\d\\d-W\\d\\d-\\d$";
        String pattern10 = "^\\d\\d\\d\\d-W\\d\\d$";
        String pattern11 = "^\\d\\d\\d\\dW\\d\\d";
        RegularExpression regEx = new RegularExpression(pattern1);

        String year = null;
        String month = null;
        String day = null;
        String julianDay = null;
        String week = null;
        String weekDay = null;
        if (regEx.matches(date))
        {
            // YYYYMMDD
            year = date.substring(0, 4);
            month = date.substring(4, 6);
            day = date.substring(6, 8);
            return isValidYear(year) && isValidMonth(month) &&
                isValidMonthDay(day, month, year);
        }
        regEx.setPattern(pattern2);
        if (regEx.matches(date))
        {
            // YYYY-MM-DD
            year = date.substring(0, 4);
            month = date.substring(5, 7);
            day = date.substring(8, 10);
            return isValidYear(year) && isValidMonth(month) &&
                isValidMonthDay(day, month, year);
        }
        regEx.setPattern(pattern3);
        if (regEx.matches(date))
        {
            // YYYY-MM
            year = date.substring(0, 4);
            month = date.substring(5, 7);
            return isValidYear(year) && isValidMonth(month);
        }
        regEx.setPattern(pattern4);
        if (regEx.matches(date))
        {
            // YYYY
            year = date.substring(0, 4);
            return isValidYear(year);
        }
        regEx.setPattern(pattern5);
        if (regEx.matches(date))
        {
            // YY
            String century = date.substring(0, 2);
            return isValidCentury(century);
        }
        regEx.setPattern(pattern6);
        if (regEx.matches(date))
        {
            // YYYYJJJ
            year = date.substring(0, 4);
            julianDay = date.substring(4, 7);
            return isValidYear(year) && isValidJulianDay(julianDay, year);
        }
        regEx.setPattern(pattern7);
        if (regEx.matches(date))
        {
            // YYYY-JJJ
            year = date.substring(0, 4);
            julianDay = date.substring(5, 8);
            return isValidYear(year) && isValidJulianDay(julianDay, year);
        }
        regEx.setPattern(pattern8);
        if (regEx.matches(date))
        {
            // YYYYWwwD
            year = date.substring(0, 4);
            week = date.substring(5, 7);
            weekDay = date.substring(7);
            return isValidYear(year) && isValidWeek(week) && isValidWeekDay(weekDay);
        }
        regEx.setPattern(pattern9);
        if (regEx.matches(date))
        {
            // YYYY-Www-D
            year = date.substring(0, 4);
            week = date.substring(6, 8);
            weekDay = date.substring(9);
            return isValidYear(year) && isValidWeek(week) && isValidWeekDay(weekDay);
        }
        regEx.setPattern(pattern10);
        if (regEx.matches(date))
        {
            // YYYY-Www
            year = date.substring(0, 4);
            week = date.substring(6, 8);
            return isValidYear(year) && isValidWeek(week);
        }
        regEx.setPattern(pattern11);
        if (regEx.matches(date))
        {
            // YYYYWww
            year = date.substring(0, 4);
            week = date.substring(5, 7);
            return isValidYear(year) && isValidWeek(week);
        }
        else
        {
            if (log.isErrorEnabled())
            {
                log.error("UDA dataType validation failed: date " +
                        "value had invalid ISO8601 format: " + date);
            }
        }
        return false;
    }

    private static boolean validateIso8601Time(String time)
    {
        // Valid ISO8601 times can have a leading "T"
        if (time.charAt(0) == 'T')
        {
            time = time.substring(1);
        }
        StringTokenizer tokenizer = new StringTokenizer(time, ":");
        int tokens = tokenizer.countTokens();
        // The case where there are no colons
        if (tokens == 1)
        {
            // Valid ISO8601 times with no colon characters can be in the
            // following formats: hhmmss hhmm hh hhmmss,ss hhmm,mm hh,hh
            StringTokenizer commaTokenizer = new StringTokenizer(time, ",");
            int commaTokens = commaTokenizer.countTokens();
            if (commaTokens == 1)
            {
                int length = time.length();
                String hour = time.substring(0, 2);
                if (length == 2)
                {
                    // The format must be hh
                    return isValidHour(hour);
                }
                else if (length == 4)
                {
                    // The format must be hhmm
                    String minute = time.substring(2);
                    return isValidHour(hour) && isValidMinuteOrSecond(minute);
                }
                else if (length == 6)
                {
                    // The format must be hhmmss
                    String minute = time.substring(2, 4);
                    String second = time.substring(4,6);
                    return isValidHour(hour) && isValidMinuteOrSecond(minute) &&
                    isValidMinuteOrSecond(second);
                }
                else
                {
                    if (log.isErrorEnabled())
                    {
                        log.error("UDA dataType validation failed: time " +
                                "value had invalid ISO8601 format: " + time);
                    }
                    return false;
                }
            }
            else if (commaTokens == 2)
            {
                // Possible formats are hhmmss,ss hhmm,mm or hh,hh
                String hoursMinutesSeconds = commaTokenizer.nextToken();
                String timeFraction = commaTokenizer.nextToken();
                try
                {
                    // The time fraction, which is the value after the comma,
                    // can be any valid number with varying length
                    Integer.parseInt(timeFraction);
                }
                catch (NumberFormatException n)
                {
                    if (log.isErrorEnabled())
                    {
                        log.error("UDA dataType validation failed: time value" +
                                " had invalid ISO8601 format: " + time);
                    }
                    return false;
                }
                int length = hoursMinutesSeconds.length();
                String hour = hoursMinutesSeconds.substring(0,2);
                if (length == 6)
                {
                    String minute = hoursMinutesSeconds.substring(2,4);
                    String second = hoursMinutesSeconds.substring(4);
                    return isValidHour(hour) && isValidMinuteOrSecond(minute) &&
                    isValidMinuteOrSecond(second);
                }
                else if (length == 4)
                {
                    String minute = hoursMinutesSeconds.substring(2);
                    return isValidHour(hour) && isValidMinuteOrSecond(minute);
                }
                else if (length == 2)
                {
                    return isValidHour(hour);
                }
                else
                {
                    // The only valid formats are hhmmss,ss hhmm,mm or hh,hh so
                    // anything else is not a valid ISO8601 time format
                    if (log.isErrorEnabled())
                    {
                        log.error("UDA dataType validation failed: time value" +
                                " had invalid ISO8601 format: " + time);
                    }
                    return false;
                }
            }
            else
            {
                if (log.isErrorEnabled())
                {
                    log.error("UDA dataType validation failed: time value" +
                            " had invalid ISO8601 format: " + time);
                }
                return false;
            }
        }
        // The case where the time has one colon
        else if (tokens == 2)
        {
            // Possible valid ISO8601 time formats with one colon are hh:mm or hh:mm,mm
            String hour = tokenizer.nextToken();
            String minutes = tokenizer.nextToken();
            StringTokenizer commaTokenizer = new StringTokenizer(minutes, ",");
            int commaTokens = commaTokenizer.countTokens();
            if (!isValidHour(hour))
            {
                // No need for a log error message, as isValidHour logs errors
                return false;
            }

            if (commaTokens == 1)
            {
                return isValidMinuteOrSecond(minutes);
            }
            else if (commaTokens == 2)
            {
                String minute = commaTokenizer.nextToken();
                String minuteExtension = commaTokenizer.nextToken();
                try
                {
                    Integer.parseInt(minuteExtension);
                }
                catch (NumberFormatException n)
                {
                    if (log.isErrorEnabled())
                    {
                        log.error("UDA dataType validation failed: time value" +
                                " had invalid ISO8601 format: " + time);
                    }
                    return false;
                }
                return isValidMinuteOrSecond(minute);
            }
            else
            {
                if (log.isErrorEnabled())
                {
                    log.error("UDA dataType validation failed: time value" +
                            " had invalid ISO8601 format: " + time);
                }
                return false;
            }
        }
        // The case where the time has two colons
        else if (tokens == 3)
        {
            // Valid formats are hh:mm:ss hh:mm:ss,ss
            String hour = tokenizer.nextToken();
            String minutes = tokenizer.nextToken();
            String seconds = tokenizer.nextToken();
            StringTokenizer commaTokenizer = new StringTokenizer(seconds, ",");
            int commaTokens = commaTokenizer.countTokens();
            // Validate the hour and minute tokens now since they will be the
            // same for any of the formats
            if (!isValidHour(hour) || !isValidMinuteOrSecond(minutes))
            {
                // The validation methods above log errors, so no error logging
                // is needed here
                return false;
            }

            if (commaTokens == 1)
            {
                // Hour and minute have already been validated, so validate seconds
                return isValidMinuteOrSecond(seconds);
            }
            else if (commaTokens == 2)
            {
                String second = commaTokenizer.nextToken();
                String secondExtension = commaTokenizer.nextToken();
                try
                {
                    Integer.parseInt(secondExtension);
                }
                catch (NumberFormatException n)
                {
                    if (log.isErrorEnabled())
                    {
                        log.error("UDA dataType validation failed: time value " +
                                "had invalid ISO8601 format: " + time);
                    }
                    return false;
                }
                return isValidMinuteOrSecond(second);
            }
            else
            {
                if (log.isErrorEnabled())
                {
                    log.error("UDA dataType validation failed: time value " +
                            "had invalid ISO8601 format: " + time);
                }
                return false;
            }
        }
        else
        {
            if (log.isErrorEnabled())
            {
                log.error("UDA dataType validation failed: time value " +
                        "had invalid ISO8601 format: " + time);
            }
            return false;
        }
    }

    private static boolean validateIso8601TimeWithTimeZone(String time)
    {
        // A valid ISO8601 time with time zone must be in a valid ISO8601 time
        // followed by the following possible time zone offsets: +hh, +hhmm, -hh,
        // -hhmm, +hh:mm, or -hh:mm
        StringTokenizer plusTokenizer = new StringTokenizer(time, "+");
        StringTokenizer minusTokenizer = new StringTokenizer(time, "-");
        int plusTokens = plusTokenizer.countTokens();
        int minusTokens = minusTokenizer.countTokens();
        if (plusTokens == 1 && minusTokens == 1)
        {
            if (time.endsWith("Z"))
            {
                time = time.substring(0, time.length() - 1);
            }
            return validateIso8601Time(time);
        }
        if ((plusTokens == 2) != (minusTokens == 2))
        {
            String timeValue = null;
            String offset = null;
            if (plusTokens ==2)
            {
                timeValue = plusTokenizer.nextToken();
                offset = plusTokenizer.nextToken();
            }
            else
            {
                timeValue = minusTokenizer.nextToken();
                offset = minusTokenizer.nextToken();
            }
            if (!validateIso8601Time(timeValue))
            {
                if (log.isErrorEnabled())
                {
                    log.error("UDA dataType validation failed: time.tz value " +
                            "had invalid ISO8601 format: " + time);
                }
            }
            StringTokenizer colonTokenizer = new StringTokenizer(offset, ":");
            int colonTokens = colonTokenizer.countTokens();
            if (colonTokens == 1)
            {
                if (offset.length() == 2)
                {
                    return isValidHour(offset);
                }
                else if (offset.length() == 4)
                {
                    return isValidHour(offset.substring(0,2)) &&
                    isValidMinuteOrSecond(offset.substring(2));
                }
                else
                {
                    if (log.isErrorEnabled())
                    {
                        log.error("UDA dataType validation failed: time.tz value " +
                                "had invalid ISO8601 format: " + time);
                    }
                    return false;
                }
            }
            else if (colonTokens == 2)
            {
                String hour = colonTokenizer.nextToken();
                String minutes = colonTokenizer.nextToken();
                return isValidHour(hour) && isValidMinuteOrSecond(minutes);
            }
            else
            {
                if (log.isErrorEnabled())
                {
                    log.error("UDA dataType validation failed: time.tz value " +
                            "had invalid ISO8601 format: " + time);
                }
                return false;
            }
        }
        else
        {
            if (log.isErrorEnabled())
            {
                log.error("UDA dataType validation failed: time.tz value " +
                        "had invalid ISO8601 format: " + time);
            }
            return false;
        }
    }

    private static boolean validateIso8601DateTime(String dateTime)
    {
        // ISO8601 dateTime datatypes have a 'T' separating the date portion
        // from the time portion. The date portion must be defined as a
        // complete representation
        StringTokenizer tokenizer = new StringTokenizer(dateTime, "T");
        // UDA dateTime.tz values may have a date but not a time
        if (tokenizer.countTokens() == 1)
        {
            String date = tokenizer.nextToken();
            if (dateTime.indexOf("T") != -1)
            {
                if (log.isErrorEnabled())
                {
                    log.error("UDA dataType validation failed: dateTime.tz value " +
                            "had invalid ISO8601 format: " + dateTime);
                }
                return false;
            }
            if (!(isCompleteDateRepresentation(date) && validateIso8601Date(date)))
            {
                if (log.isErrorEnabled())
                {
                    log.error("UDA dataType validation failed: dateTime value " +
                            "had invalid ISO8601 format: " + dateTime);
                }
                return false;
            }
            else
            {
                return true;
            }
        }
        else if (tokenizer.countTokens() == 2)
        {
            String date = tokenizer.nextToken();
            String time = tokenizer.nextToken();
            if (!(isCompleteDateRepresentation(date) && validateIso8601Date(date)))
            {
                if (log.isErrorEnabled())
                {
                    log.error("UDA dataType validation failed: dateTime value " +
                            "had invalid ISO8601 format: " + dateTime);
                }
                return false;
            }
            return validateIso8601Time(time);
        }
        else
        {
            if (log.isErrorEnabled())
            {
                log.error("UDA dataType validation failed: " + dateTime +
                "is not a valid dateTime value.");
            }
            return false;
        }
    }

    private static boolean validateDateTimeWithTimeZone(String dateTime)
    {
        // The dateTime data types should have a 'T' character separating the
        // date portion from the time portion. The date portion must be defined
        // as a complete representation
        StringTokenizer tokenizer = new StringTokenizer(dateTime, "T");
        int tokens = tokenizer.countTokens();
        // A dateTime.tz value may have only a valid date with no time or time zone
        if (tokens == 1)
        {
            String date = tokenizer.nextToken();
            // If the dateTime.tz value does not contain time data, there should not be
            // a "T" character in the string
            if (dateTime.indexOf("T") != -1)
            {
                if (log.isErrorEnabled())
                {
                    log.error("UDA dataType validation failed: dateTime.tz value " +
                            "had invalid ISO8601 format: " + dateTime);
                }
                return false;
            }
            if (!(isCompleteDateRepresentation(date) && validateIso8601Date(date)))
            {
                if (log.isErrorEnabled())
                {
                    log.error("UDA dataType validation failed: dateTime.tz value " +
                            "had invalid ISO8601 format: " + dateTime);
                }
                return false;
            }
            else
            {
                return true;
            }
        }
        if (tokens == 2)
        {
            String date = tokenizer.nextToken();
            String time = dateTime.substring(date.length() + 1);
            if (isCompleteDateRepresentation(date) && validateIso8601Date(date) &&
                    validateIso8601TimeWithTimeZone(time))
            {
                return true;
            }
            else
            {
                if (log.isErrorEnabled())
                {
                    log.error("UDA dataType validation failed: dateTime.tz value " +
                            "had invalid ISO8601 format: " + dateTime);
                }
                return false;
            }
        }
        else
        {
            if (log.isErrorEnabled())
            {
                log.error("UDA dataType validation failed: dateTime.tz value " +
                        "had invalid ISO8601 format: " + dateTime);
            }
            return false;
        }
    }

    // The validateDateTime method will call this to verify that the date format being used is
    // a complete ISO8601 date representation (YYYYMMDD, YYYYJJJ, YYYYWwwD, YYYY-JJJ, YYYY-MM-DD, or
    // YYYY-Www-D
    private static boolean isCompleteDateRepresentation(String date)
    {
        StringTokenizer tokenizer = new StringTokenizer(date, "-");
        int tokens = tokenizer.countTokens();
        if (tokens == 1)
        {
            // Format should be either YYYYMMDD, YYYYJJJ, or YYYYWwwD
            StringTokenizer weekTokenizer = new StringTokenizer(date, "W");
            int weekTokens = weekTokenizer.countTokens();
            if (weekTokens == 1)
            {
                if (date.length() == 7 || date.length() == 8)
                {
                    try
                    {
                        Integer.parseInt(date);
                        return true;
                    }
                    catch (NumberFormatException n)
                    {
                        if (log.isErrorEnabled())
                        {
                            log.error("UDA dataType validation failed: date value" +
                                    "had invalid ISO8601 format:" + date);
                        }
                        return false;
                    }
                }
                else
                {
                    return false;
                }
            }
            else if (weekTokens == 2)
            {
                String year = weekTokenizer.nextToken();
                String weekAndDay = weekTokenizer.nextToken();
                if (year.length() == 4 && weekAndDay.length() == 3)
                {
                    try
                    {
                        Integer.parseInt(year);
                        Integer.parseInt(weekAndDay);
                        return true;
                    }
                    catch (NumberFormatException n)
                    {
                        if (log.isErrorEnabled())
                        {
                            log.error("UDA dataType validation failed: date value" +
                                    "had invalid ISO8601 format:" + date);
                        }
                        return false;
                    }
                }
                else
                {
                    return false;
                }
            }
            else
            {
                return false;
            }
        }
        else if (tokens == 2)
        {
            // Format should be YYYY-JJJ
            String year = tokenizer.nextToken();
            String julianDay = tokenizer.nextToken();
            if (year.length() == 4 && julianDay.length() == 3)
            {
                try
                {
                    Integer.parseInt(year);
                    Integer.parseInt(julianDay);
                    return true;
                }
                catch (NumberFormatException n)
                {
                    if (log.isErrorEnabled())
                    {
                        log.error("UDA dataType validation failed: date value" +
                                "had invalid ISO8601 format:" + date);
                    }
                    return false;
                }
            }
            else
            {
                return false;
            }
        }
        else if (tokens == 3)
        {
            // Format should be YYYY-MM-DD or YYYY-Www-D
            String year = tokenizer.nextToken();
            String monthOrWeek = tokenizer.nextToken();
            String day = tokenizer.nextToken();
            if (!(day.length() == 1 || day.length() == 2))
            {
                return false;
            }
            try
            {
                Integer.parseInt(year);
                Integer.parseInt(day);
            }
            catch (NumberFormatException n)
            {
                if (log.isErrorEnabled())
                {
                    log.error("UDA dataType validation failed: date value" +
                            "had invalid ISO8601 format:" + date);
                }
                return false;
            }
            if (monthOrWeek.charAt(0) == 'W')
            {
                monthOrWeek = monthOrWeek.substring(1);
            }
            if (monthOrWeek.length() == 2)
            {
                try
                {
                    Integer.parseInt(monthOrWeek);
                    return true;
                }
                catch (NumberFormatException n)
                {
                    if (log.isErrorEnabled())
                    {
                        log.error("UDA dataType validation failed: date value" +
                                "had invalid ISO8601 format:" + date);
                    }
                    return false;
                }
            }
            else
            {
                return false;
            }
        }
        else
        {
            return false;
        }
    }

    private static boolean isValidHour(String hour)
    {
        if (hour.length() != 2)
        {
            if (log.isErrorEnabled())
            {
                log.error("UDA dataType validation failed: " + hour + " is not a valid" +
                " hour. Hour must be a two-digit number.");
            }
            return false;
        }
        try
        {
            int hourValue = Integer.parseInt(hour);
            if (hourValue >= 0 && hourValue < 24)
            {
                return true;
            }
            else
            {
                if (log.isErrorEnabled())
                {
                    log.error("UDA dataType validation failed: " + hour + " is not a valid hour.");
                }
                return false;
            }
        }
        catch (NumberFormatException n)
        {
            if (log.isErrorEnabled())
            {
                log.error("UDA dataType validation failed: " + hour + " is not a valid hour.");
            }
            return false;
        }
    }

    private static boolean isValidMinuteOrSecond(String minuteOrSecond)
    {
        if (minuteOrSecond.length() != 2)
        {
            if (log.isErrorEnabled())
            {
                log.error("UDA dataType validation failed: " + minuteOrSecond +
                        "is not a valid minute or second. Valid minutes or seconds" +
                " must be two-digit values.");
            }
            return false;
        }
        try
        {
            int minuteOrSecondValue = Integer.parseInt(minuteOrSecond);
            if (minuteOrSecondValue >= 0 && minuteOrSecondValue < 60)
            {
                return true;
            }
            else
            {
                if (log.isErrorEnabled())
                {
                    log.error("UDA dataType validation failed: " + minuteOrSecond +
                    " is not a valid value for minutes or seconds.");
                }
                return false;
            }
        }
        catch (NumberFormatException n)
        {
            if (log.isErrorEnabled())
            {
                log.error("UDA dataType validation failed: " + minuteOrSecond +
                " is not a valid value for minutes or seconds.");
            }
            return false;
        }
    }

    private static boolean isValidMonth(String month)
    {
        if (month.length() != 2)
        {
            if (log.isErrorEnabled())
            {
                log.error("UDA dataType validation failed: " + month +
                " is not a valid month value. Months must be two-digit values.");
            }
            return false;
        }
        try
        {
            int monthValue = Integer.parseInt(month);
            if (monthValue > 0 && monthValue < 13)
            {
                return true;
            }
            else
            {
                if (log.isErrorEnabled())
                {
                    log.error("UDA dataType validation failed: " + month +
                    " is not a valid month value.");
                }
                return false;
            }
        }
        catch (NumberFormatException n)
        {
            if (log.isErrorEnabled())
            {
                log.error("UDA dataType validation failed: " + month +
                " is not a valid month value.");
            }
            return false;
        }
    }

    private static boolean isValidMonthDay(String day, String month, String year)
    {
        if (day.length() != 2 || month.length() != 2 || year.length() != 4)
        {
            if (log.isErrorEnabled())
            {
                log.error("UDA dataType validation failed: " + year + "-" + month + "-" +
                        day + ", is not a valid year-month-day combination.");
            }
            return false;
        }
        try
        {
            int monthValue = Integer.parseInt(month);
            int yearValue = Integer.parseInt(year);
            int dayValue = Integer.parseInt(day);
            if (monthValue == 4 || monthValue == 6 || monthValue == 9 || monthValue == 11)
            {
                if (dayValue > 0 && dayValue < 31)
                {
                    return true;
                }
                else
                {
                    if (log.isErrorEnabled())
                    {
                        log.error("UDA dataType validation failed: " + month + "-" +
                                day + ", is not a valid month, day combination.");
                    }
                    return false;
                }
            }
            else if (monthValue == 1 || monthValue == 3 || monthValue == 5 || monthValue == 7 ||
                    monthValue == 8 || monthValue == 10 || monthValue == 12)
            {
                if (dayValue > 0 && dayValue < 32)
                {
                    return true;
                }
                else
                {
                    if (log.isErrorEnabled())
                    {
                        log.error("UDA dataType validation failed: " + month + "-" +
                                day + ", is not a valid month, day combination.");
                    }
                    return false;
                }
            }
            else if (monthValue == 2)
            {
                GregorianCalendar cal = new GregorianCalendar();
                if (cal.isLeapYear(yearValue))
                {
                    if (dayValue > 0 && dayValue < 30)
                    {
                        return true;
                    }
                    else
                    {
                        if (log.isErrorEnabled())
                        {
                            log.error("UDA dataType validation failed: " +
                                    year + "-" + month + "-" + day + " is not " +
                            "a valid year-month-day combination.");
                        }
                        return false;
                    }
                }
                else
                {
                    if (dayValue > 0 && dayValue < 29)
                    {
                        return true;
                    }
                    else
                    {
                        if (log.isErrorEnabled())
                        {
                            log.error("UDA dataType validation failed: " +
                                    year + "-" + month + "-" + day + " is not " +
                            "a valid year-month-day combination.");
                        }
                        return false;
                    }
                }
            }
            else
            {
                return false;
            }
        }
        catch (NumberFormatException n)
        {
            if (log.isErrorEnabled())
            {
                log.error("UDA dataType validation failed: " + year + "-" +
                        month + "-" + day + " is not a valid year-month-day combination.");
            }
            return false;
        }
    }

    private static boolean isValidWeek(String week)
    {
        if (week.length() != 2)
        {
            if (log.isErrorEnabled())
            {
                log.error("UDA dataType validation failed: " + week + "is " +
                "not a valid week value. Weeks must be a two-digit number.");
            }
            return false;
        }
        try
        {
            int weekValue = Integer.parseInt(week);
            if (weekValue < 1 || weekValue > 54)
            {
                if (log.isErrorEnabled())
                {
                    log.error("UDA dataType validation failed: " + week +
                    "is not a valid week value.");
                }
                return false;
            }
            return true;
        }
        catch (NumberFormatException n)
        {
            if (log.isErrorEnabled())
            {
                log.error("UDA dataType validation failed: " + week + "is not a valid" +
                "week value.");
            }
            return false;
        }
    }

    private static boolean isValidWeekDay(String weekDay)
    {
        if (weekDay.length() != 1)
        {
            if (log.isErrorEnabled())
            {
                log.error("UDA dataType validation failed: " + weekDay + "is " +
                "not a valid weekday value. Weekdays must be a one-digit number.");
            }
            return false;
        }
        try
        {
            int weekDayValue = Integer.parseInt(weekDay);
            if (weekDayValue < 1 || weekDayValue > 7)
            {
                if (log.isErrorEnabled())
                {
                    log.error("UDA dataType validation failed: " + weekDay + "is " +
                    "not a valid weekday value.");
                }
                return false;
            }
            return true;
        }
        catch (NumberFormatException n)
        {
            if (log.isErrorEnabled())
            {
                log.error("UDA dataType validation failed: " + weekDay + "is " +
                "not a valid weekday value.");
            }
            return false;
        }
    }

    private static boolean isValidJulianDay(String day, String year)
    {
        if (day.length() != 3 || year.length() != 4)
        {
            if (log.isErrorEnabled())
            {
                log.error("UDA dataType validation failed: " + year + "-" +
                        day + "is not a valid year-day value.");
            }
            return false;
        }
        try
        {
            int yearValue = Integer.parseInt(year);
            int dayValue = Integer.parseInt(day);
            GregorianCalendar cal = new GregorianCalendar();
            if (cal.isLeapYear(yearValue))
            {
                if (dayValue > 0 && dayValue < 367)
                {
                    return true;
                }
                else
                {
                    if (log.isErrorEnabled())
                    {
                        log.error("UDA dataType validation failed: " + year +
                                "-" + day + "is not a valid year-day value.");
                    }
                    return false;
                }
            }
            else
            {
                if (dayValue > 0 && dayValue < 366)
                {
                    return true;
                }
                else
                {
                    if (log.isErrorEnabled())
                    {
                        log.error("UDA dataType validation failed: " + year +
                                "-" + day + "is not a valid year-day value.");
                    }
                    return false;
                }
            }
        }
        catch (NumberFormatException n)
        {
            if (log.isErrorEnabled())
            {
                log.error("UDA dataType validation failed: " + year + "-" + day +
                "is not a valid year-day value.");
            }
            return false;
        }
    }

    private static boolean isValidCentury(String year)
    {
        try
        {
            int centuryValue = Integer.parseInt(year);
            return centuryValue > 17;
        }
        catch (NumberFormatException n)
        {
            if (log.isErrorEnabled())
            {
                log.error("UDA dataType validation failed: " + year + "is not"
                        + " a valid century.");
            }
            return false;
        }
    }

    private static boolean isValidYear(String year)
    {
        if (year.length() != 4)
        {
            if (log.isErrorEnabled())
            {
                log.error("UDA dataType validation failed: " + year + "is not"
                        + " a valid year.");
            }
            return false;
        }
        try
        {
            int yearValue = Integer.parseInt(year);
            return yearValue > 1582;
        }
        catch (NumberFormatException n)
        {
            if (log.isErrorEnabled())
            {
                log.error("UDA dataType validation failed: " + year + "is not"
                        + " a valid year.");
            }
            return false;
        }
    }

    private static String[] getTimeRegexPatterns()
    {
        String pattern1 = "^\\d\\d\\d\\d\\d\\d$";
        String pattern2 = "^\\d\\d\\d\\d$";
        String pattern3 = "^\\d\\d$";
        String pattern4 = "^\\d\\d\\d\\d\\d\\d,\\d+$";
        String pattern5 = "^\\d\\d\\d\\d\\d\\d,\\d+(\\+|-)\\d\\d$";
        String pattern6 = "^\\d\\d\\d\\d\\d\\d,\\d+(\\+|-)\\d\\d\\d\\d$";
        String pattern7 = "^\\d\\d\\d\\d,\\d+$";
        String pattern8 = "^\\d\\d\\d\\d,\\d+(\\+|-)\\d\\d$";
        String pattern9 = "^\\d\\d\\d\\d,\\d+(\\+|-)\\d\\d\\d\\d$";
        String pattern10 = "^\\d\\d,\\d+$";
        String pattern11 = "^\\d\\d,\\d+(\\+|-)\\d\\d$";
        String pattern12 = "^\\d\\d,\\d+(\\+|-)\\d\\d\\d\\d$";
        String pattern13 = "^\\d\\d\\d\\d\\d\\dZ$";
        String pattern14 = "^\\d\\d\\d\\dZ$";
        String pattern15 = "^\\d\\dZ$";
        String pattern16 = "^\\d\\d(\\+|-)\\d\\d$";
        String pattern17 = "^\\d\\d(\\+|-)\\d\\d\\d\\d$";
        String pattern18 = "^\\d\\d\\d\\d(\\+|-)\\d\\d$";
        String pattern19 = "^\\d\\d\\d\\d(\\+|-)\\d\\d\\d\\d$";
        String pattern20 = "^\\d\\d\\d\\d\\d\\d(\\+|-)\\d\\d$";
        String pattern21 = "^\\d\\d\\d\\d\\d\\d(\\+|-)\\d\\d\\d\\d$";
        String pattern22 = "^\\d\\d:\\d\\d$";
        String pattern23 = "^\\d\\d:\\d\\d(\\+|-)\\d\\d$";
        String pattern24 = "^\\d\\d:\\d\\d(\\+|-)\\d\\d:\\d\\d$";
        String pattern25 = "^\\d\\d:\\d\\d,\\d+$";
        String pattern26 = "^\\d\\d:\\d\\d,\\d+(\\+|-)\\d\\d$";
        String pattern27 = "^\\d\\d:\\d\\d,\\d+(\\+|-)\\d\\d:\\d\\d$";
        String pattern28 = "^\\d\\d:\\d\\dZ$";
        String pattern29 = "^\\d\\d:\\d\\d:\\d\\d$";
        String pattern30 = "^\\d\\d:\\d\\d:\\d\\d,\\d+$";
        String pattern31 = "^\\d\\d:\\d\\d:\\d\\d,\\d+(\\+|-)\\d\\d$";
        String pattern32 = "^\\d\\d:\\d\\d:\\d\\d,\\d+(\\+|-)\\d\\d:\\d\\d$";
        String pattern33 = "^\\d\\d:\\d\\d:\\d\\dZ$";
        String pattern34 = "^\\d\\d:\\d\\d:\\d\\d(\\+|-)\\d\\d$";
        String pattern35 = "^\\d\\d:\\d\\d:\\d\\d(\\+|-)\\d\\d:\\d\\d$";

        String[] patterns = {pattern1, pattern2, pattern3, pattern4, pattern5,
                            pattern6, pattern7, pattern8, pattern9, pattern10,
                            pattern11, pattern12, pattern13, pattern14, pattern15,
                            pattern16, pattern17, pattern18, pattern19, pattern20,
                            pattern21, pattern22, pattern23, pattern24, pattern25,
                            pattern26, pattern27, pattern28, pattern29, pattern30,
                            pattern31, pattern32, pattern33, pattern34, pattern35};
        return patterns;
    }

    private static String[] getTimeFormatPatterns()
    {
        String formatPattern1 = "HHmmss";
        String formatPattern2 = "HHmm";
        String formatPattern3 = "HH";
        String formatPattern4 = "HHmmss,S";
        String formatPattern5 = "HHmmss,SZ";
        String formatPattern6 = "HHmmss,SZ";
        String formatPattern7 = "HHmmss";
        String formatPattern8 = "HHmmssZ";
        String formatPattern9 = "HHmmssZ";
        String formatPattern10 = "HHmm";
        String formatPattern11 = "HHmmZ";
        String formatPattern12 = "HHmmZ";
        String formatPattern13 = "HHmmss'Z'";
        String formatPattern14 = "HHmm'Z'";
        String formatPattern15 = "HH'Z'";
        String formatPattern16 = "HHZ";
        String formatPattern17 = "HHZ";
        String formatPattern18 = "HHmmZ";
        String formatPattern19 = "HHmmZ";
        String formatPattern20 = "HHmmssZ";
        String formatPattern21 = "HHmmssZ";
        String formatPattern22 = "HH:mm";
        String formatPattern23 = "HH:mmZ";
        String formatPattern24 = "HH:mmZ";
        String formatPattern25 = "HH:mmss";
        String formatPattern26 = "HH:mmssZ";
        String formatPattern27 = "HH:mmssZ";
        String formatPattern28 = "HH:mm'Z'";
        String formatPattern29 = "HH:mm:ss";
        String formatPattern30 = "HH:mm:ss,S";
        String formatPattern31 = "HH:mm:ss,SZ";
        String formatPattern32 = "HH:mm:ss,SZ";
        String formatPattern33 = "HH:mm:ss'Z'";
        String formatPattern34 = "HH:mm:ssZ";
        String formatPattern35 = "HH:mm:ssZ";
        String[] formatPatterns = {formatPattern1, formatPattern2, formatPattern3,
                                    formatPattern4, formatPattern5, formatPattern6,
                                    formatPattern7, formatPattern8, formatPattern9,
                                    formatPattern10, formatPattern11, formatPattern12,
                                    formatPattern13, formatPattern14, formatPattern15,
                                    formatPattern16, formatPattern17, formatPattern18,
                                    formatPattern19, formatPattern20, formatPattern21,
                                    formatPattern22, formatPattern23, formatPattern24,
                                    formatPattern25, formatPattern26, formatPattern27,
                                    formatPattern28, formatPattern29, formatPattern30,
                                    formatPattern31, formatPattern32, formatPattern33,
                                    formatPattern34, formatPattern35};
        return formatPatterns;
    }

    private static String[] getDateRegexPatterns()
    {
        String pattern1 = "^((\\+|-)(0+))?\\d\\d\\d\\d\\d\\d\\d\\d$";
        String pattern2 = "^((\\+|-)(0+))?\\d\\d\\d\\d-\\d\\d-\\d\\d$";
        String pattern3 = "^((\\+|-)(0+))?\\d\\d\\d\\d-\\d\\d$";
        String pattern4 = "^((\\+|-)(0+))?\\d\\d\\d\\d$";
        String pattern5 = "^((\\+|-)(0+))?\\d\\d$";
        String pattern6 = "^((\\+|-)(0+))?\\d\\d\\d\\d\\d\\d\\d$";
        String pattern7 = "^((\\+|-)(0+))?\\d\\d\\d\\d-\\d\\d\\d$";
        String pattern8 = "^((\\+|-)(0+))?\\d\\d\\d\\dW\\d\\d\\d$";;
        String pattern9 = "^((\\+|-)(0+))?\\d\\d\\d\\d-W\\d\\d-\\d$";
        String pattern10 = "^((\\+|-)(0+))?\\d\\d\\d\\d-W\\d\\d$";
        String pattern11 = "^((\\+|-)(0+))?\\d\\d\\d\\dW\\d\\d";

        String[] patterns = {pattern1, pattern2, pattern3, pattern4, pattern5,
                            pattern6, pattern7, pattern8, pattern9, pattern10,
                            pattern11};
        return patterns;
    }

    private static String[] getCompleteDateRepresentationRegexPatterns()
    {
        String[] allDatePatterns = getDateRegexPatterns();
        String[] completeRepresentations = {allDatePatterns[0], allDatePatterns[1],
                                            allDatePatterns[5], allDatePatterns[6],
                                            allDatePatterns[7], allDatePatterns[8]};
        return completeRepresentations;
    }

    private static String[] getDateFormatPatterns()
    {
        String dateFormatPattern1 = "yyyyMMdd";
        String dateFormatPattern2 = "yyyy-MM-dd";
        String dateFormatPattern3 = "yyyy-MM";
        String dateFormatPattern4 = "yyyy";
        String dateFormatPattern5 = "yy";
        String dateFormatPattern6 = "yyyyDDD";
        String dateFormatPattern7 = "yyyy-DDD";
        String dateFormatPattern8 = "yyyyMMdd";
        String dateFormatPattern9 = "yyyyMMdd";
        String dateFormatPattern10 = "yyyyMMdd";
        String dateFormatPattern11 = "yyyyMMdd";

        String[] formatPatterns = {dateFormatPattern1, dateFormatPattern2,
                                    dateFormatPattern3, dateFormatPattern4,
                                    dateFormatPattern5, dateFormatPattern6,
                                    dateFormatPattern7, dateFormatPattern8,
                                    dateFormatPattern9, dateFormatPattern10,
                                    dateFormatPattern11};
        return formatPatterns;
    }

    private static String[] getDateTimeRegexPatterns()
    {
        String[] completeDates = getCompleteDateRepresentationRegexPatterns();
        String[] timePatterns = getTimeRegexPatterns();
        String[] patterns = new String[completeDates.length * timePatterns.length];
        int index = 0;
        for (int i = 0; i < completeDates.length; i++)
        {
            for (int j = 0; j < timePatterns.length; j++)
            {
                String datePattern = completeDates[i].substring(0, completeDates[i].length() -1);
                String timePattern = timePatterns[j].substring(1);
                patterns[index] = datePattern + "T" + timePattern;
                index++;
            }
        }
        return patterns;
    }

    private static Date parseISO8601Date(String date, int index)
    {
        if (date.startsWith("+") || date.startsWith("-"))
        {
            date = date.substring(1);
            while (date.length() > 0 && date.charAt(0) == '0')
            {
                date = date.substring(1);
            }
        }
        Date retDate = null;
        // Date patterns 0-6 do not include weeks, so they can be handled simply
        // with SimpleDateFormat
        if (index >= 0 && index < 7)
        {
            String[] dateFormatPatterns = getDateFormatPatterns();
            SimpleDateFormat format = new SimpleDateFormat(dateFormatPatterns[index]);
            try
            {
                return format.parse(date);
            }
            catch (ParseException e)
            {
                if (log.isErrorEnabled())
                {
                    log.error("Unable to parse date: " + date);
                }
            }
        }
        // The remaining date patterns all have weeks, so these need to be parsed
        // differently
        else
        {
            retDate = getModifiedWeekDate(date, index);
        }
        return retDate;
    }

    private static Date parseISO8601DateTime(String dateTime)
    {
        Date retDate = null;
        SimpleDateFormat format = new SimpleDateFormat();
        String[] dateRegexPatterns = getDateRegexPatterns();
        String[] timeRegexPatterns = getTimeRegexPatterns();
        String[] dateFormats = getDateFormatPatterns();
        String[] timeFormats = getTimeFormatPatterns();
        StringTokenizer tokenizer = new StringTokenizer(dateTime, "T");
        String date = tokenizer.nextToken();
        String time = tokenizer.nextToken();
        String dateFormatPattern = "";
        String timeFormatPattern = "";
        RegularExpression regEx = new RegularExpression("");
        int dateIndex = 0;
        for (int i = 0; i < dateRegexPatterns.length; i++)
        {
            regEx.setPattern(dateRegexPatterns[i]);
            if (regEx.matches(date))
            {
                dateIndex = i;
                break;
            }
        }
        Date parsedDate = parseISO8601Date(date, dateIndex);
        dateFormatPattern = dateFormats[dateIndex];
        format.applyPattern(dateFormatPattern);
        date = format.format(parsedDate);

        int timeIndex = 0;
        for (int i = 0; i < timeRegexPatterns.length; i++)
        {
            regEx.setPattern(timeRegexPatterns[i]);
            if (regEx.matches(time))
            {
                timeIndex = i;
                break;
            }
        }

        if (timeIndex == 4 || timeIndex == 6 || timeIndex == 7 || timeIndex == 8 ||
                timeIndex == 9 || timeIndex == 10 || timeIndex == 11 || timeIndex == 15 ||
                timeIndex == 17 || timeIndex == 19 || timeIndex == 22 || timeIndex == 23 ||
                timeIndex == 24 || timeIndex == 25 || timeIndex == 26 || timeIndex == 30 ||
                timeIndex == 31 || timeIndex == 33 || timeIndex == 34)
        {

            time = getModifiedTime(time, timeIndex);
        }
        timeFormatPattern = timeFormats[timeIndex];
        format.applyPattern(dateFormatPattern + "'T'" + timeFormatPattern);
        try
        {
            retDate = format.parse(date + "T" + time);
        }
        catch (ParseException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return retDate;
    }

    private static String getModifiedTime(String time, int index)
    {
        StringTokenizer commaTokenizer = new StringTokenizer(time, ",");
        String modifiedTime = "";

        // Since many of these scenarios are handled the same way, it's
        // best to group them by if statements rather than several
        // duplicated cases in a switch statement
        if (index == 4 || index == 15 || index == 17 || index == 19 ||
                index == 22 || index == 30 || index == 33)
        {
            // Formats are: hhmmss,s+hh hh+hh hhmm+hh hhmmss+hh hh:mm+hh
            // hh:mm:ss,s+hh and hh:mm:ss+hh
            modifiedTime = time + "00";
        }
        else if (index == 6 || index == 9 || index == 24)
        {
            // Formats are: hhmm,m hh,h and hh:mm,m
            String baseTime = commaTokenizer.nextToken();
            String decimal = commaTokenizer.nextToken();
            double decimalValue = Double.parseDouble(decimal)/Math.pow(10.0,
                                (double)(decimal.length() * - 1));
            int extension = (int)decimalValue * 60;
            modifiedTime = baseTime + "" + extension;
        }
        else if (index == 7 || index == 10 || index == 25)
        {
            // Formats are: hhmm,m+hh hh,h+hh hh:mm,m+hh
            String baseTime = commaTokenizer.nextToken();
            String decimalWithOffset = commaTokenizer.nextToken();
            StringTokenizer plusMinusTokenizer = new StringTokenizer(decimalWithOffset, "+-");
            String decimal = plusMinusTokenizer.nextToken();
            String offset = plusMinusTokenizer.nextToken();
            char symbol = decimalWithOffset.charAt(decimalWithOffset.length() - 3);
            double decimalValue = Double.parseDouble(decimal)/Math.pow(10.0,
                                (double)(decimal.length() * - 1));
            int extension = (int)decimalValue * 60;
            modifiedTime = baseTime + "" + extension + "" + symbol + offset + "00";
        }
        else if (index == 8 || index == 11 || index == 26)
        {
            // Formats are: hhmm,m+hhmm hh,h+hhmm hh:mm,m+hhmm
            String baseTime = commaTokenizer.nextToken();
            String decimalWithOffset = commaTokenizer.nextToken();
            StringTokenizer plusMinusTokenizer = new StringTokenizer(decimalWithOffset, "+-");
            String decimal = plusMinusTokenizer.nextToken();
            String offset = plusMinusTokenizer.nextToken();
            char symbol = decimalWithOffset.charAt(decimalWithOffset.length() - 5);
            double decimalValue = Double.parseDouble(decimal)/Math.pow(10.0,
                                (double)(decimal.length() * - 1));
            int extension = (int)decimalValue * 60;
            modifiedTime = baseTime + "" + extension + symbol + offset;
        }
        else if (index == 23 || index == 31 || index == 34)
        {
            // Formats are: hh:mm+hh:mm
            StringTokenizer plusMinusTokenizer = new StringTokenizer(time, "+-");
            String timeComponent = plusMinusTokenizer.nextToken();
            String offset = plusMinusTokenizer.nextToken();
            char symbol = time.charAt(time.length() - 6);
            modifiedTime = timeComponent + symbol + offset.substring(0, 2) + offset.substring(3);
        }
        return modifiedTime;
    }

    private static Date getModifiedWeekDate(String date, int index)
    {
        int year = 0;
        int week = 0;
        int weekDay = 0;
        GregorianCalendar cal = new GregorianCalendar();
        switch (index)
        {
            case 7:
            {
                // Since the regex specified that these values must be digits,
                // there is no need to check for a NumberFormatException
                // Format is YYYYWwwD
                year = Integer.parseInt(date.substring(0, 4));
                week = Integer.parseInt(date.substring(5, 7));
                weekDay = Integer.parseInt(date.substring(7, 8));
                break;
            }
            case 8:
            {
                // Format is YYYY-Www-D
                year = Integer.parseInt(date.substring(0, 4));
                week = Integer.parseInt(date.substring(6, 8));
                weekDay = Integer.parseInt(date.substring(9, 10));
                break;
            }
            case 9:
            {
                // Format is YYYY-Www
                year = Integer.parseInt(date.substring(0, 4));
                week = Integer.parseInt(date.substring(6, 8));
                break;
            }
            case 10:
            {
                // Format is YYYYWww
                year = Integer.parseInt(date.substring(0, 4));
                week = Integer.parseInt(date.substring(5, 7));
                break;
            }
            default:
            {
                break;
            }
        }
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.WEEK_OF_YEAR, week);
        if (weekDay != 0)
        {
            cal.set(Calendar.DAY_OF_WEEK, weekDay);
        }
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        Date retDate = cal.getTime();
        return retDate;
    }
}
