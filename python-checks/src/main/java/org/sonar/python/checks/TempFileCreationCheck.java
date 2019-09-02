/*
 * SonarQube Python Plugin
 * Copyright (C) 2011-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.python.checks;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.python.PythonSubscriptionCheck;
import org.sonar.python.api.tree.PyCallExpressionTree;
import org.sonar.python.api.tree.Tree;
import org.sonar.python.semantic.Symbol;

@Rule(key = "S5445")
public class TempFileCreationCheck extends PythonSubscriptionCheck {

  private static final List<String> SUSPICIOUS_CALLS = Arrays.asList("os.tempnam", "os.tmpnam", "tempfile.mktemp");

  @Override
  public void initialize(Context context) {
    context.registerSyntaxNodeConsumer(Tree.Kind.CALL_EXPR, ctx -> {
      PyCallExpressionTree callExpr = (PyCallExpressionTree) ctx.syntaxNode();
      Symbol symbol = ctx.symbolTable().getSymbol(callExpr);
      isInsecureTempFile(symbol).ifPresent(s -> ctx.addIssue(callExpr, String.format("'%s' is insecure. Use 'tempfile.TemporaryFile' instead", s)));
    });
  }

  private static Optional<String> isInsecureTempFile(@Nullable Symbol symbol) {
    if (symbol == null) {
      return Optional.empty();
    }
    return SUSPICIOUS_CALLS.stream().filter(symbol.qualifiedName()::equals).findFirst();
  }
}