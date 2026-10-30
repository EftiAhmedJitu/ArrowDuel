package com.arrowduel.game.engine;

import android.os.Handler;
import android.os.Looper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handles Real Local Hotspot & Wi-Fi LAN Multiplayer Socket Communication:
 * - Host starts ServerSocket on port 8888.
 * - Client connects via Socket(hostIp, 8888).
 * - Bi-directional real-time synchronization of movement, aim, arrows, hits.
 */
public class NetworkDuelManager {

    public interface NetworkEventListener {
        void onConnected(boolean isHost, String opponentName);
        void onConnectionFailed(String error);
        void onOpponentMove(float normX, float normY, int tier, boolean isWalking);
        void onOpponentAim(float angle, float power);
        void onOpponentAimCancel();
        void onOpponentShoot(float normSx, float normSy, float vx, float vy, int color, int damage);
        void onOpponentHit(int damage, boolean isHeadshot, int remainingHp);
        default void onLocalPlayerHitByOpponent(int damage, boolean isHeadshot, int remainingHp) {}
        default void onOpponentConfirmedHit(int damage, boolean isHeadshot, int remainingHp) {}
        default void onOpponentMatchResult(String result) {}
        default void onTournamentConfig(boolean isTournament, int totalRounds, int targetWins) {}
        void onOpponentPowerup(String type);
        default void onOpponentNextReady(int level) {}
        default void onNextRoundStart(int level) {}
        default void onOpponentLeave() {}
        void onDisconnected();
    }

    private static NetworkDuelManager instance;
    public static final int PORT = 8888;

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter writer;
    private BufferedReader reader;

    private boolean isHost = false;
    private boolean isConnected = false;
    private boolean isTournament = false;
    private int tournamentTotalRounds = 3;
    private int tournamentTargetWins = 2;
    private NetworkEventListener listener;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static synchronized NetworkDuelManager getInstance() {
        if (instance == null) {
            instance = new NetworkDuelManager();
        }
        return instance;
    }

    public void setListener(NetworkEventListener listener) {
        this.listener = listener;
    }

    public boolean isConnected() { return isConnected; }
    public boolean isHost() { return isHost; }

