/*
 * Copyright 2013, Google Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *     * Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.dexlib2.writer.builder;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

import org.jf.dexlib2.base.reference.BaseTypeReference;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.util.FieldUtil;
import org.jf.dexlib2.util.MethodUtil;
import org.jf.dexlib2.writer.DexWriter;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;

public class BuilderClassDef extends BaseTypeReference implements ClassDef {
    @NonNull
    final BuilderTypeReference type;
    final int accessFlags;
    @Nullable
    final BuilderTypeReference superclass;
    @NonNull
    final BuilderTypeList interfaces;
    @Nullable
    final BuilderStringReference sourceFile;
    @NonNull
    final BuilderAnnotationSet annotations;
    @NonNull
    final SortedSet<BuilderField> staticFields;
    @NonNull
    final SortedSet<BuilderField> instanceFields;
    @NonNull
    final SortedSet<BuilderMethod> directMethods;
    @NonNull
    final SortedSet<BuilderMethod> virtualMethods;

    int classDefIndex = DexWriter.NO_INDEX;
    int encodedArrayOffset = DexWriter.NO_OFFSET;
    int annotationDirectoryOffset = DexWriter.NO_OFFSET;

    BuilderClassDef(@NonNull BuilderTypeReference type,
                    int accessFlags,
                    @Nullable BuilderTypeReference superclass,
                    @NonNull BuilderTypeList interfaces,
                    @Nullable BuilderStringReference sourceFile,
                    @NonNull BuilderAnnotationSet annotations,
                    @Nullable Iterable<? extends BuilderField> fields,
                    @Nullable Iterable<? extends BuilderMethod> methods) {
        if (fields == null) {
            fields = ImmutableList.of();
        }
        if (methods == null) {
            methods = ImmutableList.of();
        }

        this.type = type;
        this.accessFlags = accessFlags;
        this.superclass = superclass;
        this.interfaces = interfaces;
        this.sourceFile = sourceFile;
        this.annotations = annotations;
        this.staticFields = ImmutableSortedSet.copyOf(
                (Iterable<? extends BuilderField>) Iterables.filter(fields, FieldUtil.FIELD_IS_STATIC));
        this.instanceFields = ImmutableSortedSet.copyOf(
                (Iterable<? extends BuilderField>) Iterables.filter(fields, FieldUtil.FIELD_IS_INSTANCE));
        this.directMethods = ImmutableSortedSet.copyOf(
                (Iterable<? extends BuilderMethod>) Iterables.filter(methods, MethodUtil.METHOD_IS_DIRECT));
        this.virtualMethods = ImmutableSortedSet.copyOf(
                (Iterable<? extends BuilderMethod>) Iterables.filter(methods, MethodUtil.METHOD_IS_VIRTUAL));
    }

    @NonNull
    @Override
    public String getType() {
        return type.getType();
    }

    @Override
    public int getAccessFlags() {
        return accessFlags;
    }

    @Nullable
    @Override
    public String getSuperclass() {
        return superclass == null ? null : superclass.getType();
    }

    @Nullable
    @Override
    public String getSourceFile() {
        return sourceFile == null ? null : sourceFile.getString();
    }

    @NonNull
    @Override
    public BuilderAnnotationSet getAnnotations() {
        return annotations;
    }

    @NonNull
    @Override
    public SortedSet<BuilderField> getStaticFields() {
        return staticFields;
    }

    @NonNull
    @Override
    public SortedSet<BuilderField> getInstanceFields() {
        return instanceFields;
    }

    @NonNull
    @Override
    public SortedSet<BuilderMethod> getDirectMethods() {
        return directMethods;
    }

    @NonNull
    @Override
    public SortedSet<BuilderMethod> getVirtualMethods() {
        return virtualMethods;
    }

    @NonNull
    @Override
    public List<String> getInterfaces() {
        return Lists.transform(this.interfaces, Functions.toStringFunction());
    }

    @NonNull
    @Override
    public Collection<BuilderField> getFields() {
        return new AbstractCollection<BuilderField>() {
            @NonNull
            @Override
            public Iterator<BuilderField> iterator() {
                return Iterators.mergeSorted(
                        ImmutableList.of(staticFields.iterator(), instanceFields.iterator()),
                        Ordering.natural());
            }

            @Override
            public int size() {
                return staticFields.size() + instanceFields.size();
            }
        };
    }

    @NonNull
    @Override
    public Collection<BuilderMethod> getMethods() {
        return new AbstractCollection<BuilderMethod>() {
            @NonNull
            @Override
            public Iterator<BuilderMethod> iterator() {
                return Iterators.mergeSorted(
                        ImmutableList.of(directMethods.iterator(), virtualMethods.iterator()),
                        Ordering.natural());
            }

            @Override
            public int size() {
                return directMethods.size() + virtualMethods.size();
            }
        };
    }
}
