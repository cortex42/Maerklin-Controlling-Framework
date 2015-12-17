package de.cortex42.maerklin.framework.scripting;

import de.cortex42.maerklin.framework.CS2CANCommands;
import de.cortex42.maerklin.framework.FrameworkException;
import de.cortex42.maerklin.framework.packetlistener.VelocityPacketListener;

/**
 * Created by ivo on 20.11.15.
 */
public class ScriptBooleanEventTrainVelocity implements BooleanEvent {
    private final ScriptContext scriptContext;
    private final int locId;
    private final int velocity;
    private final long timeout;
    private final long DELAY = 250L;
    private final static long DEFAULT_TIMEOUT = 60000L; //60s

    public ScriptBooleanEventTrainVelocity(final ScriptContext scriptContext, final int locId, final int velocity, final long timeout) {
        this.scriptContext = scriptContext;
        this.locId = locId;
        this.velocity = velocity;
        this.timeout = timeout;
    }

    public ScriptBooleanEventTrainVelocity(final ScriptContext scriptContext, final int locId, final int velocity) {
        this(scriptContext, locId, velocity, DEFAULT_TIMEOUT);
    }

    @Override
    public boolean getAsBoolean() throws FrameworkException {
        return check();
    }

    private boolean check() throws FrameworkException {
        WaitingThreadExchangeObject waitingThreadExchangeObject = new WaitingThreadExchangeObject();

        VelocityPacketListener velocityPacketListener = new VelocityPacketListener() {
            @Override
            public void onSuccess() {
                if (getVelocity() == velocity) {
                    waitingThreadExchangeObject.value = true;
                }
            }
        };

        scriptContext.addPacketListener(velocityPacketListener);

        long counter = 0L;
        while (!waitingThreadExchangeObject.value) {
            scriptContext.writeCANPacket(CS2CANCommands.queryVelocity(locId));

            try {
                Thread.sleep(DELAY);
                counter += DELAY;

                if (counter >= timeout) {
                    //timeout
                    return false;
                }
            } catch (InterruptedException e) {
                throw new FrameworkException(e);
            } finally {
                scriptContext.removePacketListener(velocityPacketListener);
            }
        }

        return waitingThreadExchangeObject.value;
    }
}
