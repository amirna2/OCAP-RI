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
//        Â·Redistributions of source code must retain the above copyright notice, this list 
//             of conditions and the following disclaimer.
//        Â·Redistributions in binary form must reproduce the above copyright notice, this list of conditions 
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

/** \file test_sys_mem.c
 *
 *  \brief Test functions for MPEOS memory functions
 *
 *  This file contains tests for the following MPEOS functions :\n
 *
 *    -# mpeos_memAllocH()\n
 *    -# mpeos_memAllocP()\n
 *    -# mpeos_memCompact()\n
 *    -# mpeos_memFreeH()\n
 *    -# mpeos_memFreeP()\n
 *    -# mpeos_memGetFreeSize()\n
 *    -# mpeos_memGetLargestFree()\n
 *    -# mpeos_memGetStats()\n
 *    -# mpeos_memInit()\n
 *    -# mpeos_memLockH()\n
 *    -# mpeos_memPurge()\n
 *    -# mpeos_memReallocH()\n
 *    -# mpeos_memReallocP()\n
 *    -# mpeos_memStats()\n
 */

#define _MPE_MEM
#define TEST_MPE_MEM    /* Force resolution of test mem bindings. */

#include <test_sys.h>

#include <stdlib.h>
#include <mpe_sys.h>
#include <mpeos_mem.h>

CuSuite* getTestSuite_sysMem(void);

#ifdef TEST_MPEOS
# define initMem mpeos_memInit         /* need to call 'mpeos_memInit()' if testing MPEOS  */
#else
# define initMem memInitNull           /* 'mpeos_memInit()' has already been called if testing MPE */
static void memInitNull(void);
static void memInitNull()
{
    return;
}
#endif /* TEST_MPEOS */

/*
 *  'memFrag' struct. Represents an allocated chunk of test memory
 */

typedef struct memFrag
{
    void *mem; /* pointer to allocated memory  */
    void *memprev; /* previous pointer to allocated memory  */
    int size; /* size of allocated memory, in bytes  */
    uint32_t first; /* value of first four bytes of fill value  */
    uint32_t crc; /* value of CRC-32 over allocated and filled memory */
} memFrag;

#define PATTERN (0xB16B00B5)
#define ALLOCSIZE (32*1024)

#define FRAGFRAGS 17                   /* Number of allocations used to force fragmentation  */
#define FRAGSIZE  (ALLOCSIZE/2)+1      /* Default size of fragmentation allocations  */

/*  Minimum and maximum memory sizes. Right now these are guesses as
 *  to what are reasonable limits.
 */

#define MEMORY_MIN    8388608  /* 8 Mbytes minimum  */
#define MEMORY_MAX  268435456  /* 256 Mbytes maximum */

/*
 *  Test function declarations
 */
static void vte_test_memAllocP(CuTest*);
static void vte_test_memFreeP(CuTest*);
static void vte_test_memReallocP(CuTest*);
static void vte_test_memAllocH(CuTest*);
static void vte_test_memFreeH(CuTest*);
static void vte_test_memLockH(CuTest*);
static void vte_test_memReallocH(CuTest*);
static void vte_test_memCompact(CuTest*);
static void vte_test_memGetFreeSize(CuTest*);
static void vte_test_memGetLargestFree(CuTest*);
static void vte_test_memGetStats(CuTest*);
static void vte_test_memPurge(CuTest*);
static void vte_test_memStats(CuTest*);

/*
 *  Internal utility functions
 */
static void genTable(void);
static uint32_t get_crc(unsigned char*, int, uint32_t);
static uint32_t update_crc(unsigned char*, int, uint32_t);
static long int checkFree(mpe_MemColor, long int);
static void fragmentMemory(mpe_MemColor color, uint32_t fragSize, int freeIt);
static void fillMem(memFrag*);
static int checkFill(memFrag*);
static void checkMem(mpe_MemHandle, memFrag, CuTest*);
/* static void testcrc(void); */

/*
 *  Memory system inited flag. If true, indicates that 'mpeos_memInit()'
 *  has been called already.
 */

#ifdef TEST_MPEOS
static int g_memInited = 0; /*  initially indicate that 'memInit() has not been called */
#else
static int g_memInited = 1; /*  if doing MPE testing, 'memInit() has already been called */
#endif /* TEST_MPEOS */

/****************************************************************************
 *
 *  vte_test_memAllocP()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_memAllocP" function 
 *
 * \api mpeos_memAllocP()
 *
 * \strategy Call the "mpeos_memAllocP()" function with a variety of valid
 *           and invalid parameters and checks for correct return values.
 *
 * \assets none
 *
 */

static void vte_test_memAllocP(CuTest *tc)
{
    mpe_Error ec;
    void *mem;
    uint32_t *p;
    uint32_t size = ALLOCSIZE;
    long int initialFree;
    long int freeDiff;
    mpe_MemColor bogusColor;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_memAllocP\n");

    if (!g_memInited)
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  Calling 'mpeos_memInit()'\n");
        initMem();
        g_memInited = 1;
    }

    bogusColor = MPE_MEM_NCOLORS + 3;

    /*  get initial free memory size  */

    initialFree = checkFree(MPE_MEM_TEST, 0);

    /**
     * \assertion "MPE_EINVAL" is returned if a size of zero is passed
     */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  calling 'memAllocP()' with zero size\n");
    ec = memAllocP(MPE_MEM_TEST, 0, &mem);
    CuAssertIntEquals_Msg(tc, "memAllocP with zero size failed", MPE_EINVAL, ec);

    /**
     * \assertion "MPE_EINVAL" is returned if a NULL pointer is passed
     */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  calling 'memAllocP()' with NULL pointer\n");
    ec = memAllocP(MPE_MEM_TEST, size, NULL);
    CuAssertIntEquals_Msg(tc, "memAllocP with NULL pointer failed", MPE_EINVAL,
            ec);

    /**
     * \assertion memAllocP() with an invalid color returns MPE_SUCCESS and allocates
     *  default memory color
     */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  calling 'memAllocH()' with invalid color (%d)\n", bogusColor);
    ec = memAllocP(bogusColor, size, &mem);
    if (MPE_EINVAL != ec)
    {
        memFreeP(bogusColor, mem);
        CuFail(tc, "memAllocP with invalid color failed");
    }

    /**
     * \assertion "MPE_SUCCESS" is returned if valid size and pointer are passed
     */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  calling 'memAllocP()' with valid size and pointer\n");
    ec = memAllocP(MPE_MEM_TEST, size, &mem);
    CuAssert(tc, "memAllocP failed", ec == MPE_SUCCESS);

    /**
     * \assertion Allocated memory is readable and writable
     */

    p = (uint32_t *) mem;
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  attempting to write begining of allocated memory\n");
    *p = PATTERN;
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  attempting to read begining of allocated memory\n");
    CuAssert(tc, "memAllocP() failed, can't write to 1st address", *p
            == PATTERN);

    p += ((ALLOCSIZE / sizeof(uint32_t)) - 1);

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  attempting to write last address of allocated memory\n");
    *p = PATTERN;
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  attempting to read last address of allocated memory\n");
    CuAssert(tc, "memAllocP() failed, can't write to last address", *p
            == PATTERN);

    /*
     * \assertion free memory available has decreased accordinly
     */
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "Checking memory for proper allocation against free memory");
    freeDiff = checkFree(MPE_MEM_TEST, initialFree);
    CuAssert(tc, "allocation has not changed free memory available", size
            == freeDiff);

    /*  free allocated memory  */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  attempting to free allocated memory\n");
    ec = memFreeP(MPE_MEM_TEST, mem);
    CuAssert(tc, "memFreeP(mem) failed", ec == MPE_SUCCESS);

    /**
     * \assertion All memory allocated is freed and the amount of free memory at
     *  the end of the test is the same as at the begining.
     */

    freeDiff = checkFree(MPE_MEM_TEST, initialFree);
    CuAssert(tc, "possible memory leak.", 0 == freeDiff);

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "  vte_test_memAllocP finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_memFreeP()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_memFreeP" function 
 *
 * \api mpeos_memFreeP()
 *
 * \strategy Call the "mpeos_memFreeP()" function with a variety of valid and
 *  invalid parameters and checks for correct results.
 * return values.
 *
 * \assets none
 *
 */

