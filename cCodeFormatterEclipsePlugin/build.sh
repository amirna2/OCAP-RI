#! /bin/bash

# COPYRIGHT_BEGIN
#  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
#  
#  Copyright (C) 2008-2013, Cable Television Laboratories, Inc. 
#  
#  This software is available under multiple licenses: 
#  
#  (1) BSD 2-clause 
#   Redistribution and use in source and binary forms, with or without modification, are
#   permitted provided that the following conditions are met:
#        ·Redistributions of source code must retain the above copyright notice, this list 
#             of conditions and the following disclaimer.
#        ·Redistributions in binary form must reproduce the above copyright notice, this list of conditions 
#             and the following disclaimer in the documentation and/or other materials provided with the 
#             distribution.
#   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
#   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED 
#   TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A 
#   PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT 
#   HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
#   SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
#   LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
#   DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
#   THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
#   (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF 
#   THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#  
#  (2) GPL Version 2
#   This program is free software; you can redistribute it and/or modify
#   it under the terms of the GNU General Public License as published by
#   the Free Software Foundation, version 2. This program is distributed
#   in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
#   even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
#   PURPOSE. See the GNU General Public License for more details.
#  
#   You should have received a copy of the GNU General Public License along
#   with this program.If not, see<http:www.gnu.org/licenses/>.
#  
#  (3)CableLabs License
#   If you or the company you represent has a separate agreement with CableLabs
#   concerning the use of this code, your rights and obligations with respect
#   to this code shall be as set forth therein. No license is granted hereunder
#   for any other purpose.
#  
#   Please contact CableLabs if you need additional information or 
#   have any questions.
#  
#       CableLabs
#       858 Coal Creek Cir
#       Louisville, CO 80027-9750
#       303 661-9100
# COPYRIGHT_END

##############################################################################
#
# This script builds and installs the Eclipse C code formatter application
# plugin.
#
# This script runs under Cygwin. It takes no arguments. It must be run from
# the directory containing the plugin "src" directory; that is, the directory
# containing the script itself. Eclipse should not be running when this
# script is run.
#
# The environment variable ECLIPSE_HOME may be set to define the root
# directory of the Eclipse installation. If it is not set, the path
# "C:/Program Files/eclipse" will be used.
#
# The following Eclipse plugins must be installed prior to running this
# script:
#
#    cdt.core
#    core.runtime
#    equinox.app
#    equinox.common
#    osgi
#    text
#
# The functions in this script were ordered alphabetically.
#
# exit code 1: The runtime environment is incorrect.
# exit code 2: The Eclipse installation could not be validated.
# exit code 3: The script is not being run from the correct directory.
# exit code 4: A required Eclipse plugin could not be found.
#
##############################################################################

# TODO: need to require 5.1.2 of cdt.core, or adapt manifest
# TODO: use date instead of yyyymmddHHMM; delete other versions on install
# TODO: findPlugin fails if more than one version of the target plugin exists
# TODO: make a Linux version?
# TODO: detect if Eclipse is running?
# TODO: if ECLIPSE_HOME comes in from the environment, we don't know it's in
#       DOS syntax

##############################################################################
#
# function buildClassPath
#
# Define a global variable as follows:
#
#    $cp The class path required to build the plugin.
#
##############################################################################

function buildClassPath
{
    cp=.

    for i in $requiredPlugins
    do
        findPlugin $i
        cp="$cp;$plugin"
    done
}

##############################################################################
#
# function compilePlugin
#
# Compile the plugin.
#
##############################################################################

function compilePlugin
{
    cd src
        javac -d ../dst -cp "$cp" $pluginSourceFile
    cd ..
}

##############################################################################
#
# function findPlugin
#
# Find an Eclipse plugin. Define a global variable as follows:
#
#    $plugin The path name of the plugin.
#
# args: The name of the plugin.
#
# exit code 4: The plugin could not be found.
#
##############################################################################

function findPlugin
{
    plugin=$(echo "$ECLIPSE_HOME"/plugins/org.eclipse.$1_*.jar)

    if [ ! \( -f "$plugin" -a -r "$plugin" \) ]
    then
        echo "  The Eclipse plugin '$1' does not exist or is not readable."
        exit 4
    fi
}

