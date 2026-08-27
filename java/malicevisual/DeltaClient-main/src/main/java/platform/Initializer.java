package platform;


import aethereal.core.Skeleton;
import aethereal.network.MaliceUsers;
import net.fabricmc.api.ClientModInitializer;

public class Initializer implements ClientModInitializer {


    public void onInitializeClient() {
        MaliceUsers.registerPayloads();
        new Skeleton();
        MaliceUsers.get().setup();
    }
}
