
#
# gst-editor configure arguments
#
GST_EDITOR_CONFIGURE = ./configure \
	CC=$(RI_CC) CXX=$(RI_CXX) \
	--with-pkg-config-path=$(PLATFORM_PKGCFG) \
	--build=$(RI_BUILD) --host=$(RI_HOST) \
	--prefix=$(PLATFORM_INST_DIR) \

GST4EDITOR_CONFIGURE = ./configure \
	--without-check --disable-valgrind \
	--with-pkg-config-path=$(PLATFORM_PKGCFG) \
	CPPFLAGS=$(GST_CPPFLAGS) \
	LDFLAGS=$(GST_LDFLAGS) \
	CC=$(RI_CC) CXX=$(RI_CXX) \
	NM=nm \
	--build=$(RI_BUILD) --host=$(RI_HOST) \
	--prefix=$(PLATFORM_INST_DIR)