##############################################################################
#
# function initializeConstants
#
# Define global variables as follows:
#
#    $ECLIPSE_HOME     The Eclipse root directory (if not defined in the
#                      environment).
#    $requiredPlugins  The names of Eclipse plugins required to build this
#                      plugin.
#    $pluginSourceFile The path name of the plugin source file.
#    $pluginJarFile    The name of the plugin jar file.
#
##############################################################################

function initializeConstants
{
    if [ "$ECLIPSE_HOME" = "" ]
    then
        ECLIPSE_HOME="C:/Program Files/eclipse"
        echo "    The environment variable ECLIPSE_HOME is not defined; using '$ECLIPSE_HOME'."
    fi

    requiredPlugins="cdt.core equinox.app text osgi equinox.common core.runtime"
    pluginSourceFile=org/cablelabs/ccodeformatter/Application.java
    pluginJarFile=org.cablelabs.ccodeformatter_1.0.0.yyyymmddHHMM.jar
}

##############################################################################
#
# function initializeDestinationDirectory
#
# Initialize the destination directory with all except the plugin class files.
#
##############################################################################

function initializeDestinationDirectory
{
    rm -rf dst
    cp -r src dst
    find dst -name \*.java -exec rm {} \;
}

##############################################################################
#
# function installPlugin
#
# Install the plugin.
#
##############################################################################

function installPlugin
{
    local p="$ECLIPSE_HOME"/plugins/$pluginJarFile

    rm -f "$p"
    cp $pluginJarFile "$p"
    chmod -x "$p"
}

##############################################################################
#
# function main
#
# Do the actual work of the script. (See the comment at the top of the
# script.)
#
# exit code 1: The runtime environment is incorrect.
# exit code 2: The Eclipse installation could not be validated.
# exit code 3: The script is not being run from the correct directory.
# exit code 4: A required Eclipse plugin could not be found.
#
##############################################################################

function main
{
    validateCygwin

    echo "  Initializing ..."

    initializeConstants
    validateEclipseInstallation
    validateWorkingDirectory

    buildClassPath

    initializeDestinationDirectory

    echo "  Building plugin ..."
    compilePlugin
    makePluginJar

    installPlugin
    echo "  Plugin installed."
}

##############################################################################
#
# function makePluginJar
#
# Make the plugin jar file.
#
##############################################################################

function makePluginJar
{
    cd dst
        jar cMf ../$pluginJarFile *
    cd ..
}

##############################################################################
#
# function validateCygwin
#
# Validate the runtime environment.
#
# exit code 1: The runtime environment is incorrect.
#
##############################################################################

function validateCygwin
{
    local uname=$(uname)

    if [ ${#uname} -lt 6 -o "${uname:0:6}" != "CYGWIN" ]
    then
        echo "Please run me under Cygwin."
        exit 1
    fi
}

##############################################################################
#
# function validateEclipseInstallation
#
# Validate the Eclipse installation.
#
# exit code 2: The Eclipse installation could not be validated.
#
##############################################################################

function validateEclipseInstallation
{
    if [ ! \( -d "$ECLIPSE_HOME"/plugins -a -w "$ECLIPSE_HOME"/plugins -a -x "$ECLIPSE_HOME"/plugins \) ]
    then
        echo "  ECLIPSE_HOME ('$ECLIPSE_HOME') is supposed to refer to the Eclipse installation directory."
        echo "  The directory '$ECLIPSE_HOME/plugins' does not exist or is not writeable or searchable."
        exit 2
    fi
}

##############################################################################
#
# function validateWorkingDirectory
#
# Validate the working directory.
#
# exit code 3: The script is not being run from the correct directory.
#
##############################################################################

function validateWorkingDirectory
{
    if [ ! \( -f src/$pluginSourceFile -a -r src/$pluginSourceFile \) ]
    then
        echo "  The file 'src/$pluginSourceFile' does not exist or is not readable."
        exit 3
    fi
}

##############################################################################
#
# The main program.
#
# Invoke function main.
#
# exit code 1: The runtime environment is incorrect.
# exit code 2: The Eclipse installation could not be validated.
# exit code 3: The script is not being run from the correct directory.
# exit code 4: A required Eclipse plugin could not be found.
#
##############################################################################

main
