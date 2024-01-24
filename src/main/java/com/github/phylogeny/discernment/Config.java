package com.github.phylogeny.discernment;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.*;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Mod.EventBusSubscriber(modid = Discernment.MOD_ID, value = Dist.CLIENT)
public class Config {
    private static final boolean GENERATE_LANG_FILE_LINES = !FMLEnvironment.production;

    private static String getTranslationKeyBase(String name) {
        if (GENERATE_LANG_FILE_LINES)
            System.out.printf("\"_comment\": \"%s Configs\",%n", name.toUpperCase().charAt(0) + name.substring(1).toLowerCase());

        return String.join(".", "config", Discernment.MOD_ID, name.toLowerCase()) + ".";
    }

    private enum PatternFlag {
        UNIX_LINES(Pattern.UNIX_LINES),
        CASE_INSENSITIVE(Pattern.CASE_INSENSITIVE),
        COMMENTS(Pattern.COMMENTS),
        MULTILINE(Pattern.MULTILINE),
        LITERAL(Pattern.LITERAL),
        DOTALL(Pattern.DOTALL),
        UNICODE_CASE(Pattern.UNICODE_CASE),
        CANON_EQ(Pattern.CANON_EQ),
        UNICODE_CHARACTER_CLASS(Pattern.UNICODE_CHARACTER_CLASS);

        private final int value;

        PatternFlag(int value) {
            this.value = value;
        }
    }

    public static class Server {
        private static final String TRANSLATION_KEY_BASE = getTranslationKeyBase("server");
        private static final Builder BUILDER = new Builder();
        public static final Functionality FUNCTIONALITY = new Functionality();
        public static final Particles PARTICLES = new Particles();
        public static final Sounds SOUNDS = new Sounds();

        public static class Functionality extends ConfigBase {
            public final BooleanValue availableInEnchantingTable;
            public final Protection protection;

            Functionality() {
                super("Functionality", "Configures functionality of the discernment effect/enchantment", TRANSLATION_KEY_BASE, BUILDER);

                availableInEnchantingTable = define("Available In Enchanting Table", "enchanting_table",
                        "Specifies whether the discernment enchantment shows up as a possible enchantment in an enchanting table.",
                        name -> builder.define(name, true));

                protection = new Protection(translationKeyBase);

                builder.pop();
            }

            public static class Protection extends ConfigBase {
                public final NamedEntity namedEntityBlacklist, namedEntityWhitelist;
                public final BooleanValue protectPeacefulMobs;
                public final BooleanValue protectPlayers;
                public final BooleanValue protectOwnedEntities;

                Protection(String translationKeyBase) {
                    super("Protection", "Configures the creative for determining which entities are protected by the discernment effect/enchantment", translationKeyBase, BUILDER);

                    namedEntityBlacklist = new NamedEntity("Blacklist", "prevented from being protected by discernment", this.translationKeyBase, "(?:unprotected)");

                    namedEntityWhitelist = new NamedEntity("Whitelist", "protected by discernment", this.translationKeyBase, "(?:protected)");

                    protectPeacefulMobs = define("Protect Peaceful Mobs", "peaceful",
                            "Specifies whether peaceful mobs will be protected by discernment.",
                            name -> builder.define(name, true));

                    protectPlayers = define("Protect Players", "players",
                            "Specifies whether players will be protected by discernment.",
                            name -> builder.define(name, true));

                    protectOwnedEntities = define("Protect Owned Entities", "owned",
                            "Specifies whether entities owned by the entity with discernment will be protected.",
                            name -> builder.define(name, true));

                    builder.pop();
                }

                public static class NamedEntity extends ConfigBase {
                    private static final List<String> PATTERN_FLAG_VALIDATORS = Arrays.stream(PatternFlag.values()).map(Enum::name).toList();
                    public final BooleanValue enabled;
                    public final ConfigValue<List<? extends String>> namePatternFlags;
                    public final ConfigValue<String> nameRegex;
                    private Pattern namePattern;
                    private boolean useNamePattern;

