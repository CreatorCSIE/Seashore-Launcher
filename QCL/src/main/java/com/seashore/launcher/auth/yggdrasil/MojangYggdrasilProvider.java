package com.seashore.launcher.auth.yggdrasil;

import com.seashore.launcher.utils.gson.UUIDTypeAdapter;
import com.seashore.launcher.utils.io.NetworkUtils;
import java.net.URL;
import java.util.UUID;

/* loaded from: classes2.dex */
public class MojangYggdrasilProvider implements YggdrasilProvider {
    public String toString() {
        return "mojang";
    }

    @Override // com.seashore.launcher.auth.yggdrasil.YggdrasilProvider
    public URL getAuthenticationURL() {
        return NetworkUtils.toURL("https://authserver.mojang.com/authenticate");
    }

    @Override // com.seashore.launcher.auth.yggdrasil.YggdrasilProvider
    public URL getRefreshmentURL() {
        return NetworkUtils.toURL("https://authserver.mojang.com/refresh");
    }

    @Override // com.seashore.launcher.auth.yggdrasil.YggdrasilProvider
    public URL getValidationURL() {
        return NetworkUtils.toURL("https://authserver.mojang.com/validate");
    }

    @Override // com.seashore.launcher.auth.yggdrasil.YggdrasilProvider
    public URL getInvalidationURL() {
        return NetworkUtils.toURL("https://authserver.mojang.com/invalidate");
    }

    @Override // com.seashore.launcher.auth.yggdrasil.YggdrasilProvider
    public URL getSkinUploadURL(UUID uuid) throws UnsupportedOperationException {
        return NetworkUtils.toURL("https://api.mojang.com/user/profile/" + UUIDTypeAdapter.fromUUID(uuid) + "/skin");
    }

    @Override // com.seashore.launcher.auth.yggdrasil.YggdrasilProvider
    public URL getProfilePropertiesURL(UUID uuid) {
        return NetworkUtils.toURL("https://sessionserver.mojang.com/session/minecraft/profile/" + UUIDTypeAdapter.fromUUID(uuid));
    }
}
