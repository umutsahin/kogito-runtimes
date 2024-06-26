/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jbpm.workflow.instance;

import java.util.Collection;

import org.jbpm.workflow.core.node.AsyncEventNode;
import org.kie.api.definition.process.Node;
import org.kie.api.definition.process.NodeContainer;
import org.kie.api.definition.process.WorkflowElementIdentifier;
import org.kie.kogito.internal.process.runtime.KogitoNodeInstance;
import org.kie.kogito.internal.process.runtime.KogitoNodeInstanceContainer;

import static org.jbpm.ruleflow.core.Metadata.CUSTOM_ASYNC;

/**
 *
 */
public interface NodeInstanceContainer extends KogitoNodeInstanceContainer {

    Collection<NodeInstance> getNodeInstances(boolean recursive);

    NodeInstance getFirstNodeInstance(WorkflowElementIdentifier nodeId);

    NodeInstance getNodeInstance(Node node);

    void addNodeInstance(NodeInstance nodeInstance);

    void removeNodeInstance(NodeInstance nodeInstance);

    NodeContainer getNodeContainer();

    void nodeInstanceCompleted(NodeInstance nodeInstance, String outType);

    int getState();

    void setState(int state);

    int getLevelForNode(String uniqueID);

    int getCurrentLevel();

    void setCurrentLevel(int level);

    NodeInstance getNodeInstance(String nodeInstanceId, boolean recursive);

    default NodeInstance getByNodeDefinitionId(final String nodeDefinitionId, NodeContainer nodeContainer) {
        for (Node node : nodeContainer.getNodes()) {

            if (nodeDefinitionId.equals(node.getUniqueId())) {
                if (nodeContainer instanceof Node) {
                    Collection<KogitoNodeInstance> nodeInstances = getKogitoNodeInstances(ni -> ni.getNode().getId() == (((Node) nodeContainer).getId()), true);
                    if (nodeInstances.isEmpty()) {
                        return ((NodeInstanceContainer) getNodeInstance((Node) nodeContainer)).getNodeInstance(node);
                    } else {
                        return ((NodeInstanceContainer) nodeInstances.iterator().next()).getNodeInstance(node);
                    }
                } else {
                    return getNodeInstance(node);
                }
            }

            if (node instanceof NodeContainer) {
                NodeInstance ni = getByNodeDefinitionId(nodeDefinitionId, ((NodeContainer) node));

                if (ni != null) {
                    return ni;
                }
            }
        }

        throw new IllegalArgumentException("Node with definition id " + nodeDefinitionId + " was not found");
    }

    default Node resolveAsync(Node node) {
        // async continuation handling
        if (node instanceof AsyncEventNode) {
            return ((AsyncEventNode) node).getActualNode();
        }
        boolean asyncMode = Boolean.parseBoolean((String) node.getMetaData().get(CUSTOM_ASYNC));
        if (asyncMode) {
            return new AsyncEventNode(node);
        }
        return node;
    }
}