static void vte_test_memFreeP(CuTest *tc)
{
    mpe_Error ec;
    void *mem;
    uint32_t size = ALLOCSIZE;
    long int initialFree;
    long int freeDiff;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_memFreeP\n");

    if (!g_memInited)
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  Calling 'mpeos_memInit()'\n");
        initMem();
        g_memInited = 1;
    }

    /*  get initial free memory size  */

    initialFree = checkFree(MPE_MEM_TEST, 0);

    /**
     * \assertion "MPE_EINVAL" is returned if a NULL pointer is passed
     */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  Attempting to call 'memFreeP()' with a NULL pointer\n");

    ec = memFreeP(MPE_MEM_TEST, NULL);
    CuAssertIntEquals_Msg(tc, "memFreeP(NULL) failed", MPE_EINVAL, ec);

    /* Allocate some memory so we can try to free it */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  Attempting to allocate memory\n");
    ec = memAllocP(MPE_MEM_TEST, size, &mem);
    CuAssert(tc, "  memAllocP(MPE_MEM_TEST, size, &mem) failed", ec
            == MPE_SUCCESS);

#ifndef POWERTV    /*  PowerTV complains about the color mismatch, then
                       goes ahead and frees the memory                    */
    /**
     * \assertion "MPE_EINVAL" is returned if an invalid color is passed
     */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  Attempting to call 'memFreeP()' with an invalid color\n");
    ec = memFreeP(MPE_MEM_NCOLORS + 1, mem);
    CuAssert(tc, "memFreeP(MPE_MEM_NCOLORS+1, &mem) failed", MPE_EINVAL == ec);
#endif

    /**
     * \assertion "MPE_SUCCESS" is returned if a valid pointer and color are passed
     */
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  Attempting to call 'memFreeP()' with valid parameters\n");
    ec = memFreeP(MPE_MEM_TEST, mem);
    CuAssert(tc, "memFreeP(MPE_MEM_TEST, mem) failed", ec == MPE_SUCCESS);

#if (!defined(POWERTV)) /* !! (!defined(WIN32)) */

    /**
     * \assertion "MPE_SUCCESS" is returned if memory is freed twice
     */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  Attempting to free the same memory twice\n");
    ec = memFreeP(MPE_MEM_TEST, mem);
    CuAssert(tc, "memFreeP(MPE_MEM_TEST, mem) a second time failed", ec
            == MPE_SUCCESS);

#endif

    /**
     * \assertion All memory allocated is freed and the amount of free memory at
     *  the end o fthe test i sthe same as at the begining.
     */

    freeDiff = checkFree(MPE_MEM_TEST, initialFree);
    CuAssert(tc, "possible memory leak.", 0 == freeDiff);

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "  vte_test_memFreeP finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_memReallocP()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_memReallocP" function 
 *
 * \api mpeos_memFreeP()
 *
 * \strategy Call the "mpeos_memReallocP()" function with a variety of
 *  valid and invalid parameters and checks for correct results.
 *
 * \assets none
 *
 */

static void vte_test_memReallocP(CuTest *tc)
{
    mpe_Error ec;
    void *mem = NULL;
    void *memTwo = NULL;
    void *memOriginal = NULL;
    uint32_t *p;
    uint32_t size = ALLOCSIZE;
    uint32_t newsize = ALLOCSIZE * 2;
    long int initialFree;
    long int freeDiff;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_memReallocP\n");

    if (!g_memInited)
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  Calling 'mpeos_memInit()'\n");
        initMem();
        g_memInited = 1;
    }

    /*  get initial free memory size  */

    initialFree = checkFree(MPE_MEM_TEST, 0);

    /* Allocate some memory */

    ec = memAllocP(MPE_MEM_TEST, size, &mem);
    CuAssert(tc, "memAllocP(size, &mem) allocation failed", ec == MPE_SUCCESS);

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  Allocated memory at %p\n", mem);

    /* Now write test patterns to memory. */

    p = (uint32_t *) mem;
    *p = PATTERN;
    CuAssert(
            tc,
            "memReallocP(size, &mem) failed, can't write to 1st address before reallocation",
            PATTERN == *p);
    p += ((ALLOCSIZE / sizeof(uint32_t)) - 1);
    *p = PATTERN;
    CuAssert(
            tc,
            "memReallocP(size, &mem) failed, can't write to last address before reallocation",
            PATTERN == *p);

    /*  cause some fragmentation  */

    fragmentMemory(MPE_MEM_TEST, FRAGSIZE, FALSE);

    /**
     * \assertion "MPE_EINVAL" is returned if 'memReallocP()' is called with a NULL pointer
     */

    ec = memReallocP(MPE_MEM_TEST, newsize, NULL);
    CuAssert(tc, "memReallocP(newsize, NULL) failed", MPE_EINVAL == ec);

    /**
     * \assertion "MPE_SUCCESS" is returned if 'memReallocP()' is called with valid arguments
     */

    memOriginal = mem; /* save original memory pointer  */
    ec = memReallocP(MPE_MEM_TEST, newsize, &mem);
    CuAssert(tc, "memReallocP(newsize, mem) failed", ec == MPE_SUCCESS);

    if (mem != memOriginal)
    {
        TRACE(
                MPE_LOG_DEBUG,
                MPE_MOD_TEST,
                "  'memReallocP()' caused memory to move.\n    Old location was %p, new location is %p\n",
                memOriginal, mem);
    }
    else
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "  'memReallocP()' did not cause memory to move.\n");
    }

    /**
     * \assertion Memory contents are intact after reallocation
     */

    p = (uint32_t *) mem;
    CuAssert(
            tc,
            "memReallocP(size, &mem) failed, content at 1st address not valid after expansion",
            *p == PATTERN);
    p += (((ALLOCSIZE) / sizeof(uint32_t)) - 1);
    CuAssert(
            tc,
            "memReallocP(size, &mem) failed, content at last address not valid after expansion",
            *p == PATTERN);

    /**
     * \assertion Newly expanded memory is readable and writable
     */

    p = (uint32_t *) mem + (((ALLOCSIZE * 2) / sizeof(uint32_t)) - 1);
    *p = PATTERN;
    CuAssert(
            tc,
            "memReallocP(size, &mem) failed, can't write to new last address after expansion",
            *p == PATTERN);

    /* Check 0 size case, which will free the memory at 'mem'.*/

    ec = memReallocP(MPE_MEM_TEST, 0, &mem);
    CuAssert(tc, "memReallocP(MPE_MEM_TEST, 0, &mem) failed", MPE_SUCCESS == ec);

    /**
     * \assertion Calling memReallocP()' with a NULL memory pointer causes new
     *  memory to be allocated.
     */

    ec = memReallocP(MPE_MEM_TEST, size, &memTwo);
    CuAssert(tc, "memReallocP(MPE_MEM_TEST, size, &memTwo) failed", MPE_SUCCESS
            == ec);

    /**
     * \assertion Calling memReallocP()' with 'size' == 0 causes
     *  memory to be free'd.
     */

    ec = memReallocP(MPE_MEM_TEST, 0, &memTwo);
    CuAssert(tc, "memReallocP(MPE_MEM_TEST, 0, &memTwo) failed", MPE_SUCCESS
            == ec);

    /*  
     *  Could try some invalid arguments : negative size, very large size
     */

    /*  cleanup forced fragmentation  */

    fragmentMemory(MPE_MEM_TEST, 0, TRUE);

    /**
     * \assertion All memory allocated is freed and the amount of free memory at
     *  the end of the test is the same as at the begining.
     */

    freeDiff = checkFree(MPE_MEM_TEST, initialFree);
    CuAssert(tc, "possible memory leak.", 0 == freeDiff);

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "  vte_test_memReallocP finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_memAllocH()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_memAllocH" function 
 *
 * \api mpeos_memAllocP()
 *
 * \strategy Call the "mpeos_memAllocH()" function with a variety of valid
 *           and invalid parameters and checks for correct return values.
 *           Attempt to force memory compaction and make sure allocated
 *           memory isn't disturbed.
 *
 * \assets none
 *
 */

