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

#include <ri_cablecard.h>
#include <net_utils.h>
#include <ri_log.h>
#include <DumpApdu.h>

#include <glib.h>
#include <inttypes.h>

#include <ri_config.h>

#include "genericfeatures.h"
#include "sectionutils.h"
#include "sas.h"
#include "test_interface.h"

#define RILOG_CATEGORY cablecardLogCat
log4c_category_t *cablecardLogCat = NULL;

#define MAX_CABLECARD_SESSIONS            4
#define MAX_CABLECARD_CALLBACKS           10
#define NUM_GF_SUPPORTED_GENERIC_FEATURES 12
#define TEST_MMI_DIALOG "http://127.0.0.1/test.html"

#define APP_INFO_REQ_APDU_TAG 0x9F8020
#define APP_INFO_CNF_APDU_TAG 0x9F8021
#define SERVER_QUERY_APDU_TAG 0x9F8022
#define SERVER_REPLY_APDU_TAG 0x9F8023

#define OPEN_MMI_REQ_APDU_TAG 0x9F8820
#define OPEN_MMI_CNF_APDU_TAG 0x9F8821
#define CLOSE_MMI_REQ_APDU_TAG 0x9F8822
#define CLOSE_MMI_CNF_APDU_TAG 0x9F8823

typedef struct
{
    uint32_t eventType;
    ri_cablecard_session_callback_f callback;
    void* data;
    ri_session_id id;
    GMutex* mutex;
    GCond* cond;
} SasData;

typedef struct
{
    ri_cablecard_session_callback_f callback;
    void* data;
    GMutex* mutex;
} MmiData;

typedef struct
{
    ri_cablecard_session_callback_f callback;
    void* data;
    GMutex* mutex;
} AiData;

typedef struct
{
    uint8_t appID[8];
    uint32_t sessionNb;
    //SessionKey encrypt;
    //SessionKey signature;
} Session;

struct cablecard_data_s
{
    uint16_t vct_id;
    uint32_t cfd_AppId;
    int cfd_SD;

    GStaticMutex mutex;
    ri_cablecard_callback_f cablecard_callback[MAX_CABLECARD_CALLBACKS];
    void* callback_data[MAX_CABLECARD_CALLBACKS];

    SasData sasSession[MAX_CABLECARD_SESSIONS];
    MmiData dialogMmiSession;
    MmiData broadcastMmiSession;
    AiData  aiSession;

} cablecard_data;

ri_cablecard_generic_feature
        supported_generic_features[NUM_GF_SUPPORTED_GENERIC_FEATURES] =
        { RI_CCARD_GF_RF_OUTPUT_CHANNEL, RI_CCARD_GF_PC_PIN,
                RI_CCARD_GF_PC_SETTINGS, RI_CCARD_GF_TIME_ZONE,
                RI_CCARD_GF_DAYLIGHT_SAVINGS, RI_CCARD_GF_LANGUAGE,
                RI_CCARD_GF_RATING_REGION, RI_CCARD_GF_RESET_PIN,
                RI_CCARD_GF_CABLE_URLS, RI_CCARD_GF_EA_LOCATION_CODE,
                RI_CCARD_GF_VCT_ID, RI_CCARD_GF_TURN_ON_CHANNEL };

