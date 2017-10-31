package org.jf.dexlib2.builder.instruction;

import android.support.annotation.NonNull;

import org.jf.dexlib2.builder.BuilderSwitchPayload;
import org.jf.dexlib2.builder.Label;
import org.jf.dexlib2.iface.instruction.SwitchElement;

public class BuilderSwitchElement implements SwitchElement {
    @NonNull
    BuilderSwitchPayload parent;
    private final int key;
    @NonNull
    private final Label target;

    public BuilderSwitchElement(@NonNull BuilderSwitchPayload parent,
                                int key,
                                @NonNull Label target) {
        this.parent = parent;
        this.key = key;
        this.target = target;
    }

    @Override
    public int getKey() {
        return key;
    }

    @Override
    public int getOffset() {
        return target.getCodeAddress() - parent.getReferrer().getCodeAddress();
    }

    @NonNull
    public Label getTarget() {
        return target;
    }
}
