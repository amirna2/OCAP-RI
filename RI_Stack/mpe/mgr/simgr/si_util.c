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

// --------------------------------------------------------------
//                          Linked List functions
// --------------------------------------------------------------

#include "si_util.h"
#include <mpe_types.h>
#include <mpe_error.h>
#include <mpeos_mem.h>
#include <mpe_dbg.h>
#include "simgr.h"
#include <mpe_file.h>
#include <string.h>

/*
 * =========================================================================
 * Create and initialize a list header.
 * Returns a pointer to the header, or NULL on error.
 * =========================================================================
 */

LINKHD * llist_create(void)
{
    LINKHD * hp;

    /*
     * Get space.
     */
    if (MPE_SUCCESS != mpeos_memAllocP(MPE_MEM_SI, sizeof(LINKHD),
            (void **) &(hp)))
    {
        return NULL;
    }

    /*
     * Init list to empty and return it.
     */
    hp->headp = NULL;
    hp->tailp = NULL;
    hp->nlinks = 0;

    return hp;
}

/*
 * =========================================================================
 * Destroy (free) a list header and all it's links.
 *  headerp     header of list to destroy.
 * Returns headerp or NULL on error.
 * =========================================================================
 */
void llist_free(LINKHD *headerp)
{
    LINK * lp;

    while (1)
    {
        lp = llist_first(headerp);
        if (lp == NULL)
            break;

        llist_rmfirst(headerp);
        llist_freelink(lp);
    }

    mpeos_memFreeP(MPE_MEM_SI, headerp);
}

/*
 * =========================================================================
 * Create a new link, and bind the 'user' data.
 *  datap       pointer to arbitrary data.
 * Returns a pointer to the new link, or NULL on error.
 * =========================================================================
 */
LINK * llist_mklink(void * datap)
{
    LINK * lp;

    /*
     * Get a free link.
     */
    if (MPE_SUCCESS != mpeos_memAllocP(MPE_MEM_SI, sizeof(LINK),
            (void **) &(lp)))
    {
        return NULL;
    }

    lp->datap = datap;
    return lp;
}

/*
 * =========================================================================
 * Destroy (free) a link.
 *  linkp       pointer to link to destroy.
 * The link must not be in a list.
 * Returns a pointer to the freed link, or NULL on error.
 * Really oughta return BOOL status, instead of ptr to free memory.
 * =========================================================================
 */
void llist_freelink(LINK *linkp)
{
    mpeos_memFreeP(MPE_MEM_SI, linkp);
}

/*
 * =========================================================================
 * Append a link to the list tail.
 *  headerp     pointer to the list header.
 *  linkp       the link to append.
 * The link must not already be in a list.
 * Returns a pointer to the link, or NULL on error.
 * =========================================================================
 */
void llist_append(LINKHD *headerp, LINK *linkp)
{
    linkp->nextp = NULL;
    linkp->headerp = headerp;
    linkp->prevp = headerp->tailp;

    if (headerp->tailp)
        headerp->tailp->nextp = linkp;

    headerp->tailp = linkp;

    if (headerp->headp == NULL)
        headerp->headp = linkp;

    ++headerp->nlinks;
}

/*
 * =========================================================================
 * Return a pointer to the first link in the list.
 *  headerp     the list to look in.
 * Returns the link pointer found, or NULL on error.
 * =========================================================================
 */
LINK * llist_first(LINKHD * headerp)
{
    return headerp->headp;
}

/*
 * =========================================================================
 * Return a pointer to the last link in the list.
 *  headerp     the list to look in.
 * Returns the link pointer found, or NULL on error.
 * =========================================================================
 */
LINK * llist_last(LINKHD * headerp)
{
    return headerp->tailp;
}

/*
 * =========================================================================
 * Return a pointer to the link after the given link.
 *  afterp      the link to look after.
 * Returns the link pointer found, or NULL on error.
 * =========================================================================
 */
LINK * llist_after(LINK * afterp)
{
    return afterp->nextp;
}

/*
 * =========================================================================
 * Remove specified link from it's list and return a pointer to it.
 * The link is not destroyed.
 *  linkp       the link to remove.
 * Returns the link removed, or NULL on error.
 * =========================================================================
 */
void llist_rmlink(LINK *linkp)
{
    LINKHD * hp = linkp->headerp;

    if (linkp == hp->headp)
        llist_rmfirst(hp);
    else
        llist_rmafter(linkp->prevp);
}

/*
 * =========================================================================
 * Remove the first link from the list and return a pointer to it.
 * The link is not destroyed.
 *  headerp     the list to look in.
 * Returns the link removed, or NULL on error.
 * =========================================================================
 */
void llist_rmfirst(LINKHD * headerp)
{
    LINK * lp = headerp->headp;
    if (lp == NULL)
        return;

    headerp->headp = lp->nextp;
    if (lp->nextp != NULL)
        lp->nextp->prevp = NULL;

    if (headerp->tailp == lp)
        headerp->tailp = NULL;

    --headerp->nlinks;

    lp->nextp = NULL;
    lp->prevp = NULL;
    lp->headerp = NULL;
}

/*
 * =========================================================================
 * Remove the link after the given link and return a pointer to it.
 * The link is not destroyed.
 *  afterp      the link to look after.
 * Returns the link removed, or NULL on error.
 * =========================================================================
 */
void llist_rmafter(LINK * afterp)
{
    if (afterp->nextp == NULL)
        return;

    if (afterp->nextp == afterp->headerp->tailp)
    {
        llist_rmlast(afterp->headerp);
    }
    else
    {
        LINK * lp = afterp->nextp;

        lp->nextp->prevp = lp->prevp;
        afterp->nextp = lp->nextp;

        --afterp->headerp->nlinks;

        lp->nextp = NULL;
        lp->prevp = NULL;
        lp->headerp = NULL;
    }
}

/*
 * =========================================================================
 * Remove the last link from the list and return a pointer to it.
 * The link is not destroyed.
 *  headerp     the list to look in.
 * Returns the link removed, or NULL on error.
 * =========================================================================
 */
void llist_rmlast(LINKHD * headerp)
{
    LINK * lp = headerp->tailp;
    if (lp == NULL)
        return;

    headerp->tailp = lp->prevp;
    if (lp->prevp != NULL)
        lp->prevp->nextp = NULL;

    if (headerp->headp == lp)
        headerp->headp = NULL;

    --headerp->nlinks;

    lp->nextp = NULL;
    lp->prevp = NULL;
    lp->headerp = NULL;
}

