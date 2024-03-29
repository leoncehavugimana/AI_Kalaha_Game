package ai;

import ai.Global;
import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.*;
import kalaha.*;

/**
 * This is the main class for your Kalaha AI bot. Currently it only makes a
 * random, valid move each turn.
 *
 * @author Johan Hagelbäck
 */
public class AIClient implements Runnable {

    private int player;
    private JTextArea text;

    private PrintWriter out;
    private BufferedReader in;
    private Thread thr;
    private Socket socket;
    private boolean running;
    private boolean connected;

    private boolean searching;

    /**
     * Creates a new client.
     */
    public AIClient() {
        player = -1;
        connected = false;

        //This is some necessary client stuff. You don't need
        //to change anything here.
        initGUI();

        try {
            addText("Connecting to localhost:" + KalahaMain.port);
            socket = new Socket("localhost", KalahaMain.port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            addText("Done");
            connected = true;
        } catch (Exception ex) {
            addText("Unable to connect to server");
            return;
        }
    }

    /**
     * Starts the client thread.
     */
    public void start() {
        //Don't change this
        if (connected) {
            thr = new Thread(this);
            thr.start();
        }
    }

    /**
     * Creates the GUI.
     */
    private void initGUI() {
        //Client GUI stuff. You don't need to change this.
        JFrame frame = new JFrame("My AI Client");
        frame.setLocation(Global.getClientXpos(), 445);
        frame.setSize(new Dimension(420, 250));
        frame.getContentPane().setLayout(new FlowLayout());

        text = new JTextArea();
        JScrollPane pane = new JScrollPane(text);
        pane.setPreferredSize(new Dimension(400, 210));

        frame.getContentPane().add(pane);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.setVisible(true);
    }

    /**
     * Adds a text string to the GUI textarea.
     *
     * @param txt The text to add
     */
    public void addText(String txt) {
        //Don't change this
        text.append(txt + "\n");
        text.setCaretPosition(text.getDocument().getLength());
    }

    /**
     * Thread for server communication. Checks when it is this client's turn to
     * make a move.
     */
    public void run() {
        String reply;
        running = true;

        try {
            while (running) {
                //Checks which player you are. No need to change this.
                if (player == -1) {
                    out.println(Commands.HELLO);
                    reply = in.readLine();

                    String tokens[] = reply.split(" ");
                    player = Integer.parseInt(tokens[1]);

                    addText("I am player " + player);
                }

                //Check if game has ended. No need to change this.
                out.println(Commands.WINNER);
                reply = in.readLine();
                if (reply.equals("1") || reply.equals("2")) {
                    int w = Integer.parseInt(reply);
                    if (w == player) {
                        addText("I won!");
                    } else {
                        addText("I lost...");
                    }
                    running = false;
                }
                if (reply.equals("0")) {
                    addText("Even game!");
                    running = false;
                }

                //Check if it is my turn. If so, do a move
                out.println(Commands.NEXT_PLAYER);
                reply = in.readLine();
                if (!reply.equals(Errors.GAME_NOT_FULL) && running) {
                    int nextPlayer = Integer.parseInt(reply);

                    if (nextPlayer == player) {
                        out.println(Commands.BOARD);
                        String currentBoardStr = in.readLine();
                        boolean validMove = false;
                        while (!validMove) {
                            long startT = System.currentTimeMillis();
                            //This is the call to the function for making a move.
                            //You only need to change the contents in the getMove()
                            //function.
                            GameState currentBoard = new GameState(currentBoardStr);
                            int cMove = getMove(currentBoard);

                            //Timer stuff
                            long tot = System.currentTimeMillis() - startT;
                            double e = (double) tot / (double) 1000;

                            out.println(Commands.MOVE + " " + cMove + " " + player);
                            reply = in.readLine();
                            if (!reply.startsWith("ERROR")) {
                                validMove = true;
                                addText("Made move " + cMove + " in " + e + " secs");
                            }
                        }
                    }
                }

                //Wait
                Thread.sleep(100);
            }
        } catch (Exception ex) {
            running = false;
        }

        try {
            socket.close();
            addText("Disconnected from server");
        } catch (Exception ex) {
            addText("Error closing connection: " + ex.getMessage());
        }
    }

    /**
     * This is the method that makes a move each time it is your turn. Here you
     * need to change the call to the random method to your Minimax search.
     *
     * @param currentBoard The current board state
     * @return Move to make (1-6)
     */
    public int getMove(GameState currentBoard) {

        // Grade C. Minimax with DPS and ABP
        Node rootNode = new Node(currentBoard.clone(), 0, -999, 999); // Create a new node object. This one is the root node. Params: GameState, NodeId, Alpha, Beta values.
        int depth = 8; // How deep the algoritm should go.
        Node bestNode = minimaxAbp(depth, rootNode, 1); // Returns the node with the best path. Params: Maximum depth, root node, player (1 = MAX, 2 = MIN).
        int bestMove = bestNode.prevNode; // Best move is stored in the nodes prevNode variable. 

        return bestMove;
    }

    /**
     * This is the Minimax method. It uses Depth-first search along with alpha-beta pruning. 
     *
     * @param depthLevel The current depth of the search.
     * @param node The node to be expanded / searched.
     * @param player The player that is going to make the move, either 1 for MAX or 2 for MIN.
     * @return The node with the best utility score. 
     */
    public Node minimaxAbp(int depthLevel, Node node, int player) {

        // Expand the node to find children. 
        node.expandNode();
        
        // Check if maximum depthlevel has been reached or if node is terminal node.
        if (depthLevel <= 0 || node.isTerminalNode) {
            // Calculate utility score for the node.
            node.calculateUtilityScore();
            return node;
        } else if (player == 1) { // If player = MAX
            // Initiate to worst possible score. 
            node.utilityScore = -999;
            
            for (Node n : node.children) {
                if (node.utilityScore > node.alpha) {
                    node.alpha = node.utilityScore;
                } // Check to see if we can prune 
                else if (node.utilityScore > node.beta) {
                    //Max prunes this branch. 
                    break;
                }            
                n.state.makeMove(n.nodeId);
                // Recursivly call itself to go deeper into the tree. 
                n = minimaxAbp(depthLevel - 1, n, 2);
                // If new utility score is better, store it as well as store the nodeId in prevNode for backtracking. 
                if (n.utilityScore > node.utilityScore) {
                    node.utilityScore = n.utilityScore;
                    node.prevNode = n.nodeId;
                }
            }
            return node;
        } else if (player == 2) { // If player = MIN
            node.utilityScore = 999;
            for (Node n : node.children) {   
                if (node.utilityScore < node.beta) {
                    node.beta = node.utilityScore;
                } // Check to see if we can prune 
                else if (node.utilityScore < node.alpha) {
                    break;
                }
                n.state.makeMove(n.nodeId);
                n = minimaxAbp(depthLevel - 1, n, 1);
                if (n.utilityScore < node.utilityScore) {
                    node.utilityScore = n.utilityScore;
                    node.prevNode = n.nodeId;
                }
            }
            return node;
        }

        // Should never get here.
        return null;
    }

    /**
     * Returns a random ambo number (1-6) used when making a random move.
     *
     * @return Random ambo number
     */
    public int getRandom() {
        return 1 + (int) (Math.random() * 6);
    }
}
