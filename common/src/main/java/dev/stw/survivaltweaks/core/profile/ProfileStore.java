package dev.stw.survivaltweaks.core.profile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.stw.survivaltweaks.SurvivalTweaks;
import dev.stw.survivaltweaks.core.scanner.ScanStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ProfileStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProfileStore.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String PROFILES_FILE = "profiles.json";
    private static final String LEGACY_FILE = "scan-store.json";

    private final Path configRoot;
    private final List<XRayProfile> profiles = new ArrayList<>();
    private XRayProfile activeProfile;
    private ProfileActivationContext lastActivationContext;
    private boolean suppressSave;

    public ProfileStore() {
        this(SurvivalTweaks.XPLAT.configPath().get());
    }

    public ProfileStore(Path configRoot) {
        this.configRoot = configRoot;
    }

    public void load() {
        suppressSave = true;
        var profilesPath = configRoot.resolve(PROFILES_FILE);
        if (Files.exists(profilesPath)) {
            loadProfiles(profilesPath);
        } else {
            profiles.clear();
            loadLegacyOrCreateDefault();
        }
        repairState();
        suppressSave = false;
        save();
    }

    public void save() {
        if (suppressSave) return;
        try {
            Files.createDirectories(configRoot);
            Files.writeString(configRoot.resolve(PROFILES_FILE), GSON.toJson(toJson()));
        } catch (Exception e) {
            LOGGER.error("Failed to save profiles: {}", e.getMessage());
        }
    }

    public List<XRayProfile> profiles() {
        return List.copyOf(profiles);
    }

    public XRayProfile activeProfile() {
        return activeProfile;
    }

    public boolean selectForContext(ProfileActivationContext context) {
        var selected = selectMatchingProfile(context);
        if (selected == null || selected == activeProfile) {
            return false;
        }

        activeProfile = selected;
        save();
        return true;
    }

    public boolean selectForContextIfChanged(ProfileActivationContext context) {
        var normalizedContext = normalizeContext(context);
        if (Objects.equals(normalizedContext, lastActivationContext)) {
            return false;
        }

        lastActivationContext = normalizedContext;
        selectForContext(context);
        return true;
    }

    public boolean clearLastActivationContext() {
        if (lastActivationContext == null) {
            return false;
        }

        lastActivationContext = null;
        return true;
    }

    public XRayProfile defaultProfile() {
        return findDefaultProfile();
    }

    public XRayProfile createProfile(String name) {
        var trimmed = name == null ? "" : name.trim();
        if (trimmed.isBlank()) return null;
        var profile = XRayProfile.create(UUID.randomUUID().toString(), trimmed, ProfileActivationMode.NONE, this::save);
        profiles.add(profile);
        save();
        return profile;
    }

    public boolean deleteProfile(String id) {
        var profile = findProfile(id);
        if (profile == null || profile == defaultProfile()) return false;
        profiles.remove(profile);
        if (profile == activeProfile) activeProfile = defaultProfile();
        save();
        return true;
    }

    public boolean selectProfile(String id) {
        var profile = findProfile(id);
        if (profile == null) return false;
        activeProfile = profile;
        save();
        return true;
    }

    public void setActivationMode(String id, ProfileActivationMode mode) {
        var profile = findProfile(id);
        if (profile == null) return;
        updateProfiles(() -> {
            if (mode == ProfileActivationMode.DEFAULT) {
                for (var other : profiles) if (other != profile && other.activationMode() == ProfileActivationMode.DEFAULT) other.setActivationMode(ProfileActivationMode.NONE);
            }
            profile.setActivationMode(mode);
            repairState();
        });
    }

    public boolean renameProfile(String id, String name) {
        var profile = findProfile(id);
        if (profile == null) return false;
        var trimmed = name == null ? "" : name.trim();
        if (trimmed.isBlank() || trimmed.equals(profile.name())) return false;
        updateProfiles(() -> profile.setName(trimmed));
        return true;
    }

    public boolean addServerAddress(String id, String address) {
        var normalized = normalizeServerAddress(address);
        if (normalized.isBlank()) return false;
        if (profiles.stream().flatMap(p -> p.serverAddresses().stream()).map(ProfileStore::normalizeServerAddress).anyMatch(normalized::equals)) return false;
        var profile = findProfile(id);
        if (profile == null) return false;
        updateProfiles(() -> profile.addServerAddress(normalized));
        return true;
    }

    public boolean removeServerAddress(String id, String address) {
        var profile = findProfile(id);
        if (profile == null) return false;
        var normalized = normalizeServerAddress(address);
        if (!profile.serverAddresses().contains(normalized)) return false;
        updateProfiles(() -> profile.removeServerAddress(normalized));
        return true;
    }

    public static String normalizeServerAddress(String address) {
        return address == null ? "" : address.trim().toLowerCase();
    }

    private static ProfileActivationContext normalizeContext(ProfileActivationContext context) {
        if (context == null) {
            return null;
        }

        return new ProfileActivationContext(context.singleplayer(), normalizeServerAddress(context.serverAddress()));
    }

    private XRayProfile selectMatchingProfile(ProfileActivationContext context) {
        if (context == null) return findDefaultProfile();

        var normalizedServerAddress = normalizeServerAddress(context.serverAddress());
        if (!normalizedServerAddress.isBlank()) {
            var multiplayer = profiles.stream()
                    .filter(p -> p.activationMode() == ProfileActivationMode.MULTIPLAYER)
                    .filter(p -> p.serverAddresses().stream().map(ProfileStore::normalizeServerAddress).anyMatch(normalizedServerAddress::equals))
                    .findFirst()
                    .orElse(null);
            if (multiplayer != null) return multiplayer;
        }

        if (context.singleplayer()) {
            var singleplayer = profiles.stream().filter(p -> p.activationMode() == ProfileActivationMode.SINGLEPLAYER).findFirst().orElse(null);
            if (singleplayer != null) return singleplayer;
        }

        return findDefaultProfile();
    }

    private void loadProfiles(Path profilesPath) {
        List<XRayProfile> loadedProfiles;
        String activeProfileId;
        String defaultProfileId;
        try {
            var root = GSON.fromJson(Files.readString(profilesPath), JsonObject.class);
            loadedProfiles = new ArrayList<>();
            activeProfileId = root.has("active_profile") ? root.get("active_profile").getAsString() : null;
            defaultProfileId = root.has("default_profile") ? root.get("default_profile").getAsString() : null;
            for (var element : root.getAsJsonArray("profiles")) loadedProfiles.add(loadProfile(element.getAsJsonObject()));
        } catch (Exception e) {
            LOGGER.error("Failed to load profiles: {}", e.getMessage());
            moveBrokenProfilesFile(profilesPath);
            profiles.clear();
            loadLegacyOrCreateDefault();
            return;
        }
        profiles.clear();
        profiles.addAll(loadedProfiles);
        applySavedDefault(defaultProfileId);
        activeProfile = findProfile(activeProfileId);
        if (activeProfile == null) activeProfile = findDefaultProfile();
        repairServerAddresses();
    }

    private void loadLegacyOrCreateDefault() {
        var legacyPath = configRoot.resolve(LEGACY_FILE);
        var profile = XRayProfile.create(UUID.randomUUID().toString(), "Default", ProfileActivationMode.DEFAULT, this::save);
        if (Files.exists(legacyPath)) {
            try {
                var categories = GSON.fromJson(Files.readString(legacyPath), JsonArray.class);
                profile.scanStore().load(categories);
            } catch (Exception e) {
                LOGGER.error("Failed to load legacy scan store: {}", e.getMessage());
            }
        }
        profiles.add(profile);
        activeProfile = profile;
    }

    private XRayProfile loadProfile(JsonObject obj) {
        var profile = XRayProfile.create(obj.get("id").getAsString(), obj.get("name").getAsString(), ProfileActivationMode.fromJsonName(obj.has("activation") ? obj.get("activation").getAsString() : null), this::save);
        if (obj.has("server_addresses")) {
            var addresses = new ArrayList<String>();
            for (var address : obj.getAsJsonArray("server_addresses")) addresses.add(address.getAsString());
            profile.setServerAddresses(addresses);
        }
        if (obj.has("categories")) profile.scanStore().load(obj.getAsJsonArray("categories"));
        return profile;
    }

    private void repairState() {
        if (profiles.isEmpty()) {
            loadLegacyOrCreateDefault();
            return;
        }
        var defaults = profiles.stream().filter(p -> p.activationMode() == ProfileActivationMode.DEFAULT).toList();
        if (defaults.size() != 1) {
            var keepDefault = defaults.isEmpty() ? profiles.get(0) : defaults.get(0);
            for (var profile : profiles) profile.setActivationMode(profile == keepDefault ? ProfileActivationMode.DEFAULT : ProfileActivationMode.NONE);
        }
        activeProfile = findProfile(activeProfile == null ? null : activeProfile.id());
        if (activeProfile == null) activeProfile = findDefaultProfile();
    }

    private void applySavedDefault(String defaultProfileId) {
        var savedDefault = findProfile(defaultProfileId);
        if (savedDefault != null) {
            for (var profile : profiles) profile.setActivationMode(profile == savedDefault ? ProfileActivationMode.DEFAULT : ProfileActivationMode.NONE);
            return;
        }

        var firstDefault = findDefaultProfile();
        if (firstDefault != null) {
            for (var profile : profiles) profile.setActivationMode(profile == firstDefault ? ProfileActivationMode.DEFAULT : ProfileActivationMode.NONE);
        }
    }

    private void repairServerAddresses() {
        var seen = new LinkedHashSet<String>();
        for (var profile : profiles) {
            var repaired = new ArrayList<String>();
            for (var address : profile.serverAddresses()) {
                var normalized = normalizeServerAddress(address);
                if (normalized.isBlank() || !seen.add(normalized)) continue;
                repaired.add(normalized);
            }
            profile.setServerAddresses(repaired);
        }
    }

    private XRayProfile findDefaultProfile() {
        return profiles.stream().filter(p -> p.activationMode() == ProfileActivationMode.DEFAULT).findFirst().orElse(null);
    }

    private XRayProfile findProfile(String id) {
        if (id == null) return null;
        return profiles.stream().filter(p -> p.id().equals(id)).findFirst().orElse(null);
    }

    private JsonObject toJson() {
        repairState();
        var root = new JsonObject();
        root.addProperty("active_profile", activeProfile == null ? defaultProfile().id() : activeProfile.id());
        root.addProperty("default_profile", defaultProfile().id());
        var arr = new JsonArray();
        for (var profile : profiles) {
            var obj = new JsonObject();
            obj.addProperty("id", profile.id());
            obj.addProperty("name", profile.name());
            obj.addProperty("activation", profile.activationMode().jsonName());
            var addresses = new JsonArray();
            for (var address : profile.serverAddresses()) addresses.add(address);
            obj.add("server_addresses", addresses);
            obj.add("categories", profile.scanStore().toJson());
            arr.add(obj);
        }
        root.add("profiles", arr);
        return root;
    }

    private void updateProfiles(Runnable mutation) {
        var previous = suppressSave;
        suppressSave = true;
        try {
            mutation.run();
        } finally {
            suppressSave = previous;
        }
        save();
    }

    private void moveBrokenProfilesFile(Path profilesPath) {
        try {
            var brokenPath = configRoot.resolve(PROFILES_FILE + ".broken");
            Files.createDirectories(configRoot);
            if (Files.exists(brokenPath)) Files.delete(brokenPath);
            if (Files.exists(profilesPath)) Files.move(profilesPath, brokenPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception moveError) {
            LOGGER.error("Failed to quarantine broken profiles file: {}", moveError.getMessage());
        }
    }
}