/*
 * =========================================================================
 * Change the arbitrary data pointer bound to a link.
 *  linkp       the link to rebind.
 *  datap       pointer to be bound.
 * Returns linkp, or NULL on error.
 * =========================================================================
 */
LINK * llist_setdata(LINK * linkp, void * datap)
{
    linkp->datap = datap;
    return linkp;

}

/*
 * =========================================================================
 * Return the arbitrary data pointer bound to a link.
 *  linkp       the link to look at.
 * Returns a void pointer, or NULL on error.
 * =========================================================================
 */
void * llist_getdata(LINK * linkp)
{
    return linkp->datap;
}

/*
 * =========================================================================
 * Return the number of links in a list.
 *  headerp     the list to look at.
 * Returns a link count, 0 on error.
 * =========================================================================
 */
unsigned long llist_cnt(LINKHD * headerp)
{
    return headerp->nlinks;
}

/*
 * =========================================================================
 * Return the first link in the list that is bound to the given data
 * pointer. Note that nothing prevents a data pointer from being bound to
 * more than one link.
 *  headerp     the list to look in.
 *  datap       the data pointer to look for.
 * Returns the link found, or NULL on error.
 * =========================================================================
 */
LINK * llist_linkof(LINKHD * headerp, void * datap)
{
    LINK * lp = headerp->headp;

    while (lp != NULL)
        if (lp->datap == datap)
            break;
        else
            lp = lp->nextp;

    return lp;
}

/*
 * =========================================================================
 * Return the next link after the given link that is bound to the given data
 * pointer. Note that nothing prevents a data pointer from being bound to
 * more than one link.
 *  afterp      the link to start from.
 *  datap       the data pointer to look for.
 * Returns the link found, or NULL on error.
 * =========================================================================
 */
LINK * llist_nextlinkof(void * datap, LINK * afterp)
{
    LINK * lp = afterp->nextp;
    while (lp != NULL)
        if (lp->datap == datap)
            break;
        else
            lp = lp->nextp;
    return lp;
}

LINK * llist_getNodeAtIndex(LINKHD * headerp, unsigned long index)
{
    unsigned long i = 0;
    LINK * lp = headerp->headp;

    if (index == 0)
        return headerp->headp;

    for (i = 0; i < headerp->nlinks && lp; i++)
    {
        if (i == index)
        {
            return lp;
        }
        else
        {
            lp = lp->nextp;
        }
    }

    return NULL;
}

/*
 * =========================================================================
 * Return pointer to the header of the list a link is in,
 * or NULL if not in a list, or not a link.
 * =========================================================================
 */
LINKHD * llist_hdrof(LINK * lp)
{
    return lp->headerp;
}

/*
 * =========================================================================
 * SI Caching
 * Enabled by ini setting
 * =========================================================================
 */
/**
 * Serialize SI data in g_si_entry onto a file.  The format of serialization is
 * - struct mpe_SiTableEntry
 * - Names contained for sourceName.
 * - Names contained in long Name
 * - Names contained in description.
 * - next mpe_SiTableEntry;
 */

extern mpe_SiTableEntry *g_si_entry;
extern mpe_siSourceNameEntry *g_si_sourcename_entry;
extern mpe_SiTransportStreamEntry *g_si_oob_ts_entry;
extern mpe_SiTransportStreamEntry *g_si_dsg_ts_entry;
extern mpe_SiTransportStreamEntry *g_si_HN_ts_entry;
extern uint32_t g_frequency[MAX_FREQUENCIES+1];
extern mpe_SiModulationMode g_mode[MAX_FREQUENCIES+1];
extern mpe_SiGlobalState g_si_state;
extern mpe_Bool g_SITPConditionSet;
extern uint32_t g_maxFrequency;
extern uint32_t g_minFrequency;
extern void quickSortArray(uint32_t numbers[], int array_size);

static mpe_Error write_name_string(mpe_File *cache_fd, mpe_SiLangSpecificStringList *name, uint32_t *write_count);
static mpe_Error read_name_string(mpe_File *cache_fd, mpe_SiLangSpecificStringList *name, uint32_t *read_count);