static void vte_test_memAllocH(CuTest *tc)
{
    mpe_Error ec;
    uint32_t *p1;
    uint32_t *p2;
    uint32_t size = ALLOCSIZE;
    mpe_MemColor memColors = MPE_MEM_NCOLORS;
    mpe_MemColor bogusColor;

    mpe_MemHandle hand;
    //mpe_MemHandle hand2;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_memAllocH\n");

    if (!g_memInited)
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  Calling 'mpeos_memInit()'\n");
        initMem();
        g_memInited = 1;
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  %d memory colors are defined\n",
                (int) memColors);
    }

    bogusColor = memColors + 3;

    /**
     * \assertion "MPE_EINVAL" is returned if a size of zero is passed
     */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  calling 'memAllocH()' with zero size\n");
    ec = memAllocH(MPE_MEM_TEST, 0, MPE_MEM_NOPURGE, &hand);
    CuAssertIntEquals_Msg(tc, "memAllocH with zero size failed", MPE_EINVAL, ec);

    /**
     * \assertion "MPE_EINVAL" is returned if a NULL handle is passed
     */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  calling 'memAllocH()' with NULL handle\n");
    ec = memAllocH(MPE_MEM_TEST, size, MPE_MEM_PRIOR_LOW, NULL);
    CuAssertIntEquals_Msg(tc, "memAllocH with NULL handle failed", MPE_EINVAL,
            ec);

    /**
     * \assertion "MPE_EINVAL" is returned if an invalid priority paramenters are passed in
     */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  calling 'memAllocH()' with invalid priority of 1000\n");
    ec = memAllocH(MPE_MEM_TEST, size, 1000, &hand);
    if (MPE_EINVAL != ec)
    {
        memFreeH(MPE_MEM_TEST, hand);
        CuFail(tc, "memAllocH with invalid priority failed");
    }

    /**
     * \assertion memAllocH() with an invalid color returns MPE_SUCCESS and allocates
     *  default memory color
     */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  calling 'memAllocH()' with invalid color (%d)\n", bogusColor);
    ec = memAllocH(bogusColor, size, MPE_MEM_PRIOR_LOW, &hand);
    if (MPE_EINVAL != ec)
    {
        memFreeH(bogusColor, hand);
        CuFail(tc, "memAllocH with invalid color failed");
    }

    /**
     * \assertion "MPE_SUCCESS" is returned if valid color, size and handle are passed
     */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  calling 'memAllocH()' with valid size and pointer\n");
    ec = memAllocH(MPE_MEM_TEST, size, MPE_MEM_PRIOR_LOW, &hand);
    CuAssert(tc, "memAllocH failed", ec == MPE_SUCCESS);

    /**
     * \assertion Allocated memory can be locked and a valid pointer to the
     *  locked memory obtained.
     */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  calling 'memLockH()'\n");
    p1 = NULL;
    ec = memLockH(hand, (void *) &p1);
    CuAssert(tc, "memLockH failed", ec == MPE_SUCCESS);
    CuAssert(tc, "memLockH returned a NULL pointer", p1 != NULL);
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  'memLockH()' returned %d, memory at %d\n", ec, p1);

    /**
     * \assertion Allocated and locked memory can be written and read.
     */

    if (NULL != p1)
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "  writing and reading test memory at %p\n", p1);
        *p1 = PATTERN;
        CuAssert(tc, "Can't write to begining of allocated memory", *p1
                == PATTERN);
        p1 += ((ALLOCSIZE / sizeof(uint32_t)) - 1);
        *p1 = PATTERN;
        CuAssert(tc, "Can't write to end of allocated memory", *p1 == PATTERN);
    }
    else
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "  'memLockH()' returned NULL pointer\n");
    }

    /*
     try doing an unlock before a lock
     try doing a second lock while still locked
     */

    /**
     * \assertion Locked memory can be unlocked.
     */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  calling 'memLockH()' to unlock\n");
    ec = memLockH(hand, NULL);
    CuAssert(tc, "memLockH failed to unlock", ec == MPE_SUCCESS);

    /**
     * \assertion Allocated memory can be locked a second time
     */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  calling 'memLockH()' again\n");
    p2 = NULL;
    ec = memLockH(hand, (void *) &p2);
    CuAssert(tc, "memLockH failed", ec == MPE_SUCCESS);
    CuAssert(tc, "memLockH returned a NULL pointer", p2 != NULL);

    /**
     * \assertion Allocated memory hasn't been altered.
     */

    if (NULL != p2)
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  reading test memory at %p\n", p2);
        CuAssert(tc, "Begining of allocated memory has been altered", *p2
                == PATTERN);
        p2 += ((ALLOCSIZE / sizeof(uint32_t)) - 1);
        CuAssert(tc, "End of allocated memory has been altered", *p2 == PATTERN);
    }
    else
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "  'memLockH()' returned NULL pointer\n");
    }

    /**
     * \assertion Locked memory can be unlocked again.
     */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  calling 'memLockH()' to unlock\n");
    ec = memLockH(hand, NULL);
    CuAssert(tc, "memLockH failed to unlock", ec == MPE_SUCCESS);

    /**
     * \assertion Memory can be free'ed.
     */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  calling 'memFreeH()'\n");
    ec = memFreeH(MPE_MEM_TEST, hand);
    CuAssert(tc, "memFreeH failed", ec == MPE_SUCCESS);

    /**
     * \assertion "MPE_SUCCESS" is returned if valid color, size and handle are passed on non-purgable memeory
     */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  calling 'memAllocH()' with non-purgable memory\n");
    ec = memAllocH(MPE_MEM_TEST, size, MPE_MEM_NOPURGE, &hand);
    CuAssert(tc, "memAllocH failed", ec == MPE_SUCCESS);

    /**
     * \assertion Memory can be free'ed.
     */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  calling 'memFreeH()'\n");
    ec = memFreeH(MPE_MEM_TEST, hand);
    CuAssert(tc, "memFreeH failed", ec == MPE_SUCCESS);

    /**
     * \ create a large allocation of non-purgable memory
     */
    /*
     TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
     "  calling 'memAllocH()' to get available space\n");
     ec = memGetFreeSize(MPE_MEM_TEST, &size);
     CuAssert(tc, "memAllocH failed", ec == MPE_SUCCESS);

     TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
     "  calling 'memAllocH()' to allocate large non-purgable memory\n");
     ec = memAllocH(MPE_MEM_TEST, size, MPE_MEM_NOPURGE, &hand);
     CuAssert(tc, "memAllocH failed", ec == MPE_SUCCESS);
     */
    /**
     * \ try and allocate more memory , this should fail with MPE_ENOMEM
     */
    //ec = memAllocH(MPE_MEM_TEST, size, MPE_MEM_NOPURGE, &hand2);
    //CuAssert(tc, "memAllocH failed", ec == MPE_ENOMEM);

    /**
     * \assertion Memory can be free'ed.
     */
    //TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  calling 'memFreeH()'\n");
    //ec = memFreeH(MPE_MEM_TEST, hand);
    //CuAssert(tc, "memFreeH failed", ec == MPE_SUCCESS);

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "  vte_test_memAllocH finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_memFreeH()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_memFreeH" function 
 *
 * \api mpeos_memFreeH()
 *
 * \strategy Call the "mpeos_memFreeH()" function with a variety of valid
 *           and invalid parameters and checks for correct return values.
 *
 * \assets none
 *
 */

