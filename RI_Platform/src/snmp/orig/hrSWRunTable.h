/*
 * Note: this file originally auto-generated by mib2c using
 *  : mib2c.create-dataset.conf 9375 2004-02-02 19:06:54Z rstory $
 */
#ifndef HRSWRUNTABLE_H
#define HRSWRUNTABLE_H

/* function declarations */
void init_hrSWRunTable(void);
void initialize_table_hrSWRunTable(void);
Netsnmp_Node_Handler hrSWRunTable_handler;

/* column number definitions for table hrSWRunTable */
       #define COLUMN_HRSWRUNINDEX		1
       #define COLUMN_HRSWRUNNAME		2
       #define COLUMN_HRSWRUNID		3
       #define COLUMN_HRSWRUNPATH		4
       #define COLUMN_HRSWRUNPARAMETERS		5
       #define COLUMN_HRSWRUNTYPE		6
       #define COLUMN_HRSWRUNSTATUS		7
#endif /* HRSWRUNTABLE_H */