package controllers;

import io.hbt.bubblegum.core.Bubblegum;

import java.util.HashMap;

public class State {

    static Bubblegum bubblegum = new Bubblegum(false);

    static HashMap<String, NetworkDescription> descriptions = new HashMap<>();
    public static class NetworkDescription {
        private String id, name;
        private NetworkDescription(String id, String name) {
            this.name = name;
            this.id = id;
        }
        public String getName() { return this.name; }
        public String getID() { return this.id; }
        private void setName(String name) { this.name = name; }
    }

    static NetworkDescription newNetworkdescription(String id, String name) {
        NetworkDescription nd = new NetworkDescription(id, name);
        descriptions.put(id, nd);
        return nd;
    }

    static NetworkDescription getNetworkDescription(String id) {
        return descriptions.get(id);
    }

    static NetworkDescription updateNetworkDescription(String id, String name) {
        if(descriptions.containsKey(id)) {
            descriptions.get(id).setName(name);
            return descriptions.get(id);
        }
        return null;
    }

}
