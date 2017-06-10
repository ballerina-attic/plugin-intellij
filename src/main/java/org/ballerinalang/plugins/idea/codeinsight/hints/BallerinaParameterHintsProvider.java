/*
 *  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.ballerinalang.plugins.idea.codeinsight.hints;

import com.intellij.codeInsight.hints.InlayInfo;
import com.intellij.codeInsight.hints.InlayParameterHintsProvider;
import com.intellij.codeInsight.hints.MethodInfo;
import com.intellij.lang.Language;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;
import org.ballerinalang.plugins.idea.psi.ActionInvocationNode;
import org.ballerinalang.plugins.idea.psi.ConnectorInitExpressionNode;
import org.ballerinalang.plugins.idea.psi.ExpressionListNode;
import org.ballerinalang.plugins.idea.psi.ExpressionNode;
import org.ballerinalang.plugins.idea.psi.FunctionInvocationNode;
import org.ballerinalang.plugins.idea.psi.FunctionInvocationStatementNode;
import org.ballerinalang.plugins.idea.psi.NameReferenceNode;
import org.ballerinalang.plugins.idea.psi.ParameterListNode;
import org.ballerinalang.plugins.idea.psi.ParameterNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class BallerinaParameterHintsProvider implements InlayParameterHintsProvider {

    /**
     * Calls first to get inlay info of parameters.
     *
     * @param element element to get parameter info
     * @return {@link InlayInfo} object which contains the parameter hint info.
     */
    @NotNull
    @Override
    public List<InlayInfo> getParameterHints(PsiElement element) {
        if (element instanceof FunctionInvocationStatementNode || element instanceof FunctionInvocationNode
                || element instanceof ConnectorInitExpressionNode || element instanceof ActionInvocationNode) {
            return getParameterHintsForElement(element);
        }
        return new LinkedList<>();
    }

    /**
     * Calls second to get method info. This is used to identify blacklisted functions and prevent
     * adding parameter info.
     *
     * @param element element to get method info
     * @return {@link MethodInfo} object which contains function details.
     */
    @Nullable
    @Override
    public MethodInfo getMethodInfo(PsiElement element) {
        if (element instanceof FunctionInvocationStatementNode || element instanceof FunctionInvocationNode
                || element instanceof ConnectorInitExpressionNode || element instanceof ActionInvocationNode) {
            return getMethodInfoForElement(element);
        }
        return null;
    }

    /**
     * Returns a list of {@link InlayInfo} objects which corresponds to the provided element.
     *
     * @param element element which is being processed
     * @return List of {@link InlayInfo} objects
     */
    private LinkedList<InlayInfo> getParameterHintsForElement(PsiElement element) {
        LinkedList<InlayInfo> hints = new LinkedList<>();
        // Get the correct name identifier.
        PsiElement nameIdentifier = getNameIdentifier(element);
        if (nameIdentifier == null) {
            return hints;
        }
        PsiReference reference = nameIdentifier.getReference();
        if (reference == null) {
            return hints;
        }
        PsiElement resolvedElement = reference.resolve();
        if (resolvedElement == null) {
            return hints;
        }
        // Get the parent element. This is the definition element. We use this to get details of parameters.
        PsiElement parent = resolvedElement.getParent();
        if (parent == null) {
            return hints;
        }
        // ParameterListNode contains all of the parameters.
        ParameterListNode parameterListNode = PsiTreeUtil.getChildOfType(parent, ParameterListNode.class);
        if (parameterListNode == null) {
            return hints;
        }
        // Get all of the parameters.
        ParameterNode[] parameterNodes = PsiTreeUtil.getChildrenOfType(parameterListNode, ParameterNode.class);
        if (parameterNodes == null) {
            return hints;
        }
        // From the current element, get the ExpressionListNode. This contains all the expressions (arguments)
        // provided when we invoke a function, etc.
        ExpressionListNode expressionListNode = PsiTreeUtil.getChildOfType(element, ExpressionListNode.class);
        if (expressionListNode == null) {
            return hints;
        }
        // Get all provided arguments.
        ExpressionNode[] expressionNodes = PsiTreeUtil.getChildrenOfType(expressionListNode, ExpressionNode.class);
        if (expressionNodes == null) {
            return hints;
        }

        // We iterate through all the expressions since the number of expressions and the number of parameters can be
        // different.
        for (int i = 0; i < expressionNodes.length; i++) {
            // If the caret is after an comma(argument separator), we don't suggest parameter info since it can cause
            // usability issues. This is checked by the first condition. The second condition checks whether the
            // index of current expression exists at the parameters. Otherwise, ArrayIndexOutOfBound exception can
            // occur when we call parameterNodes[i].
            if (!expressionNodes[i].getText().isEmpty() && parameterNodes.length > i) {
                // Get the corresponding parameter.
                PsiElement parameterName = parameterNodes[i].getNameIdentifier();
                if (parameterName == null) {
                    continue;
                }
                // The first argument is the text which we want to display in the function invocation, etc. The
                // second argument is the offset of th InlayInfo. This is same as the offset of the expression.
                InlayInfo inlayInfo = new InlayInfo(parameterName.getText(), expressionNodes[i].getTextOffset());
                hints.add(inlayInfo);
            }
        }
        return hints;
    }

    @Nullable
    private MethodInfo getMethodInfoForElement(PsiElement element) {
        // Get the correct name identifier.
        PsiElement nameIdentifier = getNameIdentifier(element);
        if (nameIdentifier == null) {
            return null;
        }
        PsiReference reference = nameIdentifier.getReference();
        if (reference == null) {
            return null;
        }
        PsiElement resolvedElement = reference.resolve();
        if (resolvedElement == null) {
            return null;
        }
        // Get the parent element. This is the definition element.
        PsiElement parent = resolvedElement.getParent();
        if (parent == null) {
            return null;
        }
        ParameterListNode parameterListNode = PsiTreeUtil.getChildOfType(parent, ParameterListNode.class);
        if (parameterListNode == null) {
            return null;
        }
        // Get all the parameters.
        ParameterNode[] parameterNodes = PsiTreeUtil.getChildrenOfType(parameterListNode, ParameterNode.class);
        if (parameterNodes == null) {
            return null;
        }
        List<String> params = new LinkedList<>();
        // Iterate through all parameters and add the parameter name to the list.
        for (ParameterNode parameterNode : parameterNodes) {
            PsiElement parameterName = parameterNode.getNameIdentifier();
            if (parameterName == null) {
                return null;
            }
            params.add(parameterName.getText());
        }
        // Return a MethodInfoObject. The first argument is the fully qualified name of the function, etc. The second
        // argument is a list of parameters. This seems to be used to prevent adding InlayInfo in blacklisted functions.
        return new MethodInfo(nameIdentifier.getText(), params);
    }

    /**
     * Returns the correct identifier element which will be used to get the definition.
     *
     * @param element element which is used to get the identifier
     * @return identifier element if one can be found, {@code null} otherwise.
     */
    @Nullable
    private PsiElement getNameIdentifier(@NotNull PsiElement element) {
        PsiElement nameIdentifier;
        if (element instanceof ActionInvocationNode) {
            nameIdentifier = ((ActionInvocationNode) element).getNameIdentifier();
        } else {
            NameReferenceNode nameReferenceNode = PsiTreeUtil.getChildOfType(element, NameReferenceNode.class);
            if (nameReferenceNode == null) {
                return null;
            }
            nameIdentifier = nameReferenceNode.getNameIdentifier();
        }
        return nameIdentifier;
    }

    @NotNull
    @Override
    public Set<String> getDefaultBlackList() {
        return Collections.emptySet();
    }

    @Nullable
    @Override
    public Language getBlackListDependencyLanguage() {
        return null;
    }
}