//
// getApduInfo
// 
// get the APDU tag, size, and beginning of data from the APDU buffer
//
static void getApduInfo(uint8_t *apdu, uint8_t **data, size_t *len, uint32_t *tag)
{
    RILOG_DEBUG("%s(%p, %p, %p, %p) - Entry\n", __func__, apdu, data, len, tag);

    if (NULL == apdu || NULL == data || NULL == len || NULL == tag)
    {
        RILOG_ERROR("%s input argument error!?\n", __func__);
        return;
    }

    if (apdu[3] > 0x7F)
    {
        if (apdu[3] == 0x82)
        {
            *len |= (apdu[4] << 8);
            *len |= apdu[5];
            *data = &apdu[6];
        }
        else if (apdu[3] == 0x81)
        {
            *len |= apdu[4];
            *data = &apdu[5];
        }
        else
        {
            RILOG_ERROR("%s unsupported APDU length!?\n", __func__);
        }
    }
    else
    {
        *len = apdu[3];
        *data = &apdu[4];
    }

    *tag = (apdu[0] << 16 | apdu[1] << 8 | apdu[2]);
    RILOG_INFO("%s APDU(%X) length: %d\n", __func__, *tag, *len);
    hex_dump(apdu, *len + 4);

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

static uint8_t *createApdu(uint32_t apduTag,
                           uint8_t *data, size_t dataLen, size_t *apduLen)
{
    uint8_t *retData = NULL, *p;

    RILOG_DEBUG("%s(%p, %d) Entry\n", __func__, data, dataLen);

    // tag + apduLen + apduDataLen
    *apduLen = 3 + 3 + dataLen;
 
    p = retData = g_try_malloc0(*apduLen);
    if (NULL == retData)
    {
        RILOG_FATAL(-1, "line %d of %s, %s memory allocation failure!\n",
                    __LINE__, __FILE__, __func__);
    }

    *p++ = (apduTag >> 16) & 0xFF;
    *p++ = (apduTag >> 8) & 0xFF;
    *p++ = (apduTag & 0xFF);

    if (dataLen > 0x7F)
    {
        if (dataLen > 0xFF && dataLen <= 0xFFFF)
        {
            *p++ = 0x82;
            *p++ = (dataLen >> 8) & 0xFF;
            *p++ = (dataLen & 0xFF);
        }
        else if (dataLen <= 0xFF)
        {
            *p++ = 0x81;
            *p++ = (dataLen & 0xFF);
        }
        else
        {
            RILOG_ERROR("%s unsupported APDU length!?\n", __FUNCTION__);
        }
    }
    else
    {
        *p++ = dataLen;
    }

    memcpy(p, data, dataLen);
    *apduLen = dataLen + (p - retData);

    RILOG_INFO("%s -- Exit (%p)\n", __FUNCTION__, retData);
    return retData;
}

ri_bool OpenMmiUrl(uint8_t displayType, char* url)
{
    uint16_t urlLength = 0;
    size_t len = 0;
    uint8_t *apdu = NULL;
    uint8_t *data = NULL;
    uint8_t *p = NULL;
    MmiData *pMmi = &cablecard_data.broadcastMmiSession;

    if (NULL != url)
    {
        RILOG_INFO("%s (%X, %s);\n", __FUNCTION__, displayType, url);
        // Packing the binary event data buffer...
        //
        // data sent with a OPEN_MMI event is a buffer containing:
        //      display type (DIALOGBOX_MMI or FULLSCREEN_MMI)
        //      URL_length (length of URL to follow)
        //      URL (uint_8[URL_length])
        urlLength = strlen(url);
        p = data = g_try_malloc0(1 + 2 + urlLength); // type + urlLen + url

        if (NULL == data)
        {
            RILOG_FATAL(-1, "line %d of %s, %s memory allocation failure!\n",
                        __LINE__, __FILE__, __func__);
        }

        *p++ = displayType;
        *p++ = (urlLength >> 8) & 0xFF;
        *p++ = (urlLength & 0xFF);
        memcpy(p, url, urlLength);
        apdu = createApdu(OPEN_MMI_REQ_APDU_TAG, data, urlLength + (p - data), &len);

        if (NULL != pMmi->callback)
        {
            RILOG_INFO("%s sending APDU %X size %d\n", __func__,
                       OPEN_MMI_REQ_APDU_TAG, len);
            pMmi->callback(RI_CCARD_EVENT_APDU_RECV, pMmi,
                           apdu, len, pMmi->data);
            dumpApdu(apdu, len);
        }

        return TRUE;
    }
    else
    {
        RILOG_ERROR("%s NULL URL!\n", __func__);
    }

    return FALSE;
}

#define gfMenu \
    "\r\n" \
    "|---+-----------------------\r\n" \
    "| g | Get value\r\n" \
    "|---+-----------------------\r\n" \
    "| s | Set value\r\n"

int gfKeyHandler(int sock, char *rxBuf, int *retCode, char **retStr)
{
    char buf[1024];
    char line[512];
    char *result = NULL;
    int key = toupper(rxBuf[0]);

    *retCode = MENU_SUCCESS;

    switch (key)
    {
    case 'G':
        test_SendString(sock, "\r\nGet value...\r\n");
        if (test_GetString(sock, line, sizeof(line), "\r\n\nname: "))
        {
            if (NULL == (result = GetValue(line)))
            {
                RILOG_ERROR("%s - couldn't GetValue!\n", __func__);
            }
            else
            {
                gsize len = 0;
                uint8_t *val = g_base64_decode(result, &len);
                RILOG_INFO("%s -- value:%p\n", __FUNCTION__, val);
                hex_dump(val, len);
                g_free(result);
                g_free(val);
            }
        }
        else
        {
            test_SendString(sock, "\r\nGETS error!?\r\n");
        }
        break;
    case 'S':
        test_SendString(sock, "\r\nSet value...\r\n");
        (void)test_GetString(sock, buf, sizeof(buf), "\r\n\ndialogRequest: ");
        sscanf(buf, "%d", &key);
        test_SendString(sock, "\r\n  name: ");

        if (0 < test_GetString(sock, buf, sizeof(buf), "\r\n\nname: "))
        {
            if (0 < test_GetString(sock, line, sizeof(line), "\r\n\nvalue: "))
            {
                if (FALSE == SetValue(key, buf, line))
                {
                    RILOG_ERROR("%s -- SetValue failed\n", __FUNCTION__);
                }
                else
                {
                    RILOG_INFO("%s -- SetValue %s = %s SUCCESS!\n",
                               __FUNCTION__, buf, line);
                }
                return 0;
            }
        }
        test_SendString(sock, "\r\nGETS error!?\r\n");
        break;
    case 'X':
        return -1;
    default:
        *retCode = MENU_INVALID;
        break;
    }
 
    return 0;
}

#define mmiMenu \
    "\r\n" \
    "|---+-----------------------\r\n" \
    "| o | Open MMI dialog\r\n" \
    "|---+-----------------------\r\n" \
    "| c | Close MMI dialog\r\n" \

int mmiKeyHandler(int sock, char *rxBuf, int *retCode, char **retStr)
{
    char buf[1024] = {0};
    int key = toupper(rxBuf[0]);
    uint8_t displayType = 0;
    uint8_t dialogNumber = 0;
    size_t len = 0;
    uint8_t *data = NULL;
    uint8_t *apdu = NULL;
    uint8_t *p = NULL;
    MmiData *pMmi = &cablecard_data.broadcastMmiSession;

    *retCode = MENU_SUCCESS;

    switch (key)
    {
    case 'O':
        test_SendString(sock, "\r\nOpen MMI...");
        (void)test_GetString(sock, buf, sizeof(buf), "\r\n\ndisplayType: ");
        sscanf(buf, "%d", (int*)&displayType);
        OpenMmiUrl(displayType, TEST_MMI_DIALOG);
        break;
    case 'C':
        test_SendString(sock, "\r\nClose MMI...\r\n");
        (void)test_GetString(sock, buf, sizeof(buf), "\r\n\ndialogNumber: ");
        sscanf(buf, "%d", (int*)&dialogNumber);
 
        // Packing the binary event data buffer...
        //
        // data sent with a CLOSE_MMI event is a buffer containing:
        //      dialog number to close
        p = data = g_try_malloc0(1);

        if (NULL == data)
        {
            RILOG_FATAL(-1, "line %d of %s, %s memory allocation failure!\n",
                        __LINE__, __FILE__, __func__);
        }

        *p++ = dialogNumber;
        apdu = createApdu(CLOSE_MMI_REQ_APDU_TAG, data, p - data, &len);

        if (NULL != pMmi->callback)
        {
            RILOG_INFO("%s sending APDU %X size %d\n",
                       __func__, CLOSE_MMI_REQ_APDU_TAG, len);
            pMmi->callback(RI_CCARD_EVENT_APDU_RECV, pMmi,
                           apdu, len, pMmi->data);
            dumpApdu(apdu, len);
        }
        break;
    case 'X':
        return -1;
    default:
        *retCode = MENU_INVALID;
        break;
    }
 
    return 0;
}

#define ccMenu \
   "\r\n" \
    "|---+-----------------------\r\n" \
    "| g | GF test\r\n" \
    "|---+-----------------------\r\n" \
    "| m | MMI test\r\n" \

int ccKeyHandler(int sock, char *rxBuf, int *retCode, char **retStr)
{
    int key = toupper(rxBuf[0]);

    *retCode = MENU_SUCCESS;

    switch (key)
    {
    case 'G':
        if (!test_SetNextMenu(sock, test_FindMenu("GF")))
        {
            RILOG_ERROR("%s -- GF sub-menu failed?\n", __FUNCTION__);
            *retCode = MENU_FAILURE;
            break;
        }
        else
        {
            return 1;
        }
    case 'M':
        if (!test_SetNextMenu(sock, test_FindMenu("MMI")))
        {
            RILOG_ERROR("%s -- MMI sub-menu failed?\n", __FUNCTION__);
            *retCode = MENU_FAILURE;
            break;
        }
        else
        {
            return 1;
        }
    case 'X':
        return -1;
    default:
        *retCode = MENU_INVALID;
        break;
    }

    return 0;
}

static MenuItem gfMenuItem =
{ FALSE, "g", "GF", gfMenu, gfKeyHandler };
static MenuItem mmiMenuItem =
{ FALSE, "m", "MMI", mmiMenu, mmiKeyHandler };
static MenuItem ccMenuItem =
{ TRUE, "c", "CableCARD", ccMenu, ccKeyHandler };

static SasData *findSasBySessionNb(uint32_t sessionNb)
{
    int i;

    for(i = 0; i < MAX_CABLECARD_SESSIONS; i++)
    {
        SasData *pSas = &cablecard_data.sasSession[i];

        if(pSas->id && ((Session *)pSas->id)->sessionNb == sessionNb)
        {
            RILOG_INFO("%s %X found at index %d\n", __FUNCTION__, sessionNb, i);
            return pSas;
        }
    }

    RILOG_INFO("%s %X not found\n", __FUNCTION__, sessionNb);
    return NULL;
}

static SasData *findSasByAppId(uint8_t *appID)
{
    int i;

    for(i = 0; i < MAX_CABLECARD_SESSIONS; i++)
    {
        SasData *pSas = &cablecard_data.sasSession[i];

        if(pSas->id && memcmp(((Session *)pSas->id)->appID, appID, 8) == 0)
        {
            RILOG_INFO("%s %02X%02X %02X%02X %02X%02X %02X%02X found at %d\n",
                        __FUNCTION__, appID[0], appID[1], appID[2], appID[3],
                        appID[4], appID[5], appID[6], appID[7], i);
            return pSas;
        }
    }

    RILOG_INFO("%s %02X%02X %02X%02X %02X%02X %02X%02X not found\n",
                __FUNCTION__, appID[0], appID[1], appID[2], appID[3],
                appID[4], appID[5], appID[6], appID[7]);
    return NULL;
}

static SasData *findSasUnused(void)
{
    int i;

    for(i = 0; i < MAX_CABLECARD_SESSIONS; i++)
    {
        SasData *pSas = &cablecard_data.sasSession[i];

        if(NULL == pSas->id)
        {
            RILOG_INFO("%s using SAS session at index %d\n", __FUNCTION__, i);
            return pSas;
        }
    }

    RILOG_WARN("%s all SAS %d sessions in use!\n", __FUNCTION__, i);
    return NULL;
}

uint8_t* createAppInfoCnfApdu(uint16_t manufID, uint16_t version, uint8_t *macAddress,
                              uint8_t serialNumLen, uint8_t *serialNum,
                              uint8_t appLen, uint8_t* apps,
                              size_t *apduLen)
{
    size_t appInfoCnfLen = 0;
    uint8_t *appInfoCnfData, *p, *retData;

    RILOG_DEBUG("%s(%d, %d, %p, %d, %p, %d, %p) Entry\n",
                __func__, manufID, version, macAddress,
                serialNumLen, serialNum, appLen, apps);

    // manufID + version + macAddress (always 6) + serialNumLen + serialNum + appLen
    appInfoCnfLen = 2 + 2 + 6 + 1 + serialNumLen + appLen;

    appInfoCnfData = p = g_try_malloc0(appInfoCnfLen);
    if (NULL == appInfoCnfData)
    {
        RILOG_FATAL(-1, "line %d of %s, %s memory allocation failure!\n",
                    __LINE__, __FILE__, __func__);
    }

    *p++ = (manufID >> 8) & 0xFF;       // manufacturer_id
    *p++ = manufID & 0xFF;
    *p++ = (version >> 8) & 0xFF;       // manufacturer_id
    *p++ = version & 0xFF;
    memcpy(p, macAddress, 6);           // MAC address
    p+= 6;
    *p++ = serialNumLen & 0xFF;         // Serial number length
    memcpy(p, serialNum, serialNumLen); // Serial number
    p+= serialNumLen;
    *p++ = *apps++ & 0xFF;               // Num apps is first byte in data
    memcpy(p, apps, appLen-1);           // Apps

    // Form the APDU and return it
    retData = createApdu(APP_INFO_CNF_APDU_TAG, appInfoCnfData, appInfoCnfLen, apduLen);
    g_free(appInfoCnfData);
    return retData;
}

uint8_t* createServerReplyApdu(uint8_t trans, uint8_t status,
                               uint8_t* data, size_t dataLen, size_t *apduLen)
{
    size_t serverReplyLength = 0;
    uint16_t header_length = 0;   // no HTTP optional header
    uint8_t *serverReplyData, *p, *retData;

    RILOG_DEBUG("%s(%d, %d, %p, %d) Entry\n",
                __func__, trans, status, data, dataLen);

    // transaction + status + headerLen (always 0) + fileLen + file
    serverReplyLength = 1 + 1 + 2 + 2 + dataLen;

    serverReplyData = p = g_try_malloc0(serverReplyLength);
    if (NULL == serverReplyData)
    {
        RILOG_FATAL(-1, "line %d of %s, %s memory allocation failure!\n",
                    __LINE__, __FILE__, __func__);
    }

    *p++ = trans;                       // transaction_number
    *p++ = status;                      // file_status
    *p++ = (header_length >> 8) & 0xFF; // header_length (always 0)
    *p++ = (header_length & 0xFF);
    *p++ = (dataLen >> 8) & 0xFF;       // file_length
    *p++ = (dataLen & 0xFF);
    memcpy(p, data, dataLen);           // file bytes

    // Form the APDU and return it
    retData = createApdu(SERVER_REPLY_APDU_TAG, serverReplyData, serverReplyLength, apduLen);
    g_free(serverReplyData);
    return retData;
}

uint8_t *localhost_MMI(uint8_t transaction, char *url, size_t *apduLen)
{
    uint8_t file_status = 1;      // not found
    char *data = "<br/><b><i><p align=\"center\">unknown request</p></i></b>";
    size_t dataLen = strlen(data);

    RILOG_DEBUG("%s(%d, %p) Entry\n", __func__, transaction, url);

    if (strstr(url, "test.html"))
    {
        data = "<br/><b><i><p align=\"center\">hello world</p></i></b>"
               "<br/><br/><a align=\"center\" "
               "href=\"http://127.0.0.1/test-p2.html\">p2 Link</a>"
               "<br/><br/><a align=\"center\" "
               "href=\"http://127.0.0.1/test-p3.html\">p3 Link</a>"
               "<br/><br/><button defaultFocus=\"true\" align=\"center\" "
               "href=\"http://127.0.0.1/test-p2.html\">Next</button>";
        dataLen = strlen(data);
        file_status = 0;      // OK
    }
    else if (strstr(url, "test-p2.html"))
    {
        data = "<br/><b><i><p align=\"center\">goodbye world</p></i></b>"
               "<br/><br/><a align=\"center\" "
               "href=\"http://127.0.0.1/test-p3.html\">p3 Link</a>";
        dataLen = strlen(data);
        file_status = 0;      // OK
    }

    RILOG_TRACE("%s -- Exit after call to replyApdu()\n", __FUNCTION__);
    return createServerReplyApdu(transaction, file_status, (uint8_t*)data, dataLen, apduLen);
}

uint8_t *cablecard_MMI(uint8_t transaction, char* url, size_t *apduLen)
{
    uint8_t *ret = NULL;
    char *data = "<br/><b><i><p align=\"left\">denied</p></i></b>";
    size_t dataLen = strlen(data);
    uint8_t file_status = 2;      // URL access not granted
    char buf[MAXURLLEN];

    RILOG_DEBUG("%s(%d, %p, %u) Entry\n", __func__, transaction, url, dataLen);
    sprintf(buf, "GF:/NV/Resource/%s", url);

    if (NULL != (data = GetValue(buf)))
    {
        file_status = 0;      // OK
        dataLen = strlen(data);
    }

    ret = createServerReplyApdu(transaction, file_status, (uint8_t*)data, dataLen, apduLen);
    g_free(data);
    RILOG_TRACE("%s -- Exit, returning: %s\n", __FUNCTION__, ret);
    return ret;
}

/**
 * Registers the given callback function to receive events related to CableCARD
 * status and the Generic Features resource
 *
 * @param event_func the function that will receive CableCARD events
 * @param data callback data to be passed on every invocation of event_func
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *     RI_ERROR_ILLEGAL_ARG: Illegal or NULL arguments specified
 */
ri_error ri_cablecard_register_for_events(ri_cablecard_callback_f event_func,
        void* data)
{
    int i = 0;
    ri_error retval = RI_ERROR_NONE;

    RILOG_INFO("%s -- Entry\n", __FUNCTION__);

    g_static_mutex_lock(&(cablecard_data.mutex));
    for (i = 0; i < MAX_CABLECARD_CALLBACKS; i++)
    {
        if (cablecard_data.cablecard_callback[i] == NULL)
        {
            cablecard_data.cablecard_callback[i] = event_func;
            cablecard_data.callback_data[i] = data;
            RILOG_INFO("%s -- Added callback %p, data %p at position %d\n",
                    __FUNCTION__, event_func, data, i);
            break;
        }
    }
    g_static_mutex_unlock(&(cablecard_data.mutex));

    if (i == MAX_CABLECARD_CALLBACKS)
    {
        RILOG_ERROR(
                "%s -- Array is full - unable to add callback %p, data %p\n",
                __FUNCTION__, event_func, data);
        retval = RI_ERROR_OUT_OF_RESOURCES;
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);

    return retval;
}

void ri_cablecard_send_event_for_callback(ri_cablecard_event event)
{
	int i = 0;
    for (i = 0; i < MAX_CABLECARD_CALLBACKS; i++)
    {
        if (cablecard_data.cablecard_callback[i] != NULL)
        {
            cablecard_data.cablecard_callback[i](event, NULL,
            cablecard_data.callback_data[i]);
        }
    }
}

/**
 * Returns whether or not the card is inserted
 *
 * @return TRUE if the card is inserted, FALSE otherwise
 */
ri_bool ri_cablecard_is_inserted()
{
    ri_bool retVal = FALSE;
    char *result = NULL;

    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    if(NULL != (result = GetValue("GF:/NV/Variable/Card_status")))
    {
        if (strstr(result, "Inserted") != NULL)
        {
            retVal = TRUE;
        }
        else if (strstr(result, "Removed") == NULL)
        {
            RILOG_WARN("%s -- Unexpected value returned: %s\n",
                       __FUNCTION__, result);
        }

        g_free(result);
    }

    RILOG_DEBUG("%s -- returning %s\n", __FUNCTION__, boolStr(retVal));
    return retVal;
}

/**
 * Returns whether or not the card is inserted and ready
 *
 * @return TRUE if the card is inserted and ready, FALSE otherwise
 */
ri_bool ri_cablecard_is_ready()
{
    ri_bool result = FALSE;
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);
    // Looks like GF does not support "is_ready" functionality:
    // just assume is_inserted => is_ready...
    result = ri_cablecard_is_inserted();
    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return result;
}

