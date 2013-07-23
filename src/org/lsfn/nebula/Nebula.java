package org.lsfn.nebula;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.lsfn.nebula.StarshipServer.ServerStatus;


public class Nebula {

    private StarshipServer starshipServer;
    private GameManager gameManager;
    private boolean keepGoing;
    
    public Nebula() {
        this.starshipServer = null;
        this.gameManager = null;
        this.keepGoing = true;
    }
    
    private void startStarshipServer(int port) {
        this.starshipServer = new StarshipServer();
        if(port == -1) {
            this.starshipServer.listen();
        } else {
            this.starshipServer.listen(port);
        }
        this.starshipServer.start();
        this.gameManager = new GameManager(starshipServer);
        this.gameManager.start();
    }
    
    private void printHelp() {
        System.out.println("Nebula commands:");
        System.out.println("\thelp   : print this help text.");
        System.out.println("\tlisten : opens the starship server on the default port.");
        System.out.println("\texit   : end this program.");
    }
    
    private void processCommand(String commandStr) {
        String[] commandParts = commandStr.split(" ");
         
        if(commandParts[0].equals("listen")) {
            if(commandParts.length >= 2) {
                startStarshipServer(Integer.parseInt(commandParts[1]));
            } else {
                startStarshipServer(-1);
            }
        } else if(commandParts[0].equals("exit")) {
            this.keepGoing = false;
        } else if(commandParts[0].equals("help")) {
            printHelp();
        } else {
            System.out.println("You're spouting gibberish. Please try English.");
        }
    }
    
    public void run(String[] args) {
        printHelp();
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        while(this.keepGoing) {
            try {
                processCommand(reader.readLine());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        // Close up the threads
        if(starshipServer.getListenStatus() == ServerStatus.OPEN) {
            starshipServer.shutDown();
        }
        if(starshipServer.isAlive()) {
            try {
                starshipServer.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        Nebula nebula = new Nebula();
        nebula.run(args);
    }

}
