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

                    // 1.1.1版本更新：
                    // 修复1.1.0中 展示物品时聊天颜色会覆盖名字颜色的问题
                    // 修复展示的物品名字受消息中的颜色格式影响的问题(如加粗、斜体、下划线等)

                    // %1$s = 玩家名  %2$s = 聊天消息内容
                    //System.out.println("event.getFormat() = " + event.getFormat());
                    // event.getFormat() = [ 称2号 ] [world]测试称号<%1$s> %2$s

                    // 将替换了名字但没有聊天内容的消息添加到builder  结果例如：[ 称2号 ] [world]测试称号<玩家名>
                    builder.append(TextComponent.fromLegacyText(getTextLeft(event.getFormat(), "%2$s").replace("%1$s", event.getPlayer().getDisplayName())));

                    // 获取关键词左边的消息
                    String left = getTextLeft(msg, keyword);

                    // 获取字符串中最后的颜色 以便后面用来还原关键词右边消息的颜色
                    String color = org.bukkit.ChatColor.getLastColors(left);
                    //System.out.println("color = " + color.replace('§', '&'));

                    TextComponent itemInfo = new TextComponent("[");

                    // 如果物品有显示名 那就直接用显示名称
                    if (meta.hasDisplayName()) {
                        for (BaseComponent component : TextComponent.fromLegacyText(meta.getDisplayName())) {
                            itemInfo.addExtra(component);
                        }
                    } else { // 没有显示名称 只能尝试获取翻译名称显示
                        //fixed a bug here - Bluemangoo
                        /*
                        bug occurs as the server has installed the "PlayerTitle" plugin or similar plugins
                        when the player's title end with bolded character, the whole line will be bolded(if it contains keyword)
                        fixed by resetting the format
                         */

                        // 修复1.12版本无法使用的问题 - myunco
                        if (mcVersion > 12) {
                            Material[] uncommonList = {
                                    Material.DRAGON_BREATH,
                                    Material.EXPERIENCE_BOTTLE,
                                    Material.ELYTRA,
                                    Material.ENCHANTED_BOOK,
                                    Material.CREEPER_HEAD,
                                    Material.PLAYER_HEAD,
                                    Material.DRAGON_HEAD,
                                    Material.PISTON_HEAD,
                                    Material.ZOMBIE_HEAD,
                                    Material.HEART_OF_THE_SEA,
                                    Material.NETHER_STAR,
                                    Material.TOTEM_OF_UNDYING
                            };
                            Material[] rareList = {
                                    Material.BEACON,
                                    Material.CONDUIT,
                                    Material.END_CRYSTAL,
                                    Material.GOLDEN_APPLE
                            };
                            Material[] epicList = {
                                    Material.COMMAND_BLOCK,
                                    Material.CHAIN_COMMAND_BLOCK,
                                    Material.REPEATING_COMMAND_BLOCK,
                                    Material.COMMAND_BLOCK_MINECART,
                                    Material.DRAGON_EGG,
                                    Material.ENCHANTED_GOLDEN_APPLE,
                                    Material.KNOWLEDGE_BOOK,
                                    Material.STRUCTURE_BLOCK,
                                    Material.STRUCTURE_VOID
                            };
                            boolean flag = true;
                            for (Material material : uncommonList) {
                                if (item.getType().equals(material)) {
                                    itemInfo.setColor(ChatColor.YELLOW);
                                    flag = false;
                                    break;
                                }
                            }
                            if (flag) {
                                if (item.getType().isRecord()) {
                                    itemInfo.setColor(ChatColor.AQUA);
                                    flag = false;
                                } else {
                                    for (Material material : rareList) {
                                        if (item.getType().equals(material)) {
                                            itemInfo.setColor(ChatColor.AQUA);
                                            flag = false;
                                            break;
                                        }
                                    }
                                }
                            }
                            if (flag) {
                                for (Material material : epicList) {
                                    if (item.getType().equals(material)) {
                                        flag = false;
                                        itemInfo.setColor(ChatColor.LIGHT_PURPLE);
                                        break;
                                    }
                                }
                            }
                            if (flag) {
                                if (nbt.contains("Enchantments:[")) {
                                    itemInfo.setColor(ChatColor.AQUA);
                                } else {
                                    itemInfo.setColor(ChatColor.WHITE);
                                }
                            }
                        } else {
                            if (item.getType().isRecord() || nbt.contains("ench:[")) {
                                itemInfo.setColor(ChatColor.AQUA);
                            } else {
                                itemInfo.setColor(ChatColor.WHITE);
                            }
                        }

                        //new feature here - Bluemangoo
                        /*
                        set the color of name like what you see on your hand :)
                        correspondence between items and colors come from Minecraft Wiki
                        wiki: https://minecraft.fandom.com/wiki/Rarity
                         */

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
                        builder.append(TextComponent.fromLegacyText(left + "§r"));
                        // 尾部 + "§r"的方式在低版本有效但在高版本失效 所以再用下面的方法重置一下格式
                        builder.bold(false)
                                .italic(false)
                                .underlined(false)
                                .strikethrough(false)
                                .obfuscated(false);
                        if (mcVersion == 12) {
                            builder.append(new BaseComponent[]{itemInfo});
                        } else {
                            builder.append(itemInfo);
                        }
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
