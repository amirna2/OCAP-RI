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

#include <test_oc.h>
#include <test_utils.h>
#include <mpetest_media.h>
#include <mpetest_si.h>
#include <mpetest_file.h>

/**
 * Defines for this test
 */
#define MULTIMOUNT_NUM_CAROUSELS     (2)
#define REPEATEDMOUNT_NUM_CAROUSELS (16)

/**
 * Static Constant variables for this test
 */
static const int m_iFrequency = OCTEST_FREQUENCY;
static const int m_iProgram = OCTEST_PROGRAM;
static const int m_iCarouselID = OCTEST_CAROUSELID;
static const int m_iSleepDuration = OCTEST_SLEEPDURATION;
static const int m_iNumIterations = 128;
static const char m_pcReadFileName[] = OCTEST_READFILENAME;
static OCTEST_FileCRC2 m_filesMultiThreadCRCs[] =
{
{ "/Arena/com/microstar/xml/HandlerBase.class", 0x7cb3d0f3, 1264 },
{ "/Arena/com/microstar/xml/SAXDriver.class", 0x49ab215e, 6250 },
{ "/Arena/com/microstar/xml/XmlException.class", 0xc9cb8f44, 550 },
{ "/Arena/com/microstar/xml/XmlHandler.class", 0xdf7d9c67, 865 },
{ "/Arena/com/microstar/xml/XmlParser.class", 0xac586396, 26197 },
{ "/Arena/fi/sfd/StxtXlet.class", 0xabcb8ef0, 3906 },
{ "/Arena/fi/sfd/a/a.class", 0x0873dd49, 141 },
{ "/Arena/fi/sfd/a/b.class", 0xc726dd63, 147 },
{ "/Arena/fi/sfd/a/c.class", 0x2e436103, 592 },
{ "/Arena/fi/sfd/a/d.class", 0xb0a51209, 5269 },
{ "/Arena/fi/sfd/a/e.class", 0x7025de15, 8579 },
{ "/Arena/fi/sfd/a/f.class", 0xfad17538, 786 },
{ "/Arena/fi/sfd/a/g.class", 0xbefda487, 268 },
{ "/Arena/fi/sfd/a/h$a.class", 0x443509b6, 451 },
{ "/Arena/fi/sfd/a/h$b.class", 0x83ce3ae0, 1452 },
{ "/Arena/fi/sfd/a/h$c.class", 0x3905a271, 2741 },
{ "/Arena/fi/sfd/a/h.class", 0xdbc963d8, 7673 },
{ "/Arena/fi/sfd/a/i.class", 0x24289a27, 5989 },
{ "/Arena/fi/sfd/a.class", 0xd576949a, 2161 },
{ "/Arena/fi/sfd/b/a.class", 0xacb93963, 3531 },
{ "/Arena/fi/sfd/b/b.class", 0x76dac373, 1888 },
{ "/Arena/fi/sfd/b/c.class", 0x509005ba, 3272 },
{ "/Arena/fi/sfd/stxt/a/a.class", 0x28959c88, 1261 },
{ "/Arena/fi/sfd/stxt/a/b.class", 0x9d7336a3, 4698 },
{ "/Arena/fi/sfd/stxt/a/c.class", 0xdebf8a34, 206 },
{ "/Arena/fi/sfd/stxt/a/d.class", 0xa3a679a2, 10595 },
{ "/Arena/fi/sfd/stxt/a/e.class", 0x04f15eb8, 2894 },
{ "/Arena/fi/sfd/stxt/a/f.class", 0x7b70c61a, 3039 },
{ "/Arena/fi/sfd/stxt/b/a.class", 0xbb75e158, 5314 },
{ "/Arena/fi/sfd/stxt/b/b.class", 0xfff813a1, 3101 },
{ "/Arena/fi/sfd/stxt/b/c.class", 0x95b7f95d, 141 },
{ "/Arena/fi/sfd/stxt/b/d$1.class", 0x594b777d, 124 },
{ "/Arena/fi/sfd/stxt/b/d$a.class", 0x14503a34, 571 },
{ "/Arena/fi/sfd/stxt/b/d$b.class", 0x07e8174d, 921 },
{ "/Arena/fi/sfd/stxt/b/d$c.class", 0x160a613d, 3060 },
{ "/Arena/fi/sfd/stxt/b/d.class", 0x29a29316, 20722 },
{ "/Arena/fi/sfd/stxt/forms/Form.class", 0xaa8496cd, 3453 },
{ "/Arena/fi/sfd/stxt/forms/RadioButtonController.class", 0xad548656, 1886 },
{ "/Arena/fi/sfd/stxt/forms/StxtButton.class", 0x86d3daa6, 4237 },
{ "/Arena/fi/sfd/stxt/forms/StxtCheckBox.class", 0x2292d14f, 3149 },
{ "/Arena/fi/sfd/stxt/forms/StxtHidden.class", 0x06c37a6b, 1249 },
{ "/Arena/fi/sfd/stxt/forms/StxtInput.class", 0xf6af2985, 408 },
{ "/Arena/fi/sfd/stxt/forms/StxtRadioButton.class", 0x947992af, 2892 },
{ "/Arena/fi/sfd/stxt/forms/StxtTextControl.class", 0xe557ae04, 3679 },
{ "/Arena/fi/sfd/stxt/ui/ElementFactory.class", 0xe0271eb8, 2172 },
{ "/Arena/fi/sfd/stxt/ui/FourWayController.class", 0x599541dc, 2657 },
{ "/Arena/fi/sfd/stxt/ui/Page$a.class", 0x1c3bfef7, 1891 },
{ "/Arena/fi/sfd/stxt/ui/Page.class", 0xbbee9a06, 15513 },
{ "/Arena/fi/sfd/stxt/ui/StxtElement.class", 0x4bb30294, 3464 },
{ "/Arena/fi/sfd/stxt/ui/StxtImage.class", 0x40b3e85b, 4333 },
{ "/Arena/fi/sfd/stxt/ui/StxtLine.class", 0xd3de93f1, 5645 },
{ "/Arena/fi/sfd/stxt/ui/StxtLink.class", 0xe5b6afa8, 520 },
{ "/Arena/fi/sfd/stxt/ui/StxtLinkController.class", 0xa9105893, 4162 },
{ "/Arena/fi/sfd/stxt/ui/StxtStyle.class", 0x78244d06, 11858 },
{ "/Arena/fi/sfd/stxt/ui/StxtTable.class", 0x8ecb4a62, 2353 },
{ "/Arena/fi/sfd/stxt/ui/StxtTableRow.class", 0xe92bce2b, 636 },
{ "/Arena/fi/sfd/stxt/ui/StxtTextElement.class", 0x065ebac0, 8721 },
{ "/Arena/fi/sfd/ui/CMBar.class", 0x46e2ad72, 4362 },
{ "/Arena/fi/sfd/ui/CMenu.class", 0x7f679408, 4234 },
{ "/Arena/icons/dvb.icon.0000", 0x0e0e5a11, 1057 },
{ "/Arena/org/xml/sax/AttributeList.class", 0xbc844905, 254 },
{ "/Arena/org/xml/sax/Attributes.class", 0x4320a916, 457 },
{ "/Arena/org/xml/sax/ContentHandler.class", 0x57d397d2, 768 },
{ "/Arena/org/xml/sax/DTDHandler.class", 0xaf175378, 339 },
{ "/Arena/org/xml/sax/DocumentHandler.class", 0x88ea495a, 569 },
{ "/Arena/org/xml/sax/EntityResolver.class", 0xaa65a75e, 260 },
{ "/Arena/org/xml/sax/ErrorHandler.class", 0x78048b76, 253 },
{ "/Arena/org/xml/sax/HandlerBase.class", 0x3552f066, 1430 },
{ "/Arena/org/xml/sax/InputSource.class", 0x0b857631, 1087 },
{ "/Arena/org/xml/sax/Locator.class", 0x080a36ba, 206 },
{ "/Arena/org/xml/sax/Parser.class", 0x98e6fcee, 548 },
{ "/Arena/org/xml/sax/SAXException.class", 0xf4ae080b, 644 },
{ "/Arena/org/xml/sax/SAXNotRecognizedException.class", 0x7b183f55, 188 },
{ "/Arena/org/xml/sax/SAXNotSupportedException.class", 0x2457d98c, 187 },
{ "/Arena/org/xml/sax/SAXParseException.class", 0x88fea8dd, 1207 },
{ "/Arena/org/xml/sax/XMLFilter.class", 0x550a1a6b, 211 },
{ "/Arena/org/xml/sax/XMLReader.class", 0xe75a8b81, 1075 },
{ "/Arena/org/xml/sax/helpers/AttributeListImpl.class", 0xdc7d984d, 1600 },
{ "/Arena/org/xml/sax/helpers/AttributesImpl.class", 0x4a5ca3e5, 3218 },
{ "/Arena/org/xml/sax/helpers/DefaultHandler.class", 0x28328638, 1657 },
{ "/Arena/org/xml/sax/helpers/LocatorImpl.class", 0x27b02c3c, 916 },
{ "/Arena/org/xml/sax/helpers/NamespaceSupport$a.class", 0xe2982efe, 2280 },
{ "/Arena/org/xml/sax/helpers/NamespaceSupport.class", 0x8e3e5b9b, 2348 },
{ "/Arena/org/xml/sax/helpers/ParserAdapter$a.class", 0x2844bf04, 1585 },
{ "/Arena/org/xml/sax/helpers/ParserAdapter.class", 0x1e7871b8, 8429 },
{ "/Arena/org/xml/sax/helpers/ParserFactory.class", 0x4493a883, 905 },
{ "/Arena/org/xml/sax/helpers/XMLFilterImpl.class", 0xe44d61d5, 4859 },
{ "/Arena/org/xml/sax/helpers/XMLReaderAdapter$a.class", 0xae61be13, 957 },
{ "/Arena/org/xml/sax/helpers/XMLReaderAdapter.class", 0xdd3aab17, 3127 },
{ "/Arena/org/xml/sax/helpers/XMLReaderFactory.class", 0xde78c2d7, 1666 },
{ "/Arena/pics/alapalkki.jpg", 0xea052054, 4270 },
{ "/Arena/pics/kelta.gif", 0xc9c506f5, 867 },
{ "/Arena/pics/kelta.png", 0x3ccc0ac8, 225 },
{ "/Arena/pics/luuri.png", 0xee3b9b1c, 520 },
{ "/Arena/pics/puna.gif", 0xbc99cdb6, 867 },
{ "/Arena/pics/puna.png", 0x3de44cb1, 225 },
{ "/Arena/pics/sini.gif", 0x9f66ceaf, 867 },
{ "/Arena/pics/sini.png", 0xd8548567, 225 },
{ "/Arena/pics/vihrea.gif", 0xe06ca673, 867 },
{ "/Arena/pics/vihrea.png", 0xe0ceeb7a, 225 },
{ "/Arena/properties/supertext.properties", 0x8f26c60a, 787 },
{ "/Arena/properties/supertext_fi.properties", 0x2dd742a3, 209 },
{ "/Arena/xml_files/access.png", 0xee7a3350, 4658 },
{ "/Arena/xml_files/agora.png", 0x631668fa, 4435 },
{ "/Arena/xml_files/amigo.png", 0xbf0223d2, 3936 },
{ "/Arena/xml_files/annex.png", 0x55cb314e, 5019 },
{ "/Arena/xml_files/arcade.png", 0xa597180d, 4681 },
{ NULL, 0, 0 } }; /* end m_filesMultiThreadCRCs[] */

