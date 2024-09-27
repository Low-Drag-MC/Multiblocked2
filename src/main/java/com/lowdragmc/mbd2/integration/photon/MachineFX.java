package com.lowdragmc.mbd2.integration.photon;

import com.lowdragmc.mbd2.api.machine.IMachine;
import com.lowdragmc.mbd2.common.machine.MBDMachine;
import com.lowdragmc.photon.client.fx.FX;
import com.lowdragmc.photon.client.fx.FXEffect;
import com.lowdragmc.photon.client.gameobject.emitter.IParticleEmitter;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class MachineFX extends FXEffect {
    public final String identifier;
    public final MBDMachine machine;

    public MachineFX(FX fx, String identifier, MBDMachine machine) {
        super(fx, machine.getLevel());
        this.identifier = identifier;
        this.machine = machine;
    }

    public void kill(boolean forcedDeath) {
        for (IParticleEmitter emitter : emitters) {
            emitter.remove(forcedDeath);
        }
    }

    @Override
    public void start() {
        this.emitters.clear();
        this.emitters.addAll(fx.generateEmitters());
        if (this.emitters.isEmpty()) return;
        var fxs = machine.getPhotonFXs();
        var previous = fxs.get(identifier);
        if (previous instanceof MachineFX machineFX) {
            machineFX.kill(machineFX.forcedDeath);
        }
        fxs.put(identifier, this);

        var pos = machine.getPos();
        var realPos= new Vector3f(pos.getX(), pos.getY(), pos.getZ()).add((float) (xOffset + 0.5f), (float) (yOffset + 0.5f), (float) (zOffset + 0.5f));

        for (var emitter : emitters) {
            if (!emitter.isSubEmitter()) {
                emitter.reset();
                emitter.self().setDelay(delay);
                emitter.emmitToLevel(this, level, realPos.x, realPos.y, realPos.z, xRotation, yRotation, zRotation);
            }
        }
    }

    @Override
    public boolean updateEmitter(IParticleEmitter emitter) {
        var pos = machine.getPos();
        if (!level.isLoaded(pos) || machine.getPhotonFXs().get(identifier) != this ||
                machine.isInValid() || IMachine.ofMachine(level, pos).stream().noneMatch(m -> m == machine)) {
            emitter.remove(forcedDeath);
            return forcedDeath;
        }
        return false;
    }

}
