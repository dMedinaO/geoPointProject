package cl.esanhueza.map_david.models;

public class Choice {
    String value;
    String label;

    public Choice(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return "CHOICE("+ this.label +", "+ this.value +")";
    }
}