/**
 * Static Global Values
 */
static OCTEST_FileCRC *m_fileGenCRCs;
static uint32_t m_genCRCFiles = 0;
static char m_rootDir[64];
static mpe_Mutex m_messageMutex;
static mpe_EventQueue m_mainQueue;
static mpe_EventQueue m_loaderQueue;

/**
 * Function prototypes for this test
 */
static mpe_Error genCRCWalkDir(char *path);
static mpe_Error printDirTreeWalkDir(char *path);
static mpe_Error walkDirTreeWalkDir(char *path);
static void printMsg(int thread, char *format, ...);
static void fileFunc(void *arg);

static void vpk_test_walkDirTree(CuTest* tc);
static void vpk_test_repeatedMount(CuTest* tc);
static void vpk_test_multiThreadCRC(CuTest* tc);
static void vpk_test_printDirTree(CuTest* tc);
static void vpk_test_multiMount(CuTest* tc);
static void vpk_test_mountUnmount(CuTest* tc);
static void vpk_test_fileRead(CuTest* tc);
static void vpk_test_genCRC(CuTest* tc);

CuSuite* vpk_suite_oc_all(void);
CuSuite* vpk_suite_oc_fileRead(void);
CuSuite* vpk_suite_oc_genCRC(void);
CuSuite* vpk_suite_oc_mountUnmount(void);
CuSuite* vpk_suite_oc_multiMount(void);
CuSuite* vpk_suite_oc_printDirTree(void);
CuSuite* vpk_suite_oc_multiThreadCRC(void);
CuSuite* vpk_suite_oc_repeatedMount(void);
CuSuite* vpk_suite_oc_walkDirTree(void);

/**
 * printStatus
 * Will print status if not successful.
 * @param status Status returned
 * @param msg Message
 */
static void printStatus(mpe_FileError status, char* msg)
{
    if (status != MPE_SUCCESS)
    {
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "**********************\n");
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                "********************** %s failed (%04x)\n", msg, status);
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "**********************\n");
    }
} /* end printStatus(mpe_FileError,char*) */

/**
 */
