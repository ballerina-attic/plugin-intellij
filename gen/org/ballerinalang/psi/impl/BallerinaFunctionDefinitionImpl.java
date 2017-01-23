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

public class BallerinaFunctionDefinitionImpl extends ASTWrapperPsiElement implements BallerinaFunctionDefinition {

  public BallerinaFunctionDefinitionImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull BallerinaVisitor visitor) {
    visitor.visitFunctionDefinition(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof BallerinaVisitor) accept((BallerinaVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<BallerinaAnnotation> getAnnotationList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, BallerinaAnnotation.class);
  }

  @Override
  @NotNull
  public BallerinaFunctionBody getFunctionBody() {
    return findNotNullChildByClass(BallerinaFunctionBody.class);
  }

  @Override
  @Nullable
  public BallerinaParameterList getParameterList() {
    return findChildByClass(BallerinaParameterList.class);
  }

  @Override
  @Nullable
  public BallerinaReturnParameters getReturnParameters() {
    return findChildByClass(BallerinaReturnParameters.class);
  }

}