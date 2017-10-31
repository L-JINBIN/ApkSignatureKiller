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

package org.jf.dexlib2.writer.pool;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;

import org.jf.dexlib2.DebugItemType;
import org.jf.dexlib2.ReferenceType;
import org.jf.dexlib2.builder.MutableMethodImplementation;
import org.jf.dexlib2.iface.Annotation;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.ExceptionHandler;
import org.jf.dexlib2.iface.Field;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.MethodParameter;
import org.jf.dexlib2.iface.TryBlock;
import org.jf.dexlib2.iface.debug.DebugItem;
import org.jf.dexlib2.iface.debug.EndLocal;
import org.jf.dexlib2.iface.debug.LineNumber;
import org.jf.dexlib2.iface.debug.RestartLocal;
import org.jf.dexlib2.iface.debug.SetSourceFile;
import org.jf.dexlib2.iface.debug.StartLocal;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.ReferenceInstruction;
import org.jf.dexlib2.iface.reference.FieldReference;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.iface.reference.Reference;
import org.jf.dexlib2.iface.reference.StringReference;
import org.jf.dexlib2.iface.reference.TypeReference;
import org.jf.dexlib2.iface.value.EncodedValue;
import org.jf.dexlib2.immutable.value.ImmutableEncodedValueFactory;
import org.jf.dexlib2.util.EncodedValueUtils;
import org.jf.dexlib2.util.ReferenceUtil;
import org.jf.dexlib2.writer.ClassSection;
import org.jf.dexlib2.writer.DebugWriter;
import org.jf.util.AbstractForwardSequentialList;
import org.jf.util.CollectionUtils;
import org.jf.util.ExceptionWithContext;

import java.io.IOException;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;

