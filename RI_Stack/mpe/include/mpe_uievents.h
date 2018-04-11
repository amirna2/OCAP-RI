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

#ifndef _MPE_UIEVENTS_H
#define _MPE_UIEVENTS_H

#define OCAP_KEY_FIRST 400L
#define OCAP_KEY_LAST 402L
#define OCAP_KEY_TYPED 400L
#define OCAP_KEY_PRESSED 401L
#define OCAP_KEY_RELEASED 402L
#define OCAP_VK_ENTER 10L
#define OCAP_VK_BACK_SPACE 8L
#define OCAP_VK_TAB 9L
#define OCAP_VK_CANCEL 3L
#define OCAP_VK_CLEAR 12L
#define OCAP_VK_SHIFT 16L
#define OCAP_VK_CONTROL 17L
#define OCAP_VK_ALT 18L
#define OCAP_VK_PAUSE 19L
#define OCAP_VK_CAPS_LOCK 20L
#define OCAP_VK_ESCAPE 27L
#define OCAP_VK_SPACE 32L
#define OCAP_VK_PAGE_UP 33L
#define OCAP_VK_PAGE_DOWN 34L
#define OCAP_VK_END 35L
#define OCAP_VK_HOME 36L
#define OCAP_VK_LEFT 37L
#define OCAP_VK_UP 38L
#define OCAP_VK_RIGHT 39L
#define OCAP_VK_DOWN 40L
#define OCAP_VK_COMMA 44L
#define OCAP_VK_PERIOD 46L
#define OCAP_VK_SLASH 47L
#define OCAP_VK_0 48L
#define OCAP_VK_1 49L
#define OCAP_VK_2 50L
#define OCAP_VK_3 51L
#define OCAP_VK_4 52L
#define OCAP_VK_5 53L
#define OCAP_VK_6 54L
#define OCAP_VK_7 55L
#define OCAP_VK_8 56L
#define OCAP_VK_9 57L
#define OCAP_VK_SEMICOLON 59L
#define OCAP_VK_EQUALS 61L
#define OCAP_VK_A 65L
#define OCAP_VK_B 66L
#define OCAP_VK_C 67L
#define OCAP_VK_D 68L
#define OCAP_VK_E 69L
#define OCAP_VK_F 70L
#define OCAP_VK_G 71L
#define OCAP_VK_H 72L
#define OCAP_VK_I 73L
#define OCAP_VK_J 74L
#define OCAP_VK_K 75L
#define OCAP_VK_L 76L
#define OCAP_VK_M 77L
#define OCAP_VK_N 78L
#define OCAP_VK_O 79L
#define OCAP_VK_P 80L
#define OCAP_VK_Q 81L
#define OCAP_VK_R 82L
#define OCAP_VK_S 83L
#define OCAP_VK_T 84L
#define OCAP_VK_U 85L
#define OCAP_VK_V 86L
#define OCAP_VK_W 87L
#define OCAP_VK_X 88L
#define OCAP_VK_Y 89L
#define OCAP_VK_Z 90L
#define OCAP_VK_OPEN_BRACKET 91L
#define OCAP_VK_BACK_SLASH 92L
#define OCAP_VK_CLOSE_BRACKET 93L
#define OCAP_VK_NUMPAD0 96L
#define OCAP_VK_NUMPAD1 97L
#define OCAP_VK_NUMPAD2 98L
#define OCAP_VK_NUMPAD3 99L
#define OCAP_VK_NUMPAD4 100L
#define OCAP_VK_NUMPAD5 101L
#define OCAP_VK_NUMPAD6 102L
#define OCAP_VK_NUMPAD7 103L
#define OCAP_VK_NUMPAD8 104L
#define OCAP_VK_NUMPAD9 105L
#define OCAP_VK_MULTIPLY 106L
#define OCAP_VK_ADD 107L
#define OCAP_VK_SEPARATER 108L
#define OCAP_VK_SUBTRACT 109L
#define OCAP_VK_DECIMAL 110L
#define OCAP_VK_DIVIDE 111L
#define OCAP_VK_F1 112L
#define OCAP_VK_F2 113L
#define OCAP_VK_F3 114L
#define OCAP_VK_F4 115L
#define OCAP_VK_F5 116L
#define OCAP_VK_F6 117L
#define OCAP_VK_F7 118L
#define OCAP_VK_F8 119L
#define OCAP_VK_F9 120L
#define OCAP_VK_F10 121L
#define OCAP_VK_F11 122L
#define OCAP_VK_F12 123L
#define OCAP_VK_DELETE 127L
#define OCAP_VK_NUM_LOCK 144L
#define OCAP_VK_SCROLL_LOCK 145L
#define OCAP_VK_ASTERISK 151L
#define OCAP_VK_PRINTSCREEN 154L
#define OCAP_VK_INSERT 155L
#define OCAP_VK_HELP 156L
#define OCAP_VK_META 157L
#define OCAP_VK_BACK_QUOTE 192L
#define OCAP_VK_QUOTE 222L
#define OCAP_VK_FINAL 24L
#define OCAP_VK_CONVERT 28L
#define OCAP_VK_NONCONVERT 29L
#define OCAP_VK_ACCEPT 30L
#define OCAP_VK_MODECHANGE 31L
#define OCAP_VK_KANA 21L
#define OCAP_VK_KANJI 25L
#define OCAP_VK_UNDEFINED 0L
#define OCAP_CHAR_UNDEFINED 0L
#define OCAP_RC_FIRST 400L
#define OCAP_VK_COLORED_KEY_0 403L
#define OCAP_VK_COLORED_KEY_1 404L
#define OCAP_VK_COLORED_KEY_2 405L
#define OCAP_VK_COLORED_KEY_3 406L
#define OCAP_VK_COLORED_KEY_4 407L
#define OCAP_VK_COLORED_KEY_5 408L
#define OCAP_VK_POWER 409L
#define OCAP_VK_DIMMER 410L
#define OCAP_VK_WINK 411L
#define OCAP_VK_REWIND 412L
#define OCAP_VK_STOP 413L
#define OCAP_VK_EJECT_TOGGLE 414L
#define OCAP_VK_PLAY 415L
#define OCAP_VK_RECORD 416L
#define OCAP_VK_FAST_FWD 417L
#define OCAP_VK_PLAY_SPEED_UP 418L
#define OCAP_VK_PLAY_SPEED_DOWN 419L
#define OCAP_VK_PLAY_SPEED_RESET 420L
#define OCAP_VK_RECORD_SPEED_NEXT 421L
#define OCAP_VK_GO_TO_START 422L
#define OCAP_VK_GO_TO_END 423L
#define OCAP_VK_TRACK_PREV 424L
#define OCAP_VK_TRACK_NEXT 425L
#define OCAP_VK_RANDOM_TOGGLE 426L
#define OCAP_VK_CHANNEL_UP 427L
#define OCAP_VK_CHANNEL_DOWN 428L
#define OCAP_VK_STORE_FAVORITE_0 429L
#define OCAP_VK_STORE_FAVORITE_1 430L
#define OCAP_VK_STORE_FAVORITE_2 431L
#define OCAP_VK_STORE_FAVORITE_3 432L
#define OCAP_VK_RECALL_FAVORITE_0 433L
#define OCAP_VK_RECALL_FAVORITE_1 434L
#define OCAP_VK_RECALL_FAVORITE_2 435L
#define OCAP_VK_RECALL_FAVORITE_3 436L
#define OCAP_VK_CLEAR_FAVORITE_0 437L
#define OCAP_VK_CLEAR_FAVORITE_1 438L
#define OCAP_VK_CLEAR_FAVORITE_2 439L
#define OCAP_VK_CLEAR_FAVORITE_3 440L
#define OCAP_VK_SCAN_CHANNELS_TOGGLE 441L
#define OCAP_VK_PINP_TOGGLE 442L
#define OCAP_VK_SPLIT_SCREEN_TOGGLE 443L
#define OCAP_VK_DISPLAY_SWAP 444L
#define OCAP_VK_SCREEN_MODE_NEXT 445L
#define OCAP_VK_VIDEO_MODE_NEXT 446L
#define OCAP_VK_VOLUME_UP 447L
#define OCAP_VK_VOLUME_DOWN 448L
#define OCAP_VK_MUTE 449L
#define OCAP_VK_SURROUND_MODE_NEXT 450L
#define OCAP_VK_BALANCE_RIGHT 451L
#define OCAP_VK_BALANCE_LEFT 452L
#define OCAP_VK_FADER_FRONT 453L
#define OCAP_VK_FADER_REAR 454L
#define OCAP_VK_BASS_BOOST_UP 455L
#define OCAP_VK_BASS_BOOST_DOWN 456L
#define OCAP_VK_INFO 457L
#define OCAP_VK_GUIDE 458L
#define OCAP_VK_TELETEXT 459L
#define OCAP_VK_SUBTITLE 460L
#define OCAP_RC_LAST 460L
#define OCAP_VK_NUMBER_SIGN 520L
#define OCAP_OCRC_FIRST 600L
#define OCAP_VK_RF_BYPASS 600L
#define OCAP_VK_EXIT 601L
#define OCAP_VK_MENU 602L
#define OCAP_VK_NEXT_DAY 603L
#define OCAP_VK_PREV_DAY 604L
#define OCAP_VK_APPS 605L
#define OCAP_VK_LINK 606L
#define OCAP_VK_LAST 607L
#define OCAP_VK_BACK 608L
#define OCAP_VK_FORWARD 609L
#define OCAP_VK_ZOOM 610L
#define OCAP_VK_SETTINGS 611L
#define OCAP_VK_NEXT_FAVORITE_CHANNEL 612L
#define OCAP_VK_RESERVE_1 613L
#define OCAP_VK_RESERVE_2 614L
#define OCAP_VK_RESERVE_3 615L
#define OCAP_VK_RESERVE_4 616L
#define OCAP_VK_RESERVE_5 617L
#define OCAP_VK_RESERVE_6 618L
#define OCAP_VK_LOCK 619L
#define OCAP_VK_SKIP 620L
#define OCAP_VK_LIST 621L
#define OCAP_VK_LIVE 622L
#define OCAP_VK_ON_DEMAND 623L
#define OCAP_VK_PINP_MOVE 624L
#define OCAP_VK_PINP_UP 625L
#define OCAP_VK_PINP_DOWN 626L
#define OCAP_VK_INSTANT_REPLAY 627L
#define OCAP_OCRC_LAST 627L

/** Indicates that MPE cannot generate event Unicode characters. */
#define OCAP_CHAR_UNKNOWN -1L

#endif
