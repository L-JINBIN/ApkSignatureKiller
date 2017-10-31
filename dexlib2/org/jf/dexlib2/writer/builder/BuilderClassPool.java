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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;

import org.jf.dexlib2.DebugItemType;
import org.jf.dexlib2.builder.MutableMethodImplementation;
import org.jf.dexlib2.iface.ExceptionHandler;
import org.jf.dexlib2.iface.Field;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.TryBlock;
import org.jf.dexlib2.iface.debug.DebugItem;
import org.jf.dexlib2.iface.debug.EndLocal;
import org.jf.dexlib2.iface.debug.LineNumber;
import org.jf.dexlib2.iface.debug.RestartLocal;
import org.jf.dexlib2.iface.debug.SetSourceFile;
import org.jf.dexlib2.iface.debug.StartLocal;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.reference.StringReference;
import org.jf.dexlib2.iface.reference.TypeReference;
import org.jf.dexlib2.iface.value.EncodedValue;
import org.jf.dexlib2.util.EncodedValueUtils;
import org.jf.dexlib2.writer.ClassSection;
import org.jf.dexlib2.writer.DebugWriter;
import org.jf.dexlib2.writer.builder.BuilderEncodedValues.BuilderEncodedValue;
import org.jf.util.AbstractForwardSequentialList;
import org.jf.util.CollectionUtils;
import org.jf.util.ExceptionWithContext;

import java.io.IOException;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentMap;

