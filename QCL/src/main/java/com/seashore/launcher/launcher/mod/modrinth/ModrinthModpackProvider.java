package com.seashore.launcher.launcher.mod.modrinth;

import android.os.AsyncTask;
import com.google.gson.JsonParseException;
import com.seashore.launcher.launcher.mod.Modpack;
import com.seashore.launcher.launcher.mod.ModpackProvider;
import com.seashore.launcher.utils.gson.JsonUtils;
import com.seashore.launcher.utils.io.ZipTools;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import org.apache.commons.compress.archivers.zip.ZipFile;

/* loaded from: classes2.dex */
public final class ModrinthModpackProvider implements ModpackProvider {
    public static final ModrinthModpackProvider INSTANCE = new ModrinthModpackProvider();

    @Override // com.seashore.launcher.launcher.mod.ModpackProvider
    public String getName() {
        return "Modrinth";
    }

    @Override // com.seashore.launcher.launcher.mod.ModpackProvider
    public Modpack readManifest(ZipFile zipFile, Path path, Charset charset) throws IOException, JsonParseException {
        final ModrinthManifest modrinthManifest = (ModrinthManifest) JsonUtils.fromNonNullJson(ZipTools.readTextZipEntry(zipFile, "modrinth.index.json"), ModrinthManifest.class);
        return new Modpack(modrinthManifest.getName(), "", modrinthManifest.getVersionId(), modrinthManifest.getGameVersion(), modrinthManifest.getSummary(), charset, modrinthManifest) { // from class: com.seashore.launcher.launcher.mod.modrinth.ModrinthModpackProvider.1
            @Override // com.seashore.launcher.launcher.mod.Modpack
            public AsyncTask getInstallTask(File file, String str) {
                return new ModrinthInstallTask(file, this, modrinthManifest, str);
            }
        };
    }
}