mpe_Error cache_si_entries(const char *siOOBCacheLocation)
{
    mpe_Error err = MPE_SUCCESS;
    mpe_SiTableEntry *walker = NULL;
    mpe_siSourceNameEntry *source_name_entry = NULL;
    mpe_File *cache_fd = NULL;
    mpe_File fd;
    mpe_SiProgramInfo *program_info = NULL;
    uint32_t num_entry_written = 0;
    uint32_t defined_mapped_count = 0;
    uint32_t count = 0;
    // Parameter check
    {
        if (MPE_SUCCESS != mpe_fileDelete(siOOBCacheLocation))
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI, "CACHE: Error deleting file...%s\n", siOOBCacheLocation);
        }
        err = mpe_fileOpen(siOOBCacheLocation, MPE_FS_OPEN_WRITE | MPE_FS_OPEN_MUST_CREATE, &fd);
        if (err == MPE_SUCCESS)
        {
            cache_fd = fd;
        }
        else
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI, "open [%s]  failed with %d\n", siOOBCacheLocation, err);
            err = MPE_SI_INVALID_PARAMETER;
        }
    }
    MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI,"CACHE: starting writing cache\n");
    if (err == MPE_SUCCESS)
    {
        uint32_t write_count = 0;
        uint32_t loop_count = 0;
        uint32_t version = MPE_SI_CACHE_FILE_VERSION;

        // Write header version
        count = sizeof(version);
        err = mpe_fileWrite(cache_fd, &count, (void *)&version);

        if (err == MPE_SUCCESS && (count == sizeof(version)))
        {
            MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI, "writing version to [%s] 0x%x count:%d\n", siOOBCacheLocation, MPE_SI_CACHE_FILE_VERSION, sizeof(version));
            write_count += count;
        }
        else
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI, "writing version to [%s] failed with %d\n", siOOBCacheLocation, err);
            err = MPE_SI_INVALID_PARAMETER;
        }

        if (err == MPE_SUCCESS)
        {
            // Write frequency table
            count = sizeof(g_frequency);
            err = mpe_fileWrite(cache_fd, &count, (void *)g_frequency);
    
            if (err == MPE_SUCCESS && (count == sizeof(g_frequency)))
            {
                MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "writing FREQ to [%s] %d \n", siOOBCacheLocation, count);
                write_count += count;
            }
            else
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI, "writing FREQ to [%s] failed with %d\n", siOOBCacheLocation, err);
                err = MPE_SI_INVALID_PARAMETER;
            }
        }

        if (err == MPE_SUCCESS)
        {
            // Write modulation mode table
            count = sizeof(g_mode);
            err = mpe_fileWrite(cache_fd, &count, (void *)g_mode);
            if (err == MPE_SUCCESS && (count == sizeof(g_mode)))
            {
                MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "writing MODE to [%s] %d \n", siOOBCacheLocation, count);
                write_count += count;
            }
            else
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI, "writing MODE to [%s] failed with %d\n", siOOBCacheLocation, err);
                err = MPE_SI_INVALID_PARAMETER;
            }
        }

        // Write table entries
        walker = g_si_entry;
        while (walker && err == MPE_SUCCESS)
        {
            mpe_SiLangSpecificStringList *name_list = walker->descriptions;
            int                             name_count = 0;
            mpe_SiTransportStreamHandle     ts_handle;

            // OOB, HN??
            if (!( (walker->state == SIENTRY_DEFINED_MAPPED) ||
                   ((walker->state == SIENTRY_MAPPED) &&
                    (walker->ts_handle == (mpe_SiTransportStreamHandle)g_si_dsg_ts_entry))) )
            {
                walker = walker->next;
                continue;
            }

            loop_count++;
            {
                source_name_entry = walker->source_name_entry;
                if (walker->state == SIENTRY_DEFINED_MAPPED)
                {
                    defined_mapped_count++;
                }
                while(name_list)
                {
                    name_count++;
                    name_list = name_list->next;
                }

                walker->source_name_entry   = (mpe_siSourceNameEntry *)0;
                walker->descriptions        = (mpe_SiLangSpecificStringList *)name_count;
                program_info = walker->program;
                if (walker->program != NULL)
                {
                    walker->program = (mpe_SiProgramInfo *)PROGRAM_NUMBER_UNKNOWN;
                }
                else
                {
                    walker->program = (mpe_SiProgramInfo *)0;
                }
                ts_handle = walker->ts_handle;
                if(walker->ts_handle == (mpe_SiTransportStreamHandle)g_si_dsg_ts_entry)
                {
                    walker->ts_handle = MPE_SI_INVALID_HANDLE;
                    walker->program = (mpe_SiProgramInfo *)PROGRAM_NUMBER_UNKNOWN;
                }

                count = sizeof(*walker);
                err = mpe_fileWrite(cache_fd, &count, (void *)walker);
                walker->source_name_entry   = source_name_entry;
                walker->descriptions        = name_list;
                walker->program             = program_info;
                walker->ts_handle           = ts_handle;

                if (count == sizeof(*walker))
                {
                    write_count += count;
                    num_entry_written++;
                    // now copy names
                    {
                        // LEN-NAME, LEN-NAME...
                        err = write_name_string(cache_fd, name_list, &write_count);
                    }
                }
                else
                {
                    MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI, "writing SI to %s failed with %d\n", siOOBCacheLocation, err);
                    err = MPE_SI_INVALID_PARAMETER;
                    break;
                }
                if (1)
                MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI, "WChannelVCN#%06d-SRCID#%06x-Name[%s]-State-[%d]-Freq[%08d]-Mode[%04d]-Prog[%08d]-Vid[%d]-TT[%d]\n",
                        walker->virtual_channel_number,
                        walker->isAppType ? walker->app_id : walker->source_id,
                        (walker->source_name_entry && walker->source_name_entry->source_names && walker->source_name_entry->source_names->string)? walker->source_name_entry->source_names->string : "NULL",
                        walker->state,
                        g_frequency[walker->freq_index],
                        g_mode[walker->mode_index],
                        walker->program_number,
                        walker->video_standard,
                        walker->transport_type);

            }
            walker = walker->next;
        } // For each walker

        MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI, "writing /persistent/si/si_table_cache DONE writing SVCT entries %d bytes %d entries\n", sizeof(*walker), loop_count);

        mpe_fileClose(cache_fd);

        if (defined_mapped_count == 0)
        {
            /* Only OOB channels are cached, discard it */
            MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI, "CACHE: No DEFINED_MAPPED entries...\n");
            if (MPE_SUCCESS != mpe_fileDelete(siOOBCacheLocation))
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI, "CACHE: Error deleting file...%s\n", siOOBCacheLocation);
            }
        }
    }
    MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI,"CACHE: done writing cache %d, %d\n", num_entry_written, defined_mapped_count);

    return err;
}

mpe_Error cache_sns_entries(const char *siOOBSNSCacheLocation)
{
    mpe_Error err = MPE_SUCCESS;
    mpe_siSourceNameEntry *sn_walker = NULL;
    mpe_File *cache_fd = NULL;
    mpe_File fd;
    uint32_t count = 0;
    // Parameter check
    {
        if (MPE_SUCCESS != mpe_fileDelete(siOOBSNSCacheLocation))
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI, "CACHE: Error deleting file...%s\n", siOOBSNSCacheLocation);
        }
        err = mpe_fileOpen(siOOBSNSCacheLocation, MPE_FS_OPEN_WRITE | MPE_FS_OPEN_MUST_CREATE, &fd);
        if (err == MPE_SUCCESS)
        {
            cache_fd = fd;
        }
        else
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI, "open [%s]  failed with %d\n", siOOBSNSCacheLocation, err);
            err = MPE_SI_INVALID_PARAMETER;
        }
    }

    if (err == MPE_SUCCESS)
    {
        uint32_t write_count = 0;
        uint32_t loop_count = 0;
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI,"CACHE: starting writing SNS cache\n");
        // write the source name entry list
        sn_walker = g_si_sourcename_entry;
        while (sn_walker && err == MPE_SUCCESS)
        {
            mpe_SiLangSpecificStringList *name_list[2] = {sn_walker->source_names, sn_walker->source_long_names};
            mpe_SiLangSpecificStringList *name_walker = NULL;
            int                          name_counts[2] = {0};

            if (!(sn_walker->mapped))
            {
                sn_walker = sn_walker->next;
                continue;
            }

            {
                uint8_t list_idx = 0;
                {
                    for (list_idx = 0; list_idx < 2; list_idx++)
                    {
                        uint8_t name_count = 0;
                        name_walker = name_list[list_idx];
                        while(name_walker)
                        {
                            name_count++;
                            name_walker = name_walker->next;
                        }
                        name_counts[list_idx] = name_count;
                    }

                    sn_walker->source_names        = (mpe_SiLangSpecificStringList   *)(name_counts[0]);
                    sn_walker->source_long_names   = (mpe_SiLangSpecificStringList   *)(name_counts[1]);
                }
                count = sizeof(*sn_walker);
                loop_count++;
                err = mpe_fileWrite(cache_fd, &count, (void *)sn_walker);

                sn_walker->source_names        = name_list[0];
                sn_walker->source_long_names   = name_list[1];

                if (count == sizeof(*sn_walker))
                {
                    write_count += count;
                    {
                        // now copy names
                        for (list_idx = 0; list_idx < 2; list_idx++)
                        {
                             // LEN-NAME, LEN-NAME...
                            name_walker = name_list[list_idx];
                            err = write_name_string(cache_fd, name_walker, &write_count);
                        } // For each name list
                    }
                }
                else
                {
                    MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI, "writing SI /persistent/si/si_table_cache failed with %d\n", err);
                    err = MPE_SI_INVALID_PARAMETER;
                    break;
                }
            }

            if (1)
            MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI, "AppType[%d] SourceId[%d], Name[%s] \n",
                    sn_walker->appType,  sn_walker->id,
                    (sn_walker->source_names && sn_walker->source_names->string) ? sn_walker->source_names->string : "NULL");

            sn_walker = sn_walker->next;
        } // For each sn_walker
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI, "writing /persistent/si/si_table_cache DONE writing SNS entries, %d bytes %d entries\n", sizeof(*sn_walker), loop_count);

        mpe_fileClose(cache_fd);
    }
    return err;
}


