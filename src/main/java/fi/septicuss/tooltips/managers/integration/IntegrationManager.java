package fi.septicuss.tooltips.managers.integration;

import fi.septicuss.tooltips.Tooltips;
import fi.septicuss.tooltips.managers.integration.impl.axgens.LookingAtAxGen;
import fi.septicuss.tooltips.managers.integration.impl.betonquest.BetonQuestCondition;
import fi.septicuss.tooltips.managers.integration.impl.betonquest.actions.EndConversationCommand;
import fi.septicuss.tooltips.managers.integration.impl.betonquest.actions.NextOptionCommand;
import fi.septicuss.tooltips.managers.integration.impl.betonquest.actions.SelectOptionCommand;
import fi.septicuss.tooltips.managers.integration.impl.betonquest.conversation.TooltipsConversationIO;
import fi.septicuss.tooltips.managers.integration.impl.betonquest.conversation.TooltipsConversationIOFactory;
import fi.septicuss.tooltips.managers.integration.impl.craftengine.CraftEngineFurnitureProvider;
import fi.septicuss.tooltips.managers.integration.impl.crucible.CrucibleFurnitureProvider;
import fi.septicuss.tooltips.managers.integration.impl.itemsadder.ItemsAdderFurnitureProvider;
import fi.septicuss.tooltips.managers.integration.impl.nexo.NexoFurnitureProvider;
import fi.septicuss.tooltips.managers.integration.impl.nexo.NexoListener;
import fi.septicuss.tooltips.managers.integration.impl.oraxen.OraxenFurnitureProvider;
import fi.septicuss.tooltips.managers.integration.impl.packetevents.PacketEventsPacketProvider;
import fi.septicuss.tooltips.managers.integration.impl.papi.TooltipsExpansion;
import fi.septicuss.tooltips.managers.integration.impl.protocollib.ProtocolLibPacketProvider;
import fi.septicuss.tooltips.managers.integration.impl.worldguard.WorldGuardAreaProvider;
import fi.septicuss.tooltips.managers.integration.providers.AreaProvider;
import fi.septicuss.tooltips.managers.integration.providers.FurnitureProvider;
import fi.septicuss.tooltips.managers.integration.providers.PacketProvider;
import fi.septicuss.tooltips.managers.integration.wrappers.FurnitureWrapper;
import fi.septicuss.tooltips.managers.preset.actions.command.ActionCommands;
import org.betonquest.betonquest.BetonQuest;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Optional;

public class IntegrationManager {

    private final Tooltips plugin;
    private final HashMap<String, FurnitureProvider> furnitureProviders = new HashMap<>();
    private final HashMap<String, AreaProvider> areaProviders = new HashMap<>();
    private PacketProvider packetProvider;

    public IntegrationManager(Tooltips plugin) {
        this.plugin = plugin;
    }

    public void registerDefaultIntegrations() {

        if (isPresent("packetevents")) {
            this.setPacketProvider(new PacketEventsPacketProvider());
        } else if (isPresent("ProtocolLib")) {
            this.setPacketProvider(new ProtocolLibPacketProvider());
        }

        if (isPresent("Nexo")) {
            this.addFurnitureProvider(new NexoFurnitureProvider());
            this.plugin.getServer().getPluginManager().registerEvents(new NexoListener(plugin), plugin);
        }

        if (isPresent("Oraxen")) {
            this.addFurnitureProvider(new OraxenFurnitureProvider());
        }

        if (isPresent("ItemsAdder")) {
            this.addFurnitureProvider(new ItemsAdderFurnitureProvider());
        }

        if (isPresent("MythicCrucible")) {
            this.addFurnitureProvider(new CrucibleFurnitureProvider());
        }

        if (isPresent("CraftEngine")) {
            this.addFurnitureProvider(new CraftEngineFurnitureProvider());
        }

        if (isPresent("WorldGuard")) {
            this.addAreaProvider(new WorldGuardAreaProvider());
        }

        if (isPresent("AxGens")) {
            Tooltips.get().getConditionManager().register(new LookingAtAxGen());
        }

        if (isPresent("PlaceholderAPI")) {
            TooltipsExpansion expansion = new TooltipsExpansion();
            if (expansion.isRegistered())
                expansion.unregister();
            expansion.register();
        }

        if (isPresent("BetonQuest")) {
            Plugin betonQuestPlugin = Bukkit.getPluginManager().getPlugin("BetonQuest");

            if (betonQuestPlugin == null || !betonQuestPlugin.isEnabled())
                return;

            PluginDescriptionFile description = betonQuestPlugin.getDescription();
            String version = description.getVersion();

            if (!version.startsWith("3")) {
                Tooltips.warn("This version of Tooltips is only compatible with BetonQuest version larger than 3");
                return;
            }

            BetonQuest.getInstance().getFeatureRegistries().conversationIO().register("tooltips", new TooltipsConversationIOFactory());
            Tooltips.get().getConditionManager().register(new BetonQuestCondition());
            ActionCommands.addCommand("selectoption", new SelectOptionCommand());
            ActionCommands.addCommand("endconversation", new EndConversationCommand());
            ActionCommands.addCommand("nextoption", new NextOptionCommand());
        }

    }

    public void disable() {

        if (isPresent("BetonQuest")) {
            TooltipsConversationIO.endConversations();
        }

    }

    public Optional<FurnitureWrapper> getFurniture(final @Nonnull Block block) {
        for (FurnitureProvider provider : this.furnitureProviders.values()) {
            final FurnitureWrapper furniture = provider.getFurniture(block);

            if (furniture == null) {
                continue;
            }

            return Optional.ofNullable(provider.getFurniture(block));
        }
        return Optional.empty();
    }

    public Optional<FurnitureWrapper> getFurniture(final @Nonnull Entity entity) {
        for (FurnitureProvider provider : this.furnitureProviders.values()) {
            final FurnitureWrapper furniture = provider.getFurniture(entity);

            if (furniture == null) {
                continue;
            }

            return Optional.ofNullable(provider.getFurniture(entity));
        }
        return Optional.empty();
    }

    public boolean isPresent(String plugin) {
        return (Bukkit.getPluginManager().getPlugin(plugin) != null);
    }

    public FurnitureProvider getFurnitureProvider(String id) {
        return this.furnitureProviders.get(id);
    }

    public HashMap<String, FurnitureProvider> getFurnitureProviders() {
        return furnitureProviders;
    }

    public void addFurnitureProvider(FurnitureProvider furnitureProvider) {
        this.furnitureProviders.put(furnitureProvider.identifier(), furnitureProvider);
    }

    public void removeFurnitureProvider(String id) {
        this.furnitureProviders.remove(id);
    }

    public AreaProvider getAreaProvider(String id) {
        return this.areaProviders.get(id);
    }

    public HashMap<String, AreaProvider> getAreaProviders() {
        return areaProviders;
    }

    public void addAreaProvider(AreaProvider areaProvider) {
        this.areaProviders.put(areaProvider.identifier(), areaProvider);
    }

    public void removeAreaProvider(String id) {
        this.areaProviders.remove(id);
    }

    public PacketProvider getPacketProvider() {
        return this.packetProvider;
    }

    public void setPacketProvider(PacketProvider packetProvider) {
        this.packetProvider = packetProvider;
    }

}
