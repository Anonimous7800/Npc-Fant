package com.example.advancednpc.util;

import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import org.bukkit.Bukkit;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

public final class ProtocolPacketIdSync {

    private static final Map<String, PacketType.Play.Server> CLIENTBOUND_MAP = new HashMap<>();
    private static final Map<String, PacketType.Play.Client> SERVERBOUND_MAP = new HashMap<>();

    static {
        mapClient("bundle_delimiter", PacketType.Play.Server.BUNDLE);
        mapClient("bundle", PacketType.Play.Server.BUNDLE);
        mapClient("add_entity", PacketType.Play.Server.SPAWN_ENTITY);
        mapClient("animate", PacketType.Play.Server.ENTITY_ANIMATION);
        mapClient("swing_animation", PacketType.Play.Server.ENTITY_ANIMATION);
        mapClient("award_stats", PacketType.Play.Server.STATISTICS);
        mapClient("block_changed_ack", PacketType.Play.Server.ACKNOWLEDGE_BLOCK_CHANGES);
        mapClient("block_destruction", PacketType.Play.Server.BLOCK_BREAK_ANIMATION);
        mapClient("block_entity_data", PacketType.Play.Server.BLOCK_ENTITY_DATA);
        mapClient("block_event", PacketType.Play.Server.BLOCK_ACTION);
        mapClient("block_update", PacketType.Play.Server.BLOCK_CHANGE);
        mapClient("boss_event", PacketType.Play.Server.BOSS_BAR);
        mapClient("change_difficulty", PacketType.Play.Server.SERVER_DIFFICULTY);
        mapClient("chunk_batch_finished", PacketType.Play.Server.CHUNK_BATCH_END);
        mapClient("chunk_batch_start", PacketType.Play.Server.CHUNK_BATCH_BEGIN);
        mapClient("chunks_biomes", PacketType.Play.Server.CHUNK_BIOMES);
        mapClient("clear_titles", PacketType.Play.Server.CLEAR_TITLES);
        mapClient("command_suggestions", PacketType.Play.Server.TAB_COMPLETE);
        mapClient("commands", PacketType.Play.Server.DECLARE_COMMANDS);
        mapClient("container_close", PacketType.Play.Server.CLOSE_WINDOW);
        mapClient("container_set_content", PacketType.Play.Server.WINDOW_ITEMS);
        mapClient("container_set_data", PacketType.Play.Server.WINDOW_PROPERTY);
        mapClient("container_set_slot", PacketType.Play.Server.SET_SLOT);
        mapClient("cookie_request", PacketType.Play.Server.COOKIE_REQUEST);
        mapClient("cooldown", PacketType.Play.Server.SET_COOLDOWN);
        mapClient("custom_chat_completions", PacketType.Play.Server.CUSTOM_CHAT_COMPLETIONS);
        mapClient("custom_payload", PacketType.Play.Server.PLUGIN_MESSAGE);
        mapClient("damage_event", PacketType.Play.Server.DAMAGE_EVENT);
        mapClient("debug_block_value", PacketType.Play.Server.DEBUG_BLOCK_VALUE);
        mapClient("debug_chunk_value", PacketType.Play.Server.DEBUG_CHUNK_VALUE);
        mapClient("debug_entity_value", PacketType.Play.Server.DEBUG_ENTITY_VALUE);
        mapClient("debug_event", PacketType.Play.Server.DEBUG_EVENT);
        mapClient("debug_sample", PacketType.Play.Server.DEBUG_SAMPLE);
        mapClient("delete_chat", PacketType.Play.Server.DELETE_CHAT);
        mapClient("disconnect", PacketType.Play.Server.DISCONNECT);
        mapClient("disguised_chat", PacketType.Play.Server.DISGUISED_CHAT);
        mapClient("entity_event", PacketType.Play.Server.ENTITY_STATUS);
        mapClient("entity_position_sync", PacketType.Play.Server.ENTITY_POSITION_SYNC);
        mapClient("explode", PacketType.Play.Server.EXPLOSION);
        mapClient("forget_level_chunk", PacketType.Play.Server.UNLOAD_CHUNK);
        mapClient("game_event", PacketType.Play.Server.CHANGE_GAME_STATE);
        mapClient("game_rule_values", PacketType.Play.Server.GAME_RULE_VALUES);
        mapClient("game_test_highlight_pos", PacketType.Play.Server.GAME_TEST_HIGHLIGHT_POS);
        mapClient("horse_screen_open", PacketType.Play.Server.OPEN_HORSE_WINDOW);
        mapClient("mount_screen_open", PacketType.Play.Server.OPEN_HORSE_WINDOW);
        mapClient("hurt_animation", PacketType.Play.Server.HURT_ANIMATION);
        mapClient("initialize_border", PacketType.Play.Server.INITIALIZE_WORLD_BORDER);
        mapClient("keep_alive", PacketType.Play.Server.KEEP_ALIVE);
        mapClient("level_chunk_with_light", PacketType.Play.Server.CHUNK_DATA);
        mapClient("level_event", PacketType.Play.Server.EFFECT);
        mapClient("level_particles", PacketType.Play.Server.PARTICLE);
        mapClient("light_update", PacketType.Play.Server.UPDATE_LIGHT);
        mapClient("login", PacketType.Play.Server.JOIN_GAME);
        mapClient("low_disk_space_warning", PacketType.Play.Server.LOW_DISK_SPACE_WARNING);
        mapClient("map_item_data", PacketType.Play.Server.MAP_DATA);
        mapClient("merchant_offers", PacketType.Play.Server.MERCHANT_OFFERS);
        mapClient("move_entity_pos", PacketType.Play.Server.ENTITY_RELATIVE_MOVE);
        mapClient("move_entity_pos_rot", PacketType.Play.Server.ENTITY_RELATIVE_MOVE_AND_ROTATION);
        mapClient("move_minecart_along_track", PacketType.Play.Server.MOVE_MINECART);
        mapClient("move_entity_rot", PacketType.Play.Server.ENTITY_ROTATION);
        mapClient("move_vehicle", PacketType.Play.Server.VEHICLE_MOVE);
        mapClient("open_book", PacketType.Play.Server.OPEN_BOOK);
        mapClient("open_screen", PacketType.Play.Server.OPEN_WINDOW);
        mapClient("open_sign_editor", PacketType.Play.Server.OPEN_SIGN_EDITOR);
        mapClient("ping", PacketType.Play.Server.PING);
        mapClient("pong_response", PacketType.Play.Server.DEBUG_PONG);
        mapClient("ping_response", PacketType.Play.Server.DEBUG_PONG);
        mapClient("place_ghost_recipe", PacketType.Play.Server.CRAFT_RECIPE_RESPONSE);
        mapClient("player_abilities", PacketType.Play.Server.PLAYER_ABILITIES);
        mapClient("player_chat", PacketType.Play.Server.CHAT_MESSAGE);
        mapClient("player_combat_end", PacketType.Play.Server.END_COMBAT_EVENT);
        mapClient("player_combat_enter", PacketType.Play.Server.ENTER_COMBAT_EVENT);
        mapClient("player_combat_kill", PacketType.Play.Server.DEATH_COMBAT_EVENT);
        mapClient("player_info_remove", PacketType.Play.Server.PLAYER_INFO_REMOVE);
        mapClient("player_info_update", PacketType.Play.Server.PLAYER_INFO_UPDATE);
        mapClient("player_look_at", PacketType.Play.Server.FACE_PLAYER);
        mapClient("player_position", PacketType.Play.Server.PLAYER_POSITION_AND_LOOK);
        mapClient("player_rotation", PacketType.Play.Server.PLAYER_ROTATION);
        mapClient("recipe_book_add", PacketType.Play.Server.RECIPE_BOOK_ADD);
        mapClient("recipe_book_remove", PacketType.Play.Server.RECIPE_BOOK_REMOVE);
        mapClient("recipe_book_settings", PacketType.Play.Server.RECIPE_BOOK_SETTINGS);
        mapClient("remove_entities", PacketType.Play.Server.DESTROY_ENTITIES);
        mapClient("remove_mob_effect", PacketType.Play.Server.REMOVE_ENTITY_EFFECT);
        mapClient("reset_score", PacketType.Play.Server.RESET_SCORE);
        mapClient("resource_pack_pop", PacketType.Play.Server.RESOURCE_PACK_REMOVE);
        mapClient("resource_pack_push", PacketType.Play.Server.RESOURCE_PACK_SEND);
        mapClient("respawn", PacketType.Play.Server.RESPAWN);
        mapClient("rotate_head", PacketType.Play.Server.ENTITY_HEAD_LOOK);
        mapClient("section_blocks_update", PacketType.Play.Server.MULTI_BLOCK_CHANGE);
        mapClient("select_advancements_tab", PacketType.Play.Server.SELECT_ADVANCEMENTS_TAB);
        mapClient("server_data", PacketType.Play.Server.SERVER_DATA);
        mapClient("set_action_bar_text", PacketType.Play.Server.ACTION_BAR);
        mapClient("set_border_center", PacketType.Play.Server.WORLD_BORDER_CENTER);
        mapClient("set_border_lerp_size", PacketType.Play.Server.WORLD_BORDER_LERP_SIZE);
        mapClient("set_border_size", PacketType.Play.Server.WORLD_BORDER_SIZE);
        mapClient("set_border_warning_delay", PacketType.Play.Server.WORLD_BORDER_WARNING_DELAY);
        mapClient("set_border_warning_distance", PacketType.Play.Server.WORLD_BORDER_WARNING_REACH);
        mapClient("set_camera", PacketType.Play.Server.CAMERA);
        mapClient("set_chunk_cache_center", PacketType.Play.Server.UPDATE_VIEW_POSITION);
        mapClient("set_chunk_cache_radius", PacketType.Play.Server.UPDATE_VIEW_DISTANCE);
        mapClient("set_cursor_item", PacketType.Play.Server.SET_CURSOR_ITEM);
        mapClient("set_default_spawn_position", PacketType.Play.Server.SPAWN_POSITION);
        mapClient("set_display_objective", PacketType.Play.Server.DISPLAY_SCOREBOARD);
        mapClient("set_entity_data", PacketType.Play.Server.ENTITY_METADATA);
        mapClient("set_entity_link", PacketType.Play.Server.ATTACH_ENTITY);
        mapClient("set_entity_motion", PacketType.Play.Server.ENTITY_VELOCITY);
        mapClient("set_equipment", PacketType.Play.Server.ENTITY_EQUIPMENT);
        mapClient("set_experience", PacketType.Play.Server.SET_EXPERIENCE);
        mapClient("set_health", PacketType.Play.Server.UPDATE_HEALTH);
        mapClient("set_held_slot", PacketType.Play.Server.HELD_ITEM_CHANGE);
        mapClient("set_objective", PacketType.Play.Server.SCOREBOARD_OBJECTIVE);
        mapClient("set_passengers", PacketType.Play.Server.SET_PASSENGERS);
        mapClient("set_player_inventory", PacketType.Play.Server.SET_PLAYER_INVENTORY);
        mapClient("set_player_team", PacketType.Play.Server.TEAMS);
        mapClient("set_score", PacketType.Play.Server.UPDATE_SCORE);
        mapClient("set_simulation_distance", PacketType.Play.Server.UPDATE_SIMULATION_DISTANCE);
        mapClient("set_subtitle_text", PacketType.Play.Server.SET_TITLE_SUBTITLE);
        mapClient("set_time", PacketType.Play.Server.TIME_UPDATE);
        mapClient("set_title_text", PacketType.Play.Server.SET_TITLE_TEXT);
        mapClient("set_titles_animation", PacketType.Play.Server.SET_TITLE_TIMES);
        mapClient("sound_entity", PacketType.Play.Server.ENTITY_SOUND_EFFECT);
        mapClient("sound", PacketType.Play.Server.SOUND_EFFECT);
        mapClient("start_configuration", PacketType.Play.Server.CONFIGURATION_START);
        mapClient("stop_sound", PacketType.Play.Server.STOP_SOUND);
        mapClient("store_cookie", PacketType.Play.Server.STORE_COOKIE);
        mapClient("system_chat", PacketType.Play.Server.SYSTEM_CHAT_MESSAGE);
        mapClient("tab_list", PacketType.Play.Server.PLAYER_LIST_HEADER_AND_FOOTER);
        mapClient("tag_query", PacketType.Play.Server.NBT_QUERY_RESPONSE);
        mapClient("take_item_entity", PacketType.Play.Server.COLLECT_ITEM);
        mapClient("teleport_entity", PacketType.Play.Server.ENTITY_TELEPORT);
        mapClient("test_instance_block_status", PacketType.Play.Server.TEST_INSTANCE_BLOCK_STATUS);
        mapClient("ticking_state", PacketType.Play.Server.TICKING_STATE);
        mapClient("ticking_step", PacketType.Play.Server.TICKING_STEP);
        mapClient("transfer", PacketType.Play.Server.TRANSFER);
        mapClient("update_advancements", PacketType.Play.Server.UPDATE_ADVANCEMENTS);
        mapClient("update_attributes", PacketType.Play.Server.UPDATE_ATTRIBUTES);
        mapClient("update_mob_effect", PacketType.Play.Server.ENTITY_EFFECT);
        mapClient("update_recipes", PacketType.Play.Server.DECLARE_RECIPES);
        mapClient("update_tags", PacketType.Play.Server.TAGS);
        mapClient("projectile_power", PacketType.Play.Server.PROJECTILE_POWER);
        mapClient("custom_report_details", PacketType.Play.Server.CUSTOM_REPORT_DETAILS);
        mapClient("server_links", PacketType.Play.Server.SERVER_LINKS);
        mapClient("waypoint", PacketType.Play.Server.WAYPOINT);
        mapClient("clear_dialog", PacketType.Play.Server.CLEAR_DIALOG);
        mapClient("show_dialog", PacketType.Play.Server.SHOW_DIALOG);

        mapServer("accept_teleportation", PacketType.Play.Client.TELEPORT_CONFIRM);
        mapServer("attack", PacketType.Play.Client.ATTACK);
        mapServer("punch", PacketType.Play.Client.ANIMATION);
        mapServer("block_entity_tag_query", PacketType.Play.Client.QUERY_BLOCK_NBT);
        mapServer("bundle_item_selected", PacketType.Play.Client.SELECT_BUNDLE_ITEM);
        mapServer("change_difficulty", PacketType.Play.Client.SET_DIFFICULTY);
        mapServer("change_game_mode", PacketType.Play.Client.CHANGE_GAME_MODE);
        mapServer("chat_ack", PacketType.Play.Client.CHAT_ACK);
        mapServer("chat_command", PacketType.Play.Client.CHAT_COMMAND_UNSIGNED);
        mapServer("chat_command_signed", PacketType.Play.Client.CHAT_COMMAND);
        mapServer("chat", PacketType.Play.Client.CHAT_MESSAGE);
        mapServer("chat_session_update", PacketType.Play.Client.CHAT_SESSION_UPDATE);
        mapServer("chunk_batch_received", PacketType.Play.Client.CHUNK_BATCH_ACK);
        mapServer("client_command", PacketType.Play.Client.CLIENT_STATUS);
        mapServer("client_tick_end", PacketType.Play.Client.CLIENT_TICK_END);
        mapServer("client_information", PacketType.Play.Client.CLIENT_SETTINGS);
        mapServer("command_suggestion", PacketType.Play.Client.TAB_COMPLETE);
        mapServer("configuration_acknowledged", PacketType.Play.Client.CONFIGURATION_ACK);
        mapServer("container_button_click", PacketType.Play.Client.CLICK_WINDOW_BUTTON);
        mapServer("container_click", PacketType.Play.Client.CLICK_WINDOW);
        mapServer("container_close", PacketType.Play.Client.CLOSE_WINDOW);
        mapServer("container_slot_state_changed", PacketType.Play.Client.SLOT_STATE_CHANGE);
        mapServer("cookie_response", PacketType.Play.Client.COOKIE_RESPONSE);
        mapServer("custom_payload", PacketType.Play.Client.PLUGIN_MESSAGE);
        mapServer("debug_subscription_request", PacketType.Play.Client.DEBUG_SUBSCRIPTION_REQUEST);
        mapServer("edit_book", PacketType.Play.Client.EDIT_BOOK);
        mapServer("entity_tag_query", PacketType.Play.Client.QUERY_ENTITY_NBT);
        mapServer("interact", PacketType.Play.Client.INTERACT_ENTITY);
        mapServer("jigsaw_generate", PacketType.Play.Client.GENERATE_STRUCTURE);
        mapServer("keep_alive", PacketType.Play.Client.KEEP_ALIVE);
        mapServer("lock_difficulty", PacketType.Play.Client.LOCK_DIFFICULTY);
        mapServer("move_player_pos", PacketType.Play.Client.PLAYER_POSITION);
        mapServer("move_player_pos_rot", PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION);
        mapServer("move_player_rot", PacketType.Play.Client.PLAYER_ROTATION);
        mapServer("move_player_status_only", PacketType.Play.Client.PLAYER_FLYING);
        mapServer("move_vehicle", PacketType.Play.Client.VEHICLE_MOVE);
        mapServer("paddle_boat", PacketType.Play.Client.STEER_BOAT);
        mapServer("pick_item_from_block", PacketType.Play.Client.PICK_ITEM_FROM_BLOCK);
        mapServer("pick_item_from_entity", PacketType.Play.Client.PICK_ITEM_FROM_ENTITY);
        mapServer("ping_request", PacketType.Play.Client.DEBUG_PING);
        mapServer("place_recipe", PacketType.Play.Client.CRAFT_RECIPE_REQUEST);
        mapServer("player_abilities", PacketType.Play.Client.PLAYER_ABILITIES);
        mapServer("player_action", PacketType.Play.Client.PLAYER_DIGGING);
        mapServer("player_command", PacketType.Play.Client.ENTITY_ACTION);
        mapServer("player_input", PacketType.Play.Client.PLAYER_INPUT);
        mapServer("player_loaded", PacketType.Play.Client.PLAYER_LOADED);
        mapServer("pong", PacketType.Play.Client.PONG);
        mapServer("recipe_book_change_settings", PacketType.Play.Client.SET_RECIPE_BOOK_STATE);
        mapServer("recipe_book_seen_recipe", PacketType.Play.Client.SET_DISPLAYED_RECIPE);
        mapServer("rename_item", PacketType.Play.Client.NAME_ITEM);
        mapServer("resource_pack", PacketType.Play.Client.RESOURCE_PACK_STATUS);
        mapServer("seen_advancements", PacketType.Play.Client.ADVANCEMENT_TAB);
        mapServer("select_trade", PacketType.Play.Client.SELECT_TRADE);
        mapServer("set_beacon", PacketType.Play.Client.SET_BEACON_EFFECT);
        mapServer("set_carried_item", PacketType.Play.Client.HELD_ITEM_CHANGE);
        mapServer("set_command_block", PacketType.Play.Client.UPDATE_COMMAND_BLOCK);
        mapServer("set_command_minecart", PacketType.Play.Client.UPDATE_COMMAND_BLOCK_MINECART);
        mapServer("set_creative_mode_slot", PacketType.Play.Client.CREATIVE_INVENTORY_ACTION);
        mapServer("set_game_rule", PacketType.Play.Client.SET_GAME_RULE);
        mapServer("set_jigsaw_block", PacketType.Play.Client.UPDATE_JIGSAW_BLOCK);
        mapServer("set_structure_block", PacketType.Play.Client.UPDATE_STRUCTURE_BLOCK);
        mapServer("set_test_block", PacketType.Play.Client.SET_TEST_BLOCK);
        mapServer("sign_update", PacketType.Play.Client.UPDATE_SIGN);
        mapServer("spectate_entity", PacketType.Play.Client.SPECTATE_ENTITY);
        mapServer("swing", PacketType.Play.Client.ANIMATION);
        mapServer("spectator_action", PacketType.Play.Client.SPECTATE_ENTITY);
        mapServer("teleport_to_entity", PacketType.Play.Client.SPECTATE);
        mapServer("test_instance_block_action", PacketType.Play.Client.TEST_INSTANCE_BLOCK_ACTION);
        mapServer("use_item_on", PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT);
        mapServer("use_item", PacketType.Play.Client.USE_ITEM);
        mapServer("custom_click_action", PacketType.Play.Client.CUSTOM_CLICK_ACTION);
    }