/**
 * De-serialize SI data from cache file int g_si_entry. The data in g_si_entry is later updated by new tables
 * acquired during SI acquisition.
 */
mpe_Error load_si_entries(const char *siOOBCacheLocation)
{
    mpe_Error err = MPE_SUCCESS;
    mpe_SiTableEntry input_si_entry_buf;
    mpe_SiTableEntry *input_si_entry = &input_si_entry_buf;
    mpe_SiTableEntry *new_si_entry = NULL;
    int ret = -1;
    mpe_File cache_fd;
    uint32_t program_number = 0;
    uint32_t num_entry_read = 0;
    uint32_t count = 0;

    MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI,"CACHE: start loading cache\n");
    {
        mpe_File fd;
        err = mpe_fileOpen(siOOBCacheLocation, MPE_FS_OPEN_READ, &fd);
        if (err == MPE_SUCCESS && fd != NULL)
        {
            cache_fd = fd;
        }
        else
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI, "open [%s] failed with %d\n", siOOBCacheLocation, err);
            return MPE_SI_INVALID_PARAMETER;
        }
    }
    // SI DB write lock is acquired by the caller!
    if (err == MPE_SUCCESS)
    {
        uint32_t read_count = 0;
        uint32_t version;

        // Read header version
        count = sizeof(version);
        err = mpe_fileRead(cache_fd, &count, (void *)&version);

        if (err == MPE_SUCCESS && (count == sizeof(version)))
        {
            read_count += count;
            if (version < MPE_SI_CACHE_FILE_VERSION)
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI, "reading Version failed with %x, expected %x\n", version, MPE_SI_CACHE_FILE_VERSION);
                err =  MPE_SI_INVALID_PARAMETER;
            }
            else
            {
                MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI, "Read version 0x%x\n", version);
            }
        }
        else
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI, "reading Version [%s] failed with err:%d count:%d version:0x%x\n", siOOBCacheLocation, err, count, version);
            err =  MPE_SI_INVALID_PARAMETER;
        }
        if (err == MPE_SUCCESS)
        {
            count = sizeof(g_frequency);
            ret = mpe_fileRead(cache_fd, &count, (void *)g_frequency);
            if (ret == MPE_SUCCESS && (count == sizeof(g_frequency)))
            {
                read_count += count;
            }
            else
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI, "reading FREQ to [%s] failed with %d\n", siOOBCacheLocation, err);
                err =  MPE_SI_INVALID_PARAMETER;
            }
        }

        if (err == MPE_SUCCESS)
        {
            count = sizeof(g_mode);
            ret = mpe_fileRead(cache_fd, &count, (void *)g_mode);
            if (ret == MPE_SUCCESS && count == sizeof(g_mode))
            {
                read_count += count;
            }
            else
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI, "reading MODE from [%s] failed with %d\n", siOOBCacheLocation, err);
                err = MPE_SI_INVALID_PARAMETER;
            }
        }

        MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI, "Reading SVCT entries..\n");
        // Read SVCT entries
        while (err == MPE_SUCCESS)
        {
            // Now read the table entries
            if (err == MPE_SUCCESS && input_si_entry != NULL)
            {
                memset(input_si_entry, 0, sizeof(*input_si_entry));
                {
                    count = sizeof(*input_si_entry);
                    ret = mpe_fileRead(cache_fd, &count, (void *)input_si_entry);
                    if (ret == MPE_SUCCESS && count == sizeof(*input_si_entry))
                    {
                        num_entry_read++;
                        read_count += count;
                        /* Read description names first */
                        mpe_SiLangSpecificStringList **name_list = &input_si_entry->descriptions;
                        int name_count = (int)(input_si_entry->descriptions);
                        {
                            int i = 0;
                            mpe_SiLangSpecificStringList *name_walker = NULL;
                            for (i = 0; i < name_count; i++)
                            {
                                unsigned char buf[4] = {0};

                                count = sizeof(buf);
                                ret = mpe_fileRead(cache_fd, &count, (void *)buf);

                                read_count += count;
                                ret = mpeos_memAllocP(MPE_MEM_SI, sizeof(*name_walker), (void **)&(name_walker));
                                memset(name_walker, 0, sizeof(*name_walker));
                                if (ret == MPE_SUCCESS && name_walker)
                                {
                                    ret = read_name_string(cache_fd, name_walker, &read_count);
                                    *name_list = name_walker;
                                }
                            }
                        }

                        {
                            program_number = (uint32_t)(input_si_entry->program_number);
                            mpe_SiProgramHandle prog_handle = MPE_SI_INVALID_HANDLE;

                            mpe_siGetTransportStreamEntryFromFrequencyModulation(g_frequency[input_si_entry->freq_index], g_mode[input_si_entry->mode_index], &input_si_entry->ts_handle);
                            mpe_siGetProgramEntryFromTransportStreamEntry(input_si_entry->ts_handle, program_number, &prog_handle);
                            mpe_siGetServiceEntryFromChannelNumber(input_si_entry->virtual_channel_number, (mpe_SiServiceHandle *)(&new_si_entry));
                            mpe_siSetSourceId((mpe_SiServiceHandle)new_si_entry, input_si_entry->source_id);
                            mpe_siSetAppType((mpe_SiServiceHandle)new_si_entry, input_si_entry->isAppType);
                            mpe_siSetAppId((mpe_SiServiceHandle)new_si_entry, input_si_entry->app_id);
                            mpe_siSetActivationTime ((mpe_SiServiceHandle)new_si_entry, input_si_entry->activation_time);
                            mpe_siSetChannelNumber((mpe_SiServiceHandle)new_si_entry, input_si_entry->virtual_channel_number, MPE_SI_DEFAULT_CHANNEL_NUMBER);
                            mpe_siSetCDSRef((mpe_SiServiceHandle)new_si_entry, input_si_entry->freq_index);
                            mpe_siSetMMSRef ((mpe_SiServiceHandle)new_si_entry, input_si_entry->mode_index);
                            mpe_siSetChannelType((mpe_SiServiceHandle)new_si_entry, input_si_entry->channel_type);
                            mpe_siSetVideoStandard((mpe_SiServiceHandle)new_si_entry, input_si_entry->video_standard);
                            mpe_siSetServiceType((mpe_SiServiceHandle)new_si_entry, input_si_entry->service_type);
                            mpe_siSetProgramNumber((mpe_SiServiceHandle)new_si_entry, input_si_entry->program_number);
                            mpe_siSetTransportType((mpe_SiServiceHandle)new_si_entry, input_si_entry->transport_type);
                            mpe_siSetScrambled((mpe_SiServiceHandle)new_si_entry, input_si_entry->scrambled);
                            mpe_siSetServiceEntryStateMapped((mpe_SiServiceHandle)new_si_entry);
                            // This field will be reconciled when updateServiceEntries is called
                            new_si_entry->source_name_entry = NULL;
                            new_si_entry->descriptions = input_si_entry->descriptions;
                            new_si_entry->state = input_si_entry->state;
                            mpeos_timeGetMillis(&(new_si_entry->ptime_service));
                        }

                        if (1)
                        MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI, "RChannelVCN#%06d-%s#%06x-State-[%d]-Freq[%08d]-Mode[%04d]-Prog[%08d]\n",
                                new_si_entry->virtual_channel_number,
                                new_si_entry->isAppType ? "APPID" : "SRCID",
                                new_si_entry->isAppType ? new_si_entry->app_id : new_si_entry->source_id,
                                new_si_entry->state,
                                g_frequency[new_si_entry->freq_index],
                                g_mode[new_si_entry->mode_index],
                                new_si_entry->program_number);
                    }
                    else
                    {
                        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI, "Reading [%s] reaches EOF with %d\n", siOOBCacheLocation, err);
                        break;
                    }
                }
            }
        }
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "reading [%s] DONE %d bytes\n", siOOBCacheLocation, read_count);
        mpe_fileClose(cache_fd);
        if (err == MPE_SUCCESS)
        {
            if (num_entry_read > 0)
            {
                g_si_state = SI_FULLY_ACQUIRED;
                g_SITPConditionSet = TRUE;
                MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "SI_FULLY_ACQUIRED FULLY via CACHING\n");
                get_frequency_range(&g_frequency[1], 255, &g_minFrequency, &g_maxFrequency);
            }
            else
            {
                err = MPE_SI_NOT_FOUND;
            }
        }
    }

    // Reconcile service entry's program handle, transport stream handle and service names etc.
    mpe_siUpdateServiceEntries();

    MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI,"CACHE: done loading cache %d\n", num_entry_read);

    return err;
}

