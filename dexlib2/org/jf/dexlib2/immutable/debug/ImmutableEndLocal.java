

package org.jf.dexlib2.immutable.debug;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jf.dexlib2.DebugItemType;
import org.jf.dexlib2.iface.debug.EndLocal;

public class ImmutableEndLocal extends ImmutableDebugItem implements EndLocal {
    protected final int register;
    @Nullable
    protected final String name;
    @Nullable
    protected final String type;
    @Nullable
    protected final String signature;

    public ImmutableEndLocal(int codeAddress,
                             int register) {
        super(codeAddress);
        this.register = register;
        this.name = null;
        this.type = null;
        this.signature = null;
    }

    public ImmutableEndLocal(int codeAddress,
                             int register,
                             @Nullable String name,
                             @Nullable String type,
                             @Nullable String signature) {
        super(codeAddress);
        this.register = register;
        this.name = name;
        this.type = type;
        this.signature = signature;
    }

    @NonNull
    public static ImmutableEndLocal of(@NonNull EndLocal endLocal) {
        if (endLocal instanceof ImmutableEndLocal) {
            return (ImmutableEndLocal) endLocal;
        }
        return new ImmutableEndLocal(
                endLocal.getCodeAddress(),
                endLocal.getRegister(),
                endLocal.getType(),
                endLocal.getName(),
                endLocal.getSignature());
    }

    @Override
    public int getRegister() {
        return register;
    }

    @Nullable
    @Override
    public String getName() {
        return name;
    }

    @Nullable
    @Override
    public String getType() {
        return type;
    }

    @Nullable
    @Override
    public String getSignature() {
        return signature;
    }

    @Override
    public int getDebugItemType() {
        return DebugItemType.END_LOCAL;
    }
}
