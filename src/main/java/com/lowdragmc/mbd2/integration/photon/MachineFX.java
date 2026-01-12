package com.lowdragmc.mbd2.integration.photon;

import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.photon.client.fx.FX;
import com.lowdragmc.photon.client.fx.FXEffect;
import com.lowdragmc.photon.client.gameobject.IFXObject;
import lombok.Setter;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import com.lowdragmc.photon.client.gameobject.emitter.IParticleEmitter;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class MachineFX extends FXEffect {
    public final String identifier;
    public final MBDMachine machine;
    @Setter
    public boolean replaceExisting = false;

    public MachineFX(FX fx, String identifier, MBDMachine machine) {
        super(fx, machine.getLevel());
        this.identifier = identifier;
        this.machine = machine;
    }

    public void kill(boolean forcedDeath) {
        if (runtime != null) {
            this.runtime.destroy(forcedDeath);
        }
    }

    @Override
    public void start() {
        this.runtime = this.fx.createRuntime();
        IFXObject root = this.runtime.getRoot();

        var fxs = machine.getPhotonFXs();
        var previous = fxs.get(identifier);
        if (previous instanceof MachineFX machineFX) {
            if (replaceExisting || machineFX.runtime == null || !machineFX.runtime.isAlive()) {
                // replace the previous FX
                machineFX.kill(machineFX.forcedDeath);
            } else {
                // do not replace the previous FX
                return;
            }
        }
        fxs.put(identifier, this);

        var pos = machine.getPos();
        root.updatePos((new Vector3f((float)pos.getX(), (float)pos.getY(), (float)pos.getZ())).add(this.offset.x + 0.5F, this.offset.y + 0.5F, this.offset.z + 0.5F));
        root.updateRotation(this.rotation);
        root.updateScale(this.scale);
        this.runtime.emmit(this);
    }

    @Override
    public void updateFXObjectTick(IFXObject fxObject) {
        var pos = machine.getPos();
        if (this.runtime != null && !level.isLoaded(pos) || machine.getPhotonFXs().get(identifier) != this ||
                machine.isInValid() || IMachine.ofMachine(level, pos).stream().noneMatch(m -> m == machine)) {
            this.runtime.destroy(this.forcedDeath);
        }
        return false;
    }

}