mpe_Error load_sns_entries(const char *siOOBSNSCacheLocation)
{
    mpe_Error err = MPE_SUCCESS;
    mpe_siSourceNameEntry input_sn_entry_buf;
    mpe_siSourceNameEntry *input_sn_entry = &input_sn_entry_buf;
    mpe_siSourceNameEntry *new_sn_entry = NULL;

    int ret = -1;
    mpe_File cache_fd;
    uint32_t count = 0;

    MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI,"CACHE: start loading SNS cache\n");
    {
        mpe_File fd;
        err = mpe_fileOpen(siOOBSNSCacheLocation, MPE_FS_OPEN_READ, &fd);
        if (err == MPE_SUCCESS && fd != NULL)
        {
            cache_fd = fd;
        }
        else
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI, "open [%s] failed with %d\n", siOOBSNSCacheLocation, err);
            return MPE_SI_INVALID_PARAMETER;
        }
    }

    // SI DB write lock is acquired by the caller!
    if (err == MPE_SUCCESS)
    {
        uint32_t read_count = 0;

        MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI, "Reading SNS entries..\n");
        // Read SNS entries
        while (err == MPE_SUCCESS)
        {
            if (err == MPE_SUCCESS && input_sn_entry != NULL)
            {
                // Read source name list first
                memset(input_sn_entry, 0, sizeof(*input_sn_entry));
                {
                    count = sizeof(*input_sn_entry);
                    ret = mpe_fileRead(cache_fd, &count, (void *)input_sn_entry);
                    if (ret == MPE_SUCCESS && count == sizeof(*input_sn_entry))
                    {
                        read_count += count;
                        /* Read names */
                        int list_idx = 0;
                        mpe_SiLangSpecificStringList **name_list[2] = {&input_sn_entry->source_names,  &input_sn_entry->source_long_names};
                        int name_counts[2] = {(int)(input_sn_entry->source_names), (int)(input_sn_entry->source_long_names)};
                        for (list_idx = 0; list_idx < 2; list_idx++)
                        {
                            int i = 0;
                            *name_list[list_idx] = NULL;
                            mpe_SiLangSpecificStringList *name_walker = NULL;
                            for (i = 0; i < name_counts[list_idx]; i++)
                            {
                                unsigned char buf[4] = {0};

                                count = sizeof(buf);
                                ret = mpe_fileRead(cache_fd, &count, (void *)buf);

                                read_count += count;
                                ret = mpeos_memAllocP(MPE_MEM_SI, sizeof(*name_walker), (void **)&(name_walker));
                                memset(name_walker, 0, sizeof(*name_walker));
                                if (ret == MPE_SUCCESS && name_walker)
                                {
                                    ret = read_name_string(cache_fd, name_walker, &read_count);
                                    name_walker->next = *name_list[list_idx];
                                    *name_list[list_idx] = name_walker;
                                }
                            }
                        }

                        {
                            mpe_siGetSourceNameEntry(input_sn_entry->id, input_sn_entry->appType, &new_sn_entry, TRUE);
                            // 'Mapped' field is set when updateServiceEntries is called
                            new_sn_entry->source_names = input_sn_entry->source_names;
                            new_sn_entry->source_long_names = input_sn_entry->source_long_names;
                        }

                        if (1)
                        MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI, "AppType[%d] SourceId[%d], Name[%s]\n",
                                new_sn_entry->appType,  new_sn_entry->id,
                                (new_sn_entry->source_names && new_sn_entry->source_names->string) ? new_sn_entry->source_names->string : "NULL");

                    }
                    else
                    {
                        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI, "Reading reaches EOF with %d\n", err);
                        break;
                    }
                }
            }

        }

        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_SI, "reading DONE %d bytes\n", read_count);
        mpe_fileClose(cache_fd);
    }
    return err;
}