static void vte_test_memFreeH(CuTest *tc)
{
    mpe_Error ec;
    uint32_t size = ALLOCSIZE;
    mpe_MemColor memColors = MPE_MEM_NCOLORS;
    mpe_MemColor bogusColor;

    mpe_MemHandle hand;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_memFreeH\n");

    if (!g_memInited)
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  Calling 'mpeos_memInit()'\n");
        initMem();
        g_memInited = 1;
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  %d memory colors are defined\n",
                (int) memColors);
    }

    bogusColor = memColors + 3;

    /**
     * \assertion "MPE_EINVAL" is returned if a NULL handle and valid color
     *  are passed
     */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  calling 'memFreeH()' with NULL handle and valid color\n");
    ec = memFreeH(MPE_MEM_TEST, NULL);
    CuAssertIntEquals_Msg(tc, "memFreeH with NULL handle and valid color",
            MPE_EINVAL, ec);

    /**
     * \assertion "MPE_EINVAL" is returned if a NULL handle and invalid color
     *  are passed
     */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  calling 'memFreeH()' with NULL handle and invalid color\n");
    ec = memFreeH(bogusColor, NULL);
    CuAssertIntEquals_Msg(tc, "memFreeH with NULL handle and invalid color",
            MPE_EINVAL, ec);

    /*
     * Allocate test memory
     */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  calling 'memAllocH()' to get test memory\n");
    ec = memAllocH(MPE_MEM_TEST, size, MPE_MEM_PRIOR_HIGH, &hand);
    CuAssertIntEquals_Msg(tc, "memAllocH() of test memory failed", MPE_SUCCESS,
            ec);

    /**
     * \assertion "MPE_EINVAL" is returned if an invalid color is passed
     */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  calling 'memFreeH()' with valid handle and invalid color\n");
    ec = memFreeH(bogusColor, hand);
    CuAssertIntEquals_Msg(tc, "memFreeH with valid handle and invalid color",
            MPE_EINVAL, ec);

    /*
     *  The previous test case may or may not have actually freed the test memory,
     *  depending on whether it passed or failed, and exactly how it failed. The
     *  exact behaviour will be platform specific. In any case, at this point we
     *  don't know if our test memory has been freed or not, so we free it again
     *  (and ignore any errors) and allocate it again for the next test.
     *  On PowerTV the test memory does get freed by the previous test case, and
     *  freeing it again causes a crash, so we don't free it again. Other platforms
     *  may also crash; if so, skip the free.
     */

#ifndef POWERTV
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  calling 'memFreeH()' to free test memory\n");
    memFreeH(MPE_MEM_TEMP, hand);
#endif  /* #ifndef POWERTV */

    /*
     * Allocate test memory (again)
     */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  calling 'memAllocH()' to get test memory\n");
    ec = memAllocH(MPE_MEM_TEST, size, MPE_MEM_PRIOR_HIGH, &hand);
    CuAssertIntEquals_Msg(tc, "memAllocH() of test memory failed", MPE_SUCCESS,
            ec);

    /**
     * \assertion "MPE_EINVAL" is returned if the wrong color is passed in with the handle
     */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  calling 'memFreeH()' with valid handle and invalid color\n");
    ec = memFreeH(bogusColor, hand);
    CuAssertIntEquals_Msg(tc, "memFreeH with valid handle and invalid color",
            MPE_EINVAL, ec);

    /*
     *  The previous test case may or may not have actually freed the test memory,
     *  depending on whether it passed or failed, and exactly how it failed. The
     *  exact behaviour will be platform specific. In any case, at this point we
     *  don't know if our test memory has been freed or not, so we free it again
     *  (and ignore any errors) and allocate it again for the next test.
     *  On PowerTV the test memory does get freed by the previous test case, and
     *  freeing it again causes a crash, so we don't free it again. Other platforms
     *  may also crash; if so, skip the free.
     */

#ifndef POWERTV
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  calling 'memFreeH()' to free test memory\n");
    memFreeH(MPE_MEM_TEST, hand);
#endif  /* #ifndef POWERTV */

    /*
     * Allocate test memory (again)
     */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  calling 'memAllocH()' to get test memory\n");
    ec = memAllocH(MPE_MEM_TEST, size, MPE_MEM_PRIOR_HIGH, &hand);
    CuAssertIntEquals_Msg(tc, "memAllocH() of test memory failed", MPE_SUCCESS,
            ec);

    /**
     * \assertion "MPE_SUCCESS" is returned if valid parameters are passed
     */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  calling 'memFreeH()' with valid handle and valid color\n");
    ec = memFreeH(MPE_MEM_TEST, hand);
    CuAssertIntEquals_Msg(tc, "memFreeH with valid handle and valid color",
            MPE_SUCCESS, ec);

    /**
     * \assertion "MPE_SUCCESS" is returned if the same memory is freed twice
     *  NOTE : This crashes some systems (like PowerTV)
     */

#ifndef POWERTV
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  calling 'memFreeH()' to free the same memory twice\n");
    ec = memFreeH(MPE_MEM_TEST, hand);
    CuAssertIntEquals_Msg(tc, "memFreeH() called twice for same memory",
            MPE_SUCCESS, ec);
#endif  /* #ifndef POWERTV */

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "  vte_test_memFreeH finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_memLockH()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "memLockH" function 
 *
 * \api memLockH()
 *
 * \strategy Call "memLockH()" with a variety of valid and invalid parameters
 *   and check for correct return values. Verifiy that memory can be written
 *   and read, and that it retains it's value after being unlocked and then
 *   relocked.
 *
 * \assets none
 *
 */

