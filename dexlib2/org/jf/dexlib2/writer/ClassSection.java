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

package org.jf.dexlib2.writer;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jf.dexlib2.builder.MutableMethodImplementation;
import org.jf.dexlib2.iface.ExceptionHandler;
import org.jf.dexlib2.iface.TryBlock;
import org.jf.dexlib2.iface.debug.DebugItem;
import org.jf.dexlib2.iface.instruction.Instruction;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ClassSection<StringKey extends CharSequence, TypeKey extends CharSequence, TypeListKey, ClassKey,
        FieldKey, MethodKey, AnnotationSetKey, EncodedValue> extends IndexSection<ClassKey> {
    @NonNull
    Collection<? extends ClassKey> getSortedClasses();

    @Nullable
    Map.Entry<? extends ClassKey, Integer> getClassEntryByType(@Nullable TypeKey key);

    @NonNull
    TypeKey getType(@NonNull ClassKey key);

    int getAccessFlags(@NonNull ClassKey key);

    @Nullable
    TypeKey getSuperclass(@NonNull ClassKey key);

    @Nullable
    TypeListKey getInterfaces(@NonNull ClassKey key);

    @Nullable
    StringKey getSourceFile(@NonNull ClassKey key);

    @Nullable
    Collection<? extends EncodedValue> getStaticInitializers(@NonNull ClassKey key);

    @NonNull
    Collection<? extends FieldKey> getSortedStaticFields(@NonNull ClassKey key);

    @NonNull
    Collection<? extends FieldKey> getSortedInstanceFields(@NonNull ClassKey key);

    @NonNull
    Collection<? extends FieldKey> getSortedFields(@NonNull ClassKey key);

    @NonNull
    Collection<? extends MethodKey> getSortedDirectMethods(@NonNull ClassKey key);

    @NonNull
    Collection<? extends MethodKey> getSortedVirtualMethods(@NonNull ClassKey key);

    @NonNull
    Collection<? extends MethodKey> getSortedMethods(@NonNull ClassKey key);

    int getFieldAccessFlags(@NonNull FieldKey key);

    int getMethodAccessFlags(@NonNull MethodKey key);

    @Nullable
    AnnotationSetKey getClassAnnotations(@NonNull ClassKey key);

    @Nullable
    AnnotationSetKey getFieldAnnotations(@NonNull FieldKey key);

    @Nullable
    AnnotationSetKey getMethodAnnotations(@NonNull MethodKey key);

    @Nullable
    List<? extends AnnotationSetKey> getParameterAnnotations(@NonNull MethodKey key);

    @Nullable
    Iterable<? extends DebugItem> getDebugItems(@NonNull MethodKey key);

    @Nullable
    Iterable<? extends StringKey> getParameterNames(@NonNull MethodKey key);

    int getRegisterCount(@NonNull MethodKey key);

    @Nullable
    Iterable<? extends Instruction> getInstructions(@NonNull MethodKey key);

    @NonNull
    List<? extends TryBlock<? extends ExceptionHandler>> getTryBlocks(@NonNull MethodKey key);

    @Nullable
    TypeKey getExceptionType(@NonNull ExceptionHandler handler);

    @NonNull
    MutableMethodImplementation makeMutableMethodImplementation(@NonNull MethodKey key);

    void setEncodedArrayOffset(@NonNull ClassKey key, int offset);

    int getEncodedArrayOffset(@NonNull ClassKey key);

    void setAnnotationDirectoryOffset(@NonNull ClassKey key, int offset);

    int getAnnotationDirectoryOffset(@NonNull ClassKey key);

    void setAnnotationSetRefListOffset(@NonNull MethodKey key, int offset);

    int getAnnotationSetRefListOffset(@NonNull MethodKey key);

    void setCodeItemOffset(@NonNull MethodKey key, int offset);

    int getCodeItemOffset(@NonNull MethodKey key);

    void writeDebugItem(@NonNull DebugWriter<StringKey, TypeKey> writer, DebugItem debugItem) throws IOException;
}
