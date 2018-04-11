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

package org.cablelabs.impl.manager.system.mmi;

import org.cablelabs.impl.manager.system.APDUReader;
import org.cablelabs.impl.manager.system.APDUWriter;
import org.cablelabs.impl.manager.system.SystemModule;
import org.cablelabs.impl.manager.system.SystemModuleRegistrarImpl;
import org.cablelabs.impl.manager.system.ui.Dialog;
import org.cablelabs.impl.manager.system.ui.DialogCloseListener;
import org.cablelabs.impl.manager.system.ui.DisplayManager;
import org.ocap.system.SystemModuleRegistrar;
import org.apache.log4j.Logger;

/**
 * The MMISystemModuleHandler provides the framework for a default MMI handler
 * for the OCAP stack. It allows for a provider to easily customize the default
 * MMI handler.
 * 
 * This class is instantiated by the OCAP system module support at startup time.
 * The constructor for this class will register itself as the handler, after
 * which the ready() method will be invoked.
 */
public class ResidentMmiHandlerImpl implements org.ocap.system.SystemModuleHandler
{

    private static final Logger log = Logger.getLogger(ResidentMmiHandlerImpl.class.getName());

    /**
     * Cyclic counter maps for transaction and dialog numbers.
     */
    protected CyclicCounterMap dialogs = new CyclicCounterMap();

    /**
     * Variable to hold the SystemModule instance once the handler has been
     * installed.
     */
    private org.ocap.system.SystemModule mmiModule = null;
    
    private final DisplayManager displayManager;
    private CableCardUrlGetter urlGetter;

    /**
     * MMISystemModuleHandler
     * 
     * This is the constructor for the default MMI handler. The constructor will
     * call the registrar to get itself installed as the MMI handler.
     */
    public ResidentMmiHandlerImpl()
    {
        org.ocap.system.SystemModuleRegistrar.getInstance().registerMMIHandler(this);
        
        displayManager = ((SystemModuleRegistrarImpl) SystemModuleRegistrar.getInstance()).getDisplayManager();
    }
    

    /**
     * Open status codes for sendOpenMMICnf()
     */
    public static final int OPEN_STATUS_OK = 0;

    public static final int OPEN_STATUS_HOST_BUSY = 1;
    public static final int OPEN_STATUS_DISPLAY_TYPE_NOT_SUPPORTED = 2;
    public static final int OPEN_STATUS_NO_MORE_WINDOWS_AVAILABLE = 3;

    
    /**
     * <p>
     * This is a call back method to notify an APDU received from the CableCARD
     * device.
     * </p>
     * <p>
     * For the Private Host Application on the SAS Resource, the
     * SystemModuleHandler is bound to a specific session number (and a specific
     * Private Host Application ID) when it is registered via the
     * {@link SystemModuleRegistrar#registerSASHandler} method. Only the
     * receiveAPDU() method that is bound to the session of the received APDU
     * shall be called only once by the OCAP implementation.
     * </p>
     * <p>
     * For the MMI Resource and the Application Information Resource, the OCAP-J
     * application can receive APDUs for both Resources by a single
     * SystemModuleHandler. The OCAP implementation shall call the receiveAPDU()
     * method of the SystemModuleHandler registered via the
     * {@link SystemModuleRegistrar#registerMMIHandler} method only once for
     * both the MMI and Application Information APDU.
     * </p>
     * <p>
     * The OCAP implementation extract the APDU from an SPDU from the CableCARD
     * device according to the OpenCable CableCARD Interface Specification, and
     * then call this method. Note that the OCAP implementation simply retrieves
     * the field values from the APDU and call this method. No validity check is
     * done by the OCAP implementation. Though SPDU and TPDU mechanism may
     * detect a destruction of the APDU structure while transmitting, the OCAP
     * shall call this method everytime when it receive an APDU. In such case,
     * the parameters may be invalid so that the OCAP-J application can detect
     * an error.
     * </p>
     * <p>
     * Note that if the CableCARD device returns an APDU indicating an error
     * condition, this method is called instead of the sendAPDUFailed() method.
     * </p>
     * <p>
     * This method shall return immediately.
     * <p>
     * 
     * @param apduTag
     *            an apdu_tag value in the APDU coming from the CableCARD
     *            device. I.e., first 3 bytes. If the corresponding bytes are
     *            missed, they are filled by zero. Note that the OCAP
     *            implementation calls this method according to the session
     *            number, so the apdu_tag value may be out of the valid range.
     * 
     * @param lengthField
     *            an length_field value in the APDU coming from the CableCARD
     *            device. The length_field is encoded in ASN.1 BER. If the
     *            corresponding bytes are missed, they are filled by zero.
     * 
     * @param dataByte
     *            an data_byte bytes in the APDU coming from the CableCARD
     *            device. If the corresponding bytes are missed since signalling
     *            trouble, only existing bytes are specified. If they are more
     *            than expected length, all existing bytes are specified. The
     *            APDU consists of the specified apdu_tag, dataByte and
     *            length_field. The APDU format is defined in the OpenCable
     *            CableCARD Interface Specification.
     * 
     */
    public void receiveAPDU(int apduTag, int lengthField, byte[] dataByte)
    {
        APDUReader reader = new APDUReader(dataByte);
        
        // Parse APDU depending on tag value. See OC-SP-CCIF2.0-I20
        // for APDU format details.
        try
        {
            switch (apduTag)
            {
                case MmiApduProtocol.APDU_TAG_OPEN_MMI_REQ:
                {
                    int displayType = reader.getInt(1);
                    int urlLength = reader.getInt(2);
                    byte[] url = reader.getBytes(urlLength);

                    handleOpenMMIReq(displayType, url);
                    break;
                }

                case MmiApduProtocol.APDU_TAG_CLOSE_MMI_REQ:
                {
                    int dialogNumber = reader.getInt(1);

                    handleCloseMMIReq(dialogNumber);
                    break;
                }

                case MmiApduProtocol.APDU_TAG_APPLICATION_INFO_CNF:
                    break; // Ignore this APDU
                    
                default:
                    if (log.isErrorEnabled())
                    {
                        log.error("Received APDU with unknown tag " + Integer.toHexString(apduTag));
                    }
                    break;
            }
        }
        catch (APDUReader.APDUReadException e)
        {
            if (log.isErrorEnabled())
            {
                log.error("Received malformed APDU with tag " + Integer.toHexString(apduTag));
            }
    }
    }

