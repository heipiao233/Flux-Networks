package sonar.fluxnetworks.client;

import it.unimi.dsi.fastutil.ints.Int2ObjectFunction;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import sonar.fluxnetworks.api.FluxConstants;
import sonar.fluxnetworks.api.misc.FeedbackInfo;
import sonar.fluxnetworks.common.connection.FluxNetwork;
import sonar.fluxnetworks.common.connection.FluxNetworkInvalid;
import sonar.fluxnetworks.common.util.FluxUtils;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class FluxClientCache {

    private static final FluxClientCache sInstance = new FluxClientCache();

    private static final Int2ObjectFunction<FluxNetwork> sCacheNetwork = i -> new FluxNetwork();

    private final Int2ObjectOpenHashMap<FluxNetwork> mNetworks = new Int2ObjectOpenHashMap<>();

    public static boolean superAdmin = false;
    public static boolean detailedNetworkView = false;

    public static int adminViewingNetwork = FluxConstants.INVALID_NETWORK_ID;

    private static FeedbackInfo feedback = FeedbackInfo.NONE; // Text message.

    private static int feedbackTimer = 0;

    private FluxClientCache() {
    }

    /**
     * A constant pointer to the global cache instance.
     *
     * @return the global instance
     */
    public static FluxClientCache getInstance() {
        return sInstance;
    }

    /**
     * Release buffers and view models.
     */
    public void invalidate() {
        synchronized (mNetworks) {
            mNetworks.trim(0);
        }
        adminViewingNetwork = FluxConstants.INVALID_NETWORK_ID;
        feedback = FeedbackInfo.NONE;
    }

    public void onNetworkUpdate(@Nonnull FriendlyByteBuf payload) {
        final byte type = payload.readByte();
        final int size = payload.readVarInt();
        synchronized (mNetworks) {
            for (int i = 0; i < size; i++) {
                int id = payload.readVarInt();
                CompoundTag tag = payload.readNbt();
                assert tag != null;
                mNetworks.computeIfAbsent(id, sCacheNetwork).readCustomTag(tag, type);
            }
        }
        //TODO notify view models
    }

    public void delete(int id) {
        synchronized (mNetworks) {
            mNetworks.remove(id);
        }
    }

    public static void updateConnections(int networkID, List<CompoundTag> tags) {
        FluxNetwork network = sInstance.mNetworks.get(networkID);
        if (network != null) {
            for (CompoundTag tag : tags) {
                GlobalPos globalPos = FluxUtils.readGlobalPos(tag);
                network.getConnectionByPos(globalPos).ifPresent(c -> c.readCustomTag(tag,
                        FluxConstants.TYPE_PHANTOM_UPDATE));
            }
        }
    }

    @Nonnull
    public static FluxNetwork getNetwork(int id) {
        return sInstance.mNetworks.getOrDefault(id, FluxNetworkInvalid.INSTANCE);
    }

    public static String getDisplayName(@Nonnull CompoundTag subTag) {
        FluxNetwork network = getNetwork(subTag.getInt(FluxConstants.NETWORK_ID));
        if (network.isValid()) {
            return network.getNetworkName();
        }
        return "NONE";
    }

    @Nonnull
    public static String getFeedbackText() {
        return feedback.getText();
    }

    public static int getFeedbackColor() {
        return 16733525 | (int) (Math.min(feedbackTimer, 16) * Math.min(61 - feedbackTimer, 5) * 3.1875f) << 24;
    }

    public static void setFeedbackText(FeedbackInfo info) {
        feedback = info;
        if (feedbackTimer > 0) {
            feedbackTimer = 58;
        } else {
            feedbackTimer = 60;
        }
    }

    /*public static void tick() {
        if (feedback != FeedbackInfo.NONE) {
            if (--feedbackTimer <= 0) {
                feedback = FeedbackInfo.NONE;
            }
        }
    }*/

    @Nonnull
    public static Collection<FluxNetwork> getAllNetworks() {
        return sInstance.mNetworks.values();
    }
}
