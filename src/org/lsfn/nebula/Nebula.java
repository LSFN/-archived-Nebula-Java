package org.lsfn.nebula;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


public class Nebula {

    private GameManager gameManager;
    private boolean keepGoing;
    
    public Nebula() {
        this.gameManager = null;
        this.keepGoing = true;
    }
    
    private void printHelp() {
        System.out.println("Nebula commands:");
        System.out.println("\thelp   : Print this help text.");
        System.out.println("\texit   : End the program.");
    }
    
    private void processCommand(String commandStr) {
        String[] commandParts = commandStr.split(" ");
         
        if(commandParts[0].equals("help")) {
            printHelp();
        } else if(commandParts[0].equals("exit")) {
            this.keepGoing = false;
        } else {
            System.out.println("You're spouting gibberish. Please try English.");
        }
    }
    
    public void run(String[] args) {
        printHelp();
        
        this.gameManager = new GameManager();
        this.gameManager.start();
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        while(this.keepGoing) {
            try {
                processCommand(reader.readLine());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        // TODO Close up the threads
        
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        Nebula nebula = new Nebula();
        nebula.run(args);
    }

}
