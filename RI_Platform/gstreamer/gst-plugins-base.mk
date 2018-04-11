
#
# GStreamer base plugings configure arguments
#
GST_BASE_CONFIGURE = ./configure \
	CC=$(RI_CC) CXX=$(RI_CXX) \
	NM=nm \
	--with-pkg-config-path=$(PLATFORM_PKGCFG) \
	CPPFLAGS=$(GST_BASE_CPPFLAGS) \
	LDFLAGS=$(GST_BASE_LDFLAGS) \
	--build=$(RI_BUILD) --host=$(RI_HOST) \
	--prefix=$(PLATFORM_INST_DIR) \
	$(GST_BASE_CONFIG_FLAGS) \
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
	--disable-largefile \
	--disable-gtk-doc \
	--disable-adder \
	--enable-app \
	--disable-audioconvert \
	--disable-audiorate \
	--disable-audiotestsrc \
	--enable-ffmpegcolorspace \
	--disable-gdp \
	--disable-playback \
	--disable-speexresample \
	--disable-subparse \
	--enable-tcp \
	--disable-typefind \
	--enable-videotestsrc \
	--disable-videorate \
	--disable-videoscale \
	--disable-volume \
	--disable-x \
	--disable-xvideo \
	--disable-xshm \
	--disable-gst_v4l \
	--disable-alsa \
	--disable-cdparanoia \
	--disable-gnome_vfs \
	--disable-gio \
	--disable-libvisual \
	--disable-ogg \
	--disable-oggtest \
	--disable-pango \
	--disable-theora \
	--disable-vorbis \
	--disable-vorbistest \
	--disable-introspection \
	--disable-freetypetest
