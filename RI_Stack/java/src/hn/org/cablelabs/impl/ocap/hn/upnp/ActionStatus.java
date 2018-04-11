/*Copyright (c) 2004-2006, Satoshi Konno Copyright (c) 2005-2006, 
Nokia Corporation Copyright (c) 2005-2006, Theo Beisch Collectively 
the Copyright Owners All rights reserved
 */
package org.cablelabs.impl.ocap.hn.upnp;

/**
 * Implementation of the IActionStatus interface
 * 
 * @author Michael Jastad
 * @version $Revision$
 * 
 * @see
 */
public class ActionStatus
{
    public final static ActionStatus HTTP_CONTINUE =            new ActionStatus(100, "Continue");
    public final static ActionStatus HTTP_OK =                  new ActionStatus(200, "OK");
    public final static ActionStatus HTTP_PARTIAL_CONTENT =     new ActionStatus(206, "Partial Content");
    public final static ActionStatus HTTP_BAD_REQUEST =         new ActionStatus(400, "Bad Request");
    public final static ActionStatus HTTP_UNAUTHORIZED =        new ActionStatus(401, "Unauthorized");
    public final static ActionStatus UPNP_INVALID_ACTION =      new ActionStatus(401, "Invalid Action");
    public final static ActionStatus UPNP_INVALID_ARGUMENTS =   new ActionStatus(402, "Invalid Arguments");
    public final static ActionStatus UPNP_OUT_OF_SYNC =         new ActionStatus(403, "Out of Sync");
    public final static ActionStatus HTTP_NOT_FOUND =           new ActionStatus(404, "Not Found");
    public final static ActionStatus HTTP_NOT_ACCEPTABLE =      new ActionStatus(406, "Not Acceptable");
    public final static ActionStatus HTTP_RANGE_NOT_SATISFIABLE = new ActionStatus(416, "Range Not Satisfiable");
    public final static ActionStatus HTTP_SERVER_ERROR =        new ActionStatus(500, "Server Error");
    public final static ActionStatus HTTP_NOT_IMPLEMENTED =     new ActionStatus(501, "Not Implemented");
    public final static ActionStatus UPNP_FAILED =              new ActionStatus(501, "Action Failed");
    public final static ActionStatus HTTP_VERSION_NOT_SUPPORTED = new ActionStatus(505, "HTTP Version Not Supported");
    public final static ActionStatus HTTP_UNAVAILABLE =         new ActionStatus(503, "Unavailable");
    public final static ActionStatus UPNP_ARGUMENT_INVALID =    new ActionStatus(600, "Argument Value Invalid");
    public final static ActionStatus UPNP_UNSUPPORTED_ACTION =  new ActionStatus(602, "Optional Action Not Implemented");
    public final static ActionStatus UPNP_UNAUTHORIZED =        new ActionStatus(606, "Unauthorized Access");
    public final static ActionStatus UPNP_NO_SUCH_OBJECT =      new ActionStatus(701, "No such object");
    public final static ActionStatus UPNP_SRS_INVALID_SYNTAX =  new ActionStatus(701, "Invalid Syntax");
    public final static ActionStatus UPNP_RUI_REJECT_OP =       new ActionStatus(701, "Operation Rejected");
    public final static ActionStatus UPNP_INVALID_ID =          new ActionStatus(701, "Invalid ID");
    public final static ActionStatus UPNP_INVALID_CURR_TAG =    new ActionStatus(702, "Invalid currentTagValue");
    public final static ActionStatus UPNP_INVALID_NEW_TAG =     new ActionStatus(703, "Invalid newTagValue");
    public final static ActionStatus UPNP_SRS_INVALID_VALUE =   new ActionStatus(703, "Invalid Value");
    public final static ActionStatus UPNP_TEST_ALREADY_ACTIVE =   new ActionStatus(703, "Test Already Active");
    public final static ActionStatus UPNP_REQUIRED_TAG =        new ActionStatus(704, "Required tag");
    public final static ActionStatus UPNP_NO_SUCH_RECORD_SCHEDULE_TAG = new ActionStatus(704, "Record schedule does not exist");
    public final static ActionStatus UPNP_RECORDTASK_ACTIVE =   new ActionStatus(705, "RecordTask active");
    // Adding additional error as per Table 2-20 Error code for UpdateObject() - UPnP-av-ContentDirectory-v3-Service 
    public final static ActionStatus UPNP_PARAMETER_MISMATCH =  new ActionStatus(706, "Parameter Mismatch");
    public final static ActionStatus UPNP_INVALID_CONNECTION =  new ActionStatus(706, "Invalid connection reference");
    public final static ActionStatus UPNP_NO_SUCH_TEST =  new ActionStatus(706, "No Such test");
    public final static ActionStatus UPNP_WRONG_TEST_TYPE =  new ActionStatus(707, "Wrong Test Type");
    // Adding additional error as per Table 2-20 Error code for CreateRecordSchedule() - UPnP-av-ScheduledRecording-v1-Service 
    public final static ActionStatus UPNP_SRS_MISSING_MANDATORY = new ActionStatus(708, "Required property missing");
    public final static ActionStatus UPNP_BAD_SEARCH_CRITERIA = new ActionStatus(708, "Unsupported or invalid search criteria");
    public final static ActionStatus UPNP_INVALID_TEST_STATE = new ActionStatus(708, "Invalid Test State");
    public final static ActionStatus UPNP_BAD_SORT_CRITERIA =   new ActionStatus(709, "Unsupported or invalid sort criteria");    
    public final static ActionStatus UPNP_STATE_PRECLUDES_CANCEL =   new ActionStatus(709, "State Precludes Cancel");    
    public final static ActionStatus UPNP_NO_SUCH_CONTAINER =   new ActionStatus(710, "No such container");
    public final static ActionStatus UPNP_RESTRICTED_OBJECT =   new ActionStatus(711, "Restricted object");
    public final static ActionStatus UPNP_INVALID_DATATYPE =    new ActionStatus(711, "Invalid data type");    
    public final static ActionStatus UPNP_BAD_METADATA =        new ActionStatus(712, "Bad Metadata");
    public final static ActionStatus UPNP_RESTRICT_PARENT_OBJ = new ActionStatus(713, "Restricted parent object");
    public final static ActionStatus UPNP_RECORDTASK_NOT_FOUND =new ActionStatus(713, "Record task not found");
    public final static ActionStatus UPNP_NON_HOMOGENEOUS_IDS = new ActionStatus(714, "Non homogeneous ids");
    public final static ActionStatus UPNP_NO_SUCH_RESOURCE =    new ActionStatus(714, "No such resource");
    public final static ActionStatus UPNP_ACCESS_DENIED =       new ActionStatus(715, "Source resource access denied");
    public final static ActionStatus UPNP_CANNOT_PROCESS =      new ActionStatus(720, "Cannot process the request");
    public final static ActionStatus UPNP_RECORDTASK_DONE =     new ActionStatus(741, "Record task done");    

    /**
     * contains the status code
     */
    private int m_code = 0;

    /**
     * contains the description
     */
    private String m_description = "";

    /**
     * Creates a new CActionStatus object.
     * 
     * @param c
     *            int value defines status Code
     * @param d
     *            String value defines status Description
     */
    public ActionStatus(int c, String d)
    {
        setCode(c);
        setDescription(d);
    }

    /**
     * Returns the Status Code
     * 
     * @return int representing the status code.
     */
    public int getCode()
    {
        return m_code;
    }

    /**
     * Returns the Status Description
     * 
     * @return String representing the status description
     */
    public String getDescription()
    {
        return m_description;
    }

    /**
     * Sets the Status code
     * 
     * @param code
     *            The value of the status code
     */
    public void setCode(int code)
    {
        m_code = code;
    }

    /**
     * Sets the status description
     * 
     * @param desc
     *            The status description
     */
    public void setDescription(String desc)
    {
        m_description = desc;
    }
}