static void vte_test_memLockH(CuTest *tc)
{
    mpe_Error ec;
    //  mpe_TimeMillis mSec;
    uint32_t *p1;
    uint32_t *p2;
    uint32_t size = ALLOCSIZE;
    mpe_MemColor memColors = MPE_MEM_NCOLORS;
    mpe_MemHandle hand;
    MPE_UNUSED_PARAM(memColors);

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_memLockH\n");

    if (!g_memInited)
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  Calling 'mpeos_memInit()'\n");
        initMem();
        g_memInited = 1;
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  %d memory colors are defined\n",
                (int) memColors);
    }

    /**
     * \assertion "MPE_SUCCESS" is returned if valid color, size and handle are passed
     */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  calling 'memAllocH()' with valid size and pointer\n");
    ec = memAllocH(MPE_MEM_TEST, size, MPE_MEM_PRIOR_LOW, &hand);
    CuAssert(tc, "memAllocH failed", ec == MPE_SUCCESS);

    /**
     * \assertion Allocated memory can be locked and a valid pointer to the
     *  locked memory obtained.
     */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  calling 'memLockH()'\n");
    p1 = NULL;
    ec = memLockH(hand, (void *) &p1);
    CuAssert(tc, "memLockH failed", ec == MPE_SUCCESS);
    CuAssert(tc, "memLockH returned a NULL pointer", p1 != NULL);

    /**
     * \assertion Allocated and locked memory can be written and read.
     */

    if (NULL != p1)
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "  trying to write and read locked memory\n");
        *p1 = PATTERN;
        CuAssert(tc, "Can't write to begining of allocated memory", *p1
                == PATTERN);
        p1 += ((ALLOCSIZE / sizeof(uint32_t)) - 1);
        *p1 = PATTERN;
        CuAssert(tc, "Can't write to end of allocated memory", *p1 == PATTERN);
    }
    else
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "  'memLockH()' returned NULL pointer\n");
    }

    /*
     try doing an unlock before a lock
     try doing a second lock while still locked
     */

    /**
     * \assertion Locked memory can be unlocked.
     */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  calling 'memLockH()' to unlock\n");
    ec = memLockH(hand, NULL);
    CuAssert(tc, "memLockH failed to unlock", ec == MPE_SUCCESS);

    /**
     * \assertion Allocated memory can be locked a second time
     */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  calling 'memLockH()' again\n");
    p2 = NULL;
    ec = memLockH(hand, (void *) &p2);
    CuAssert(tc, "memLockH failed", ec == MPE_SUCCESS);
    CuAssert(tc, "memLockH returned a NULL pointer", p2 != NULL);
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  'memLockH()' returned %p\n", p2);

    /**
     * \assertion Allocated memory hasn't been altered.
     */

    if (NULL != p2)
    {
        CuAssert(tc, "Begining of allocated memory has ben altered", *p2
                == PATTERN);
        p2 += ((ALLOCSIZE / sizeof(uint32_t)) - 1);
        CuAssert(tc, "End of allocated memory has been altered", *p2 == PATTERN);
    }
    else
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "  'memLockH()' returned NULL pointer\n");
    }

    /**
     * \assertion Locked memory can be unlocked again.
     */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  calling 'memLockH()' to unlock\n");
    ec = memLockH(hand, NULL);
    CuAssert(tc, "memLockH failed to unlock", ec == MPE_SUCCESS);

    /**
     * \assertion Memory can be free'ed.
     */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  calling 'memFreeH()'\n");
    ec = memFreeH(MPE_MEM_TEST, hand);
    CuAssert(tc, "memFreeH failed", ec == MPE_SUCCESS);

    /*  try memFreeH() with invalid parameters  */

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "  vte_test_memLockH finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_memReallocH()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "memReallocH" function 
 *
 * \api memReallocH()
 *
 * \strategy Call "memReallocH()" with a variety of valid and invalid
 *   parameters and check for correct return values. Fragment memory, then
 *   expand allocated memory and make sure the contents are not disturbed.
 *
 * \assets none
 *
 */

static void vte_test_memReallocH(CuTest *tc)
{
    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_memReallocH\n");

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "  vte_test_memReallocH finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_memCompact()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "memCompact" function 
 *
 * \api memCompact()
 *
 * \strategy Call "memCompact()" with a variety of valid and invalid parameters
 *   and check for correct return values. Try to force memory fragmentation
 *   and then compact it.
 *
 * \assets none
 *
 */

static void vte_test_memCompact(CuTest *tc)
{
#ifdef POWERTV
    mpe_Error ec;
    mpe_MemColor memColors = MPE_MEM_NCOLORS;
    mpe_MemColor bogusColor;

    mpe_MemHandle hand1;
    memFrag mem1 =
    {   NULL,NULL,ALLOCSIZE,0,0};
    mpe_MemHandle hand2;
    memFrag mem2 =
    {   NULL,NULL,ALLOCSIZE,0,0};
#endif

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_memCompact\n");

#ifdef POWERTV
    if (!g_memInited)
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  Calling 'mpeos_memInit()'\n");
        initMem();
        g_memInited = 1;
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  %d memory colors are defined\n", (int)memColors);
    }
    bogusColor = memColors + 3;

    /**
     * \assertion "MPE_EINVAL" is returned if an invalid color is passed
     */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  calling 'memCompact()' with invalid color\n");
    ec = memCompact(bogusColor);
    CuAssertIntEquals_Msg(tc, "memCompact() with invalid color failed", MPE_EINVAL, ec);

    /**
     * \assertion "MPE_SUCCESS" is returned if a valid color is passed
     */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  calling 'memCompact()' with valid color\n");
    ec = memCompact(MPE_MEM_TEST);
    CuAssertIntEquals_Msg(tc, "memCompact() with valid color failed", MPE_SUCCESS, ec);

    /*
     * Now try to force memory fragmentation, compact and check for corruption
     */

    /*  allocate memory by handle  */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  calling 'memAllocH()'\n");
    ec = memAllocH(MPE_MEM_TEST, ALLOCSIZE, MPE_MEM_PRIOR_LOW, &hand1);
    CuAssert(tc, "memAllocH failed", ec == MPE_SUCCESS);

    /*  lock allocated memory, remember address  */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  calling 'memLockH()'\n");
    ec = memLockH(hand1, &mem1.mem);
    CuAssert(tc, "memLockH failed", ec == MPE_SUCCESS);
    CuAssert(tc, "memLockH returned a NULL pointer", mem1.mem != NULL);
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  'memLockH()' returned %p\n", mem1.mem);
    mem1.memprev = mem1.mem; /*  save current memory address  */

    /* fill allocated memory, then unlock  */

    fillMem (&mem1);
    ec = memLockH(hand1, NULL);
    CuAssert(tc, "failed to unlock test block 1", ec == MPE_SUCCESS);

    /* fragment memory  */

    fragmentMemory(MPE_MEM_TEST, FRAGSIZE, FALSE);

    /*  allocate more memory by handle  */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  calling 'memAllocH()'\n");
    ec = memAllocH(MPE_MEM_TEST, ALLOCSIZE, MPE_MEM_PRIOR_LOW, &hand2);
    CuAssert(tc, "memAllocH failed", ec == MPE_SUCCESS);

    /*  lock allocated memory, remember address  */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  calling 'memLockH()'\n");
    ec = memLockH(hand2, &mem2.mem);
    CuAssert(tc, "memLockH failed", ec == MPE_SUCCESS);
    CuAssert(tc, "memLockH returned a NULL pointer", mem2.mem != NULL);
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  'memLockH()' returned %p\n", mem2.mem);
    mem2.memprev = mem2.mem; /*  save current memory address  */

    /* fill allocated memory, then unlock  */

    fillMem (&mem2);
    ec = memLockH(hand2, NULL);
    CuAssert(tc, "failed to unlock test block 2", ec == MPE_SUCCESS);

    /**
     * \assertion "MPE_SUCCESS" is returned by 'memCompact()'
     */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  calling 'memCompact()'\n");
    ec = memCompact(MPE_MEM_TEST);
    CuAssertIntEquals_Msg(tc, "memCompact() failed", MPE_SUCCESS, ec);

    /**
     * \assertion Compacting memory does not alter memory contents
     */

    /*  check if test memory moved (not an error) or was altered  */

    checkMem(hand1, mem1, tc);
    checkMem(hand2, mem2, tc);

    /* free fragmented memory  */

    fragmentMemory(MPE_MEM_TEST, FRAGSIZE, TRUE);

    /**
     * \assertion "MPE_SUCCESS" is returned by 'memCompact()'
     */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  calling 'memCompact()'\n");
    ec = memCompact(MPE_MEM_TEST);
    CuAssertIntEquals_Msg(tc, "memCompact() failed", MPE_SUCCESS, ec);

    /*  check if test memory moved (not an error) or was altered  */

    checkMem(hand1, mem1, tc);
    checkMem(hand2, mem2, tc);

    /*  free first test block  */

    ec = memFreeH(MPE_MEM_TEST, hand1);
    CuAssert(tc, "memFreeH failed for first test block", ec == MPE_SUCCESS);

    /**
     * \assertion "MPE_SUCCESS" is returned by 'memCompact()'
     */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  calling 'memCompact()'\n");
    ec = memCompact(MPE_MEM_TEST);
    CuAssertIntEquals_Msg(tc, "memCompact() failed", MPE_SUCCESS, ec);

    /*  check if test memory moved (not an error) or was altered  */

    checkMem(hand2, mem2, tc);

    /*  free second test block  */

    ec = memFreeH(MPE_MEM_TEST, hand2);
    CuAssert(tc, "memFreeH failed for second test block", ec == MPE_SUCCESS);

    /**
     * \assertion "MPE_SUCCESS" is returned by 'memCompact()'
     */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  calling 'memCompact()'\n");
    ec = memCompact(MPE_MEM_TEST);
    CuAssertIntEquals_Msg(tc, "memCompact() failed", MPE_SUCCESS, ec);
#else
    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST,
            "  vte_test_memCompact not applicable.\n\n");
