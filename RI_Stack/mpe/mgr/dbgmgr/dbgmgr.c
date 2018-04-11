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

#include <sysmgr.h>
#include <dbgmgr.h>
#include <mgrdef.h>
#include <mpe_dbg.h>
#include <mpeos_dbg.h>
#include <mpeos_util.h>

#include <string.h> // memset

/* Debug manager function table definition */
mpe_dbg_ftable_t dbg_ftable =
{ mpe_dbgInit, /* Debug manager initialization. */
mpeos_dbgMsgRaw, /* Formatted debug message output function. */
_mpe_dbgStatusGetTypes, /* Get status type mapping table. */
_mpe_dbgStatusGet, /* Retrieve general purpose status information API. */
_mpe_dbgStatusRegister, /* Register event queue/ED handle for status events. */
_mpe_dbgStatusUnregister, /* Unregister for status events. */
_mpe_dbgStatusRegisterInterest, /* Register for specific status event. */
_mpe_dbgStatusUnregisterInterest, /* Unregister for specific status event. */
_mpe_dbgStatusDeliverEvent, /* Deliver status event. */
mpeos_dbgAddLogEntry, /* log timestamp and msg to SNMP table via provided oid */
};

static mpe_EventQueue statusQ;
static mpe_Bool statusQRegistered = FALSE;

static void *statusACT = NULL;
static mpe_Bool registered[ENUM_MPE_DBG_STATUS_COUNT + 1];

/**
 * <i>mpe_dbgSetup</i> calls the sys manager to install the debug manager function table
 */
void mpe_dbgSetup()
{
    mpe_sys_install_ftable(&dbg_ftable, MPE_MGR_TYPE_DBG);
}

/**
 * Initialize the underlying MPEOS debug support.
 */
void mpe_dbgInit()
{
    int mod, i;
    char config[128];
    const char *modptr = NULL;
    static mpe_Bool inited = false;

    if (!inited)
    {
        mpeos_dbgInit();
        mpeos_dbgLogControlInit();
        inited = true;
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_OS, "\n");
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_OS,
                "-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-\n");
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_OS, "Stack level logging levels: \n");

        /*
         * Now just dump all the current settings so that an analysis of a log
         * file will include what logging information to expect
         */
        for (mod = ENUM_MPE_MOD_BEGIN; mod < ENUM_MPE_MOD_COUNT; mod++)
        {
            modptr = mpe_logModuleStrings[mod];
            memset(config, 0, sizeof(config));
            (void) mpeos_dbgLogQueryOpSysIntf((char*) modptr, config, 127);

            MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_OS,
                    "Initial Logging Level for %-10s: %s\n", modptr, config);
        }

        /* Initialized registered status event flag array. */
        for (i = ENUM_MPE_DBG_STATUS_BEGIN; i <= ENUM_MPE_DBG_STATUS_COUNT; ++i)
        {
            registered[i] = FALSE; /* Nothing registered. */
        }

        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_OS,
                "-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-\n\n");
    }
}

/**
 * Acquire table of all supported status types.
 *
 * @param types is a pointer for returning the status type mapping table.
 *
 * @return MPE_SUCCESS if types acquired.
 */
mpe_Error _mpe_dbgStatusGetTypes(mpe_DbgStatusType **types)
{
    extern mpe_DbgStatusType mpeDbgStatusTypes[];

    /* Sanity check. */
    if (types == NULL)
        return MPE_EINVAL;

    /* Return pointer to table. */
    *types = &mpeDbgStatusTypes[0];

    /* Call MPEOS to allow for runtime registration of porting layer status types. */
    return mpeos_dbgStatusGetTypes(mpeDbgStatusTypes);
}

/**
 * <code>mpe_dbgStatusGet</code> acquires status from the specified native component.  Different
 * modules can support access to different sets of status information in which case the
 * type identifier specifies the type of information to return.
 *
 * @param id is the module identifier of the module to acquire status from.
 * @param type is the type of status information
 * @param size is the size of the status buffer used for acquiring the information.
 * @param status is a pointer to a block of memory to store the information
 *
 * @param MPE_SUCCESS if the desired information was acquired.
 */
mpe_Error _mpe_dbgStatusGet(mpe_LogModule id, mpe_DbgStatusId type,
        uint32_t *size, void *status, void *params)
{
    extern mpe_Error si_dbgStatus(mpe_DbgStatusId, uint32_t *, void *, void *);

    switch (id)
    {
    case MPE_MOD_SI:
        return si_dbgStatus(type, size, status, params);
    default:
        return MPE_EINVAL;
    }
}

/**
 * <code>mpe_dbgStatusRegister</code> registers an event queue for status events.
 *
 * @param queueId is the event queue to deliver status events to.
 * @param act is the asynchronous callback token to deliver with the event.
 *
 * @return MPE_SUCCESS if the queue was registered successfully.
 */
mpe_Error _mpe_dbgStatusRegister(mpe_EventQueue queueId, void *act)
{
    if ((act == NULL) || statusQRegistered)
    {
        return MPE_EINVAL;
    }

    /* Save event deliver parameters. */
    statusQ = queueId;
    statusQRegistered = TRUE;
    statusACT = act;

    /* Pass delivery parameters to porting layer. */
    return mpeos_dbgStatusRegister(queueId, act);
}

