/*
 * Note: this file originally auto-generated by mib2c using
 *        : mib2c.scalar.conf 11805 2005-01-07 09:37:18Z dts12 $
 */
#ifndef SYSTEM_H
#define SYSTEM_H

/* function declarations */
void init_system(void);
Netsnmp_Node_Handler handle_sysDescr;
Netsnmp_Node_Handler handle_sysObjectID;
Netsnmp_Node_Handler handle_sysUpTime;
Netsnmp_Node_Handler handle_sysContact;
Netsnmp_Node_Handler handle_sysName;
Netsnmp_Node_Handler handle_sysLocation;
Netsnmp_Node_Handler handle_sysServices;
Netsnmp_Node_Handler handle_sysORLastChange;

#endif /* SYSTEM_H */
