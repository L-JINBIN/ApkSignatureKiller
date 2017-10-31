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

import com.google.common.collect.Maps;

import org.jf.dexlib2.base.reference.BaseMethodReference;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.writer.MethodSection;

import android.support.annotation.NonNull;

import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;

class BuilderMethodPool extends BaseBuilderPool implements MethodSection<BuilderStringReference, BuilderTypeReference,
        BuilderMethodProtoReference, BuilderMethodReference, BuilderMethod> {
    @NonNull
    private final ConcurrentMap<MethodReference, BuilderMethodReference> internedItems =
            Maps.newConcurrentMap();

    public BuilderMethodPool(@NonNull DexBuilder dexBuilder) {
        super(dexBuilder);
    }

    @NonNull
    public BuilderMethodReference internMethod(@NonNull MethodReference methodReference) {
        BuilderMethodReference ret = internedItems.get(methodReference);
        if (ret != null) {
            return ret;
        }

        BuilderMethodReference dexPoolMethodReference = new BuilderMethodReference(
                dexBuilder.typeSection.internType(methodReference.getDefiningClass()),
                dexBuilder.stringSection.internString(methodReference.getName()),
                dexBuilder.protoSection.internMethodProto(methodReference));
        ret = internedItems.putIfAbsent(dexPoolMethodReference, dexPoolMethodReference);
        return ret == null ? dexPoolMethodReference : ret;
    }

    @NonNull
    public BuilderMethodReference internMethod(@NonNull String definingClass, @NonNull String name,
                                               @NonNull List<? extends CharSequence> parameters,
                                               @NonNull String returnType) {
        return internMethod(new MethodKey(definingClass, name, parameters, returnType));
    }

    @NonNull
    @Override
    public BuilderMethodReference getMethodReference(@NonNull BuilderMethod builderMethod) {
        return builderMethod.methodReference;
    }

    @NonNull
    @Override
    public BuilderTypeReference getDefiningClass(@NonNull BuilderMethodReference key) {
        return key.definingClass;
    }

    @NonNull
    @Override
    public BuilderMethodProtoReference getPrototype(@NonNull BuilderMethodReference key) {
        return key.proto;
    }

    @NonNull
    @Override
    public BuilderMethodProtoReference getPrototype(@NonNull BuilderMethod builderMethod) {
        return builderMethod.methodReference.proto;
    }

    @NonNull
    @Override
    public BuilderStringReference getName(@NonNull BuilderMethodReference key) {
        return key.name;
    }

    @Override
    public int getMethodIndex(@NonNull BuilderMethod builderMethod) {
        return builderMethod.methodReference.index;
    }

    @Override
    public int getItemIndex(@NonNull BuilderMethodReference key) {
        return key.index;
    }

    @NonNull
    @Override
    public Collection<? extends Entry<? extends BuilderMethodReference, Integer>> getItems() {
        return new BuilderMapEntryCollection<BuilderMethodReference>(internedItems.values()) {
            @Override
            protected int getValue(@NonNull BuilderMethodReference key) {
                return key.index;
            }

            @Override
            protected int setValue(@NonNull BuilderMethodReference key, int value) {
                int prev = key.index;
                key.index = value;
                return prev;
            }
        };
    }

    @Override
    public int getItemCount() {
        return internedItems.size();
    }

    private static class MethodKey extends BaseMethodReference implements MethodReference {
        @NonNull
        private final String definingClass;
        @NonNull
        private final String name;
        @NonNull
        private final List<? extends CharSequence> parameterTypes;
        @NonNull
        private final String returnType;

        public MethodKey(@NonNull String definingClass, @NonNull String name,
                         @NonNull List<? extends CharSequence> parameterTypes, @NonNull String returnType) {
            this.definingClass = definingClass;
            this.name = name;
            this.parameterTypes = parameterTypes;
            this.returnType = returnType;
        }

        @NonNull
        @Override
        public String getDefiningClass() {
            return definingClass;
        }

        @NonNull
        @Override
        public String getName() {
            return name;
        }

        @NonNull
        @Override
        public List<? extends CharSequence> getParameterTypes() {
            return parameterTypes;
        }

        @NonNull
        @Override
        public String getReturnType() {
            return returnType;
        }
    }
}
