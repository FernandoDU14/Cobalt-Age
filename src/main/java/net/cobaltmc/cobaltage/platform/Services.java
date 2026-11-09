//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.cobaltmc.cobaltage.platform;

import net.cobaltmc.cobaltage.CobaltAge;
import net.cobaltmc.cobaltage.platform.services.IPlatformHelper;

import java.util.ServiceLoader;

public class Services {
    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);

    public static <T> T load(Class<T> clazz) {
        T loadedService = (T)ServiceLoader.load(clazz, Services.class.getClassLoader()).findFirst().orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
        CobaltAge.LOGGER.debug("Loaded {} for service {}", loadedService, clazz);
        return loadedService;
    }
}