static void printMsg(int thread, char *format, ...)
{
    va_list args;
    char buffer[512];
    va_start(args, format);
    vsprintf(buffer, format, args);
    mutexAcquire(m_messageMutex);
#if (qDebug == 1)
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%2d : %s", thread, buffer);
#else
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%2d : %s", thread, buffer);
#endif
    mutexRelease(m_messageMutex);
    va_end(args);
} /* end printMsg(int,char*) */

/**
 */
static void fileFunc(void *arg)
{
    static uint32_t m_iIterations = 140;
    int i;
    OCTEST_ThreadArgs *ta = (OCTEST_ThreadArgs *) arg;
    char name[128];
    uint8_t buffer[OCTEST_BUFSIZE];
    mpe_File file;
    mpe_Error status;
    uint32_t read;
    uint32_t crc;
    int fileNum = ta->threadNum;

    printMsg(ta->threadNum,
            "-------------------------------------------------------------\n");
    printMsg(ta->threadNum, "Starting thread %s\n", ta->threadName);
    printMsg(ta->threadNum,
            "-------------------------------------------------------------\n");

    for (i = 0; i < m_iIterations; i++)
    {
        sprintf(name, "%s/%s", m_rootDir,
                m_filesMultiThreadCRCs[fileNum].filename);
        printMsg(ta->threadNum, "Opening file %d: %s\n", fileNum, name);

        if ((status = fileOpen(name, MPE_FS_OPEN_READ, &file)) != MPE_SUCCESS)
        {
            printMsg(ta->threadNum,
                    "-------------------------------------------------------------\n");
            printMsg(ta->threadNum,
                    "Error %s (%04x) on open file %s (%d) on iteration %d\n",
                    decodeError(status), status, name, fileNum, i);
            printMsg(ta->threadNum,
                    "-------------------------------------------------------------\n");
            ta->iteration = i;
            ta->fileNum = fileNum;
            ta->errorCode = status;
            eventQueueSend(m_mainQueue, OCTEST_THREAD_FAILED, ta, NULL, 0);
            return;
        }

        read = OCTEST_BUFSIZE;
        crc = 0xffffffff;

        while ((status = fileRead(file, &read, buffer)) == MPE_SUCCESS)
        {
            crc = get_crc(buffer, read, crc);
            read = OCTEST_BUFSIZE;
        }

        fileClose(file);

        if (status != MPE_FS_ERROR_EOF)
        {
            printMsg(ta->threadNum,
                    "-------------------------------------------------------------\n");
            printMsg(ta->threadNum,
                    "Read error %s (%04x) on file %s (%d) on iteration %d\n",
                    decodeError(status), status, name, fileNum, i);
            printMsg(ta->threadNum,
                    "-------------------------------------------------------------\n");
            eventQueueSend(m_mainQueue, OCTEST_THREAD_FAILED, ta, NULL, 0);
            ta->iteration = i;
            ta->fileNum = fileNum;
            ta->errorCode = status;
            return;
        }

#ifdef CHECK_CRC
        if (crc != m_filesMultiThreadCRCs[fileNum].crc)
        {
            printMsg(ta->threadNum, "-------------------------------------------------------------\n");
            printMsg(ta->threadNum,
                    "CRC on file %s (%d) didn't match on iteration %d: Expected: %08x Received %08x\n",
                    name,
                    fileNum,
                    i,
                    m_filesMultiThreadCRCs[fileNum].crc,
                    crc);
            printMsg(ta->threadNum, "-------------------------------------------------------------\n");
            ta->iteration = i;
            ta->fileNum = fileNum;
            ta->errorCode = status;
            eventQueueSend(m_mainQueue, OCTEST_THREAD_FAILED, ta, NULL, 0);
            return;
        }
#endif

        fileNum++;
        if (m_filesMultiThreadCRCs[fileNum].filename == NULL)
            fileNum = 0;
    }

    printMsg(ta->threadNum,
            "-------------------------------------------------------------\n");
    printMsg(ta->threadNum, "Thread completed successfully\n");
    printMsg(ta->threadNum,
            "-------------------------------------------------------------\n");
    eventQueueSend(m_mainQueue, OCTEST_THREAD_SUCCEEDED, ta, NULL, 0);
} /* end fileFunc(void*) */

/**
 * genCRCWalkDir
 * @param path The start path to walk the carousel at.
 */
static mpe_Error genCRCWalkDir(char *path)
{
    static uint32_t m_dirs = 0;

    char subPath[OCTEST_PATHLEN];
    static char buffer[OCTEST_BUFSIZE];
    mpe_Error status;
    mpe_File file;
    uint32_t read;
    uint32_t crc;
    mpe_FileInfo fInfo;
    mpe_DirEntry dirEntry;
    uint32_t size;

    status = fileGetStat(path, MPE_FS_STAT_TYPE, &fInfo);
    if (status != MPE_SUCCESS)
    {
        return status;
    }

    // Sleep for quarter of a second, just to give up the CPU and eliminate the annoying messages
    threadSleep(250, 0);

    if (fInfo.type == MPE_FS_TYPE_DIR)
    {
        if ((status = dirOpen(path, &file)) != MPE_SUCCESS)
        {
            return status;
        }
        m_dirs++;
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", path);
        while (dirRead(file, &dirEntry) == MPE_SUCCESS)
        {
            sprintf(subPath, "%s/%s", path, dirEntry.name);
            if ((status = genCRCWalkDir(subPath)) != MPE_SUCCESS)
            {
                return status;
            }
        }
        dirClose(file);
    }
    else
    {
        if ((status = fileOpen(path, MPE_FS_OPEN_READ, &file)) != MPE_SUCCESS)
        {
            return status;
        }
        read = OCTEST_BUFSIZE;
        crc = 0xffffffff;
        size = 0;
        while ((status = fileRead(file, &read, buffer)) == MPE_SUCCESS)
        {
            crc = get_crc(buffer, read, crc);
            size += read;
            read = OCTEST_BUFSIZE;
        }
        fileClose(file);

        strncpy(m_fileGenCRCs[m_genCRCFiles].name, path, OCTEST_PATHLEN);
        m_fileGenCRCs[m_genCRCFiles].crc = crc;
        m_fileGenCRCs[m_genCRCFiles].size = size;
        m_genCRCFiles++;

        TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                "-------------------------------------------------------------\n");
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                "-------------------------------------------------------------\n");
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%-40s   %08x %d\n", path, crc, size);
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                "-------------------------------------------------------------\n");
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                "-------------------------------------------------------------\n");
    }
    return MPE_SUCCESS;
} /* end genCRCWalkDir(char*) */

/**
 * printDirTreeWalkDir
 * @param path
 * @returns error
 */
