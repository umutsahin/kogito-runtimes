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
package org.jbpm.compiler.canonical;

import java.util.Map;
import java.util.Set;

import org.jbpm.compiler.canonical.descriptors.ExpressionUtils;
import org.jbpm.process.core.Context;
import org.jbpm.process.core.ContextContainer;
import org.jbpm.process.core.context.exception.CompensationScope;
import org.jbpm.process.core.context.variable.Variable;
import org.jbpm.process.core.context.variable.VariableScope;
import org.jbpm.process.core.datatype.DataTypeResolver;
import org.jbpm.ruleflow.core.WorkflowElementIdentifierFactory;
import org.kie.api.definition.process.NodeContainer;
import org.kie.api.definition.process.WorkflowElementIdentifier;
import org.kie.kogito.internal.process.runtime.KogitoNode;
import org.kie.kogito.internal.utils.KogitoTags;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;

import static com.github.javaparser.StaticJavaParser.parseClassOrInterfaceType;
import static org.jbpm.ruleflow.core.RuleFlowProcessFactory.METHOD_ADD_COMPENSATION_CONTEXT;
import static org.jbpm.ruleflow.core.RuleFlowProcessFactory.METHOD_VARIABLE;
import static org.jbpm.ruleflow.core.factory.NodeFactory.METHOD_METADATA;
import static org.kie.kogito.internal.utils.ConversionUtils.sanitizeString;

public abstract class AbstractVisitor {

    protected static final String FACTORY_FIELD_NAME = "factory";
    protected static final String KCONTEXT_VAR = "kcontext";

    protected MethodCallExpr getWorkflowElementConstructor(WorkflowElementIdentifier identifier) {
        Type type = new ClassOrInterfaceType().setName(WorkflowElementIdentifierFactory.class.getName());
        MethodCallExpr identifierConstructor = new MethodCallExpr(new TypeExpr(type), "fromExternalFormat", NodeList.nodeList(new StringLiteralExpr(identifier.toExternalFormat())));
        return identifierConstructor;
    }

    protected MethodCallExpr getFactoryMethod(String object, String methodName, Expression... args) {
        MethodCallExpr variableMethod = new MethodCallExpr(new NameExpr(object), methodName);

        for (Expression arg : args) {
            variableMethod.addArgument(arg);
        }
        return variableMethod;
    }

    protected String getOrDefault(String value, String defaultValue) {
        if (value == null) {
            return sanitizeString(defaultValue);
        }
        return sanitizeString(value);
    }

    protected Expression getOrNullExpr(String value) {
        if (value == null) {
            return new NullLiteralExpr();
        }
        return new StringLiteralExpr(value);
    }

    protected void visitMetaData(Map<String, Object> metadata, BlockStmt body, String variableName) {
        metadata.entrySet().stream().filter(this::isValidMetadata)
                .forEach(e -> body.addStatement(getFactoryMethod(variableName, METHOD_METADATA, new StringLiteralExpr(e.getKey()), ExpressionUtils.getLiteralExpr(e.getValue()))));
    }

    private boolean isValidMetadata(Map.Entry<String, Object> e) {
        return !e.getKey().startsWith("BPMN.") && (e.getKey().startsWith("custom") || ExpressionUtils.isTypeSupported(e.getValue()));
    }

    protected void visitVariableScope(String field, VariableScope variableScope, BlockStmt body, Set<String> visitedVariables, String contextClass) {
        if (variableScope != null && !variableScope.getVariables().isEmpty()) {
            for (Variable variable : variableScope.getVariables()) {
                if (!visitedVariables.add(variable.getName())) {
                    continue;
                }
                String tags = (String) variable.getMetaData(KogitoTags.VARIABLE_TAGS);
                Object defaultValue = variable.getValue();
                body.tryAddImportToParentCompilationUnit(variable.getType().getClass());
                body.addStatement(getFactoryMethod(field, METHOD_VARIABLE, new StringLiteralExpr(variable.getName()),
                        new MethodCallExpr(DataTypeResolver.class.getName() + ".fromClass", new ClassExpr(parseClassOrInterfaceType(variable.getType().getStringType()).removeTypeArguments())),
                        defaultValue != null ? new StringLiteralExpr(defaultValue.toString()) : new NullLiteralExpr(), new StringLiteralExpr(KogitoTags.VARIABLE_TAGS),
                        tags != null ? new StringLiteralExpr(tags) : new NullLiteralExpr()));
            }
        }
    }

    protected final void visitCompensationScope(ContextContainer contextContainer, BlockStmt body, String factoryField) {
        Context context = getContext(contextContainer);
        if (context != null && context instanceof CompensationScope) {
            String contextId = ((CompensationScope) context).getContextContainerId();
            body.addStatement(getFactoryMethod(factoryField, METHOD_ADD_COMPENSATION_CONTEXT, new StringLiteralExpr(contextId)));
        }
    }

    private Context getContext(ContextContainer contextContainer) {
        Context context = contextContainer.getDefaultContext(CompensationScope.COMPENSATION_SCOPE);
        if (context == null && contextContainer instanceof KogitoNode) {
            NodeContainer parentContainer = ((KogitoNode) contextContainer).getParentContainer();
            if (parentContainer instanceof ContextContainer) {
                return getContext((ContextContainer) parentContainer);
            }
        }
        return context;
    }
}
