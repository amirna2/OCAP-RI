

package org.cablelabs.xlet.HnNetConfigState;

import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import org.apache.log4j.Logger;

import org.ocap.diagnostics.*;

public class HnNetConfigState implements Xlet
{
    MIBManager mibm = null;
    XletContext m_ctx = null;
    private static final Logger m_log = Logger.getLogger(HnNetConfigState.class);

    // @Override
    public void initXlet(XletContext ctx) throws XletStateChangeException
    {
        m_log.info(".initXlet()");
        m_ctx = ctx;

        if (null == (mibm = MIBManager.getInstance()))
        {
            throw new XletStateChangeException("No MIBManager Found");
        }
    }

    // @Override
    public void startXlet() throws XletStateChangeException
    {
        m_log.info(".startXlet()");
        String ocHnNetConfigViewPrimaryOutputPort_oid =
            "1.3.6.1.4.1.4491.2.3.2.2.5.1.0";

        String ocHnNetConfigViewPrimaryOutputPortOrgID_oid =
            "1.3.6.1.4.1.4491.2.3.2.2.5.2.0";
     
        String ocHnNetConfigPersistentLinkLocalAddress_oid =
            "1.3.6.1.4.1.4491.2.3.2.2.5.3.0";

        int vpop = getMIBIntValue(ocHnNetConfigViewPrimaryOutputPort_oid);
        int oldVpop = vpop;
        m_log.info("VPOP state: " + (vpop == 1? "true" : "false"));
        // Set it to the opposite value 1=true, 2=false(default)
        if (vpop == 1)
        {
            vpop = 2;
        }
        else
        {
            vpop = 1;
        }
        setMIBIntValue(ocHnNetConfigViewPrimaryOutputPort_oid, vpop);
        vpop = 0; //just to make sure we set vpop to the new getMIBIntValue
        vpop = getMIBIntValue(ocHnNetConfigViewPrimaryOutputPort_oid);
        m_log.info("new VPOP state: " + (vpop == 1? "true" : "false"));
        if (vpop == oldVpop)
        {
            m_log.error("VPOP state set failed");
        }
        else
        {
            m_log.info("VPOP state set passed");
        } 

        long vpopOrgID = getMIBLongValue(ocHnNetConfigViewPrimaryOutputPortOrgID_oid);
        long oldVpopOrgID = vpopOrgID;
        m_log.info("VPOP OrgID: " + vpopOrgID);
        // setting the VPOP Org ID to a very big number.
        setMIBLongValue(ocHnNetConfigViewPrimaryOutputPortOrgID_oid, 99999999);
        vpopOrgID = getMIBLongValue(ocHnNetConfigViewPrimaryOutputPortOrgID_oid);
        m_log.info("new VPOP OrgID: " + vpopOrgID);
        if (vpopOrgID == oldVpopOrgID)
        {
            m_log.error("VPOP orgID set failed");
        }
        else
        {
            m_log.info("VPOP orgID set passed");
        }

        // For persistent Link local addressing
        int persistentLL = getMIBIntValue(ocHnNetConfigPersistentLinkLocalAddress_oid);
        int oldPersistentLL = persistentLL;
        m_log.info("Persistent Link local addressing value: " + (persistentLL == 1? "true" : "false"));
        // Set it to the opposite value 1=true, 2=false(default)
        if (persistentLL == 1)
        {
            persistentLL = 2;
        }
        else
        {
            persistentLL = 1;
        }
        setMIBIntValue(ocHnNetConfigPersistentLinkLocalAddress_oid, persistentLL);
        persistentLL = 0; //just to make sure we set vpop to the new getMIBIntValue
        persistentLL = getMIBIntValue(ocHnNetConfigPersistentLinkLocalAddress_oid);
        m_log.info("new Persistent Link local addressing value: " + (persistentLL == 1? "true" : "false"));
        if (persistentLL == oldPersistentLL)
        {
            m_log.error("Persistent link local addressing value set failed");
        }
        else
        {
            m_log.info("Persistent link local addressing value set passed");
        } 

        m_log.info("done.");
    }

    // @Override
    public void destroyXlet(boolean unconditional) throws XletStateChangeException
    {
        // TODO Auto-generated method stub
    }

    // @Override
    public void pauseXlet()
    {
        // TODO Auto-generated method stub
    }