static mpe_Error printDirTreeWalkDir(char *path)
{
    static uint32_t m_printDirDirs = 0;
    static uint32_t m_printDirFiles = 0;

    char subPath[OCTEST_PATHLEN];
    mpe_Error status;
    mpe_File file;
    mpe_FileInfo fInfo;
    mpe_DirEntry dirEntry;

    status = fileGetStat(path, MPE_FS_STAT_TYPE, &fInfo);
    printStatus(status, "printDirTreeWalkDir");

    printf("%s %d\n", path, fInfo.type);

    if (fInfo.type == MPE_FS_TYPE_DIR)
    {
        if ((status = dirOpen(path, &file)) != MPE_SUCCESS)
        {
            return status;
        }
        m_printDirDirs++;
        while (dirRead(file, &dirEntry) == MPE_SUCCESS)
        {
            sprintf(subPath, "%s/%s", path, dirEntry.name);
            if ((status = printDirTreeWalkDir(subPath)) != MPE_SUCCESS)
            {
                return status;
            }
        }
        dirClose(file);
    }
    else
    {
        m_printDirFiles++;
    }
    return MPE_SUCCESS;
} /* end printDirTreeWalkDir(char*) */

/**
 * walkDirTreeWalkDir
 * @param path
 * @returns mpe_Error
 */
static mpe_Error walkDirTreeWalkDir(char *path)
{
    static uint32_t m_files = 0;
    static uint32_t m_dirs = 0;

    char subPath[OCTEST_PATHLEN];
    static char buffer[OCTEST_BUFSIZE];
    mpe_Error status;
    mpe_File file;
    uint32_t read;
    mpe_FileInfo fInfo;
    mpe_DirEntry dirEntry;

    status = fileGetStat(path, MPE_FS_STAT_TYPE, &fInfo);
    if (status != MPE_SUCCESS)
    {
        return status;
    }

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "***************************************************************\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "**** Walking %s\n", path);
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "***************************************************************\n");

    if (fInfo.type == MPE_FS_TYPE_DIR)
    {
        if ((status = dirOpen(path, &file)) != MPE_SUCCESS)
        {
            return status;
        }
        m_dirs++;
        while (dirRead(file, &dirEntry) == MPE_SUCCESS)
        {
            sprintf(subPath, "%s/%s", path, dirEntry.name);
            if ((status = walkDirTreeWalkDir(subPath)) != MPE_SUCCESS)
            {
                return status;
            }
        }
        dirClose(file);
    }
    else
    {
        m_files++;
        if ((status = fileOpen(path, MPE_FS_OPEN_READ, &file)) != MPE_SUCCESS)
        {
            return status;
        }
        read = OCTEST_BUFSIZE;
        while ((status = fileRead(file, &read, buffer)) == MPE_SUCCESS)
        {
            TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                    "---------------------------- %d\n", read);
            vte_dump(buffer, read);
            read = OCTEST_BUFSIZE;
        }
        fileClose(file);
    }
    return MPE_SUCCESS;
} /* end walkDirTreeWalkDir(char*) */

/**
 * vpk_test_fileRead
 * Test case for file reading of the OC.
 * @param tc Test Case used by CuTest.
 */
static void vpk_test_fileRead(CuTest* tc)
{
    mpe_FileError rc;
    mpe_File file;
    uint8_t buffer[OCTEST_BUFSIZE];
    uint32_t count;
    mpe_Error status;
    mpe_DirUrl dirUrl;
    mpe_EventQueue queue;
    //mpe_MediaAsyncTunerStatus   tunerStatus;
    mpe_Event event;
    mpe_DirInfo dInfo;
    char filename[512];
    void *foo;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------------\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Running\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------------\n");

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------------\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Tuning\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------------\n");

    // Tune to the channel
    status = eventQueueNew(&queue, "TestOcFileRead");
    CuAssertIntEquals_Msg(tc, "Unable to create queue", MPE_SUCCESS, status);

    //media_tuner_selectServiceUsingTuningParams(0, m_iFrequency, m_iProgram,
    //                                           MPE_SI_MODULATION_QAM64, queue, &tunerStatus);

    eventQueueWaitNext(queue, &event, &foo, NULL, NULL, 60000);

    /* Should return MPE_TUNE_SYNC
     */
    CuAssertIntEquals(tc, MPE_TUNE_SYNC, event);

    dirUrl.url = "ocap://yomama";
    dirUrl.carouselId = m_iCarouselID;

    CuAssertIntEquals_Msg(tc, "Get si handle", MPE_SUCCESS,
            getServiceHandleByFrequencyProgramNumber(m_iFrequency, m_iProgram,
                    &dirUrl.siHandle));

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------------\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Mounting\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------------\n");

    rc = dirMount(&dirUrl);
    CuAssertIntEquals_Msg(tc, "Mount", MPE_SUCCESS, rc);

    rc = dirGetUStat(&dirUrl, MPE_FS_STAT_MOUNTPATH, &dInfo);
    CuAssertIntEquals_Msg(tc, "GetUStat", MPE_SUCCESS, rc);
    sprintf(filename, "%s/%s", dInfo.path, m_pcReadFileName);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------------\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Opening %s\n", filename);
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------------\n");

    rc = fileOpen(filename, MPE_FS_OPEN_READ, &file);
    CuAssertIntEquals_Msg(tc, "File Open", MPE_SUCCESS, rc);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------------\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Reading %s\n", filename);
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------------\n");

    while (rc == MPE_SUCCESS)
    {
        count = OCTEST_BUFSIZE;
        rc = fileRead(file, &count, buffer);
        CuAssertIntEquals_Msg(tc, "File read", MPE_SUCCESS, rc);
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Read %d bytes\n", count);
        /* vte_dump(buffer, count); */
    }

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------------\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Closing %s\n", filename);
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------------\n");

    rc = fileClose(file);
    CuAssertIntEquals_Msg(tc, "File close", MPE_SUCCESS, rc);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------------\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Unmounting\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------------\n");

    rc = dirUnmount(&dirUrl);
    CuAssertIntEquals_Msg(tc, "Unmount", MPE_SUCCESS, rc);

} /* end vpk_test_fileRead(CuTest*) */

/**
 * vpk_test_genCRC
 * @param tc Test Case used by CuTest.
 */
