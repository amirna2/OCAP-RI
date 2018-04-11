
#ifndef __H_MOCKDLL
#define __H_MOCKDLL

#define MPE_SUCCESS         0


int dtcpip_cmn_init(const char* storage_path);
void dtcpip_cmn_get_version(char* string, size_t length);

int dtcpip_src_init(unsigned short dtcp_port);
int dtcpip_src_open(int* session_handle, int is_audio_only);
int dtcpip_src_alloc_encrypt(int session_handle, unsigned char cci,
                 char* cleartext_data, size_t cleartext_size,
                 char** encrypted_data, size_t* encrypted_size);
int dtcpip_src_free(char* encrypted_data);
int dtcpip_src_close(int session_handle);

int dtcpip_snk_init(void);
int dtcpip_snk_open(char* ip_addr, unsigned short ip_port, int *session_handle);
int dtcpip_snk_alloc_decrypt(int session_handle, char* encrypted_data, size_t encrypted_size,
                 char** cleartext_data, size_t* cleartext_size);
int dtcpip_snk_free(char* cleartext_data);
int dtcpip_snk_close(int session_handle);


#endif
