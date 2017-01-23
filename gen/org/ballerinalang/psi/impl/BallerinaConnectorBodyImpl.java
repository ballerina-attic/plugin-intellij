// This is a generated file. Not intended for manual editing.
package org.ballerinalang.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static org.ballerinalang.psi.BallerinaTypes.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import org.ballerinalang.psi.*;

public class BallerinaConnectorBodyImpl extends ASTWrapperPsiElement implements BallerinaConnectorBody {

  public BallerinaConnectorBodyImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull BallerinaVisitor visitor) {
    visitor.visitConnectorBody(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof BallerinaVisitor) accept((BallerinaVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<BallerinaActionDefinition> getActionDefinitionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, BallerinaActionDefinition.class);
  }

  @Override
  @NotNull
  public List<BallerinaConnectorDeclaration> getConnectorDeclarationList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, BallerinaConnectorDeclaration.class);
  }

  @Override
  @NotNull
  public List<BallerinaVariableDeclaration> getVariableDeclarationList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, BallerinaVariableDeclaration.class);
  }

}