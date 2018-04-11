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

#ifndef _MPE_DBG_H_
#define _MPE_DBG_H_

/**
 * This is used to refer to different modules in the logging
 * context. It is also used to construct mpeos_g_logControlTbl and an
 * enum to string mapping array in $/mpe/os/common/mpeos_dbg_log.c, so
 * care must be taken to update that array if modules are added.
 *
 * The concept of a module is pretty nebulous, they are actually handles for
 * logically cohesive systems, sub-systems and sub-sub-systems.
 */
typedef enum
{
    ENUM_MPE_MOD_BEGIN = 0, /* Used as array index. */

    MPE_MOD_TARGET = ENUM_MPE_MOD_BEGIN, /* Target specific code. */
    MPE_MOD_CC, /* Closed Captioning */
    MPE_MOD_CDL, /* Common Download */
    MPE_MOD_COND, /* Conditionals */
    MPE_MOD_DBG, /* Debugging (debug infrastructure) */
    MPE_MOD_DIRECTFB, /* Direct Frame Buffer */
    MPE_MOD_DISP, /* Display */
    MPE_MOD_DLL, /* Dynamic Library Management */
    MPE_MOD_DVR, /* Digital Video Recorder */
    MPE_MOD_EVENT, /* Events */
    MPE_MOD_ED, /* Event Distributor */
    MPE_MOD_FILESYS, /* File System */
    MPE_MOD_FILTER, /* Section Filtering */
    MPE_MOD_HN, /* Home Networking */
    MPE_MOD_VBI, /* VBI */
    MPE_MOD_FP, /* Front Panel */
    MPE_MOD_GFX, /* Graphics */
    MPE_MOD_JAVA, /* Java */
    MPE_MOD_JNI, /* Java Native Interface (JNI) */
    MPE_MOD_JNI_AWT, /* AWT (JNI) */
    MPE_MOD_JVM, /* Java Virtual Machine */
    MPE_MOD_MEDIA, /* Media */
    MPE_MOD_MEM, /* Memory */
    MPE_MOD_MUTEX, /* Mutex */
    MPE_MOD_NET, /* Networking */
    MPE_MOD_OS, /* Operating System */
    MPE_MOD_POD, /* POD (CableCARD) */
    MPE_MOD_SI, /* System Information */
    MPE_MOD_SOUND, /* Sound Playback */
    MPE_MOD_SYS, /* System */
    MPE_MOD_TEST, /* Test (unit & system) */
    MPE_MOD_THREAD, /* Threading */
    MPE_MOD_UTIL, /* Utility */
    MPE_MOD_UI, /* User Interface */
    MPE_MOD_MRDVR, /* Multiple-Room DVR */
    MPE_MOD_SNMP, /* SNMP */
    MPE_MOD_STORAGE, /* Storage */
    MPE_MOD_PROD, /* Production Logging Module */
    MPE_MOD_FDR, /* Flight Data Recorder */
    MPE_MOD_PERF, /* Performance (this is added for logging performance events) */
    ENUM_MPE_MOD_COUNT
/* number of modules defined */
} mpe_LogModule;

/**
 * String names that correspond modules represented in MPE_MOD_* enum.
 * Note: This array *must* match the MPE_MOD_* enum in
 * $OCAPROOT/mpe/include/mpe_dbg.h
 */
#ifdef MPE_DBG_DEFINE_STRINGS
const char* mpe_logModuleStrings[ENUM_MPE_MOD_COUNT] =
{
    "TARGET",
    "CC",
    "CDL",
    "COND",
    "DBG",
    "DIRECTFB",
    "DISP",
    "DLL",
    "DVR",
    "EVENT",
    "ED",
    "FILESYS",
    "FILTER",
    "HN",
    "VBI",
    "FP",
    "GFX",
    "JAVA",
    "JNI",
    "JNI:AWT",
    "JVM",
    "MEDIA",
    "MEM",
    "MUTEX",
    "NET",
    "OS",
    "POD",
    "SI",
    "SOUND",
    "SYS",
    "TEST",
    "THREAD",
    "UTIL",
    "UI",
    "MRDVR",
    "SNMP",
    "STORAGE",
    "PROD",
    "FDR",
    "PERF"
};
#else
extern const char *mpe_logModuleStrings[ENUM_MPE_MOD_COUNT];
#endif /* MPE_DBG_DEFINE_STRINGS */

