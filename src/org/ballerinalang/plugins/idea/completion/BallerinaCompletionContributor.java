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

package org.ballerinalang.plugins.idea.completion;

import com.intellij.codeInsight.completion.AddSpaceInsertHandler;
import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.DumbAware;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.util.ProcessingContext;
import org.ballerinalang.plugins.idea.psi.AliasNode;
import org.ballerinalang.plugins.idea.psi.AnnotationNameNode;
import org.ballerinalang.plugins.idea.psi.CompilationUnitNode;
import org.ballerinalang.plugins.idea.psi.FunctionDefinitionNode;
import org.ballerinalang.plugins.idea.psi.IdentifierPSINode;
import org.ballerinalang.plugins.idea.psi.ImportDeclarationNode;
import org.ballerinalang.plugins.idea.psi.LiteralValueNode;
import org.ballerinalang.plugins.idea.psi.PackageDeclarationNode;
import org.ballerinalang.plugins.idea.psi.PackageNameNode;
import org.ballerinalang.plugins.idea.psi.ParameterNode;
import org.ballerinalang.plugins.idea.psi.SimpleTypeNode;
import org.ballerinalang.plugins.idea.psi.StatementNode;
import org.ballerinalang.plugins.idea.psi.VariableReferenceNode;
import org.ballerinalang.plugins.idea.psi.impl.BallerinaPsiImplUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

import static org.ballerinalang.plugins.idea.completion.BallerinaCompletionUtil.*;

public class BallerinaCompletionContributor extends CompletionContributor implements DumbAware {

    private static final LookupElementBuilder PACKAGE;
    private static final LookupElementBuilder IMPORT;
    private static final LookupElementBuilder CONST;
    private static final LookupElementBuilder SERVICE;
    private static final LookupElementBuilder FUNCTION;
    private static final LookupElementBuilder CONNECTOR;
    private static final LookupElementBuilder STRUCT;
    private static final LookupElementBuilder TYPECONVERTER;

    // Simple types
    private static final LookupElementBuilder BOOLEAN;
    private static final LookupElementBuilder INT;
    private static final LookupElementBuilder DOUBLE;
    private static final LookupElementBuilder STRING;

    // Reference types
    private static final LookupElementBuilder MESSAGE;
    private static final LookupElementBuilder XML;
    private static final LookupElementBuilder JSON;
    private static final LookupElementBuilder EXCEPTION;
    private static final LookupElementBuilder MAP;
    private static final LookupElementBuilder DATATABLE;

    static {
        PACKAGE = createKeywordLookupElement("package", true, AddSpaceInsertHandler.INSTANCE_WITH_AUTO_POPUP);
        IMPORT = createKeywordLookupElement("import", true, AddSpaceInsertHandler.INSTANCE_WITH_AUTO_POPUP);
        CONST = createKeywordLookupElement("const", true, AddSpaceInsertHandler.INSTANCE_WITH_AUTO_POPUP);
        SERVICE = createKeywordLookupElement("service", true, AddSpaceInsertHandler.INSTANCE);
        FUNCTION = createKeywordLookupElement("function", true, AddSpaceInsertHandler.INSTANCE);
        CONNECTOR = createKeywordLookupElement("connector", true, AddSpaceInsertHandler.INSTANCE);
        STRUCT = createKeywordLookupElement("struct", true, AddSpaceInsertHandler.INSTANCE);
        TYPECONVERTER = createKeywordLookupElement("typeconverter", true, AddSpaceInsertHandler.INSTANCE);

        BOOLEAN = createSimpleTypeLookupElement("boolean", true, AddSpaceInsertHandler.INSTANCE);
        INT = createSimpleTypeLookupElement("int", true, AddSpaceInsertHandler.INSTANCE);
        DOUBLE = createSimpleTypeLookupElement("double", true, AddSpaceInsertHandler.INSTANCE);
        STRING = createSimpleTypeLookupElement("string", true, AddSpaceInsertHandler.INSTANCE);

        MESSAGE = createReferenceTypeLookupElement("message", true, AddSpaceInsertHandler.INSTANCE);
        XML = createReferenceTypeLookupElement("xml", true, AddSpaceInsertHandler.INSTANCE);
        JSON = createReferenceTypeLookupElement("json", true, AddSpaceInsertHandler.INSTANCE);
        EXCEPTION = createReferenceTypeLookupElement("exception", true, AddSpaceInsertHandler.INSTANCE);
        MAP = createReferenceTypeLookupElement("map", true, AddSpaceInsertHandler.INSTANCE);
        DATATABLE = createReferenceTypeLookupElement("datatable", true, AddSpaceInsertHandler.INSTANCE);
    }

