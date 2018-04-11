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

#include <windows.h>
#include <psapi.h>
#include <stdio.h>

static void usage()
{
    printf("\nUsage:  pk.exe [-k] <process_name>\n\n");
}

// Takes a single string argument which is the process to terminate
int main(int argc, char** argv)
{
    DWORD processIDs[5000];
    DWORD bytesReturned;
    int numProcesses;
    int i;
    LPTSTR appToKill = argv[1];

    if (argc != 2 && argc != 3)
    {
        usage();
        return 1;
    }

    // For backwards compatibility with Process.exe, support the -k option
    if (lstrcmpi(argv[1],"-k") == 0)
    {
        if (argc != 3)
        {
            usage();
            return 1;
        }
        appToKill = argv[2];
    }
    else
    {
        appToKill = argv[1];
    }

    printf("Process killer -- attempting to kill %s\n", appToKill);

    // Enumerate all the currently running processes
    if (EnumProcesses(processIDs, sizeof(processIDs), &bytesReturned) == 0)
    {
        printf("Error enumerating win32 process (%d)!\n", (int)GetLastError());
        return 1;
    }

    // There is 1 process per DWORD bytes
    numProcesses = bytesReturned / sizeof(DWORD);
    
    // Allocate space for the process name
    LPTSTR processName = (LPTSTR)HeapAlloc(GetProcessHeap(), 0, MAX_PATH*sizeof(TCHAR));

    // Go through each running process looking for the one of interest
    for (i = 0; i < numProcesses; i++)
    {
        // Open the process and determine its name
        HANDLE process = OpenProcess(PROCESS_QUERY_INFORMATION | PROCESS_VM_READ | PROCESS_TERMINATE, FALSE, processIDs[i]);
        if (process != NULL)
        {
            if (GetModuleBaseName(process, NULL, processName, MAX_PATH) > 0)
            {
                if (!lstrcmpi(processName, appToKill))
                {
                    if (TerminateProcess(process,0) == 0)
                    {
                        printf("Process killer -- Error terminating process (%d)\n", (int)GetLastError());
                    }
                }
            }
            CloseHandle(process);
        }
    }
    HeapFree(GetProcessHeap(), 0, (LPVOID)processName);

    return 0;
}