/**
 * Lookup a host by name
 *
 * @param name for lookup
 *
 * @return pointer to structure containing host entry or NULL on ERROR
 */
struct hostent *ri_cablecard_gethostbyname(const char *name)
{
    char *ipAddr = NULL;
    struct hostent *pHE = NULL;
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);
    // TODO: verify IPv6 compatibility...
    pHE = gethostbyname(name);

    if (NULL == (ipAddr = ricfg_getValue("RIPlatform", "RI.Platform.IpAddr")))
    {
        ipAddr = "127.0.0.1";
        RILOG_WARN("%s -- RI Platform IP address not specified!\n", __func__);
    }

    if (NULL != pHE)
    {
        int i;
        RILOG_DEBUG("%s: name = %s, h_length = %d\n",
                    __func__, name, pHE->h_length);

        for(i = 0; NULL != pHE->h_addr_list[i]; i++)
        {
            RILOG_DEBUG("%s: addr[%d] = %x\n",
                        __func__, i+1, *(int*)pHE->h_addr_list[i]);
        }

        if (NULL != pHE->h_addr_list[0])
        {
                *(int*)pHE->h_addr_list[0] = inet_addr(ipAddr);
                pHE->h_addr_list[1] = NULL;
                RILOG_INFO("%s:  reset addr[0] to %x\n",
                            __func__, *(int*)pHE->h_addr_list[0]);
        }
    }
    else
    {
        RILOG_ERROR("%s: error occured for %s\n", __func__, name);
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return pHE;
}

