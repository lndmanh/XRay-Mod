package dev.stw.survivaltweaks.core.scanner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.stw.survivaltweaks.SurvivalTweaks;
import dev.stw.survivaltweaks.utils.LazyValue;

import java.nio.file.Files;
import java.util.*;

public class ScanStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Logger LOGGER = LoggerFactory.getLogger(ScanStore.class);
    private static final String STORE_FILE = "scan-store.json";

    private static final LazyValue<Map<Identifier, String>> BLOCK_TO_COLOR_DEFAULTS = LazyValue.of(() -> {
        Map<Identifier, String> defaults = new HashMap<>();
        defaults.put(fromBlock(Blocks.DIAMOND_ORE), "rgb(0, 255, 0)"); // Green
        defaults.put(fromBlock(Blocks.DEEPSLATE_DIAMOND_ORE), "rgb(0, 255, 0)"); // Green (Deepslate Diamond)
        defaults.put(fromBlock(Blocks.GOLD_ORE), "rgb(255, 215, 0)"); // Gold
        defaults.put(fromBlock(Blocks.DEEPSLATE_GOLD_ORE), "rgb(255, 215, 0)"); // Gold (Deepslate Gold)
        defaults.put(fromBlock(Blocks.IRON_ORE), "rgb(192, 192, 192)"); // Silver
        defaults.put(fromBlock(Blocks.DEEPSLATE_IRON_ORE), "rgb(192, 192, 192)"); // Silver (Deepslate Iron)
        defaults.put(fromBlock(Blocks.COAL_ORE), "rgb(0, 0, 0)"); // Black
        defaults.put(fromBlock(Blocks.DEEPSLATE_COAL_ORE), "rgb(0, 0, 0)"); // Black (Deepslate Coal)
        defaults.put(fromBlock(Blocks.REDSTONE_ORE), "rgb(255, 0, 0)"); // Red
        defaults.put(fromBlock(Blocks.DEEPSLATE_REDSTONE_ORE), "rgb(255, 0, 0)"); // Red (Deepslate Redstone)
        defaults.put(fromBlock(Blocks.LAPIS_ORE), "rgb(0, 0, 255)"); // Blue
        defaults.put(fromBlock(Blocks.DEEPSLATE_LAPIS_ORE), "rgb(0, 0, 255)"); // Blue (Deepslate Lapis)
        defaults.put(fromBlock(Blocks.EMERALD_ORE), "rgb(0, 128, 0)"); // Emerald Green
        defaults.put(fromBlock(Blocks.DEEPSLATE_EMERALD_ORE), "rgb(0, 128, 0)"); // Emerald Green (Deepslate Emerald)
        defaults.put(fromBlock(Blocks.NETHER_GOLD_ORE), "rgb(255, 215, 0)"); // Nether Gold (Gold)
        defaults.put(fromBlock(Blocks.NETHER_QUARTZ_ORE), "rgb(255, 255, 255)"); // Quartz (White)
        defaults.put(fromBlock(Blocks.ANCIENT_DEBRIS), "rgb(128, 64, 0)"); // Ancient Debris (Brown)
        return defaults;
    });

    private static final Map<ScanType.Type, java.util.function.BiFunction<ScanType.Type, JsonObject, ScanType>> SCAN_TYPE_CREATORS = Map.of(
        ScanType.Type.BLOCK, BlockScanType::new
    );

    private final List<Category> categories = new ArrayList<>();
    private final Runnable onSave;

    // In memory holder of only the enabled scan targets
    private volatile Set<ActiveScanTarget> activeScanTargets = Set.of();

    public ScanStore() { this(null); }

    public ScanStore(Runnable onSave) { this.onSave = onSave; }

    public void createDefaultCategories() {
        List<ScanType> entries = new ArrayList<>();
        var oresTag = SurvivalTweaks.XPLAT.oreTag();

        var blockColorDefaults = BLOCK_TO_COLOR_DEFAULTS.get();

        BuiltInRegistries.BLOCK
                .stream()
                .filter(e -> e.defaultBlockState().is(oresTag))
                .map(e -> new BlockScanType(e, e.getName().getString(), blockColorDefaults.getOrDefault(e.builtInRegistryHolder().key().identifier(), ScanType.randomRgbColor()), 0))
                .forEach(entries::add);

        this.categories.add(createDefaultCategory(entries));

        this.save();
    }

    private static Category createDefaultCategory(List<ScanType> entries) {
        return new Category(
                new ItemStack(Blocks.DIAMOND_ORE),
                "Default",
                0x00FF00, // Green color
                0,
                entries
        );
    }

    // TODO: Support categories once the GUI can support it.
    public void addEntry(ScanType type) {
        if (this.categories.isEmpty()) {
            this.categories.add(createDefaultCategory(new ArrayList<>()));
        }

        Optional<Category> defaultCategory = this.categories.stream().findFirst();
        if (defaultCategory.isEmpty()) {
            return;
        }

        Category category = defaultCategory.get();
        category.entries.add(type);

        this.save();
    }

    // TODO: Support categories once the GUI can support it.
    public void removeEntry(ScanType type) {
        for (Category category : this.categories) {
            if (category.entries.remove(type)) {
                this.save();
                return; // Exit after removing the entry
            }
        }

        LOGGER.warn("Scan type not found in any category: {}", type);
    }

    public int getNextOrder() {
        var firstCategory = this.categories.stream().findFirst();
        if (firstCategory.isEmpty()) {
            return 0; // No categories, return 0
        }

        // Find the maximum order value in the first category
        return firstCategory.get().entries.stream()
                .mapToInt(ScanType::order)
                .max()
                .orElse(0) + 1; // Return max order + 1
    }

    public void load() {
        loadFromFile();
        updateActiveTargets();
    }

    public void load(JsonArray categories) {
        this.categories.clear();
        if (categories != null) {
            for (var categoryObj : categories) {
                this.categories.add(Category.load(categoryObj.getAsJsonObject()));
            }
        }
        updateActiveTargets();
    }

    public JsonArray toJson() {
        JsonArray categoriesArray = new JsonArray();
        for (Category category : this.categories) {
            JsonObject categoryObj = new JsonObject();
            category.save(categoryObj);
            categoriesArray.add(categoryObj);
        }
        return categoriesArray;
    }

    private void loadFromFile() {
        this.categories.clear();

        JsonArray categories;
        try {
            var configPath = SurvivalTweaks.XPLAT.configPath().get().resolve(STORE_FILE);
            var parentDir = configPath.getParent();
            if (Files.notExists(parentDir)) {
                // If the config directory does not exist, create it
                Files.createDirectories(parentDir);
            }

            String jsonContent = Files.readString(configPath);
            categories = GSON.fromJson(jsonContent, JsonArray.class);
        } catch (Exception e) {
            LOGGER.error("Failed to load scan store from file: {}", e.getMessage());
            return;
        }

        for (var categoryObj : categories) {
            var category = Category.load(categoryObj.getAsJsonObject());
            this.categories.add(category);
        }
    }

    public void save() {
        updateActiveTargets();
        if (this.onSave != null) {
            this.onSave.run();
            return;
        }

        save(SurvivalTweaks.XPLAT.configPath().get().resolve(STORE_FILE));
    }

    public void save(java.nio.file.Path configPath) {
        updateActiveTargets();
        try {
            var parentDir = configPath.getParent();
            if (parentDir != null && Files.notExists(parentDir)) {
                Files.createDirectories(parentDir);
            }
            Files.writeString(configPath, GSON.toJson(toJson()));
        } catch (Exception e) {
            LOGGER.error("Failed to save scan store to file: {}", e.getMessage());
        }
    }

    public void updateActiveTargets() {
        var activeTargets = new HashSet<ActiveScanTarget>();
        for (Category category : this.categories) {
            for (ScanType scanType : category.entries) {
                if (!scanType.enabled) {
                    continue; // Skip disabled scan types
                }
                if (scanType instanceof BlockScanType blockScanType && blockScanType.block != null) {
                    activeTargets.add(new ActiveScanTarget(blockScanType.block, scanType.colorInt()));
                }
            }
        }
        this.activeScanTargets = Set.copyOf(activeTargets);
    }

    public List<Category> categories() {
        return categories.stream()
                .map(category -> new Category(category.icon(), category.name(), category.color(), category.order(), Collections.unmodifiableList(category.entries())))
                .toList();
    }

    public Set<ActiveScanTarget> activeScanTargets() {
        return activeScanTargets;
    }

    public record ActiveScanTarget(Block block, int colorInt) {
        public boolean matches(BlockState state, FluidState fluidState) {
            return state.is(block);
        }
    }

    public record Category(
            ItemStack icon,
            String name,
            int color,
            int order,
            List<ScanType> entries
    ) {
        public void save(JsonObject obj) {
            obj.addProperty("name", name);
            obj.addProperty("color", color);
            obj.addProperty("order", order);

            var entriesArray = new JsonArray();
            for (ScanType entry : entries) {
                entriesArray.add(entry.save());
            }

            try {
                var encodedIcon = ItemStack.CODEC.encodeStart(JsonOps.INSTANCE, icon == null ? ItemStack.EMPTY : icon).result();
                obj.add("icon", encodedIcon.<JsonElement>map(e -> e).orElseGet(JsonObject::new));
            } catch (Exception e) {
                obj.add("icon", new JsonObject());
            }

            obj.add("entries", entriesArray);
        }

        public static Category load(JsonObject obj) {
            var name = obj.has("name") ? obj.get("name").getAsString() : "Default";
            var color = obj.has("color") ? obj.get("color").getAsInt() : 0x00FF00;
            var order = obj.has("order") ? obj.get("order").getAsInt() : 0;
            var entriesArray = obj.has("entries") && obj.get("entries").isJsonArray() ? obj.getAsJsonArray("entries") : new JsonArray();

            var entries = new ArrayList<ScanType>();
            for (var entry : entriesArray) {
                var entryObj = entry.getAsJsonObject();
                try {
                    if (!entryObj.has("type")) {
                        continue;
                    }
                    var type = ScanType.Type.fromId(Identifier.tryParse(entryObj.get("type").getAsString()));
                    var creator = SCAN_TYPE_CREATORS.get(type);
                    if (creator == null) {
                        LOGGER.warn("No creator found for scan type: {}", type);
                        continue; // Skip unknown types
                    }

                    var scanType = creator.apply(type, entryObj);
                    entries.add(scanType);
                } catch (Exception e) {
                    LOGGER.warn("Unknown scan type in store: {}, data: {}", entryObj.has("type") ? entryObj.get("type").getAsString() : "<missing>", entryObj);
                }
            }

            var icon = ItemStack.EMPTY;
            if (obj.has("icon") && obj.get("icon").isJsonObject()) {
                try {
                    icon = ItemStack.CODEC.parse(JsonOps.INSTANCE, obj.getAsJsonObject("icon")).result().orElse(ItemStack.EMPTY);
                } catch (Exception ignored) {
                    icon = ItemStack.EMPTY;
                }
            }

            return new Category(
                icon,
                name,
                color,
                order,
                entries
            );
        }
    }

    private static Identifier fromBlock(Block block) {
        if (block == null) {
            return null;
        }

        return BuiltInRegistries.BLOCK.getKey(block);
    }
}
