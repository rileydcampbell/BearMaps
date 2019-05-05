


import java.util.LinkedList;
import java.util.Map;

/**
 * Created by rileycampbell on 4/8/16.
 */
public class QuadTree {

    private Node root; //fix
    private double ullon = MapServer.ROOT_ULLON; //fix
    private double ullat = MapServer.ROOT_ULLAT; //fix
    private double lrlon = MapServer.ROOT_LRLON; //fix
    private double lrlat = MapServer.ROOT_LRLAT; //fix
    LinkedList<Integer> digits;

    public QuadTree() {
        root = new Node(0, ullon, ullat, lrlon, lrlat, 0);
    }

    public Node getRoot() {
        return root;
    }

    public LinkedList<Node> getTilesOnLevel(int level, Map<String, Double> params) {
        LinkedList<Node> tilesOnLevel = new LinkedList<>();
        helper(root, level, tilesOnLevel, params);
        return tilesOnLevel;
    }

    public void helper(Node parent, int level, LinkedList<Node> tiles, Map<String, Double> params) {
        if (level == 0) {
            if (containedTiles(parent, params.get("ullat"), params.get("ullon"),
                    params.get("lrlat"), params.get("lrlon"))) { //fix
                tiles.add(parent);
            }
            return;
        }
        for (Node child : parent.getChildren()) {
            helper(child, level - 1, tiles, params);
        }
    }

    public boolean containedTiles(Node tile, double ullat1,
                                  double ullon2, double lrlat3, double lrlon4) { //fix
        if (Double.compare(tile.getRootUllat(), lrlat3) >= 0
                && Double.compare(tile.getRootLrlat(), ullat1) <= 0) { //fix
            if (Double.compare(tile.getRootUllon(), lrlon4) <= 0
                    && Double.compare(tile.getRootLrlon(), ullon2) >= 0) { //fix
                return true;
            }
        }
        return false;
    }




    @Override
    public String toString() {
        return "QuadTree{"  //fix
                + "root=" + root  //fix
                + ", ULLon=" + ullon  //fix
                + ", ULLat=" + ullat  //fix
                + ", LRLon=" + lrlon  //fix
                + ", LRLat=" + lrlat  //fix
                + ", digits=" + digits + '}';
    }

}
