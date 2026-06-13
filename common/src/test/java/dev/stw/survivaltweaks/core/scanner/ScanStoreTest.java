package dev.stw.survivaltweaks.core.scanner;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ScanStoreTest {
    static {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void activeScanTargetsAreImmutableSnapshots() {
        var store = new ScanStore();

        store.load(categoriesWithEntry("Diamond", "minecraft:diamond_ore"));
        var snapshot = store.activeScanTargets();

        assertEquals(1, snapshot.size());
        var first = snapshot.iterator().next();
        assertEquals(ScanType.parseColor("rgb(0, 255, 0)"), first.colorInt());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.add(new ScanStore.ActiveScanTarget(Blocks.EMERALD_ORE, 0xff00ff00)));

        store.load(categoriesWithEntry("Emerald", "minecraft:emerald_ore"));

        assertEquals(1, snapshot.size());
        assertTrue(snapshot.contains(first));
        assertEquals(1, store.activeScanTargets().size());
        assertFalse(store.activeScanTargets().contains(first));
    }

    private static JsonArray categoriesWithEntry(String name, String blockName) {
        var category = new JsonObject();
        category.addProperty("name", "Default");
        category.addProperty("color", 65280);
        category.addProperty("order", 0);
        category.add("icon", new JsonObject());
        var entries = new JsonArray();
        var entry = new JsonObject();
        entry.addProperty("type", "survivaltweaks:block");
        entry.addProperty("name", name);
        entry.addProperty("color", "rgb(0, 255, 0)");
        entry.addProperty("order", 0);
        entry.addProperty("enabled", true);
        entry.addProperty("block_name", blockName);
        entries.add(entry);
        category.add("entries", entries);
        var categories = new JsonArray();
        categories.add(category);
        return categories;
    }
}
