#if !defined(_MOCK_SYNC_H)
#define _MOCK_SYNC_H


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
#include <mpe_types.h>	/* Resolve basic type references. */
#include <mpeos_thread.h>
#include <os_sync.h>	/* Resolve target specific synchronization definitions. */

#ifdef __cplusplus
extern "C"
{
#endif

/***
 * Mutex and Condition type definitions:
 */
typedef os_Mutex mock_Mutex; /* Mutex type binding. */
typedef os_Condition mock_Cond; /* Condition type binding. */

/***
 * Mutex API prototypes:
 */

/**
 * The <i>mock_mutexNew()</i> function will create a new mutex.
 *
 * @param mutex is a pointer for returning the identifier of the new mutex.
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/> 
 *          is returned.
 */
mpe_Error mock_mutexNew(mock_Mutex *mutex);

/**
 * The <i>mock_mutexDelete()</i> function will destroy a previously created mutex.
 *
 * @param mutex is the identifier of the mutex to destroy.
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/> 
 *          is returned.
 */
mpe_Error mock_mutexDelete(mock_Mutex mutex);

/**
 * The <i>mock_mutexAqcuire()</i> function will acquire ownership of the target 
 * mutex.  If the mutex is already owned by another thread, the calling thread will 
 * be suspended within a priority based queue until the mutex is free for this 
 * thread's acquisition.
 *
 * @param mutex is the identifier of the mutex to acquire.
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/> 
 *          is returned.
 */
mpe_Error mock_mutexAcquire(mock_Mutex mutex);

/**
 * The <i>mock_mutexAcquireTry()</i> function will attempt to acquire ownership of
 * the target mutex without blocking.  If the mutex is busy an error will be returned
 * to indicate failure to acquire the mutex.
 *
 * @param mutex is the identifier of the mutex to acquire.
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/> 
 *          is returned.
 */
mpe_Error mock_mutexAcquireTry(mock_Mutex mutex);

/**
 * The <i>mock_mutexRelease()</i> function will release ownership of a mutex.  The
 * current thread must own the mutex in order to release it.
 *
 * @param mutex is the identifier of the mutex to release.
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/> 
 *          is returned.
 */
mpe_Error mock_mutexRelease(mock_Mutex mutex);

/***
 * Condition API prototypes:
 */

/**
 * The <i>mock_condNew()</i> function will create a new condition synchronization
 * object.
 *
 * @param autoReset is a boolean variable that indicated whether the new 
 *          condition is an "autoreset" condition object, in which case the 
 *          condition object automatically resets to the unset (FALSE) state 
 *          when a thread acquires it using the <i>mpe_condGet()<i/> function. 
 *          If autoReset is TRUE, the condition object is automatically reset to 
 *          the unset (FALSE) condition.
 * @param initialState is a boolean variable that specifies the initial state of 
 *          the condition object. The condition object is in the set state if 
 *          <i>initialState<i/> is TRUE and in the unset state if <i>initialState<i/> 
 *          is FALSE.
 * @param cond is a pointer for returning the new condition identifier used for
 *          subsequent condition variable operations.
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/> 
 *          is returned.
 */
mpe_Error mock_condNew(mpe_Bool autoReset, mpe_Bool initialState,
        mock_Cond *cond);

/**
 * The <i>mock_condDelete()</i> function will destroy a condition object.
 *
 * @param cond is the identifier of the target condition to destroy.
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/> 
 *          is returned.
 */
mpe_Error mock_condDelete(mock_Cond cond);

/**
 * The <i>mock_condGet()</i> function will get exclusive access of the specified
 * condition object.  If the condition object is in the FALSE state at the time of
 * the call the calling thread is suspended until the condition is set to the
 * TRUE state.  If the calling thread already has exclusive access to the
 * condition this function does nothing.
 *
 * @param cond is the identifier of the target condition object to acquire.
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/> 
 *          is returned.
 */
mpe_Error mock_condGet(mock_Cond cond);

/**
 * The <i>mock_condWaitFor()</i> function will attempt to get exclusive access
 * of the specified condition object.  If the condition object is in the FALSE state 
 * at the time of the call the calling thread is suspended for a maximum period of
 * time as specified by the time out parameter until the condition is set to the
 * TRUE state.  If the calling thread already has exclusive access to the
 * condition this function does nothing.
 *
 * @param cond is the identifier of the target condition object to acquire.
 * @param timeout is the maximum time in milliseconds to wait for condition object 
 *          to become TRUE.
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/> 
 *          is returned.
 */
mpe_Error mock_condWaitFor(mock_Cond cond, uint32_t timeout);

/**
 * The <i>mock_condSet()</i> function will set the condition variable to TRUE 
 * state and activate the first thread waiting.
 *
 * @param cond is the identifier of the target condition object to set.
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/> 
 *          is returned.
 */
mpe_Error mock_condSet(mock_Cond cond);

/**
 * The <i>mock_condUnset()</i> function will set the condition variable to the
 * FALSE state without modifying ownership of the condition variable???
 *
 * @param cond is the identifier of the target condition object to unset.
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/> 
 *          is returned.
 */
mpe_Error mock_condUnset(mock_Cond cond);

mpe_Error mock_threadGetCurrent(mpe_ThreadId *threadId);


#ifdef __cplusplus
}
#endif
#endif /* _MOCK_SYNC_H */