static void vpk_test_genCRC(CuTest* tc)
{
    mpe_FileError rc;
    int i, j;
    mpe_Error status;
    mpe_DirUrl dirUrl;
    mpe_EventQueue queue;
    //mpe_MediaAsyncTunerStatus   tunerStatus;
    mpe_Event event;
    mpe_DirInfo dInfo;
    void *foo;
    char rootDir[64];

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------------\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Running\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------------\n");

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------------\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Tuning\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------------\n");

    // Tune to the channel
    status = eventQueueNew(&queue, "TestOcGenCRC");
    if (status != MPE_SUCCESS)
    {
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Unable to create queue\n");
        return;
    }

    if (mpe_memAllocP(MPE_MEM_TEST, sizeof(OCTEST_FileCRC) * OCTEST_MAX_FILES,
            (void **) &m_fileGenCRCs) != MPE_SUCCESS)
    {
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Unable to allocate memory\n");
        return;
    }

    //media_tuner_selectServiceUsingTuningParams(0, m_iFrequency, m_iProgram,
    //                                           MPE_SI_MODULATION_QAM64, queue, &tunerStatus);

    eventQueueWaitNext(queue, &event, &foo, NULL, NULL, 60000);

    dirUrl.url = "ocap://yomama";
    dirUrl.carouselId = m_iCarouselID;

    CuAssertIntEquals_Msg(tc, "Get Handle", MPE_SUCCESS,
            getServiceHandleByFrequencyProgramNumber(m_iFrequency, m_iProgram,
                    &dirUrl.siHandle));

    for (j = 0; j < m_iNumIterations; j++)
    {
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                "-------------------------------------------------------------\n");
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Mounting %d\n", j);
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                "-------------------------------------------------------------\n");

        rc = dirMount(&dirUrl);
        printStatus(rc, "Mount");

        TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                "-------------------------------------------------------------\n");
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Stating directory\n");
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                "-------------------------------------------------------------\n");

        rc = dirGetUStat(&dirUrl, MPE_FS_STAT_MOUNTPATH, &dInfo);
        printStatus(rc, "Directory stat");

        TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Mountpoint: %s\n", dInfo.path);
        strcpy(rootDir, dInfo.path);

        strcat(rootDir, "/");
        rc = genCRCWalkDir(rootDir);
        printStatus(rc, "Walking directory");

        for (i = 0; i < m_genCRCFiles; i++)
        {
            TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%-40s   %08x %d\n",
                    m_fileGenCRCs[i].name, m_fileGenCRCs[i].crc,
                    m_fileGenCRCs[i].size);
        } /* end for */

        TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                "-------------------------------------------------------------\n");
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Unmounting\n");
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                "-------------------------------------------------------------\n");

        printStatus(dirUnmount(&dirUrl), "Unmount");

        // If the genCRCWalkDir failed, quit
        if (rc != MPE_SUCCESS)
        {
            break;
        }
    } /* end for */

    mpe_memFreeP(MPE_MEM_TEST, m_fileGenCRCs);
} /* end vpk_test_genCRC(CuTest*) */

/**
 * vpk_test_mountUnmount
 * @param tc Test Case used by CuTest.
 */
static void vpk_test_mountUnmount(CuTest* tc)
{
    mpe_FileError rc;
    mpe_Error status;
    mpe_DirUrl dirUrl;
    mpe_EventQueue queue;
    //mpe_MediaAsyncTunerStatus    tunerStatus;
    mpe_Event event;
#ifdef NDEF
    mpe_File file;
    uint32_t count;
    mpe_FileInfo fInfo;
#endif /* #ifdef NDEF */
    void *foo;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------------\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Running\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------------\n");

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------------\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Tuning\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------------\n");

    // Tune to the channel
    status = eventQueueNew(&queue, "TestOcMountUMount");
    CuAssertIntEquals_Msg(tc, "Unable to create queue", MPE_SUCCESS, status);

    //media_tuner_selectServiceUsingTuningParams(0, m_iFrequency, m_iProgram,
    //                                           MPE_SI_MODULATION_QAM64, queue, &tunerStatus);

    eventQueueWaitNext(queue, &event, &foo, NULL, NULL, 60000);
    CuAssertIntEquals_Msg(tc, "Tune", MPE_TUNE_SYNC, event);

    dirUrl.url = "ocap://yomama";
    dirUrl.carouselId = m_iCarouselID;

    CuAssertIntEquals_Msg(tc, "Get Handle", MPE_SUCCESS,
            getServiceHandleByFrequencyProgramNumber(m_iFrequency, m_iProgram,
                    &dirUrl.siHandle));

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------------\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Mounting\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------------\n");
    rc = dirMount(&dirUrl);
    CuAssertIntEquals_Msg(tc, "Mount", MPE_SUCCESS, rc);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------------\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Sleeping\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------------\n");
    threadSleep(m_iSleepDuration, 0);

#ifdef NDEF
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "-------------------------------------------------------------\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Opening /oc/1/Arena/com/microstar/xml/HandlerBase.class\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "-------------------------------------------------------------\n");
    rc = fileOpen("/oc/1/Arena/com/microstar/xml/HandlerBase.class", MPE_FS_OPEN_READ, &file);
    CuAssertIntEquals_Msg( tc, "File Open", MPE_SUCCESS, rc );

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "-------------------------------------------------------------\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Opening /oc/1/Arena/com/microstar/xml/HandlerBase.class\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "-------------------------------------------------------------\n");

    rc = fileGetFStat(file, MPE_FS_STAT_SIZE, &fInfo);
    CuAssertIntEquals_Msg( tc, "Stat file", MPE_SUCCESS, rc );
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Filesize: %ld\n", fInfo.size);

    rc = fileGetFStat(file, MPE_FS_STAT_TYPE, &fInfo);
    CuAssertIntEquals_Msg( tc, "Stat file", MPE_SUCCESS, rc );
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Filetype: %d\n", fInfo.type);

    rc = fileGetFStat(file, MPE_FS_STAT_ISLOADED, &fInfo);
    CuAssertIntEquals_Msg( tc, "Stat isloaded", MPE_SUCCESS, rc );
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Is Loaded: %d\n", fInfo.isLoaded);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "-------------------------------------------------------------\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Reading SandT.png\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "-------------------------------------------------------------\n");

    count = 512;
    rc = fileRead(file, &count, buffer);
    CuAssertIntEquals_Msg( tc, "File read", MPE_SUCCESS, rc );
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Read %d bytes\n", count);
    vte_dump(buffer, count);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "-------------------------------------------------------------\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Closing SandT.png\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "-------------------------------------------------------------\n");
    rc = fileClose(file);
    CuAssertIntEquals_Msg( tc, "File close", MPE_SUCCESS, rc );
#endif

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------------\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Unmounting\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------------\n");

    rc = dirUnmount(&dirUrl);
    CuAssertIntEquals_Msg(tc, "Unmount", MPE_SUCCESS, rc);

} /* end vpk_test_mountUnmount(CuTest*) */

/**
 * vpk_test_multiMount
 * Will test multi mount.
 */
