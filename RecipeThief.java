package net.chase;

import java.lang.reflect.Field;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import net.minecraft.src.game.item.Item;
import net.minecraft.src.game.item.ItemStack;
import net.minecraft.src.game.recipe.CraftingManager;
import net.minecraft.src.game.recipe.IRecipe;
import net.minecraft.src.game.recipe.ShapedRecipes;
import net.minecraft.src.game.recipe.ShapelessRecipes;
import net.minecraft.src.game.recipe.FurnaceRecipes;
import net.minecraft.src.game.recipe.BlastFurnaceRecipes;
import net.minecraft.src.game.recipe.RefridgifreezerRecipes;

import net.minecraft.src.game.item.ItemBlockSlab;
import net.minecraft.src.game.block.BlockStairs;

import net.minecraft.src.client.gui.StringTranslate;

import net.minecraft.client.Minecraft;

import net.minecraft.src.client.renderer.RenderEngine;
import net.minecraft.src.client.renderer.block.TextureMap;
import net.minecraft.src.game.item.ItemBlock;
import net.minecraft.src.client.renderer.block.icon.IconRegister;
import net.minecraft.src.client.renderer.block.icon.Icon;
import net.minecraft.src.game.block.Block;
import net.minecraft.src.client.renderer.block.TextureStitched;
import net.minecraft.src.client.renderer.block.Texture;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

import java.io.FileWriter;
import java.io.BufferedWriter;

class IconThief implements IconRegister {
  static int dmg;
  static String output;

  public Icon registerIcon(String paramString) {
    if (dmg >= 0) {
      dmg--;
      IconThief.output = paramString;
    }
    return null;
  }

  public IconThief() {}
}

public class RecipeThief {
  static boolean[][] refed;
  static List slab_ids = new ArrayList(0);
  static List stair_ids = new ArrayList(0);

  static BufferedWriter writer;
  static RenderEngine re;

  static Object steal(Object obj, String name) throws Exception {
    Field field = obj.getClass().getDeclaredField(name);
    field.setAccessible(true);
    return field.get(obj);
  }

  // updates refed array as a side effect
  static String encodeStack(ItemStack stack) throws Exception {
    System.out.print(stack);
    if (stack == null) return "0";

    // side effect time
    Item item = Item.itemsList[stack.itemID];
    if (stack.itemDamage == -1) {
      // mark all valid damage levels
      int n = item.getMaxDamage();
      for (int i = 0; (i < n && n < 16) || i == 0; i++) {
        refed[stack.itemID][i] = true;
      }
    }
    else {
      refed[stack.itemID][stack.itemDamage] = true;
    }

    if (item instanceof ItemBlockSlab) {
    }
    if (item instanceof ItemBlockSlab && !(boolean)steal((ItemBlockSlab)item, "isFullBlock")) {
      if (!slab_ids.contains(stack.itemID)) slab_ids.add(stack.itemID);
    }
    if (item instanceof ItemBlock && Block.blocksList[((ItemBlock)item).blockID] instanceof BlockStairs) {
      if (!stair_ids.contains(stack.itemID)) stair_ids.add(stack.itemID);
    }

    return "[" + stack.itemID + "," + stack.stackSize + "," + stack.itemDamage + "]";
  }

  static String getDisplayName(int id, int dmg) {
    String trans = new ItemStack(id, 1, dmg).getDisplayName();
    if (trans.length() == 0) {
      trans = Item.itemsList[id].getItemName();
    }
    trans = trans.replace("\\", "\\\\").replace("'", "\\'");
    return trans;
  }

  static String getIconSrc(int id, int dmg) throws Exception {
    String path = "../../textures/";
    Item item = Item.itemsList[id];

    IconThief.dmg = dmg;
    IconThief.output = null;
    item.registerIcons(new IconThief());
    if (IconThief.output != null) {
      path += "items/" + IconThief.output + ".png";
    }
    else {
      Icon blockItemIcon = Block.blocksList[((ItemBlock) item).blockID].getIcon(3, dmg);
      path += "blocks/" + blockItemIcon.getIconName() + ".png";
    }
    byte[] bytes = Files.readAllBytes(Paths.get(path));
    return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
  }

