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
                    builder.append(TextComponent.fromLegacyText(getTextLeft(event.getFormat(), "%2$s").replaceAll("%1\\$s", "").replaceAll("<> ","")));
                    String left = getTextLeft(msg, keyword);
                    String color = org.bukkit.ChatColor.getLastColors(left);
                    builder.append(TextComponent.fromLegacyText(color +"<"+event.getPlayer().getDisplayName()+"> "), mcVersion != 12 || fixedServer ? ComponentBuilder.FormatRetention.NONE : ComponentBuilder.FormatRetention.ALL);

                    //fixed a bug here - Bluemangoo
                    /*
                    bug occurs as the server has installed the "PlayerTitle" plugin or similar plugins
                    when the player's title end with bolded character, the whole line will be bolded(if it contains keyword)
                    fixed by resetting the format
                     */

                    //TODO:bug here:half more space
                    /*
                    here's a half more space before the player's name
                    for example:
                     - when the plugin is unloaded
                           <bluemangoo> look at my [item]
                     - when the plugin is loaded
                           <bluemangoo>  look at my [Best Sword]
                     see it?
                     what's strange, there is not even a more entire space
                     I mean, the width does not reach an entire space
                     maybe I see it wrongly :)
                     fixed a bug and another appears 💦💦💦
                     */

                    TextComponent itemInfo = new TextComponent("[");
                    boolean flag=false;
                    boolean uncommonFlag=false;
                    boolean rareFlag=false;
                    boolean epicFlag=false;
                    Material[] uncommonList={
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
                    Material[] rareList={
                            Material.BEACON,
                            Material.CONDUIT,
                            Material.END_CRYSTAL,
                            Material.GOLDEN_APPLE,
                    };
                    Material[] epicList={
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
                    for(Material material:uncommonList){
                        if(item.getType().equals(material)){
                            uncommonFlag=true;
                            break;
                        }
                    }
                    if(item.getType().isRecord()){
                        rareFlag=true;
                    }else{
                        for(Material material:rareList){
                            if(item.getType().equals(material)){
                                rareFlag=true;
                                break;
                            }
                        }
                    }for(Material material:epicList){
                        if(item.getType().equals(material)){
                            epicFlag=true;
                            break;
                        }
                    }
                    if(uncommonFlag){
                        itemInfo.setColor(ChatColor.YELLOW);
                        flag=true;
                    }
                    if(rareFlag){
                        itemInfo.setColor(ChatColor.AQUA);
                        flag=true;
                    }
                    if(epicFlag){
                        itemInfo.setColor(ChatColor.DARK_PURPLE);
                        flag=true;
                    }
                    if(nbt.contains("Enchantments:[")&!nbt.contains("Enchantments:[]")){
                        itemInfo.setColor(ChatColor.AQUA);
                        flag=true;
                    }
                    if(!flag){
                        itemInfo.setColor(ChatColor.WHITE);
                    }
                    //new feature here - Bluemangoo
                    /*
                    set the color of name like what you see on your hand :)
                    correspondence between items and colors come from Minecraft Wiki
                    wiki: https://minecraft.fandom.com/wiki/Rarity
                     */


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
                        builder.append(TextComponent.fromLegacyText(left));
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
