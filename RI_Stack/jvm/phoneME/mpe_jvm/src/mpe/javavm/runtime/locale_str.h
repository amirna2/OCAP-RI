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
 * Mappings from partial locale names to full locale names
 */
static const char * const locale_aliases[] =
{ "ar", "ar_EG", "be", "be_BY", "bg", "bg_BG", "ca", "ca_ES", "cs", "cs_CZ",
        "cz", "cs_CZ", "da", "da_DK", "de", "de_DE", "el", "el_GR", "en",
        "en_US", "es", "es_ES", "et", "et_EE", "fi", "fi_FI", "fr", "fr_FR",
        "iw", "iw_IL", "hr", "hr_HR", "hu", "hu_HU", "is", "is_IS", "it",
        "it_IT", "ja", "ja_JP", "ko", "ko_KR", "lt", "lt_LT", "lv", "lv_LV",
        "mk", "mk_MK", "nl", "nl_NL", "no", "no_NO", "pl", "pl_PL", "pt",
        "pt_PT", "ro", "ro_RO", "ru", "ru_RU", "sh", "sh_YU", "sk", "sk_SK",
        "sl", "sl_SI", "sq", "sq_AL", "sr", "sr_YU", "su", "fi_FI", "sv",
        "sv_SE", "th", "th_TH", "tr", "tr_TR", "uk", "uk_UA", "zh", "zh_CN",
        "tchinese", "zh_TW", "big5", "zh_TW.Big5", "japanese", "ja_JP", "" };

/*
 * Solaris language string to ISO639 string mapping table.
 */
static const char * const language_names[] =
{ "C", "en", "POSIX", "en", "ar", "ar", "be", "be", "bg", "bg", "ca", "ca",
        "chinese", "zh", "cs", "cs", "cz", "cs", "da", "da", "de", "de", "el",
        "el", "en", "en", "es", "es", "et", "et", "fi", "fi", "su", "fi", "fr",
        "fr", "he", "iw", "hr", "hr", "hu", "hu", "is", "is", "it", "it", "iw",
        "iw", "ja", "ja", "japanese", "ja", "ko", "ko", "korean", "ko", "lt",
        "lt", "lv", "lv", "mk", "mk", "nl", "nl", "no", "no", "nr", "nr", "pl",
        "pl", "pt", "pt", "ro", "ro", "ru", "ru", "sh", "sh", "sk", "sk", "sl",
        "sl", "sq", "sq", "sr", "sr", "sv", "sv", "th", "th", "tr", "tr", "uk",
        "uk", "zh", "zh", "", };

/*
 * Solaris country string to ISO3166 string mapping table.
 * Currently only different string is UK/GB.
 */
static const char * const region_names[] =
{ "AT", "AT", "AU", "AU", "AR", "AR", "BE", "BE", "BR", "BR", "BO", "BO", "CA",
        "CA", "CH", "CH", "CL", "CL", "CN", "CN", "CO", "CO", "CR", "CR", "EC",
        "EC", "GT", "GT", "IE", "IE", "IL", "IL", "JP", "JP", "KR", "KR", "MX",
        "MX", "NI", "NI", "NZ", "NZ", "PA", "PA", "PE", "PE", "PY", "PY", "SV",
        "SV", "TH", "TH", "UK", "GB", "US", "US", "UY", "UY", "VE", "VE", "TW",
        "TW", "", };

/*
 * Solaris variant string to Java variant name mapping table.
 */
static const char * const variant_names[] =
{ "euro", "EURO", "", };