    private ProtocolPacketIdSync() {}

    private static void mapClient(String mojangName, PacketType.Play.Server type) {
        CLIENTBOUND_MAP.put(mojangName, type);
    }

    private static void mapServer(String mojangName, PacketType.Play.Client type) {
        SERVERBOUND_MAP.put(mojangName, type);
    }

    public static void synchronize(Logger logger) {
        try {
            PacketType.prepare();
            Class<?> gameProtocolsClass = Class.forName("net.minecraft.network.protocol.game.GameProtocols");
            Map<String, Integer> clientboundIds = new HashMap<>();
            Map<String, Integer> serverboundIds = new HashMap<>();
            IdentityHashMap<Object, Boolean> visited = new IdentityHashMap<>();

            for (Field field : gameProtocolsClass.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())) continue;
                field.setAccessible(true);
                Object root = field.get(null);
                if (root == null) continue;
                extractFromObject(root, clientboundIds, serverboundIds, visited, 0);
            }

            // Las IDs obtenidas directamente de NMS GameProtocols ya corresponden a la numeración oficial del protocolo.

            int updatedClient = 0;
            int updatedServer = 0;

            if (!clientboundIds.isEmpty()) {
                updatedClient = applyClientboundIds(clientboundIds, logger);
            }
            if (!serverboundIds.isEmpty()) {
                updatedServer = applyServerboundIds(serverboundIds, logger);
            }

