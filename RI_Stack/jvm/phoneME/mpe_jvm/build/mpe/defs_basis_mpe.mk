
# setup mpe tools, includes, and libs
#include ../share/defs_qt.mk

TOOLKIT_CLASS = java.awt.MPEToolkit

#
# MPE native directories
#
PROFILE_SRCDIRS_NATIVE += \
        $(CVM_TARGETROOT)/awt/native
PROFILE_INCLUDE_DIRS += \
        $(CVM_SHAREROOT)/awt/include
#
# MPE shared class directories
#
PROFILE_SRCDIRS += \
        $(CVM_TARGETROOT)/awt/classes

CLASSLIB_CLASSES += \
    java.awt.MPEARGBGraphicsConfiguration \
    java.awt.MPEDefaultGraphicsConfiguration \
    java.awt.MPEFontMetrics \
    java.awt.MPEGraphics \
    java.awt.MPEGraphicsConfiguration \
    java.awt.MPEGraphicsDevice \
    java.awt.MPEGraphicsEnvironment \
    java.awt.MPEImage \
    java.awt.MPEOffscreenImage \
    java.awt.MPESubimage \
    java.awt.MPESurface \
    java.awt.MPEToolkit \
    org.cablelabs.impl.awt.EventDispatchable \
    org.cablelabs.impl.awt.EventDispatcher \
    org.cablelabs.impl.awt.GraphicsAdaptable \
    org.cablelabs.impl.awt.GraphicsFactory \
    org.cablelabs.impl.awt.KeyboardFocusManager \
    org.cablelabs.impl.awt.KeyboardFocusManagerFactory \
    org.cablelabs.impl.awt.ResizableFrame \
    org.cablelabs.impl.dvb.ui.MPEFontFactoryPeer

AWT_LIB_OBJS += \
    MPEFontFactoryPeer.o \
    MPEDefaultGraphicsConfiguration.o \
    MPEFontMetrics.o \
    MPEGraphics.o \
    MPEGraphicsConfiguration.o \
    MPEGraphicsDevice.o \
    MPEGraphicsEnv.o \
    MPEImage.o \
    ResizableFrame.o \
	Window.o

AWT_LIB_LIBS += 