/**
 * Retrieve basic CableCARD info.  The returned pointer must be subsequently
 * passed to ri_cablecard_release_data() so the platform may release allocated
 * resources associated with the info structure
 *
 * @param info the address of a pointer to a ri_cablecard_info_t structure that
 *        will hold the platform-allocated CableCARD information upon successful
 *        return
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *     RI_ERROR_CABLECARD_NOT_READY: Card not inserted or not ready
 *     RI_ERROR_ILLEGAL_ARG: Illegal or NULL arguments specified
 */
ri_error ri_cablecard_get_info(ri_cablecard_info_t** info)
{
    ri_error retval = RI_ERROR_GENERAL;
    char *result = NULL;

    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    *info = g_try_malloc0(sizeof(ri_cablecard_info_t));

    if (NULL == info)
    {
        RILOG_FATAL(-1, "line %d of %s, %s memory allocation failure!\n",
                    __LINE__, __FILE__, __func__);
    }

    result = GetValue("GF:/NV/Variable/CableCARD_manufacturer_id");

    if (result != NULL)
    {
        (*info)->manuf_id = atoi(result);
        RILOG_INFO("%s -- returning CableCARD Manufacturer ID:%X\n",
                __FUNCTION__, (*info)->manuf_id);
        g_free(result);
        retval = RI_ERROR_NONE;
    }

    if (retval == RI_ERROR_NONE)
    {
        result = GetValue("GF:/NV/Variable/CableCARD_version_number");

        if (result == NULL)
        {
            retval = RI_ERROR_ILLEGAL_ARG;
        }
        else
        {
            (*info)->version_number = atoi(result);
            RILOG_INFO("%s -- returning CableCARD version number:%X\n",
                    __FUNCTION__, (*info)->version_number);
            g_free(result);
            retval = RI_ERROR_NONE;
        }
    }

    if (retval == RI_ERROR_NONE)
    {
        result = GetValue("GF:/NV/Variable/Card_application");

        if (result == NULL)
        {
            retval = RI_ERROR_ILLEGAL_ARG;
        }
        else
        {
            gsize outlen = 0;
            (*info)->application_data = g_base64_decode(result, &outlen);
            (*info)->app_data_length = outlen;
            g_free(result);
            retval = RI_ERROR_NONE;
            RILOG_INFO("%s -- returning %d CableCARD apps.\n",
                    __FUNCTION__, (*info)->application_data[0]);
        }
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);

    return retval;
}

/**
 * Returns a list of Generic Features supported by this CableCARD.  The returned
 * feature list ptr must be subsequently passed to ri_cablecard_release_data()
 * so that the platform may release allocated resources 
 *
 * @param feature_list the address of an array of generic features IDs that
 *        that will hold the platform-allocated list of generic features
 *        supported by this card upon successful return
 * @param num_features the address where the platform will return the length of
 *        the returned generic feature array      
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *     RI_ERROR_CABLECARD_NOT_READY: Card not inserted or not ready
 *     RI_ERROR_ILLEGAL_ARG: Illegal or NULL arguments specified
 */
ri_error ri_cablecard_get_supported_features(
        ri_cablecard_generic_feature** feature_list, uint8_t* num_features)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);
    *feature_list = g_memdup(&supported_generic_features[0],
            NUM_GF_SUPPORTED_GENERIC_FEATURES
                    * sizeof(ri_cablecard_generic_feature));
    *num_features = NUM_GF_SUPPORTED_GENERIC_FEATURES;
    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return RI_ERROR_NONE;
}

/**
 * Returns the generic feature associated with a given feature ID.  The returned
 * feature data ptr must be subsequently passed to ri_cablecard_release_data()
 * so that the platform may release allocated resources
 *
 * @param feature_id the requested generic feature ID
 * @param feature_data the address of a byte buffer that will hold the generic
 *        feature data upon successful return
 * @param data_length the address where the platform will return the length of
 *        the returned generic feature data bufer
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *     RI_ERROR_CABLECARD_NOT_READY: Card not inserted or not ready
 *     RI_ERROR_GF_NOT_SUPPORTED: The given generic feature ID is not supported
 *     RI_ERROR_ILLEGAL_ARG: Illegal or NULL arguments specified
 */
ri_error ri_cablecard_get_generic_feature(
         ri_cablecard_generic_feature feature_id, uint8_t** feature_data,
         uint8_t* data_length)
{
    ri_error retval = RI_ERROR_GENERAL;
    char *result = NULL;

    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    switch (feature_id)
    {
    case RI_CCARD_GF_RF_OUTPUT_CHANNEL:
        result = GetValue("GF:/NV/Variable/RF_output_channel");
        break;
    case RI_CCARD_GF_PC_PIN:
        result = GetValue("GF:/NV/Variable/p_c_pin");
        break;
    case RI_CCARD_GF_PC_SETTINGS:
        result = GetValue("GF:/NV/Variable/p_c_settings");
        break;
    case RI_CCARD_GF_TIME_ZONE:
        result = GetValue("GF:/NV/Variable/time_zone");
        break;
    case RI_CCARD_GF_DAYLIGHT_SAVINGS:
        result = GetValue("GF:/NV/Variable/daylight_savings");
        break;
    case RI_CCARD_GF_LANGUAGE:
        result = GetValue("GF:/NV/Variable/language");
        break;
    case RI_CCARD_GF_RATING_REGION:
        result = GetValue("GF:/NV/Variable/Rating_region");
        break;
    case RI_CCARD_GF_RESET_PIN:
        result = GetValue("GF:/NV/Variable/reset_pin");
        break;
    case RI_CCARD_GF_CABLE_URLS:
        result = GetValue("GF:/NV/Variable/cable_urls");
        break;
    case RI_CCARD_GF_EA_LOCATION_CODE:
        result = GetValue("GF:/NV/Variable/ea_location_code");
        break;
    case RI_CCARD_GF_VCT_ID:
        result = GetValue("GF:/NV/Variable/vct_id");
        break;
    case RI_CCARD_GF_TURN_ON_CHANNEL:
        result = GetValue("GF:/NV/Variable/turn_on_channel");
        break;
    default:
        retval = RI_ERROR_GF_NOT_SUPPORTED;
        break;
    }

    if (result == NULL)
    {
        retval = RI_ERROR_ILLEGAL_ARG;
    }
    else
    {
        gsize outlen = 0;
        *feature_data = g_base64_decode(result, &outlen);
        *data_length = outlen;
        g_free(result);
        retval = RI_ERROR_NONE;
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);

    return retval;
}

