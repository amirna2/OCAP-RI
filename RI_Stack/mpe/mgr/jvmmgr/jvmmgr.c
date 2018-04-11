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

#include <stdlib.h>

#include <mpe_sys.h>
#include <mpe_types.h>
#include <mpe_error.h>
#include <mpeos_dbg.h>
#include <mpeos_dll.h>
#include <mpeos_util.h>
#include <mpeos_mem.h>
#include <jvmmgr.h>

/******************************************
 * JVM Manager representation
 *****************************************/
typedef struct jvmMgr_t
{
    JNIEnv *m_JNIEnv;
    JavaVM *m_JVM;
    char *m_JAppClassString;
    jclass m_appClass; // store the associated application class
    jobject m_appClassRef; // global reference to the application class (needed?)
} jvmMgr_t;

/* jvm manager global instance */
jvmMgr_t g_JvmMgr =
{ NULL, NULL, NULL, NULL, NULL };

static void jvmGetOptions(JavaVMOption options[], int *numOpts);
static void jvmGetMainArgs(char **mainArgs, int32_t *numArgs);

static void jvmInit(void);

/* jvm managers function table */
static mpe_jvm_ftable_t jvm_ftable =
{ jvmInit, mpe_jvmCreateVM, mpe_jvmExecuteMain };

/**
 * <i>mpe_jvmSetup</i> installs the jvm managers function table in the system
 * managers function table
 */
void mpe_jvmSetup(void)
{
    mpe_sys_install_ftable(&jvm_ftable, MPE_MGR_TYPE_JVM);
}

/**
 * <i>jvmInit</i> initializes the global jvm manager instance
 */
static void jvmInit(void)
{
    /* nothing to initialize for this JVM manager */
}

/**
 * <i>mpe_jvmCreateVM</i> uses the JNI library to instantiate and store
 * a JavaVM and thread local JNI environment
 */
uint32_t mpe_jvmCreateVM(void)
{
    const char* VMPath; /* Path to VM module. */
    JNIEnv *env = NULL; /* JNI environment pointer. */
    void *sunEnv = NULL; /* SunJVM API requires Env pointer to be void* */
    JavaVM *jvm = NULL; /* JVM pointer. */
    mpe_Dlmod jvm_dll; /* JVM dll module ID. */
    mpe_Error err;
    jint res = -1;
#define MAX_OPTS  32
#define MAX_ARGS 64
    int numOpts = MAX_OPTS;
    JavaVMOption *options = NULL; /* Options/parameters for the VM instantiation */
    JavaVMInitArgs vm_args;
    /*lint -e(578)*/jint (JNICALL *JNI_CreateJavaVM)(JavaVM **, void **, void *); /* Create VM entry pointer. */
    const char *optCountStr;

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_JVM,
            "@@@@@@@JvmManager, CreateJavaVM @JNI_VERSION_1_2.\n");

    /*
     * Read in the VM options from the environment
     */
    optCountStr = mpeos_envGet("VMOPT.COUNT");
    if (NULL != optCountStr)
    {
        numOpts = atoi(optCountStr);
    }

    if (MPE_SUCCESS != mpeos_memAllocP(MPE_MEM_GENERAL, sizeof(JavaVMOption)
            * numOpts, (void**) &options))
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_JVM,
                "@@@@@@@JvmManager, CreateJavaVM failed to alloc options mem.\n");
        return false;
    }

    jvmGetOptions(options, &numOpts);

    /* package the vm_args block */
    vm_args.version = JNI_VERSION_1_2;
    vm_args.options = options;
    vm_args.nOptions = numOpts;
    vm_args.ignoreUnrecognized = TRUE;

    /* Get the path the VM module. */
    VMPath = mpeos_envGet("VMDLLPATH");
    if (NULL == VMPath)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_JVM,
                "@@@@@@@JvmManager, VMDllPath env var not found!.\n");
        goto nogo;
    }

    /* Acquire the JVM module. */
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_JVM,
            " Attempting to load VM module: %s\n", VMPath);
    err = mpe_dlmodOpen(VMPath, &jvm_dll);
    if (err != MPE_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_JVM,
                "@@@@@@@JvmManager, mpeos_dlmodOpen>>, failed, error = %d.\n",
                err);
        goto nogo;
    }

    /* Get the pointer to the VM creation entry point. */
    err = mpe_dlmodGetSymbol(jvm_dll, "JNI_CreateJavaVM",
            (void**) &JNI_CreateJavaVM);
    if (err != MPE_SUCCESS)
    {
        MPEOS_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_JVM,
                "@@@@@@@JvmManager, mpeos_dlmodGetSymbol(JNI_CreateJavaVM)>>, failed, error = %d.\n",
                err);
        goto nogo;
    }

    /* Create the Java VM */
    res = (*JNI_CreateJavaVM)(&jvm, &sunEnv, &vm_args);
    env = (JNIEnv*) sunEnv;

    /* If the JVM couldn't be created. */
    if (res < 0)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_JVM,
                "@@@@@@@JvmManager, CreateJavaVM>>, failed.\n");
        goto nogo;
    }

    /* Save a pointer to the Java environment. */
    g_JvmMgr.m_JNIEnv = env;
    g_JvmMgr.m_JVM = jvm;

    /* free allocated memory */
    mpeos_memFreeP(MPE_MEM_GENERAL, options);

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_JVM,
            "@@@@@@@@JvmManager, CreateJavaVM success.\n");

    return true;

    nogo:

    /* Unexpected failure -- cleanup any resources we've grabbed. */
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_JVM,
            "@@@@@@JvmManager, undetected exception occurred.\n");

    /* free allocated memory */
    if (NULL != options)
    {
        mpeos_memFreeP(MPE_MEM_GENERAL, options);
    }

    /* Report nature of failure. */
    if (env != NULL)
    {
        if ((*env)->ExceptionOccurred(env))
        {
            (*env)->ExceptionDescribe(env);
            (*env)->ExceptionClear(env);
        }
    }

    /* Check for need to destroy the VM. */
    if (res > -1)
    {
        if (jvm != NULL)
        {
            (void) (*jvm)->DestroyJavaVM(jvm);
        }
    }
    return false;
}