/**
 * These values represent the logging 'levels' or 'types', they are each
 * independent. This enum is used to traverse an internal enum to string
 * mapping array in $/mpe/os/common/mpeos_dbg_log.c
 */
typedef enum
{
    ENUM_MPE_LOG_BEGIN = 0, /* Used as array index. */

    MPE_LOG_FATAL = ENUM_MPE_LOG_BEGIN,
    MPE_LOG_ERROR,
    MPE_LOG_WARN,
    MPE_LOG_INFO,
    MPE_LOG_DEBUG,

    MPE_LOG_TRACE1,
    MPE_LOG_TRACE2,
    MPE_LOG_TRACE3,
    MPE_LOG_TRACE4,
    MPE_LOG_TRACE5,
    MPE_LOG_TRACE6,
    MPE_LOG_TRACE7,
    MPE_LOG_TRACE8,
    MPE_LOG_TRACE9,

    ENUM_MPE_LOG_COUNT
} mpe_LogLevel;

/**
 * String names that correspond to the various logging types.
 * Note: This array *must* match the MPE_MOD_* enum.
 */
#ifdef MPE_DBG_DEFINE_STRINGS
const char *mpe_logLevelStrings[ENUM_MPE_LOG_COUNT] =
{
    "FATAL",
    "ERROR",
    "WARNING",
    "INFO",
    "DEBUG",

    "TRACE1",
    "TRACE2",
    "TRACE3",
    "TRACE4",
    "TRACE5",
    "TRACE6",
    "TRACE7",
    "TRACE8",
    "TRACE9",
};
#else
extern const char *mpe_logLevelStrings[ENUM_MPE_LOG_COUNT];
#endif /* MPE_DBG_DEFINE_STRINGS */

/**
 * Enumeration of the mpe_dbgStatus status information types.  These values
 * identify the type of status information being requested throught this API.
 */
typedef enum
{
    ENUM_MPE_DBG_STATUS_BEGIN = 0,
    /*
     * MPE level status types reside here.
     */
    MPE_DBG_STATUS_SI_ENTRY_COUNT,
    MPE_DBG_STATUS_SI_ENTRY,
    MPE_DBG_STATUS_SI_ENTRIES,
    MPE_DBG_STATUS_OC_MOUNT_EVENT,
    MPE_DBG_STATUS_OC_UNMOUNT_EVENT,

    ENUM_MPE_DBG_STATUS_COUNT = 63,

    ENUM_MPEOS_DBG_STATUS_BEGIN,
    /*
     * MPEOS port specific status types reside here and are
     * allocated at runtime in the MPEOS porting layer.
     */
    ENUM_MPEOS_DBG_STATUS_COUNT = 127,
    ENUM_DBG_STATUS_TOTAL = 128
} mpe_DbgStatusTypeId;

typedef unsigned int mpe_DbgStatusId; /* Composed of [mpe_LogModule << 16 | mpe_DbgStatusTypeId] */

/*
 * Mask used to isolate the status type identifier.
 */
#define MPE_DBG_STATUS_TYPEID_MASK (0x0000FFFF)

typedef enum
{
    MPE_DBG_STATUS_FMT_ANY = 0,
    MPE_DBG_STATUS_FMT_INT = 1,
    MPE_DBG_STATUS_FMT_STRING = 2,
    MPE_DBG_STATUS_FMT_BEAN = 3
} mpe_DbgStatusFormat;

/*
 * This structure defines the association between a status
 * type string and its integer identifier.
 */
struct _mpe_DbgStatusType
{
    const char *stringId;
    mpe_DbgStatusId typeId;
};

