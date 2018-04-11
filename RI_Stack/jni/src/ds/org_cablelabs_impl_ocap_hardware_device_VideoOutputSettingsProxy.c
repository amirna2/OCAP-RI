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

#include <org_cablelabs_impl_ocap_hardware_device_VideoOutputSettingsProxy.h>
#include "jni_util.h"
#include <mpe_disp.h>
#include <mpe_snd.h>
#include <mpe_error.h>


/**
 * Initializes JNI.
 */
JNIEXPORT void JNICALL
Java_org_cablelabs_impl_ocap_hardware_device_VideoOutputSettingsProxy_nInit(JNIEnv *env, jclass cls)
{
    jniutil_CachedIds.VideoOutputSettingsProxy_aspectRatio = NULL;
    jniutil_CachedIds.Dimension = NULL;
    jniutil_CachedIds.Dimension_width = NULL;

    /* Lookup fields in VideoOutputSettingsProxy */
    GET_FIELD_ID(VideoOutputSettingsProxy_serialNumber, "serialNumber", "I");
    GET_FIELD_ID(VideoOutputSettingsProxy_manufacturerName, "manufacturerName", "Ljava/lang/String;");
    GET_FIELD_ID(VideoOutputSettingsProxy_productCode, "productCode", "S");
    GET_FIELD_ID(VideoOutputSettingsProxy_manufactureYear, "manufactureYear", "B");
    GET_FIELD_ID(VideoOutputSettingsProxy_manufactureWeek, "manufactureWeek", "B");
    GET_FIELD_ID(VideoOutputSettingsProxy_aspectRatio, "aspectRatio", "Ljava/awt/Dimension;");
    GET_FIELD_ID(VideoOutputSettingsProxy_currentConfig, "currentConfig", "Lorg/ocap/hardware/device/VideoOutputConfiguration;");
    GET_FIELD_ID(VideoOutputSettingsProxy_isDisplayConnected, "isDisplayConnected", "Z");
    GET_FIELD_ID(VideoOutputSettingsProxy_isDynamicConfigSupported, "isDynamicConfigSupported", "Z");
    GET_FIELD_ID(VideoOutputSettingsProxy_portUniqueId, "parentUniqueId", "Ljava/lang/String;");

    /* Lookup methods in DeviceSettingsVideoOutputPortImpl */
    //GET_METHOD_ID(VideoOutputSettingsProxy_clearSupportedConfigurations,    "clearSupportedConfigurations",  "()V" );
    GET_METHOD_ID(VideoOutputSettingsProxy_addFixedConfig, "addFixedConfig", "(ZLjava/lang/String;IIIIIZII)V" );
    GET_METHOD_ID(VideoOutputSettingsProxy_updateFixedConfig, "updateFixedConfig", "(ZI)V" );

    GET_METHOD_ID(VideoOutputSettingsProxy_startDynamicConfig, "startDynamicConfig", "(I)V" );
    GET_METHOD_ID(VideoOutputSettingsProxy_addOutputResolution, "addOutputResolution", "(IIIIIZILjava/lang/String;IIIIIZII)V" );
    GET_METHOD_ID(VideoOutputSettingsProxy_endDynamicConfig, "endDynamicConfig", "(I)V" );

    GET_METHOD_ID(VideoOutputSettingsProxy_setCurrOutputConfigUsingHandle, "setCurrOutputConfigUsingHandle", "(I)V" );

    /* do this last since this changes the cls value */
    GET_CLASS(Dimension, "java/awt/Dimension");
    GET_FIELD_ID(Dimension_width, "width", "I");
    GET_FIELD_ID(Dimension_height, "height", "I");
}

