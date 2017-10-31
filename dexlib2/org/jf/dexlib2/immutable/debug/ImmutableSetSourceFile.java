

package org.jf.dexlib2.immutable.debug;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jf.dexlib2.DebugItemType;
import org.jf.dexlib2.base.reference.BaseStringReference;
import org.jf.dexlib2.iface.UpdateReference;
import org.jf.dexlib2.iface.debug.SetSourceFile;
import org.jf.dexlib2.iface.reference.StringReference;
import org.jf.dexlib2.writer.builder.DexBuilder;

public class ImmutableSetSourceFile extends ImmutableDebugItem implements SetSourceFile, UpdateReference {
    @Nullable
    protected final String sourceFile;

    public ImmutableSetSourceFile(int codeAddress,
                                  @Nullable String sourceFile) {
        super(codeAddress);
        this.sourceFile = sourceFile;
    }

    @NonNull
    public static ImmutableSetSourceFile of(@NonNull SetSourceFile setSourceFile) {
        if (setSourceFile instanceof ImmutableSetSourceFile) {
            return (ImmutableSetSourceFile) setSourceFile;
        }
        return new ImmutableSetSourceFile(
                setSourceFile.getCodeAddress(),
                setSourceFile.getSourceFile());
    }

    @Nullable
    @Override
    public String getSourceFile() {
        return sourceFile;
    }

    @Nullable
    @Override
    public StringReference getSourceFileReference() {
        if (sourceFileRef != null)
            return sourceFileRef;
        return sourceFile == null ? null : new BaseStringReference() {
            @NonNull
            @Override
            public String getString() {
                return sourceFile;
            }
        };
    }


    @Override
    public int getDebugItemType() {
        return DebugItemType.SET_SOURCE_FILE;
    }

    @Nullable
    private StringReference sourceFileRef;

    @Override
    public void updateReference(DexBuilder dexBuilder) {
        sourceFileRef = dexBuilder.internNullableStringReference(sourceFile);
    }
}