typedef struct _mpe_DbgStatusType mpe_DbgStatusType;

/*
 * Convenience macro for constructing debug status identifiers.
 */
#define MPE_DBG_STATUS_ID(mod, id) ((mod << 16) | (id))

#ifdef MPE_DBG_DEFINE_STRINGS
/*
 * The mpedbgStatusTypes table defines the mappings between then logical status
 * type string identifier and the integer type identifier.  The static mapping
 * allows for more efficient type identification at runtime by eliminating the
 * string compare operation in native. The java class will hash the string to
 * its native identifier when passing the status types to native.
 *
 * To add new MPE level status types simply add an enum above and the corresponding
 * string type to the table below.  The integer type identifiers consist of the
 * high-order 16-bits representing the associated debug module identifier and the
 * low-order 16-bits representing the enum value of the status type.
 */
mpe_DbgStatusType mpeDbgStatusTypes[ENUM_DBG_STATUS_TOTAL] =
{
    {   "sidbSiGetEntryCount", MPE_DBG_STATUS_ID(MPE_MOD_SI, MPE_DBG_STATUS_SI_ENTRY_COUNT)},
    {   "sidbSiGetEntry", MPE_DBG_STATUS_ID(MPE_MOD_SI, MPE_DBG_STATUS_SI_ENTRY)},
    {   "sidbSiGetEntries", MPE_DBG_STATUS_ID(MPE_MOD_SI, MPE_DBG_STATUS_SI_ENTRIES)},
    {   "ocStatusMountEvent", MPE_DBG_STATUS_ID(MPE_MOD_FILESYS, MPE_DBG_STATUS_OC_MOUNT_EVENT)},
    {   "ocStatusUnmountEvent", MPE_DBG_STATUS_ID(MPE_MOD_FILESYS, MPE_DBG_STATUS_OC_UNMOUNT_EVENT)},

    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},

    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},

    /* ENTRIES BELOW ARE PLACE HOLDERS FOR MPEOS ADDITIONS TO THE TABLE. */
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},

    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},

    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0},
    {   NULL, 0}

};
#else
extern mpe_DbgStatusType mpedbgStatusTypes[ENUM_DBG_STATUS_TOTAL];
#endif /* MPE_DBG_DEFINE_STRINGS */

#include "mpe_sys.h"
#include "mpeos_dbg.h"
#include "../mgr/include/dbgmgr.h"
#include "../mgr/include/mgrdef.h"

/** Conveneince macro for refering to the debug manager function table. */
#define mpe_dbg_ftable ((mpe_dbg_ftable_t*)(FTABLE[MPE_MGR_TYPE_DBG]))

/** Convenience macro for mapping into the debug manager initialisation function. */
#define mpe_dbg_init ((mpe_dbg_ftable->mpe_dbg_init_ptr))

/* Debug status macros for APIs */
#define mpe_dbgStatusGetTypes           ((mpe_dbg_ftable->mpe_dbgStatusGetTypes_ptr))
#define mpe_dbgStatusGet                ((mpe_dbg_ftable->mpe_dbgStatusGet_ptr))
#define mpe_dbgStatusRegister           ((mpe_dbg_ftable->mpe_dbgStatusRegister_ptr))
#define mpe_dbgStatusUnregister         ((mpe_dbg_ftable->mpe_dbgStatusUnregister_ptr))
#define mpe_dbgStatusRegisterInterest   ((mpe_dbg_ftable->mpe_dbgStatusRegisterInterest_ptr))
#define mpe_dbgStatusUnregisterInterest ((mpe_dbg_ftable->mpe_dbgStatusUnregisterInterest_ptr))
#define mpe_dbgStatusDeliverEvent       ((mpe_dbg_ftable->mpe_dbgStatusDeliverEvent_ptr))
#define mpe_dbgAddLogEntry              ((mpe_dbg_ftable->mpe_dbgAddLogEntry_ptr))

/* MPE level logging macro */
#define MPE_LOG OS_MPE_LOG

#endif /* _MPE_DBG_H_ */
