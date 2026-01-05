package io.github.noahdbyers.roguelite;

import java.util.*;
public class RoomLibrary {
    public final ArrayList<RoomTemplate> templates = new ArrayList<>();
    public final ArrayList<ArrayList<Room>> roomsByTemplateId = new ArrayList<>();
    public final Map<RoomTemplate, Integer> templateToId = new HashMap<>();

    public RoomLibrary(ArrayList<Room> rooms) {
        for (Room r : rooms) {
            RoomTemplate t = r.getTemplate();

            Integer id = templateToId.get(t);
            if (id == null) {
                id = templates.size();
                templateToId.put(t, id);
                templates.add(t);
                roomsByTemplateId.add(new ArrayList<>());
            }
            roomsByTemplateId.get(id).add(r);
        }
    }

    public Room pickRoomForTemplate(int templateId, Random rng) {
        ArrayList<Room> list = roomsByTemplateId.get(templateId);
        // Return a deep copy so each world cell can maintain per-room state (chests, cleared props, etc.)
        return new Room(list.get(rng.nextInt(list.size())));
    }

    public ArrayList<RoomTemplate> getTemplates() {
        return templates;
    }
}
