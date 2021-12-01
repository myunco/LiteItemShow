package ml.mcos.liteitemshow;

import ml.mcos.liteitemshow.metrics.Metrics;
import ml.mcos.liteitemshow.nbt.NMS;
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

import java.util.List;

public class LiteItemShow extends JavaPlugin implements Listener {
    public String keyword;
    boolean fixedServer;
    int mcVersion;
    NMS nms;

    @Override
    public void onEnable() {
        initConfig();
        try {
            mcVersion = Integer.parseInt(getServer().getBukkitVersion().replace('-', '.').split("\\.")[1]);
            getLogger().info("minecraft version: 1." + mcVersion);
            fixedServer = getServer().getName().equals("CatServer") || getServer().getName().equals("Paper");
            nms = new NMS(this);
            getServer().getPluginManager().registerEvents(this, this);
            new Metrics(this, 13325);
        } catch (Exception e) {
            e.printStackTrace();
            getLogger().severe("插件加载出错：不受支持的服务端版本");
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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void asyncPlayerChatEvent(AsyncPlayerChatEvent event) {
        String msg = event.getMessage();
        if (msg.contains(keyword) && event.getPlayer().hasPermission("LiteItemShow.show")) {
            ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
            if (item.getType() != Material.AIR) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    event.setCancelled(true);
                    String nbt = nms.getItemNBT(item);
                    ComponentBuilder builder = new ComponentBuilder("");
                    builder.append(TextComponent.fromLegacyText(getTextLeft(event.getFormat(), "%2$s").replaceAll("%1\\$s", event.getPlayer().getDisplayName())));
                    TextComponent itemInfo = new TextComponent("[");
                    itemInfo.setColor(ChatColor.AQUA);
                    if (meta.hasDisplayName()) {
                        for (BaseComponent component : TextComponent.fromLegacyText(meta.getDisplayName())) {
                            itemInfo.addExtra(component);
                        }
                    } else {
                        String key = mcVersion == 12 ? nms.getTranslateKey(item) : getTranslateKey(item.getType().getKey().toString(), item.getType().isBlock());
                        itemInfo.addExtra(new TranslatableComponent(key));
                    }
                    itemInfo.addExtra("]");
                    itemInfo.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new BaseComponent[]{new TextComponent(nbt)}));
                    if (msg.equals(keyword)) {
                        if (mcVersion == 12) {
                            builder.append(new BaseComponent[]{itemInfo});
                        } else {
                            builder.append(itemInfo);
                        }
                    } else {
                        String left = getTextLeft(msg, keyword);
                        builder.append(TextComponent.fromLegacyText(left));
                        if (mcVersion == 12) {
                            builder.append(new BaseComponent[]{itemInfo});
                        } else {
                            builder.append(itemInfo);
                        }
                        String color = org.bukkit.ChatColor.getLastColors(left);
                        builder.append(TextComponent.fromLegacyText(color + getTextRight(msg, keyword)), mcVersion != 12 || fixedServer ? ComponentBuilder.FormatRetention.NONE : ComponentBuilder.FormatRetention.ALL);
                    }
                    BaseComponent[] messages = builder.create();
                    getServer().spigot().broadcast(builder.create());
                    getServer().getConsoleSender().spigot().sendMessage(messages);
                }
            }
        }
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