static mpe_Error write_name_string(mpe_File *cache_fd, mpe_SiLangSpecificStringList *name, uint32_t *write_count)
{
    mpe_Error err = MPE_SUCCESS;
    int name_len = 0;
    uint32_t count = 0;
    mpe_SiLangSpecificStringList *name_walker = name;
    while(name_walker)
    {
        unsigned char buf[4] = {0};
        /*
         * write len
         * write lanaguage[4]
         * write string
         */
        name_len = sizeof(name_walker->language) + (name_walker->string ? strlen(name_walker->string) : 0);
        buf[0] = name_len     & 0xFF;
        buf[1] =(name_len>>8) & 0xFF;
        buf[2] =(name_len>>16)& 0xFF;
        buf[3] =(name_len>>24)& 0xFF;

        count = sizeof(buf);
        err = mpe_fileWrite(cache_fd, &count, (void *)&buf);
        if (count == sizeof(buf))
        {
            *write_count += count;

            count = sizeof(name_walker->language);

            err = mpe_fileWrite(cache_fd, &count, (void *)name_walker->language);

            if (count == sizeof(name_walker->language))
            {
                *write_count += count;

                if (name_walker->string)
                {
                    uint32_t size;
                    size = strlen(name_walker->string);
                    count = sizeof(uint32_t);

                    err = mpe_fileWrite(cache_fd, &count, (void *)&size);

                    if (count == sizeof(uint32_t))
                    {
                        *write_count += count;
                        if (size > 0)
                        {
                          count = strlen(name_walker->string);
                          err = mpe_fileWrite(cache_fd, &count, (void *)name_walker->string);

                          if (count != size)
                          {
                            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI, "writing SI name failed with %d\n", err);
                            err = MPE_SI_INVALID_PARAMETER;
                          }
                        }
                    }
                    else
                    {
                        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI, "writing SI name length failed with %d\n", err);
                        err = MPE_SI_INVALID_PARAMETER;
                    }
                }
            }
            else
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI, "writing SI name language failed with %d\n", err);
                err = MPE_SI_INVALID_PARAMETER;
            }
        }
        else
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI, "writing SI name total failed with %d\n", err);
            err = MPE_SI_INVALID_PARAMETER;
        }
        name_walker = name_walker->next;
    } // For each name

    return err;
}

static mpe_Error read_name_string(mpe_File *cache_fd, mpe_SiLangSpecificStringList *name, uint32_t *read_count)
{
    uint32_t count = 0;
    mpe_Error ret = MPE_SUCCESS;
    mpe_SiLangSpecificStringList *name_walker = name;

    if (name_walker)
    {
        uint32_t size;
        count = sizeof(name_walker->language);
        ret = mpe_fileRead(cache_fd, &count, (void *)name_walker->language);

        *read_count += count;

        ret = mpe_fileRead(cache_fd, &count, (void *)&size);
        *read_count += count;
        mpeos_memAllocP(MPE_MEM_SI, size+1, (void **)&(name_walker->string));
        memset(name_walker->string, 0, size + 1);
        count = size;

        ret = mpe_fileRead(cache_fd, &count, (void *)name_walker->string);
        *read_count += count;
    }
    return ret;
}

void get_frequency_range(uint32_t *freqArr, int count, uint32_t *minFreq, uint32_t *maxFreq )
{
    int i = 0;
    int tempCount = 0;
    uint32_t tempArray[MAX_FREQUENCIES+1] = {0};
    for (i = 0; i < (sizeof(tempArray) / sizeof(tempArray[0])); i++)
    {
        tempArray[i] = 0;
    }
    i = 0;
    while (i < count)
    {
        if (freqArr[i] != 0)
        {
            tempArray[i] = freqArr[i];
            tempCount++;
        }
        i++;
    }
    // If the frequency array is valid sort it
    // and set the min and max frequency values
    if(tempCount != 0)
    {
        quickSortArray(tempArray, tempCount);
        *minFreq = tempArray[0];
        *maxFreq = tempArray[tempCount - 1];
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI,"<get_frequency_range> g_minFrequency: %d, g_maxFrequency: %d out of %d frequencies\n", *minFreq, *maxFreq, tempCount);
    }
    else
    {
        *minFreq = 0;
        *maxFreq = 0;
    }
}

// --------------------------------------------------------------
//                          CRC UTIL
// --------------------------------------------------------------

#define CRC_QUOTIENT 0x04C11DB7
/*
 * This table should be initialized once
 */
static uint32_t crctab[256];

void init_mpeg2_crc(void)
{
    uint16_t i, j;
    uint32_t crc;

    for (i = 0; i < 256; i++)
    {
        crc = i << 24;
        for (j = 0; j < 8; j++)
        {
            if (crc & 0x80000000)
                crc = (crc << 1) ^ CRC_QUOTIENT;
            else
                crc = crc << 1;
        }
        crctab[i] = crc;
    }
} // END crc32_init()

/*
 *  CRC calculation function
 */
uint32_t calc_mpeg2_crc(uint8_t * data, uint32_t len)
{
    uint32_t result;
    uint32_t i;

    if (len < 4)
        return 0;

    result = *data++ << 24;
    result |= *data++ << 16;
    result |= *data++ << 8;
    result |= *data++;
    result = ~result;
    len -= 4;

    for (i = 0; i < len; i++)
    {
        result = (result << 8 | *data++) ^ crctab[result >> 24];
    }

    return ~result;
} // END calc_mpeg2_crc()

