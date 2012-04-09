/*******************************************************************************
 * * Copyright 2012 Impetus Infotech.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 ******************************************************************************/
package com.impetus.kundera.lifecycle.states;


import java.util.List;
import java.util.Map;

import javax.persistence.CascadeType;

import com.impetus.kundera.client.Client;
import com.impetus.kundera.graph.Node;
import com.impetus.kundera.graph.NodeLink;
import com.impetus.kundera.graph.ObjectGraphBuilder;
import com.impetus.kundera.graph.NodeLink.LinkProperty;
import com.impetus.kundera.lifecycle.NodeStateContext;
import com.impetus.kundera.metadata.KunderaMetadataManager;
import com.impetus.kundera.metadata.model.EntityMetadata;
import com.impetus.kundera.persistence.context.PersistenceCache;

/**
 * @author amresh
 *
 */
public class RemovedState extends NodeState
{
    @Override
    public void initialize(NodeStateContext nodeStateContext)
    {
    }

    @Override
    public void handlePersist(NodeStateContext nodeStateContext)
    {
        nodeStateContext.setCurrentNodeState(new ManagedState());
        //TODO: Recurse persist operation on all related entities for whom cascade=ALL or PERSIST
    }    

    @Override
    public void handleRemove(NodeStateContext nodeStateContext)
    {
        //Ignored, entity will remain in removed state
        //TODO: Recurse remove operation for all related entities for whom cascade=ALL or REMOVE
    }

    @Override
    public void handleRefresh(NodeStateContext nodeStateContext)
    {
      //Ignored, entity will remain in removed state
      //TODO: Cascade refresh operation for all related entities for whom cascade=ALL or REFRESH
    }

    @Override
    public void handleMerge(NodeStateContext nodeStateContext)
    {
        throw new IllegalArgumentException("Merge operation not allowed in Removed state");
    }
    
    @Override
    public void handleFind(NodeStateContext nodeStateContext)
    {
    }

    @Override
    public void handleClose(NodeStateContext nodeStateContext)
    {
      //Nothing to do, only entities in Managed state move to detached state
    }

    @Override
    public void handleClear(NodeStateContext nodeStateContext)
    {
      //Nothing to do, only entities in Managed state move to detached state
    }

    @Override
    public void handleFlush(NodeStateContext nodeStateContext)
    {
        //Entity state to remain as Removed     
        
        //Flush this node to database       
        Client client = nodeStateContext.getClient();
        
        Node node = (Node)nodeStateContext;
        String entityId = ObjectGraphBuilder.getEntityId(node.getNodeId());
        client.delete(node.getData(), entityId);
        
        //Since node is flushed, mark it as NOT dirty
        nodeStateContext.setDirty(false);
        
        //Remove this node from Persistence Cache
        PersistenceCache.INSTANCE.getMainCache().removeNodeFromCache(node);
        
        logNodeEvent("DELETED", this, nodeStateContext.getNodeId());        
        
    }

    @Override
    public void handleLock(NodeStateContext nodeStateContext)
    {
    }

    @Override
    public void handleDetach(NodeStateContext nodeStateContext)
    {
        //Removed ---> Detached
        NodeState nextState = new DetachedState();
        nodeStateContext.setCurrentNodeState(nextState);
        logStateChangeEvent(this, nextState, nodeStateContext.getNodeId());        
        
        //Cascade detach operation to all referenced entities for whom cascade=ALL or DETACH
        Map<NodeLink, Node> children = nodeStateContext.getChildren();
        if(children != null) {
            for(NodeLink nodeLink : children.keySet()) {
                List<CascadeType> cascadeTypes = (List<CascadeType>) nodeLink.getLinkProperty(LinkProperty.CASCADE);
                if(cascadeTypes.contains(CascadeType.DETACH) || cascadeTypes.contains(CascadeType.ALL)) {
                    Node childNode = children.get(nodeLink);                
                    childNode.detach();
                }
            }
        }  
    }

    @Override
    public void handleCommit(NodeStateContext nodeStateContext)
    {
        nodeStateContext.setCurrentNodeState(new TransientState());
    }

    @Override
    public void handleRollback(NodeStateContext nodeStateContext)
    {
        //If Persistence Context is EXTENDED
        nodeStateContext.setCurrentNodeState(new ManagedState());
        
        //If Persistence Context is TRANSACTIONAL
        //nodeStateContext.detach();
    }

    @Override
    public void handleGetReference(NodeStateContext nodeStateContext)
    {
    }

    @Override
    public void handleContains(NodeStateContext nodeStateContext)
    {
    }   
    
    
}
