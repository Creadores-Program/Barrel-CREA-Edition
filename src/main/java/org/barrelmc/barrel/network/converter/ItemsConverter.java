package org.barrelmc.barrel.network.converter;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.barrelmc.barrel.utils.FileManager;

import java.util.HashMap;
import java.util.Map;

public class ItemsConverter {
  public static final HashMap<String, String> ItemsJEtoBE = new HashMap<>();
  public static final HashMap<String, String> ItemsBEtoJE = new HashMap<>();
  public static void init() {
    JsonObject jsonObject = FileManager.getJsonObjectFromResource("runtime_items.json");

    assert jsonObject != null;

    for(Map.Entry<String, JsonElement> entry : jsonObject.entrySet()){
      String JavaID = entry.getKey();
      JsonObject itemEntry = entry.getValue().getAsJsonObject();
      ItemsJEtoBE.put(JavaID, itemEntry.get("name").getAsString() + "::"+itemEntry.get("damage").getAsString());
      ItemsBEtoJE.put(itemEntry.get("name").getAsString() + "::"+itemEntry.get("damage").getAsString(), JavaID);
    }
  }
  public static String BedrockToJava(String IDName){
    return ItemsBEtoJE.getOrDefault(IDName, "minecraft:air");
  }
  public static String JavaToBedrock(String IDName){
    return ItemsJEtoBE.getOrDefault(IDName, "minecraft:air::0");
  }
}
