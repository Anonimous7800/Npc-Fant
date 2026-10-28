package com.example.advancednpc.modules.skin;

public class SkinData {

    private final String value;
    private final String signature;

    public SkinData(String value, String signature) {
        this.value = value;
        this.signature = signature;
    }

    public String getValue()     { return value; }
    public String getSignature() { return signature; }

    @Override
    public String toString() {
        return "SkinData{value=" + value.substring(0, Math.min(20, value.length())) + "...}";
    }
}
