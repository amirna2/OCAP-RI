/*Copyright (c) 2004-2006, Satoshi Konno Copyright (c) 2005-2006, 
Nokia Corporation Copyright (c) 2005-2006, Theo Beisch Collectively 
the Copyright Owners All rights reserved
 */
package org.cablelabs.impl.ocap.hn.content;

import java.util.Vector;

import org.apache.log4j.Logger;
import org.cablelabs.impl.ocap.hn.content.navigation.ContentListImpl;
import org.cablelabs.impl.ocap.hn.recording.NetRecordingEntryImpl;
import org.cablelabs.impl.ocap.hn.recording.RecordingContentItemImpl;
import org.cablelabs.impl.ocap.hn.upnp.UPnPConstants;
import org.cablelabs.impl.ocap.hn.util.xml.miniDom.MiniDomParser;
import org.cablelabs.impl.ocap.hn.util.xml.miniDom.Node;
import org.cablelabs.impl.ocap.hn.util.xml.miniDom.NodeList;
import org.cablelabs.impl.ocap.hn.util.xml.miniDom.QualifiedName;
import org.ocap.hn.content.ContentItem;
import org.ocap.hn.upnp.common.UPnPAction;
import org.ocap.hn.upnp.common.UPnPActionResponse;

/**
 * Creates OCAP HN Content based on Containers and Items persisted in a UPnP
 * Content Directory Service.
 * 
 * @author Michael A. Jastad
 * @version $Revision$
 * 
 * @see
 */
public class ContentFactory
{
    /** contentDatabase reference */
    private static ContentFactory m_contentFactory = null;

    private final int UNKNOWN = -1;

    private final int CONTENT_CONTAINER = 0x0;

    private final int NET_RECORDING_ENTRY = 0x01;

    private final int CONTENT_ITEM = 0x02;

    private final int RECORDING_CONTENT_ITEM = 0x03;
    
    private final int CHANNEL_CONTENT_ITEM = 0x04;

    /** Action Result key flag */
    private static final String RESULT = "Result";
    
    /** Log4J logging facility. */
    private static final Logger log = Logger.getLogger(ContentFactory.class);

    /**
     * Creates a new ContentFactory object.
     */
    private ContentFactory()
    {
    }

    /**
     * Creates and returns a single ContentEntry
     * 
     * @param action
     *            The action that originated the request.
     * @param node
     *            The xml node containing the content metadata
     * 
     * @return CDSContentEntryImpl
     */
    public ContentEntryImpl getContentEntry(UPnPAction action, Node node)
    {
        ContentEntryImpl contentEntry = null;

        switch (contentType(node))
        {
            case CONTENT_ITEM:
                contentEntry = new ContentItemImpl(action, new MetadataNodeImpl(node));
                break;

            case CONTENT_CONTAINER:
                contentEntry = new ContentContainerImpl(action, new MetadataNodeImpl(node));
                break;

            case RECORDING_CONTENT_ITEM:
                contentEntry = new RecordingContentItemImpl(action, new MetadataNodeImpl(node));
                break;

            case NET_RECORDING_ENTRY:
                contentEntry = new NetRecordingEntryImpl(action, new MetadataNodeImpl(node));
                break;

            case CHANNEL_CONTENT_ITEM:
                contentEntry = new ChannelContentItemImpl(action, new MetadataNodeImpl(node));
                break;

            default:
                if (log.isWarnEnabled())
                {
                    log.warn("Can't find content type = " + contentType(node));
                }
        }

        return contentEntry;
    }

