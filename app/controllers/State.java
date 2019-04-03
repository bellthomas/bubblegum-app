package controllers;

import io.hbt.bubblegum.core.Bubblegum;
import io.hbt.bubblegum.core.auxiliary.NetworkingHelper;

import java.awt.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Random;

public class State {
    static {
        NetworkingHelper.setLookupExternalIP(false);
    }

    static Bubblegum bubblegum = new Bubblegum(false);

    static HashMap<String, NetworkDescription> descriptions = new HashMap<>();
    public static class NetworkDescription {
        private String id, hash, name, colour, displayName;
        private NetworkDescription(String hash, String id, String name, String displayName, String colour) {
            this.name = name;
            this.id = id;
            this.hash = hash;
            this.colour = colour;
            this.displayName = displayName;
        }
        public String getName() { return this.name; }
        public String getID() { return this.id; }
        public String getHash() { return this.hash; }
        public String getColour() { return this.colour; }
        public String getDisplayName() { return this.displayName; }
        private void setName(String name) { this.name = name; }
        private void setDisplayName(String displayName) { this.displayName = displayName; }
    }


    static NetworkDescription newNetworkDescription(String id, String name, String displayName, String colour) {
        String hash = idToHash(id);
        NetworkDescription nd = new NetworkDescription(hash, id, name, displayName, colour);
        descriptions.put(hash, nd);
        return nd;
    }

    static String idToHash(String id) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(id.getBytes());
            byte[] digest = md.digest();
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < 8; i++) {
                hexString.append(Integer.toHexString(0xFF & digest[i]));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return id;
        }
    }

    static NetworkDescription getNetworkDescription(String hash) {
        return descriptions.get(hash);
    }

    static NetworkDescription updateNetworkDescription(String hash, String name, String displayName) {
        if(descriptions.containsKey(hash)) {
            NetworkDescription nd = descriptions.get(hash);
            nd.setName(name);
            nd.setDisplayName(displayName);
            return nd;
        }
        return null;
    }

    public static String randomColour() {
        Random random = new Random();
        final float hue = (random.nextInt(8000) + 2000) / 10000f;
        final float saturation = (random.nextInt(2000) + 8000) / 10000f;
        final float luminance = 0.7f;
        final Color color = Color.getHSBColor(hue, saturation, luminance);
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }
}
