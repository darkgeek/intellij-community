package com.jetbrains.python.inspections;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiReference;
import com.jetbrains.python.PyBundle;
import com.jetbrains.python.PyNames;
import com.jetbrains.python.actions.ReplaceFunctionWithSetLiteralQuickFix;
import com.jetbrains.python.psi.*;
import com.jetbrains.python.psi.impl.PyBuiltinCache;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

/**
 * User: catherine
 *
 * Inspection to find set built-in function and replace it with set literal
 * available if the selected language level supports set literals.
 */
public class PySetFunctionToLiteralInspection extends PyInspection {

  @Nls
  @NotNull
  @Override
  public String getDisplayName() {
    return PyBundle.message("INSP.NAME.set.function.to.literal");
  }

  @NotNull
  @Override
  public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    return new Visitor(holder);
  }

  private static class Visitor extends PyInspectionVisitor {
    public Visitor(final ProblemsHolder holder) {
      super(holder);
    }

    @Override
    public void visitPyCallExpression(final PyCallExpression node) {
      if (LanguageLevel.forElement(node).supportsSetLiterals()) {
        PyExpression callee = node.getCallee();
        if (node.isCalleeText(PyNames.SET) && isInBuiltins(callee)) {
          PyExpression[] arguments = node.getArguments();
          if (arguments.length == 1) {
            PyExpression argument= arguments[0];
            String string = "";
            PyElement[] elements = {};
            if (argument instanceof PyStringLiteralExpression) {
              string = ((PyStringLiteralExpression)argument).getStringValue();
            }
            if ((argument instanceof PySequenceExpression || (argument instanceof PyParenthesizedExpression &&
                          ((PyParenthesizedExpression)argument).getContainedExpression() instanceof PyTupleExpression))) {

              if (argument instanceof PySequenceExpression)
                elements = ((PySequenceExpression)argument).getElements();
              if (argument instanceof PyParenthesizedExpression) {
                PyExpression tuple = ((PyParenthesizedExpression)argument).getContainedExpression();
                if (tuple instanceof PyTupleExpression)
                  elements = ((PyTupleExpression)(tuple)).getElements();
              }
            }
            if (elements.length != 0 || !string.isEmpty())
                registerProblem(node, "Function call can be replaced with set literal",
                                                 new ReplaceFunctionWithSetLiteralQuickFix(elements, string));
          }
        }
      }
    }

    private static boolean isInBuiltins(PyExpression callee) {
      PsiReference reference = callee.getReference();
      if (reference != null) {
        PsiElement resolved = reference.resolve();
        if (resolved != null && PyBuiltinCache.getInstance(callee).hasInBuiltins(resolved)) {
          return true;
        }
      }
      return false;
    }
  }
}