jboolean commonUpdate(JNIEnv *env, jobject obj, mpe_DispOutputPort port)
{
    mpe_DispOutputPortInfo portInfo;
    mpe_DispVideoDisplayAttrInfo displayAttributes;
    jstring uniqueId = NULL;
    jobject aspectRatioDim = NULL;
    jstring name = NULL;

    int i=0, j=0;

    unsigned int fixedConfigInfoCount;
    unsigned int dynamicConfigInfoCount;
    unsigned int curConfigHandle;
    mpe_Bool configFound = false;
    mpe_Bool displayAttributesAvailable = true;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,"enter jboolean commonUpdate(JNIEnv *env, jobject obj, mpe_DispOutputPort port)\n");

    mpe_Error err = mpe_dispGetOutputPortInfo(port, &portInfo);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,"commonUpdate -- 1\n");
    if (MPE_SUCCESS != err)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "jni:commonUpdate failure--mpe_dispGetOutputPortInfo failed\n");
        return JNI_FALSE;
    }
    

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,"commonUpdate -- 2\n");
    uniqueId = (*env)->NewStringUTF(env, portInfo.idString);
    if (uniqueId == NULL)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "jni:commonUpdate failure--uniqueID = NULL \n");
        return JNI_FALSE;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,"commonUpdate -- 3\n");
    (*env)->SetObjectField(env, obj,
            jniutil_CachedIds.VideoOutputSettingsProxy_portUniqueId, uniqueId);

    (*env)->SetBooleanField(env, obj,
            jniutil_CachedIds.VideoOutputSettingsProxy_isDisplayConnected,
            JNI_ISTRUE(portInfo.connected));

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,"commonUpdate -- 5: port = %p\n", port);
        
    mpe_Error err1 = mpe_dispGetDisplayAttributes(port, &displayAttributes);
    MPE_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_JNI,
                "jni:commonUpdate--return value from mpe_dispGetDisplayAttributes = %d \n",
                err1);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,"commonUpdate -- 6: err1 = %d\n", err1);
    if (err1 != MPE_SUCCESS && err1 != MPE_DISP_ERROR_NOT_AVAILABLE)
    {
        return JNI_FALSE;
    }

    if (MPE_DISP_ERROR_NOT_AVAILABLE == err1)
    {
        displayAttributesAvailable = false;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,"commonUpdate -- 7: %d\n", displayAttributesAvailable);

    name = (*env)->NewStringUTF(env, displayAttributesAvailable?displayAttributes.manufacturerName:"");
    (*env)->SetObjectField(env, obj,
            jniutil_CachedIds.VideoOutputSettingsProxy_manufacturerName,
            name);
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,"commonUpdate -- 8: name = %s\n", displayAttributesAvailable?displayAttributes.manufacturerName:"");

    (*env)->SetByteField(env, obj,
            jniutil_CachedIds.VideoOutputSettingsProxy_manufactureWeek,
            (jbyte) (displayAttributesAvailable?displayAttributes.manufactureWeek:0));

    (*env)->SetByteField(env, obj,
            jniutil_CachedIds.VideoOutputSettingsProxy_manufactureYear,
            (jbyte) (displayAttributesAvailable?displayAttributes.manufactureYear:0));

    (*env)->SetShortField(env, obj,
            jniutil_CachedIds.VideoOutputSettingsProxy_productCode,
            (jshort) (displayAttributesAvailable?displayAttributes.productCode:0));

    (*env)->SetIntField(env, obj,
            jniutil_CachedIds.VideoOutputSettingsProxy_serialNumber,
            (jint) (displayAttributesAvailable?displayAttributes.serialNumber:0));

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,"commonUpdate -- 9\n");
    aspectRatioDim = (*env)->GetObjectField(env, obj,
            jniutil_CachedIds.VideoOutputSettingsProxy_aspectRatio);


    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,"commonUpdate -- 10\n");
    if (MPE_SUCCESS != mpe_dispGetSupportedFixedVideoOutputConfigurationCount(port, &fixedConfigInfoCount))
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "jni:commonUpdate--mpe_dispGetSupportedFixedVideoOutputConfigurationCount failed--port = %p Count = %d \n", port, fixedConfigInfoCount);
        return JNI_FALSE;
    } 

    // Get count of number of dynamic vid ouput configs
    if (MPE_SUCCESS != mpe_dispGetSupportedDynamicVideoOutputConfigurationCount(port, &dynamicConfigInfoCount))
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "jni:commonUpdate--mpe_dispGetSupportedDynamicVideoOutputConfigurationCount failed--port = %p Count = %d \n", port, dynamicConfigInfoCount);
        return JNI_FALSE;
    }

                
    curConfigHandle = (unsigned int) (portInfo.fixedConfigInfo->curConfig);
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "jni:commonUpdate: curConfig = %x\n", curConfigHandle);

    // the following code determined whether the curConfig is a fixedConfig or a dynamicConfig.  There must be an easier way to do this...
    for (i=0; i<fixedConfigInfoCount; i++)
    {
        unsigned int fixedConfigHandle = (unsigned int) (portInfo.fixedConfigInfo->fixedConfigs + i);
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "jni:commonUpdate: fixedConfig[%d] = %x\n", i, fixedConfigHandle);
        if (curConfigHandle == fixedConfigHandle)
        {
            mpe_DispFixedVideoOutputConfigInfo* pVideoOutputConfig = (mpe_DispFixedVideoOutputConfigInfo *)portInfo.fixedConfigInfo->curConfig;
            jniutil_setDimension(env, aspectRatioDim,
                    (jint) pVideoOutputConfig->resolution->aspectRatio.width,
                    (jint) pVideoOutputConfig->resolution->aspectRatio.height);
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "+(ds-vop-j) jni:commonUpdate--aspectRatio set to (%d, %d) (fixed config)\n", 
                pVideoOutputConfig->resolution->aspectRatio.width, pVideoOutputConfig->resolution->aspectRatio.height);
            configFound = true;
            break;
        }
    }

    if (!configFound)
    {
        for (i=0; i<dynamicConfigInfoCount; i++)
        {

            float currentDisplayAR = 0.0;
            if (displayAttributesAvailable)
            {
                currentDisplayAR = displayAttributes.aspectRatio.width / displayAttributes.aspectRatio.height;
            }

            unsigned int dynamicConfigHandle = (unsigned int) (portInfo.fixedConfigInfo->dynamicConfigs + i*sizeof(mpe_DispDynamicVideoOutputConfigInfo*));
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "jni:commonUpdate: dynamicConfigInfoArray = %x\n", dynamicConfigHandle);
            if (curConfigHandle == dynamicConfigHandle)
            {
                configFound = true;
                mpe_DispDynamicVideoOutputConfigInfo* pVideoOutputConfig = (mpe_DispDynamicVideoOutputConfigInfo *)portInfo.fixedConfigInfo->curConfig;
                for (j=0; j<pVideoOutputConfig->mappingsCount; j++)
                {
                    mpe_GfxDimensions displayAspectRatio = pVideoOutputConfig->mappings[j].inputResolution->aspectRatio;
                    float displayAR = displayAspectRatio.width / displayAspectRatio.height;
                    if (currentDisplayAR == displayAR)
                    {
                        mpe_DispFixedVideoOutputConfigInfo* pFixedVideoOutputConfig = pVideoOutputConfig->mappings[j].outputResolution;
                        jniutil_setDimension(env, aspectRatioDim,
                                (jint) pFixedVideoOutputConfig->resolution->aspectRatio.width,
                                (jint) pFixedVideoOutputConfig->resolution->aspectRatio.height);
                        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "+(ds-vop-j) jni:commonUpdate--aspectRatio set to (%d, %d) (dynamic config)\n", 
                            pFixedVideoOutputConfig->resolution->aspectRatio.width, pFixedVideoOutputConfig->resolution->aspectRatio.height);
                        break;
                    }
                }
                break;
            }
        }
    }

    if (!configFound)
    {
        return JNI_FALSE;
    }


    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,"exit jboolean commonUpdate(JNIEnv *env, jobject obj, mpe_DispOutputPort port)\n");

    return JNI_TRUE;

}