    /**
     * Get Local Wi-Fi or Hotspot IP address
     */
    public static String getLocalIpAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            // Pass 1: Active Wi-Fi / Hotspot interface
            for (NetworkInterface intf : interfaces) {
                if (!intf.isUp() || intf.isLoopback()) continue;
                String name = intf.getName().toLowerCase();
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                        String sAddr = addr.getHostAddress();
                        if (sAddr != null) {
                            if (sAddr.startsWith("192.168.43.")) {
                                return sAddr;
                            }
                            if (name.contains("wlan") || name.contains("ap") || name.contains("wifi")) {
                                return sAddr;
                            }
                        }
                    }
                }
            }
            // Pass 2: Any non-loopback private IPv4 address
            for (NetworkInterface intf : interfaces) {
                if (!intf.isUp() || intf.isLoopback()) continue;
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                        String sAddr = addr.getHostAddress();
                        if (sAddr != null && (sAddr.startsWith("192.168.") || sAddr.startsWith("10.") || sAddr.startsWith("172."))) {
                            return sAddr;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return "192.168.43.1"; // Standard Android Hotspot Gateway Default
    }

    /**
     * Start Hosting a Room on Local Hotspot / Wi-Fi
     */
    public void startHosting(String playerName) {
        disconnect();
        isHost = true;

        executor.execute(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                serverSocket.setReuseAddress(true);
                // Listen for client connection
                clientSocket = serverSocket.accept();
                setupStreams(playerName);
            } catch (Exception e) {
                post(() -> {
                    if (listener != null) listener.onConnectionFailed("Host Error: " + e.getMessage());
                });
            }
        });
    }

    /**
     * Join Room hosted by a friend on Hotspot / Wi-Fi
     */
    public void joinRoom(String hostIp, String playerName) {
        disconnect();
        isHost = false;

        executor.execute(() -> {
            try {
                clientSocket = new Socket(hostIp, PORT);
                setupStreams(playerName);
            } catch (Exception e) {
                post(() -> {
                    if (listener != null) listener.onConnectionFailed("Could not connect to " + hostIp + ": " + e.getMessage());
                });
            }
        });
    }

    private void setupStreams(String playerName) {
        try {
            writer = new PrintWriter(clientSocket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            isConnected = true;

            // Send handshake
            writer.println("HELLO:" + playerName);
            if (isHost && isTournament) {
                writer.println("TOURNAMENT_CONFIG:true:" + tournamentTotalRounds + ":" + tournamentTargetWins);
            }

            // Listen for incoming messages
            String line;
            while (isConnected && (line = reader.readLine()) != null) {
                processMessage(line);
            }
        } catch (Exception e) {
            disconnect();
        }
    }

    private void processMessage(String msg) {
        if (msg == null) return;
        post(() -> {
            if (listener == null) return;
            try {
                String[] parts = msg.split(":");
                switch (parts[0]) {
                    case "HELLO":
                        String oppName = (parts.length > 1) ? parts[1] : "Opponent";
                        listener.onConnected(isHost, oppName);
                        break;

                    case "MOVE":
                        if (parts.length >= 5) {
                            float normX = Float.parseFloat(parts[1]);
                            float normY = Float.parseFloat(parts[2]);
                            int tier = Integer.parseInt(parts[3]);
                            boolean isWalking = Boolean.parseBoolean(parts[4]);
                            listener.onOpponentMove(normX, normY, tier, isWalking);
                        } else if (parts.length >= 3) {
                            float normX = Float.parseFloat(parts[1]);
                            int tier = Integer.parseInt(parts[2]);
                            listener.onOpponentMove(normX, -1f, tier, false);
                        }
                        break;

                    case "AIM":
                        if (parts.length >= 3) {
                            float angle = Float.parseFloat(parts[1]);
                            float power = Float.parseFloat(parts[2]);
                            listener.onOpponentAim(angle, power);
                        }
                        break;

                    case "AIM_CANCEL":
                        listener.onOpponentAimCancel();
                        break;

                    case "SHOOT":
                        if (parts.length >= 7) {
                            float normSx = Float.parseFloat(parts[1]);
                            float normSy = Float.parseFloat(parts[2]);
                            float vx = Float.parseFloat(parts[3]);
                            float vy = Float.parseFloat(parts[4]);
                            int col = Integer.parseInt(parts[5]);
                            int dmg = Integer.parseInt(parts[6]);
                            listener.onOpponentShoot(normSx, normSy, vx, vy, col, dmg);
                        }
                        break;

                    case "HIT":
                        if (parts.length >= 4) {
                            int dmg = Integer.parseInt(parts[1]);
                            boolean hs = Boolean.parseBoolean(parts[2]);
                            int remHp = Integer.parseInt(parts[3]);
                            listener.onOpponentHit(dmg, hs, remHp);
                        } else if (parts.length >= 3) {
                            int dmg = Integer.parseInt(parts[1]);
                            boolean hs = Boolean.parseBoolean(parts[2]);
                            listener.onOpponentHit(dmg, hs, -1);
                        }
                        break;

                    case "HIT_YOU":
                        if (parts.length >= 4) {
                            int dmg = Integer.parseInt(parts[1]);
                            boolean hs = Boolean.parseBoolean(parts[2]);
                            int remHp = Integer.parseInt(parts[3]);
                            listener.onLocalPlayerHitByOpponent(dmg, hs, remHp);
                        }
                        break;

                    case "HIT_SELF":
                        if (parts.length >= 4) {
                            int dmg = Integer.parseInt(parts[1]);
                            boolean hs = Boolean.parseBoolean(parts[2]);
                            int remHp = Integer.parseInt(parts[3]);
                            listener.onOpponentConfirmedHit(dmg, hs, remHp);
                        }
                        break;

                    case "MATCH_RESULT":
                        if (parts.length >= 2) {
                            listener.onOpponentMatchResult(parts[1]);
                        }
                        break;

                    case "TOURNAMENT_CONFIG":
                        if (parts.length >= 4) {
                            boolean isTourn = Boolean.parseBoolean(parts[1]);
                            int rounds = Integer.parseInt(parts[2]);
                            int target = Integer.parseInt(parts[3]);
                            this.isTournament = isTourn;
                            this.tournamentTotalRounds = rounds;
                            this.tournamentTargetWins = target;
                            listener.onTournamentConfig(isTourn, rounds, target);
                        }
                        break;

                    case "POWERUP":
                        if (parts.length >= 2) {
                            listener.onOpponentPowerup(parts[1]);
                        }
                        break;

                    case "NEXT_READY":
                        int readyLvl = (parts.length > 1) ? Integer.parseInt(parts[1]) : 1;
                        listener.onOpponentNextReady(readyLvl);
                        break;

                    case "NEXT_ROUND_START":
                        int startLvl = (parts.length > 1) ? Integer.parseInt(parts[1]) : 1;
                        listener.onNextRoundStart(startLvl);
                        break;

                    case "LEAVE_MATCH":
                        listener.onOpponentLeave();
                        break;

                    case "BYE":
                        listener.onDisconnected();
                        disconnect();
                        break;
                }
            } catch (Exception ignored) {}
        });
    }

    public void sendMove(float normX, float normY, int tier, boolean isWalking) {
        send("MOVE:" + normX + ":" + normY + ":" + tier + ":" + isWalking);
    }

    public void sendMove(float normX, int tier) {
        sendMove(normX, -1f, tier, false);
    }

    public void sendAimCancel() {
        send("AIM_CANCEL");
    }

    public void sendAim(float angle, float power) {
        send("AIM:" + angle + ":" + power);
    }

    public void sendShoot(float normSx, float normSy, float vx, float vy, int color, int damage) {
        send("SHOOT:" + normSx + ":" + normSy + ":" + vx + ":" + vy + ":" + color + ":" + damage);
    }

    public void sendHit(int damage, boolean headshot, int remainingHp) {
        sendHitYou(damage, headshot, remainingHp);
    }

    public void sendHitYou(int damage, boolean headshot, int remainingHp) {
        send("HIT_YOU:" + damage + ":" + headshot + ":" + remainingHp);
    }

    public void sendHitSelf(int damage, boolean headshot, int remainingHp) {
        send("HIT_SELF:" + damage + ":" + headshot + ":" + remainingHp);
    }

    public void sendMatchResult(String result) {
        send("MATCH_RESULT:" + result);
    }

    public void sendPowerup(String type) {
        send("POWERUP:" + type);
    }

    public void sendNextReady(int level) {
        send("NEXT_READY:" + level);
    }

    public void sendNextRoundStart(int level) {
        send("NEXT_ROUND_START:" + level);
    }

    public void setTournamentConfig(boolean isTourn, int rounds, int targetWins) {
        this.isTournament = isTourn;
        this.tournamentTotalRounds = rounds;
        this.tournamentTargetWins = targetWins;
        if (isConnected) {
            send("TOURNAMENT_CONFIG:" + isTourn + ":" + rounds + ":" + targetWins);
        }
    }

    public boolean isTournament() { return isTournament; }
    public int getTournamentTotalRounds() { return tournamentTotalRounds; }
    public int getTournamentTargetWins() { return tournamentTargetWins; }

    public void sendLeaveMatch() {
        send("LEAVE_MATCH");
    }

    private void send(String text) {
        if (!isConnected || writer == null) return;
        executor.execute(() -> {
            try {
                writer.println(text);
            } catch (Exception ignored) {}
        });
    }

    public void disconnect() {
        isConnected = false;
        try { if (writer != null) writer.close(); } catch (Exception ignored) {}
        try { if (reader != null) reader.close(); } catch (Exception ignored) {}
        try { if (clientSocket != null) clientSocket.close(); } catch (Exception ignored) {}
        try { if (serverSocket != null) serverSocket.close(); } catch (Exception ignored) {}
        clientSocket = null;
        serverSocket = null;
        writer = null;
        reader = null;
    }

    private void post(Runnable r) {
        mainHandler.post(r);
    }
}
