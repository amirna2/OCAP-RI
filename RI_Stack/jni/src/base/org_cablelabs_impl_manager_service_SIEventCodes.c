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

#include "org_cablelabs_impl_manager_service_SIEventCodes.h"

#include "simgr.h"

JNIEXPORT void JNICALL Java_org_cablelabs_impl_manager_service_SIEventCodes_nInitEventCodes
(JNIEnv *env, jclass obj)
{
    jfieldID fID;

    fID = (*env)->GetStaticFieldID(env, obj, "SI_EVENT_UNKNOWN", "I");
    (*env)->SetStaticIntField(env, obj, fID, MPE_SI_EVENT_UNKNOWN);

    fID = (*env)->GetStaticFieldID(env, obj, "SI_EVENT_OOB_VCT_ACQUIRED", "I");
    (*env)->SetStaticIntField(env, obj, fID, MPE_SI_EVENT_OOB_VCT_ACQUIRED);

    fID = (*env)->GetStaticFieldID(env, obj, "SI_EVENT_OOB_NIT_ACQUIRED", "I");
    (*env)->SetStaticIntField(env, obj, fID, MPE_SI_EVENT_OOB_NIT_ACQUIRED);

    fID = (*env)->GetStaticFieldID(env, obj, "SI_EVENT_OOB_PAT_ACQUIRED", "I");
    (*env)->SetStaticIntField(env, obj, fID, MPE_SI_EVENT_OOB_PAT_ACQUIRED);

    fID = (*env)->GetStaticFieldID(env, obj, "SI_EVENT_OOB_PMT_ACQUIRED", "I");
    (*env)->SetStaticIntField(env, obj, fID, MPE_SI_EVENT_OOB_PMT_ACQUIRED);

    fID = (*env)->GetStaticFieldID(env, obj, "SI_EVENT_IB_PAT_ACQUIRED", "I");
    (*env)->SetStaticIntField(env, obj, fID, MPE_SI_EVENT_IB_PAT_ACQUIRED);

    fID = (*env)->GetStaticFieldID(env, obj, "SI_EVENT_IB_PMT_ACQUIRED", "I");
    (*env)->SetStaticIntField(env, obj, fID, MPE_SI_EVENT_IB_PMT_ACQUIRED);

    fID = (*env)->GetStaticFieldID(env, obj, "SI_EVENT_TRANSPORT_STREAM_UPDATE", "I");
    (*env)->SetStaticIntField(env, obj, fID, MPE_SI_EVENT_TRANSPORT_STREAM_UPDATE);

    fID = (*env)->GetStaticFieldID(env, obj, "SI_EVENT_NETWORK_UPDATE", "I");
    (*env)->SetStaticIntField(env, obj, fID, MPE_SI_EVENT_NETWORK_UPDATE);

    fID = (*env)->GetStaticFieldID(env, obj, "SI_EVENT_SERVICE_DETAILS_UPDATE", "I");
    (*env)->SetStaticIntField(env, obj, fID, MPE_SI_EVENT_SERVICE_DETAILS_UPDATE);

    fID = (*env)->GetStaticFieldID(env, obj, "SI_EVENT_SERVICE_COMPONENT_UPDATE", "I");
    (*env)->SetStaticIntField(env, obj, fID, MPE_SI_EVENT_SERVICE_COMPONENT_UPDATE);

    fID = (*env)->GetStaticFieldID(env, obj, "SI_EVENT_IB_PAT_UPDATE", "I");
    (*env)->SetStaticIntField(env, obj, fID, MPE_SI_EVENT_IB_PAT_UPDATE);

    fID = (*env)->GetStaticFieldID(env, obj, "SI_EVENT_IB_PMT_UPDATE", "I");
    (*env)->SetStaticIntField(env, obj, fID, MPE_SI_EVENT_IB_PMT_UPDATE);

    fID = (*env)->GetStaticFieldID(env, obj, "SI_EVENT_OOB_PAT_UPDATE", "I");
    (*env)->SetStaticIntField(env, obj, fID, MPE_SI_EVENT_OOB_PAT_UPDATE);

    fID = (*env)->GetStaticFieldID(env, obj, "SI_EVENT_OOB_PMT_UPDATE", "I");
    (*env)->SetStaticIntField(env, obj, fID, MPE_SI_EVENT_OOB_PMT_UPDATE);

    fID = (*env)->GetStaticFieldID(env, obj, "SI_EVENT_SI_ACQUIRING", "I");
    (*env)->SetStaticIntField(env, obj, fID, MPE_SI_EVENT_SI_ACQUIRING);

    fID = (*env)->GetStaticFieldID(env, obj, "SI_EVENT_SI_NOT_AVAILABLE_YET", "I");
    (*env)->SetStaticIntField(env, obj, fID, MPE_SI_EVENT_SI_NOT_AVAILABLE_YET);

    fID = (*env)->GetStaticFieldID(env, obj, "SI_EVENT_SI_FULLY_ACQUIRED", "I");
    (*env)->SetStaticIntField(env, obj, fID, MPE_SI_EVENT_SI_FULLY_ACQUIRED);

    fID = (*env)->GetStaticFieldID(env, obj, "SI_EVENT_SI_DISABLED", "I");
    (*env)->SetStaticIntField(env, obj, fID, MPE_SI_EVENT_SI_DISABLED);

    fID = (*env)->GetStaticFieldID(env, obj, "SI_EVENT_NIT_SVCT_ACQUIRED", "I");
    (*env)->SetStaticIntField(env, obj, fID, MPE_SI_EVENT_NIT_SVCT_ACQUIRED);

    fID = (*env)->GetStaticFieldID(env, obj, "SI_EVENT_TUNED_AWAY", "I");
    (*env)->SetStaticIntField(env, obj, fID, MPE_SI_EVENT_TUNED_AWAY);

    fID = (*env)->GetStaticFieldID(env, obj, "SI_CHANGE_TYPE_ADD", "I");
    (*env)->SetStaticIntField(env, obj, fID, MPE_SI_CHANGE_TYPE_ADD);

    fID = (*env)->GetStaticFieldID(env, obj, "SI_CHANGE_TYPE_MODIFY", "I");
    (*env)->SetStaticIntField(env, obj, fID, MPE_SI_CHANGE_TYPE_MODIFY);

    fID = (*env)->GetStaticFieldID(env, obj, "SI_CHANGE_TYPE_REMOVE", "I");
    (*env)->SetStaticIntField(env, obj, fID, MPE_SI_CHANGE_TYPE_REMOVE);
}