/**
 * The <i>si_getFileSize()</i> function will get the size the specified file.
 *
 * @param fileName Is a pointer for the name of the file
 * @param size Is the size to file to be checked.
 * @return The size of the given file. -1 if the file not present or any other failure.
 *          is returned.
 */
unsigned int si_getFileSize(const char * location, unsigned int *size)
{
    mpe_FileInfo info;
    if (mpe_fileGetStat(location, MPE_FS_STAT_SIZE, &info) == MPE_FS_ERROR_SUCCESS)
    {
        *size = (unsigned int) info.size;
        return *size;
    }
    return -1;
}

mpe_Error write_crc (const char * pSICacheFileName, unsigned int crcValue)
{
    mpe_Error err = MPE_SUCCESS;
    uint32_t readDataSize = 0;
    mpe_File cache_fd = 0;
    mpe_File fd = 0;
    uint32_t sizeOfSICache = 0;
    unsigned char *pActualData = NULL;
    si_getFileSize(pSICacheFileName, &sizeOfSICache);

    if (sizeOfSICache > 0)
    {
        err = mpeos_memAllocP(MPE_MEM_SI, sizeOfSICache, (void**) &pActualData);
    }

    if ((err == MPE_SUCCESS) && (pActualData != NULL))
    {
        err = mpe_fileOpen(pSICacheFileName, MPE_FS_OPEN_READ, &fd);
        if (err == MPE_SUCCESS)
        {
            cache_fd = fd;
        }
        else
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI, "open [%s]  failed with %d\n", pSICacheFileName, err);
            err = MPE_SI_INVALID_PARAMETER;
        }
        if (err == MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI,"CACHE: starting writing cache\n");
            readDataSize = sizeOfSICache;
            err = mpe_fileRead(cache_fd, &readDataSize, pActualData);
            if (err == MPE_SUCCESS)
            {
                // The file pointer is moved bcoz of read operations, so close and reopen the file to write the data again.
                err = mpe_fileClose(cache_fd);
                err = mpe_fileOpen(pSICacheFileName, MPE_FS_OPEN_WRITE, &fd);

                if (err == MPE_SUCCESS)
                {
                    cache_fd = fd;
                    err = mpe_fileWrite(cache_fd, &readDataSize, (void *)pActualData);
                    /* Write the CRC value */
                    if (err == MPE_SUCCESS)
                    {
                        uint32_t crcValueReceived = crcValue;
                        readDataSize = sizeof(crcValue);
                        err = mpe_fileWrite(cache_fd, &readDataSize, (void *)&crcValueReceived);
                    }
                }
                else
                {
                    MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI, "Writing original data failed\n");
                    err = MPE_SI_INVALID_PARAMETER;
                }

                if (err == MPE_SUCCESS)
                    MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI, "CRC Appended to the end of file\n");
                else
                    MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI, "Failed to append CRC to the end of file\n");
            }
            /* Close the file */
            mpe_fileClose(cache_fd);
        }
        else
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI, "Failed to open a file\n");
        }
        mpeos_memFreeP (MPE_MEM_SI, pActualData);
    }
    else
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI, "Failed to allocate memory");
    }
    return err;
}

/**
 * The <i>write_crc_for_si_and_sns_cache()</i> function will verify the
 * CRC value that is written in the size with calculated CRC.
 *
 * @param pSICache Is a pointer for the name of the SI Cache file
 * @param pSISNSCache Is a pointer for the name of the SISNS Cache file
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error write_crc_for_si_and_sns_cache(const char* pSICache, const char* pSISNSCache)
{
    mpe_Error err = MPE_SUCCESS;
    mpe_File fdSNSCache = 0;
    mpe_File fdSICache = 0;
    unsigned char *pCRCData = NULL;
    uint32_t sizeOfSICache = 0;
    uint32_t sizeOfSISNSCache = 0;
    uint32_t crcFileSize = 0;
    uint32_t readSNSSize = 0;
    uint32_t readSISize = 0;
    uint32_t crcValue = 0xFFFFFFFF;

    si_getFileSize(pSICache, &sizeOfSICache);
    si_getFileSize(pSISNSCache, &sizeOfSISNSCache);

    readSNSSize = sizeOfSISNSCache;
    readSISize = sizeOfSICache;

    crcFileSize = sizeOfSISNSCache + sizeOfSICache;
    err = mpeos_memAllocP(MPE_MEM_SI, crcFileSize,(void**) &pCRCData);
    if (err == MPE_SUCCESS && pCRCData != NULL)
    {

        err = mpe_fileOpen(pSISNSCache, MPE_FS_OPEN_READ, &fdSNSCache);
        if ((err == MPE_SUCCESS) && (fdSNSCache != NULL))
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI, "open [%s] success\n", pSISNSCache);
            err = mpe_fileRead(fdSNSCache, &readSNSSize, pCRCData);
            if ((err != MPE_SUCCESS))
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI, "Read less than the size of the file\n");
            }
            mpe_fileClose(fdSNSCache);
        }
        else
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI, "open [%s] failed with %d\n", pSISNSCache, err);
            return err;
        }

        /* Read the SICache file, even if readSNSSize != sizeOfSISNSCache */
        err = mpe_fileOpen(pSICache, MPE_FS_OPEN_READ, &fdSICache);
        if ((err == MPE_SUCCESS) && (fdSICache != NULL))
        {
            /* Read from SI cache file. the buffer shd be offsetted to SNScache size */
            err = mpe_fileRead(fdSICache, &readSISize, (pCRCData + readSNSSize));
            if ((err != MPE_SUCCESS))
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI, "Read less than the size of the file\n");
            }
            mpe_fileClose(fdSICache);
        }
        else
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI, "open [%s] failed with %d\n", pSICache, err);
            return err;
        }

        /* Find the CRC and write it at the end of SICache file */
        crcValue = calc_mpeg2_crc (pCRCData, crcFileSize);
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI, "Calculated CRC is [%u]\n", crcValue);
        err = write_crc(pSICache, crcValue);
        mpeos_memFreeP (MPE_MEM_SI, pCRCData);
        return err;
    }
    return err;
}

