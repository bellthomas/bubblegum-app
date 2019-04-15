package controllers;

import io.hbt.bubblegum.core.Bubblegum;
import io.hbt.bubblegum.core.Configuration;
import io.hbt.bubblegum.core.databasing.Post;
import io.hbt.bubblegum.core.exceptions.MalformedKeyException;
import io.hbt.bubblegum.core.kademlia.BubblegumNode;
import io.hbt.bubblegum.core.kademlia.NodeID;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class State {

    static Bubblegum bubblegum = new Bubblegum(true);

    static HashMap<String, NetworkDescription> descriptions = new HashMap<>();

    public static class NetworkDescription {
        private String id, hash, name, colour, displayName;
        private NetworkDescription(String hash, String id, String name, String displayName, String colour) {
            this.name = name;
            this.id = id;
            this.hash = hash;
            this.colour = colour;
            this.setDisplayName(displayName);
        }
        public String getName() { return this.name; }
        public String getID() { return this.id; }
        public String getHash() { return this.hash; }
        public String getColour() { return this.colour; }
        public String getDisplayName() { return this.displayName; }
        private void setName(String name) { this.name = name; }
        private void setDisplayName(String displayName) {
            this.displayName = displayName;
            bubblegum.getNode(this.getID()).updateMeta("username", displayName);
        }
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

    public static String hashToNodeID(String hash) {
        NetworkDescription nd = getNetworkDescription(hash);
        if(nd == null) return "";
        else return bubblegum.getNode(nd.getID()).getNodeIdentifier().toString();
    }


    // Posts cache, index is postID
    private static HashMap<String, HashMap<Long, TreeSet<String>>> index = new HashMap<>();
    private static HashMap<String, HashMap<String, Post>> cache = new HashMap<>();
    private static HashMap<String, HashMap<String, String>> metaCache = new HashMap<>();
    private static List<String> globalMetaKeys = Stream.of("username").collect(Collectors.toList());

    static Post getCachedPost(String hash, String key) {
        BubblegumNode node = getNodeForHash(hash);
        if(node != null) {
            if(cache.containsKey(node.getIdentifier())) {
                return cache.get(node.getIdentifier()).get(key);
            }
        }
        return null;
    }

    static List<Post> refreshEpoch(BubblegumNode node, long epoch) {
        return resolveIndex(node, NodeID.hash(epoch));
    }

    static List<Post> getComments(BubblegumNode node, String pid) {
        return resolveIndex(node, NodeID.hash("responses_" + pid));
    }

    static Post lookupPost(BubblegumNode node, String dest, String pid) {
        if(node == null) return null;
        try {
            NodeID nid = new NodeID(dest);
            List<Post> posts = node.query(nid, -1, -1, new ArrayList<>() {{ add(pid); }});
            if(posts !=  null) {
                for (Post p : posts) {
                    if (p.getID().equals(pid)) return p;
                }
            }
        } catch (MalformedKeyException e) {
            // Best effort
            return null;
        }
        return null;
    }

    static List<Post> resolveIndex(BubblegumNode node, NodeID indexNode) {
        List<byte[]> idBytes = node.lookup(indexNode);
        List<Post> results = new ArrayList<>();

        if(!cache.containsKey(node.getIdentifier())) cache.put(node.getIdentifier(), new HashMap<>());
        HashMap<String, Post> nodeCache = cache.get(node.getIdentifier());

//        if(!index.containsKey(node.getIdentifier())) index.put(node.getIdentifier(), new HashMap<>());
//        HashMap<Long, TreeSet<String>> nodeIndex = index.get(node.getIdentifier());

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
                                for(String key : globalMetaKeys) {
//                                    if(!haveMeta(idParts[0], key)) {
                                        add("_"+key+"_"+idParts[0]);
//                                    }
                                }
                            }});
                            if (posts != null && posts.size() > 0) {
                                String prefixedOwner;
                                String[] metaKeyParts;
                                for (Post p : posts) {
                                    if(p.getID().startsWith("_")) {
                                        // meta value
                                        metaKeyParts = p.getID().split("_");
                                        if(metaKeyParts.length == 3) {
                                            prefixedOwner = node.getNetworkIdentifier() + ":" + p.getOwner();
                                            if (!metaCache.containsKey(prefixedOwner))
                                                metaCache.put(prefixedOwner, new HashMap<>());

                                            metaCache.get(prefixedOwner).put(metaKeyParts[1], p.getContent());
                                        }
                                    }
                                    else {
                                        nodeCache.put(p.getOwner() + ":" + p.getID(), p);
                                        results.add(p);
                                    }
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

    static boolean haveMeta(String owner, String key) {
        if(metaCache.containsKey(owner) && metaCache.get(owner).containsKey(key)) return true;
        return false;
    }

    static String getMeta(String owner, String key) {
        if(metaCache.containsKey(owner)) return metaCache.get(owner).get(key);
        else return null;
    }

    private static void cacheUpkeep(String hash) {

    }

    static List<Path> getUploadedResources(String hash) {
        NetworkDescription description = State.getNetworkDescription(hash);
        if(description != null) {
            try (Stream<Path> walk = Files.walk(Paths.get(Configuration.RESOLVER_ASSETS_FOLDER, description.id))) {
                List<Path> result = walk.filter(Files::isRegularFile).collect(Collectors.toList());
                return result;
            } catch (IOException e) {
                return new ArrayList<>();
            }
        }
        return new ArrayList<>();
    }
}