/**
 * <code>mpe_dbgStatusUnregister</code> unregisters for status events.
 *
 * @param queueId is the previously registered event queue.
 *
 * @return MPE_SUCCESS if the unregistration succeeded.
 */
mpe_Error _mpe_dbgStatusUnregister(mpe_EventQueue queueId)
{
    if (queueId != statusQ)
    {
        return MPE_EINVAL;
    }

    /* Clear registered event queue and act. */
    statusQRegistered = FALSE;
    statusACT = NULL;

    /* Inform porting layer of unregistration. */
    return mpeos_dbgStatusUnregister(queueId);
}

/**
 * <code>mpe_dbgStatusRegisterInterest</code> registers for delivery of a specific status event.
 *
 * @param typeId is the status type identifier for the event.
 * @param format is the status format identifier.
 * @param size is the size of the optional parameter (e.g. buffer size).
 * @param param is the optional parameter (e.g. buffer).
 *
 * @return MPE_SUCCESS if the registration succeeded.
 */
mpe_Error _mpe_dbgStatusRegisterInterest(mpe_DbgStatusId typeId,
        mpe_DbgStatusFormat format, void *param)
{
    extern mpe_Error ocRegisterEvent(mpe_DbgStatusId, mpe_DbgStatusFormat,
            void*);
    extern mpe_DbgStatusType mpeDbgStatusTypes[];
    mpe_Error ec = MPE_SUCCESS;
    mpe_LogModule modId = (typeId >> 16) & MPE_DBG_STATUS_TYPEID_MASK;

    /* Remove module identifier. */
    typeId = typeId & MPE_DBG_STATUS_TYPEID_MASK;

    /* Validate type identifier. */
    if ((typeId <= ENUM_MPE_DBG_STATUS_BEGIN) || (typeId
            > ENUM_DBG_STATUS_TOTAL) || (mpeDbgStatusTypes[typeId - 1].stringId
            == NULL))
    {
        return MPE_EINVAL;
    }

    /* If it's an MPEOS status type pass on registration. */
    if (typeId > ENUM_MPEOS_DBG_STATUS_BEGIN)
        return mpeos_dbgStatusRegisterInterest(typeId, format, param);
    else
    {
        /*
         * Use of the module identifier is really for logical convenience,
         * since each type identifier is actually unique anyway.
         */
        switch (modId)
        {
        case MPE_MOD_FILESYS:
            /* Currently, only the OC file system has registerable events. */
            ec = ocRegisterEvent(typeId, format, param);
            break;

            /*
             * Add new modules that support status events here:
             */

        default:
            ec = MPE_EINVAL;
        }
        /* Flag specific type as having a register listener. */
        if (ec == MPE_SUCCESS)
            registered[typeId - 1] = TRUE;
    }
    return ec;
}

/**
 * <code>mpe_dbgStatusUnregisterInterest</code> unregisters interest in a specific status event.
 *
 * @param typeId is the status type identifier for the event.
 *
 * @return MPE_SUCCESS if the unregistration succeeded.
 */
mpe_Error _mpe_dbgStatusUnregisterInterest(mpe_DbgStatusId typeId)
{
    extern mpe_DbgStatusType mpeDbgStatusTypes[];

    /* Remove the module identifier. */
    typeId = typeId & MPE_DBG_STATUS_TYPEID_MASK;

    /* Validate the type identifier. */
    if ((typeId <= ENUM_MPE_DBG_STATUS_BEGIN) || (typeId
            > ENUM_DBG_STATUS_TOTAL) || (mpeDbgStatusTypes[typeId - 1].stringId
            == NULL))
        return MPE_EINVAL;

    /* If it's an MPEOS status type pass on registration. */
    if (typeId > ENUM_MPEOS_DBG_STATUS_BEGIN)
        return mpeos_dbgStatusUnregisterInterest(typeId); /* MPEOS tracks its own registrations. */
    else
        registered[typeId - 1] = FALSE; /* Clear MPE-level registration. */

    return MPE_SUCCESS;
}

/**
 * <code>mpe_dbgStatusDeliverEvent</code> delivers a specific status event.  This is a convenience
 * method used by the MPE layer to deliver status events.  The MPEOS level events should be
 * delivered directly to the originally registered event queue.
 *
 * @param typeId is the (MPE_MOD_XYZ | MPE_DBG_STATUS_ABC) status type identifier.
 * @param format is the MPE_DBG_STATUS_FMT_ identifier.
 * @param size is an optional size parameter for the status.
 * @param status is the status parameter.
 *
 */
mpe_Error _mpe_dbgStatusDeliverEvent(mpe_DbgStatusId typeId,
        mpe_DbgStatusFormat format, void *status)
{
    extern mpe_DbgStatusType mpeDbgStatusTypes[];
    mpe_DbgStatusId tid = typeId;

    /* Remove module identifier. */
    typeId = typeId & MPE_DBG_STATUS_TYPEID_MASK;

    /* Validate type identifier. */
    if ((typeId <= ENUM_MPE_DBG_STATUS_BEGIN) || (typeId
            > ENUM_DBG_STATUS_TOTAL) || (mpeDbgStatusTypes[typeId - 1].stringId
            == NULL))
        return MPE_EINVAL;

    if ((typeId > ENUM_MPE_DBG_STATUS_COUNT + 1) || (registered[typeId - 1]
            == FALSE))
        return MPE_EINVAL;

    /* Send the event. */
    return mpe_eventQueueSend(statusQ, tid, status, statusACT, format);
}
