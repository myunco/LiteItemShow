package ml.mcos.liteitemshow.nbt;

import ml.mcos.liteitemshow.LiteItemShow;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;

public class NMS {
    Class<?> OBC_CraftItemStack;
    Class<?> NMS_NBTTagCompound;
    Class<?> NMS_ItemStack;
    Method OBC_CraftItemStack_asNMSCopy;
    Method NMS_ItemStack_save;
    Method NMS_ItemStack_a;
    LiteItemShow plugin;
    int mcVersion;

    public NMS(LiteItemShow plugin) throws Exception {
        this.plugin = plugin;
        String OBCPackage = plugin.getServer().getClass().getPackage().getName();
        String version = OBCPackage.substring(23);
        this.mcVersion = Integer.parseInt(version.split("_")[1]);
        OBC_CraftItemStack = Class.forName(OBCPackage + ".inventory.CraftItemStack");
        OBC_CraftItemStack_asNMSCopy = OBC_CraftItemStack.getMethod("asNMSCopy", ItemStack.class);
        if (mcVersion < 17) {
            String NMSPackage = "net.minecraft.server." + version;
            NMS_NBTTagCompound = Class.forName(NMSPackage + ".NBTTagCompound");
            NMS_ItemStack = Class.forName(NMSPackage + ".ItemStack");
            if (mcVersion == 12) {
                NMS_ItemStack_a = NMS_ItemStack.getMethod("a");
            }} else {
            NMS_NBTTagCompound = Class.forName("net.minecraft.nbt.NBTTagCompound");
            NMS_ItemStack = Class.forName("net.minecraft.world.item.ItemStack");
        }
        if (mcVersion == 18) {
            NMS_ItemStack_save = NMS_ItemStack.getMethod("b", NMS_NBTTagCompound);
        } else {
            NMS_ItemStack_save = NMS_ItemStack.getMethod("save", NMS_NBTTagCompound);
        }
    }

    public String getItemNBT(ItemStack item) {
        try {
            return NMS_ItemStack_save.invoke(OBC_CraftItemStack_asNMSCopy.invoke(OBC_CraftItemStack, item), NMS_NBTTagCompound.newInstance()).toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "{id:\"minecraft:air\"}";
    }

    public String getTranslateKey(ItemStack item) {
        try {
            return NMS_ItemStack_a.invoke(OBC_CraftItemStack_asNMSCopy.invoke(OBC_CraftItemStack, item)) + ".name";
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "tile.air.name";
    }

}