public class ClassPool extends BasePool<String, PoolClassDef> implements ClassSection<CharSequence, CharSequence,
        TypeListPool.Key<? extends Collection<? extends CharSequence>>, PoolClassDef, Field, PoolMethod,
        Set<? extends Annotation>, EncodedValue> {

    public ClassPool(@NonNull DexPool dexPool) {
        super(dexPool);
    }

    public void intern(@NonNull ClassDef classDef) {
        PoolClassDef poolClassDef = new PoolClassDef(classDef);

        PoolClassDef prev = internedItems.put(poolClassDef.getType(), poolClassDef);
        if (prev != null) {
            throw new ExceptionWithContext("Class %s has already been interned", poolClassDef.getType());
        }

        dexPool.typeSection.intern(poolClassDef.getType());
        dexPool.typeSection.internNullable(poolClassDef.getSuperclass());
        dexPool.typeListSection.intern(poolClassDef.getInterfaces());
        dexPool.stringSection.internNullable(poolClassDef.getSourceFile());

        HashSet<String> fields = new HashSet<String>();
        for (Field field : poolClassDef.getFields()) {
            String fieldDescriptor = ReferenceUtil.getShortFieldDescriptor(field);
            if (!fields.add(fieldDescriptor)) {
                throw new ExceptionWithContext("Multiple definitions for field %s->%s",
                        poolClassDef.getType(), fieldDescriptor);
            }
            dexPool.fieldSection.intern(field);

            EncodedValue initialValue = field.getInitialValue();
            if (initialValue != null) {
                dexPool.internEncodedValue(initialValue);
            }

            dexPool.annotationSetSection.intern(field.getAnnotations());
        }

        HashSet<String> methods = new HashSet<String>();
        for (PoolMethod method : poolClassDef.getMethods()) {
            String methodDescriptor = ReferenceUtil.getMethodDescriptor(method, true);
            if (!methods.add(methodDescriptor)) {
                throw new ExceptionWithContext("Multiple definitions for method %s->%s",
                        poolClassDef.getType(), methodDescriptor);
            }
            dexPool.methodSection.intern(method);
            internCode(method);
            internDebug(method);
            dexPool.annotationSetSection.intern(method.getAnnotations());

            for (MethodParameter parameter : method.getParameters()) {
                dexPool.annotationSetSection.intern(parameter.getAnnotations());
            }
        }

        dexPool.annotationSetSection.intern(poolClassDef.getAnnotations());
    }

    private void internCode(@NonNull Method method) {
        // this also handles parameter names, which aren't directly tied to the MethodImplementation, even though the debug items are
        boolean hasInstruction = false;

        MethodImplementation methodImpl = method.getImplementation();
        if (methodImpl != null) {
            for (Instruction instruction : methodImpl.getInstructions()) {
                hasInstruction = true;
                if (instruction instanceof ReferenceInstruction) {
                    Reference reference = ((ReferenceInstruction) instruction).getReference();
                    switch (instruction.getOpcode().referenceType) {
                        case ReferenceType.STRING:
                            dexPool.stringSection.intern((StringReference)reference);
                            break;
                        case ReferenceType.TYPE:
                            dexPool.typeSection.intern((TypeReference)reference);
                            break;
                        case ReferenceType.FIELD:
                            dexPool.fieldSection.intern((FieldReference) reference);
                            break;
                        case ReferenceType.METHOD:
                            dexPool.methodSection.intern((MethodReference)reference);
                            break;
                        default:
                            throw new ExceptionWithContext("Unrecognized reference type: %d",
                                    instruction.getOpcode().referenceType);
                    }
                }
            }

            List<? extends TryBlock> tryBlocks = methodImpl.getTryBlocks();
            if (!hasInstruction && tryBlocks.size() > 0) {
                throw new ExceptionWithContext("Method %s has no instructions, but has try blocks.",
                        ReferenceUtil.getMethodDescriptor(method));
            }

            for (TryBlock<? extends ExceptionHandler> tryBlock : methodImpl.getTryBlocks()) {
                for (ExceptionHandler handler : tryBlock.getExceptionHandlers()) {
                    dexPool.typeSection.internNullable(handler.getExceptionType());
                }
            }
        }
    }

    private void internDebug(@NonNull Method method) {
        for (MethodParameter param : method.getParameters()) {
            String paramName = param.getName();
            if (paramName != null) {
                dexPool.stringSection.intern(paramName);
            }
        }

        MethodImplementation methodImpl = method.getImplementation();
        if (methodImpl != null) {
            for (DebugItem debugItem : methodImpl.getDebugItems()) {
                switch (debugItem.getDebugItemType()) {
                    case DebugItemType.START_LOCAL:
                        StartLocal startLocal = (StartLocal) debugItem;
                        dexPool.stringSection.internNullable(startLocal.getName());
                        dexPool.typeSection.internNullable(startLocal.getType());
                        dexPool.stringSection.internNullable(startLocal.getSignature());
                        break;
                    case DebugItemType.SET_SOURCE_FILE:
                        dexPool.stringSection.internNullable(((SetSourceFile) debugItem).getSourceFile());
                        break;
                }
            }
        }
    }

    private ImmutableList<PoolClassDef> sortedClasses = null;

    @NonNull
    @Override
    public Collection<? extends PoolClassDef> getSortedClasses() {
        if (sortedClasses == null) {
            sortedClasses = Ordering.natural().immutableSortedCopy(internedItems.values());
        }
        return sortedClasses;
    }

    @Nullable
    @Override
    public Map.Entry<? extends PoolClassDef, Integer> getClassEntryByType(@Nullable CharSequence name) {
        if (name == null) {
            return null;
        }

        final PoolClassDef classDef = internedItems.get(name.toString());
        if (classDef == null) {
            return null;
        }

        return new Map.Entry<PoolClassDef, Integer>() {
            @Override
            public PoolClassDef getKey() {
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
    public CharSequence getType(@NonNull PoolClassDef classDef) {
        return classDef.getType();
    }

    @Override
    public int getAccessFlags(@NonNull PoolClassDef classDef) {
        return classDef.getAccessFlags();
    }

    @Nullable
    @Override
    public CharSequence getSuperclass(@NonNull PoolClassDef classDef) {
        return classDef.getSuperclass();
    }

    @Nullable
    @Override
    public TypeListPool.Key<List<String>> getInterfaces(@NonNull PoolClassDef classDef) {
        return classDef.interfaces;
    }

    @Nullable
    @Override
    public CharSequence getSourceFile(@NonNull PoolClassDef classDef) {
        return classDef.getSourceFile();
    }

    private static final Predicate<Field> HAS_INITIALIZER = new Predicate<Field>() {
        @Override
        public boolean apply(Field input) {
            EncodedValue encodedValue = input.getInitialValue();
            return encodedValue != null && !EncodedValueUtils.isDefaultValue(encodedValue);
        }
    };

    private static final Function<Field, EncodedValue> GET_INITIAL_VALUE = new Function<Field, EncodedValue>() {
        @Override
        public EncodedValue apply(Field input) {
            EncodedValue initialValue = input.getInitialValue();
            if (initialValue == null) {
                return ImmutableEncodedValueFactory.defaultValueForType(input.getType());
            }
            return initialValue;
        }
    };

    @Nullable
    @Override
    public Collection<? extends EncodedValue> getStaticInitializers(
            @NonNull PoolClassDef classDef) {
        final SortedSet<Field> sortedStaticFields = classDef.getStaticFields();

        final int lastIndex = CollectionUtils.lastIndexOf(sortedStaticFields, HAS_INITIALIZER);
        if (lastIndex > -1) {
            return new AbstractCollection<EncodedValue>() {
                @NonNull
                @Override
                public Iterator<EncodedValue> iterator() {
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
    public Collection<? extends Field> getSortedStaticFields(@NonNull PoolClassDef classDef) {
        return classDef.getStaticFields();
    }

    @NonNull
    @Override
    public Collection<? extends Field> getSortedInstanceFields(@NonNull PoolClassDef classDef) {
        return classDef.getInstanceFields();
    }

    @NonNull
    @Override
    public Collection<? extends Field> getSortedFields(@NonNull PoolClassDef classDef) {
        return classDef.getFields();
    }

    @NonNull
    @Override
    public Collection<PoolMethod> getSortedDirectMethods(@NonNull PoolClassDef classDef) {
        return classDef.getDirectMethods();
    }

    @NonNull
    @Override
    public Collection<PoolMethod> getSortedVirtualMethods(@NonNull PoolClassDef classDef) {
        return classDef.getVirtualMethods();
    }

    @NonNull
    @Override
    public Collection<? extends PoolMethod> getSortedMethods(@NonNull PoolClassDef classDef) {
        return classDef.getMethods();
    }

    @Override
    public int getFieldAccessFlags(@NonNull Field field) {
        return field.getAccessFlags();
    }

    @Override
    public int getMethodAccessFlags(@NonNull PoolMethod method) {
        return method.getAccessFlags();
    }

    @Nullable
    @Override
    public Set<? extends Annotation> getClassAnnotations(@NonNull PoolClassDef classDef) {
        Set<? extends Annotation> annotations = classDef.getAnnotations();
        if (annotations.size() == 0) {
            return null;
        }
        return annotations;
    }

    @Nullable
    @Override
    public Set<? extends Annotation> getFieldAnnotations(@NonNull Field field) {
        Set<? extends Annotation> annotations = field.getAnnotations();
        if (annotations.size() == 0) {
            return null;
        }
        return annotations;
    }

    @Nullable
    @Override
    public Set<? extends Annotation> getMethodAnnotations(@NonNull PoolMethod method) {
        Set<? extends Annotation> annotations = method.getAnnotations();
        if (annotations.size() == 0) {
            return null;
        }
        return annotations;
    }

    private static final Predicate<MethodParameter> HAS_PARAMETER_ANNOTATIONS = new Predicate<MethodParameter>() {
        @Override
        public boolean apply(MethodParameter input) {
            return input.getAnnotations().size() > 0;
        }
    };

    private static final Function<MethodParameter, Set<? extends Annotation>> PARAMETER_ANNOTATIONS =
            new Function<MethodParameter, Set<? extends Annotation>>() {
                @Override
                public Set<? extends Annotation> apply(MethodParameter input) {
                    return input.getAnnotations();
                }
            };

    @Nullable
    @Override
    public List<? extends Set<? extends Annotation>> getParameterAnnotations(
            @NonNull final PoolMethod method) {
        final List<? extends MethodParameter> parameters = method.getParameters();
        boolean hasParameterAnnotations = Iterables.any(parameters, HAS_PARAMETER_ANNOTATIONS);

        if (hasParameterAnnotations) {
            return new AbstractForwardSequentialList<Set<? extends Annotation>>() {
                @NonNull
                @Override
                public Iterator<Set<? extends Annotation>> iterator() {
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
    public Iterable<? extends DebugItem> getDebugItems(@NonNull PoolMethod method) {
        MethodImplementation impl = method.getImplementation();
        if (impl != null) {
            return impl.getDebugItems();
        }
        return null;
    }

    @Nullable
    @Override
    public Iterable<CharSequence> getParameterNames(@NonNull PoolMethod method) {
        return Iterables.transform(method.getParameters(), new Function<MethodParameter, CharSequence>() {
            @Nullable
            @Override
            public CharSequence apply(MethodParameter input) {
                return input.getName();
            }
        });
    }

    @Override
    public int getRegisterCount(@NonNull PoolMethod method) {
        MethodImplementation impl = method.getImplementation();
        if (impl != null) {
            return impl.getRegisterCount();
        }
        return 0;
    }

    @Nullable
    @Override
    public Iterable<? extends Instruction> getInstructions(@NonNull PoolMethod method) {
        MethodImplementation impl = method.getImplementation();
        if (impl != null) {
            return impl.getInstructions();
        }
        return null;
    }

    @NonNull
    @Override
    public List<? extends TryBlock<? extends ExceptionHandler>> getTryBlocks(
            @NonNull PoolMethod method) {
        MethodImplementation impl = method.getImplementation();
        if (impl != null) {
            return impl.getTryBlocks();
        }
        return ImmutableList.of();
    }

    @Nullable
    @Override
    public CharSequence getExceptionType(@NonNull ExceptionHandler handler) {
        return handler.getExceptionType();
    }

    @NonNull
    @Override
    public MutableMethodImplementation makeMutableMethodImplementation(@NonNull PoolMethod poolMethod) {
        return new MutableMethodImplementation(poolMethod.getImplementation());
    }

    @Override
    public void setEncodedArrayOffset(@NonNull PoolClassDef classDef, int offset) {
        classDef.encodedArrayOffset = offset;
    }

    @Override
    public int getEncodedArrayOffset(@NonNull PoolClassDef classDef) {
        return classDef.encodedArrayOffset;
    }

    @Override
    public void setAnnotationDirectoryOffset(@NonNull PoolClassDef classDef, int offset) {
        classDef.annotationDirectoryOffset = offset;
    }

    @Override
    public int getAnnotationDirectoryOffset(@NonNull PoolClassDef classDef) {
        return classDef.annotationDirectoryOffset;
    }

    @Override
    public void setAnnotationSetRefListOffset(@NonNull PoolMethod method, int offset) {
        method.annotationSetRefListOffset = offset;

    }

    @Override
    public int getAnnotationSetRefListOffset(@NonNull PoolMethod method) {
        return method.annotationSetRefListOffset;
    }

    @Override
    public void setCodeItemOffset(@NonNull PoolMethod method, int offset) {
        method.codeItemOffset = offset;
    }

    @Override
    public int getCodeItemOffset(@NonNull PoolMethod method) {
        return method.codeItemOffset;
    }

    @Override
    public void writeDebugItem(@NonNull DebugWriter<CharSequence, CharSequence> writer,
                               DebugItem debugItem) throws IOException {
        switch (debugItem.getDebugItemType()) {
            case DebugItemType.START_LOCAL: {
                StartLocal startLocal = (StartLocal) debugItem;
                writer.writeStartLocal(startLocal.getCodeAddress(),
                        startLocal.getRegister(),
                        startLocal.getName(),
                        startLocal.getType(),
                        startLocal.getSignature());
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
                writer.writeSetSourceFile(setSourceFile.getCodeAddress(), setSourceFile.getSourceFile());
            }
            default:
                throw new ExceptionWithContext("Unexpected debug item type: %d", debugItem.getDebugItemType());
        }
    }

    @Override
    public int getItemIndex(@NonNull PoolClassDef classDef) {
        return classDef.classDefIndex;
    }

    @NonNull
    @Override
    public Collection<? extends Map.Entry<PoolClassDef, Integer>> getItems() {
        class MapEntry implements Map.Entry<PoolClassDef, Integer> {
            @NonNull
            private final PoolClassDef classDef;

            public MapEntry(@NonNull PoolClassDef classDef) {
                this.classDef = classDef;
            }

            @Override
            public PoolClassDef getKey() {
                return classDef;
            }

            @Override
            public Integer getValue() {
                return classDef.classDefIndex;
            }

            @Override
            public Integer setValue(Integer value) {
                int prev = classDef.classDefIndex;
                classDef.classDefIndex = value;
                return prev;
            }
        }

        return new AbstractCollection<Entry<PoolClassDef, Integer>>() {
            @NonNull
            @Override
            public Iterator<Entry<PoolClassDef, Integer>> iterator() {
                return new Iterator<Entry<PoolClassDef, Integer>>() {
                    Iterator<PoolClassDef> iter = internedItems.values().iterator();

                    @Override
                    public boolean hasNext() {
                        return iter.hasNext();
                    }

                    @Override
                    public Entry<PoolClassDef, Integer> next() {
                        return new MapEntry(iter.next());
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }

            @Override
            public int size() {
                return internedItems.size();
            }
        };
    }
}
