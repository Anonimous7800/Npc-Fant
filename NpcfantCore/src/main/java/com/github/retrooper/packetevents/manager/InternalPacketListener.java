package com.github.retrooper.packetevents.manager;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.util.LogManager;
import com.github.retrooper.packetevents.util.mappings.SynchronizedRegistriesHandler;
import com.github.retrooper.packetevents.wrapper.configuration.server.WrapperConfigServerConfigurationEnd;
import com.github.retrooper.packetevents.wrapper.configuration.server.WrapperConfigServerRegistryData;
import com.github.retrooper.packetevents.wrapper.handshaking.client.WrapperHandshakingClientHandshake;
import com.github.retrooper.packetevents.wrapper.login.server.WrapperLoginServerLoginSuccess;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerJoinGame;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerRespawn;

public class InternalPacketListener extends PacketListenerAbstract {

    public InternalPacketListener() {
        this(PacketListenerPriority.LOWEST);
    }

    public InternalPacketListener(PacketListenerPriority priority) {
        super(priority);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        User user = event.getUser();
        if (event.getPacketType() == PacketType.Login.Server.LOGIN_SUCCESS) {
            Object channel = event.getChannel();
            WrapperLoginServerLoginSuccess loginSuccess = new WrapperLoginServerLoginSuccess(event);
            UserProfile profile = loginSuccess.getUserProfile();
            user.getProfile().setUUID(profile.getUUID());
            user.getProfile().setName(profile.getName());
            user.getProfile().setTextureProperties(profile.getTextureProperties());
            synchronized (channel) {
                PacketEvents.getAPI().getProtocolManager().setChannel(profile.getUUID(), channel);
            }
            if (PacketEvents.getAPI().getLogManager().isDebug()) {
                PacketEvents.getAPI().getLogManager().debug("Mapped player UUID with their channel " + profile.getUUID() + " " + channel);
            }
            boolean proxy = PacketEvents.getAPI().getInjector().isProxy();
            if (proxy ? event.getUser().getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_20_2)
                      : event.getServerVersion().isNewerThanOrEquals(com.github.retrooper.packetevents.manager.server.ServerVersion.V_1_20_2)) {
                user.setEncoderState(ConnectionState.CONFIGURATION);
            } else {
                user.setConnectionState(ConnectionState.PLAY);
            }
        } else if (event.getPacketType() == PacketType.Configuration.Server.REGISTRY_DATA) {
            Object buf = event.getByteBuf();
            int readerIdx = buf != null ? ByteBufHelper.readerIndex(buf) : 0;
            try {
                WrapperConfigServerRegistryData registryData = new WrapperConfigServerRegistryData(event);
                if (registryData.getElements() != null) {
                    SynchronizedRegistriesHandler.handleRegistry(user, registryData, registryData.getRegistryKey(), registryData.getElements());
                }
                if (registryData.getRegistryData() != null) {
                    SynchronizedRegistriesHandler.handleLegacyRegistries(user, registryData, registryData.getRegistryData());
                }
            } catch (Throwable ignored) {
                event.setLastUsedWrapper(null);
                if (buf != null) {
                    ByteBufHelper.readerIndex(buf, readerIdx);
                }
            }
        } else if (event.getPacketType() == PacketType.Play.Server.JOIN_GAME) {
            Object buf = event.getByteBuf();
            int readerIdx = buf != null ? ByteBufHelper.readerIndex(buf) : 0;
            try {
                if (buf != null && ByteBufHelper.readableBytes(buf) >= 4) {
                    int entityId = ByteBufHelper.readInt(buf);
                    user.setEntityId(entityId);
                    ByteBufHelper.readerIndex(buf, readerIdx);
                }
                WrapperPlayServerJoinGame joinGame = new WrapperPlayServerJoinGame(event);
                user.setEntityId(joinGame.getEntityId());
                if (joinGame.getDimensionCodec() != null) {
                    SynchronizedRegistriesHandler.handleLegacyRegistries(user, joinGame, joinGame.getDimensionCodec());
                    user.finalizeRegistries(joinGame);
                }
                user.setDimensionType(joinGame.getDimensionType());
            } catch (Throwable ignored) {
                event.setLastUsedWrapper(null);
                if (buf != null) {
                    ByteBufHelper.readerIndex(buf, readerIdx);
                }
            }
        } else if (event.getPacketType() == PacketType.Play.Server.RESPAWN) {
            Object buf = event.getByteBuf();
            int readerIdx = buf != null ? ByteBufHelper.readerIndex(buf) : 0;
            try {
                WrapperPlayServerRespawn respawn = new WrapperPlayServerRespawn(event);
                user.setDimensionType(respawn.getDimensionType());
            } catch (Throwable ignored) {
                event.setLastUsedWrapper(null);
                if (buf != null) {
                    ByteBufHelper.readerIndex(buf, readerIdx);
                }
            }
        } else if (event.getPacketType() == PacketType.Play.Server.CONFIGURATION_START) {
            user.setEncoderState(ConnectionState.CONFIGURATION);
        } else if (event.getPacketType() == PacketType.Configuration.Server.CONFIGURATION_END) {
            user.setEncoderState(ConnectionState.PLAY);
            try {
                user.finalizeRegistries(new WrapperConfigServerConfigurationEnd(event));
            } catch (Throwable ignored) {
                event.setLastUsedWrapper(null);
            }
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        User user = event.getUser();
        if (event.getPacketType() == PacketType.Handshaking.Client.HANDSHAKE) {
            WrapperHandshakingClientHandshake handshake = new WrapperHandshakingClientHandshake(event);
            ClientVersion clientVersion = handshake.getClientVersion();
            ConnectionState state = handshake.getNextConnectionState();
            LogManager logger = PacketEvents.getAPI().getLogManager();
            if (logger.isDebug()) {
                logger.debug("Processed handshake for " + event.getAddress() + ": " + state.name() + " / " + handshake.getClientVersion().getReleaseName());
            }
            user.setClientVersion(clientVersion);
            user.setConnectionState(state);
        } else if (event.getPacketType() == PacketType.Login.Client.LOGIN_SUCCESS_ACK) {
            user.setDecoderState(ConnectionState.CONFIGURATION);
        } else if (event.getPacketType() == PacketType.Play.Client.CONFIGURATION_ACK) {
            user.setDecoderState(ConnectionState.CONFIGURATION);
        } else if (event.getPacketType() == PacketType.Configuration.Client.CONFIGURATION_END_ACK) {
            user.setDecoderState(ConnectionState.PLAY);
        }
    }
}

