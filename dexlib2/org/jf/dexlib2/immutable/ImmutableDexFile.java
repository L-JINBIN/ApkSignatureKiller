

package org.jf.dexlib2.immutable;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.collect.ImmutableSet;

import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.jf.util.ImmutableUtils;

import java.util.Collection;

public class ImmutableDexFile implements DexFile {
    @NonNull
    protected final ImmutableSet<? extends ImmutableClassDef> classes;
    @NonNull
    private final Opcodes opcodes;

    public ImmutableDexFile(@NonNull Opcodes opcodes, @Nullable Collection<? extends ClassDef> classes) {
        this.classes = ImmutableClassDef.immutableSetOf(classes);
        this.opcodes = opcodes;
    }

    public ImmutableDexFile(@NonNull Opcodes opcodes, @Nullable ImmutableSet<? extends ImmutableClassDef> classes) {
        this.classes = ImmutableUtils.nullToEmptySet(classes);
        this.opcodes = opcodes;
    }

    public static ImmutableDexFile of(DexFile dexFile) {
        if (dexFile instanceof ImmutableDexFile) {
            return (ImmutableDexFile) dexFile;
        }
        return new ImmutableDexFile(dexFile.getOpcodes(), dexFile.getClasses());
    }

    @NonNull
    @Override
    public ImmutableSet<? extends ImmutableClassDef> getClasses() {
        return classes;
    }

    @NonNull
    @Override
    public Opcodes getOpcodes() {
        return opcodes;
    }
}
