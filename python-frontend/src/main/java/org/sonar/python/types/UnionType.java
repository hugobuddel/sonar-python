/*
 * SonarQube Python Plugin
 * Copyright (C) 2011-2020 SonarSource SA
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
package org.sonar.python.types;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.sonar.plugins.python.api.types.InferredType;

import static org.sonar.python.types.InferredTypes.anyType;

class UnionType implements InferredType {

  private final Set<InferredType> types;

  private UnionType(Set<InferredType> types) {
    this.types = types;
  }

  public static InferredType or(InferredType type1, InferredType type2) {
    if (type1.equals(anyType()) || type2.equals(anyType())) {
      return anyType();
    }
    if (type1.equals(type2)) {
      return type1;
    }
    Set<InferredType> types = new HashSet<>();
    addTypes(type1, types);
    addTypes(type2, types);
    return new UnionType(types);
  }

  private static void addTypes(InferredType type, Set<InferredType> types) {
    if (type instanceof UnionType) {
      types.addAll(((UnionType) type).types);
    } else {
      types.add(type);
    }
  }

  @Override
  public boolean isIdentityComparableWith(InferredType other) {
    // TODO Use canBeOrExtend
    if (other.equals(anyType())) {
      return true;
    }
    if (other instanceof UnionType) {
      Set<InferredType> allTypes = new HashSet<>(types);
      Set<InferredType> otherTypes = ((UnionType) other).types;
      allTypes.addAll(otherTypes);
      return allTypes.size() != this.types.size() + otherTypes.size();
    }
    return types.contains(other);
  }

  @Override
  public boolean canHaveMember(String memberName) {
    return types.stream().anyMatch(t -> t.canHaveMember(memberName));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UnionType unionType = (UnionType) o;
    return Objects.equals(types, unionType.types);
  }

  @Override
  public int hashCode() {
    return Objects.hash(types);
  }

  @Override
  public String toString() {
    return "UnionType" + types;
  }
}