                    NamedEntity(String namePrefix, String description, String translationKeyBase, String defaultRegex) {
                        super(namePrefix + " Named Entities", "Configures the protection of entities named with a name tag", translationKeyBase, BUILDER);

                        enabled = define("Enabled", "enabled",
                                String.format("Specifies whether entities named with a name tag will be %s. The Name Regex " +
                                        "Pattern/Flags configs determine which names will provide protection.", description),
                                name -> builder.define(name, true));

                        nameRegex = define("Name Regex Pattern", "pattern",
                                "Specifies the regular expression pattern that determine which names will provide " +
                                        "protection. If an entity's custom name matches the pattern, it will be protected. " +
                                        "Simple Examples: (?:) will match characters between the ? and :, so (?:test) " +
                                        "will match if a name contains 'test'. Starting with ^ will only match against the " +
                                        "start of the name, so ^(?:test) will match the name 'test1' but not '2test1'. " +
                                        "Ending with $ will only match against the end of the name, so (?:test)$ will match " +
                                        "the name '2test' but not '2test1'. Adding | between elements will match if either " +
                                        "matches, so ^(?:test1)|(?:test2) will match the name 'test1_test2_test3' or " +
                                        "'test1_test3_test2' or 'test3_test2' but not 'test1_test3'. As this is only a small " +
                                        "fraction of what regular expressions can do, visit regex101.com and select 'Java' " +
                                        "for more help. The Name Regex Flags config determines the flags to use with this pattern.",
                                name -> builder.define(name, defaultRegex));

                        namePatternFlags = define("Name Regex Flags", "flags",
                                "Specifies the flags that determine the matching behavior of the regular expression " +
                                        "pattern specified by the Name Regex Pattern config. Possible values are: " +
                                        "UNIX_LINES (enables Unix lines mode), CASE_INSENSITIVE (enables case-insensitive matching), " +
                                        "COMMENTS (permits whitespace and comments in pattern), MULTILINE (enables multiline mode), " +
                                        "LITERAL (enables literal parsing of the pattern), DOTALL (enables dotall mode), " +
                                        "UNICODE_CASE (enables Unicode-aware case folding), CANON_EQ (enables canonical equivalence), " +
                                        "UNICODE_CHARACTER_CLASS (enables the Unicode version of Predefined character classes and " +
                                        "POSIX character classes). Visit the Filed Detail section at " +
                                        "docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html " +
                                        "for a more in-depth description of each flag.",
                                name -> builder.defineListAllowEmpty(name,
                                        Lists.newArrayList(PatternFlag.CASE_INSENSITIVE.name(), PatternFlag.COMMENTS.name()),
                                        element -> element instanceof String s && PATTERN_FLAG_VALIDATORS.contains(s.toUpperCase())));

                        builder.pop();
                    }

                    public void constructRegex() {
                        int flags = 0;
                        for (String patternFlag : Sets.newHashSet(namePatternFlags.get()))
                            flags |= PatternFlag.valueOf(patternFlag.toUpperCase()).value;

                        try {
                            namePattern = Pattern.compile(nameRegex.get(), flags);
                            useNamePattern = enabled.get();
                        } catch (PatternSyntaxException exception) {
                            useNamePattern = false;
                            Discernment.LOGGER.error(String.format("The pattern %s contains Java regex pattern syntax error(s). " +
                                    "As a result, named entities will not be protected. See the java.io.Serializable.Pattern JavaDoc " +
                                    "and/or visit regex101.com for help.", nameRegex.get()));
                        }
                    }

                    public boolean useNamePattern() {
                        return useNamePattern;
                    }

                    public boolean namePatternMatches(Component component) {
                        return namePattern.matcher(component.getString()).find();
                    }
                }
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

                volume = new SoundRange("Volume", translationKeyBase, 0.5D, 0.5D);
                pitch = new SoundRange("Pitch", translationKeyBase, -1D, -1D);

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
            translationKeyBase += name.toLowerCase().replace(" ", "_");
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

    private static void onLoadAndLoggIn() {
        Server.PARTICLES.processNames();
        Server.SOUNDS.processNames();
        Server.FUNCTIONALITY.protection.namedEntityWhitelist.constructRegex();
        Server.FUNCTIONALITY.protection.namedEntityBlacklist.constructRegex();
    }

    private static void onLoad(final ModConfigEvent.Loading event) {
        onLoadAndLoggIn();
    }

    @SubscribeEvent
    public static void onServerJoin(ClientPlayerNetworkEvent.LoggingIn event) {
        onLoadAndLoggIn();
    }

    private static final ModConfigSpec SERVER = Server.BUILDER.build();

    public static void register(IEventBus bus) {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SERVER);
        bus.addListener(Config::onLoad);
    }
}