    private int getMIBIntValue(String oid)
    {
        int retVal = 0;
        MIBDefinition[] mibd = mibm.queryMibs(oid);

        if (mibd == null)
        {
            m_log.info("getMIBIntValue null returned from queryMibs");
        }
        else if (mibd.length != 1)
        {
            m_log.info("getMIBIntValue queryMibs bad array length: " +
                               mibd.length);
        }
        else if (mibd[0].getDataType() != MIBDefinition.SNMP_TYPE_INTEGER)
        {
            m_log.info("getMIBIntValue queryMibs bad dataType: " +
                               mibd[0].getDataType());
        }
        else
        {
            MIBObject mibo = mibd[0].getMIBObject();

            if (mibo == null)
            {
                m_log.info("getMIBIntValue getMIBObject returned null");
            }
            else
            {
                byte[] mibObjBytes = mibo.getData();
                int tag = (mibObjBytes[0] & 0x000000FF);
                int len = (mibObjBytes[1] & 0x000000FF);
                int val = 0;

                for (int x = 0; x < len; x++)
                {
                    val <<= 8;  // prepare to add in next byte
                    val += (mibObjBytes[2+x] & 0x000000FF);
                }

                retVal = val;
            }
        }

        return retVal;
    }

    private long getMIBLongValue(String oid)
    {
        long retVal = 0;
        MIBDefinition[] mibd = mibm.queryMibs(oid);

        if (mibd == null)
        {
            m_log.info("getMIBLongValue null returned from queryMibs");
        }
        else if (mibd.length != 1)
        {
            m_log.info("getMIBLongValue queryMibs bad array length: " +
                               mibd.length);
        }
        else if (mibd[0].getDataType() != MIBDefinition.SNMP_TYPE_GAUGE32)
        {
            m_log.info("getMIBLongValue queryMibs bad dataType: " +
                               mibd[0].getDataType());
        }
        else
        {
            MIBObject mibo = mibd[0].getMIBObject();

            if (mibo == null)
            {
                m_log.info("getMIBLongValue getMIBObject returned null");
            }
            else
            {
                byte[] mibObjBytes = mibo.getData();
                int tag = (mibObjBytes[0] & 0x000000FF);
                int len = (mibObjBytes[1] & 0x000000FF);
                long val = 0;

                for (int x = 0; x < len; x++)
                {
                    val <<= 8;  // prepare to add in next byte
                    val += (mibObjBytes[2+x] & 0x000000FF);
                }

                retVal = val;
            }
        }

        return retVal;
    }
 
    private boolean setMIBIntValue(String oid, int val)
    {
        boolean retVal = false;
        MIBDefinition[] mibd = mibm.queryMibs(oid);

        if (mibd == null)
        {
            m_log.info("setMIBIntValue null returned from queryMibs");
        }
        else if (mibd.length != 1)
        {
            m_log.info("setMIBIntValue queryMibs bad array length: " +
                               mibd.length);
        }
        else if (mibd[0].getDataType() != MIBDefinition.SNMP_TYPE_INTEGER)
        {
            m_log.info("setMIBIntValue queryMibs bad dataType: " +
                               mibd[0].getDataType());
        }
        else
        {
            // BER encode integer result
            byte[] mibObjBytes = new byte[6];
            byte x = 0;
            int offset = 24;

            for(x = 0; x < 4; x++)
            {
                mibObjBytes[2+x] = (byte)((val >>> offset) & 0xFF);
                offset -= 8;
            }

            mibObjBytes[0] = MIBDefinition.SNMP_TYPE_INTEGER;
            mibObjBytes[1] = x;
            MIBObject mibo = new MIBObject(oid, mibObjBytes);
            mibm.setMIBObject(mibo);
            retVal = true;
        }

        return retVal;
    }

    private boolean setMIBLongValue(String oid, long val)
    {
        boolean retVal = false;
        MIBDefinition[] mibd = mibm.queryMibs(oid);

        if (mibd == null)
        {
            m_log.info("setMIBLongValue null returned from queryMibs");
        }
        else if (mibd.length != 1)
        {
            m_log.info("setMIBLongValue queryMibs bad array length: " +
                               mibd.length);
        }
        else if (mibd[0].getDataType() != MIBDefinition.SNMP_TYPE_GAUGE32)
        {
            m_log.info("setMIBLongValue queryMibs bad dataType: " +
                               mibd[0].getDataType());
        }
        else
        {
            // BER encode integer result
            byte[] mibObjBytes = new byte[6];
            byte x = 0;
            int offset = 24;

            for(x = 0; x < 4; x++)
            {
                mibObjBytes[2+x] = (byte)((val >>> offset) & 0xFF);
                offset -= 8;
            }

            mibObjBytes[0] = MIBDefinition.SNMP_TYPE_GAUGE32;
            mibObjBytes[1] = x;
            MIBObject mibo = new MIBObject(oid, mibObjBytes);
            mibm.setMIBObject(mibo);
            retVal = true;
        }

        return retVal;
    }

}