            ensureConfigurationFallbacks();
            syncEntityTypes(logger);
            syncEntityDataSerializers(logger);

            if (logger != null && (updatedClient > 0 || updatedServer > 0)) {
                logger.info("Protocol sync completado con NMS GameProtocols (Clientbound: "
                    + updatedClient + "/" + clientboundIds.size()
                    + ", Serverbound: " + updatedServer + "/" + serverboundIds.size()
                    + ", JOIN_GAME=" + PacketType.Play.Server.JOIN_GAME.getId(com.github.retrooper.packetevents.protocol.player.ClientVersion.getLatest())
                    + ", SPAWN_ENTITY=" + PacketType.Play.Server.SPAWN_ENTITY.getId(com.github.retrooper.packetevents.protocol.player.ClientVersion.getLatest())
                    + ", McVersion: " + Bukkit.getBukkitVersion() + ")");
            }
        } catch (Throwable ignored) {
        }
    }

    private static boolean extractFromObject(
        Object obj,
        Map<String, Integer> clientboundIds,
        Map<String, Integer> serverboundIds,
        IdentityHashMap<Object, Boolean> visited,
        int depth
    ) {
        if (obj == null || depth > 4 || visited.containsKey(obj)) return false;
        visited.put(obj, Boolean.TRUE);

        Class<?> cls = obj.getClass();
        String clsName = cls.getName();
        if (clsName.startsWith("java.lang.") || clsName.startsWith("java.util.concurrent.")) return false;

        try {
            for (Method m : cls.getMethods()) {
                if (m.getParameterCount() == 0 && m.getReturnType().getName().contains("Details")) {
                    m.setAccessible(true);
                    Object details = m.invoke(obj);
                    if (details != null) {
                        int before = clientboundIds.size() + serverboundIds.size();
                        extractViaDetails(details, clientboundIds, serverboundIds);
                        if (clientboundIds.size() + serverboundIds.size() > before) {
                            return true;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}

        if (obj instanceof List<?> list && !list.isEmpty()) {
            boolean foundAny = false;
            for (int i = 0; i < list.size(); i++) {
                Object elem = list.get(i);
                if (elem != null) {
                    Object packetTypeObj = findPacketType(elem);
                    if (packetTypeObj != null) {
                        recordPacketType(packetTypeObj, i, clientboundIds, serverboundIds);
                        foundAny = true;
                    }
                }
            }
            if (foundAny) return true;
        }

        Class<?> curr = cls;
        while (curr != null && curr != Object.class) {
            for (Field f : curr.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) continue;
                try {
                    f.setAccessible(true);
                    Object val = f.get(obj);
                    if (val != null) {
                        if (extractFromObject(val, clientboundIds, serverboundIds, visited, depth + 1)) {
                            return true;
                        }
                    }
                } catch (Throwable ignored) {}
            }
            curr = curr.getSuperclass();
        }
        return false;
    }

    private static void extractViaDetails(
        Object details,
        Map<String, Integer> clientboundIds,
        Map<String, Integer> serverboundIds
    ) {
        try {
            for (Method m : details.getClass().getMethods()) {
                if (m.getParameterCount() == 1 && m.getParameterTypes()[0].isInterface()) {
                    Class<?> visitorInterface = m.getParameterTypes()[0];
                    Object proxy = Proxy.newProxyInstance(
                        visitorInterface.getClassLoader(),
                        new Class<?>[]{visitorInterface},
                        (p, method, args) -> {
                            if (args != null && args.length == 2 && args[0] != null && args[1] instanceof Integer id) {
                                recordPacketType(args[0], id, clientboundIds, serverboundIds);
                            }
                            return null;
                        }
                    );
                    m.setAccessible(true);
                    m.invoke(details, proxy);
                }
            }
        } catch (Throwable ignored) {}
    }

    private static Object findPacketType(Object elem) {
        if (elem == null) return null;
        if (elem.getClass().getSimpleName().equals("PacketType")) {
            return elem;
        }
        Class<?> curr = elem.getClass();
        while (curr != null && curr != Object.class) {
            for (Field f : curr.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) continue;
                if (f.getType().getSimpleName().equals("PacketType")) {
                    try {
                        f.setAccessible(true);
                        return f.get(elem);
                    } catch (Throwable ignored) {}
                }
            }
            curr = curr.getSuperclass();
        }
        return null;
    }

    private static void recordPacketType(
        Object packetTypeObj,
        int id,
        Map<String, Integer> clientboundIds,
        Map<String, Integer> serverboundIds
    ) {
        try {
            String flow = null;
            String path = null;

            for (Field f : packetTypeObj.getClass().getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) continue;
                f.setAccessible(true);
                Object val = f.get(packetTypeObj);
                if (val == null) continue;
                if (val instanceof Enum<?> e) {
                    flow = e.name().toLowerCase(Locale.ROOT);
                } else {
                    String s = val.toString();
                    int colon = s.indexOf(':');
                    path = colon != -1 ? s.substring(colon + 1) : s;
                }
            }

            if (flow == null || path == null) {
                String raw = packetTypeObj.toString().toLowerCase(Locale.ROOT);
                if (raw.contains("clientbound")) flow = "clientbound";
                else if (raw.contains("serverbound")) flow = "serverbound";
                int colon = raw.lastIndexOf(':');
                if (colon != -1) {
                    path = raw.substring(colon + 1).trim();
                }
            }

            if (flow != null && path != null) {
                path = path.replace('/', '_');
                if (flow.contains("clientbound")) {
                    clientboundIds.put(path, id);
                } else if (flow.contains("serverbound")) {
                    serverboundIds.put(path, id);
                }
            }
        } catch (Throwable ignored) {}
    }

    @SuppressWarnings("unchecked")
    private static int applyClientboundIds(Map<String, Integer> clientboundIds, Logger logger) throws Exception {
        Field idsField = PacketType.Play.Server.class.getDeclaredField("ids");
        idsField.setAccessible(true);
        Field mapField = PacketType.Play.Server.class.getDeclaredField("PACKET_TYPE_ID_MAP");
        mapField.setAccessible(true);
        Map<Byte, Map<Integer, PacketTypeCommon>> typeIdMap =
            (Map<Byte, Map<Integer, PacketTypeCommon>>) mapField.get(null);

        int[] sampleIds = (int[]) idsField.get(PacketType.Play.Server.JOIN_GAME);
        int latestIdx = sampleIds.length - 1;
        Map<Integer, PacketTypeCommon> versionMap =
            typeIdMap.computeIfAbsent((byte) latestIdx, k -> new HashMap<>());
        versionMap.clear();

        for (PacketType.Play.Server pt : PacketType.Play.Server.values()) {
            int[] arr = (int[]) idsField.get(pt);
            arr[latestIdx] = -1;
        }

        int count = 0;
        List<String> unmapped = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : clientboundIds.entrySet()) {
            PacketType.Play.Server pt = CLIENTBOUND_MAP.get(entry.getKey());
            int id = entry.getValue();
            if (pt != null) {
                int[] arr = (int[]) idsField.get(pt);
                arr[latestIdx] = id;
                versionMap.put(id, pt);
                count++;
            } else {
                versionMap.put(id, PacketType.Play.Server.MAP_CHUNK_BULK);
                unmapped.add(entry.getKey() + "=" + id);
            }
        }

        for (int id = 0; id < 256; id++) {
            versionMap.putIfAbsent(id, PacketType.Play.Server.MAP_CHUNK_BULK);
        }

        if (logger != null && !unmapped.isEmpty()) {
            logger.info("Paquetes Clientbound 26.3 mapeados como passthrough nativo: " + unmapped);
        }
        return count;
    }

    @SuppressWarnings("unchecked")
    private static int applyServerboundIds(Map<String, Integer> serverboundIds, Logger logger) throws Exception {
        Field idsField = PacketType.Play.Client.class.getDeclaredField("ids");
        idsField.setAccessible(true);
        Field mapField = PacketType.Play.Client.class.getDeclaredField("PACKET_TYPE_ID_MAP");
        mapField.setAccessible(true);
        Map<Byte, Map<Integer, PacketTypeCommon>> typeIdMap =
            (Map<Byte, Map<Integer, PacketTypeCommon>>) mapField.get(null);

        int[] sampleIds = (int[]) idsField.get(PacketType.Play.Client.INTERACT_ENTITY);
        int latestIdx = sampleIds.length - 1;
        Map<Integer, PacketTypeCommon> versionMap =
            typeIdMap.computeIfAbsent((byte) latestIdx, k -> new HashMap<>());
        versionMap.clear();

        for (PacketType.Play.Client pt : PacketType.Play.Client.values()) {
            int[] arr = (int[]) idsField.get(pt);
            arr[latestIdx] = -1;
        }

        int count = 0;
        List<String> unmapped = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : serverboundIds.entrySet()) {
            PacketType.Play.Client pt = SERVERBOUND_MAP.get(entry.getKey());
            int id = entry.getValue();
            if (pt != null) {
                int[] arr = (int[]) idsField.get(pt);
                arr[latestIdx] = id;
                versionMap.put(id, pt);
                count++;
            } else {
                versionMap.put(id, PacketType.Play.Client.WINDOW_CONFIRMATION);
                unmapped.add(entry.getKey() + "=" + id);
            }
        }

        for (int id = 0; id < 256; id++) {
            versionMap.putIfAbsent(id, PacketType.Play.Client.WINDOW_CONFIRMATION);
        }

        if (logger != null && !unmapped.isEmpty()) {
            logger.info("Paquetes Serverbound 26.3 mapeados como passthrough nativo: " + unmapped);
        }
        return count;
    }

    @SuppressWarnings("unchecked")
    private static void ensureConfigurationFallbacks() {
        try {
            Field mapFieldS = PacketType.Configuration.Server.class.getDeclaredField("PACKET_TYPE_ID_MAP");
            mapFieldS.setAccessible(true);
            Map<Byte, Map<Integer, PacketTypeCommon>> mapS =
                (Map<Byte, Map<Integer, PacketTypeCommon>>) mapFieldS.get(null);
            for (Map<Integer, PacketTypeCommon> m : mapS.values()) {
                for (int id = 0; id < 64; id++) {
                    m.putIfAbsent(id, PacketType.Configuration.Server.PLUGIN_MESSAGE);
                }
            }

            Field mapFieldC = PacketType.Configuration.Client.class.getDeclaredField("PACKET_TYPE_ID_MAP");
            mapFieldC.setAccessible(true);
            Map<Byte, Map<Integer, PacketTypeCommon>> mapC =
                (Map<Byte, Map<Integer, PacketTypeCommon>>) mapFieldC.get(null);
            for (Map<Integer, PacketTypeCommon> m : mapC.values()) {
                for (int id = 0; id < 64; id++) {
                    m.putIfAbsent(id, PacketType.Configuration.Client.PLUGIN_MESSAGE);
                }
            }
        } catch (Throwable ignored) {}
    }

    @SuppressWarnings("unchecked")
    private static void syncEntityTypes(Logger logger) {
        try {
            Class<?> birClass = Class.forName("net.minecraft.core.registries.BuiltInRegistries");
            Field etField = birClass.getDeclaredField("ENTITY_TYPE");
            etField.setAccessible(true);
            Object registry = etField.get(null);
            if (registry == null) return;

            Method getIdMethod = null;
            Method getKeyMethod = null;
            for (Method m : registry.getClass().getMethods()) {
                if (m.getName().equals("getId") && m.getParameterCount() == 1) {
                    getIdMethod = m;
                } else if (m.getName().equals("getKey") && m.getParameterCount() == 1) {
                    getKeyMethod = m;
                }
            }

            Map<String, Integer> nmsEntityIds = new HashMap<>();
            if (registry instanceof Iterable<?> it && getIdMethod != null && getKeyMethod != null) {
                for (Object entry : it) {
                    int id = (Integer) getIdMethod.invoke(registry, entry);
                    Object key = getKeyMethod.invoke(registry, entry);
                    if (key != null) {
                        String fullKey = key.toString().toLowerCase(Locale.ROOT);
                        nmsEntityIds.put(fullKey, id);
                        int colon = fullKey.indexOf(':');
                        if (colon != -1) {
                            nmsEntityIds.put(fullKey.substring(colon + 1), id);
                        }
                    }
                }
            }

            if (nmsEntityIds.isEmpty()) return;

            Field typeIdsField = null;
            try {
                typeIdsField = com.github.retrooper.packetevents.util.mappings.VersionedRegistry.class.getDeclaredField("typeIds");
                typeIdsField.setAccessible(true);
            } catch (Throwable ignored) {}

            Map<Integer, Object>[] typeIdsArray = null;
            if (typeIdsField != null) {
                try {
                    typeIdsArray = (Map<Integer, Object>[]) typeIdsField.get(
                        com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.getRegistry()
                    );
                } catch (Throwable ignored) {}
            }

            int syncedCount = 0;
            for (com.github.retrooper.packetevents.protocol.entity.type.EntityType peType :
                 com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.values()) {
                if (peType instanceof com.github.retrooper.packetevents.protocol.mapper.AbstractMappedEntity mapped) {
                    var data = mapped.getRegistryData();
                    if (data != null && data.getName() != null) {
                        String name = data.getName().toString().toLowerCase(Locale.ROOT);
                        String rawName = data.getName().getKey().toLowerCase(Locale.ROOT);
                        Integer nmsId = nmsEntityIds.get(name);
                        if (nmsId == null) nmsId = nmsEntityIds.get(rawName);
                        if (nmsId != null) {
                            int[] ids = data.getData();
                            if (ids != null && ids.length > 0) {
                                ids[ids.length - 1] = nmsId;
                                syncedCount++;
                            }
                            if (typeIdsArray != null && typeIdsArray.length > 0) {
                                Map<Integer, Object> latestMap = typeIdsArray[typeIdsArray.length - 1];
                                if (latestMap != null) {
                                    latestMap.put(nmsId, peType);
                                }
                            }
                        }
                    }
                }
            }

            if (logger != null) {
                logger.info("EntityTypes sync completado con NMS (" + syncedCount + " tipos sincronizados, PLAYER="
                    + com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.PLAYER.getId(com.github.retrooper.packetevents.protocol.player.ClientVersion.getLatest())
                    + ", ARMOR_STAND=" + com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.ARMOR_STAND.getId(com.github.retrooper.packetevents.protocol.player.ClientVersion.getLatest()) + ")");
            }
        } catch (Throwable ignored) {}
    }

    private static void syncEntityDataSerializers(Logger logger) {
        try {
            Class<?> edsClass = Class.forName("net.minecraft.network.syncher.EntityDataSerializers");
            Method getSerializedIdMethod = null;
            for (Method m : edsClass.getMethods()) {
                if (m.getParameterCount() == 1 && (m.getName().equals("getSerializedId") || m.getReturnType() == int.class)) {
                    if (m.getParameterTypes()[0].getName().contains("EntityDataSerializer")) {
                        getSerializedIdMethod = m;
                        break;
                    }
                }
            }
            if (getSerializedIdMethod == null) return;

            Map<String, com.github.retrooper.packetevents.protocol.entity.data.EntityDataType<?>> map = new HashMap<>();
            map.put("BYTE", com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.BYTE);
            map.put("INT", com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.INT);
            map.put("LONG", com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.LONG);
            map.put("FLOAT", com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.FLOAT);
            map.put("STRING", com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.STRING);
            map.put("COMPONENT", com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.ADV_COMPONENT);
            map.put("OPTIONAL_COMPONENT", com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.OPTIONAL_ADV_COMPONENT);
            map.put("ITEM_STACK", com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.ITEMSTACK);
            map.put("BOOLEAN", com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.BOOLEAN);
            map.put("ROTATIONS", com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.ROTATION);
            map.put("BLOCK_POS", com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.BLOCK_POSITION);
            map.put("OPTIONAL_BLOCK_POS", com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.OPTIONAL_BLOCK_POSITION);
            map.put("DIRECTION", com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.BLOCK_FACE);
            map.put("BLOCK_STATE", com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.BLOCK_STATE);
            map.put("OPTIONAL_BLOCK_STATE", com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.OPTIONAL_BLOCK_STATE);
            map.put("COMPOUND_TAG", com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.NBT);
            map.put("PARTICLE", com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.PARTICLE);
            map.put("PARTICLES", com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.PARTICLES);
            map.put("VILLAGER_DATA", com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.VILLAGER_DATA);
            map.put("OPTIONAL_UNSIGNED_INT", com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.OPTIONAL_INT);
            map.put("POSE", com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.ENTITY_POSE);
            map.put("VECTOR3", com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.VECTOR3F);
            map.put("QUATERNION", com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.QUATERNION);

            int syncedCount = 0;
            for (Map.Entry<String, com.github.retrooper.packetevents.protocol.entity.data.EntityDataType<?>> entry : map.entrySet()) {
                try {
                    Field f = edsClass.getDeclaredField(entry.getKey());
                    f.setAccessible(true);
                    Object serializer = f.get(null);
                    if (serializer != null) {
                        int id = (Integer) getSerializedIdMethod.invoke(null, serializer);
                        var data = entry.getValue().getRegistryData();
                        if (data != null) {
                            int[] ids = data.getData();
                            if (ids != null && ids.length > 0) {
                                ids[ids.length - 1] = id;
                                syncedCount++;
                            }
                        }
                    }
                } catch (Throwable ignored) {}
            }

            if (logger != null && syncedCount > 0) {
                logger.info("EntityDataSerializers sync completado (" + syncedCount + " tipos sincronizados, POSE="
                    + com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.ENTITY_POSE.getId(com.github.retrooper.packetevents.protocol.player.ClientVersion.getLatest()) + ")");
            }
        } catch (Throwable ignored) {}
    }
}