static void vpk_test_multiMount(CuTest* tc)
{
    static const uint32_t m_carouselIDs[] =
    { 1, 2, 3, 1, 2, 3, 1, 2, 0 };
    static const uint8_t m_strings[MULTIMOUNT_NUM_CAROUSELS][32];

    uint32_t iNumIterations = 1;
    uint32_t iNumTunes = 1;
    mpe_FileError rc;
    int i, j, k;
    mpe_Error status;
    mpe_DirUrl dirUrl[MULTIMOUNT_NUM_CAROUSELS];
    mpe_EventQueue queue;
    //mpe_MediaAsyncTunerStatus tunerStatus;
    mpe_Event event;
    void *foo;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------------\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Running\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------------\n");

    for (i = 0; i < MULTIMOUNT_NUM_CAROUSELS; i++)
    {
        dirUrl[i].url = (char *) m_strings[i];
        sprintf((char *) dirUrl[i].url, "ocap://carousel_%d", i);
        dirUrl[i].carouselId = m_carouselIDs[i];
    } /* end for(i) */

    for (k = 0; k < iNumTunes; k++)
    {
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                "-------------------------------------------------------------\n");
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Tuning: Iteration %d\n", k);
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                "-------------------------------------------------------------\n");

        // Tune to the channel
        status = eventQueueNew(&queue, "TestOcMultiMount");
        CuAssertIntEquals_Msg(tc, "Unable to create queue", MPE_SUCCESS, status);

        //media_tuner_selectServiceUsingTuningParams(0, m_iFrequency, m_iProgram,
        //                                           MPE_SI_MODULATION_QAM64, queue, &tunerStatus);

        eventQueueWaitNext(queue, &event, &foo, NULL, NULL, 60000);
        CuAssertIntEquals_Msg(tc, "Tune", MPE_TUNE_SYNC, event);

        for (i = 0; i < MULTIMOUNT_NUM_CAROUSELS; i++)
        {
            printStatus(getServiceHandleByFrequencyProgramNumber(m_iFrequency,
                    m_iProgram, &dirUrl[i].siHandle), "Get Handle");
        } /* end for(i) */

        for (j = 0; j < iNumIterations; j++)
        {
            for (i = 0; i < MULTIMOUNT_NUM_CAROUSELS; i++)
            {
                TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                        "-------------------------------------------------------------\n");
                TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Mounting %d:%d -- %s\n", j,
                        i, dirUrl[i].url);
                TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                        "-------------------------------------------------------------\n");
                rc = dirMount(&dirUrl[i]);
                printStatus(rc, "Mount");
            } /* end for(i) */

            TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                    "-------------------------------------------------------------\n");
            TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Sleeping\n", i);
            TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                    "-------------------------------------------------------------\n");
            threadSleep(m_iSleepDuration, 0);

            for (i = 0; i < MULTIMOUNT_NUM_CAROUSELS; i++)
            {
                TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                        "-------------------------------------------------------------\n");
                TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Unmounting %d:%d -- %s\n",
                        j, i, dirUrl[i].url);
                TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                        "-------------------------------------------------------------\n");
                rc = dirUnmount(&dirUrl[i]);
                printStatus(rc, "Unmount");
            } /* end for(i) */
        } /* end for(j) */

        for (i = 0; i < MULTIMOUNT_NUM_CAROUSELS; i++)
        {
        } /* end for(i) */
    } /* end for(k) */
} /* end vpk_test_multiMount() */

/**
 * vpk_test_multiThreadCRC
 * @param tc Standard test case info for CuTest.
 */
static void vpk_test_multiThreadCRC(CuTest* tc)
{
    mpe_FileError rc;
    int i;
    mpe_DirUrl dirUrl;
    //mpe_MediaAsyncTunerStatus tunerStatus;
    mpe_Event event;
    mpe_DirInfo dInfo;
    void *foo;
    OCTEST_ThreadArgs *ta;
    static OCTEST_ThreadArgs threadInfo[OCTEST_NUM_THREADS + 2];
    static mpe_ThreadId threadId[OCTEST_NUM_THREADS + 2];
    int threadsRunning;

    printMsg(0,
            "-------------------------------------------------------------\n");
    printMsg(0, "Running\n");
    printMsg(0,
            "-------------------------------------------------------------\n");

    /*   gen_table(); */

    eventQueueNew(&m_mainQueue, "TestOcMultiThreadCrcMain");
    eventQueueNew(&m_loaderQueue, "TestOcMultiThreadCrcLoader");

    printMsg(0,
            "-------------------------------------------------------------\n");
    printMsg(0, "Tuning\n");
    printMsg(0,
            "-------------------------------------------------------------\n");

    //media_tuner_selectServiceUsingTuningParams(0, m_iFrequency, m_iProgram,
    //                                           MPE_SI_MODULATION_QAM64, m_loaderQueue, &tunerStatus);

    eventQueueWaitNext(m_loaderQueue, &event, &foo, NULL, NULL, 60000);
    CuAssertIntEquals_Msg(tc, "Event != MPE_TUNE_SYNC", MPE_TUNE_SYNC, event);

    dirUrl.url = "ocap://yomama";
    dirUrl.carouselId = m_iCarouselID;
    rc = getServiceHandleByFrequencyProgramNumber(m_iFrequency, m_iProgram,
            &dirUrl.siHandle);
    CuAssertIntEquals_Msg(tc, "Could not get Handle", MPE_SUCCESS, rc);

    printMsg(0, "Return Code: %d %08x\n", rc, dirUrl.siHandle);

    printMsg(0,
            "-------------------------------------------------------------\n");
    printMsg(0, "Mounting\n");
    printMsg(0,
            "-------------------------------------------------------------\n");
    rc = dirMount(&dirUrl);
    CuAssertIntEquals(tc, MPE_SUCCESS, rc);

    printMsg(0,
            "-------------------------------------------------------------\n");
    printMsg(0, "Stating directory\n");
    printMsg(0,
            "-------------------------------------------------------------\n");
    rc = dirGetUStat(&dirUrl, MPE_FS_STAT_MOUNTPATH, &dInfo);
    CuAssertIntEquals(tc, MPE_SUCCESS, rc);

    printMsg(0, "Mountpoint: %s\n", dInfo.path);
    strcpy(m_rootDir, dInfo.path);
    printMsg(0, "RootDir: %s\n", m_rootDir);

    for (i = 0; i < OCTEST_NUM_THREADS; i++)
    {
        threadInfo[i].threadNum = i + 1;
        sprintf((char *) threadInfo[i].threadName, "FileThread_%d", i + 1);
        printMsg(0, "Starting thread %s\n", threadInfo[i].threadName);

        threadCreate(fileFunc, &threadInfo[i], MPE_THREAD_PRIOR_DFLT,
                64 * 1024, &threadId[i], (char *) threadInfo[i].threadName);
    } /* end for(i) */

    threadsRunning = OCTEST_NUM_THREADS;
    while (threadsRunning)
    {
        eventQueueWaitNext(m_mainQueue, &event, &foo, NULL, NULL, 0);
        if (event == OCTEST_THREAD_SUCCEEDED || event == OCTEST_THREAD_FAILED)
        {
            ta = (OCTEST_ThreadArgs *) foo;
            printMsg(0,
                    "-------------------------------------------------------------\n");
            printMsg(0, "Thread %d completed %s\n", ta->threadNum, (event
                    == OCTEST_THREAD_SUCCEEDED) ? "successfully" : "failed");
            printMsg(0,
                    "-------------------------------------------------------------\n");
            ta->status = event;
            threadsRunning--;
        }
        else
        {
            printMsg(0,
                    "-------------------------------------------------------------\n");
            printMsg(0, "Unexpected event in main queue: %08x %08x\n", event,
                    foo);
            printMsg(0,
                    "-------------------------------------------------------------\n");
        }
    }

    printMsg(0,
            "-------------------------------------------------------------\n");
    for (i = 0; i < OCTEST_NUM_THREADS; i++)
    {
        printMsg(0, "Thread %s (%d): %s\n", threadInfo[i].threadName,
                threadInfo[i].threadNum, (threadInfo[i].status
                        == OCTEST_THREAD_SUCCEEDED) ? "successfully" : "failed");
        if (threadInfo[i].status != OCTEST_THREAD_SUCCEEDED)
        {
            printMsg(0,
                    "    Error code: %s (%04x) Iteration: %d File: (%d) %s\n",
                    decodeError(threadInfo[i].errorCode),
                    threadInfo[i].errorCode, threadInfo[i].iteration,
                    threadInfo[i].fileNum,
                    m_filesMultiThreadCRCs[threadInfo[i].fileNum].filename);
        }

    }
    printMsg(0,
            "-------------------------------------------------------------\n");

    printMsg(0,
            "-------------------------------------------------------------\n");
    printMsg(0, "Unmounting\n");
    printMsg(0,
            "-------------------------------------------------------------\n");
    rc = dirUnmount(&dirUrl);

} /* end vpk_test_multiThreadCRC(CuTest*) */