  static void printDictionary(String type) throws Exception {
    writer.write("const " + type + " = {\n");
    for (int id = 0; id < refed.length; id++) {
      Item item = Item.itemsList[id];
      if (item == null) continue;
      boolean found = false;
      for (int dmg = 0; dmg < 16; dmg++) {
        if (!refed[id][dmg]) continue;
        if (found) {
          writer.write(", ");
        }
        else {
          found = true;
          writer.write("  " + id + ": {");
        }

        writer.write(dmg + ": '");
        if (type == "names")
          writer.write(getDisplayName(id, dmg));
        else
          writer.write(getIconSrc(id, dmg));
        writer.write("'");
      }
      if (!found) continue;
      writer.write("},\n");
    }
    writer.write("};\n");
  }

  static void printList(String name, List<Integer> value) throws Exception {
    writer.write("const " + name + " = [");
    boolean comma = false;
    for (Integer id : value) {
      if (comma) writer.write(", ");
      else comma = true;
      writer.write(id.toString());
    }
    writer.write("];\n\n");
  }

  static void printSmelting(Object obj, String name) throws Exception {
    writer.write("const " + name + " = [\n");
    Map<Integer, ItemStack> smelt = (Map<Integer, ItemStack>)steal(obj, "smeltingList");
    for (Map.Entry<Integer, ItemStack> entry : smelt.entrySet()) {
      int id = entry.getKey() & 0xFFF;
      int dmg = 0;
      if (entry.getKey() > 1073741824) {
        dmg = (entry.getKey() - 1073741824) >> 13;
      }
      ItemStack input = new ItemStack(id, 1, dmg);
      ItemStack output = entry.getValue();
      writer.write("  [" + encodeStack(output) + ", " + encodeStack(input) + "],\n");
    }
    writer.write("];\n\n");
  }

  public static void main(String[] args) throws Exception {
    // actually run minecraft to initialize some shtuff
    Minecraft.startMainThread("", "", "");

    // there is a race condition and we to lose
    Thread.sleep(4000);

    // open output file
    writer = new BufferedWriter(new FileWriter("output.js"));

    // language example
    // StringTranslate.langFile = "ru_RU";
    // StringTranslate.reloadKeys();
    refed = new boolean[Item.itemsList.length][16];
    List<IRecipe> recipes = CraftingManager.getInstance().getRecipeList();

    // construct js array of shaped recipes
    writer.write("const shaped_recipes = [\n");
    for (IRecipe rec : recipes) {
      if (false == rec instanceof ShapedRecipes) continue;
      writer.write("  [");
      // output stack
      ItemStack output = rec.getRecipeOutput();
      writer.write(encodeStack(output) + ", ");

      // matrix of ingredients
      writer.write("[");
      boolean i = false;
      ItemStack[] matrix = (ItemStack[])steal(rec, "ingredients");
      for (ItemStack stack : matrix) {
        if (i) writer.write(",");
        else i = true;
        writer.write(encodeStack(stack));
      }
      writer.write("], ");

      // width of matrix
      int width = (int)steal(rec, "width");
      writer.write(String.valueOf(width));
      writer.write("],\n");
    }
    writer.write("];\n\n");

    // construct js array of shapeless recipes
    writer.write("const shapeless_recipes = [\n");
    for (IRecipe rec : recipes) {
      if (false == rec instanceof ShapelessRecipes) continue;
      writer.write("  [");
      // output stack
      ItemStack output = rec.getRecipeOutput();
      writer.write(encodeStack(output) + ", ");

      // list of ingredients
      writer.write("[");
      boolean i = false;
      List<ItemStack> list = (List<ItemStack>)steal(rec, "ingredients");
      for (ItemStack stack : list) {
        if (i) writer.write(",");
        else i = true;
        writer.write(encodeStack(stack));
      }
      writer.write("]");
      writer.write("],\n");
    }
    writer.write("];\n\n");

    // furnace recipes
    printSmelting(FurnaceRecipes.instance, "furnace_recipes");
    printSmelting(BlastFurnaceRecipes.instance, "forge_recipes");
    printSmelting(RefridgifreezerRecipes.instance, "refridgifreezer_recipes");

    // print js list of slabs and stairs and ItemBlockColored
    printList("slab_ids", slab_ids);
    printList("stair_ids", stair_ids);

    // construct js dictionary of dictionaries of names
    printDictionary("names");
    writer.write("\n");

    // construct js dictionary of dictionaries of icons
    // this initializes the block textures without actually loading a world
    re = Minecraft.getInstance().renderEngine;
    TextureMap textureMapBlocks = (TextureMap)steal(re, "textureMapBlocks");
    for (Block block : Block.blocksList) {
      if (block != null)
        block.registerIcons(textureMapBlocks);
    }
    printDictionary("icons");

    writer.close();
    System.exit(0);
  }
}
