
#
# GStreamer good plugins configure arguments
#
GST_GOOD_CONFIGURE = ./configure \
	CC=$(RI_CC) CXX=$(RI_CXX) \
	NM=nm \
	--with-pkg-config-path=$(PLATFORM_PKGCFG) \
	CPPFLAGS=$(GST_GOOD_CPPFLAGS) \
	LDFLAGS=$(GST_GOOD_LDFLAGS) \
	--build=$(RI_BUILD) --host=$(RI_HOST) \
	--prefix=$(PLATFORM_INST_DIR) \
	$(GST_GOOD_CONFIG_FLAGS) \
	--disable-maintainer-mode \
	--enable-shared \
	--disable-static \
	--enable-fast-install \
	--disable-dependency-tracking \
	--disable-libtool-lock \
	--disable-rpath \
	--disable-nls \
	--disable-profiling \
	--disable-valgrind \
	--disable-gcov \
	--disable-examples \
	--disable-external \
	--disable-experimental \
	--disable-schemas-install \
	--disable-gtk-doc \
	--disable-gconftool \
	--disable-videofilter \
	--disable-alpha \
	--disable-apetag \
	--disable-audiofx \
	--disable-auparse \
	--disable-autodetect \
	--disable-avi \
	--disable-cutter \
	--disable-effectv \
	--disable-equalizer \
	--disable-id3demux \
	--disable-icydemux \
	--disable-interleave \
	--disable-flx \
	--disable-goom \
	--disable-goom2k1 \
	--disable-law \
	--disable-level \
	--disable-matroska \
	--disable-monoscope \
	--disable-multifile \
	--disable-multipart \
	--disable-replaygain \
	--enable-rtp \
	--enable-rtsp \
	--disable-smpte \
	--disable-spectrum \
	--enable-udp \
	--disable-videobox \
	--disable-videocrop \
	--disable-videomixer \
	--disable-wavenc \
	--disable-wavparse \
	--disable-directsound \
	--disable-oss \
	--disable-sunaudio \
	--disable-osx_audio \
	--disable-osx_video \
	--disable-gst_v4l2 \
	--disable-x \
	--disable-xshm \
	--disable-xvideo \
	--disable-aalib \
	--disable-aalibtest \
	--disable-annodex \
	--disable-cairo \
	--disable-esd \
	--disable-esdtest \
	--disable-flac \
	--disable-gconf \
	--disable-gdk_pixbuf \
	--disable-hal \
	--disable-jpeg \
	--disable-libcaca \
	--disable-libdv \
	--disable-libpng \
	--disable-pulse \
	--disable-dv1394 \
	--disable-shout2 \
	--disable-shout2test \
	--disable-soup \
	--disable-speex \
	--disable-taglib \
	--disable-wavpack \
	--disable-zlib