    /**
     * Handle an incoming open_mmi_req APDU.
     * 
     * @param displayType
     *            display type for the dialog
     * 
     * @param url
     *            URL to query for HTML to display
     * 
     */
    protected void handleOpenMMIReq(int displayType, byte[] url)
    {
        if (log.isInfoEnabled())
        {
            log.info("Got open_mmi_req APDU: displayType=" + displayType + "  URL=" + new String(url));
        }

        if(!supportedDisplayType(displayType))
        {
            // Not supported display type            
            sendOpenMMICnf(0, OPEN_STATUS_DISPLAY_TYPE_NOT_SUPPORTED);
        }
        else
        {
            int dialogNumber = dialogs.getNumber();
            if (dialogNumber < 0)
            {
                // All dialog numbers are in use; return busy status.
                sendOpenMMICnf(0, OPEN_STATUS_HOST_BUSY);
            }
            else
            {
                Dialog dialog = createDialog(dialogNumber, displayType, new String(url));
                
                boolean status = displayManager.displayDialog(dialog);
                
                if(status) 
                {
                    dialogs.put(dialogNumber, dialog);
                    sendOpenMMICnf(dialogNumber, OPEN_STATUS_OK);                
                }
                else
                {
                    dialogs.remove(dialogNumber);
                    sendOpenMMICnf(dialogNumber, OPEN_STATUS_NO_MORE_WINDOWS_AVAILABLE);                
                }
            }
        }
    }
    
    protected boolean supportedDisplayType(int displayType)
    {
        return true;
    }

    /**
     * Handle an incoming close_mmi_req APDU.
     * 
     * @param dialogNumber
     *            number of dialog to close
     * 
     */
    protected void handleCloseMMIReq(int dialogNumber)
    {
        if (log.isInfoEnabled())
        {
            log.info("Got close_mmi_req APDU: dialog=" + dialogNumber);
        }

        // Find the dialog in the map.
        Dialog dialog = (Dialog) dialogs.remove(dialogNumber);

        // Notify the dialog that it has been closed.
        if (null != dialog)
        {
            dialog.close();
            displayManager.dismissDialog(dialog);
        }
    }

