package controllers;

import com.google.common.graph.Network;
import io.hbt.bubblegum.core.Bubblegum;
import io.hbt.bubblegum.core.Configuration;
import io.hbt.bubblegum.core.auxiliary.NetworkingHelper;
import io.hbt.bubblegum.core.databasing.Post;
import io.hbt.bubblegum.core.exceptions.MalformedKeyException;
import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.NodeID;

import java.awt.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

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

    static BubblegumNode getNodeForHash(String hash) {
        NetworkDescription nd = getNetworkDescription(hash);
        if(nd != null) {
            return bubblegum.getNode(nd.getID());
        }
        return null;
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


    // Posts cache, index is postID
    private static HashMap<String, HashMap<Long, TreeSet<String>>> index = new HashMap<>();
    private static HashMap<String, HashMap<String, Post>> cache = new HashMap<>();

    static List<Post> getFeed(String hash, int numEpochs) {
        NetworkDescription nd = getNetworkDescription(hash);
        if(nd == null) return null;
        BubblegumNode node = bubblegum.getNode(nd.getID());
        if(node == null) return null;

        long currentEpoch = System.currentTimeMillis() / Configuration.BIN_EPOCH_DURATION;
        long downTo = currentEpoch - numEpochs;

        List<Post> found = new ArrayList<>();
        long c = currentEpoch;
        while(c > downTo) {
            found.addAll(refreshEpoch(node, c));
            c--;
        }

        Collections.sort(found, (a, b) -> -1 * (int)(a.getTimeCreated() - b.getTimeCreated()));
        return found;
    }

    static List<Post> refreshEpoch(BubblegumNode node, long epoch) {
        List<byte[]> idBytes = node.lookup(NodeID.hash(epoch));
        List<Post> results = new ArrayList<>();

        if(!cache.containsKey(node.getIdentifier())) cache.put(node.getIdentifier(), new HashMap<>());
        HashMap<String, Post> nodeCache = cache.get(node.getIdentifier());

        if(!index.containsKey(node.getIdentifier())) index.put(node.getIdentifier(), new HashMap<>());
        HashMap<Long, TreeSet<String>> nodeIndex = index.get(node.getIdentifier());

        if(idBytes != null) {
            List<String> ids = idBytes.stream().map((b) -> new String(b)).collect(Collectors.toList());
            for(String id : ids) {
                if(nodeCache.containsKey(id)) {
                    results.add(nodeCache.get(id));
                }
                else {
                    String[] idParts = id.split(":");
                    if (idParts.length == 2) {
                        try {
                            NodeID nid = new NodeID(idParts[0]);
                            List<Post> posts = node.query(nid, -1, -1, new ArrayList<>() {{
                                add(idParts[1]);
                            }});
                            if (posts != null && posts.size() > 0) {
                                for (Post p : posts) {
                                    nodeCache.put(p.getOwner() + ":" + p.getID(), p);
                                    if(!nodeIndex.containsKey(epoch)) nodeIndex.put(epoch, new TreeSet<>());
                                    nodeIndex.get(epoch).add(p.getOwner() + ":" + p.getID());
                                    results.add(p);
                                }
                            }
                        } catch (MalformedKeyException e) {
                            // Best effort
                        }
                    }
                }
            }
        }

        Collections.sort(results, (a, b) -> -1 * (int)(a.getTimeCreated() - b.getTimeCreated()));
        return results;
    }

    private static void cacheUpkeep(String hash) {

    }

}