#endif
    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "  vte_test_memCompact finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_memGetFreeSize()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "memGetFreeSize" function 
 *
 * \api memGetFreeSize()
 *
 * \strategy Call the "memGetFreeSize()" function with a variety of valid
 *           and invalid parameters and checks for correct return values.
 *
 * \assets none
 *
 */

static void vte_test_memGetFreeSize(CuTest *tc)
{
    mpe_Error ec;
    uint32_t fsize = 0;
    mpe_MemColor bogusColor = MPE_MEM_NCOLORS + 1;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_memGetFreeSize\n");

    if (!g_memInited)
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  Calling 'mpeos_memInit()'\n");
        initMem();
        g_memInited = 1;
    }

    /**
     * \assertion "MPE_EINVAL" is returned if a NULL pointer is passed
     */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  calling 'memGetFreeSize()' with NULL pointer\n");
    ec = memGetFreeSize(MPE_MEM_GENERAL, NULL);
    CuAssert(tc, "memGetFreeSize(NULL) failed", ec == MPE_EINVAL);

    /**
     * \assertion "MPE_EINVAL" is returned if an invalid color is passed
     */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  calling 'memGetFreeSize()' with invalid color\n");
    if (MPE_EINVAL != (ec = memGetFreeSize(bogusColor, &fsize)))
    {
        TRACE(
                MPE_LOG_DEBUG,
                MPE_MOD_TEST,
                "    'memGetFreeSize()' with invalid color (%d) returned %d, expected %d, size = %u\n",
                (int) bogusColor, ec, MPE_EINVAL, fsize);
        CuFail(tc, "memGetFreeSize() with invalid color failed");
    }

    /**
     * \assertion "MPE_SUCCESS" is returned if valid color and pointer are passed
     */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  calling 'memGetFreeSize()' with valid parameters\n");
    ec = memGetFreeSize(MPE_MEM_GENERAL, &fsize);
    CuAssert(tc, "memGetFreeSize() failed to return a value", ec == MPE_SUCCESS
            && fsize != 0);
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  Free size = %u\n", fsize);

    /**
     * \assertion Value for amount of free memory is within extected range
     */

    CuAssert(tc, "memGetFreeSize() returned a value out of range", (fsize
            >= MEMORY_MIN) && (fsize <= MEMORY_MAX));

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST,
            "  vte_test_memGetFreeSize finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_memGetLargestFree()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "memGetLargestFree" function 
 *
 * \api memGetLargestFree()
 *
 * \strategy Call the "memGetLargestFree()" and prints the result
 *
 * \assets none
 *
 */

static void vte_test_memGetLargestFree(CuTest *tc)
{
    mpe_Error ec;
    uint32_t fsize = 0;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_memGetLargestFree\n");

    if (!g_memInited)
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  Calling 'mpeos_memInit()'\n");
        initMem();
        g_memInited = 1;
    }

    /* Check null pointer case: */

    ec = memGetLargestFree(MPE_MEM_GENERAL, NULL);
    CuAssert(tc, "memGetLargestFree(NULL) failed", ec == MPE_EINVAL);

    ec = memGetLargestFree(MPE_MEM_GENERAL, &fsize);
    CuAssert(tc, "memGetLargestFree(NULL) failed to return a value", ec
            == MPE_SUCCESS && fsize != 0);

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST,
            "  vte_test_memGetLargestFree finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_memGetStats()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "memGetStats" function 
 *
 * \api vte_test_memGetStats()
 *
 * \strategy Call the "memGetStats()" function to get memory statistics
 *
 * \assets none
 *
 */

static void vte_test_memGetStats(CuTest *tc)
{
    mpe_Error ec;
    mpe_MemStatsInfo *stats = NULL;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_memGetStats\n");

    if (!g_memInited)
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  Calling 'mpeos_memInit()'\n");
        initMem();
        g_memInited = 1;
    }

    /*  allocate memory to hold returned  stats  */

    ec = memAllocP(MPE_MEM_GENERAL, (sizeof(struct mpe_MemStatsInfo)
            * MPE_MEM_NCOLORS) + 10, (void*) &stats);

    CuAssert(tc, "memAllocP() failed to allocate memory for stats buffer", ec
            == MPE_SUCCESS);
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  Allocated memory for stats at %p\n",
            stats);

    /**
     * \assertion 'mem_GetStats()' returns MPE_SUCCESS
     */

    ec = memGetStats(sizeof(struct mpe_MemStatsInfo) * MPE_MEM_NCOLORS, stats);
    CuAssert(tc, "memGetStats() failed.", ec == MPE_SUCCESS);

    /*
     *  also try with invalid parameters (NULL pointer, bad size)
     */

    /*
     *  could also evaluate returned parameters, for example check that
     *  'currAllocated' <= 'maxAllocated' for each color.
     */

    /*
     *  could also get stats, then allocate a particular color, then get stats
     *  again and make sure amount of free memory decreased by the expected amount.
     *  This may be tricky, since the requested size may be rounded up, and because
     *  other things in the system may be allocating and freeing memory at the
     *  same time test code is running. This is especially true when running MPE
     *  testing.
     */

    /*
     *  optionally dump returned stats
     */

    /*  free memory allocated for returned stats  */

    if (NULL != stats)
    {
        ec = memFreeP(MPE_MEM_GENERAL, (void*) stats);
        CuAssert(tc, "error freeing memory allocated for stats", ec
                == MPE_SUCCESS);
    }

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "  vte_test_memGetStats finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_memPurge()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "memPurge" function 
 *
 * \api memStats()
 *
 * \strategy Call the "memPurge()" function with a variety of valid and
 *   invalid parameters
 *
 * \assets none
 *
 */

static void vte_test_memPurge(CuTest *tc)
{
    mpe_Error ec;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_memPurge\n");

    if (!g_memInited)
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  Calling 'mpeos_memInit()'\n");
        initMem();
        g_memInited = 1;
    }

    /**
     * \assertion 'memPurge()' returns MPE_SUCCESS
     */

    ec = memPurge(MPE_MEM_GENERAL, MPE_MEM_PRIOR_HIGH);
    CuAssert(tc, "memPurge() failed", ec == MPE_SUCCESS);

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "  vte_test_memPurge finished.\n\n");
}