/**
 * The <i>verify_crc_for_si_and_sns_cache()</i> function will verify the
 * CRC value that is written in the size with calculated CRC.
 *
 * @param pSICache Is a pointer for the name of the SI Cache file
 * @param pSISNSCache Is a pointer for the name of the SISNS Cache file
 * @param pStatus Is the result of CRC comparison.
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error verify_crc_for_si_and_sns_cache(const char* pSICache, const char* pSISNSCache, int *pStatus)
{
    mpe_Error err = MPE_SUCCESS;
    mpe_File fdSNSCache = 0;
    mpe_File fdSICache = 0;
    unsigned char *pCRCData = NULL;
    uint32_t sizeOfSICache = 0;
    uint32_t sizeOfSISNSCache = 0;
    uint32_t crcFileSize = 0;
    uint32_t readSNSSize = 0;
    uint32_t readSISize = 0;
    uint32_t crcValue = 0xFFFFFFFF;
    uint32_t crcRead = 0xFFFFFFFF;

    *pStatus = 0;

    si_getFileSize(pSICache, &sizeOfSICache);
    si_getFileSize(pSISNSCache, &sizeOfSISNSCache);

    readSNSSize = sizeOfSISNSCache;
    // SI file size includes the CRC at the end
    // of the file
    readSISize = sizeOfSICache; // SIFilesize+sizeof(CRC)

    crcFileSize = sizeOfSISNSCache + sizeOfSICache;
    err = mpeos_memAllocP(MPE_MEM_SI, crcFileSize,(void**) &pCRCData);
    if (err == MPE_SUCCESS && pCRCData != NULL)
    {
        err = mpe_fileOpen(pSISNSCache, MPE_FS_OPEN_READ, &fdSNSCache);
        if ((err == MPE_SUCCESS) && (fdSNSCache != NULL))
        {
            MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI, "open [%s] success\n", pSISNSCache);
            err = mpe_fileRead(fdSNSCache, &readSNSSize, pCRCData);
            if ((err != MPE_SUCCESS))
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI, "Read less than the size of the file\n");
            }
            mpe_fileClose(fdSNSCache);
        }
        else
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI, "open [%s] failed with %d\n", pSISNSCache, err);
            return err;
        }

        /* Read the SICache file, even if readSNSSize != sizeOfSISNSCache */
        err = mpe_fileOpen(pSICache, MPE_FS_OPEN_READ, &fdSICache);
        if ((err == MPE_SUCCESS) && (fdSICache != NULL))
        {
            /* Read from SI cache file. the buffer should be offset to SNScache size */
            err = mpe_fileRead(fdSICache, &readSISize, (pCRCData + readSNSSize));
            if ((err != MPE_SUCCESS))
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI, "Read less than the size of the file\n");
            }
            mpe_fileClose(fdSICache);
        }
        else
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI, "open [%s] failed with %d\n", pSICache, err);
            return err;
        }

        /* Read the CRC and compare */
        readSISize = sizeof(crcValue);
        // subtract the CRC size and compute the CRC from SI/SNS data
        crcValue = calc_mpeg2_crc (pCRCData, (crcFileSize - readSISize));
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI, "Calculated CRC is [%u]\n", crcValue);

        // CRC retrieved from the last 4 bytes of SI file
        memcpy (&crcRead, &pCRCData[crcFileSize - readSISize], readSISize);

        MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI, "CRC Read from file [%u]\n", crcRead);
        if(crcValue == crcRead)
        {
            *pStatus = 1;
            MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI, "CRC Matched.....\n");
        }
        else
        {
            MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI, "CRC did not match.....\n");
        }
        mpeos_memFreeP (MPE_MEM_SI, pCRCData);
    }
    return err;
}

/**
 * The <i>verify_version_and_crc()</i> function will verify the version of cache and
 * CRC value that is written in the size with calculated CRC.
 *
 * @param siOOBCacheLocation Is a pointer for the name of the SI Cache file
 * @param siOOBSNSCacheLocation Is a pointer for the name of the SISNS Cache file
 * @return The result of verification of version and crc. 0 when fails and 1 when success is returned.
 */
int verify_version_and_crc (const char *siOOBCacheLocation, const char *siOOBSNSCacheLocation)
{
    mpe_File cache_fd;
    mpe_Error err = MPE_SUCCESS;
    int proceed = 0;
    int isCRCMatch = 0;

    mpe_File fd;
    err = mpe_fileOpen(siOOBCacheLocation, MPE_FS_OPEN_READ, &fd);
    if (err == MPE_SUCCESS && fd != NULL)
    {
        cache_fd = fd;
    }
    else
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI, "open [%s] failed with %d\n", siOOBCacheLocation, err);
    }

    // SI DB write lock is acquired by the caller!
    if (err == MPE_SUCCESS)
    {
        uint32_t read_count = 0;
        uint32_t version;
        uint32_t count = 0;

        // Read header version
        count = sizeof(version);
        err = mpe_fileRead(cache_fd, &count, (void *)&version);

        if (err == MPE_SUCCESS && (count == sizeof(version)))
        {
            read_count += count;
            if (version < MPE_SI_CACHE_FILE_VERSION)
            {
                MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI, "reading Version failed with %x, expected %x\n", version, MPE_SI_CACHE_FILE_VERSION);
                proceed = 0;
            }
            else
            {
                MPE_LOG(MPE_LOG_INFO, MPE_MOD_SI, "Read version 0x%x\n", version);
                proceed = 1;
            }
        }
        else
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_SI, "reading Version [%s] failed with err:%d count:%d version:0x%x\n", siOOBCacheLocation, err, count, version);
        }

        mpe_fileClose(cache_fd);
    }

    /* check CRC only when version matches.. */
    if ((err == MPE_SUCCESS) && (proceed == 1))
    {
        err = verify_crc_for_si_and_sns_cache (siOOBCacheLocation, siOOBSNSCacheLocation, &isCRCMatch);
    }

    if ((err != MPE_SUCCESS) || (isCRCMatch == 0) || (proceed == 0))
    {
        return 0;
    }

    /* Return SUCCESS, so that the calling method can proceed */
    return 1;
}


mpe_Error verify_si_cache_files_exist(const char *siFileLocation, const char *snsFileLocation)
{
    mpe_Error err = MPE_SUCCESS;
    mpe_File fd1, fd2;
    err = mpe_fileOpen(siFileLocation, MPE_FS_OPEN_READ, &fd1);
    if (err == MPE_SUCCESS)
    {
        mpe_fileClose(fd1);
        err = mpe_fileOpen(snsFileLocation, MPE_FS_OPEN_READ, &fd2);
        if (err == MPE_SUCCESS)
        {
            mpe_fileClose(fd2);
            return MPE_SUCCESS;
        }
    }
    return err;
}
