package org.cablelabs.impl.snmp.agentx.util;

import org.cablelabs.impl.snmp.agentx.types.util.AgentXVarbindList;

public class AgentXTransaction
{
    int myTransactionId;
    
    private AgentXVarbindList myCommitData;
    private AgentXVarbindList myUndoData;
    
    /**
     * Create a new AgentXSessionState with default data set
     */
    public AgentXTransaction(long transactionId)
    {
        myTransactionId = (int)transactionId;
    }
    
    
    public int getTransactionId()
    {
        return myTransactionId;
    }
    
    /**
     * Get the AgentXVarbindList containing the OIDs and data for a CommitSet operation
     * 
     * @return the AgentXVarbindList containing the OIDs and data for a CommitSet operation
     */
    public AgentXVarbindList getCommitData() 
    {
        return myCommitData;
    }
    
    /**
     * Get the AgentXVarbindList containing the OIDs and data for an UndoSet operation
     * 
     * @return the AgentXVarbindList containing the OIDs and data for an UndoSet operation
     */
    public AgentXVarbindList getUndoData()
    {
        return myUndoData;
    }
    
    /**
     * Set the AgentXVarbindList containing the OIDs and data for a CommitSet operation
     *  
     * @param commitData the AgentXVarbindList containing the OIDs and data for a CommitSet operation 
     */
    public void setCommitData(AgentXVarbindList commitData)
    {
        myCommitData = commitData;
    }
    
    /**
     * Set the AgentXVarbindList containing the OIDs and data for an UndoSet operation
     *  
     * @param commitData the AgentXVarbindList containing the OIDs and data for an UndoSet operation 
     */
    public void setUndoData(AgentXVarbindList undoData)
    {
        myUndoData = undoData;
    }
}
