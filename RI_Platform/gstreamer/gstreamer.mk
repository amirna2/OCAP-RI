
#
# GStreamer configure arguments
#
GST_CONFIGURE = ./configure \
	--enable-option-checking \
	--disable-silent-rules \
	--enable-shared \
	--disable-static \
	--enable-fast-install \
	--enable-libtool-lock \
	--disable-rpath \
	--disable-loadsave \
	--disable-trace \
	--disable-registry \
	--disable-debug \
	--disable-profiling \
	--disable-valgrind \
	--disable-gcov \
	--disable-examples \
	--disable-tests \
	--disable-introspection \
	--enable-check \
	--with-pkg-config-path=$(PLATFORM_PKGCFG) \
	CPPFLAGS=$(GST_CPPFLAGS) \
	LDFLAGS=$(GST_LDFLAGS) \
	CC=$(RI_CC) CXX=$(RI_CXX) \
	NM=nm \
	$(GST_CONFIG_FLAGS) \
	--build=$(RI_BUILD) --host=$(RI_HOST) \
	--prefix=$(PLATFORM_INST_DIR)