    /**
     * Creates and returns a list of ContentEntries.
     * 
     * @param action
     *            The action that originated the request.
     * 
     * @return ContentListImpl
     */
    public ContentListImpl getContentEntries(UPnPActionResponse response)
    {
        ContentListImpl cList = new ContentListImpl();

        if (response != null)
        {
            String result = response.getArgumentValue(RESULT);
            
            // Create a list of entries returned to use to create content entries
            if (result != null)
            {
                Vector nodes = getNodes(result);

                for (int i = 0; i < nodes.size(); ++i)
                {
                    cList.addContentEntry(getContentEntry(response.getActionInvocation().getAction(), 
                            (Node)nodes.get(i)));
                }

                /* Make sure entries are linked to parents in this list */
                linkContentParents(cList);
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("getContentEntries() - result was null");
                }
            }
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("*** action can't be null - abort.");
            }
        }

        return cList;
    }

    /**
     * Retrieves list of items from xml result string.
     * 
     * @param xml   xml string which was result from browse or search action.
     * 
     * @return  vector containing item nodes
     */
    private Vector getNodes(String xml)
    {
        Vector nodes = new Vector();
        Node document = MiniDomParser.parse(xml);

        if (document != null)
        {
            NodeList nodeList = document.getChildNodes();

            if (nodeList != null)
            {
                for (int i = 0, n = nodeList.getLength(); i < n; ++ i)
                {
                    Node childNode = nodeList.item(i);

                    QualifiedName name = childNode.getName();

                    if (UPnPConstants.QN_DIDL_LITE_ITEM.equals(name) || 
                            UPnPConstants.QN_DIDL_LITE_CONTAINER.equals(name))
                    {
                        nodes.add(childNode);
                    }
                }
            }
        }
        return nodes;
    }
    
    /**
     * Determines Content Type based on available Metadata.
     * 
     * @param Node
     *            node - contains data to be evaluated.
     * 
     * @return ContentType
     */
    private int contentType(Node node)
    {
        if (node == null)
        {
            if (log.isWarnEnabled())
            {
                log.warn("contentType() - null node");
            }

            return UNKNOWN;
        }

        QualifiedName contentType = node.getName();

        if (contentType.equals(UPnPConstants.QN_DIDL_LITE_CONTAINER))
        {
            return CONTENT_CONTAINER;
        }

        if (! contentType.equals(UPnPConstants.QN_DIDL_LITE_ITEM))
        {
            if (log.isWarnEnabled())
            {
                log.warn("contentType() - unknown content type '" + contentType + "'");
            }

            return UNKNOWN;
        }

        // Look at metadata to determine type of item
        boolean taskId = false;
        boolean scheduleId = false;

        NodeList nl = node.getChildNodes();

        for (int i = 0, n = nl.getLength(); i < n; i++)
        {
            QualifiedName nodeName = nl.item(i).getName();            

            
            // If upnp:class is of a Broadcast type then this is a 
            // channel content item.
            if (UPnPConstants.QN_UPNP_CLASS.equals(nodeName))
            {
                String value = null;
                
                if(nl.item(i).getFirstChild() != null)
                {
                    value = nl.item(i).getFirstChild().getValue();
                }
                
                if(ContentItem.VIDEO_ITEM_BROADCAST.equals(value) ||
                   ContentItem.VIDEO_ITEM_BROADCAST_VOD.equals(value) ||
                   ContentItem.AUDIO_ITEM_BROADCAST.equals(value))
                {
                    return CHANNEL_CONTENT_ITEM; 
                }
            }
            
            if (nodeName.equals(UPnPConstants.QN_UPNP_SRS_RECORD_TASK_ID))
            {
                taskId = true;
            }
            else if (nodeName.equals(UPnPConstants.QN_UPNP_SRS_RECORD_SCHEDULE_ID))
            {
                scheduleId = true;
            }
        }

        return taskId ? RECORDING_CONTENT_ITEM : scheduleId ? NET_RECORDING_ENTRY : CONTENT_ITEM;
    }

    /**
     * Links any entries with parent Containers in the list to their parent
     * Containers
     * 
     * @param cList
     *            ContentListImpl to link together
     */
    private void linkContentParents(ContentListImpl cList)
    {
        /* Iterate through all entries */
        for (int i = 0; i < cList.size(); ++i)
        {
            ContentEntryImpl entry = cList.getContentEntry(i);

            if (entry == null)
            {
                // This represents an error, probably an XML parsing failure.
                // Callers are responsible for dealing gracefully with the
                // pathological case of a null ContentEntry in a ContentList.
                // Here we simply avoid a NullPointerException.
                continue;
            }

            String entryParentID = entry.getParentID();

            /*
             * Look for a parent of this entry if it has one and is not root
             * container
             */
            if ((null != entryParentID) && !("-1".equals(entryParentID)))
            {
                /*
                 * Iterate through all entries to see if any are the parent of
                 * the outer entry
                 */
                for (int j = 0; j < cList.size(); ++j)
                {
                    ContentEntryImpl entryInner = cList.getContentEntry(j);

                    /* Only containers can be used */
                    if (entryInner instanceof ContentContainerImpl)
                    {
                        if (entryInner.getID().equals(entryParentID))
                        {
                            entry.setEntryParent((ContentContainerImpl) entryInner);
                            ((ContentContainerImpl) entryInner).addChild(entry);
                            break;
                        }
                    } /* endif container */
                } /* endfor inner entries */
            } /* endif valid, non-root parent ID */
        } /* endfor all entries */
    }

    /**
     * Returns a singleton factory reference.
     * 
     * @return A reference to the factory.
     */
    public static ContentFactory getInstance()
    {
        if (m_contentFactory == null)
        {
            m_contentFactory = new ContentFactory();
        }

        return m_contentFactory;
    }
}
