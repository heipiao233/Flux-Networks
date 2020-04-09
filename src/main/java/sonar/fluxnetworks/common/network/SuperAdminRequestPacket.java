package sonar.fluxnetworks.common.network;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;
import sonar.fluxnetworks.api.utils.Capabilities;
import sonar.fluxnetworks.common.capabilities.DefaultSuperAdmin;
import sonar.fluxnetworks.common.handler.PacketHandler;

public class SuperAdminRequestPacket extends AbstractPacket {

    public SuperAdminRequestPacket() {}

    public SuperAdminRequestPacket(PacketBuffer b) {}

    @Override
    public void encode(PacketBuffer b){}

    @Override
    public Object handle(NetworkEvent.Context ctx) {
        PlayerEntity player = PacketHandler.getPlayer(ctx);

        player.getCapability(Capabilities.SUPER_ADMIN).ifPresent(iSuperAdmin -> {
            if (iSuperAdmin.getPermission() || DefaultSuperAdmin.canActivateSuperAdmin(player)) {
                iSuperAdmin.changePermission();
                reply(ctx, new SuperAdminPacket(iSuperAdmin.getPermission()));
            }
        });
        return null;
    }

}
