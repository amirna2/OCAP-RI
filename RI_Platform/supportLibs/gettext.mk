
#
# gettext configure arguments
#
ifeq ($(GETTEXT_CONF_CACHE_FILE),)
	GETTEXT_CACHE_FILE_ARG =
else
	GETTEXT_CACHE_FILE_ARG = --cache-file=$(GETTEXT_CONF_CACHE_FILE)
endif
GETTEXT_CONFIGURE = ./configure \
	CC=$(RI_CC) CXX=$(RI_CXX) \
	--build=$(RI_BUILD) --host=$(RI_HOST) \
	$(GETTEXT_CACHE_FILE_ARG) \
	--disable-java --disable-native-java \
	--with-included-gettext \
	--with-included-glib \
	--with-included-libxml \
	--without-emacs \
	--prefix=$(PLATFORM_INST_DIR)