/**
 * execute static main method
 */
void mpe_jvmExecuteMain(void)
{
    jvmMgr_t *jvmmgr = &g_JvmMgr; /* Java manager pointer. */
    JNIEnv *env = jvmmgr->m_JNIEnv; /* JNI environment pointer. */
    jclass stringClass = NULL; /* java/lang/String class reference. */
    jobjectArray jArgs; /* main class arguments object reference. */
    jmethodID mid; /* main method Id. */
    char localName[256]; /* Temporary class name array. */
    int p = 0; /* Loop index. */
    int32_t iNumArgs; /* The number of arguments contained in mainArgs array. */
    char *mainArgs[MAX_ARGS]; /* An array of null terminated character strings which */
    /* contain arguements to be passed into the main method. */
    /* The first arguement should be the class name. */

    /* Get main arguments. */
    jvmGetMainArgs(mainArgs, &iNumArgs);
    if (iNumArgs == 0)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_JVM,
                "mpe_jvmExecuteMain : No main method found in environment \n");
        return;
    }

    /* Cleanup classname... replace '.' with '/' */
    do
    {
        if (mainArgs[0][p] == '.')
        {
            localName[p] = '/';
        }
        else
        {
            localName[p] = mainArgs[0][p];
        }
    } while (mainArgs[0][p++]);

    /* Find the initial class. The classpath must be setup correctly. */
    jvmmgr->m_appClass = (*env)->FindClass(env, localName);
    if ((jvmmgr->m_appClass == NULL) || ((*env)->ExceptionOccurred(env)))
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_JVM,
                "JvmMgr_ExecuteMain FindClass %s>>, failed.\n", localName);
        if ((*env)->ExceptionOccurred(env))
        {
            (*env)->ExceptionDescribe(env);
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_JVM,
                    "JvmMgr_ExecuteMain FindClass %s exception>>.\n", localName);
            goto nogo;
        }
        goto nogo;
    }
    /* Get a global reference to the class. */
    jvmmgr->m_appClassRef = (*env)->NewGlobalRef(env, jvmmgr->m_appClass);

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_JVM,
            "JvmMgr_ExecuteMain FindClass(%s) success.\n", localName);

    /* Get the method Id of the main method within the class. */
    mid = (*env)->GetStaticMethodID(env, jvmmgr->m_appClass, "main",
            "([Ljava/lang/String;)V");
    if (mid == NULL)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_JVM,
                "JvmMgr_ExecuteMain GetStaticMethodID(main)>>, failed.\n");
        goto nogo;
    }
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_JVM,
            "JvmMgr_ExecuteMain GetStaticMethodID(main) success.\n");

    /* Initialise any java arguments. */
    stringClass = (*env)->FindClass(env, "java/lang/String");
    if (stringClass == NULL)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_JVM,
                "JvmMgr_ExecuteMain FindClass(java/lang/String)>>, failed.\n");
        goto nogo;
    }

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_JVM,
            "JvmMgr_ExecuteMain FindClass(String) success.\n");

    /* Create the args String array for the main method. */
    jArgs = (*env)->NewObjectArray(env, iNumArgs - 1, stringClass, NULL);
    if (jArgs != NULL)
    {
        int i;

        /* Pass args, excluding the class name, to the main function */
        for (i = 1; i < iNumArgs; i++)
        {
            jstring arg = (*env)->NewStringUTF(env, mainArgs[i]);

            if (arg != NULL)
            {
                (*env)->SetObjectArrayElement(env, jArgs, i - 1, arg);

                if ((*env)->ExceptionOccurred(env))
                {
                    jArgs = NULL;
                    break;
                }
            }
            else
            {
                jArgs = NULL;
                break;
            }
        }
    }

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_JVM,
            "JvmMgr_ExecuteMain jArgs setup success success.\n");

    if (jArgs == NULL)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_JVM,
                "JvmMgr_ExecuteMain jArgs == NULL.\n");
        goto nogo;
    }

    /* Now call the main method. */
    (*env)->CallStaticVoidMethod(env, jvmmgr->m_appClass, mid, jArgs);
    if ((*env)->ExceptionOccurred(env))
    {
        (*env)->ExceptionDescribe(env);
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_JVM,
                "JvmMgr_ExecuteMain CallStaticMethodID(main)>>, failed.\n");
        goto nogo;
    }

    return;

    nogo: /* Error condition code: */
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_JVM,
            "JvmMgr_ExecuteMain undetected exception occurred.\n");
}

