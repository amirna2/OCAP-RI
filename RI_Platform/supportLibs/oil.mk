
#
# oil configure arguments
#
OIL_CONFIGURE = ./configure \
	CC=$(RI_CC) CXX=$(RI_CXX) \
	--build=$(RI_BUILD) --host=$(RI_HOST) \
	--prefix=$(PLATFORM_INST_DIR) \
	--disable-glib


