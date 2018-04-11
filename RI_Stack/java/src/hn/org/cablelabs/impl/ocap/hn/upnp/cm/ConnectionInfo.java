/*Copyright (c) 2004-2006, Satoshi Konno Copyright (c) 2005-2006, 
Nokia Corporation Copyright (c) 2005-2006, Theo Beisch Collectively 
the Copyright Owners All rights reserved
 */
package org.cablelabs.impl.ocap.hn.upnp.cm;

import org.cablelabs.impl.media.streaming.session.data.HNStreamProtocolInfo;

public class ConnectionInfo
{
    // //////////////////////////////////////////////
    // Constants
    // //////////////////////////////////////////////

    public final static String INPUT = "Input";

    public final static String OUTPUT = "Output";

    public final static String OK = "OK";

    public final static String UNKNOWN = "Unknown";

    // //////////////////////////////////////////////
    // Constructor
    // //////////////////////////////////////////////

    public ConnectionInfo(int cid, int rcsid, int avtid, HNStreamProtocolInfo info, String peerConnMgr, int peerConnId)
    {
        setID(cid);
        setRcsID(rcsid);
        setAVTransportID(avtid);
        setProtocolInfo(info.getAsString());
        setPeerConnectionManager(peerConnMgr);
        setPeerConnectionID(peerConnId);
        setDirection(OUTPUT);
        setStatus(UNKNOWN);

    }

    // //////////////////////////////////////////////
    // ID
    // //////////////////////////////////////////////

    private int id;

    public void setID(int value)
    {
        id = value;
    }

    public int getID()
    {
        return id;
    }

    // //////////////////////////////////////////////
    // RcsID
    // //////////////////////////////////////////////

    private int rcsId;

    public void setRcsID(int value)
    {
        rcsId = value;
    }

    public String getRcsIDStr()
    {
        return Integer.toString(rcsId);
    }

    // //////////////////////////////////////////////
    // AVTransportID
    // //////////////////////////////////////////////

    private int transId;

    public void setAVTransportID(int value)
    {
        transId = value;
    }

    public String getAVTransportIDStr()
    {
        return Integer.toString(transId);
    }

    // //////////////////////////////////////////////
    // ProtocolInfo
    // //////////////////////////////////////////////

    private String protocolInfo;

    public void setProtocolInfo(String value)
    {
        protocolInfo = value;
    }

    public String getProtocolInfo()
    {
        return protocolInfo;
    }

    // //////////////////////////////////////////////
    // PeerConnectionManager
    // //////////////////////////////////////////////

    private String peerConnectionManager;

    public void setPeerConnectionManager(String value)
    {
        peerConnectionManager = value;
    }

    public String getPeerConnectionManager()
    {
        return peerConnectionManager;
    }

    // //////////////////////////////////////////////
    // PeerConnectionID
    // //////////////////////////////////////////////

    private int peerConnectionID;

    public void setPeerConnectionID(int value)
    {
        peerConnectionID = value;
    }

    public String getPeerConnectionIDStr()
    {
        return Integer.toString(peerConnectionID);
    }

    // //////////////////////////////////////////////
    // Direction
    // //////////////////////////////////////////////

    private String direction;

    public void setDirection(String value)
    {
        direction = value;
    }

    public String getDirection()
    {
        return direction;
    }

    // //////////////////////////////////////////////
    // Status
    // //////////////////////////////////////////////

    private String status;

    public void setStatus(String value)
    {
        status = value;
    }

    public String getStatus()
    {
        return status;
    }

    public String toString()
    {
        return "ConnectionInfo - direction: " + direction + ", status: " + status + ", protocolInfo: " + protocolInfo + ", id: " + id;
    }
}
