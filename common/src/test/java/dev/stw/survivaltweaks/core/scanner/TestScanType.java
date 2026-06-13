package dev.stw.survivaltweaks.core.scanner;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public class TestScanType extends ScanType {
    public TestScanType(String name) {
        super(Type.BLOCK, name, "rgb(0, 255, 0)", 0, true);
    }

    @Override
    public boolean matches(Level level, BlockPos pos, BlockState state, FluidState fluidState) {
        return false;
    }

    @Override
    void writeData(JsonObject obj) {}
}
