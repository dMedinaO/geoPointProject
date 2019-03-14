package cl.esanhueza.map_david.models;

import org.json.JSONException;
import org.json.JSONObject;

public class Item {
    String label;
    int value = -9999;

    public void setValue(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public Item(String label) {
        this.label = label;
    }
    public Item(String label, int value) {
        this.label = label;
        this.value= value;
    }

    public Item(JSONObject jsonObject) throws JSONException {
        this.label = jsonObject.getString("text");
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return "Item("+ this.label +")";
    }
}