/**
 * Sets the generic feature associated with the given feature ID.
 *
 * @param feature_id the desired generic feature ID
 * @param feature_data the generic feature data
 * @param data_length the length of the generic feature data buffer
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *     RI_ERROR_CABLECARD_NOT_READY: Card not inserted or not ready
 *     RI_ERROR_GF_NOT_SUPPORTED: The given generic feature ID is not supported
 *     RI_ERROR_ILLEGAL_ARG: Illegal or NULL arguments specified
 */
ri_error ri_cablecard_set_generic_feature(
        ri_cablecard_generic_feature feature_id, uint8_t* feature_data,
        uint8_t data_length)
{
    ri_error retval = RI_ERROR_GENERAL;
    char *gf_name = NULL;
    char *gf_value = NULL;

    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    switch (feature_id)
    {
    case RI_CCARD_GF_RF_OUTPUT_CHANNEL:
        gf_name = "GF:/NV/Variable/RF_output_channel";
        break;
    case RI_CCARD_GF_PC_PIN:
        gf_name = "GF:/NV/Variable/p_c_pin";
        break;
    case RI_CCARD_GF_PC_SETTINGS:
        gf_name = "GF:/NV/Variable/p_c_settings";
        break;
    case RI_CCARD_GF_TIME_ZONE:
        gf_name = "GF:/NV/Variable/time_zone";
        break;
    case RI_CCARD_GF_DAYLIGHT_SAVINGS:
        gf_name = "GF:/NV/Variable/daylight_savings";
        break;
    case RI_CCARD_GF_LANGUAGE:
        gf_name = "GF:/NV/Variable/language";
        break;
    case RI_CCARD_GF_RATING_REGION:
        gf_name = "GF:/NV/Variable/Rating_region";
        break;
    case RI_CCARD_GF_RESET_PIN:
        gf_name = "GF:/NV/Variable/reset_pin";
        break;
    case RI_CCARD_GF_CABLE_URLS:
        gf_name = "GF:/NV/Variable/cable_urls";
        break;
    case RI_CCARD_GF_EA_LOCATION_CODE:
        gf_name = "GF:/NV/Variable/ea_location_code";
        break;
    case RI_CCARD_GF_VCT_ID:
        gf_name = "GF:/NV/Variable/vct_id";
        break;
    case RI_CCARD_GF_TURN_ON_CHANNEL:
        gf_name = "GF:/NV/Variable/turn_on_channel";
        break;
    default:
        retval = RI_ERROR_GF_NOT_SUPPORTED;
        break;
    }

    gf_value = g_base64_encode(feature_data, data_length);

    if (FALSE == SetValue(0, gf_name, gf_value))
    {
        RILOG_ERROR("%s -- SetValue failed\n", __FUNCTION__);
        retval = RI_ERROR_GENERAL;
    }
    else
    {
        RILOG_INFO("%s -- SetValue %s = %s SUCCESS!\n", __FUNCTION__,
                gf_name, gf_value);
        retval = RI_ERROR_NONE;
    }

    g_free(gf_value);

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);

    return retval;
}

/**
 * Handles an APDU for Application Info resource.  The APDU buffer is formatted
 * as described in OC-SP-CCIF2.0 Section 9.3.
 *
 * @param apdu the APDU buffer
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *     RI_ERROR_ILLEGAL_ARG: Illegal or NULL arguments specified
 *     RI_ERROR_APDU_SEND_FAIL: Failed to send the APDU
 */
