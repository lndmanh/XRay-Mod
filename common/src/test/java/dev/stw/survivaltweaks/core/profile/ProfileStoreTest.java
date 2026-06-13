package dev.stw.survivaltweaks.core.profile;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ProfileStoreTest {
    private static final Gson GSON = new Gson();

    static {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void createsDefaultProfileWhenProfilesFileDoesNotExist(@TempDir Path tempDir) {
        var store = new ProfileStore(tempDir);
        store.load();

        assertEquals(1, store.profiles().size());
        assertEquals(store.defaultProfile(), store.activeProfile());
        assertEquals(ProfileActivationMode.DEFAULT, store.defaultProfile().activationMode());
    }

    @Test
    void migratesLegacyScanStoreIntoDefaultProfile(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("scan-store.json"), GSON.toJson(categoriesWithEntry()));

        var store = new ProfileStore(tempDir);
        store.load();

        assertEquals(1, store.profiles().size());
        assertEquals(1, store.defaultProfile().scanStore().categories().size());
        assertEquals("Default", store.defaultProfile().scanStore().categories().get(0).name());
        assertEquals(1, store.defaultProfile().scanStore().categories().get(0).entries().size());
        assertEquals("survivaltweaks:block", store.defaultProfile().scanStore().categories().get(0).entries().get(0).save().get("type").getAsString());
    }

    @Test
    void savesAndReloadsProfileEntries(@TempDir Path tempDir) {
        var store = new ProfileStore(tempDir);
        store.load();
        store.defaultProfile().scanStore().load(categoriesWithEntry());
        store.save();

        var reloaded = new ProfileStore(tempDir);
        reloaded.load();

        assertEquals(1, reloaded.defaultProfile().scanStore().categories().size());
        assertEquals(1, reloaded.defaultProfile().scanStore().categories().get(0).entries().size());
        assertEquals("Diamond", reloaded.defaultProfile().scanStore().categories().get(0).entries().get(0).name());
    }

    @Test
    void settingDefaultChangesPreviousDefaultToNone(@TempDir Path tempDir) {
        var store = new ProfileStore(tempDir);
        store.load();
        var originalDefault = store.defaultProfile();
        var profile = store.createProfile("Other");
        store.setActivationMode(profile.id(), ProfileActivationMode.DEFAULT);

        assertEquals(ProfileActivationMode.NONE, originalDefault.activationMode());
        assertEquals(profile, store.defaultProfile());
        assertEquals(ProfileActivationMode.DEFAULT, profile.activationMode());
    }

    @Test
    void settingCurrentDefaultToNoneStillLeavesExactlyOneDefault(@TempDir Path tempDir) {
        var store = new ProfileStore(tempDir);
        store.load();
        var currentDefault = store.defaultProfile();

        store.setActivationMode(currentDefault.id(), ProfileActivationMode.NONE);

        assertEquals(1, store.profiles().stream().filter(p -> p.activationMode() == ProfileActivationMode.DEFAULT).count());
        assertNotNull(store.defaultProfile());
        assertEquals(ProfileActivationMode.DEFAULT, store.defaultProfile().activationMode());
    }

    @Test
    void saveRepairsZeroDefaultAfterInternalMutation(@TempDir Path tempDir) {
        var store = new ProfileStore(tempDir);
        store.load();

        store.defaultProfile().setActivationMode(ProfileActivationMode.NONE);
        store.save();

        assertEquals(1, store.profiles().stream().filter(p -> p.activationMode() == ProfileActivationMode.DEFAULT).count());
        assertEquals(ProfileActivationMode.DEFAULT, store.defaultProfile().activationMode());
    }

    @Test
    void deletingActiveProfileSelectsDefault(@TempDir Path tempDir) {
        var store = new ProfileStore(tempDir);
        store.load();
        var profile = store.createProfile("Other");
        store.selectProfile(profile.id());

        assertTrue(store.deleteProfile(profile.id()));
        assertEquals(store.defaultProfile(), store.activeProfile());
    }

    @Test
    void cannotDeleteDefaultProfile(@TempDir Path tempDir) {
        var store = new ProfileStore(tempDir);
        store.load();

        assertFalse(store.deleteProfile(store.defaultProfile().id()));
    }

    @Test
    void rejectsDuplicateServerAddressAcrossProfiles(@TempDir Path tempDir) {
        var store = new ProfileStore(tempDir);
        store.load();
        var other = store.createProfile("Other");

        assertTrue(store.addServerAddress(store.defaultProfile().id(), "Example.COM:25565"));
        assertFalse(store.addServerAddress(other.id(), " example.com:25565 "));
    }

    @Test
    void selectsMatchingMultiplayerProfileBeforeDefault(@TempDir Path tempDir) {
        var store = new ProfileStore(tempDir);
        store.load();
        var multiplayer = store.createProfile("Multiplayer");
        store.setActivationMode(multiplayer.id(), ProfileActivationMode.MULTIPLAYER);
        store.addServerAddress(multiplayer.id(), "Example.COM:25565");

        assertTrue(store.selectForContext(new ProfileActivationContext(false, "example.com:25565")));
        assertEquals(multiplayer, store.activeProfile());
    }

    @Test
    void fallsBackToDefaultWhenNoRuleMatches(@TempDir Path tempDir) {
        var store = new ProfileStore(tempDir);
        store.load();
        var multiplayer = store.createProfile("Multiplayer");
        store.setActivationMode(multiplayer.id(), ProfileActivationMode.MULTIPLAYER);
        store.addServerAddress(multiplayer.id(), "other.example:25565");
        store.selectProfile(multiplayer.id());

        assertTrue(store.selectForContext(new ProfileActivationContext(false, "example.com:25565")));
        assertEquals(store.defaultProfile(), store.activeProfile());
    }

    @Test
    void selectsSingleplayerProfileWhenSingleplayerContextMatches(@TempDir Path tempDir) {
        var store = new ProfileStore(tempDir);
        store.load();
        var singleplayer = store.createProfile("Singleplayer");
        store.setActivationMode(singleplayer.id(), ProfileActivationMode.SINGLEPLAYER);

        assertTrue(store.selectForContext(new ProfileActivationContext(true, null)));
        assertEquals(singleplayer, store.activeProfile());
    }

    @Test
    void selectForContextReturnsFalseWhenProfileDoesNotChange(@TempDir Path tempDir) {
        var store = new ProfileStore(tempDir);
        store.load();

        assertFalse(store.selectForContext(new ProfileActivationContext(false, null)));
        assertEquals(store.defaultProfile(), store.activeProfile());
    }

    @Test
    void cachedContextSelectionReturnsTrueWhenContextChangesEvenIfActiveProfileStaysDefault(@TempDir Path tempDir) {
        var store = new ProfileStore(tempDir);
        store.load();

        assertTrue(store.selectForContextIfChanged(new ProfileActivationContext(false, "example.com:25565")));
        assertEquals(store.defaultProfile(), store.activeProfile());
    }

    @Test
    void cachedContextSelectionPreservesManualProfileSelectionUntilContextChanges(@TempDir Path tempDir) {
        var store = new ProfileStore(tempDir);
        store.load();
        var multiplayer = store.createProfile("Multiplayer");
        store.setActivationMode(multiplayer.id(), ProfileActivationMode.MULTIPLAYER);
        store.addServerAddress(multiplayer.id(), "example.com:25565");

        var context = new ProfileActivationContext(false, "example.com:25565");
        assertTrue(store.selectForContextIfChanged(context));
        assertEquals(multiplayer, store.activeProfile());

        var manual = store.createProfile("Manual");
        assertTrue(store.selectProfile(manual.id()));

        assertFalse(store.selectForContextIfChanged(context));
        assertEquals(manual, store.activeProfile());

        assertTrue(store.selectForContextIfChanged(new ProfileActivationContext(true, null)));
        assertEquals(store.defaultProfile(), store.activeProfile());
    }

    @Test
    void repairsInvalidSavedDefaults(@TempDir Path tempDir) throws Exception {
        var json = new JsonObject();
        json.addProperty("active_profile", "missing");
        json.addProperty("default_profile", "one");
        var profiles = new JsonArray();
        profiles.add(profileJson("one", "One", ProfileActivationMode.DEFAULT, minimalLegacyCategories()));
        profiles.add(profileJson("two", "Two", ProfileActivationMode.DEFAULT, new JsonArray()));
        json.add("profiles", profiles);
        Files.createDirectories(tempDir);
        Files.writeString(tempDir.resolve("profiles.json"), GSON.toJson(json));

        var store = new ProfileStore(tempDir);
        store.load();

        assertEquals("one", store.defaultProfile().id());
        assertEquals(store.defaultProfile(), store.activeProfile());
        assertEquals(ProfileActivationMode.DEFAULT, store.defaultProfile().activationMode());
        assertEquals(ProfileActivationMode.NONE, store.profiles().get(1).activationMode());
    }

    @Test
    void restoresValidSavedActiveProfile(@TempDir Path tempDir) throws Exception {
        var json = new JsonObject();
        json.addProperty("active_profile", "two");
        json.addProperty("default_profile", "one");
        var profiles = new JsonArray();
        profiles.add(profileJson("one", "One", ProfileActivationMode.NONE, minimalLegacyCategories()));
        profiles.add(profileJson("two", "Two", ProfileActivationMode.NONE, minimalLegacyCategories()));
        json.add("profiles", profiles);
        Files.createDirectories(tempDir);
        Files.writeString(tempDir.resolve("profiles.json"), GSON.toJson(json));

        var store = new ProfileStore(tempDir);
        store.load();

        assertEquals("two", store.activeProfile().id());
        assertEquals(ProfileActivationMode.DEFAULT, store.defaultProfile().activationMode());
    }

    @Test
    void honorsSavedDefaultProfileWhenActivationFlagsAreInvalid(@TempDir Path tempDir) throws Exception {
        var json = new JsonObject();
        json.addProperty("active_profile", "missing");
        json.addProperty("default_profile", "two");
        var profiles = new JsonArray();
        profiles.add(profileJson("one", "One", ProfileActivationMode.NONE, minimalLegacyCategories()));
        profiles.add(profileJson("two", "Two", ProfileActivationMode.NONE, minimalLegacyCategories()));
        json.add("profiles", profiles);
        Files.createDirectories(tempDir);
        Files.writeString(tempDir.resolve("profiles.json"), GSON.toJson(json));

        var store = new ProfileStore(tempDir);
        store.load();

        assertEquals("two", store.defaultProfile().id());
        assertEquals(ProfileActivationMode.DEFAULT, store.defaultProfile().activationMode());
        assertEquals(ProfileActivationMode.NONE, store.profiles().get(0).activationMode());
        assertEquals(store.defaultProfile(), store.activeProfile());
    }

    @Test
    void rejectsBlankServerAddresses(@TempDir Path tempDir) {
        var store = new ProfileStore(tempDir);
        store.load();

        assertFalse(store.addServerAddress(store.defaultProfile().id(), "   "));
    }

    @Test
    void createProfileRejectsBlankNames(@TempDir Path tempDir) {
        var store = new ProfileStore(tempDir);
        store.load();

        assertNull(store.createProfile("   "));
        assertEquals(1, store.profiles().size());
    }

    @Test
    void repairsServerAddressesOnLoad(@TempDir Path tempDir) throws Exception {
        var json = new JsonObject();
        json.addProperty("active_profile", "one");
        json.addProperty("default_profile", "one");
        var profiles = new JsonArray();
        var first = profileJson("one", "One", ProfileActivationMode.DEFAULT, minimalLegacyCategories());
        var firstAddresses = new JsonArray();
        firstAddresses.add(" Example.COM:25565 ");
        firstAddresses.add(" ");
        firstAddresses.add("example.com:25565");
        first.add("server_addresses", firstAddresses);
        var second = profileJson("two", "Two", ProfileActivationMode.NONE, minimalLegacyCategories());
        var secondAddresses = new JsonArray();
        secondAddresses.add("EXAMPLE.com:25565");
        secondAddresses.add("other.example:25565");
        second.add("server_addresses", secondAddresses);
        profiles.add(first);
        profiles.add(second);
        json.add("profiles", profiles);
        Files.createDirectories(tempDir);
        Files.writeString(tempDir.resolve("profiles.json"), GSON.toJson(json));

        var store = new ProfileStore(tempDir);
        store.load();

        assertEquals(List.of("example.com:25565"), store.profiles().get(0).serverAddresses());
        assertEquals(List.of("other.example:25565"), store.profiles().get(1).serverAddresses());
    }

    @Test
    void malformedProfilesFileIsQuarantinedAndDefaultProfileCreated(@TempDir Path tempDir) throws Exception {
        Files.createDirectories(tempDir);
        Files.writeString(tempDir.resolve("profiles.json"), "not-json");

        var store = new ProfileStore(tempDir);
        store.load();

        assertTrue(Files.exists(tempDir.resolve("profiles.json.broken")));
        assertEquals(1, store.profiles().size());
        assertEquals(ProfileActivationMode.DEFAULT, store.defaultProfile().activationMode());
        assertEquals(store.defaultProfile(), store.activeProfile());
    }

    @Test
    void renameProfileRejectsBlankAndPersistsValidNames(@TempDir Path tempDir) {
        var store = new ProfileStore(tempDir);
        store.load();
        var profile = store.createProfile("Original");

        assertFalse(store.renameProfile(profile.id(), "   "));
        assertTrue(store.renameProfile(profile.id(), " Renamed "));

        var reloaded = new ProfileStore(tempDir);
        reloaded.load();
        assertEquals("Renamed", reloaded.profiles().stream().filter(p -> p.id().equals(profile.id())).findFirst().orElseThrow().name());
    }

    private static JsonArray minimalLegacyCategories() {
        var category = new JsonObject();
        category.addProperty("name", "Default");
        category.addProperty("color", 65280);
        category.addProperty("order", 0);
        category.add("icon", new JsonObject());
        category.add("entries", new JsonArray());
        var categories = new JsonArray();
        categories.add(category);
        return categories;
    }

    private static JsonArray categoriesWithEntry() {
        var category = new JsonObject();
        category.addProperty("name", "Default");
        category.addProperty("color", 65280);
        category.addProperty("order", 0);
        category.add("icon", new JsonObject());
        var entries = new JsonArray();
        var entry = new JsonObject();
        entry.addProperty("type", "survivaltweaks:block");
        entry.addProperty("name", "Diamond");
        entry.addProperty("color", "rgb(0, 255, 0)");
        entry.addProperty("order", 0);
        entry.addProperty("enabled", true);
        entry.addProperty("block_name", "minecraft:diamond_ore");
        entries.add(entry);
        category.add("entries", entries);
        var categories = new JsonArray();
        categories.add(category);
        return categories;
    }

    private static JsonObject profileJson(String id, String name, ProfileActivationMode activation, JsonArray categories) {
        var obj = new JsonObject();
        obj.addProperty("id", id);
        obj.addProperty("name", name);
        obj.addProperty("activation", activation.jsonName());
        obj.add("server_addresses", new JsonArray());
        obj.add("categories", categories);
        return obj;
    }
}
