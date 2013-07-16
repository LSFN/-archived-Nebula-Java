package org.lsfn.nebula;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import org.lsfn.nebula.FF.*;

/**
 * Listens to a single socket.
 * Designed to be discarded when disconnected.
 * @author Lukeus_Maximus
 *
 */
public class StarshipListener {

    private Socket starshipSocket;
    private BufferedInputStream starshipInput;
    private BufferedOutputStream starshipOutput;
    
    public enum ListenerStatus {
        NOT_SETUP,
        CONNECTED,
        DISCONNECTED
    }
    private ListenerStatus listenerStatus;
    
    public StarshipListener(Socket consoleSocket) {
        this.starshipSocket = consoleSocket;
        this.starshipInput = null;
        this.starshipOutput = null;
        this.listenerStatus = ListenerStatus.NOT_SETUP;
    }
    
    public ListenerStatus getListenerStatus() {
        return this.listenerStatus;
    }
    
    private boolean setupStreams() {
        try {
            this.starshipInput = new BufferedInputStream(this.starshipSocket.getInputStream());
            this.starshipOutput = new BufferedOutputStream(this.starshipSocket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
    
    public ListenerStatus disconnect() {
        try {
            this.starshipSocket.close();
        } catch (IOException e) {
            // We simply don't care if this fails.
            // This assumes nothing bad comes of a close() failing.
            e.printStackTrace();
        }
        this.listenerStatus = ListenerStatus.DISCONNECTED;
        return this.listenerStatus;
    }
    
    public ListenerStatus sendMessageToStarship(FFdown downMessage) {
        if(this.listenerStatus == ListenerStatus.NOT_SETUP) {
            if(!setupStreams()) {
                this.listenerStatus = ListenerStatus.DISCONNECTED;
            }
        }
        if(this.listenerStatus == ListenerStatus.CONNECTED) {
            try {
                downMessage.writeDelimitedTo(this.starshipOutput);
                this.starshipOutput.flush();
                System.out.println(downMessage);
            } catch (IOException e) {
                e.printStackTrace();
                this.listenerStatus = ListenerStatus.DISCONNECTED;
            }
        }
        return this.listenerStatus;
    }
    
    public List<FFup> receiveMessagesFromStarship() {
        List<FFup> upMessages = new ArrayList<FFup>();
        if(this.listenerStatus == ListenerStatus.NOT_SETUP) {
            if(setupStreams()) {
                this.listenerStatus = ListenerStatus.CONNECTED;
            } else {
                this.listenerStatus = ListenerStatus.DISCONNECTED;
            }
        }
        if(this.listenerStatus == ListenerStatus.CONNECTED) {
            try {
                while(this.starshipInput.available() > 0) {
                    FFup upMessage = FFup.parseDelimitedFrom(this.starshipInput);
                    upMessages.add(upMessage);
                }
            } catch (IOException e) {
                e.printStackTrace();
                this.listenerStatus = ListenerStatus.DISCONNECTED;
            }
        }
        return upMessages;
    }
}
