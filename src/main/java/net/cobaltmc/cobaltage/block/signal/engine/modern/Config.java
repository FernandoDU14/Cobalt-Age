package net.cobaltmc.cobaltage.block.signal.engine.modern;

import net.cobaltmc.cobaltage.CobaltAge;
import net.cobaltmc.cobaltage.util.interfaces.mixin.IServerLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelStorageSource;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Objects;

public interface Config {
    static Config forLevel(ServerLevel level, LevelStorageSource.LevelStorageAccess storage) {
        return (Config)(level.dimension() == Level.OVERWORLD ? new Primary(storage) : new Derived(((IServerLevel)level.getServer().overworld()).cobaltage$getWireHandler().getConfig()));
    }

    boolean getEnabled();

    void setEnabled(boolean var1);

    UpdateOrder getUpdateOrder();

    void setUpdateOrder(UpdateOrder var1);

    void load();

    void save(boolean var1);

    public static class Primary implements Config {
        private final Path path;
        private boolean enabled = true;
        private UpdateOrder updateOrder;
        private boolean modified;

        public Primary(LevelStorageSource.LevelStorageAccess storage) {
            this.updateOrder = UpdateOrder.HORIZONTAL_FIRST_OUTWARD;
            this.path = storage.getDimensionPath(Level.OVERWORLD).resolve("cobaltage.conf");
        }

        public boolean getEnabled() {
            return this.enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
            CobaltAge.MODERN_SIGNAL_ENGINE = enabled;
            this.modified = true;
        }

        public UpdateOrder getUpdateOrder() {
            return this.updateOrder;
        }

        public void setUpdateOrder(UpdateOrder updateOrder) {
            this.updateOrder = (UpdateOrder)Objects.requireNonNull(updateOrder);
            this.modified = true;
        }

        public void load() {
            if (Files.exists(this.path, new LinkOption[0])) {
                try (BufferedReader br = Files.newBufferedReader(this.path)) {
                    String line;
                    while((line = br.readLine()) != null) {
                        if (!line.startsWith("#")) {
                            String[] parts = line.split("[=]");
                            if (parts.length == 2) {
                                String key = parts[0];
                                String value = parts[1];

                                try {
                                    switch (key) {
                                        case "enabled":
                                            this.setEnabled(Boolean.parseBoolean(value));
                                            break;
                                        case "update-order":
                                            this.setUpdateOrder(UpdateOrder.byId(value));
                                            break;
                                        default:
                                            CobaltAge.LOGGER.info("skipping unknown option '{}' in Cobalt Age config", key);
                                    }
                                } catch (Exception e) {
                                    CobaltAge.LOGGER.info("skipping bad value '{}' for option '{}' in Cobalt Age config!", new Object[]{value, key, e});
                                }
                            }
                        }
                    }

                    this.modified = false;
                } catch (IOException e) {
                    CobaltAge.LOGGER.info("unable to load Cobalt Age config!", e);
                    this.modified = true;
                }
            } else {
                this.modified = true;
            }

        }

        public void save(boolean silent) {
            if (this.modified) {
                if (!silent) {
                    CobaltAge.LOGGER.info("saving Cobalt Age config");
                }

                try {
                    BufferedWriter bw = Files.newBufferedWriter(this.path);

                    try {
                        bw.write("enabled");
                        bw.write(61);
                        bw.write(Boolean.toString(this.enabled));
                        bw.newLine();
                        bw.write("update-order");
                        bw.write(61);
                        bw.write(this.updateOrder.id());
                        bw.newLine();
                    } catch (Throwable var11) {
                        if (bw != null) {
                            try {
                                bw.close();
                            } catch (Throwable var10) {
                                var11.addSuppressed(var10);
                            }
                        }

                        throw var11;
                    }

                    if (bw != null) {
                        bw.close();
                    }
                } catch (IOException e) {
                    CobaltAge.LOGGER.info("unable to save Cobalt Age config!", e);
                } finally {
                    this.modified = false;
                }
            }

        }
    }

    public static class Derived implements Config {
        private final Config delegate;

        public Derived(Config delegate) {
            this.delegate = delegate;
        }

        public boolean getEnabled() {
            return this.delegate.getEnabled();
        }

        public void setEnabled(boolean enabled) {
            this.delegate.setEnabled(enabled);
        }

        public UpdateOrder getUpdateOrder() {
            return this.delegate.getUpdateOrder();
        }

        public void setUpdateOrder(UpdateOrder updateOrder) {
            this.delegate.setUpdateOrder(updateOrder);
        }

        public void load() {
        }

        public void save(boolean silent) {
        }
    }
}

