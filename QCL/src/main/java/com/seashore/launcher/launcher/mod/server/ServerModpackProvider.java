package com.seashore.launcher.launcher.mod.server;

import com.google.gson.JsonParseException;
import com.seashore.launcher.launcher.mod.Modpack;
import com.seashore.launcher.launcher.mod.ModpackProvider;
import com.seashore.launcher.utils.gson.JsonUtils;
import com.seashore.launcher.utils.io.ZipTools;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import org.apache.commons.compress.archivers.zip.ZipFile;

/* loaded from: classes2.dex */
public final class ServerModpackProvider implements ModpackProvider {
    public static final ServerModpackProvider INSTANCE = new ServerModpackProvider();

    @Override // com.seashore.launcher.launcher.mod.ModpackProvider
    public String getName() {
        return "Server";
    }

    @Override // com.seashore.launcher.launcher.mod.ModpackProvider
    public Modpack readManifest(ZipFile zipFile, Path path, Charset charset) throws IOException, JsonParseException {
        return ((ServerModpackManifest) JsonUtils.fromNonNullJson(ZipTools.readTextZipEntry(zipFile, "server-manifest.json"), ServerModpackManifest.class)).toModpack(charset);
    }
}
