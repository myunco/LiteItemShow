package ml.mcos.liteitemshow;

import ml.mcos.liteitemshow.metrics.Metrics;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.List;

public class LiteItemShow extends JavaPlugin implements Listener {
    public String keyword;
    Class<?> NMS_NBTTagCompound;
    Class<?> OBC_CraftItemStack;
    Class<?> NMS_ItemStack;
    Method OBC_CraftItemStack_asNMSCopy;
    Method NMS_ItemStack_save;
    Method NMS_ItemStack_a;
    boolean fixedServer;
    int MCVersion;

    @Override
    public void onEnable() {
        initConfig();
        String OBCPackage = getServer().getClass().getPackage().getName();
        String version = OBCPackage.substring(23);
        try {
            MCVersion = Integer.parseInt(version.split("_")[1]);
            OBC_CraftItemStack = Class.forName(OBCPackage + ".inventory.CraftItemStack");
            if (MCVersion < 17) {
                String NMSPackage = "net.minecraft.server." + version;
                NMS_NBTTagCompound = Class.forName(NMSPackage + ".NBTTagCompound");
                NMS_ItemStack = Class.forName(NMSPackage + ".ItemStack");
                if (MCVersion == 12) {
                    fixedServer = getServer().getName().equals("CatServer") || getServer().getName().equals("Paper");
                    NMS_ItemStack_a = NMS_ItemStack.getMethod("a");
                }
            } else {
                NMS_NBTTagCompound = Class.forName("net.minecraft.nbt.NBTTagCompound");
                NMS_ItemStack = Class.forName("net.minecraft.world.item.ItemStack");
            }
            OBC_CraftItemStack_asNMSCopy = OBC_CraftItemStack.getMethod("asNMSCopy", ItemStack.class);
            NMS_ItemStack_save = NMS_ItemStack.getMethod("save", NMS_NBTTagCompound);
            getServer().getPluginManager().registerEvents(this, this);
            new Metrics(this, 13325);
        } catch (Exception e) {
            e.printStackTrace();
            getLogger().info("[LiteItemShow] 插件加载出错：不受支持的服务端版本");
        }
    }

    public void initConfig() {
        saveDefaultConfig();
        reloadConfig();
        keyword = getConfig().getString("keyword");
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return false;
        }
        switch (args[0].toLowerCase()) {
            case "reload":
                initConfig();
                sendMessage(sender, "§a配置文件重载完成。");
                break;
            case "version":
                sendMessage(sender, "§a当前版本: §b" + getDescription().getVersion());
                break;
            default:
                sendMessage(sender, "§6错误: 未知的命令参数。");
        }
        return true;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return TabComplete.getCompleteList(args, TabComplete.getTabList(args, command.getName()));
    }

    private void sendMessage(CommandSender sender, String msg) {
        sender.sendMessage("§8[§3LiteItemShow§8] " + msg);
    }

    private String getItemNBT(ItemStack item) {
        try {
            return NMS_ItemStack_save.invoke(OBC_CraftItemStack_asNMSCopy.invoke(OBC_CraftItemStack, item), NMS_NBTTagCompound.newInstance()).toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "{id:\"minecraft:air\"}";
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void asyncPlayerChatEvent(AsyncPlayerChatEvent event) {
        String msg = event.getMessage();
        if (msg.contains(keyword) && event.getPlayer().hasPermission("LiteItemShow.show")) {
            ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
            if (item.getType() != Material.AIR) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    event.setCancelled(true);
                    String nbt = getItemNBT(item);
                    ComponentBuilder builder = new ComponentBuilder("");
                    builder.append(TextComponent.fromLegacyText(getTextLeft(event.getFormat(), "%2$s").replaceAll("%1\\$s", event.getPlayer().getDisplayName())));
                    TextComponent itemInfo = new TextComponent("[");
                    itemInfo.setColor(ChatColor.AQUA);
                    if (meta.hasDisplayName()) {
                        for (BaseComponent component : TextComponent.fromLegacyText(meta.getDisplayName())) {
                            itemInfo.addExtra(component);
                        }
                    } else {
                        String key = MCVersion == 12 ? getTranslateKey(item) : getTranslateKey(item.getType().getKey().toString(), item.getType().isBlock());
                        itemInfo.addExtra(new TranslatableComponent(key));
                    }
                    itemInfo.addExtra("]");
                    itemInfo.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new BaseComponent[]{new TextComponent(nbt)}));
                    if (msg.equals(keyword)) {
                        if (MCVersion == 12) {
                            builder.append(new BaseComponent[]{itemInfo});
                        } else {
                            builder.append(itemInfo);
                        }
                    } else {
                        String left = getTextLeft(msg, keyword);
                        builder.append(TextComponent.fromLegacyText(left));
                        if (MCVersion == 12) {
                            builder.append(new BaseComponent[]{itemInfo});
                        } else {
                            builder.append(itemInfo);
                        }
                        String color = org.bukkit.ChatColor.getLastColors(left);
                        builder.append(TextComponent.fromLegacyText(color + getTextRight(msg, keyword)), MCVersion != 12 || fixedServer ? ComponentBuilder.FormatRetention.NONE : ComponentBuilder.FormatRetention.ALL);
                    }
                    BaseComponent[] messages = builder.create();
                    getServer().spigot().broadcast(builder.create());
                    getServer().getConsoleSender().spigot().sendMessage(messages);
                }
            }
        }
    }

    private String getTranslateKey(ItemStack item) {
        try {
            return NMS_ItemStack_a.invoke(OBC_CraftItemStack_asNMSCopy.invoke(OBC_CraftItemStack, item)) + ".name";
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "tile.air.name";
    }

    private static String getTranslateKey(String id, boolean isBlock) {
        return (isBlock ? "block." : "item.") + id.replace(':', '.');
    }

    private static String getTextRight(String str, String subStr) {
        int index = str.indexOf(subStr);
        return index == -1 ? "" : str.substring(index + subStr.length());
    }

    private static String getTextLeft(String str, String subStr) {
        int index = str.indexOf(subStr);
        return index < 1 ? "" : str.substring(0, index);
    }
}
