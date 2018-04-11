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

package org.davic.media;

/**
 * This interface is the base interface for both audio and subtitling language
 * control. This interface should never be implemented in a control alone, but
 * always either as audio or subtitling language control. If a language can not
 * be selected because the user/application is not entitled to access it, the
 * language is reset to that before the invocation of the method. If more than
 * one stream with the same language exists, the behaviour of
 * selectLanguage(String)is to select the first listed in the network
 * signalling. NOTE:This is equivalent to item b under
 * 11.4.2.3,"Default media player behaviour".
 */
public interface LanguageControl extends javax.media.Control
{
    /**
     * Provides the application a list of available languages. The returned
     * strings contain three letter language codes according to ISO 639
     * standard. If there are no selectable languages, the method returns an
     * array of length zero.
     */
    public String[] listAvailableLanguages();

    /**
     * Changes the language to the language given in the parameter.
     * 
     * @param lang
     *            the desired language code according to the ISO 639 standard.
     * @exception LanguageNotAvailableException
     *                if the language given in the parameter is not available,
     * @exception org.davic.media.NotAuthorizedException
     *                if access to the required language is not permitted
     */
    public void selectLanguage(String lang) throws LanguageNotAvailableException,
            org.davic.media.NotAuthorizedException;

    /**
     * Returns the language code of the currently selected language. If this
     * information is not available, a String of length zero is returned.
     */
    public String getCurrentLanguage();

    /**
     * Changes the language to the default language determined by the
     * implementation.
     * 
     * @return the language code of the default language.
     * @exception org.davic.media.NotAuthorizedException
     *                If access to the default language is not permitted
     */
    public String selectDefaultLanguage() throws org.davic.media.NotAuthorizedException;
}