public class BuilderClassPool extends BaseBuilderPool implements ClassSection<BuilderStringReference,
        BuilderTypeReference, BuilderTypeList, BuilderClassDef, BuilderField, BuilderMethod, BuilderAnnotationSet,
        BuilderEncodedValue> {
    @NonNull
    private final ConcurrentMap<String, BuilderClassDef> internedItems =
            Maps.newConcurrentMap();

    public BuilderClassPool(@NonNull DexBuilder dexBuilder) {
        super(dexBuilder);
    }

    @NonNull
    BuilderClassDef internClass(@NonNull BuilderClassDef classDef) {
        BuilderClassDef prev = internedItems.put(classDef.getType(), classDef);
        if (prev != null) {
            throw new ExceptionWithContext("Class %s has already been interned", classDef.getType());
        }
        return classDef;
    }

    private ImmutableList<BuilderClassDef> sortedClasses = null;

    @NonNull
    @Override
    public Collection<? extends BuilderClassDef> getSortedClasses() {
        if (sortedClasses == null) {
            sortedClasses = Ordering.natural().immutableSortedCopy(internedItems.values());
        }
        return sortedClasses;
    }

    @Nullable
    @Override
    public Entry<? extends BuilderClassDef, Integer> getClassEntryByType(@Nullable BuilderTypeReference type) {
        if (type == null) {
            return null;
        }

        final BuilderClassDef classDef = internedItems.get(type.getType());
        if (classDef == null) {
            return null;
        }

        return new Map.Entry<BuilderClassDef, Integer>() {
            @Override
            public BuilderClassDef getKey() {
                return classDef;
            }

            @Override
            public Integer getValue() {
                return classDef.classDefIndex;
            }

            @Override
            public Integer setValue(Integer value) {
                return classDef.classDefIndex = value;
            }
        };
    }

    @NonNull
    @Override
    public BuilderTypeReference getType(@NonNull BuilderClassDef builderClassDef) {
        return builderClassDef.type;
    }

    @Override
    public int getAccessFlags(@NonNull BuilderClassDef builderClassDef) {
        return builderClassDef.accessFlags;
    }

    @Nullable
    @Override
    public BuilderTypeReference getSuperclass(@NonNull BuilderClassDef builderClassDef) {
        return builderClassDef.superclass;
    }

    @Nullable
    @Override
    public BuilderTypeList getInterfaces(@NonNull BuilderClassDef builderClassDef) {
        return builderClassDef.interfaces;
    }

    @Nullable
    @Override
    public BuilderStringReference getSourceFile(@NonNull BuilderClassDef builderClassDef) {
        return builderClassDef.sourceFile;
    }

    private static final Predicate<Field> HAS_INITIALIZER = new Predicate<Field>() {
        @Override
        public boolean apply(Field input) {
            EncodedValue encodedValue = input.getInitialValue();
            return encodedValue != null && !EncodedValueUtils.isDefaultValue(encodedValue);
        }
    };

    private static final Function<BuilderField, BuilderEncodedValue> GET_INITIAL_VALUE =
            new Function<BuilderField, BuilderEncodedValue>() {
                @Override
                public BuilderEncodedValue apply(BuilderField input) {
                    BuilderEncodedValue initialValue = input.getInitialValue();
                    if (initialValue == null) {
                        return BuilderEncodedValues.defaultValueForType(input.getType());
                    }
                    return initialValue;
                }
            };

    @Nullable
    @Override
    public Collection<? extends BuilderEncodedValue> getStaticInitializers(@NonNull BuilderClassDef classDef) {
        final SortedSet<BuilderField> sortedStaticFields = classDef.getStaticFields();

        final int lastIndex = CollectionUtils.lastIndexOf(sortedStaticFields, HAS_INITIALIZER);
        if (lastIndex > -1) {
            return new AbstractCollection<BuilderEncodedValue>() {
                @NonNull
                @Override
                public Iterator<BuilderEncodedValue> iterator() {
                    return FluentIterable.from(sortedStaticFields)
                            .limit(lastIndex + 1)
                            .transform(GET_INITIAL_VALUE).iterator();
                }

                @Override
                public int size() {
                    return lastIndex + 1;
                }
            };
        }
        return null;
    }

    @NonNull
    @Override
    public Collection<? extends BuilderField> getSortedStaticFields(@NonNull BuilderClassDef builderClassDef) {
        return builderClassDef.getStaticFields();
    }

    @NonNull
    @Override
    public Collection<? extends BuilderField> getSortedInstanceFields(@NonNull BuilderClassDef builderClassDef) {
        return builderClassDef.getInstanceFields();
    }

    @NonNull
    @Override
    public Collection<? extends BuilderField> getSortedFields(@NonNull BuilderClassDef builderClassDef) {
        return builderClassDef.getFields();
    }

    @NonNull
    @Override
    public Collection<? extends BuilderMethod> getSortedDirectMethods(@NonNull BuilderClassDef builderClassDef) {
        return builderClassDef.getDirectMethods();
    }

    @NonNull
    @Override
    public Collection<? extends BuilderMethod> getSortedVirtualMethods(@NonNull BuilderClassDef builderClassDef) {
        return builderClassDef.getVirtualMethods();
    }

    @NonNull
    @Override
    public Collection<? extends BuilderMethod> getSortedMethods(@NonNull BuilderClassDef builderClassDef) {
        return builderClassDef.getMethods();
    }

    @Override
    public int getFieldAccessFlags(@NonNull BuilderField builderField) {
        return builderField.accessFlags;
    }

    @Override
    public int getMethodAccessFlags(@NonNull BuilderMethod builderMethod) {
        return builderMethod.accessFlags;
    }

    @Nullable
    @Override
    public BuilderAnnotationSet getClassAnnotations(@NonNull BuilderClassDef builderClassDef) {
        if (builderClassDef.annotations.isEmpty()) {
            return null;
        }
        return builderClassDef.annotations;
    }

    @Nullable
    @Override
    public BuilderAnnotationSet getFieldAnnotations(@NonNull BuilderField builderField) {
        if (builderField.annotations.isEmpty()) {
            return null;
        }
        return builderField.annotations;
    }

    @Nullable
    @Override
    public BuilderAnnotationSet getMethodAnnotations(@NonNull BuilderMethod builderMethod) {
        if (builderMethod.annotations.isEmpty()) {
            return null;
        }
        return builderMethod.annotations;
    }

    private static final Predicate<BuilderMethodParameter> HAS_PARAMETER_ANNOTATIONS =
            new Predicate<BuilderMethodParameter>() {
                @Override
                public boolean apply(BuilderMethodParameter input) {
                    return input.getAnnotations().size() > 0;
                }
            };

    private static final Function<BuilderMethodParameter, BuilderAnnotationSet> PARAMETER_ANNOTATIONS =
            new Function<BuilderMethodParameter, BuilderAnnotationSet>() {
                @Override
                public BuilderAnnotationSet apply(BuilderMethodParameter input) {
                    return input.getAnnotations();
                }
            };

    @Nullable
    @Override
    public List<? extends BuilderAnnotationSet> getParameterAnnotations(
            @NonNull final BuilderMethod method) {
        final List<? extends BuilderMethodParameter> parameters = method.getParameters();
        boolean hasParameterAnnotations = Iterables.any(parameters, HAS_PARAMETER_ANNOTATIONS);

        if (hasParameterAnnotations) {
            return new AbstractForwardSequentialList<BuilderAnnotationSet>() {
                @NonNull
                @Override
                public Iterator<BuilderAnnotationSet> iterator() {
                    return FluentIterable.from(parameters)
                            .transform(PARAMETER_ANNOTATIONS).iterator();
                }

                @Override
                public int size() {
                    return parameters.size();
                }
            };
        }
        return null;
    }

    @Nullable
    @Override
    public Iterable<? extends DebugItem> getDebugItems(@NonNull BuilderMethod builderMethod) {
        MethodImplementation impl = builderMethod.getImplementation();
        if (impl == null) {
            return null;
        }
        return impl.getDebugItems();
    }

    @Nullable
    @Override
    public Iterable<? extends BuilderStringReference> getParameterNames(@NonNull BuilderMethod method) {
        return Iterables.transform(method.getParameters(), new Function<BuilderMethodParameter, BuilderStringReference>() {
            @Nullable
            @Override
            public BuilderStringReference apply(BuilderMethodParameter input) {
                return input.name;
            }
        });
    }

    @Override
    public int getRegisterCount(@NonNull BuilderMethod builderMethod) {
        MethodImplementation impl = builderMethod.getImplementation();
        if (impl == null) {
            return 0;
        }
        return impl.getRegisterCount();
    }

    @Nullable
    @Override
    public Iterable<? extends Instruction> getInstructions(@NonNull BuilderMethod builderMethod) {
        MethodImplementation impl = builderMethod.getImplementation();
        if (impl == null) {
            return null;
        }
        return impl.getInstructions();
    }

    @NonNull
    @Override
    public List<? extends TryBlock<? extends ExceptionHandler>> getTryBlocks(@NonNull BuilderMethod builderMethod) {
        MethodImplementation impl = builderMethod.getImplementation();
        if (impl == null) {
            return ImmutableList.of();
        }
        return impl.getTryBlocks();
    }

    @Nullable
    @Override
    public BuilderTypeReference getExceptionType(@NonNull ExceptionHandler handler) {
        return checkTypeReference(handler.getExceptionTypeReference());
    }

    @NonNull
    @Override
    public MutableMethodImplementation makeMutableMethodImplementation(@NonNull BuilderMethod builderMethod) {
        MethodImplementation impl = builderMethod.getImplementation();
        if (impl instanceof MutableMethodImplementation) {
            return (MutableMethodImplementation) impl;
        }
        return new MutableMethodImplementation(impl);
    }

    @Override
    public void setEncodedArrayOffset(@NonNull BuilderClassDef builderClassDef, int offset) {
        builderClassDef.encodedArrayOffset = offset;
    }

    @Override
    public int getEncodedArrayOffset(@NonNull BuilderClassDef builderClassDef) {
        return builderClassDef.encodedArrayOffset;
    }

    @Override
    public void setAnnotationDirectoryOffset(@NonNull BuilderClassDef builderClassDef, int offset) {
        builderClassDef.annotationDirectoryOffset = offset;
    }

    @Override
    public int getAnnotationDirectoryOffset(@NonNull BuilderClassDef builderClassDef) {
        return builderClassDef.annotationDirectoryOffset;
    }

    @Override
    public void setAnnotationSetRefListOffset(@NonNull BuilderMethod builderMethod, int offset) {
        builderMethod.annotationSetRefListOffset = offset;
    }

    @Override
    public int getAnnotationSetRefListOffset(@NonNull BuilderMethod builderMethod) {
        return builderMethod.annotationSetRefListOffset;
    }

    @Override
    public void setCodeItemOffset(@NonNull BuilderMethod builderMethod, int offset) {
        builderMethod.codeItemOffset = offset;
    }

    @Override
    public int getCodeItemOffset(@NonNull BuilderMethod builderMethod) {
        return builderMethod.codeItemOffset;
    }

    @Nullable
    private BuilderStringReference checkStringReference(@Nullable StringReference stringReference) {
        if (stringReference == null) {
            return null;
        }
        try {
            return (BuilderStringReference) stringReference;
        } catch (ClassCastException ex) {
            throw new IllegalStateException("Only StringReference instances returned by " +
                    "DexBuilder.internStringReference or DexBuilder.internNullableStringReference may be used.");
        }
    }

    @Nullable
    private BuilderTypeReference checkTypeReference(@Nullable TypeReference typeReference) {
        if (typeReference == null) {
            return null;
        }
        try {
            return (BuilderTypeReference) typeReference;
        } catch (ClassCastException ex) {
            throw new IllegalStateException("Only TypeReference instances returned by " +
                    "DexBuilder.internTypeReference or DexBuilder.internNullableTypeReference may be used.");
        }
    }

    @Override
    public void writeDebugItem(@NonNull DebugWriter<BuilderStringReference, BuilderTypeReference> writer,
                               DebugItem debugItem) throws IOException {
        switch (debugItem.getDebugItemType()) {
            case DebugItemType.START_LOCAL: {
                StartLocal startLocal = (StartLocal) debugItem;
                writer.writeStartLocal(startLocal.getCodeAddress(),
                        startLocal.getRegister(),
                        checkStringReference(startLocal.getNameReference()),
                        checkTypeReference(startLocal.getTypeReference()),
                        checkStringReference(startLocal.getSignatureReference()));
                break;
            }
            case DebugItemType.END_LOCAL: {
                EndLocal endLocal = (EndLocal) debugItem;
                writer.writeEndLocal(endLocal.getCodeAddress(), endLocal.getRegister());
                break;
            }
            case DebugItemType.RESTART_LOCAL: {
                RestartLocal restartLocal = (RestartLocal) debugItem;
                writer.writeRestartLocal(restartLocal.getCodeAddress(), restartLocal.getRegister());
                break;
            }
            case DebugItemType.PROLOGUE_END: {
                writer.writePrologueEnd(debugItem.getCodeAddress());
                break;
            }
            case DebugItemType.EPILOGUE_BEGIN: {
                writer.writeEpilogueBegin(debugItem.getCodeAddress());
                break;
            }
            case DebugItemType.LINE_NUMBER: {
                LineNumber lineNumber = (LineNumber) debugItem;
                writer.writeLineNumber(lineNumber.getCodeAddress(), lineNumber.getLineNumber());
                break;
            }
            case DebugItemType.SET_SOURCE_FILE: {
                SetSourceFile setSourceFile = (SetSourceFile) debugItem;
                writer.writeSetSourceFile(setSourceFile.getCodeAddress(),
                        checkStringReference(setSourceFile.getSourceFileReference()));
                break;
            }
            default:
                throw new ExceptionWithContext("Unexpected debug item type: %d", debugItem.getDebugItemType());
        }
    }

    @Override
    public int getItemIndex(@NonNull BuilderClassDef builderClassDef) {
        return builderClassDef.classDefIndex;
    }

    @NonNull
    @Override
    public Collection<? extends Entry<? extends BuilderClassDef, Integer>> getItems() {
        return new BuilderMapEntryCollection<BuilderClassDef>(internedItems.values()) {
            @Override
            protected int getValue(@NonNull BuilderClassDef key) {
                return key.classDefIndex;
            }

            @Override
            protected int setValue(@NonNull BuilderClassDef key, int value) {
                int prev = key.classDefIndex;
                key.classDefIndex = value;
                return prev;
            }
        };
    }

    @Override
    public int getItemCount() {
        return internedItems.size();
    }
}
