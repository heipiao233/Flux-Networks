package sonar.fluxnetworks.client.gui;

import net.minecraft.entity.player.PlayerEntity;
import sonar.fluxnetworks.FluxNetworks;
import sonar.fluxnetworks.api.translate.FluxTranslate;
import sonar.fluxnetworks.api.gui.EnumNavigationTabs;
import sonar.fluxnetworks.client.gui.basic.GuiButtonCore;
import sonar.fluxnetworks.client.gui.basic.GuiTabCore;
import sonar.fluxnetworks.client.gui.button.SlidedSwitchButton;
import sonar.fluxnetworks.client.gui.button.FluxTextWidget;
import sonar.fluxnetworks.api.network.NetworkSettings;
import sonar.fluxnetworks.api.utils.NBTType;
import sonar.fluxnetworks.common.handler.PacketHandler;
import sonar.fluxnetworks.common.tileentity.TileFluxCore;
import net.minecraft.util.text.TextFormatting;
import sonar.fluxnetworks.common.network.*;

/**
 * The home page.
 */
public class GuiFluxConnectorHome extends GuiTabCore {

    public FluxTextWidget fluxName, priority, limit;

    public SlidedSwitchButton surge, disableLimit, chunkLoad;

    private TileFluxCore tileEntity;
    private int timer;

    public GuiFluxConnectorHome(PlayerEntity player, TileFluxCore tileEntity) {
        super(player, tileEntity);
        this.tileEntity = tileEntity;
    }

    @Override
    public EnumNavigationTabs getNavigationTab(){
        return EnumNavigationTabs.TAB_HOME;
    }

    @Override
    protected void drawForegroundLayer(int mouseX, int mouseY) {
        super.drawForegroundLayer(mouseX, mouseY);
        screenUtils.renderNetwork(network.getSetting(NetworkSettings.NETWORK_NAME), network.getSetting(NetworkSettings.NETWORK_COLOR), 20, 8);
        renderTransfer(tileEntity, 0xffffff, 30, 90);
        screenUtils.drawCenteredString(font, TextFormatting.RED + FluxNetworks.proxy.getFeedback(false).getInfo(), 89, 150, 0xffffff);

        font.drawString(FluxTranslate.SURGE_MODE.t(), 20, 120, network.getSetting(NetworkSettings.NETWORK_COLOR));
        font.drawString(FluxTranslate.DISABLE_LIMIT.t(), 20, 132, network.getSetting(NetworkSettings.NETWORK_COLOR));
        if(!tileEntity.getConnectionType().isStorage()) {
            font.drawString(FluxTranslate.CHUNK_LOADING.t(), 20, 144, network.getSetting(NetworkSettings.NETWORK_COLOR));
        }
    }

    @Override
    public void init() {
        super.init();
        configureNavigationButtons(EnumNavigationTabs.TAB_HOME, navigationTabs);

        int color = network.getSetting(NetworkSettings.NETWORK_COLOR) | 0xff000000;
        fluxName = FluxTextWidget.create(FluxTranslate.NAME.t() + ": ", font, guiLeft + 16, guiTop + 28, 144, 12).setOutlineColor(color);
        fluxName.setMaxStringLength(24);
        fluxName.setText(tileEntity.getCustomName());
        fluxName.setResponder(string -> {
            tileEntity.customName = fluxName.getText();
            PacketHandler.INSTANCE.sendToServer(new TileByteBufPacket(tileEntity, tileEntity.getPos(), 1));
        });
        addButton(fluxName);

        priority = FluxTextWidget.create(FluxTranslate.PRIORITY.t() + ": ", font, guiLeft + 16, guiTop + 45, 144, 12).setOutlineColor(color).setDigitsOnly().setAllowNegatives(true);
        priority.setMaxStringLength(5);
        priority.setText(String.valueOf(tileEntity.priority));
        priority.setResponder(string -> {
            tileEntity.priority = priority.getValidInt();
            PacketHandler.INSTANCE.sendToServer(new TileByteBufPacket(tileEntity, tileEntity.getPos(), 2));
        });
        addButton(priority);

        limit = FluxTextWidget.create(FluxTranslate.TRANSFER_LIMIT.t() + ": ", font, guiLeft + 16, guiTop + 62, 144, 12).setOutlineColor(color).setDigitsOnly().setMaxValue(tileEntity.getMaxTransferLimit());
        limit.setMaxStringLength(9);
        limit.setText(String.valueOf(tileEntity.limit));
        limit.setResponder(string -> {
            tileEntity.limit = limit.getValidLong();
            PacketHandler.INSTANCE.sendToServer(new TileByteBufPacket(tileEntity, tileEntity.getPos(), 3));
        });
        addButton(limit);

        surge = new SlidedSwitchButton(140, 120, 1, guiLeft, guiTop, tileEntity.surgeMode);
        disableLimit = new SlidedSwitchButton(140, 132, 2, guiLeft, guiTop, tileEntity.disableLimit);
        switches.add(surge);
        switches.add(disableLimit);

        if(!tileEntity.getConnectionType().isStorage()) {
            chunkLoad = new SlidedSwitchButton(140, 144, 3, guiLeft, guiTop, tileEntity.chunkLoading);
            switches.add(chunkLoad);
        }

    }

    @Override
    public void onButtonClicked(GuiButtonCore button, int mouseX, int mouseY, int mouseButton){
        super.onButtonClicked(button, mouseX, mouseY, mouseButton);
        if(mouseButton == 0 && button instanceof SlidedSwitchButton){
            SlidedSwitchButton switchButton = (SlidedSwitchButton)button;
            switchButton.switchButton();
            switch (switchButton.id) {
                case 1:
                    tileEntity.surgeMode = switchButton.slideControl;
                    PacketHandler.INSTANCE.sendToServer(new TileByteBufPacket(tileEntity, tileEntity.getPos(), 4));
                    break;
                case 2:
                    tileEntity.disableLimit = switchButton.slideControl;
                    PacketHandler.INSTANCE.sendToServer(new TileByteBufPacket(tileEntity, tileEntity.getPos(), 5));
                    break;
                case 3:
                    PacketHandler.INSTANCE.sendToServer(new TilePacket(TilePacketEnum.CHUNK_LOADING, TilePacketHandler.getChunkLoadPacket(switchButton.slideControl), tileEntity.getPos(), tileEntity.getWorld().getDimension().getType().getId()));
                    break;
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if(timer == 0) {
            PacketHandler.INSTANCE.sendToServer(new NetworkUpdateRequestPacket(network.getNetworkID(), NBTType.NETWORK_GENERAL));
        }
        if(timer % 4 == 0) {
            if (chunkLoad != null) {
                chunkLoad.slideControl = tileEntity.chunkLoading;
            }
        }
        timer++;
        timer %= 100;
    }
}