    private static LookupElementBuilder createLookupElement(String name, boolean withBoldness,
                                                            InsertHandler insertHandler) {
        return LookupElementBuilder.create(name).withBoldness(withBoldness).withInsertHandler(insertHandler);
    }

    private static LookupElementBuilder createKeywordLookupElement(String name, boolean withBoldness,
                                                                   InsertHandler insertHandler) {
        return createLookupElement(name, withBoldness, insertHandler).withTypeText("Keyword", true);
    }

    private static LookupElementBuilder createSimpleTypeLookupElement(String name, boolean withBoldness,
                                                                      InsertHandler insertHandler) {
        return createLookupElement(name, withBoldness, insertHandler).withTypeText("Simple Type", true);
    }

    private static LookupElementBuilder createReferenceTypeLookupElement(String name, boolean withBoldness,
                                                                         InsertHandler insertHandler) {
        return createLookupElement(name, withBoldness, insertHandler).withTypeText("Reference Type", true);
    }

    public BallerinaCompletionContributor() {
        extend(CompletionType.BASIC,
                PlatformPatterns.psiElement(),
                new CompletionProvider<CompletionParameters>() {
                    public void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context,
                                               @NotNull CompletionResultSet resultSet) {
                        addSuggestions(parameters.getPosition(), resultSet, parameters.getOriginalFile());
                    }
                }
        );
    }

    private void addSuggestions(PsiElement element, CompletionResultSet resultSet, PsiFile originalFile) {

        PsiElement parent = element.getParent();
        PsiElement parentPrevSibling = parent.getPrevSibling();
        PsiElement prevSibling = element.getPrevSibling();

        if (parent instanceof LiteralValueNode) {
            return;
        }
        //Todo - Add literal value node, service definition
        if (parent instanceof PsiFile) {
            addFileLevelKeywords(resultSet, CONTEXT_KEYWORD_PRIORITY, true, true);
        } else if (parentPrevSibling instanceof ImportDeclarationNode
                || parentPrevSibling instanceof PackageDeclarationNode) {
            addFileLevelKeywords(resultSet, CONTEXT_KEYWORD_PRIORITY, false, true);
        } else if (parent instanceof AnnotationNameNode) {
            // Todo - Add all annotations
            resultSet.addElement(LookupElementBuilder.create("GET"));
            resultSet.addElement(LookupElementBuilder.create("POST"));
        } else if (parent instanceof PackageNameNode) {
            //Todo- check alias node
            if (parent.getParent().getParent() instanceof PackageDeclarationNode) {
                PsiDirectory[] psiDirectories = BallerinaPsiImplUtil.suggestCurrentPackagePath(element);
                for (PsiDirectory directory : psiDirectories) {
                    InsertHandler<LookupElement> insertHandler;
                    if (BallerinaPsiImplUtil.hasSubdirectories(directory)) {
                        insertHandler = ImportCompletionInsertHandler.INSTANCE_WITH_AUTO_POPUP;
                    } else {
                        insertHandler = StatementCompletionInsertHandler.INSTANCE;
                    }
                    resultSet.addElement(LookupElementBuilder.createWithIcon(directory)
                            .withInsertHandler(insertHandler));
                }
            } else if (!(parent.getParent() instanceof AliasNode)
                    && parent.getParent().getParent() instanceof ImportDeclarationNode) {

                PsiDirectory[] psiDirectories = BallerinaPsiImplUtil.suggestImportPackages(element);
                List<String> allImportedPackages =
                        BallerinaPsiImplUtil.getAllImportedPackagesInCurrentFile(element).stream()
                                .map(PsiElement::getText)
                                .collect(Collectors.toList());

                for (PsiDirectory directory : psiDirectories) {
                    InsertHandler<LookupElement> insertHandler;
                    if (BallerinaPsiImplUtil.hasSubdirectories(directory)) {
                        insertHandler = ImportCompletionInsertHandler.INSTANCE_WITH_AUTO_POPUP;
                    } else {
                        if (allImportedPackages.contains(directory.getName())) {
                            insertHandler = AliasCompletionInsertHandler.INSTANCE;
                        } else {
                            insertHandler = StatementCompletionInsertHandler.INSTANCE;
                        }
                    }
                    resultSet.addElement(LookupElementBuilder.createWithIcon(directory)
                            .withInsertHandler(insertHandler));
                }
            } else {
                // Todo - Handle scenario
            }
        } else if (parent instanceof ImportDeclarationNode) {
            PsiDirectory[] psiDirectories = BallerinaPsiImplUtil.suggestImportPackages(element);
            for (PsiDirectory directory : psiDirectories) {
                InsertHandler<LookupElement> insertHandler;
                if (BallerinaPsiImplUtil.hasSubdirectories(directory)) {
                    insertHandler = ImportCompletionInsertHandler.INSTANCE_WITH_AUTO_POPUP;
                } else {
                    //Todo-check for duplicate package names and suggest 'as'
                    insertHandler = StatementCompletionInsertHandler.INSTANCE;
                }
                resultSet.addElement(LookupElementBuilder.create(directory).withInsertHandler(insertHandler));
            }
        } else if (parent instanceof PsiErrorElement) {

            PsiElement superParent = parent.getParent();
            //Todo - add throws keyword
            //Todo - add return keyword
            //Todo - add variables
            if (superParent instanceof StatementNode) {
                addValueTypes(resultSet, VALUE_TYPE_PRIORITY);
                addReferenceTypes(resultSet, REFERENCE_TYPE_PRIORITY);

                // Todo- remove duplication
                List<PsiElement> connectors = BallerinaPsiImplUtil.getAllConnectorsInCurrentPackage(originalFile);
                for (PsiElement connector : connectors) {
                    LookupElementBuilder builder = LookupElementBuilder.create(connector.getText())
                            .withTypeText("Connector").withIcon(AllIcons.Nodes.Class);
                    resultSet.addElement(PrioritizedLookupElement.withPriority(builder, CONNECTOR_PRIORITY));
                }

                List<PsiElement> functions = BallerinaPsiImplUtil.getAllFunctionsInCurrentPackage(originalFile);
                for (PsiElement function : functions) {
                    LookupElementBuilder builder = LookupElementBuilder.create(function.getText())
                            .withTypeText("Function").withTailText("()", true).withIcon(AllIcons.Nodes.Field)
                            .withInsertHandler(FunctionCompletionInsertHandler.INSTANCE);
                    resultSet.addElement(PrioritizedLookupElement.withPriority(builder, FUNCTION_PRIORITY));
                }

                List<PsiElement> structs = BallerinaPsiImplUtil.getAllStructsInCurrentPackage(originalFile);
                for (PsiElement struct : structs) {
                    LookupElementBuilder builder = LookupElementBuilder.create(struct.getText())
                            .withTypeText("Struct").withIcon(AllIcons.Nodes.Static);
                    resultSet.addElement(PrioritizedLookupElement.withPriority(builder, STRUCT_PRIORITY));
                }

                List<PsiElement> packages = BallerinaPsiImplUtil.getAllImportedPackagesInCurrentFile(originalFile);
                for (PsiElement pack : packages) {
                    LookupElementBuilder builder = LookupElementBuilder.create(pack.getText())
                            .withTypeText("Package").withIcon(AllIcons.Nodes.Package)
                            .withInsertHandler(PackageCompletionInsertHandler.INSTANCE_WITH_AUTO_POPUP);
                    resultSet.addElement(PrioritizedLookupElement.withPriority(builder, PACKAGE_PRIORITY));
                }

                PsiElement context = element.getContext();
                if (context == null) {
                    context = element.getParent().getContext();
                }
                List<PsiElement> variables = BallerinaPsiImplUtil.getAllVariablesInResolvableScope(context);
                for (PsiElement variable : variables) {
                    LookupElementBuilder builder = LookupElementBuilder.create(variable.getText())
                            .withTypeText("Variable").withIcon(AllIcons.Nodes.Variable);
                    resultSet.addElement(PrioritizedLookupElement.withPriority(builder, VARIABLE_PRIORITY));
                }
            } else if (superParent instanceof CompilationUnitNode) {
                if (parentPrevSibling == null) {
                    addFileLevelKeywords(resultSet, CONTEXT_KEYWORD_PRIORITY, true, true);
                } else {
                    //Todo - move to util
                    PsiElement nonWhitespaceElement = parent.getPrevSibling();
                    while (nonWhitespaceElement != null && nonWhitespaceElement instanceof PsiWhiteSpace) {
                        nonWhitespaceElement = nonWhitespaceElement.getPrevSibling();
                    }
                    if (nonWhitespaceElement != null) {
                        if (nonWhitespaceElement instanceof ImportDeclarationNode ||
                                nonWhitespaceElement instanceof PackageDeclarationNode) {
                            addFileLevelKeywords(resultSet, CONTEXT_KEYWORD_PRIORITY, false, true);
                        } else {
                            addFileLevelKeywords(resultSet, CONTEXT_KEYWORD_PRIORITY, false, false);
                        }
                    } else {
                        addFileLevelKeywords(resultSet, CONTEXT_KEYWORD_PRIORITY, false, true);
                    }
                }
            } else {
                if (parentPrevSibling == null) {
                    addFileLevelKeywords(resultSet, CONTEXT_KEYWORD_PRIORITY, true, true);
                } else {
                    addFileLevelKeywords(resultSet, CONTEXT_KEYWORD_PRIORITY, false, true);
                }
            }
        } else if (parent instanceof FunctionDefinitionNode || parent instanceof ParameterNode) {

            if (prevSibling != null) {
                if ("(".equals(prevSibling.getText())) {
                    resultSet.addElement(LookupElementBuilder.create("YYY error 3.1"));
                } else {
                    //Todo - check type
                    //                        if(){
                    //
                    //                        }else{
                    //
                    //                        }
                    resultSet.addElement(LookupElementBuilder.create("YYY error 3.2"));
                }
            } else {
                resultSet.addElement(LookupElementBuilder.create("YYY error 3.3"));
            }
        } else if (parent instanceof SimpleTypeNode) {

            PsiElement sibling = parent.getParent().getPrevSibling();

            if (sibling == null) {

                // Todo - remove duplication

                addValueTypes(resultSet, CONTEXT_KEYWORD_PRIORITY);
                addReferenceTypes(resultSet, CONTEXT_KEYWORD_PRIORITY);

                List<PsiElement> connectors = BallerinaPsiImplUtil.getAllConnectorsInCurrentPackage(originalFile);
                for (PsiElement connector : connectors) {
                    LookupElementBuilder builder = LookupElementBuilder.create(connector.getText())
                            .withTypeText("Connector").withIcon(AllIcons.Nodes.Class);
                    resultSet.addElement(PrioritizedLookupElement.withPriority(builder, CONNECTOR_PRIORITY));
                }

                List<PsiElement> structs = BallerinaPsiImplUtil.getAllStructsInCurrentPackage(originalFile);
                for (PsiElement struct : structs) {
                    LookupElementBuilder builder = LookupElementBuilder.create(struct.getText()).withTypeText("Struct")
                            .withIcon(AllIcons.Nodes.Static);
                    resultSet.addElement(PrioritizedLookupElement.withPriority(builder, STRUCT_PRIORITY));
                }

                List<PsiElement> packages = BallerinaPsiImplUtil.getAllImportedPackagesInCurrentFile(originalFile);
                for (PsiElement pack : packages) {
                    LookupElementBuilder builder = LookupElementBuilder.create(pack.getText())
                            .withTypeText("Package").withIcon(AllIcons.Nodes.Package)
                            .withInsertHandler(PackageCompletionInsertHandler.INSTANCE_WITH_AUTO_POPUP);
                    resultSet.addElement(PrioritizedLookupElement.withPriority(builder, PACKAGE_PRIORITY));
                }

                // Todo - move to utils
                PsiElement temp = parent.getParent().getParent().getParent().getParent();
                while (temp != null && !(temp instanceof PsiFile)) {
                    if (temp instanceof StatementNode) {
                        PsiElement context = element.getContext();
                        if (context == null) {
                            context = element.getParent().getContext();
                        }
                        List<PsiElement> variables = BallerinaPsiImplUtil.getAllVariablesInResolvableScope(context);
                        for (PsiElement variable : variables) {
                            LookupElementBuilder builder = LookupElementBuilder.create(variable.getText())
                                    .withTypeText("Variable").withIcon(AllIcons.Nodes.Variable);
                            resultSet.addElement(PrioritizedLookupElement.withPriority(builder, VARIABLE_PRIORITY));
                        }
                        break;
                    }
                    temp = temp.getParent();
                }

                return;
            }

            if (":".equals(sibling.getText())) {

                sibling = sibling.getPrevSibling();
                if (sibling == null) {
                    // Todo - remove duplication

                    addValueTypes(resultSet, CONTEXT_KEYWORD_PRIORITY);
                    addReferenceTypes(resultSet, CONTEXT_KEYWORD_PRIORITY);

                    List<PsiElement> connectors = BallerinaPsiImplUtil.getAllConnectorsInCurrentPackage(originalFile);
                    for (PsiElement connector : connectors) {
                        LookupElementBuilder builder = LookupElementBuilder.create(connector.getText())
                                .withTypeText("Connector").withIcon(AllIcons.Nodes.Class);
                        resultSet.addElement(PrioritizedLookupElement.withPriority(builder, CONNECTOR_PRIORITY));
                    }

                    List<PsiElement> structs = BallerinaPsiImplUtil.getAllStructsInCurrentPackage(originalFile);
                    for (PsiElement struct : structs) {
                        LookupElementBuilder builder = LookupElementBuilder.create(struct.getText()).withTypeText
                                ("Struct")
                                .withIcon(AllIcons.Nodes.Static);
                        resultSet.addElement(PrioritizedLookupElement.withPriority(builder, STRUCT_PRIORITY));
                    }

                    List<PsiElement> packages = BallerinaPsiImplUtil.getAllImportedPackagesInCurrentFile(originalFile);
                    for (PsiElement pack : packages) {
                        LookupElementBuilder builder = LookupElementBuilder.create(pack.getText())
                                .withTypeText("Package").withIcon(AllIcons.Nodes.Package)
                                .withInsertHandler(PackageCompletionInsertHandler.INSTANCE_WITH_AUTO_POPUP);
                        resultSet.addElement(PrioritizedLookupElement.withPriority(builder, PACKAGE_PRIORITY));
                    }

                    // Todo - move to utils
                    PsiElement temp = parent.getParent().getParent().getParent().getParent();
                    while (temp != null && !(temp instanceof PsiFile)) {
                        if (temp instanceof StatementNode) {
                            PsiElement context = element.getContext();
                            if (context == null) {
                                context = element.getParent().getContext();
                            }
                            List<PsiElement> variables = BallerinaPsiImplUtil.getAllVariablesInResolvableScope(context);
                            for (PsiElement variable : variables) {
                                LookupElementBuilder builder = LookupElementBuilder.create(variable.getText())
                                        .withTypeText("Variable").withIcon(AllIcons.Nodes.Variable);
                                resultSet.addElement(PrioritizedLookupElement.withPriority(builder, VARIABLE_PRIORITY));
                            }
                            break;
                        }
                        temp = temp.getParent();
                    }

                    return;
                }

                PsiElement lastChild = sibling.getLastChild();

                List<PsiElement> packages = BallerinaPsiImplUtil.getAllImportedPackagesInCurrentFile(originalFile);

                for (PsiElement pack : packages) {

                    if (lastChild.getText().equals(pack.getText())) {
                        PsiDirectory[] psiDirectories =
                                BallerinaPsiImplUtil.resolveDirectory(((PackageNameNode) pack).getNameIdentifier());

                        if (psiDirectories.length == 1) {
                            List<PsiElement> functions = BallerinaPsiImplUtil
                                    .getAllFunctionsInPackage(psiDirectories[0]);

                            for (PsiElement function : functions) {
                                LookupElementBuilder builder = LookupElementBuilder.create(function.getText())
                                        .withTypeText("Function").withTailText("()", true)
                                        .withIcon(AllIcons.Nodes.Field)
                                        .withInsertHandler(FunctionCompletionInsertHandler.INSTANCE);
                                resultSet.addElement(PrioritizedLookupElement.withPriority(builder,
                                        FUNCTION_PRIORITY));
                            }

                            List<PsiElement> connectors = BallerinaPsiImplUtil
                                    .getAllConnectorsInPackage(psiDirectories[0]);
                            for (PsiElement connector : connectors) {
                                LookupElementBuilder builder = LookupElementBuilder.create(connector.getText())
                                        .withTypeText("Connector").withIcon(AllIcons.Nodes.Class);
                                resultSet.addElement(PrioritizedLookupElement.withPriority(builder,
                                        CONNECTOR_PRIORITY));
                            }

                            // Todo - add structs
                        } else {
                            // This situation cannot/should not happen since all the imported packages are unique.
                            // This should be highlighted using an annotator.
                        }
                    }
                }
            } else {

                addValueTypes(resultSet, CONTEXT_KEYWORD_PRIORITY);
                addReferenceTypes(resultSet, CONTEXT_KEYWORD_PRIORITY);

                List<PsiElement> connectors = BallerinaPsiImplUtil.getAllConnectorsInCurrentPackage(originalFile);
                for (PsiElement connector : connectors) {
                    LookupElementBuilder builder = LookupElementBuilder.create(connector.getText())
                            .withTypeText("Connector").withIcon(AllIcons.Nodes.Class);
                    resultSet.addElement(PrioritizedLookupElement.withPriority(builder, CONNECTOR_PRIORITY));
                }

                List<PsiElement> structs = BallerinaPsiImplUtil.getAllStructsInCurrentPackage(originalFile);
                for (PsiElement struct : structs) {
                    LookupElementBuilder builder = LookupElementBuilder.create(struct.getText()).withTypeText("Struct")
                            .withIcon(AllIcons.Nodes.Static);
                    resultSet.addElement(PrioritizedLookupElement.withPriority(builder, STRUCT_PRIORITY));
                }

                List<PsiElement> packages = BallerinaPsiImplUtil.getAllImportedPackagesInCurrentFile(originalFile);
                for (PsiElement pack : packages) {
                    LookupElementBuilder builder = LookupElementBuilder.create(pack.getText())
                            .withTypeText("Package").withIcon(AllIcons.Nodes.Package)
                            .withInsertHandler(PackageCompletionInsertHandler.INSTANCE_WITH_AUTO_POPUP);
                    resultSet.addElement(PrioritizedLookupElement.withPriority(builder, PACKAGE_PRIORITY));
                }

                // Todo - move to utils
                PsiElement temp = parent.getParent().getParent().getParent().getParent();
                while (temp != null && !(temp instanceof PsiFile)) {
                    if (temp instanceof StatementNode) {
                        PsiElement context = element.getContext();
                        if (context == null) {
                            context = element.getParent().getContext();
                        }
                        List<PsiElement> variables = BallerinaPsiImplUtil.getAllVariablesInResolvableScope(context);
                        for (PsiElement variable : variables) {
                            LookupElementBuilder builder = LookupElementBuilder.create(variable.getText())
                                    .withTypeText("Variable").withIcon(AllIcons.Nodes.Variable);
                            resultSet.addElement(PrioritizedLookupElement.withPriority(builder, VARIABLE_PRIORITY));
                        }
                        break;
                    }
                    temp = temp.getParent();
                }
            }
        } else if (parent instanceof StatementNode) {
            PsiElement temp = parentPrevSibling;
            while (temp != null && temp.getText().isEmpty()) {
                temp = temp.getPrevSibling();
            }

            if (temp != null) {
                List<PsiElement> packages = BallerinaPsiImplUtil.getAllImportedPackagesInCurrentFile(originalFile);

                for (PsiElement pack : packages) {
                    if (temp.getText().equals(pack.getText() + ":")) {

                        PsiDirectory[] psiDirectories =
                                BallerinaPsiImplUtil.resolveDirectory(((PackageNameNode) pack).getNameIdentifier());

                        if (psiDirectories.length == 1) {
                            List<PsiElement> functions = BallerinaPsiImplUtil
                                    .getAllFunctionsInPackage(psiDirectories[0]);

                            for (PsiElement function : functions) {
                                LookupElementBuilder builder = LookupElementBuilder.create(function.getText())
                                        .withTypeText("Function").withTailText("()", true)
                                        .withIcon(AllIcons.Nodes.Field)
                                        .withInsertHandler(FunctionCompletionInsertHandler.INSTANCE);
                                resultSet.addElement(PrioritizedLookupElement.withPriority(builder,
                                        FUNCTION_PRIORITY));
                            }
                        } else {
                            // This situation cannot/should not happen since all the imported packages are unique.
                            // This should be highlighted using an annotator.
                        }
                    }
                }
            }

            PsiElement context = element.getContext();
            if (context == null) {
                context = element.getParent().getContext();
            }
            List<PsiElement> variables = BallerinaPsiImplUtil.getAllVariablesInResolvableScope(context);
            for (PsiElement variable : variables) {
                LookupElementBuilder builder = LookupElementBuilder.create(variable.getText())
                        .withTypeText("Variable").withIcon(AllIcons.Nodes.Variable);
                resultSet.addElement(PrioritizedLookupElement.withPriority(builder, VARIABLE_PRIORITY));
            }
        } else if (parent instanceof VariableReferenceNode) {
            // Todo - get all variables in the scope
            // Todo - get all constant variables

            PsiElement context = element.getContext();
            if (context == null) {
                context = element.getParent().getContext();
            }
            List<PsiElement> variables = BallerinaPsiImplUtil.getAllVariablesInResolvableScope(context);
            for (PsiElement variable : variables) {
                LookupElementBuilder builder = LookupElementBuilder.create(variable.getText())
                        .withTypeText("Variable").withIcon(AllIcons.Nodes.Variable);
                resultSet.addElement(PrioritizedLookupElement.withPriority(builder, VARIABLE_PRIORITY));
            }
        } else {
            if (element instanceof IdentifierPSINode) {
                return;
            }
            if (parentPrevSibling == null) {
                addFileLevelKeywords(resultSet, CONTEXT_KEYWORD_PRIORITY, true, true);
            } else {
                addFileLevelKeywords(resultSet, CONTEXT_KEYWORD_PRIORITY, false, true);
            }
        }
    }

    private void addValueTypes(CompletionResultSet resultSet, int priority) {
        resultSet.addElement(PrioritizedLookupElement.withPriority(BOOLEAN, priority));
        resultSet.addElement(PrioritizedLookupElement.withPriority(INT, priority));
        resultSet.addElement(PrioritizedLookupElement.withPriority(DOUBLE, priority));
        resultSet.addElement(PrioritizedLookupElement.withPriority(STRING, priority));
    }

    private void addReferenceTypes(CompletionResultSet resultSet, int priority) {
        resultSet.addElement(PrioritizedLookupElement.withPriority(MESSAGE, priority));
        resultSet.addElement(PrioritizedLookupElement.withPriority(XML, priority));
        resultSet.addElement(PrioritizedLookupElement.withPriority(JSON, priority));
        resultSet.addElement(PrioritizedLookupElement.withPriority(EXCEPTION, priority));
        resultSet.addElement(PrioritizedLookupElement.withPriority(MAP, priority));
        resultSet.addElement(PrioritizedLookupElement.withPriority(DATATABLE, priority));
    }

    private void addFileLevelKeywords(CompletionResultSet resultSet, int priority, boolean withPackage,
                                      boolean withImport) {
        if (withPackage) {
            resultSet.addElement(PrioritizedLookupElement.withPriority(PACKAGE, priority));
        }
        if (withImport) {
            resultSet.addElement(PrioritizedLookupElement.withPriority(IMPORT, priority));
        }
        resultSet.addElement(PrioritizedLookupElement.withPriority(CONST, priority));
        resultSet.addElement(PrioritizedLookupElement.withPriority(SERVICE, priority));
        resultSet.addElement(PrioritizedLookupElement.withPriority(FUNCTION, priority));
        resultSet.addElement(PrioritizedLookupElement.withPriority(CONNECTOR, priority));
        resultSet.addElement(PrioritizedLookupElement.withPriority(STRUCT, priority));
        resultSet.addElement(PrioritizedLookupElement.withPriority(TYPECONVERTER, priority));
    }

    @Override
    public boolean invokeAutoPopup(@NotNull PsiElement position, char typeChar) {
        return typeChar == ':' || typeChar == '@';
    }

    @Override
    public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
        super.fillCompletionVariants(parameters, result);
    }
}