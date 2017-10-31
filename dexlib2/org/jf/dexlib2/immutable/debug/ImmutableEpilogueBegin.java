

package org.jf.dexlib2.immutable.debug;

import android.support.annotation.NonNull;

import org.jf.dexlib2.DebugItemType;
import org.jf.dexlib2.iface.debug.EpilogueBegin;

public class ImmutableEpilogueBegin extends ImmutableDebugItem implements EpilogueBegin {
    public ImmutableEpilogueBegin(int codeAddress) {
        super(codeAddress);
    }

    @NonNull
    public static ImmutableEpilogueBegin of(@NonNull EpilogueBegin epilogueBegin) {
        if (epilogueBegin instanceof ImmutableEpilogueBegin) {
            return (ImmutableEpilogueBegin) epilogueBegin;
        }
        return new ImmutableEpilogueBegin(epilogueBegin.getCodeAddress());
    }

    @Override
    public int getDebugItemType() {
        return DebugItemType.EPILOGUE_BEGIN;
    }
}