/**
 * vpk_test_printDirTree
 * @param tc
 */
static void vpk_test_printDirTree(CuTest* tc)
{
    mpe_FileError rc;
    mpe_Error status;
    mpe_DirUrl dirUrl;
    mpe_EventQueue queue;
    //mpe_MediaAsyncTunerStatus  tunerStatus;
    mpe_Event event;
    mpe_DirInfo dInfo;
    void *foo;
    char rootDir[64];

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------------\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Running\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------------\n");

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------------\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Tuning\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------------\n");
    // Tune to the channel
    status = eventQueueNew(&queue, "TestOcPrintDirTree");
    CuAssertIntEquals_Msg(tc, "Unable to create queue", MPE_SUCCESS, status);

    //  media_tuner_selectServiceUsingTuningParams(0, m_iFrequency, m_iProgram,
    //                                            MPE_SI_MODULATION_QAM64, queue, &tunerStatus);

    eventQueueWaitNext(queue, &event, &foo, NULL, NULL, 60000);
    CuAssertIntEquals_Msg(tc, "Tune", MPE_TUNE_SYNC, event);

    dirUrl.url = "ocap://yomama";
    dirUrl.carouselId = m_iCarouselID;
    CuAssertIntEquals_Msg(tc, "Get Handle", MPE_SUCCESS,
            getServiceHandleByFrequencyProgramNumber(m_iFrequency, m_iProgram,
                    &dirUrl.siHandle));
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------------\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Mounting\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------------\n");
    rc = dirMount(&dirUrl);
    CuAssertIntEquals_Msg(tc, "Mount", MPE_SUCCESS, rc);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------------\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Stating directory\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------------\n");
    rc = dirGetUStat(&dirUrl, MPE_FS_STAT_MOUNTPATH, &dInfo);
    CuAssertIntEquals_Msg(tc, "Directory status", MPE_SUCCESS, rc);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Mountpoint: %s\n", dInfo.path);
    strcpy(rootDir, dInfo.path);
    strcat(rootDir, "/");
    printDirTreeWalkDir(rootDir);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------------\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Unmounting\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------------\n");
    rc = dirUnmount(&dirUrl);
    CuAssertIntEquals_Msg(tc, "Unmount", MPE_SUCCESS, rc);

} /* end vpk_test_printDirTree(CuTest*) */

/**
 * vpk_test_repeatedMount
 * @param tc
 */
static void vpk_test_repeatedMount(CuTest* tc)
{
    static uint32_t m_iNumIterations = 2;
    static uint32_t m_iNumTunes = 1;
    static uint8_t m_strings[REPEATEDMOUNT_NUM_CAROUSELS][32];
    static uint32_t m_carouselIDs[] =
    { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 };

    mpe_FileError rc;
    int i, j, k;
    mpe_Error status;
    mpe_DirUrl dirUrl[REPEATEDMOUNT_NUM_CAROUSELS];
    mpe_EventQueue queue;
    //mpe_MediaAsyncTunerStatus  tunerStatus;
    mpe_Event event;
    void *foo;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------------\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Running\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------------\n");

    for (i = 0; i < REPEATEDMOUNT_NUM_CAROUSELS; i++)
    {
        dirUrl[i].url = (char *) m_strings[i];
        sprintf((char *) dirUrl[i].url, "ocap://carousel_%d", i);
        dirUrl[i].carouselId = m_carouselIDs[i];
    }

    for (k = 0; k < m_iNumTunes; k++)
    {
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                "-------------------------------------------------------------\n");
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Tuning: Iteration %d\n", k);
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                "-------------------------------------------------------------\n");
        // Tune to the channel
        status = eventQueueNew(&queue, "TestOcRepeatedMount");
        if (status != MPE_SUCCESS)
        {
            TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Unable to create queue\n");
            return;
        }
        //media_tuner_selectServiceUsingTuningParams(0, m_iFrequency, m_iProgram, MPE_SI_MODULATION_QAM64, queue, &tunerStatus);

        eventQueueWaitNext(queue, &event, &foo, NULL, NULL, 60000);
        CuAssertIntEquals_Msg(tc, "Tune", MPE_TUNE_SYNC, event);

        for (i = 0; i < REPEATEDMOUNT_NUM_CAROUSELS; i++)
        {
            CuAssertIntEquals_Msg(tc, "Get Handle", MPE_SUCCESS,
                    getServiceHandleByFrequencyProgramNumber(m_iFrequency,
                            m_iProgram, &dirUrl[i].siHandle));
        } /* end for(i) */

        for (j = 0; j < m_iNumIterations; j++)
        {
            for (i = 0; i < REPEATEDMOUNT_NUM_CAROUSELS; i++)
            {
                TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                        "-------------------------------------------------------------\n");
                TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Mounting %d:%d -- %s\n", j,
                        i, dirUrl[i].url);
                TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                        "-------------------------------------------------------------\n");
                rc = dirMount(&dirUrl[i]);
                CuAssertIntEquals_Msg(tc, "Mount", MPE_SUCCESS, rc);
            } /* end for(i) */

            TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                    "-------------------------------------------------------------\n");
            TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Sleeping\n", i);
            TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                    "-------------------------------------------------------------\n");
            threadSleep(m_iSleepDuration, 0);

            for (i = 0; i < REPEATEDMOUNT_NUM_CAROUSELS; i++)
            {
                TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                        "-------------------------------------------------------------\n");
                TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Unmounting %d:%d -- %s\n",
                        j, i, dirUrl[i].url);
                TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                        "-------------------------------------------------------------\n");
                rc = dirUnmount(&dirUrl[i]);
                CuAssertIntEquals_Msg(tc, "Unmount", MPE_SUCCESS, rc);
            } /* end for(i) */
        } /* end for(j) */

        for (i = 0; i < REPEATEDMOUNT_NUM_CAROUSELS; i++)
        {
        } /* end for(i) */
    } /* end for(k) */
} /* end vpk_test_repeatedMount(CuTest*) */

