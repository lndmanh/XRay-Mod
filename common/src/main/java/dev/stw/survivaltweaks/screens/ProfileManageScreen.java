package dev.stw.survivaltweaks.screens;

import dev.stw.survivaltweaks.core.ScanController;
import dev.stw.survivaltweaks.core.profile.ProfileActivationMode;
import dev.stw.survivaltweaks.core.profile.ProfileStore;
import dev.stw.survivaltweaks.core.profile.XRayProfile;
import dev.stw.survivaltweaks.screens.helpers.GuiBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ProfileManageScreen extends GuiBase {
    private ProfileList profileList;
    private EditBox profileName;
    private EditBox serverAddress;
    private Button selectButton;
    private Button renameButton;
    private Button deleteButton;
    private Button modeButton;
    private Button addServerButton;
    private Button removeServerButton;
    private Button useSavedButton;
    private ServerAddressList serverList;
    private String selectedProfileId;
    private String selectedServerAddress;
    private boolean selectedServerSaved;
    private Component status = Component.empty();
    private static final int STATUS_WIDTH = 135;

    public ProfileManageScreen() { super(false); this.setSize(380, 210); }

    @Override public void init() {
        super.init(); this.children().clear();
        var store = store(); if (selectedProfileId == null && store.activeProfile() != null) selectedProfileId = store.activeProfile().id();
        profileList = new ProfileList((getWidth() / 2) - 176, getHeight() / 2 - 74, 135, 142, this); addRenderableWidget(profileList);
        profileName = new EditBox(getFontRender(), getWidth() / 2 - 30, getHeight() / 2 - 74, 158, 20, Component.empty()); addRenderableWidget(profileName);
        serverAddress = new EditBox(getFontRender(), getWidth() / 2 - 30, getHeight() / 2 - 2, 158, 20, Component.empty()); addRenderableWidget(serverAddress);
        serverList = new ServerAddressList(getWidth() / 2 - 30, getHeight() / 2 + 20, 158, 38, this); addRenderableWidget(serverList);
        selectButton = addRenderableWidget(Button.builder(Component.translatable("survivaltweaks.profile.select"), btn -> selectProfile()).pos(getWidth() / 2 + 132, getHeight() / 2 - 74).size(58, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("survivaltweaks.profile.new"), btn -> createProfile()).pos(getWidth() / 2 + 132, getHeight() / 2 - 52).size(58, 20).build());
        renameButton = addRenderableWidget(Button.builder(Component.translatable("survivaltweaks.profile.rename"), btn -> renameProfile()).pos(getWidth() / 2 + 132, getHeight() / 2 - 30).size(58, 20).build());
        deleteButton = addRenderableWidget(Button.builder(Component.translatable("survivaltweaks.profile.delete"), btn -> deleteProfile()).pos(getWidth() / 2 + 132, getHeight() / 2 - 8).size(58, 20).build());
        modeButton = addRenderableWidget(Button.builder(Component.empty(), btn -> cycleMode()).pos(getWidth() / 2 - 30, getHeight() / 2 - 30).size(158, 20).build());
        addServerButton = addRenderableWidget(Button.builder(Component.translatable("survivaltweaks.profile.add_server"), btn -> addServer()).pos(getWidth() / 2 - 30, getHeight() / 2 + 60).size(77, 20).build());
        removeServerButton = addRenderableWidget(Button.builder(Component.translatable("survivaltweaks.profile.remove_server"), btn -> removeServer()).pos(getWidth() / 2 + 51, getHeight() / 2 + 60).size(77, 20).build());
        useSavedButton = addRenderableWidget(Button.builder(Component.translatable("survivaltweaks.profile.use_saved"), btn -> useSavedServer()).pos(getWidth() / 2 - 30, getHeight() / 2 + 82).size(158, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("survivaltweaks.single.close"), btn -> Minecraft.getInstance().setScreen(new ScanManageScreen())).pos(getWidth() / 2 + 132, getHeight() / 2 + 82).size(58, 20).build());
        syncSelectedProfile();
    }

    private ProfileStore store() { return ScanController.INSTANCE.profileStore(); }
    private XRayProfile selectedProfile() { if (selectedProfileId == null) return null; for (var profile : store().profiles()) if (profile.id().equals(selectedProfileId)) return profile; return null; }
    private void setSelectedProfile(@Nullable XRayProfile profile) { selectedProfileId = profile == null ? null : profile.id(); selectedServerAddress = null; selectedServerSaved = false; if (profileName != null) profileName.setValue(profile == null ? "" : profile.name()); if (profileList != null) profileList.updateEntries(); if (serverList != null) serverList.updateEntries(); refreshButtons(); }
    private void syncSelectedProfile() { var selected = selectedProfile(); if (selected == null) { var profiles = store().profiles(); selected = profiles.isEmpty() ? null : profiles.get(0); } setSelectedProfile(selected); }
    private void refreshButtons() { var profile = selectedProfile(); var hasProfile = profile != null; var isDefault = hasProfile && profile == store().defaultProfile(); selectButton.active = hasProfile; renameButton.active = hasProfile; deleteButton.active = hasProfile && !isDefault; modeButton.active = hasProfile; addServerButton.active = hasProfile; removeServerButton.active = hasProfile; useSavedButton.active = hasProfile && !savedServers().isEmpty(); modeButton.setMessage(Component.translatable("survivaltweaks.profile.mode", modeLabel(hasProfile ? profile.activationMode() : ProfileActivationMode.NONE))); }
    private Component modeLabel(ProfileActivationMode mode) { return Component.translatable("survivaltweaks.profile.mode." + mode.jsonName()); }
    private void selectProfile() { var profile = selectedProfile(); if (profile == null) return; store().selectProfile(profile.id()); ScanController.INSTANCE.requestBlockFinder(true); status = Component.translatable("survivaltweaks.profile.message.selected", profile.name()); refreshButtons(); profileList.updateEntries(); }
    private void createProfile() { var profile = store().createProfile(profileName.getValue()); if (profile == null) { status = Component.translatable("survivaltweaks.profile.message.invalid_name"); return; } ScanController.INSTANCE.clearProfileActivationContext(); setSelectedProfile(profile); status = Component.translatable("survivaltweaks.profile.message.created", profile.name()); }
    private void renameProfile() { var profile = selectedProfile(); if (profile == null) return; if (!store().renameProfile(profile.id(), profileName.getValue())) { status = Component.translatable("survivaltweaks.profile.message.invalid_name"); return; } ScanController.INSTANCE.clearProfileActivationContext(); status = Component.translatable("survivaltweaks.profile.message.renamed"); syncSelectedProfile(); }
    private void deleteProfile() { var profile = selectedProfile(); if (profile == null || profile == store().defaultProfile()) { status = Component.translatable("survivaltweaks.profile.message.default_blocked"); refreshButtons(); return; } if (store().deleteProfile(profile.id())) { ScanController.INSTANCE.clearProfileActivationContext(); ScanController.INSTANCE.requestBlockFinder(true); selectedProfileId = null; syncSelectedProfile(); status = Component.translatable("survivaltweaks.profile.message.deleted"); } }
    private void cycleMode() { var profile = selectedProfile(); if (profile == null) return; if (profile.activationMode() == ProfileActivationMode.DEFAULT) { status = Component.translatable("survivaltweaks.profile.message.default_mode_blocked"); refreshButtons(); return; } var next = switch (profile.activationMode()) { case NONE -> ProfileActivationMode.DEFAULT; case DEFAULT -> ProfileActivationMode.SINGLEPLAYER; case SINGLEPLAYER -> ProfileActivationMode.MULTIPLAYER; case MULTIPLAYER -> ProfileActivationMode.NONE; }; store().setActivationMode(profile.id(), next); ScanController.INSTANCE.clearProfileActivationContext(); status = Component.translatable("survivaltweaks.profile.message.mode", modeLabel(next)); syncSelectedProfile(); }
    private void addServer() { var profile = selectedProfile(); if (profile == null) return; var address = serverAddress.getValue(); if (ProfileStore.normalizeServerAddress(address).isBlank()) { status = Component.translatable("survivaltweaks.profile.message.server_blank"); return; } if (serverAddressExists(address)) { status = Component.translatable("survivaltweaks.profile.message.server_duplicate"); return; } if (store().addServerAddress(profile.id(), address)) { ScanController.INSTANCE.clearProfileActivationContext(); status = Component.translatable("survivaltweaks.profile.message.server_added"); serverAddress.setValue(""); syncSelectedProfile(); } else { status = Component.translatable("survivaltweaks.profile.message.server_rejected"); } }
    private void removeServer() { var profile = selectedProfile(); if (profile == null) return; var address = selectedServerAddress != null && !selectedServerSaved ? selectedServerAddress : serverAddress.getValue(); if (ProfileStore.normalizeServerAddress(address).isBlank()) { status = Component.translatable("survivaltweaks.profile.message.server_blank"); return; } if (store().removeServerAddress(profile.id(), address)) { ScanController.INSTANCE.clearProfileActivationContext(); status = Component.translatable("survivaltweaks.profile.message.server_removed"); serverAddress.setValue(""); syncSelectedProfile(); } else { status = Component.translatable("survivaltweaks.profile.message.server_rejected"); } }
    private void useSavedServer() { var profile = selectedProfile(); if (profile == null) return; if (!selectedServerSaved || selectedServerAddress == null) { status = Component.translatable("survivaltweaks.profile.message.no_saved_selected"); return; } serverAddress.setValue(selectedServerAddress); if (serverAddressExists(selectedServerAddress)) { status = Component.translatable("survivaltweaks.profile.message.server_duplicate"); return; } if (store().addServerAddress(profile.id(), selectedServerAddress)) { ScanController.INSTANCE.clearProfileActivationContext(); status = Component.translatable("survivaltweaks.profile.message.server_added"); syncSelectedProfile(); return; } status = Component.translatable("survivaltweaks.profile.message.server_rejected"); }
    private boolean serverAddressExists(String address) { var normalized = ProfileStore.normalizeServerAddress(address); return store().profiles().stream().flatMap(profile -> profile.serverAddresses().stream()).map(ProfileStore::normalizeServerAddress).anyMatch(normalized::equals); }
    private void selectServerAddress(String address, boolean saved) { selectedServerAddress = address; selectedServerSaved = saved; serverAddress.setValue(address); if (serverList != null) serverList.updateEntries(); }
    private List<String> savedServers() { var addresses = new ArrayList<String>(); try { ServerList list = new ServerList(Minecraft.getInstance()); list.load(); for (int i = 0; i < list.size(); i++) { ServerData data = list.get(i); if (data != null && data.ip != null && !data.ip.isBlank()) addresses.add(data.ip); } } catch (Exception ignored) {} return addresses; }
    @Override public void renderExtra(GuiGraphicsExtractor graphics, int x, int y, float partialTicks) { graphics.text(getFontRender(), Component.translatable("survivaltweaks.profile.name"), getWidth() / 2 - 30, getHeight() / 2 - 86, Color.WHITE.getRGB()); graphics.text(getFontRender(), Component.translatable("survivaltweaks.profile.server"), getWidth() / 2 - 30, getHeight() / 2 - 14, Color.WHITE.getRGB()); graphics.text(getFontRender(), fitText(status.getString(), STATUS_WIDTH), getWidth() / 2 - 176, getHeight() / 2 + 71, Color.GRAY.getRGB()); }
    private String fitText(String text, int width) { var font = getFontRender(); if (font.width(text) <= width) return text; return font.plainSubstrByWidth(text, width - font.width("…")) + "…"; }
    @Override public boolean hasTitle() { return true; }
    @Override public Identifier getBackground() { return BG_LARGE; }
    @Override public String title() { return I18n.get("survivaltweaks.title.profile"); }

    class ProfileList extends ObjectSelectionList<ProfileList.ProfileSlot> {
        private final ProfileManageScreen parent;
        ProfileList(int x, int y, int width, int height, ProfileManageScreen parent) { super(ProfileManageScreen.this.minecraft, width, height, y, 22); this.parent = parent; this.setX(x); this.updateEntries(); }
        @Override public int getRowWidth() { return 124; }
        @Override protected int scrollBarX() { return this.getX() + this.getRowWidth() + 4; }
        void updateEntries() { this.clearEntries(); for (var profile : store().profiles()) this.addEntry(new ProfileSlot(profile, this)); }
        void selectSlot(ProfileSlot slot) { parent.setSelectedProfile(slot.profile); }
        class ProfileSlot extends ObjectSelectionList.Entry<ProfileSlot> {
            private final XRayProfile profile; private final ProfileList parent;
            ProfileSlot(XRayProfile profile, ProfileList parent) { this.profile = profile; this.parent = parent; }
            @Override public void extractContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean hovering, float partialTicks) { Font font = Minecraft.getInstance().font; boolean selected = profile.id().equals(selectedProfileId); boolean active = profile == store().activeProfile(); int color = selected ? Color.YELLOW.getRGB() : Color.WHITE.getRGB(); guiGraphics.text(font, profile.name(), this.getContentX(), this.getContentY() + 2, color); var mode = Component.translatable("survivaltweaks.profile.mode." + profile.activationMode().jsonName()).getString(); if (active) mode = mode + " *"; guiGraphics.text(font, mode, this.getContentX(), this.getContentY() + 12, Color.GRAY.getRGB()); }
            @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent mouse, boolean bl) { this.parent.selectSlot(this); return true; }
            @Override public @NotNull Component getNarration() { return Component.literal(profile.name()); }
        }
    }

    class ServerAddressList extends ObjectSelectionList<ServerAddressList.ServerAddressSlot> {
        private final ProfileManageScreen parent;
        ServerAddressList(int x, int y, int width, int height, ProfileManageScreen parent) { super(ProfileManageScreen.this.minecraft, width, height, y, 22); this.parent = parent; this.setX(x); this.updateEntries(); }
        @Override public int getRowWidth() { return 146; }
        @Override protected int scrollBarX() { return this.getX() + this.getRowWidth() + 4; }
        void updateEntries() { this.clearEntries(); var profile = selectedProfile(); if (profile != null) { for (var address : profile.serverAddresses()) this.addEntry(new ServerAddressSlot(address, false, this)); for (var address : savedServers()) if (profile.serverAddresses().stream().map(ProfileStore::normalizeServerAddress).noneMatch(ProfileStore.normalizeServerAddress(address)::equals)) this.addEntry(new ServerAddressSlot(ProfileStore.normalizeServerAddress(address), true, this)); } }
        void selectSlot(ServerAddressSlot slot) { parent.selectServerAddress(slot.address, slot.saved); }
        class ServerAddressSlot extends ObjectSelectionList.Entry<ServerAddressSlot> {
            private final String address; private final boolean saved; private final ServerAddressList parent;
            ServerAddressSlot(String address, boolean saved, ServerAddressList parent) { this.address = address; this.saved = saved; this.parent = parent; }
            @Override public void extractContent(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, boolean hovering, float partialTicks) { Font font = Minecraft.getInstance().font; boolean selected = address.equals(selectedServerAddress) && saved == selectedServerSaved; int labelColor = saved ? Color.GRAY.getRGB() : Color.LIGHT_GRAY.getRGB(); int addressColor = selected ? Color.YELLOW.getRGB() : Color.WHITE.getRGB(); var label = Component.translatable(saved ? "survivaltweaks.profile.server.saved" : "survivaltweaks.profile.server.current").getString(); guiGraphics.text(font, label + ":", this.getContentX(), this.getContentY() + 1, labelColor); guiGraphics.text(font, fitText(address, parent.getRowWidth() - 4), this.getContentX(), this.getContentY() + 11, addressColor); }
            @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent mouse, boolean bl) { this.parent.selectSlot(this); return true; }
            @Override public @NotNull Component getNarration() { return Component.literal(address); }
        }
    }
}
