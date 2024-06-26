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
package org.jbpm.ruleflow.core.factory;

import org.jbpm.process.core.timer.Timer;
import org.jbpm.ruleflow.core.RuleFlowNodeContainerFactory;
import org.jbpm.workflow.core.Node;
import org.jbpm.workflow.core.NodeContainer;
import org.jbpm.workflow.core.impl.DroolsConsequenceAction;
import org.jbpm.workflow.core.node.StateBasedNode;
import org.kie.api.definition.process.WorkflowElementIdentifier;

public abstract class StateBasedNodeFactory<T extends StateBasedNodeFactory<T, P>, P extends RuleFlowNodeContainerFactory<P, ?>> extends ExtendedNodeFactory<T, P> {

    public static final String METHOD_TIMER = "timer";

    protected StateBasedNodeFactory(P nodeContainerFactory, NodeContainer nodeContainer, Node node, WorkflowElementIdentifier id) {
        super(nodeContainerFactory, nodeContainer, node, id);
    }

    protected StateBasedNode getStateBasedNode() {
        return (StateBasedNode) node;
    }

    public StateBasedNodeFactory<T, P> timer(String delay, String period, String dialect, String action) {
        Timer timer = new Timer();
        timer.setDelay(delay);
        timer.setPeriod(period);
        getStateBasedNode().addTimer(timer, new DroolsConsequenceAction(dialect, action));
        return this;
    }
}