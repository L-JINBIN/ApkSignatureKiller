

package org.jf.dexlib2.immutable.debug;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jf.dexlib2.DebugItemType;
import org.jf.dexlib2.iface.debug.RestartLocal;

public class ImmutableRestartLocal extends ImmutableDebugItem implements RestartLocal {
    protected final int register;
    @Nullable
    protected final String name;
    @Nullable
    protected final String type;
    @Nullable
    protected final String signature;

    public ImmutableRestartLocal(int codeAddress,
                                 int register) {
        super(codeAddress);
        this.register = register;
        this.name = null;
        this.type = null;
        this.signature = null;
    }

    public ImmutableRestartLocal(int codeAddress,
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
    public static ImmutableRestartLocal of(@NonNull RestartLocal restartLocal) {
        if (restartLocal instanceof ImmutableRestartLocal) {
            return (ImmutableRestartLocal) restartLocal;
        }
        return new ImmutableRestartLocal(
                restartLocal.getCodeAddress(),
                restartLocal.getRegister(),
                restartLocal.getType(),
                restartLocal.getName(),
                restartLocal.getSignature());
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
        return DebugItemType.RESTART_LOCAL;
    }
}