JNIEXPORT jboolean JNICALL
Java_org_cablelabs_impl_ocap_hardware_device_VideoOutputSettingsProxy_nInitValues(JNIEnv *env, jobject obj, jint videoPortHandle)
{
    const mpe_DispOutputPort port = (mpe_DispOutputPort) videoPortHandle;
    unsigned int fixedConfigInfoCount;
    mpe_DispFixedVideoOutputConfigInfo* fixedConfigInfoArray;
    mpe_DispVideoResolutionInfo* fixedResolutionPtr;
    unsigned int dynamicConfigInfoCount;
    mpe_DispDynamicVideoOutputConfigInfo* dynamicConfigInfoArray;
    unsigned int idx, idy;
    //  jstring                               portUniqueId;       unused
    jstring name;
    jobject aspectRatioDim;
    mpe_Error err = MPE_DISP_ERROR_UNIMPLEMENTED;


	// DSJ
	mpe_DispDynamicVideoOutputConfigInfo** dynamicConfigInfoPointerArray;
	// DSJ
	mpe_DispFixedVideoOutputConfigInfo** fixedConfigInfoPointerArray;



    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "jni:nInitValues called, handle = %d port = %p port \n",
            (int)videoPortHandle, port );

    aspectRatioDim = (*env)->GetObjectField(env, obj, jniutil_CachedIds.VideoOutputSettingsProxy_aspectRatio);


    if (aspectRatioDim == NULL)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "jni:nInitValues--aspectRatioDim == NULL \n");
        return JNI_FALSE;
    }else
    {
    	MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "jni:nInitValues--aspectRatioDim != NULL \n" );
    }

    err = commonUpdate(env, obj, port);

    if (JNI_TRUE != err)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "jni:nInitValues--commonUpdate == FALSE port = %p \n", port);
        return JNI_FALSE;
    }

    if (MPE_SUCCESS != mpe_dispGetSupportedFixedVideoOutputConfigurationCount(port, &fixedConfigInfoCount))
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "jni:nInitValues--mpe_dispGetSupportedFixedVideoOutputConfigurationCount failed--port = %p Count = %d \n", port, fixedConfigInfoCount);
        return JNI_FALSE;
    } else {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "SupportedFixedVideoOutputConfigurationCount success--port = %p Count = %d \n", port, fixedConfigInfoCount);
    }

		// DSJ
		// Allocate the memory for the for the Array of pointer
		if (MPE_SUCCESS != mpe_memAllocP(MPE_MEM_TEMP, fixedConfigInfoCount *sizeof(mpe_DispFixedVideoOutputConfigInfo*),  (void*) &fixedConfigInfoPointerArray))
		{
			MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, ">>>>> failed to alocate memory for array of pointers to mpe_DispFixedVideoOutputConfigInfo <<<<<\n");
			return JNI_FALSE;
		} else {
			MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, ">>>>> allocated memory for array of pointers to mpe_DispFixedVideoOutputConfigInfo <<<<<\n");
		}
		// End allocation

		if (MPE_SUCCESS != mpe_dispGetSupportedFixedVideoOutputConfigurations(port, fixedConfigInfoPointerArray))
		{
			MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "jni:nInitValues--fixed ... Configurations failed \n");
			mpe_memFreeP(MPE_MEM_TEMP, fixedConfigInfoPointerArray);
			MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, ">>>>> deallocate array of pointers to mpe_DispFixedVideoOutputConfigInfo  <<<<< \n");
			return JNI_FALSE;
		}
	    else
	    {
	        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "jni:nInitValues--mpe_dispGetSupportedFixedVideoOutputConfigurations success--port = %p Count = %d \n", port, fixedConfigInfoCount);
	        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, ">> FIXED >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n");
	        for (idx =0 ; idx < fixedConfigInfoCount ; idx++)
	        {
	        	MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, ">> ID = %s \n", fixedConfigInfoPointerArray[idx]->idString);
	        	MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "	>> Enabled = %d \n", fixedConfigInfoPointerArray[idx]->enabled);
	        	MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "	>> pixel height = %d \n", fixedConfigInfoPointerArray[idx]->resolution->pixelResolution.height);
	        	MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "	>> pixel width = %d \n", fixedConfigInfoPointerArray[idx]->resolution->pixelResolution.width);
	        	MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "	>> aspectRatio height = %d \n", fixedConfigInfoPointerArray[idx]->resolution->aspectRatio.height);
	            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "	>> aspectRatio width = %d \n", fixedConfigInfoPointerArray[idx]->resolution->aspectRatio.width);
	            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "	>> frame rate= %d \n", fixedConfigInfoPointerArray[idx]->resolution->frameRate);
	            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "	>> interlaced = %d \n", fixedConfigInfoPointerArray[idx]->resolution->interlaced);
	        }
	        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, ">> END FIXED >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n");

	    }
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "set pointer address = %p\n", *fixedConfigInfoPointerArray);
		fixedConfigInfoArray = *fixedConfigInfoPointerArray;
	    // END

    if (MPE_SUCCESS != mpe_dispGetSupportedDynamicVideoOutputConfigurationCount(port, &dynamicConfigInfoCount))
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "jni:nInitValues--mpe_dispGetSupportedDynamicVideoOutputConfigurationCount failed--port = %p Count = %d \n", port, dynamicConfigInfoCount);
		mpe_memFreeP(MPE_MEM_TEMP, fixedConfigInfoPointerArray);
		MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, ">>>>> deallocate array of pointers to mpe_DispFixedVideoOutputConfigInfo  <<<<< \n");
        return JNI_FALSE;
    }


		// DSJ
		// Allocate the memory for the for the Array of pointer
		if (MPE_SUCCESS != mpe_memAllocP(MPE_MEM_TEMP, dynamicConfigInfoCount *sizeof(mpe_DispDynamicVideoOutputConfigInfo*),  (void*) &dynamicConfigInfoPointerArray))
		{
			MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, ">>>>> failed to allocate memory for array of pointers to mpe_DispDynamicVideoOutputConfigInfo <<<<<\n");
			mpe_memFreeP(MPE_MEM_TEMP, fixedConfigInfoPointerArray);
			MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, ">>>>> deallocate array of pointers to mpe_DispFixedVideoOutputConfigInfo  <<<<< \n");
			return JNI_FALSE;
		} else {
			MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, ">>>>> allocated memory for array of pointers to mpe_DispDynamicVideoOutputConfigInfo <<<<<\n");
		}


		if (MPE_SUCCESS != mpe_dispGetSupportedDynamicVideoOutputConfigurations(port, dynamicConfigInfoPointerArray))
		{
			MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "jni:nInitValues--dynamic ... Configurations failed \n");
			mpe_memFreeP(MPE_MEM_TEMP, fixedConfigInfoPointerArray);
			MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, ">>>>> deallocate array of pointers to mpe_DispFixedVideoOutputConfigInfo  <<<<< \n");
			mpe_memFreeP(MPE_MEM_TEMP, dynamicConfigInfoPointerArray);
			MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, ">>>>> deallocate array of pointers to mpe_DispDynamicVideoOutputConfigInfo  <<<<< \n");
			return JNI_FALSE;
		}else
	    {
	        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "jni:nInitValues--mpe_dispGetSupportedDynamicVideoOutputConfigurations success--port = %p Count = %d \n", port, dynamicConfigInfoCount);
	        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, ">> DYNAMIC >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n");
	        for (idx =0 ; idx < dynamicConfigInfoCount ; idx++)
	        {
	        	MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, ">> DynamicConfigInfoPointerArray %d >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n", idx);
	        	MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, ">> ID = %s \n", dynamicConfigInfoPointerArray[idx]->idString);
	        	MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "	>> Enabled = %d \n", dynamicConfigInfoPointerArray[idx]->enabled);
	        	MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "	>> mappings count = %d \n", dynamicConfigInfoPointerArray[idx]->mappingsCount);

	        	for (idy =0; idy < dynamicConfigInfoPointerArray[idx]->mappingsCount ; idy++)
	        	{
	        		MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, ">> MAPPING %d >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n", idy);
	                mpe_DispVideoResolutionInfo* inputResolution = (*dynamicConfigInfoPointerArray)[idx].mappings[idy].inputResolution;
	                mpe_DispVideoResolutionInfo* outputResolution =(*dynamicConfigInfoPointerArray)[idx].mappings[idy].outputResolution->resolution;


	        	    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, " >>> INPUT \n");

					MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "	>> pixel height = %d \n", inputResolution->pixelResolution.height);
					MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "	>> pixel width = %d \n", inputResolution->pixelResolution.width);

					MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "	>> aspectRatio height = %d \n", inputResolution->aspectRatio.height);
					MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "	>> aspectRatio width = %d \n", inputResolution->aspectRatio.width);

					MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "	>> frame rate= %d \n", inputResolution->frameRate);
					MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "	>> interlaced = %d \n", inputResolution->interlaced);

				    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, " >>> END INPUT \n");


					MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, " >>> OUTPUT \n");

					MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "	>> pixel height = %d \n", outputResolution->pixelResolution.height);
					MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "	>> pixel width = %d \n", outputResolution->pixelResolution.width);

					MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "	>> aspectRatio height = %d \n", outputResolution->aspectRatio.height);
					MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "	>> aspectRatio width = %d \n", outputResolution->aspectRatio.width);

					MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "	>> frame rate= %d \n", outputResolution->frameRate);
					MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "	>> interlaced = %d \n", outputResolution->interlaced);
					MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, " >>> END OUTPUT \n");

	        	}
	        }
	        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, ">> END DYNAMIC >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n");

	    }
		dynamicConfigInfoArray = *dynamicConfigInfoPointerArray;

    for (idx = 0; idx < fixedConfigInfoCount; idx++)
    {
    	MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "java : VideoOutputSettingsProxy.addFixedConfig(...)\n");
        name = (*env)->NewStringUTF(env, fixedConfigInfoArray[idx].idString);

        fixedResolutionPtr = fixedConfigInfoArray[idx].resolution;

        /* fixed video configuration */
        /* (String name, int rezWidth, int rezHeight, int arWidth, int arHeight, int rate, boolean interlaced, int configMPEHandle )*/
        (*env)->CallVoidMethod(env, obj, jniutil_CachedIds.VideoOutputSettingsProxy_addFixedConfig,
                fixedConfigInfoArray[idx].enabled,
                name,
                fixedResolutionPtr->pixelResolution.width, fixedResolutionPtr->pixelResolution.height,
                fixedResolutionPtr->aspectRatio.width, fixedResolutionPtr->aspectRatio.height,
                fixedResolutionPtr->frameRate,
                fixedResolutionPtr->interlaced,
                fixedResolutionPtr->stereoscopicMode,
                (jint) (mpe_DispVideoConfig) &fixedConfigInfoArray[idx]);
    }

    for (idx = 0; idx < dynamicConfigInfoCount; idx++)
    {
    	MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "java : VideoOutputSettingsProxy.startDynamicConfig(...)\n");
        (*env)->CallVoidMethod(env, obj, jniutil_CachedIds.VideoOutputSettingsProxy_startDynamicConfig, port);
        int mappingsIdx;

        for (mappingsIdx = 0; mappingsIdx < dynamicConfigInfoArray[idx].mappingsCount; mappingsIdx++)
        {
        	MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "java : VideoOutputSettingsProxy.addOutputResolution(...)\n");
            mpe_DispVideoResolutionInfo* inputResolutionPtr = dynamicConfigInfoArray[idx].mappings[mappingsIdx].inputResolution;
            mpe_DispVideoResolutionInfo* outputResolutionPtr = dynamicConfigInfoArray[idx].mappings[mappingsIdx].outputResolution->resolution;
            jstring inputName = (*env)->NewStringUTF(env, dynamicConfigInfoArray[idx].mappings[mappingsIdx].outputResolution->idString);

            (*env)->CallVoidMethod(env, obj, jniutil_CachedIds.VideoOutputSettingsProxy_addOutputResolution,
                    inputResolutionPtr->pixelResolution.width,
                    inputResolutionPtr->pixelResolution.height,
                    inputResolutionPtr->aspectRatio.width,
                    inputResolutionPtr->aspectRatio.height,
                    inputResolutionPtr->frameRate,
                    inputResolutionPtr->interlaced,
                    inputResolutionPtr->stereoscopicMode,
                    inputName,
                    outputResolutionPtr->pixelResolution.width,
                    outputResolutionPtr->pixelResolution.height,
                    outputResolutionPtr->aspectRatio.width,
                    outputResolutionPtr->aspectRatio.height,
                    outputResolutionPtr->frameRate,
                    outputResolutionPtr->interlaced,
                    outputResolutionPtr->stereoscopicMode,
                    port );
        }
        (*env)->CallVoidMethod(env, obj, jniutil_CachedIds.VideoOutputSettingsProxy_endDynamicConfig, port);
        idx++;
    }

		
    // Free the memory earlier allocated to the Array before
    // existing the function
	mpe_memFreeP(MPE_MEM_TEMP, fixedConfigInfoPointerArray);
	MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, ">>>>> deallocate array of pointers to mpe_DispFixedVideoOutputConfigInfo  <<<<< \n");

	mpe_memFreeP(MPE_MEM_TEMP, dynamicConfigInfoPointerArray);
	MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, ">>>>> deallocate array of pointers to mpe_DispDynamicVideoOutputConfigInfo  <<<<< \n");
	// End Free

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "jni:nInitValues--commonUpdate return = JNI_TRUE \n");

    return JNI_TRUE;

}

