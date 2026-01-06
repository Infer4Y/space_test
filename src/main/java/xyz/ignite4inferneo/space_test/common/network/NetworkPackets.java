package xyz.ignite4inferneo.space_test.common.network;

import xyz.ignite4inferneo.space_test.common.entity.PlayerEntity;

import java.util.UUID;

/**
 * Network packet definitions for multiplayer
 * These packets can be serialized/deserialized for network transmission
 */
public class NetworkPackets {

    /**
     * Base packet class
     */
    public static abstract class Packet {
        public abstract PacketType getType();
    }

    public enum PacketType {
        PLAYER_JOIN,
        PLAYER_LEAVE,
        PLAYER_POSITION,
        PLAYER_ACTION,
        BLOCK_UPDATE,
        ENTITY_SPAWN,
        ENTITY_UPDATE,
        ENTITY_REMOVE,
        CHAT_MESSAGE
    }

    /**
     * Player joined the server
     */
    public static class PlayerJoinPacket extends Packet {
        public UUID playerId;
        public String username;
        public double x, y, z;

        public PlayerJoinPacket(UUID playerId, String username, double x, double y, double z) {
            this.playerId = playerId;
            this.username = username;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public PacketType getType() {
            return PacketType.PLAYER_JOIN;
        }
    }

    /**
     * Player left the server
     */
    public static class PlayerLeavePacket extends Packet {
        public UUID playerId;

        public PlayerLeavePacket(UUID playerId) {
            this.playerId = playerId;
        }

        @Override
        public PacketType getType() {
            return PacketType.PLAYER_LEAVE;
        }
    }

    /**
     * Player position/rotation update
     */
    public static class PlayerPositionPacket extends Packet {
        public UUID playerId;
        public double x, y, z;
        public double vx, vy, vz;
        public float yaw, pitch;
        public boolean onGround;

        public PlayerPositionPacket(PlayerEntity.PlayerState state) {
            this.playerId = state.playerId;
            this.x = state.x;
            this.y = state.y;
            this.z = state.z;
            this.vx = state.vx;
            this.vy = state.vy;
            this.vz = state.vz;
            this.yaw = state.yaw;
            this.pitch = state.pitch;
            this.onGround = state.onGround;
        }

        @Override
        public PacketType getType() {
            return PacketType.PLAYER_POSITION;
        }
    }

    /**
     * Player action (jump, attack, use item, etc.)
     */
    public static class PlayerActionPacket extends Packet {
        public UUID playerId;
        public Action action;
        public int data; // Additional action data

        public enum Action {
            JUMP,
            ATTACK,
            USE_ITEM,
            DROP_ITEM,
            TAKE_DAMAGE
        }

        public PlayerActionPacket(UUID playerId, Action action, int data) {
            this.playerId = playerId;
            this.action = action;
            this.data = data;
        }

        @Override
        public PacketType getType() {
            return PacketType.PLAYER_ACTION;
        }
    }

    /**
     * Block placed or broken
     */
    public static class BlockUpdatePacket extends Packet {
        public int x, y, z;
        public String blockId;
        public UUID placedBy; // Player who placed/broke it

        public BlockUpdatePacket(int x, int y, int z, String blockId, UUID placedBy) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.blockId = blockId;
            this.placedBy = placedBy;
        }

        @Override
        public PacketType getType() {
            return PacketType.BLOCK_UPDATE;
        }
    }

    /**
     * Entity spawned in world
     */
    public static class EntitySpawnPacket extends Packet {
        public UUID entityId;
        public String entityType;
        public double x, y, z;

        public EntitySpawnPacket(UUID entityId, String entityType, double x, double y, double z) {
            this.entityId = entityId;
            this.entityType = entityType;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public PacketType getType() {
            return PacketType.ENTITY_SPAWN;
        }
    }

    /**
     * Entity position/state update
     */
    public static class EntityUpdatePacket extends Packet {
        public UUID entityId;
        public double x, y, z;
        public float yaw, pitch;

        public EntityUpdatePacket(UUID entityId, double x, double y, double z, float yaw, float pitch) {
            this.entityId = entityId;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }

        @Override
        public PacketType getType() {
            return PacketType.ENTITY_UPDATE;
        }
    }

    /**
     * Entity removed from world
     */
    public static class EntityRemovePacket extends Packet {
        public UUID entityId;

        public EntityRemovePacket(UUID entityId) {
            this.entityId = entityId;
        }

        @Override
        public PacketType getType() {
            return PacketType.ENTITY_REMOVE;
        }
    }

    /**
     * Chat message
     */
    public static class ChatMessagePacket extends Packet {
        public UUID senderId;
        public String senderName;
        public String message;

        public ChatMessagePacket(UUID senderId, String senderName, String message) {
            this.senderId = senderId;
            this.senderName = senderName;
            this.message = message;
        }

        @Override
        public PacketType getType() {
            return PacketType.CHAT_MESSAGE;
        }
    }
}