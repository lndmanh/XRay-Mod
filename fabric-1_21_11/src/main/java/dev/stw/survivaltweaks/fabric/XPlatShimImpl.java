package dev.stw.survivaltweaks.fabric;

import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import dev.stw.survivaltweaks.utils.XPlatShim;

import java.nio.file.Path;
import java.util.function.Supplier;

public class XPlatShimImpl implements XPlatShim {
    private final Supplier<Path> configPath = () -> FabricLoader.getInstance().getConfigDir().resolve("survivaltweaks");

    @Override
    public TagKey<Block> oreTag() {
        return ConventionalBlockTags.ORES;
    }

    @Override
    public Supplier<Path> configPath() {
        return configPath;
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }
}
