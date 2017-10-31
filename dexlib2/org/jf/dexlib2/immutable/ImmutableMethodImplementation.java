

package org.jf.dexlib2.immutable;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.collect.ImmutableList;

import org.jf.dexlib2.iface.ExceptionHandler;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.TryBlock;
import org.jf.dexlib2.iface.debug.DebugItem;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.immutable.debug.ImmutableDebugItem;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction;
import org.jf.util.ImmutableUtils;

import java.util.List;

public class ImmutableMethodImplementation implements MethodImplementation {
    protected final int registerCount;
    @NonNull
    protected final ImmutableList<? extends ImmutableInstruction> instructions;
    @NonNull
    protected final ImmutableList<? extends ImmutableTryBlock> tryBlocks;
    @NonNull
    protected final ImmutableList<? extends ImmutableDebugItem> debugItems;

    public ImmutableMethodImplementation(int registerCount,
                                         @Nullable Iterable<? extends Instruction> instructions,
                                         @Nullable List<? extends TryBlock<? extends ExceptionHandler>> tryBlocks,
                                         @Nullable Iterable<? extends DebugItem> debugItems) {
        this.registerCount = registerCount;
        this.instructions = ImmutableInstruction.immutableListOf(instructions);
        this.tryBlocks = ImmutableTryBlock.immutableListOf(tryBlocks);
        this.debugItems = ImmutableDebugItem.immutableListOf(debugItems);
    }

    public ImmutableMethodImplementation(int registerCount,
                                         @Nullable ImmutableList<? extends ImmutableInstruction> instructions,
                                         @Nullable ImmutableList<? extends ImmutableTryBlock> tryBlocks,
                                         @Nullable ImmutableList<? extends ImmutableDebugItem> debugItems) {
        this.registerCount = registerCount;
        this.instructions = ImmutableUtils.nullToEmptyList(instructions);
        this.tryBlocks = ImmutableUtils.nullToEmptyList(tryBlocks);
        this.debugItems = ImmutableUtils.nullToEmptyList(debugItems);
    }

    @Nullable
    public static ImmutableMethodImplementation of(@Nullable MethodImplementation methodImplementation) {
        if (methodImplementation == null) {
            return null;
        }
        if (methodImplementation instanceof ImmutableMethodImplementation) {
            return (ImmutableMethodImplementation) methodImplementation;
        }
        return new ImmutableMethodImplementation(
                methodImplementation.getRegisterCount(),
                methodImplementation.getInstructions(),
                methodImplementation.getTryBlocks(),
                methodImplementation.getDebugItems());
    }

    @Override
    public int getRegisterCount() {
        return registerCount;
    }

    @NonNull
    @Override
    public ImmutableList<? extends ImmutableInstruction> getInstructions() {
        return instructions;
    }

    @NonNull
    @Override
    public ImmutableList<? extends ImmutableTryBlock> getTryBlocks() {
        return tryBlocks;
    }

    @NonNull
    @Override
    public ImmutableList<? extends ImmutableDebugItem> getDebugItems() {
        return debugItems;
    }
}
