import eu.scrayos.proxytablist.ProxyTablist;
import eu.scrayos.proxytablist.api.Variable;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

/**
 * @author geNAZt (fabian.fassbender42@googlemail.com)
 */
public class PlayerInServer implements Variable {
    private HashMap<ServerInfo, Iterator<ProxiedPlayer>> serverPlayerList = new HashMap<ServerInfo, Iterator<ProxiedPlayer>>();
    private static final Pattern pattern = Pattern.compile("\\{playerInServer:([\\w]+)\\}");
    private String server = null;

    private int lastRefreshId = -1;
    private int lastSlot;

    public PlayerInServer() {
        (new Thread() {
            public void run() {
                while(true) {
                    try {
                        for (ServerInfo serverInfo : BungeeCord.getInstance().getServers().values()) {
                            pingServer(serverInfo);
                        }

                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        System.out.println("Interrupted ping Thread");
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private void pingServer(final ServerInfo serverInfo) {
        serverInfo.ping(new Callback<ServerPing>() {
            @Override
            public void done(ServerPing serverPing, Throwable throwable) {
                System.out.println("Pinged Server " + serverInfo.getName() + ". The Server holds " + serverPing.getPlayers().getOnline() + " Players");
            }
        });
    }

    public String formatName(ProxiedPlayer p) {
        StringBuilder name = new StringBuilder();

        //Check for Prefix
        if(ProxyTablist.getInstance().getConfig().contains("variable.player.prefix." + p.getName())) {
            name.append(ProxyTablist.getInstance().getConfig().getString("variable.player.prefix." + p.getName(), ""));
        }

        for (String c : new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f", "l", "m", "n", "o", "k", "r"}) {
            if (p.hasPermission("proxy.tablist." + c)) {
                name.append("§");
                name.append(c);
            }
        }

        name.append(p.getName());
        return name.toString();
    }

    @Override
    public Pattern getPattern() {
        return pattern;
    }

    @Override
    public void setRefreshId(int refreshId) {
        if (lastRefreshId != refreshId) {
            lastRefreshId = refreshId;
            serverPlayerList.clear();
        }

        lastSlot = 0;
    }

    @Override
    public boolean hasUpdate(int slot, ProxiedPlayer proxiedPlayer) {
        if(slot != lastSlot) {
            lastSlot = slot;

            return true;
        }

        return false;
    }

    @Override
    public void setMatchResult(MatchResult matchResult) {
        server = matchResult.group(1);
    }

    @Override
    public boolean isForGlobalTablist() {
        return false;
    }

    @Override
    public String getText(Short ping) {
        ServerInfo serverInfo = BungeeCord.getInstance().getServerInfo(server);
        if (serverInfo == null) {
            return "Error";
        }

        if (!serverPlayerList.containsKey(serverInfo)) {
            serverPlayerList.put(serverInfo, serverInfo.getPlayers().iterator());
        }

        if (!serverPlayerList.get(serverInfo).hasNext()) {
            return "";
        }

        ProxiedPlayer player = serverPlayerList.get(serverInfo).next();
        ping = (new Integer(player.getPing())).shortValue();

        return formatName(player);
    }
}
