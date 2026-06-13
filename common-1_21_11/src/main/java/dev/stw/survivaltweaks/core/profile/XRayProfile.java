package dev.stw.survivaltweaks.core.profile;

import dev.stw.survivaltweaks.core.scanner.ScanStore;

import java.util.ArrayList;
import java.util.List;

public class XRayProfile {
    private final String id;
    private String name;
    private ProfileActivationMode activationMode;
    private final List<String> serverAddresses = new ArrayList<>();
    private final ScanStore scanStore;
    private final Runnable onSave;

    private XRayProfile(String id, String name, ProfileActivationMode mode, ScanStore scanStore, Runnable onSave) {
        this.id = id; this.name = name; this.activationMode = mode; this.scanStore = scanStore; this.onSave = onSave;
    }
    public static XRayProfile create(String id, String name, ProfileActivationMode mode, Runnable onSave) {
        return new XRayProfile(id, name, mode, new ScanStore(onSave), onSave);
    }
    public String id() { return id; }
    public String name() { return name; }
    void setName(String name) { if (!java.util.Objects.equals(this.name, name)) { this.name = name; onSave.run(); } }
    public ProfileActivationMode activationMode() { return activationMode; }
    void setActivationMode(ProfileActivationMode activationMode) { if (this.activationMode != activationMode) { this.activationMode = activationMode; onSave.run(); } }
    public List<String> serverAddresses() { return java.util.Collections.unmodifiableList(serverAddresses); }
    void addServerAddress(String address) { if (!serverAddresses.contains(address)) { serverAddresses.add(address); onSave.run(); } }
    void removeServerAddress(String address) { if (serverAddresses.remove(address)) onSave.run(); }
    void setServerAddresses(List<String> addresses) { if (!serverAddresses.equals(addresses)) { serverAddresses.clear(); serverAddresses.addAll(addresses); onSave.run(); } }
    public ScanStore scanStore() { return scanStore; }
}