JNIEXPORT jboolean JNICALL
Java_org_cablelabs_impl_ocap_hardware_device_VideoOutputSettingsProxy_nInitCurrentConfig(JNIEnv *env, jobject obj, jint videoPortHandle)
{
    const mpe_DispOutputPort port = (mpe_DispOutputPort) videoPortHandle;
    mpe_DispVideoConfig currConfigHandle;

    if (MPE_SUCCESS != mpe_dispGetCurVideoOutputConfiguration(port, &currConfigHandle))
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "jni:nInitValues--current ... configurations failed \n");
        return JNI_FALSE;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "java : VideoOutputSettingsProxy.setCurrOutputConfigUsingHandle(...)\n");
    /* done last so configs exist.  Sets the default and java will be in charge of keeping track of the current config from this point on */
    (*env)->CallVoidMethod(env, obj, jniutil_CachedIds.VideoOutputSettingsProxy_setCurrOutputConfigUsingHandle, currConfigHandle);

    return JNI_TRUE;
}

// TODO, TODO_DS:  refactor nInitInfo and the refresh function to share methods since much of the code is shared.
JNIEXPORT jboolean JNICALL
Java_org_cablelabs_impl_ocap_hardware_device_VideoOutputSettingsProxy_nRefreshDisplayInfo(JNIEnv *env, jobject obj, jint videoPortHandle)
{
    const mpe_DispOutputPort port = (mpe_DispOutputPort) videoPortHandle;
    unsigned int fixedConfigInfoCount;
    mpe_DispFixedVideoOutputConfigInfo* fixedConfigInfoArray;
    unsigned int idx;

    	MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "entered java_org_cablelabs_impl_ocap_hardware_device_VideoOutputSettingsProxy_nRefreshDisplayInfo \n");

		// DSJ
		mpe_DispFixedVideoOutputConfigInfo** fixedConfigInfoPointerArray;
		// END


	    if (JNI_FALSE == commonUpdate(env, obj, port))
	    {
	        return JNI_FALSE;
	    }

	    if (MPE_SUCCESS != mpe_dispGetSupportedFixedVideoOutputConfigurationCount(port, &fixedConfigInfoCount))
	    {
	        return JNI_FALSE;
	    }

		// DSJ
		// Allocate the memory for the for the Array of pointer
		if (MPE_SUCCESS != mpe_memAllocP(MPE_MEM_TEMP, fixedConfigInfoCount *sizeof(mpe_DispFixedVideoOutputConfigInfo*),  (void*) &fixedConfigInfoPointerArray))
		{
			MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, ">>>>> failed to allocate memory for array of pointers to mpe_DispFixedVideoOutputConfigInfo <<<<<\n");
			return JNI_FALSE;
		} else {
			MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, ">>>>> allocated memory for array of pointers to mpe_DispFixedVideoOutputConfigInfo <<<<<\n");
		}


		if (MPE_SUCCESS != mpe_dispGetSupportedFixedVideoOutputConfigurations(port, fixedConfigInfoPointerArray))
		{
			MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "jni:nInitValues--fixed ... Configurations failed \n");
			return JNI_FALSE;
		}else {
	        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "jni:nInitValues--mpe_dispGetSupportedFixedVideoOutputConfigurations success--port = %p Count = %d \n", port, fixedConfigInfoCount);
	        for (idx =0 ; idx < fixedConfigInfoCount ; idx++)
	        {
	        	MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, ">>>>>>>>>>>>>>>>>>>>>>>>>> \n");
	        	MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, ">> ID = %s \n", fixedConfigInfoPointerArray[idx]->idString);
	        	MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "	>> Enabled = %d \n", fixedConfigInfoPointerArray[idx]->enabled);
	        	MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "	>> pixel height = %d \n", fixedConfigInfoPointerArray[idx]->resolution->pixelResolution.height);
	        	MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "	>> pixel width = %d \n", fixedConfigInfoPointerArray[idx]->resolution->pixelResolution.width);
	        	MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "	>> aspectRatio height = %d \n", fixedConfigInfoPointerArray[idx]->resolution->aspectRatio.height);
	            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "	>> aspectRatio width = %d \n", fixedConfigInfoPointerArray[idx]->resolution->aspectRatio.width);
	            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "	>> frame rate= %d \n", fixedConfigInfoPointerArray[idx]->resolution->frameRate);
	            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "	>> interlaced = %d \n", fixedConfigInfoPointerArray[idx]->resolution->interlaced);
	        }

	    }
		MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "setr pointer address = %p\n", *fixedConfigInfoPointerArray);
		fixedConfigInfoArray = *fixedConfigInfoPointerArray;
		// END

    for (idx = 0; idx < fixedConfigInfoCount; idx++)
    {
    	MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "java : VideoOutputSettingsProxy.addFixedConfig(...)\n");
        (*env)->CallVoidMethod(env, obj, jniutil_CachedIds.VideoOutputSettingsProxy_updateFixedConfig,
                fixedConfigInfoArray[idx].enabled,
                (jint) (mpe_DispVideoConfig) &fixedConfigInfoArray[idx]
        );
    }

    /* current config not set because Java layer is driving the changes to that value after initialization */


		// DSJ
		mpe_memFreeP(MPE_MEM_TEMP, fixedConfigInfoPointerArray);
		MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, ">>>>> deallocate array of pointers to mpe_DispFixedVideoOutputConfigInfo  <<<<< \n");
		// END
		MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "exiting java_org_cablelabs_impl_ocap_hardware_device_VideoOutputSettingsProxy_nRefreshDisplayInfo \n");

    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_org_cablelabs_impl_ocap_hardware_device_VideoOutputSettingsProxy_nSetCurrentOutputConfig(JNIEnv *env, jobject obj, jint videoPortHandle, jint value)
{
    if (MPE_SUCCESS != mpe_dispSetCurVideoOutputConfiguration((mpe_DispOutputPort) videoPortHandle, (mpe_DispVideoConfig) value) )
    {
        return JNI_FALSE;
    }
    return JNI_TRUE;
}


JNIEXPORT jboolean JNICALL Java_org_cablelabs_impl_ocap_hardware_device_VideoOutputSettingsProxy_nIsContentProtected(
        JNIEnv *env, jobject obj, jint videoPortHandle)
{
    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(obj);

    const mpe_DispOutputPort port = (mpe_DispOutputPort) videoPortHandle;

    mpe_Bool encrypted = false;
    mpe_Error err =  mpe_dispIsContentProtected(port, &encrypted);

    if (MPE_SUCCESS != err)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "jni:GetIsContentProtected failure -- mpeos_dispIsContentProtected failed\n");
        return JNI_FALSE;
    }
    
    if (encrypted)
    {
        return JNI_TRUE;
    }
    else
    {
        return JNI_FALSE;
    }
}

