package typingarena.server.lobby;

import typingarena.server.ClientHandler;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Room {
    private final String id = UUID.randomUUID().toString();
    private final String name;
    private final Set<ClientHandler> clients = Collections.synchronizedSet(new HashSet<>());

    public Room(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Set<ClientHandler> getClients() {
        return clients;
    }

    public Map<String, Object> toSummary() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("roomId", id);
        map.put("name", name);
        map.put("players", clients.size());
        return map;
    }
}