    /**
     * Construct and send an open_mmi_cnf APDU.
     * 
     * @param dialogNumber
     *            number of dialog being opened.
     * 
     * @param openStatus
     *            OPEN_STATUS constant
     * 
     */
    protected void sendOpenMMICnf(int dialogNumber, int openStatus)
    {
        byte[] apdu = new APDUWriter().putInt(dialogNumber, 1).putInt(openStatus, 1).getData();

        sendAPDU(MmiApduProtocol.APDU_TAG_OPEN_MMI_CNF, apdu);
    }

    /**
     * Construct and send a close_mmi_cnf APDU.
     * 
     * @param dialogNumber
     *            number of dialog being closed.
     * 
     */
    protected void sendCloseMMICnf(int dialogNumber)
    {
        byte[] apdu = new APDUWriter().putInt(dialogNumber, 1).getData();

        sendAPDU(MmiApduProtocol.APDU_TAG_CLOSE_MMI_CNF, apdu);
    }

    /**
     * Create an MMI dialog instance. This method can be overridden to create a
     * customized MMI dialog.
     * 
     * @param dialogNumber
     *            dialog number associated with new dialog
     * 
     * @param displayType
     *            display type for the dialog
     * @param url 
     * 
     * @return new dialog object
     */
    protected Dialog createDialog(final int dialogNumber, int displayType, String url)
    {
        DialogCloseListener closeListener = new DialogCloseListener()
        {            
            public void dialogClosed()
            {
                handleCloseMMIReq(dialogNumber);
                sendCloseMMICnf(dialogNumber);
            }
        };
        
        Dialog dialog = new Dialog(url, "CableCARD Message", DisplayManager.PRIORITY_LOW, urlGetter, closeListener);
        return dialog;
    }

    /**
     * This is a call back method to notify an error has occurred while sending
     * an APDU via the {@link SystemModule#sendAPDU} method. This method shall
     * return immediately.
     * 
     * @param apduTag
     *            an apdu_tag of the APDU that was failed to be sent. This is
     *            the apduTag value specified in the SystemModule.sendAPDU()
     *            method.
     * 
     * @param dataByte
     *            an data_byte of the APDU that was failed to be sent. This is
     *            is dataByte value specified in the SystemModule.sendAPDU()
     *            method.
     * 
     */
    public void sendAPDUFailed(int apduTag, byte[] dataByte)
    {
        // TODO: implement code to handle re-sending the APDU.
        // (CR 41562)
    }

    /**
     * This is a call back method to notify that the SystemModuleHandler is
     * being unregistered and give a chance to do a termination procedure. This
     * method returns after the termination procedure has finished.
     */
    public void notifyUnregister()
    {
        /*
         * After successful unregistration, the resident MMI Resource shall not
         * represent the MMI dialog on the screen. The Host shall close all
         * resident MMI dialog and finalize all transaction related to the MMI
         * dialog. The Host shall send the close_mmi_cnf APDU to the CableCARD
         * to notify MMI dialog closing. If the unregisterMMIHandler() is called
         * or the OCAP-J application that called this method changes its state
         * to Destroyed, the resident MMI Resource can represent the MMI dialog
         * again.
         */

        // TODO: close any current dialogues with the POD MMI
        // application so that the assuming MMI handler can take
        // over. (CR 41560) Note: make sure that when the dialogs are
        // closed, the close APDU is sent before returning from this
        // method.

        // NOTE: this method can block while sending and receiving APDUs.
        
        urlGetter.releaseSession();
        urlGetter = null;
    }

    /**
     * This is a call back method to notify that this SystemModuleHandler is
     * ready to receive an APDU, and returns a SystemModule to send an APDU to
     * the CableCARD device.
     * 
     * @param systemModule
     *            a SystemModule instance corresponding to this
     *            SystemModuleHandler. The returned SystemModule sends an APDU
     *            using the same session that this SystemModuleHandler receives
     *            an APDU. Null is specified, if the OCAP implementation fails
     *            to establish a SAS connection or fails to create an
     *            SystemModule instance due to lack of resource.
     * 
     */
    public void ready(org.ocap.system.SystemModule systemModule)
    {
        mmiModule = systemModule;
        
        urlGetter = CableCardUrlGetter.getSession();
        
        HostCapabilities capabilities = getHostCapabilities();
        sendApplicationInfoRequest(capabilities);
    }

