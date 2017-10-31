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

import org.jf.dexlib2.iface.Annotation;
import org.jf.dexlib2.iface.AnnotationElement;
import org.jf.dexlib2.iface.value.EncodedValue;
import org.jf.dexlib2.writer.AnnotationSection;

import java.util.Collection;

public class AnnotationPool extends BaseOffsetPool<Annotation>
        implements AnnotationSection<CharSequence, CharSequence, Annotation, AnnotationElement, EncodedValue> {
    public AnnotationPool(@NonNull DexPool dexPool) {
        super(dexPool);
    }

    public void intern(@NonNull Annotation annotation) {
        Integer prev = internedItems.put(annotation, 0);
        if (prev == null) {
            dexPool.typeSection.intern(annotation.getType());
            for (AnnotationElement element : annotation.getElements()) {
                dexPool.stringSection.intern(element.getName());
                dexPool.internEncodedValue(element.getValue());
            }
        }
    }

    @Override
    public int getVisibility(@NonNull Annotation annotation) {
        return annotation.getVisibility();
    }

    @NonNull
    @Override
    public CharSequence getType(@NonNull Annotation annotation) {
        return annotation.getType();
    }

    @NonNull
    @Override
    public Collection<? extends AnnotationElement> getElements(@NonNull Annotation annotation) {
        return annotation.getElements();
    }

    @NonNull
    @Override
    public CharSequence getElementName(@NonNull AnnotationElement annotationElement) {
        return annotationElement.getName();
    }

    @NonNull
    @Override
    public EncodedValue getElementValue(@NonNull AnnotationElement annotationElement) {
        return annotationElement.getValue();
    }
}
