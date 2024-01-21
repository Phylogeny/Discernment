package com.github.phylogeny.discernment;

import com.google.common.base.Predicates;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

@Mod.EventBusSubscriber(modid = Discernment.MOD_ID, value = Dist.CLIENT)
public class Config {
    private static final boolean GENERATE_LANG_FILE_LINES = !FMLEnvironment.production;

    private static String getTranslationKeyBase(String name) {
        if (GENERATE_LANG_FILE_LINES)
            System.out.printf("\"_comment\": \"%s Configs\",%n", name.toUpperCase().charAt(0) + name.substring(1).toLowerCase());

        return String.join(".", "config", Discernment.MOD_ID, name.toLowerCase()) + ".";
    }

    public static class Server {
        private static final String TRANSLATION_KEY_BASE = getTranslationKeyBase("server");
        private static final Builder BUILDER = new Builder();
        public static final Enchantment ENCHANTMENT = new Enchantment();
        public static final Particles PARTICLES = new Particles();
        public static final Sounds SOUNDS = new Sounds();

        public static class Enchantment extends ConfigBase {
            public final BooleanValue availableInEnchantingTable;

            Enchantment() {
                super("Enchantment", "Configures the discernment enchantment", TRANSLATION_KEY_BASE, BUILDER);

                availableInEnchantingTable = define("Available In Enchanting Table", "enchanting_table",
                        "Specifies whether discernment shows up as a possible enchantment in an enchanting table.",
                        name -> builder.define(name, true));

                builder.pop();
            }
        }

        public static class Particles extends RegistryNameList<ParticleType<?>> {
            public final IntValue directCount;

            Particles() {
                super("Particles", "Configures the type and number of spawned particles", TRANSLATION_KEY_BASE,
                        "Spawned particles", "particles will spawn", "none will spawn",
                        "Note that only particle types which extends the ParticleOptions class are allowed. " +
                        "Rare examples of particles that don't do so are ones that require additional data (like a BlockState, " +
                        "in the case of ParticleTypes.FALLING_DUST).", BuiltInRegistries.PARTICLE_TYPE, "enchanted_hit");

                directCount = define("Count Direct", "count.direct",
                        "Specifies the number of particles spawned when an entity directly attacks another entity. Note that this " +
                                "only applies when the attack is direct, and when a raytrace of the attacker to to target entity is successful. " +
                                "Otherwise, a fixed number of particles are spawned all around the target entity in the normal vanilla manner.",
                                name -> builder.defineInRange(name, 15, 1, Integer.MAX_VALUE));

                builder.pop();
            }
        }

        public static class Sounds extends RegistryNameList<SoundEvent> {
            public final SoundRange volume, pitch;

            Sounds() {
                super("Sounds", "Configures the type, volume, and pitch of played sounds", TRANSLATION_KEY_BASE, "Played sounds",
                        "a sound will play", "none will play", "", BuiltInRegistries.SOUND_EVENT, "block.amethyst_cluster.fall", "block.amethyst_block.fall");

                volume = new SoundRange("Volume", this.translationKeyBase, 0.5D, 0.5D);
                pitch = new SoundRange("Pitch", this.translationKeyBase, -1D, -1D);

                builder.pop();
            }

            public static class SoundRange extends ConfigBase {
                private static final Random RAND = new Random();
                private final DoubleValue min, max;

                SoundRange(String name, String translationKeyBase, double defaultMin, double defaultMax) {
                    super(name, String.format("Sounds will play within this %s range.", name.toLowerCase()), translationKeyBase, BUILDER);

                    min = getMin(name, "Min", defaultMin);
                    max = getMin(name, "Max", defaultMax);

                    builder.pop();
                }

                private DoubleValue getMin(String name, String value, double defaultValue) {
                    return define(value, value.toLowerCase(),
                            String.format("Specifies the %simum %s that sounds will play at.", value.toLowerCase(), name.toLowerCase()),
                            s -> builder.defineInRange(value, defaultValue, -Double.MAX_VALUE, Double.MAX_VALUE));
                }

                public float randomInRange() {
                    return (float) (RAND.nextDouble() * (max.get() - min.get()) + min.get());
                }
            }
        }

        public static class RegistryNameList<T> extends ConfigBase {
            private final ConfigValue<List<? extends String>> nameStrings;
            private final List<String> defaultNames;
            private List<ResourceLocation> names;
            public final BooleanValue enabled;
            private final Registry<T> registry;

            public List<ResourceLocation> getNames() {
                return names;
            }

            RegistryNameList(String name, String comment, String translationKeyBase, String type,
                             String ifEnabled, String ifDisabled, String note, Registry<T> registry, String... defaults) {
                super(name, comment, translationKeyBase, BUILDER);
                defaultNames = Arrays.stream(defaults).toList();
                this.registry = registry;

                enabled = define("Enabled", "enabled",
                        String.format("If set to true, %s when damage is blocked by the " +
                                "Discernment enchantment. If set to false, %s.", ifEnabled, ifDisabled),
                        s -> builder.define(s, true));

                nameStrings = define("Names", "names",
                        String.join(" ", type, "will be randomly selected from a pool of types " +
                                "defined by this list of registry names.", note),
                        s -> builder.worldRestart().defineList(s, defaultNames, Predicates.instanceOf(String.class)));
            }

            public void processNames() {
                names = nameStrings.get().stream().map(ResourceLocation::new)
                        .filter(name -> Discernment.getRegistryValue(registry, name).isPresent()).toList();
                if (names.isEmpty()) {
                    Discernment.LOGGER.error(String.format("Reverting to %s default values", registry.asLookup().key().location()));
                    names = defaultNames.stream().map(ResourceLocation::new).toList();
                }
            }
        }
    }

    private static class ConfigBase {
        protected final String translationKeyBase;
        protected final Builder builder;

        public ConfigBase(String name, String comment, String translationKeyBase, Builder builder) {
            translationKeyBase += name.toLowerCase();
            printLangLines(translationKeyBase, name, comment);
            this.translationKeyBase = translationKeyBase + ".";
            this.builder = builder.comment(comment).push(name);
        }

        protected <T extends ConfigValue<?>> T define(String name, String translationKey, String comment, Function<String, T> valueGetter) {
            translationKey = translationKeyBase + translationKey;
            printLangLines(translationKey, name, comment);
            builder.translation(translationKey).comment(comment);
            return valueGetter.apply(name);
        }

        private void printLangLines(String translationKey, String name, String comment) {
            if (GENERATE_LANG_FILE_LINES) {
                printLangLine(translationKey, name);
                printLangLine(translationKey + ".tooltip", comment);
            }
        }

        private void printLangLine(String translationKey, String comment) {
            System.out.println("\"" + translationKey + "\": \"" + comment + "\",");
        }
    }

    private static void processNames() {
        Server.PARTICLES.processNames();
        Server.SOUNDS.processNames();
    }

    private static void onLoad(final ModConfigEvent.Loading event) {
        processNames();
    }

    @SubscribeEvent
    public static void onServerJoin(ClientPlayerNetworkEvent.LoggingIn event) {
        processNames();
    }

    private static final ModConfigSpec SERVER = Server.BUILDER.build();

    public static void register(IEventBus bus) {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SERVER);
        bus.addListener(Config::onLoad);
    }
}