    private void sendApplicationInfoRequest(HostCapabilities capabilities) {
        APDUWriter writer = new APDUWriter();
        
        writer.putInt(capabilities.displayRows, 2);
        writer.putInt(capabilities.displayColumns, 2);
        
        writer.putInt(capabilities.verticalScrolling, 1);
        writer.putInt(capabilities.horizontalScrolling, 1);
        
        writer.putInt(capabilities.displayType, 1);
        writer.putInt(capabilities.dataEntrySupport, 1);
        writer.putInt(capabilities.htmlSupport, 1);
        
        if(capabilities.htmlSupport == HostCapabilities.HTML_SUPPORT_CUSTOM)
        {
            writer.putInt(capabilities.htmlLinkSupport, 1);
            writer.putInt(capabilities.htmlFormSupport, 1);
            writer.putInt(capabilities.htmlTableSupport, 1);
            writer.putInt(capabilities.htmlListSupport, 1);
            writer.putInt(capabilities.htmlImageSupport, 1);
        }

        sendAPDU(MmiApduProtocol.APDU_TAG_APPLICATION_INFO_REQ, writer.getData());        
    }

    protected HostCapabilities getHostCapabilities()
    {
        HostCapabilities capabilities = new HostCapabilities();
        
        capabilities.displayRows = HostCapabilities.DISPLAY_SIZE_UNLIMITED;
        capabilities.displayColumns = 32;
        
        capabilities.verticalScrolling = HostCapabilities.SCROLLING_SUPPORTED;
        capabilities.horizontalScrolling = HostCapabilities.SCROLLING_NOT_SUPPORTED;
        
        capabilities.displayType = HostCapabilities.DISPLAY_TYPE_MAX_WINDOWS;
        capabilities.dataEntrySupport = HostCapabilities.DATA_ENTRY_SUPPORT_LAST_NEXT;
        
        capabilities.htmlSupport = HostCapabilities.HTML_SUPPORT_CUSTOM;
        capabilities.htmlLinkSupport = HostCapabilities.HTML_LINK_SUPPORT_MULTIPLE;
        capabilities.htmlFormSupport = HostCapabilities.HTML_FORM_SUPPORT_NONE;
        capabilities.htmlTableSupport = HostCapabilities.HTML_TABLE_SUPPORT_NONE;
        capabilities.htmlListSupport = HostCapabilities.HTML_LIST_SUPPORT_NONE;
        capabilities.htmlImageSupport = HostCapabilities.HTML_IMAGE_SUPPORT_NONE;

        return capabilities ;
    }

    /**
     * sendAPDU()
     * 
     * This is a convenience method that allows you to send MMI APDUs without
     * needing a separate reference to the MMI SystemModule object. It allows
     * you to use a single instance variable to send and receive MMI APDUs.
     * 
     * @param apduTag
     * @param APDU
     */
    public void sendAPDU(int apduTag, byte[] APDU)
    {
        // Pass on the send...
        if (mmiModule != null)
        {
            mmiModule.sendAPDU(apduTag, APDU);
        }
    }
    
    static class HostCapabilities
    {
        public static final int DISPLAY_SIZE_UNLIMITED = 0xff;
        
        public static final int SCROLLING_SUPPORTED = 0x00;
        public static final int SCROLLING_NOT_SUPPORTED = 0x01;
        
        public static final int DISPLAY_TYPE_MAX_WINDOWS = 0x6f;
        public static final int DATA_ENTRY_SUPPORT_LAST_NEXT = 0x01;
        
        public static final int HTML_SUPPORT_CUSTOM = 0x01;
        
        public static final int HTML_LINK_SUPPORT_MULTIPLE = 0x01;        
        public static final int HTML_FORM_SUPPORT_NONE = 0x00;        
        public static final int HTML_TABLE_SUPPORT_NONE = 0x00;
        public static final int HTML_LIST_SUPPORT_NONE = 0x00;
        public static final int HTML_IMAGE_SUPPORT_NONE = 0x00;
        
        int displayRows;
        int displayColumns;
        
        int verticalScrolling;
        int horizontalScrolling;
        
        int displayType;
        int dataEntrySupport;
        
        int htmlSupport;
        int htmlLinkSupport;
        int htmlFormSupport;
        int htmlTableSupport;
        int htmlListSupport;
        int htmlImageSupport;
    }
}
