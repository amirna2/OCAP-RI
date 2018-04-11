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

#ifndef _RI_CONFIG_H_
#define _RI_CONFIG_H_

#if defined(__cplusplus)
extern "C"
{
#endif

typedef enum _RIConfigError
{
    RICONFIG_SUCCESS = 0,
    RICONFIG_ERROR_FILE_NOT_FOUND,
    RICONFIG_ERROR_ID_IN_USE,
    RICONFIG_ERROR_NOT_AVAILABLE,
    RICONFIG_MEMORY_ALLOCATION,
} RIConfigError;

/**
 * Parses a configuration file and stores the data in an internal data
 * structure.  The configurations read from this file are associated with
 * the given ID string.
 *
 * @param config_id the string that uniquely identifies the configuration
 *        values in this file
 * @param filename the configuration file to be parsed
 * @return RI_CONFIG_SUCCESS if parsing was successfull,
 *         RI_CONFIG_ERROR_ID_IN_USE if the given config_id is already
 *         in use
 *         RI_CONFIG_ERROR_FILE_NOT_FOUND if the configuraton file could
 *         not be opened
 *         RICONFIG_ERROR_NOT_AVAILABLE if the maximum number of
 *         configuration sets has been reached.
 */
RIConfigError
        ricfg_parseConfigFile(const char* config_id, const char* filename);

/**
 * Frees all memory resources associated with the current config list
 *
 * @param config_id the identifier for the configuration set that you
 *        wish to release
 */
void ricfg_freeConfigList(const char* config_id);

/**
 * Return a string config value corresponding to the given config name,
 * for the given configuration set.
 *
 * @param config_id the configuration identifier
 * @param configName the name of the configuration value to retrieve
 * @return the value associated with the given name, NULL if the
 *         config name is not found
 */
char* ricfg_getValue(const char* config_id, const char* configName);

/**
 * Return the boolean config value corresponding to the given config name,
 * for the given configuration set.  Checks config value as a case insensitive
 * string for "TRUE" or "FALSE"
 *
 * @param config_id the configuration identifier
 * @param configName the name of the configuration value to retrieve
 * @return the value associated with the given name, NULL if the
 *         config name is not found
 */
int ricfg_getBoolValue(const char* config_id, const char* configName);

/**
 * Returns the string values of a multi-value configuration variable for
 * the given configuration set.  Multi value configurations are in the
 * form <root_name>.x, where x is an integer starting at 0 and incrementing
 * by 1 for each value.
 *
 * @param config_id the configuration identifier
 * @param configNameRoot the root of the configuration variable name
 * @param configValues a pre-allocated array of string where this function
 *        will return the configuration values.
 * @param numValues a pointer to the length of the configValues array.  Upon
 *        return, this param will point to the number of values actually
 *        parsed.
 */
void ricfg_getMultiValue(const char* config_id, char* configNameRoot,
        char** configValues, int* numValues);

/**
 * Returns the maximum number of values allowed for a multi-value
 * configuration item in the given configuration set.  This value may be
 * set in your configuration file by specifying the property
 * "Config.maxMultiValues".
 *
 * @param config_id the configuration identifier
 * @return the maximum number of values, 0 if the configuration set
 *         was not found
 */
int ricfg_getMaxMultiValues(const char* config_id);

#if defined(__cplusplus)
}
#endif

#endif /* _RI_CONFIG_H_ */
