#if !defined(_MPEOS_UTIL_H)
#define _MPEOS_UTIL_H
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

#include <mpe_types.h>      /* Resolve basic type references. */
#include <os_util.h>        /* Resolve target specific utility definitions. */
#include <mpeos_event.h>

#ifdef __cplusplus
extern "C"
{
#endif

//#include <mpe_ed.h>

/***
 * Type definitions:
 */

/* setjmp/longjmp implementation buffer. */
typedef os_JmpBuf mpe_JmpBuf;

typedef enum
{
    MPE_BOOTMODE_RESET = 1, /* Reset STB, equivalent to power-on reset. */
    MPE_BOOTMODE_FAST
/* Attempt immediate 2-way (FDC+RDC) connection. */
} mpe_STBBootMode;

/*
 * MPE/MPEOS/OCAP stack bootstrap status bit definitions, returned from mpeos_stbBootStatus().
 */
#define MPE_BS_MPE_LOWLVL 	0x00000001		/* Low-level initialization done. */
#define MPE_BS_MPE_MGRS   	0x00000002  	/* MPE managers installed. */

#define MPE_BS_NET_NOWAY	0x00000010  	/* Broadcast, FDC, RDC not available. */
#define MPE_BS_NET_1WAY     0x00000020      /* Broadcast mode only, no RDC. */
#define MPE_BS_NET_2WAY     0x00000040      /* Broadcast, FDC, RDC available. */
#define MPE_BS_NET_MASK		0x00000070      /* Mask for network status bits. */

/**
 * The <i>mpeos_atomicOperation()</i> function will invoke the specified
 * function with the specified data such that the function can perform an
 * operation on the data atomically.
 *<p>
 * Warning: operations should be kept to simple operations that do not
 * invoke other APIs in the system due to the fact that the underlying
 * implementation may be such that normal task switching may be halted.
 *
 * @param operation is a pointer to the function to invoke.
 * @param data ia a pointer to pass to the operation function.
 * @return the value returned by the operation function.
 */
void *mpeos_atomicOperation(void *(*operation)(void*), void *data);

/**
 * The <i>mpeos_atomicIncrement()</i> function will atomically increment
 * a word in memory.
 *
 * @param value is a pointer to the variable to increment.
 * @return the new value.
 */
uint32_t mpeos_atomicIncrement(uint32_t *value);

/**
 * The <i>mpeos_atomicDecrement()</i> function will atomically decrement
 * a word in memory.
 *
 * @param value is a pointer to the variable to decrement.
 * @return the new value.
 */
uint32_t mpeos_atomicDecrement(uint32_t *value);

/***
 * Set jump/long jump support API prototypes:
 */

/**
 * The <i>mpeos_setJmp()</i> function will save the current stack context for
 * subsequent non-local dispatch (jump).  The return value indicates whether the
 * return is from the original direct call to save the current stack or from an
 * <i>mpeos_longJmp()<i/> operation that restored the saved stack image contents.
 *
 * @param jmpBuf is the "jump" buffer for saving the context information.
 * @return an integer value indicating the return context.  A value of zero indicates
 *          a return from a direct call and a non-zero value indicates an indirect
 *          return (i.e. via <i>mpeos_longJmp()<i/>).
 */
int mpeos_setJmp(mpe_JmpBuf jmpBuf);

/**
 * The <i>mpeos_longJmp()</i> function will perform a non-local dispatch (jump) to
 * saved stack context.
 *
 * @param jmpBuf is a pointer to the "jump" buffer context to restore.
 * @param val is the return value for the associated setjmp operation.
 */
void mpeos_longJmp(mpe_JmpBuf jmpBuf, int val);

/***
 * Environment variable utility API prototypes:
 */

/**
 * The <i>mpeos_envGet()</i> function will get the value of the specified
 * environment variable.
 *
 * @param name is a pointer to the name of the target environment variable.
 *
 * @return a pointer to the associated string value of the target environment
 * variable or NULL if the variable can't be found.
 */
const char* mpeos_envGet(const char *name);

/**
 * The <i>mpeos_envSet()</i> function will set the value of the specified
 * environment variable.
 *
 * @param name is a pointer to the name of the target environment variable.
 * @param value is a pointer to a NULL terminated value string.
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mpeos_envSet(char *name, char *value);

/**
 * The <i>mpeos_envInit()</i> function will allow the OS to initialize any environment
 * functionality prior to env usage
 */
void mpeos_envInit(void);

/* STB related utility API prototypes: */

/**
 * The <I>mpeos_registerForPowerKey()</I> function is used to register an event handle
 * and a queue id for notification of power state changes.
 *
 * @param queueId the ID of the queue to be used for notification events
 * @param act the Event Dispatcher handle
 *
 * @return error code, right now only MPE_SUCCESS
 */
mpe_Error mpeos_registerForPowerKey(mpe_EventQueue queueId, void *act);

/**
 * The <i>mpeos_stbGetPowerStatus()</i> function will get the current STB power
 * status.  The power modes currently defined are: MPE_POWER_FULL & MPE_POWER_STANDBY.
 * MPE_POWER_FULL = 1, MPE_POWER_STANDBY = 2. At boot up the STB should be in stnadby.
 *
 * @return unsigned integer representing the power mode.
 */
mpe_PowerStatus mpeos_stbGetPowerStatus(void);

/**
 * The <i>mpeos_stbGetAudioStatus()</i> function will get the current STB audio
 * status.  The audio modes currently defined are: MPE_AUDIO_ON & MPE_AUDIO_MUTED.
 * MPE_AUDIO_ON = 1, MPE_AUDIO_MUTED = 2.
 *
 * @return unsigned integer representing the audio mode.
 */
mpe_AudioStatus mpeos_stbGetAudioStatus(void);

/**
 * The <i>mpeos_stbBoot()</i> function performs various "bootstrap" related
 * operations.  The following operations are supported:
 * <ul>
 * <li> 1. Reboot the entire STB (equivilent to power-on reset).
 * <li> 2. Fast boot 2-way mode (forward & reverse data channels enabled).
 * </ul>
 */
mpe_Error mpeos_stbBoot(mpe_STBBootMode mode);

/**
 * The <i>mpeos_stbBootStatus()</i> function allows the entire MPE/MPEOS/OCAP stack
 * to query and update the bootstrap status of the stack itself (e.g. MPE managers
 * installed, JVM started, UI events available, 2-way network status, etc).
 *
 * The boot status information is simply a 32-bit value with each bit defined to
 * represent a "stage" of the boot process or the "availability" of a resource
 * (e.g. network status).
 *
 * If the "update" flag parameter is TRUE the call is an update call and the second
 * parameter contains the additional boot status information to be added to the current
 * status.  The third parameter is used to optionally clear any status bit information
 * for any status that may have changed.  If any bits are not to be cleared 0xFFFFFFFF
 * should be passed for the bit mask value.
 *
 * @param update is a boolean indicating a call to update the boot status
 * @param statusBits is a 32-bit value representing the new status update information
 *        the bits are logically ORed with the current boot status.
 * @param bitMask is a bit pattern for optionally clearing any particular status buts.
 *
 * @return a 32-bit bit pattern indicating the status of the various elements
 *         of the system.
 */
uint32_t mpeos_stbBootStatus(mpe_Bool update, uint32_t statusBits,
        uint32_t bitMask);

/**
 * <i>mpeos_stbGetAcOutletState()</i>
 *
 * Get the current AC outlet state.
 *
 * @param state indicates the current state of the external AC outlet.  If the
 * returned value is <code>true</code> the outlet is enabled, if
 * <code>false</code> the outlet is disabled.
 *
 * @return MPE_SUCCESS if the outlet state was successfully retrieved
 * @return MPE_EINVAL if the start parameter is invalid
 */
mpe_Error mpeos_stbGetAcOutletState(mpe_Bool *state);

/**
 * <i>mpeos_stbSetAcOutletState()</i>
 *
 * Set the current AC outlet state.
 *
 * @param enable value indicating the new state of the AC outlet.  If
 * <code>true</code> the outlet is enabled, if <code>false</code> the outlet
 * is disabled.
 *
 * @return MPE_SUCCESS if the AC outlet status was set successfully.
 */
mpe_Error mpeos_stbSetAcOutletState(mpe_Bool enable);

/**
 * <i>mpeos_stbGetRootCerts()</i>
 *
 * Acquire the initial set of root certificates for the platform.
 *
 * @param roots is a pointer for returning a pointer to the memory location
 *        containing the platform root certificate(s).
 * @param len is a pointer for returning the size (in bytes) of the memory location
 *        containing the roots.
 *
 * @return MPE_SUCCESS if the pointer to and length of the root certificate image
 *         was successfully acquired.
 */
mpe_Error mpeos_stbGetRootCerts(uint8_t **roots, uint32_t *len);

/**
 * <i>mpeos_stbSetSystemMuteKeyControl</i>
 *
 * Turns on or off the ability of the user to control muting.  Muting key
 * events are not sent to any applications.
 *
 * DSExt functionality.
 *
 * NOT IMPLEMENTED
 *
 * @param enable, true if muting is enabled.  False if not.
 *
 * @return MPE_SUCCESS
 */
mpe_Error mpeos_stbSetSystemMuteKeyControl(mpe_Bool enable);

/**
 * <i>mpeos_stbSetSystemVolumeKeyControl</i>
 *
 * Turns on or off the ability of the user to control volume.  Volume key
 * events are not sent to any applications.
 *
 * DSExt functionality.
 *
 * NOT IMPLEMENTED
 *
 * @param enable, true if muting is enabled.  False if not.
 *
 * @return MPE_SUCCESS
 */
mpe_Error mpeos_stbSetSystemVolumeKeyControl(mpe_Bool enable);

/**
 * <i>mpeos_stbSetSystemVolumeRange</i>
 *
 * Sets the overall audio range of the audio ports.
 *
 * DSExt functionality.
 *
 * NOT IMPLEMENTED
 *
 * @param range, one of three values:  RANGE_NORMAL, RANGE_NARROW, and RANGE_WIDE.
 *
 * @return MPE_SUCCESS
 */
mpe_Error mpeos_stbSetSystemVolumeRange(uint32_t range);

/**
 * <i>mpeos_stbResetAllDefaults</i>
 *
 * Resets all STB defaults.
 *
 * DSExt functionality.
 *
 * NOT IMPLEMENTED
 *
 * @return MPE_SUCCESS
 */

mpe_Error mpeos_stbResetAllDefaults(void);

/**
 * <i>mpeos_stbSetPowerStatus</i>
 *
 * Sets the power state to the value passed in.  Does not move the power state through a state machine,
 * just sets the value.
 *
 * Notifies listeners.
 *
 * DSExt functionality.
 *
 * NOT IMPLEMENTED
 *
 * @param newPowerMode, one of the mpe_PowerStatus values.
 *
 * @return MPE_SUCCESS
 */
mpe_Error mpeos_stbSetPowerStatus(mpe_PowerStatus newPowerMode);

/**
 * <i>mpeos_stbSetAudioStatus</i>
 *
 * Sets the audio state to the value passed in.
 *
 * @param newAudioMode, one of the mpe_AudioStatus values.
 *
 * @return MPE_SUCCESS
 */
mpe_Error mpeos_stbSetAudioStatus(mpe_AudioStatus newAudioMode);

#ifdef __cplusplus
}
#endif
#endif /* _MPEOS_UTIL_H */
