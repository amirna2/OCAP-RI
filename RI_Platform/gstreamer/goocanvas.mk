
#
# goocanvas configure arguments
#
GOOCANVAS_CONFIGURE = ./configure \
	CC=$(RI_CC) CXX=$(RI_CXX) \
	--with-pkg-config-path=$(PLATFORM_PKGCFG) \
	--build=$(RI_BUILD) --host=$(RI_HOST) \
	--prefix=$(PLATFORM_INST_DIR) \

