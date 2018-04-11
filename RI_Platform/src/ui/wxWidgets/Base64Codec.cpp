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

/*
 * Base64Encoder.cpp
 *
 *  Created on: Jul 29, 2009
 *      Author: Mark Millard
 */

// Include system header files.
#include <iostream>
#include <sstream>
#include <iomanip>

// Include glib header files for base64
#include <glib.h>

// Include class header files.
#include "Base64Codec.h"

// Declare namespace usage.
using namespace std;

Base64Codec::Base64Codec()
{
    // Do nothing for now.
}

Base64Codec::~Base64Codec()
{
    // Nothing to do.
}

string Base64Codec::Encode(const char *data, int size)
{
#if 0
    base64::encoder b64Encoder;
    stringstream stream_data;

    // Check input arguments.
    if ((data == NULL) || (size == 0))
    return NULL;

    // Coerce data to string stream data.
    (void) stream_data.write(data, size);

    // Encode stream data.
    istringstream istream_in(stream_data.str());
    ostringstream ostream_out;
    b64Encoder.encode(istream_in, ostream_out);

    //return ostream_out.str().c_str();
    return ostream_out.str();
#else
    // Check input arguments.
    if ((data == NULL) || (size == 0))
        return NULL;

    char *tmpBuf = g_base64_encode((unsigned char *) data, size);
    string retStr(tmpBuf);
    g_free(tmpBuf);
    return retStr;
#endif
}

string Base64Codec::Decode(const char *data, int size)
{
#if 0
    base64::decoder b64Decoder;
    stringstream stream_data;

    // Check input arguments.
    if ((data == NULL) || (size == 0))
    return NULL;

    // Coerce data to string stream data.
    (void) stream_data.write(data, size);

    // Decode stream data.
    istringstream istream_in(stream_data.str());
    ostringstream ostream_out;
    b64Decoder.decode(istream_in, ostream_out);

    //return ostream_out.str().c_str();
    return ostream_out.str();
#else
    // Check input arguments.
    if ((data == NULL) || (size == 0))
        return NULL;

    unsigned char *tmpBuf = g_base64_decode(data, (size_t *) &size);
    string retStr((char *) tmpBuf);
    g_free(tmpBuf);
    return retStr;
#endif
}