ri_error handle_AI_APDU(ri_session_id sess, uint8_t* apdu)
{
    uint8_t *apduData;
    size_t apduLen = 0;
    uint32_t apduTag = 0;

    RILOG_DEBUG("%s(%p, %p) -- Entry\n", __FUNCTION__, sess, apdu);

    getApduInfo(apdu, &apduData, &apduLen, &apduTag);

    switch (apduTag)
    {
        case 0x9F8020:
            {
                AiData *pAi = &cablecard_data.aiSession;
                ri_cablecard_info_t* info;
                
                /* For now, we just ignore the display capabilities in the req APDU
                   and we always return the same app info.  We are using the old
                   API to retrieve app info until we implement better simulation */
                if (ri_cablecard_get_info(&info) != RI_ERROR_NONE)
                {
                    RILOG_ERROR("%s - received error from ri_cablecard_get_info\n", __func__);
                    return RI_ERROR_APDU_SEND_FAIL;
                }
                apdu = createAppInfoCnfApdu(info->manuf_id, info->version_number, info->mac_address,
                                            info->serialnum_length, info->card_serialnum,
                                            info->app_data_length, info->application_data,
                                            &apduLen);
                ri_cablecard_release_data((void*)info);
                
                dumpApdu(apdu, apduLen);
                hex_dump(apdu, apduLen);
                pAi->callback(RI_CCARD_EVENT_APDU_RECV, pAi,
                              apdu, apduLen, pAi->data);
                break;
            }
            
        case 0x9F8021:
            RILOG_WARN("%s - app_info_cnf session %p has not been processed\n",
                    __func__, sess);
            break;
        case 0x9F8022:
            {
                uint8_t transaction_number = 0;
                uint16_t header_length = 0;
                uint16_t url_length = 0;
                char *url = NULL;
                MmiData *pMmi = &cablecard_data.broadcastMmiSession;

                transaction_number = apduData[0];
                header_length = (apduData[1] << 8 | apduData[2]);
                url_length = (apduData[header_length+3] << 8 |
                              apduData[header_length+4]);
                url = g_try_malloc0(url_length+1);
                
                if (NULL == url)
                {
                    RILOG_FATAL(-1,
                                "line %d of %s, %s memory allocation failure!\n",
                                __LINE__, __FILE__, __func__);
                }
                
                memcpy(url, &apduData[header_length+5], url_length);
                RILOG_INFO("%s - server_query session %p, trans:%d, URL:%s\n",
                           __func__, sess, transaction_number, url);
                
                if (strstr(url, "127.0.0.1"))
                {
                    apdu = localhost_MMI(transaction_number, url, &apduLen);
                }
                else
                {
                    apdu = cablecard_MMI(transaction_number, url, &apduLen);
                }
                
                g_free(url);
                dumpApdu(apdu, apduLen);
                hex_dump(apdu, apduLen);
                pMmi->callback(RI_CCARD_EVENT_APDU_RECV, pMmi,
                               apdu, apduLen, pMmi->data);
                break;
            }
            
        case 0x9F8023:
            RILOG_WARN("%s - server_reply %p has not been processed\n",
                    __func__, sess);
            break;
        default:
            RILOG_ERROR("%s - unknown APDU tag: %X\n", __func__, apduTag);
            return RI_ERROR_APDU_SEND_FAIL;
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return RI_ERROR_NONE;
}

/**
 * Opens a connection to the given method/dialog on the MMI resource.  The
 *  given callback function will be notified of asynchronous MMI events.
 *
 * @param session the address where the implementation will store the
 *         established session ID upon successful return
 * @param callback the callback function that will receive APDU events
 * @param cb_data callback data to be sent with every invocation of the callback
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *     RI_ERROR_CABLECARD_NOT_READY: Card not inserted or not ready
 *     RI_ERROR_CONNECTION_NOT_AVAIL: No connections available on this resource
 *     RI_ERROR_ILLEGAL_ARG: Illegal or NULL arguments specified
 */
ri_error ri_cablecard_open_MMI_dialog(ri_session_id* session,
         int dialog, char *method,
         ri_cablecard_session_callback_f callback, void* cb_data)
{
    MmiData *pMmi = &cablecard_data.dialogMmiSession;
    char buf[128];
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    if (NULL == session || NULL == callback || NULL == method)
        return RI_ERROR_ILLEGAL_ARG;

    if (NULL != pMmi->callback)
        return RI_ERROR_OUT_OF_RESOURCES;

    g_mutex_lock(pMmi->mutex);
    pMmi->callback = callback;
    pMmi->data = cb_data;
    g_mutex_unlock(pMmi->mutex);
    *session = pMmi;

    sprintf(buf, "GF:/NV/Method/%s", method);
    RILOG_INFO("%s - %s\n", __FUNCTION__, buf);

    if (FALSE == SetValue(dialog, buf, "1"))
    {
        g_mutex_lock(pMmi->mutex);
        pMmi->callback = NULL;
        g_mutex_unlock(pMmi->mutex);
        return RI_ERROR_GENERAL;
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return RI_ERROR_NONE;
}

/**
 * Close the given MMI dialog
 *
 * @param the session to be closed
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *     RI_ERROR_CABLECARD_NOT_READY: Card not inserted or not ready
 */
ri_error ri_cablecard_close_MMI_dialog(int nextDialog, int dialog, int reason)
{
    char buf[128];
    MmiData *pMmi = &cablecard_data.dialogMmiSession;
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    if (NULL == pMmi->callback)
        return RI_ERROR_GENERAL;

    sprintf(buf, "%d,%d,%d", nextDialog, dialog, reason);
    //ccCallErr = make_ual_call_for_cablecard(cablecard_data.ual_casContext,
                    //CloseMmiDialog, buf, &ret, &errorStr);

    g_mutex_lock(pMmi->mutex);
    pMmi->callback = NULL;
    g_mutex_unlock(pMmi->mutex);

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return RI_ERROR_NONE;
}

/**
 * Opens a connection to the only broadcast MMI resource.  The
 * given callback function will be notified of asynchronous MMI events.
 *
 * @param session the address where the implementation will store the
 *         established session ID upon successful return
 * @param callback the callback function that will receive APDU events
 * @param cb_data callback data to be sent with every invocation of the callback
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *     RI_ERROR_CABLECARD_NOT_READY: Card not inserted or not ready
 *     RI_ERROR_CONNECTION_NOT_AVAIL: No connections available on this resource
 *     RI_ERROR_ILLEGAL_ARG: Illegal or NULL arguments specified
 */
ri_error ri_cablecard_open_MMI_connection(ri_session_id* session,
         ri_cablecard_session_callback_f callback, void* cb_data)
{
    MmiData *pMmi = &cablecard_data.broadcastMmiSession;
    ri_error rc = RI_ERROR_NONE;
    RILOG_INFO("%s -- Entry\n", __FUNCTION__);

    if (NULL == session || NULL == callback)
    {
        rc = RI_ERROR_ILLEGAL_ARG;
        RILOG_ERROR("%s - Exit %s\n", __FUNCTION__, ri_errorToString(rc));
        return rc;
    }

    if (NULL != pMmi->callback && callback != pMmi->callback)
    {
        rc = RI_ERROR_OUT_OF_RESOURCES;
        RILOG_ERROR("%s - Exit %s\n", __FUNCTION__, ri_errorToString(rc));
        return rc;
    }

    g_mutex_lock(pMmi->mutex);
    pMmi->callback = callback;
    pMmi->data = cb_data;
    g_mutex_unlock(pMmi->mutex);
    *session = pMmi;

    RILOG_INFO("%s - Exit %s\n", __FUNCTION__, ri_errorToString(rc));
    return rc;
}

/**
 * Close the only broadcast MMI connection
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *     RI_ERROR_CABLECARD_NOT_READY: Card not inserted or not ready
 */
ri_error ri_cablecard_close_MMI_connection(void)
{
    ri_error retVal = RI_ERROR_GENERAL;
    MmiData *pMmi = &cablecard_data.broadcastMmiSession;
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    if (NULL != pMmi->callback)
    {
        g_mutex_lock(pMmi->mutex);
        pMmi->callback = NULL;
        g_mutex_unlock(pMmi->mutex);
        retVal = RI_ERROR_NONE;
    }

    RILOG_INFO("%s -- Exit %s\n", __FUNCTION__, ri_errorToString(retVal));
    return retVal;
}

/**
 * Opens a connection to the single Application Information resource.  The
 *  given callback function will be notified of asynchronous APDU events.
 *
 * @param session the address where the implementation will store the
 *         established session ID upon successful return
 * @param callback the callback function that will receive APDU events
 * @param cb_data callback data to be sent with every invocation of the callback
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *     RI_ERROR_CABLECARD_NOT_READY: Card not inserted or not ready
 *     RI_ERROR_CONNECTION_NOT_AVAIL: No connections available on this resource
 *     RI_ERROR_ILLEGAL_ARG: Illegal or NULL arguments specified
 */
ri_error ri_cablecard_open_AI_connection(ri_session_id* session,
         ri_cablecard_session_callback_f callback, void* cb_data)
{
    AiData *pAi = &cablecard_data.aiSession;
    ri_error rc = RI_ERROR_NONE;
    RILOG_INFO("%s -- Entry\n", __FUNCTION__);

    if (NULL == session || NULL == callback)
    {
        rc = RI_ERROR_ILLEGAL_ARG;
        RILOG_ERROR("%s - Exit %s\n", __FUNCTION__, ri_errorToString(rc));
        return rc;
    }

    if (NULL != pAi->callback && callback != pAi->callback)
    {
        rc = RI_ERROR_OUT_OF_RESOURCES;
        RILOG_ERROR("%s - Exit %s\n", __FUNCTION__, ri_errorToString(rc));
        return rc;
    }

    g_mutex_lock(pAi->mutex);
    pAi->callback = callback;
    pAi->data = cb_data;
    g_mutex_unlock(pAi->mutex);
    *session = pAi;

    RILOG_INFO("%s - Exit %s\n", __FUNCTION__, ri_errorToString(rc));
    return rc;
}

/**
 * Close the only Application Information resource connection
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *     RI_ERROR_CABLECARD_NOT_READY: Card not inserted or not ready
 */
ri_error ri_cablecard_close_AI_connection(void)
{
    ri_error retVal = RI_ERROR_GENERAL;
    AiData *pAi = &cablecard_data.aiSession;
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    if (NULL != pAi->callback)
    {
        g_mutex_lock(pAi->mutex);
        pAi->callback = NULL;
        g_mutex_unlock(pAi->mutex);
        retVal = RI_ERROR_NONE;
    }

    RILOG_INFO("%s -- Exit %s\n", __FUNCTION__, ri_errorToString(retVal));
    return retVal;
}
/**
 * Handles an APDU for the MMI resource.  The APDU buffer is formatted
 * as described in OC-SP-CCIF2.0 Section 9.3.
 *
 * @param sess the APDU will be sent to this open session
 * @param apdu the APDU buffer
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *     RI_ERROR_ILLEGAL_ARG: Illegal or NULL arguments specified
 *     RI_ERROR_APDU_SEND_FAIL: Failed to send the APDU
 */
ri_error handle_MMI_APDU(ri_session_id sess, uint8_t* apdu)
{
    uint8_t *apduData;
    size_t apduLen = 0;
    uint32_t apduTag = 0;
    RILOG_DEBUG("%s(%p, %p) -- Entry\n", __FUNCTION__, sess, apdu);

    getApduInfo(apdu, &apduData, &apduLen, &apduTag);

    switch (apduTag)
    {
        case 0x9F8820:
            RILOG_INFO("%s - request MMI session %p open\n", __func__, sess);
            break;
        case 0x9F8821:
            RILOG_INFO("%s - MMI session %p opened\n", __func__, sess);
            break;
        case 0x9F8822:
            RILOG_INFO("%s - request MMI session %p close\n", __func__, sess);
            break;
        case 0x9F8823:
            RILOG_INFO("%s - MMI session %p closed\n", __func__, sess);
            break;
        default:
            RILOG_ERROR("%s - unknown APDU tag: %X\n", __func__, apduTag);
            return RI_ERROR_APDU_SEND_FAIL;
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return RI_ERROR_NONE;
}

/**
 * Opens a connection to the given app ID on the SAS resource.  The given
 * callback function will be notified of asynchronous APDU events.
 *
 * @param session the address where the impl will store the established
 *        session ID upon successful return
 * @param appID the 4-byte (big-endian) application ID
 * @param callback the callback function that will receive APDU events
 * @param cb_data callback data to be sent with every invocation of the callback
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *     RI_ERROR_CABLECARD_NOT_READY: Card not inserted or not ready
 *     RI_ERROR_INVALID_SAS_APPID: Application ID not supported by card
 *     RI_ERROR_CONNECTION_NOT_AVAIL: No more connections available on resource
 *     RI_ERROR_ILLEGAL_ARG: Illegal or NULL arguments specified
 */
ri_error ri_cablecard_open_SAS_connection(ri_session_id* session,
        uint8_t appID[8], ri_cablecard_session_callback_f callback,
        void* cb_data)
{
    ri_error rc = RI_ERROR_NONE;
    SasData *pSas = NULL;
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    if (NULL == session || NULL == callback)
        return RI_ERROR_ILLEGAL_ARG;

    if (NULL != (pSas = findSasByAppId(appID)))
        return RI_ERROR_ALREADY_EXISTS;

    if (NULL == (pSas = findSasUnused()))
        return RI_ERROR_OUT_OF_RESOURCES;

    pSas->callback = callback;
    pSas->data = cb_data;
    pSas->id = g_try_malloc0(sizeof(Session));

    if (NULL == pSas->id)
    {
        RILOG_FATAL(-1, "line %d of %s, %s memory allocation failure!\n",
                    __LINE__, __FILE__, __func__);
    }

    // Copy the AppID into the session
    memcpy(((Session*)pSas->id)->appID, appID, 8);

    *session = pSas->id;
    RILOG_INFO("%s - appID(%p):\n", __FUNCTION__, appID);
    hex_dump(appID, 8);

    if (RI_ERROR_NONE != (rc = OpenSasTunnel(appID)))
    {
        RILOG_WARN("%s -- OpenSasTunnel failure (%d)\n", __func__, rc);
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return rc;
}

/**
 * Close the given SAS connection
 *
 * @param the session to be closed
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *     RI_ERROR_CABLECARD_NOT_READY: Card not inserted or not ready
 *     RI_ERROR_ILLEGAL_ARG: Invalid session ID
 */
ri_error ri_cablecard_close_SAS_connection(ri_session_id session)
{
    ri_error rc = RI_ERROR_CABLECARD_NOT_READY;
    SasData *pSas = NULL;
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    if (RI_ERROR_NONE != (rc = CloseSasTunnel(((Session*)session)->sessionNb)))
    {
        if (NULL != (pSas = findSasBySessionNb(((Session*)session)->sessionNb)))
        {
            g_free(pSas->id);
            pSas->id = NULL;
            rc = RI_ERROR_NONE;
        }
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return rc;
}

/**
 * Sends an APDU to the Specific Application Support resource.  The APDU buffer
 *  is formatted as described in OC-SP-CCIF2.0 Section 9.3.
 *
 * @param session the APDU will be sent to this open session
 * @param apdu the APDU buffer
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *     RI_ERROR_ILLEGAL_ARG: Illegal or NULL arguments specified
 *     RI_ERROR_APDU_SEND_FAIL: Failed to send the APDU
 */
ri_error handle_SAS_APDU(ri_session_id session, uint8_t* apdu)
{
    ri_error rc = RI_ERROR_NONE;
    uint8_t *apduData;
    size_t apduLen = 0;
    uint32_t apduTag = 0;
    RILOG_INFO("%s(%p, %p) -- Entry\n", __FUNCTION__, session, apdu);

    getApduInfo(apdu, &apduData, &apduLen, &apduTag);

    rc = SendSasApdu(((Session*)session)->sessionNb, apduTag, apduLen, apduData);
 
    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return rc;
}

/**
 * Registers a callback to listen for APDU events on Conditional Access Support
 * resource.  Only one callback may be registered at a time.  If a listener is
 * already registered, RI_CCARD_SESSION_CLOSED is sent to the that listener and
 * it is automatically replaced by the given listener.
 *
 * @param callback the callback function that will receive APDU events
 * @param cb_data callback data to be sent with every invocation of the callback
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *     RI_ERROR_ILLEGAL_ARG: Illegal or NULL arguments specified
 */
ri_error ri_cablecard_register_CAS_listener(
        ri_cablecard_session_callback_f callback, void* cb_data)
{
    RILOG_WARN("%s -- Entry\n", __FUNCTION__);
    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);

    return RI_ERROR_NOT_IMPLEMENTED;
}

/**
 * Unregisters the given Conditional Access Support resource listener.  RI_CCARD_SESSION_CLOSED
 * event is not sent to the listener.
 *
 * @param callback the callback function to be unregistered
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *     RI_ERROR_ILLEGAL_ARG: Illegal or NULL arguments specified
 */
ri_error ri_cablecard_unregister_CAS_listener(
        ri_cablecard_session_callback_f callback)
{
    RILOG_WARN("%s -- Entry\n", __FUNCTION__);
    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);

    return RI_ERROR_NOT_IMPLEMENTED;
}

/**
 * Sends an APDU to the Conditional Access Support resource.  The APDU buffer
 *  is formatted as described in OC-SP-CCIF2.0 Section 9.3.
 *
 * @param apdu the APDU buffer
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *     RI_ERROR_ILLEGAL_ARG: Illegal or NULL arguments specified
 *     RI_ERROR_APDU_SEND_FAIL: Failed to send the APDU
 */
ri_error handle_CAS_APDU(ri_session_id sess, uint8_t* apdu)
{
    uint8_t *apduData;
    size_t apduLen = 0;
    uint32_t apduTag = 0;
    RILOG_DEBUG("%s(%p, %p) -- Entry\n", __FUNCTION__, sess, apdu);

    getApduInfo(apdu, &apduData, &apduLen, &apduTag);

    switch (apduTag)
    {
        case 0x9F8030:
            RILOG_INFO("%s - ca_info_inq session %p\n", __func__, sess);
            break;
        case 0x9F8031:
            RILOG_INFO("%s - ca_info session %p\n", __func__, sess);
            break;
        case 0x9F8032:
            RILOG_INFO("%s - ca_pmt session %p\n", __func__, sess);
            break;
        case 0x9F8033:
            RILOG_INFO("%s - ca_pmt_reply %p\n", __func__, sess);
            break;
        case 0x9F8034:
            RILOG_INFO("%s - ca_update session %p\n", __func__, sess);
            break;
        default:
            RILOG_ERROR("%s - unknown APDU tag: %X\n", __func__, apduTag);
            return RI_ERROR_APDU_SEND_FAIL;
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return RI_ERROR_NONE;
}

/**
 * Opens a CFD socket flow based on the given application ID
 *
 * @param appID the 4-byte (big-endian) application ID
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *     RI_ERROR_CABLECARD_NOT_READY: Card not inserted or not ready
 *     RI_ERROR_ILLEGAL_ARG: Illegal or NULL arguments specified
 */
ri_error ri_cablecard_open_CFD(uint32_t appID)
{
    //uint32_t result;
    char *ret = NULL;
    char buf[32];
    RILOG_INFO("%s(appID: %X (%d)) Entry\n", __FUNCTION__, appID, appID);

    if (0 == appID)
        return RI_ERROR_ILLEGAL_ARG;

    if (-1 != cablecard_data.cfd_SD)
    {
        RILOG_DEBUG("%s already open sd:%d\n", __func__, cablecard_data.cfd_SD);
        return RI_ERROR_NONE;
    }

    // Once we actually make this UAL call the BOCR persists the attempt for
    // the given appID (success or fail).  That means subsequent calls after
    // a failed call will never work.  In light of this we make no attempt
    // to release the resources - an RI (and BOCR) reset are required!
    cablecard_data.cfd_AppId = appID;
    sprintf(buf, "%u,%d", appID, 990);
    //ret = make_ual_synch_call(cablecard_data.ual_opaContext, RequestCfd, buf);
    if (NULL != ret)
    {
        //if (getIntArgValue(ret, "Result = ", &result))
        //{
        //    RILOG_INFO("%s RequestCfd Result: (%08X)\n",
        //            __FUNCTION__, result);
        //}

        //g_free(ret); // must free make_ual_synch_call return data

        //if (NULL == strstr(errorStr, "ERR"))
        {
            int sd = -1;

            if ((sd = 0))//cfd_Open()) >= 0)
            {
                cablecard_data.cfd_SD = sd;
                RILOG_INFO("%s CFD connection open (sd:%d) for app:%X (%d)\n",
                           __func__, cablecard_data.cfd_SD, appID, appID);
                return RI_ERROR_NONE;
            }
            else
            {
                RILOG_ERROR("%s CFD connection failed to open!?\n", __func__);
                return RI_ERROR_CONNECTION_NOT_AVAIL;
            }
        }
    }

    //RILOG_ERROR("%s RequestCfd: %s\n", __FUNCTION__, errorStr);
    return RI_ERROR_CABLECARD_NOT_READY;
}

/**
 * Closes a CFD socket flow based on the given application ID
 *
 * @param appID the 4-byte (big-endian) application ID
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *     RI_ERROR_CABLECARD_NOT_READY: Card not inserted or not ready
 *     RI_ERROR_ILLEGAL_ARG: Illegal or NULL arguments specified
 */
ri_error ri_cablecard_close_CFD(uint32_t appID)
{
    RILOG_INFO("%s(appID: %X (%d)) Entry\n", __FUNCTION__, appID, appID);

    if (0 == appID)
    {
        return RI_ERROR_ILLEGAL_ARG;
    }

    if (-1 == cablecard_data.cfd_SD)
    {
        RILOG_DEBUG("%s CFD not open!\n", __func__);
        return RI_ERROR_CONNECTION_NOT_AVAIL;
    }

    //cfd_Close();
    RILOG_INFO("%s CFD connection closed (sd:%d) for app:%X (%d)\n",
               __func__, cablecard_data.cfd_SD, appID, appID);
    cablecard_data.cfd_AppId = 0;
    cablecard_data.cfd_SD = -1;
    return RI_ERROR_NONE;
}

/**
 * Retrieves a property value from the Host Addressable Properties resource.
 * The caller should pass returned property value to ri_cablecard_release_data()
 * when it has finished copying/inspecting the data.
 *
 * @param name a null-terminated UTF-8 string describing the property name
 * @param value the memory location where the platform will return the property
 *        value upon successful return.  Value will be a null-terminated UTF-8
 *        string.  If the property name was not found, the returned value will
 *        be NULL and should not be passed to ri_cablecard_release_data()
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *     RI_ERROR_CABLECARD_NOT_READY: Card not inserted or not ready
 *     RI_ERROR_ILLEGAL_ARG: Illegal or NULL arguments specified
 */
ri_error ri_cablecard_get_addressable_property(const char* name, char** value)
{
    ri_error retval = RI_ERROR_GENERAL;
    char *result = NULL;

    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    result = GetValue("Tuner-0/NV/Variable/Address_XAIT");

    if (result == NULL)
    {
        retval = RI_ERROR_ILLEGAL_ARG;
    }
    else
    {
        // TODO
        // Parse the received addressable XAIT data (in whatever format it is),
        // extract the addressable XAIT property requested and return the value.
        // TODO
        RILOG_WARN("%s -- partially not implemented\n", __FUNCTION__);
        *value = result;
        retval = RI_ERROR_NONE;
        g_free(result);
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);

    return retval;
}

/**
 * obtains the cablecard_data.vct_id read from GF database (real or otherwise)
 */
uint16_t cablecard_get_vct_id(void)
{
    return cablecard_data.vct_id;
}

/**
 * Sends an APDU to the CableCARD resource.  The APDU buffer is formatted
 * as described in OC-SP-CCIF2.0 Section 9.3.
 *
 * @param sess the APDU will be sent to this open session
 * @param apdu the APDU buffer
 *
 * @return Upon success, returns RI_ERROR_NONE.  Otherwise:
 *     RI_ERROR_ILLEGAL_ARG: Illegal or NULL arguments specified
 *     RI_ERROR_APDU_SEND_FAIL: Failed to send the APDU
 */
ri_error ri_cablecard_send_APDU(ri_session_id sess, uint8_t* apdu)
{
    uint32_t apduTag = 0;
    ri_error rc = RI_ERROR_NONE;

    RILOG_INFO("%s(%p, %p) -- Entry\n", __FUNCTION__, sess, apdu);

    apduTag = (apdu[0] << 16 | apdu[1] << 8 | apdu[2]);

    if (apduTag >= 0x9F8020 && apduTag <= 0x9F8027)
    {
        rc = handle_AI_APDU(sess, apdu);
    }
    else if (apduTag >= 0x9F8030 && apduTag <= 0x9F8037)
    {
        rc = handle_CAS_APDU(sess, apdu);
    }
    else if (apduTag >= 0x9F8820 && apduTag <= 0x9F8827)
    {
        rc = handle_MMI_APDU(sess, apdu);
    }
    else if (apduTag >= 0x9F9A00 && apduTag <= 0x9F9A07)
    {
        rc = handle_SAS_APDU(sess, apdu);
    }
    else
    {
        RILOG_ERROR("%s - unknown APDU tag: %X\n", __func__, apduTag);
        rc = RI_ERROR_APDU_SEND_FAIL;
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return rc;
}

/**
 * Instructs the platform to release the resources allocated by any of the RI
 * Platform CableCARD module APIs defined in this file
 *
 * @param a pointer to the data allocated by the platform that should be released
 */
void ri_cablecard_release_data(void* data)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);
    g_free(data);
    data = NULL;
    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

void cablecard_init(void)
{
    int i;
    char *result = NULL;
    cablecardLogCat = log4c_category_get("RI.Cablecard");
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    memset(&cablecard_data, 0, sizeof(cablecard_data));

    cablecard_data.cfd_AppId = 0;
    cablecard_data.cfd_SD = -1;

    g_static_mutex_init(&cablecard_data.mutex);

    for(i = 0; i < MAX_CABLECARD_SESSIONS; i++)
    {
        cablecard_data.sasSession[i].eventType = -1;
        cablecard_data.sasSession[i].mutex = g_mutex_new();
        cablecard_data.sasSession[i].cond = g_cond_new();
    }

    cablecard_data.dialogMmiSession.callback = NULL;
    cablecard_data.dialogMmiSession.mutex = g_mutex_new();
    cablecard_data.broadcastMmiSession.callback = NULL;
    cablecard_data.broadcastMmiSession.mutex = g_mutex_new();
    cablecard_data.aiSession.callback = NULL;
    cablecard_data.aiSession.mutex = g_mutex_new();

    SasInit();
    GfInit();
    result = GetValue("GF:/NV/Variable/vct_id");

    if (result == NULL)
    {
        RILOG_ERROR("%s - couldn't read CableCARD VCT ID!\n", __func__);
    }
    else
    {
        gsize len = 0;
        uint8_t *val = g_base64_decode(result, &len);
        cablecard_data.vct_id = (val[0] << 8);
        cablecard_data.vct_id |= val[1];
        RILOG_INFO("%s -- CableCARD VCT ID to use:%X\n",
                __FUNCTION__, cablecard_data.vct_id);
        g_free(result);
        g_free(val);
    }

    test_RegisterMenu(&gfMenuItem);
    test_RegisterMenu(&mmiMenuItem);
    test_RegisterMenu(&ccMenuItem);
    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