/****************************************************************************
 *
 *  vte_test_memStats()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "memStats" function 
 *
 * \api memStats()
 *
 * \strategy Call the "memStats()" function to display memory statistics
 *
 * \assets none
 *
 */

static void vte_test_memStats(CuTest *tc)
{

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entered vte_test_memStats\n");

    if (!g_memInited)
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  Calling 'mpeos_memInit()'\n");
        initMem();
        g_memInited = 1;
    }
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\n  You should see a bunch of memory stats printed after this line:\n");

    /**
     * \assertion 'memStats()' prints memory statistics
     */

    memStats(TRUE, MPE_MEM_SYSTEM,
            "  'memStats()' called from 'vte_test_memStats()'\n\n");

    threadSleep(500, 0);
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\n\n  You should see a bunch of memory stats printed above this line\n\n");

    TRACE(MPE_LOG_TRACE2, MPE_MOD_TEST, "  vte_test_memStats finished.\n\n");
}

/****************************************************************************
 *
 *   Internal utility functions.
 *
 ***************************************************************************/

/****************************************************************************
 *
 *  Check if a block of memory moved, either by compaction or realloc
 */

static void checkMem(mpe_MemHandle hand, memFrag mem, CuTest* tc)
{
    mpe_Error ec;

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  locking test block'\n");
    ec = memLockH(hand, &mem.mem);
    CuAssert(tc, "memLockH failed", ec == MPE_SUCCESS);
    CuAssert(tc, "memLockH returned a NULL pointer", mem.mem != NULL);
    if (mem.mem != mem.memprev)
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "  test block moved, was at %p, now at %p\n", mem.memprev,
                mem.mem);
    }
    else
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "  test block did not move, still at %p\n", mem.memprev);
    }

    if (!checkFill(&mem))
    {
        CuFail(tc, "compaction caused memory corruption");
    }
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  unlocking test block\n");
    ec = memLockH(hand, NULL);
    CuAssert(tc, "failed to unlock test block", ec == MPE_SUCCESS);

    return;
}

#if 0 /* TODO: unused function */
/****************************************************************************
 *
 *   printMemStats() - Print some memory statistics
 *
 ***************************************************************************/
static void printMemStats(void)
{
    mpe_Error ec;
    mpe_MemColor memColor;
    uint32_t freeTotal;
    uint32_t freeLargest;

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  printMemStats :\n");

    for (memColor = MPE_MEM_GENERAL; memColor < MPE_MEM_NCOLORS; memColor++)
    {
        ec = memGetFreeSize(memColor, &freeTotal);
        if (MPE_SUCCESS != ec)
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "  memGetFreeSize() failed to return a value");
        }
        else
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "    Color %2d free == %9u,", memColor, freeTotal);
        }

        if (NULL != memGetLargestFree)
        {
            ec = memGetLargestFree(memColor, &freeLargest);
            if (MPE_SUCCESS != ec)
            {
                TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                        "\n  memGetLargestFree() failed to return a value");
            }
            else
            {
                TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                        " largest free == %9u", freeLargest);
            }

        }
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "\n");
    }

    memStats(TRUE, (mpe_MemColor)-1, NULL);
}
#endif

/****************************************************************************
 *
 *  checkFree() - Check amount of free memory
 *
 ***************************************************************************/
/*
 *  Gets the amount of free memory of a specified color and optionaly
 *  compares it to a previous value.
 *
 */

static long int checkFree(mpe_MemColor color, long int oldFree)
{
    mpe_Error ec;
    uint32_t currentFree = 0;

    ec = memGetFreeSize(color, &currentFree);
    if (0 == oldFree)
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "  checkFree()' - currentFree = %u\n", currentFree);
        return ((long int) currentFree);
    }
    else
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "  checkFree()' - currentFree = %u, oldFree = %d, diff = %d\n",
                currentFree, oldFree, (oldFree - currentFree));
        return (oldFree - currentFree);
    }
}

/****************************************************************************
 *
 *  fragmentMemory() - Cause memory fragmentation
 *
 ***************************************************************************/
/*
 *  Attempt to cause memory fragmentation by allocating and freeing memory
 *  Allocates a bunch of blocks of memory, then frees every other one. This
 *  should leave a bunch of "holes" in free memory. Also writes patterns to
 *  allocated memory, then checks the patterns when freeing memory.
 *
 */

static void fragmentMemory(mpe_MemColor color, uint32_t fragSize, int freeIt)
{
    //    static void *mem[FRAGFRAGS+1];
    static memFrag xxxmem[FRAGFRAGS + 1];

    static int fragged = 0;
    int i;

    if (freeIt) /*  free previously allocated memory  */
    {
        if (fragged)
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "  fragmentMemory() - cleaning up\n");
            for (i = 1; i < FRAGFRAGS; i += 2) /*  free odd numbered fragments  */
            {
                if (!checkFill(&xxxmem[i]))
                {
                    TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                            "\nERROR : 'checkFill()' detected memory corruption\n");
                }
                if (memFreeP(MPE_MEM_TEST, xxxmem[i].mem) != MPE_SUCCESS)
                {
                    TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                            "    error freeing fragment at %p\n", xxxmem[i].mem);
                }
                else
                {
                    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                            "    freed fragment %2d at %p\n", i, xxxmem[i].mem);
                }
                xxxmem[i].mem = NULL;
                xxxmem[i].size = 0;

            }
            fragged = 0;
        }
        else
        {
            TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                    "\n  fragmentMemory() - ####  nothing to clean up  ####\n\n");
        }
    }
    else /*  allocate memory  */
    {
        if (!fragged)
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "  fragmentMemory() - allocating\n");
            for (i = 0; i < FRAGFRAGS; i++)
            {
                if (memAllocP(color, fragSize, &xxxmem[i].mem) != MPE_SUCCESS)
                {
                    TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                            "    error allocating fragment %2\n", i);
                }
                else
                {
                    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                            "    alloc fragment %2d at %p\n", i, xxxmem[i].mem);
                    xxxmem[i].size = fragSize;
                    fillMem(&xxxmem[i]);
                }
            }
            for (i = 0; i < FRAGFRAGS; i += 2) /*  free the even numbered fragments  */
            {
                if (memFreeP(MPE_MEM_TEST, xxxmem[i].mem) != MPE_SUCCESS)
                {
                    TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                            "    error freeing fragment %2d at %p\n",
                            xxxmem[i].mem);
                }
                else
                {
                    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                            "    freed fragment %2d at %p\n", i, xxxmem[i].mem);
                }
                xxxmem[i].mem = NULL;
                xxxmem[i].size = 0;
            }
            fragged = 1;
        }
        else
        {
            TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                    "\n  fragmentMemory() - ####  already fragmented  ####\n\n");
        }
    }
}

/****************************************************************************
 *
 *  'fillMem' - fills a block of memory with random data and computes
 *   a CRC over the block.
 *
 *  TODO : allow caller to pass in an id value, which gets put
 *         into the block, so you can tell blocks apart.
 */

