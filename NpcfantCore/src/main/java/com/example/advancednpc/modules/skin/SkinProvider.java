package com.example.advancednpc.modules.skin;

import java.util.Optional;

public interface SkinProvider {


    String getType();


    Optional<SkinData> fetchSkin(String input);
}
