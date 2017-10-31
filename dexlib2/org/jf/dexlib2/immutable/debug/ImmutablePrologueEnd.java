

package org.jf.dexlib2.immutable.debug;

import android.support.annotation.NonNull;

import org.jf.dexlib2.DebugItemType;
import org.jf.dexlib2.iface.debug.PrologueEnd;

public class ImmutablePrologueEnd extends ImmutableDebugItem implements PrologueEnd {
    public ImmutablePrologueEnd(int codeAddress) {
        super(codeAddress);
    }

    @NonNull
    public static ImmutablePrologueEnd of(@NonNull PrologueEnd prologueEnd) {
        if (prologueEnd instanceof ImmutablePrologueEnd) {
            return (ImmutablePrologueEnd) prologueEnd;
        }
        return new ImmutablePrologueEnd(prologueEnd.getCodeAddress());
    }

    @Override
    public int getDebugItemType() {
        return DebugItemType.PROLOGUE_END;
    }
}
