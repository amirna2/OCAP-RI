
# Configurations for Win32/Cygwin targets
#

# The following build targets are always phony
#
.PHONY: default help build clean purge FORCE
FORCE:

#
# Common commands used throughout the build environment
#
CD     = cd 
CP     = cp
CPTREE = cp -r
CHMOD  = chmod
ECHO   = echo
MKDIR  = mkdir -p
PERL   = perl
RM     = rm -f
RMTREE = rm -rf
SED    = sed
TOUCH  = touch
XARGS  = xargs

CC  = gcc-3
CXX = g++-3

CFLAGS = $(BUILD_CFLAGS) -mno-cygwin

CXXFLAGS = $(CFLAGS)

CPPFLAGS = $(BUILD_CPPFLAGS)

LIBGEN := ar
LIBGEN_OPTS := -cr

SHARED_LIBGEN := $(CXX)
SHARED_LIBGEN_OPTS := -Wl,--kill-at -shared -mno-cygwin -Wl,--enable-auto-import

EXEGEN := $(CXX)
EXEGEN_OPTS := -mno-cygwin

extract_tar_gz = tar zxvf $1 -C $2

extract_tar_bz2 = tar jxvf $1 -C $2

apply_patch = patch -d $2 -p0 < $1 

#
# Compile C or CXX source file into an intermediate file
#
# $@ = The name of the object (.o) file to create
# $< = The name of the source (.c or .cpp) file (must be first prerequisite listed in rule)
# $1 = Additional compile options
#
define COMPILE
    @$(ECHO) Compiling $< into $@
    @$(MKDIR) $(dir $@)
    @$(RM) $@
    $(if $(filter .c,$(suffix $<)), $(CC) -c $(CPPFLAGS) $(CFLAGS) $1 $< -o $@,)
    $(if $(filter .cpp,$(suffix $<)), $(CXX) -c $(CPPFLAGS) $(CXXFLAGS) $1 $< -o $@,)
endef

#
# Build a dependency file from a C or CXX source file
#
# $@ = The name of the dependency (.d) file to create
# $< = The name of the source (.c or .cpp) file (must be first prerequisite listed in rule)
# $1 = Additional compile options
#
define BUILD_DEPENDS
    @$(ECHO) Building dependency file for $<
    @$(MKDIR) $(dir $@)
    @$(RM) $@
    $(if $(filter .c,$(suffix $<)), $(CC) -M $(CPPFLAGS) $(CFLAGS) $1 $< > $@.tmp,)
    $(if $(filter .cpp,$(suffix $<)), $(CXX) -M $(CPPFLAGS) $(CXXFLAGS) $1 $< > $@.tmp,)
    $(SED) 's,.*\.o[ :]*,$(@:.d=.o) $@ : ,g' < $@.tmp > $@
    @$(RM) $@.tmp
endef

#
# Build a library from a list of object files
#
# $@ = The name of the library (.a) file to create
# $1 = The list of all object (.o) files to put into the library
#
define BUILD_LIBRARY
    @$(ECHO) Building library $@
    @$(MKDIR) $(dir $@)
    @$(RM) $@
    @$(MKDIR) $(dir $@)
    $(LIBGEN) $(LIBGEN_OPTS) $@ $1
endef

#
# Build a shared library from a list of object files
#
# $@ = The name of the library (.dll) file to create
# $1 = The list of all object (.o) files to put into the library
#
define BUILD_SHARED_LIBRARY
    @$(ECHO) Building shared library $@
    @$(MKDIR) $(dir $@)
    @$(RM) $@
    @$(MKDIR) $(dir $@)
    $(SHARED_LIBGEN) $(SHARED_LIBGEN_OPTS) -o $@ $1
endef

#
# Build an executable from a list of object files
#
# $@ = The name of the executable (.exe) file to create
# $1 = The list of all object (.o) files to put into the library
#
define BUILD_EXECUTABLE
    @$(ECHO) Building executable $@
    @$(MKDIR) $(dir $@)
    @$(RM) $@
    @$(MKDIR) $(dir $@)
    $(EXEGEN) $(EXEGEN_OPTS) -o $@ $1
endef

#
# Remove a library
#
# $1 = The name of the library (.dll) to remove
#
define RM_LIBRARY
    @$(ECHO) Removing library $1
    @$(RM) $1
endef

#
# Convert from a platform-dependent path to a unix-style path
#
TO_UNIX_PATH = $(shell cygpath $1)