/**
 * Returns a pointer to the instantiated VM's JNI environment pointer
 * Used by the JVM manager internally
 *
 * @return pointer to a JNIEnv structure
 */
JNIEnv *mpe_jvmGetJNIEnv(void)
{
    return g_JvmMgr.m_JNIEnv;
}

/**
 * Returns a pointer to the instantiated VM's JavaVM structure pointer
 *
 * @return pointer to a JNIEnv structure
 */
JavaVM *mpe_jvmGetJVM(void)
{
    return g_JvmMgr.m_JVM;
}

/**
 * Get the jvm options from the environment
 * @param options pointer to the memory that will contain the options
 * @param numOpts contains the initial number of jvm options.  Upon return
 * contains the actual number of options found.
 *
 * @return
 */
static void jvmGetOptions(JavaVMOption options[], int *numOpts)
{
    int32_t optIndex; /* VMOPT# index. */
    char optName[32]; /* Buffer for VMOPT# string generation. */
    const char* optString; /* Pointer to option environment value. */
    int count = *numOpts;

    *numOpts = 0;
    /* Acquire all VM option parameters. */
    for (optIndex = 0; optIndex < count; optIndex++)
    {
        /* Construct next VM option prefix. */
        sprintf(optName, "VMOPT.%d", (int) optIndex);

        /* Attempt to acquire next option number. */
        optString = mpeos_envGet(optName);
        if (NULL != optString)
        {
            /* Save pointer to VM option and increment count of number of options found. */
            options[*numOpts].optionString = (char*) optString; // promise not to alter this const optString!
            (*numOpts)++;
            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_JVM,
                    "JvmMgr_jvmGetOptions: jvm option %d = %s.\n", optIndex,
                    optString);
        }
    }
}

/**
 * reads the environment for main class and main arguements
 */
static void jvmGetMainArgs(char **mainArgs, int32_t *numArgs)
{
    int32_t argIndex;
    char argName[32];
    const char* argString;

    /* Default to no arguments. */
    *numArgs = 0;

    /*  Read the main class and it's arguements out of the environment */
    for (argIndex = 0; argIndex < MAX_ARGS; argIndex++)
    {
        /* Generate next option prefix. */
        sprintf(argName, "MainClassArgs.%d", (int) argIndex);

        /* Attempt to get next option. */
        argString = mpeos_envGet(argName);
        if (NULL != argString)
        {
            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_JVM, "Found MainClassArg: '%s'\n",
                    argString);

            /* Save pointer to next option and increment option count. */
            mainArgs[*numArgs] = (char*) argString; // promise not to alter the constant argString!
            (*numArgs)++;
        }
    }
}
