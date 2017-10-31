

package org.jf.dexlib2.immutable.instruction;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.collect.ImmutableList;

import org.jf.dexlib2.Format;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.iface.instruction.SwitchElement;
import org.jf.dexlib2.iface.instruction.formats.SparseSwitchPayload;
import org.jf.util.ImmutableUtils;

import java.util.List;

public class ImmutableSparseSwitchPayload extends ImmutableInstruction implements SparseSwitchPayload {
    public static final Opcode OPCODE = Opcode.SPARSE_SWITCH_PAYLOAD;

    @NonNull
    protected final ImmutableList<? extends ImmutableSwitchElement> switchElements;

    public ImmutableSparseSwitchPayload(@Nullable List<? extends SwitchElement> switchElements) {
        super(OPCODE);
        this.switchElements = ImmutableSwitchElement.immutableListOf(switchElements);
    }

    public ImmutableSparseSwitchPayload(
            @Nullable ImmutableList<? extends ImmutableSwitchElement> switchElements) {
        super(OPCODE);
        this.switchElements = ImmutableUtils.nullToEmptyList(switchElements);
    }

    @NonNull
    public static ImmutableSparseSwitchPayload of(SparseSwitchPayload instruction) {
        if (instruction instanceof ImmutableSparseSwitchPayload) {
            return (ImmutableSparseSwitchPayload) instruction;
        }
        return new ImmutableSparseSwitchPayload(
                instruction.getSwitchElements());
    }

    @NonNull
    @Override
    public List<? extends SwitchElement> getSwitchElements() {
        return switchElements;
    }

    @Override
    public int getCodeUnits() {
        return 2 + switchElements.size() * 4;
    }

    @Override
    public Format getFormat() {
        return OPCODE.format;
    }
}
