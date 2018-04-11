#
# Topmost makefile shared by all MPE platforms
#

# On Windows host platforms we use MinGW32, but we must let the SunJVM think
# that we are in Cygwin
ifeq ($(findstring MINGW32, $(shell uname -s)), MINGW32)
HOST_OS = cygwin
endif

