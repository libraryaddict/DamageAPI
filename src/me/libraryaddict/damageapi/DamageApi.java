package me.libraryaddict.damageapi;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class DamageApi {

    private static HashMap<Material, Integer> armorRatings = new HashMap<Material, Integer>();
    private static Method asNms;

    static {
        Material[] armorMaterials = new Material[] {

        Material.LEATHER_BOOTS, Material.LEATHER_LEGGINGS, Material.LEATHER_CHESTPLATE, Material.LEATHER_HELMET,

        Material.GOLD_BOOTS, Material.GOLD_LEGGINGS, Material.GOLD_CHESTPLATE, Material.GOLD_HELMET,

        Material.CHAINMAIL_BOOTS, Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_HELMET,

        Material.IRON_BOOTS, Material.IRON_LEGGINGS, Material.IRON_CHESTPLATE, Material.IRON_HELMET,

        Material.DIAMOND_BOOTS, Material.DIAMOND_LEGGINGS, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_HELMET };
        try {
            asNms = Class.forName(
                    "org.bukkit.craftbukkit." + Bukkit.getServer().getClass().getName().split("\\.")[3]
                            + ".inventory.CraftItemStack").getMethod("asNMSCopy", ItemStack.class);
            Object armor = getNmsArmor(Material.LEATHER_HELMET);
            Field armorField = null;
            for (Field field : armor.getClass().getFields()) {
                if (field.getType() == int.class && Modifier.isFinal(field.getModifiers())) {
                    if (field.getInt(armor) != 0) {
                        if (armorField != null) {
                            Thread.dumpStack();
                        }
                        armorField = field;
                    }
                }
            }
            for (Material material : armorMaterials) {
                armor = getNmsArmor(material);
                armorRatings.put(material, armorField.getInt(armor));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static double addArmor(ItemStack[] armor, double damage) {
        if (damage <= 0)
            return 0;
        int armorValues = 25 - getArmorRating(armor);
        double damageToDeal = damage * armorValues;
        // Here it then damages the armor. But I removed that
        damage = damageToDeal / 25F;
        return damage;
    }

    private static double addEnchants(ItemStack[] armor, double damage, DamageCause damageCause) {
        int i = getEnchantRating(armor, damageCause);
        if (i > 20) {
            i = 20;
        }
        if (i > 0 && i <= 20) {
            int j = 25 - i;
            double f1 = damage * j;
            damage = f1 / 25.0F;
        }
        return damage;
    }

    public static double calculateDamageAddArmor(ItemStack[] armor, DamageCause damageCause, double damage) {
        return calculateDamageAddArmor(armor, damageCause, damage, false, null);
    }

    public static double calculateDamageAddArmor(ItemStack[] armor, DamageCause damageCause, double damage, boolean blocking) {
        return calculateDamageAddArmor(armor, damageCause, damage, blocking, null);
    }

    public static double calculateDamageAddArmor(ItemStack[] armor, DamageCause damageCause, double damage, boolean blocking,
            PotionEffect resistancePotionEffect) {
        if (blocking) {
            damage = (1 + damage) / 2;
        }
        if (!ignoresArmor(damageCause)) {
            damage = addArmor(armor, damage);
        }
        if (resistancePotionEffect != null && damageCause != DamageCause.VOID) {
            int i = (resistancePotionEffect.getAmplifier() + 1) * 5;
            int j = 25 - i;
            double f1 = damage * j;
            damage = f1 / 25;
        }
        damage = addEnchants(armor, damage, damageCause);
        return damage;
    }

    public static double calculateDamageAddArmor(Player player, DamageCause damageCause, double damage) {
        PotionEffect effect = null;
        for (PotionEffect e : player.getActivePotionEffects()) {
            if (e.getType() == PotionEffectType.DAMAGE_RESISTANCE) {
                effect = e;
                break;
            }
        }
        return calculateDamageAddArmor(player.getInventory().getArmorContents(), damageCause, damage, player.isBlocking(), effect);
    }

    /**
     * Remember. Damage events feed the RAW damage. Not the armor damage!
     */
    public static double calculateDamageRemoveArmor(ItemStack[] armor, DamageCause damageCause, double damage) {
        return calculateDamageRemoveArmor(armor, damageCause, damage, false, null);
    }

    /**
     * Remember. Damage events feed the RAW damage. Not the armor damage!
     */
    public static double calculateDamageRemoveArmor(ItemStack[] armor, DamageCause damageCause, double damage, boolean blocking) {
        return calculateDamageRemoveArmor(armor, damageCause, damage, blocking, null);
    }

    /**
     * Remember. Damage events feed the RAW damage. Not the armor damage!
     */
    public static double calculateDamageRemoveArmor(ItemStack[] armor, DamageCause damageCause, double damage, boolean blocking,
            PotionEffect resistancePotionEffect) {
        if (blocking) {
            damage = (damage * 2) - 1;
        }
        if (!ignoresArmor(damageCause)) {
            damage = removeArmor(armor, damage);
        }
        if (resistancePotionEffect != null && damageCause != DamageCause.VOID) {
            int i = (resistancePotionEffect.getAmplifier() + 1) * 5;
            int j = 25 - i;
            double f1 = damage / j;
            damage = f1 * 25.0F;
        }
        damage = removeEnchants(armor, damage, damageCause);
        return damage;
    }

    public static double calculateDamageRemoveArmor(Player player, DamageCause damageCause, double damage) {
        PotionEffect effect = null;
        for (PotionEffect potionEffect : player.getActivePotionEffects()) {
            if (potionEffect.getType() == PotionEffectType.DAMAGE_RESISTANCE) {
                effect = potionEffect;
                break;
            }
        }
        return calculateDamageRemoveArmor(player.getInventory().getArmorContents(), damageCause, damage, player.isBlocking(),
                effect);
    }

    public static int getArmorRating(ItemStack item) {
        if (item == null)
            return 0;
        return getArmorRating(item.getType());
    }

    public static int getArmorRating(ItemStack[] items) {
        int armorRating = 0;
        for (ItemStack item : items) {
            armorRating += getArmorRating(item);
        }
        return armorRating;
    }

    public static int getArmorRating(Material material) {
        if (armorRatings.containsKey(material))
            return armorRatings.get(material);
        return 0;
    }

    public static int getArmorRating(Player player) {
        return getArmorRating(player.getInventory().getArmorContents());
    }

    private static int getEnchantRating(ItemStack[] items, DamageCause damageCause) {
        int rating = 0;
        for (ItemStack item : items) {
            if (item == null) {
                continue;
            }
            Map<Enchantment, Integer> enchants = item.getEnchantments();
            for (Enchantment enchant : enchants.keySet()) {
                if (ignoresInvulnerability(damageCause)) {
                    continue;
                }
                double enchantLevel = (6 + enchants.get(enchant) * enchants.get(enchant)) / 3;
                if (enchant.equals(Enchantment.PROTECTION_ENVIRONMENTAL)) {
                    rating += (int) Math.floor(enchantLevel * 0.75F);
                } else if (enchant.equals(Enchantment.PROTECTION_FIRE)) {
                    if (isFireDamage(damageCause)) {
                        rating += (int) Math.floor(enchantLevel * 1.25F);
                    }
                } else if (enchant.equals(Enchantment.PROTECTION_FALL)) {
                    if (damageCause == DamageCause.FALL) {
                        rating += (int) Math.floor(enchantLevel * 2.5F);
                    }
                } else if (enchant.equals(Enchantment.PROTECTION_EXPLOSIONS)) {
                    if (isExplosionDamage(damageCause)) {
                        rating += (int) Math.floor(enchantLevel * 1.5F);
                    }
                } else if (enchant.equals(Enchantment.PROTECTION_PROJECTILE)) {
                    if (isProjectileDamage(damageCause)) {
                        rating += (int) Math.floor(enchantLevel * 1.5F);
                    }
                }
            }
        }

        if (rating > 25) {
            rating = 25;
        }
        return (rating + 1 >> 1) + new Random().nextInt((rating >> 1) + 1);
    }

    private static Object getNmsArmor(Material mat) throws Exception {
        Object itemstack = asNms.invoke(null, new ItemStack(mat));
        return itemstack.getClass().getMethod("getItem").invoke(itemstack);
    }

    private static boolean ignoresArmor(DamageCause damageCause) {
        switch (damageCause) {
        case CUSTOM:
        case DROWNING:
        case FALL:
        case FIRE_TICK:
        case MAGIC:
        case STARVATION:
        case SUFFOCATION:
        case SUICIDE:
        case WITHER:
        case VOID:
            return true;
        default:
            return false;
        }
    }

    private static boolean ignoresInvulnerability(DamageCause damageCause) {
        switch (damageCause) {
        case VOID:
        case SUICIDE:
            return true;
        default:
            return false;
        }
    }

    private static boolean isExplosionDamage(DamageCause damageCause) {
        switch (damageCause) {
        case BLOCK_EXPLOSION:
        case ENTITY_EXPLOSION:
            return true;
        default:
            return false;
        }
    }

    private static boolean isFireDamage(DamageCause damageCause) {
        switch (damageCause) {
        case FIRE:
        case FIRE_TICK:
        case LAVA:
            return true;
        default:
            return false;
        }
    }

    private static boolean isProjectileDamage(DamageCause damageCause) {
        switch (damageCause) {
        case PROJECTILE:
        case FIRE_TICK:
            return true;
        default:
            return false;
        }
    }

    private static double removeArmor(ItemStack[] armor, double damage) {
        if (damage <= 0) {
            return 0;
        }
        double armorValues = 25 - getArmorRating(armor);
        double damageToDeal = damage / armorValues;
        damage = damageToDeal * 25F;
        return damage;
    }

    private static double removeEnchants(ItemStack[] armor, double damage, DamageCause damageCause) {
        int armorRating = getEnchantRating(armor, damageCause);
        if (armorRating > 20) {
            armorRating = 20;
        }
        if (armorRating > 0) {
            int j = 25 - armorRating;
            double f1 = damage / j;
            damage = f1 * 25.0F;
        }
        return damage;
    }
}