/**
 * vpk_test_walkDirTree
 * @param tc
 */
static void vpk_test_walkDirTree(CuTest* tc)
{
    static const char m_sCarouselURL[] = "ocap://walkdir";

    mpe_FileError rc;
    mpe_File file;
    int i;
    mpe_Error status;
    mpe_DirUrl dirUrl;
    mpe_EventQueue queue;
    //mpe_MediaAsyncTunerStatus tunerStatus;
    mpe_Event event;
    mpe_DirEntry dirEnt;
    mpe_DirInfo dInfo;
    void *foo;
    char rootDir[64];

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------------\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Running\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------------\n");

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------------\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Tuning\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------------\n");

    // Tune to the channel
    status = eventQueueNew(&queue, "TestOcWalkDirTree");
    CuAssertIntEquals_Msg(tc, "Unable to create queue", MPE_SUCCESS, status);

    //media_tuner_selectServiceUsingTuningParams(0, m_iFrequency, m_iProgram, MPE_SI_MODULATION_QAM64, queue, &tunerStatus);

    eventQueueWaitNext(queue, &event, &foo, NULL, NULL, 60000);
    CuAssertIntEquals_Msg(tc, "Tune", MPE_TUNE_SYNC, event);

    dirUrl.url = m_sCarouselURL;
    dirUrl.carouselId = m_iCarouselID;

    CuAssertIntEquals_Msg(tc, "Get Handle", MPE_SUCCESS,
            getServiceHandleByFrequencyProgramNumber(m_iFrequency, m_iProgram,
                    &dirUrl.siHandle));

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------------\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Mounting\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------------\n");
    rc = dirMount(&dirUrl);
    CuAssertIntEquals_Msg(tc, "Mount", MPE_SUCCESS, rc);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------------\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Stating directory\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------------\n");
    rc = dirGetUStat(&dirUrl, MPE_FS_STAT_MOUNTPATH, &dInfo);
    CuAssertIntEquals_Msg(tc, "Directory stat", MPE_SUCCESS, rc);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Mountpoint: %s\n", dInfo.path);
    strcpy(rootDir, dInfo.path);
    strcat(rootDir, "/");

    walkDirTreeWalkDir(rootDir);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------------\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Opening /\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------------\n");
    rc = dirOpen(rootDir, &file);
    CuAssertIntEquals_Msg(tc, "Dir Open", MPE_SUCCESS, rc);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------------\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Reading /\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------------\n");
    i = 0;
    while (dirRead(file, &dirEnt) == MPE_SUCCESS)
    {
        printStatus(rc, "Dir Open");
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Entry %3d: %-30s %-4s %d\n", i,
                dirEnt.name, (dirEnt.isDir) ? "Dir" : "File", dirEnt.fileSize);
        i++;
    } /* end while(dirRead()) */

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------------\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Closing /\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------------\n");
    rc = dirClose(file);
    CuAssertIntEquals_Msg(tc, "Dir close", MPE_SUCCESS, rc);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------------\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Unmounting\n");
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "-------------------------------------------------------------\n");
    rc = dirUnmount(&dirUrl);
    CuAssertIntEquals_Msg(tc, "Unmount", MPE_SUCCESS, rc);

} /* vpk_test_walkDirTree(CuTest*) */

/**
 * vpk_suite_oc_all
 * Returns cutest suite info for all the OC tests.
 * @returns CuSuite pointer containing all the CuTest tests.
 */
CuSuite* vpk_suite_oc_all(void)
{
    CuSuite* suite = CuSuiteNew();
    SUITE_ADD_TEST(suite, vpk_test_fileRead);
    SUITE_ADD_TEST(suite, vpk_test_genCRC);
    SUITE_ADD_TEST(suite, vpk_test_mountUnmount);
    SUITE_ADD_TEST(suite, vpk_test_multiMount);
    SUITE_ADD_TEST(suite, vpk_test_printDirTree);
    SUITE_ADD_TEST(suite, vpk_test_multiThreadCRC);
    SUITE_ADD_TEST(suite, vpk_test_repeatedMount);
    SUITE_ADD_TEST(suite, vpk_test_walkDirTree);
    return suite;
} /* end vpk_suite_oc_all() */

/**
 * vpk_suite_oc_fileRead
 * @returns CuSuite pointer to the file read test for the OC.
 */
CuSuite* vpk_suite_oc_fileRead(void)
{
    CuSuite* suite = CuSuiteNew();
    SUITE_ADD_TEST(suite, vpk_test_fileRead);
    return suite;
} /* end vpk_suite_oc_fileRead() */

/**
 * vpk_suite_oc_genCRC
 * @returns CuSuite pointer to the genCRC test for the OC.
 */
CuSuite* vpk_suite_oc_genCRC(void)
{
    CuSuite* suite = CuSuiteNew();
    SUITE_ADD_TEST(suite, vpk_test_genCRC);
    return suite;
} /* end vpk_suite_oc_genCRC() */

CuSuite* vpk_suite_oc_mountUnmount(void)
{
    CuSuite* suite = CuSuiteNew();
    SUITE_ADD_TEST(suite, vpk_test_mountUnmount);
    return suite;
} /* end vpk_suite_oc_mountUnmount() */

CuSuite* vpk_suite_oc_multiMount(void)
{
    CuSuite* suite = CuSuiteNew();
    SUITE_ADD_TEST(suite, vpk_test_multiMount);
    return suite;
} /* end vpk_suite_oc_mountUnmount() */

CuSuite* vpk_suite_oc_printDirTree(void)
{
    CuSuite* suite = CuSuiteNew();
    SUITE_ADD_TEST(suite, vpk_test_printDirTree);
    return suite;
} /* end vpk_suite_oc_printDirTree(void) */

CuSuite* vpk_suite_oc_multiThreadCRC(void)
{
    CuSuite* suite = CuSuiteNew();
    SUITE_ADD_TEST(suite, vpk_test_multiThreadCRC);
    return suite;
} /* end vpk_suite_oc_multiThreadCRC(void) */

CuSuite* vpk_suite_oc_repeatedMount(void)
{
    CuSuite* suite = CuSuiteNew();
    SUITE_ADD_TEST(suite, vpk_test_repeatedMount);
    return suite;
} /* end vpk_suite_oc_repeatedMount(void) */

CuSuite* vpk_suite_oc_walkDirTree(void)
{
    CuSuite* suite = CuSuiteNew();
    SUITE_ADD_TEST(suite, vpk_test_walkDirTree);
    return suite;
} /* end vpk_suite_oc_walkDirTree(void) */