static void fillMem(memFrag *fp)
{
    static unsigned int rseed = 0;
    unsigned char *cp;
    uint32_t len;
    uint32_t crc = 0xffffffff;
    mpe_TimeMillis mSec;
    mpe_Error ec;

    if (0 == rseed)
    {
        ec = timeGetMillis(&mSec);
        if (ec != MPE_SUCCESS)
        {
            TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                    "\nERROR : 'timeGetMillis()' failed.\n\n");
        }

        rseed = (unsigned int) (mSec / 7); /* there's a small chance that this will  */
        /* result in a seed value == 0, in which  */
        /* case we will do it again next time.    */
        /* That's OK.                             */

        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  random seed == 0x%08x\n", rseed);
        srand(rseed);
    }

    if (NULL == fp)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "\n  fillMem() - passed a NULL struct pointer\n\n");
        return;
    }

    if (NULL == fp->mem)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "\n  fillMem() - passed a NULL memory pointer\n\n");
        return;
    }
    if (fp->size < 4)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "\n  fillMem() - passed memory size is too small (%d)\n\n",
                fp->size);
        return;
    }

    /*  everything looks OK, so let's fill 'er up  */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "    fillMem() @ %p, size == %6d, ",
            fp->mem, fp->size);

    for (cp = (unsigned char*) fp->mem, len = fp->size; len > 0; len--)
    {
        *cp++ = (unsigned char) (rand() >> 7); /* discard lower bits */
    }

    fp->first = *((uint32_t*) (fp->mem));
    fp->crc = get_crc((unsigned char*) fp->mem, fp->size, crc);

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "first four bytes == 0x%08x\n",
            fp->first);

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "                  crc == 0x%08x, end of mem @ %p\n\n", fp->crc, cp
                    - 1);
}

/****************************************************************************
 *
 *  'checkFill' - Checks the CRC of a block of memory previously filled
 *   by 'fillMem()'.
 *
 *  Returns : TRUE if CRC is correct,
 *            FALSE is CRC is wrong
 *
 */

static int checkFill(memFrag *fp)
{
    uint32_t crc = 0xffffffff;

    if (NULL == fp)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "\n  checkFill() - passed a NULL struct pointer\n\n");
        return (FALSE);
    }

    if (NULL == fp->mem)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "\n  checkFill() - passed a NULL memory pointer\n\n");
        return (FALSE);
    }
    if (fp->size < 4)
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "\n  checkFill() - passed memory size is too small (%d)\n\n",
                fp->size);
        return (FALSE);
    }

    /*  so far, so good. now check first four bytes and CRC  */

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "    'checkFill()' mem at %p size = %6d, ", fp->mem, fp->size);

    if (fp->first != *((uint32_t*) (fp->mem)))
    {
        TRACE(
                MPE_LOG_DEBUG,
                MPE_MOD_TEST,
                "\n\n # # # # # ERROR : first four don't match, expected 0x%8x, found 0x%08x\n",
                fp->first, *((uint32_t*) (fp->mem)));
        return (FALSE);
    }
    else
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "first four OK (0x%08x), ",
                fp->first);
    }

    crc = get_crc((unsigned char*) fp->mem, fp->size, crc);

    if (fp->crc != crc)
    {
        TRACE(
                MPE_LOG_DEBUG,
                MPE_MOD_TEST,
                "\n\n # # # # # ERROR : CRC doesn't match, expected 0x%8x, found 0x%08x\n",
                fp->crc, crc);
        return (FALSE);
    }
    else
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "CRC OK (0x%08x)\n", crc);
    }
    return TRUE;
}

/* generate the crc table. Must be called before calculating the crc value */

static unsigned long crc_table[256];
static int m_isTableInit = 0; /* 0 == false, else true */

/**
 * genTable
 * Used to build the crc table.
 */
static void genTable(void)
{
    uint32_t crc, poly;
    int i, j;

    /* Only run this funtion if the crc table is not initialized.
     */
    if (1 == m_isTableInit)
    {
        return;
    }

    m_isTableInit = 1;

    poly = 0xEDB88320L;
    for (i = 0; i < 256; i++)
    {
        crc = i;
        for (j = 8; j > 0; j--)
        {
            if (crc & 1)
                crc = (crc >> 1) ^ poly;
            else
                crc >>= 1;
        }
        crc_table[i] = crc;
    }
} /* end genTable() */

/****************************************************************************
 *
 * get_crc -  generates CCITT CRC-32. Initialize 'crc' to 0xffffffff before
 * calling.
 *
 */
uint32_t get_crc(unsigned char *buf, int length, uint32_t crc)
{
    uint32_t c2;

    c2 = update_crc(buf, length, crc);
    return (c2 ^ 0xFFFFFFFF);
}

/****************************************************************************
 *
 * update_crc -  generates running CCITT CRC-32. Initialize 'crc' to 0xffffffff
 * before first call. Subsequest calls with more data update the CRC value.
 * After the final call, XOR the CRC value with 0xffffffff to get the final
 * CRC value. If you don't need to interoperate with something which expects
 * a valid CCITT CRC-32 value, you can skip the final XOR step.
 *
 */
uint32_t update_crc(unsigned char *buf, int length, uint32_t crc)
{
    int ch;
    int i;

    genTable();

    for (i = 0; i < length; i++)
    {
        ch = buf[i];
        crc = (crc >> 8) ^ crc_table[(crc ^ ch) & 0xFF];
        TRACE(MPE_LOG_TRACE9, MPE_MOD_TEST,
                "   ####  ch == %02x, CRC == 0x%08x\n", ch, crc);
    }

    return (crc);
}

#if 0  /* CRC test code - used to verify CRC code during initial development */
/****************************************************************************

 CRC Test code

 Test CRC values :

 resume = 60c1d0a0
 resumé = 84cf1fab
 123456789asdfg = ed3445b2
 Rumplestilkskin = d2662c40

 ****/

static void testcrc(void)
{
    unsigned long crc;

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "\n\n");

    crc = 0xffffffff;
    crc = get_crc("resume", 6, crc);
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  CRC of 'resume' is 0x%08x\n\n", crc);

    crc = 0xffffffff;
    crc = get_crc("resumé", 6, crc);
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  CRC of 'resumé' is 0x%08x\n\n", crc);

    crc = 0xffffffff;
    crc = update_crc("re", 2, crc);
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  CRC of 're' is 0x%08x, intermediate result is 0x%08x\n\n", crc^0xFFFFFFFF, crc);

    crc = get_crc("sume", 4, crc);
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  CRC of 're'+'sume' is 0x%08x\n\n", crc);

    crc = 0xffffffff;
    crc = get_crc("123456789asdfg", 14, crc);
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  CRC of '123456789asdfg' is 0x%08x\n\n", crc);

    crc = 0xffffffff;
    crc = get_crc("Rumplestilkskin", 15 , crc);
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  CRC of 'Rumplestilkskin' is 0x%08x\n\n", crc);
}
#endif  /* CRC test code */

/****************************************************************************
 *
 *   getTestSuite_sysMem - Create and return the memory test suite
 *
 ***************************************************************************/

CuSuite* getTestSuite_sysMem(void)
{
    CuSuite* suite = CuSuiteNew();

    SUITE_ADD_TEST(suite, vte_test_memAllocP);
    SUITE_ADD_TEST(suite, vte_test_memFreeP);
    SUITE_ADD_TEST(suite, vte_test_memReallocP);
    SUITE_ADD_TEST(suite, vte_test_memAllocH);
    SUITE_ADD_TEST(suite, vte_test_memFreeH);
    SUITE_ADD_TEST(suite, vte_test_memLockH);
    SUITE_ADD_TEST(suite, vte_test_memReallocH);
    SUITE_ADD_TEST(suite, vte_test_memCompact);
    SUITE_ADD_TEST(suite, vte_test_memGetFreeSize);
    SUITE_ADD_TEST(suite, vte_test_memGetLargestFree);
    SUITE_ADD_TEST(suite, vte_test_memGetStats);
    SUITE_ADD_TEST(suite, vte_test_memPurge);
    SUITE_ADD_TEST(suite, vte_test_memStats);
    return suite;
}
