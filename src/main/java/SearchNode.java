/**
 * Created by rileycampbell on 4/12/16.
 */
public class SearchNode implements Comparable<SearchNode> { //fix
    SearchNode prev;
    Vertex point;
    double dist;

    public SearchNode(SearchNode prev, Vertex point, double distanceSoFar) {
        this.prev = prev;
        this.point = point;
        this.dist = distanceSoFar;
    }

    public int compareTo(SearchNode compareNode) {
        double thisOverallDist = dist + calculateDistance(this, MapServer.getEnd());
        double otherOverallDist = compareNode.dist
                + calculateDistance(compareNode, MapServer.getEnd());
        if (thisOverallDist < otherOverallDist) {
            return -1;
        }
        if (thisOverallDist > otherOverallDist) {
            return 1;
        }
        return 0;
    }

    public double calculateDistance(SearchNode node1, Vertex node2) {
        double diffLats = Math.abs(node1.point.lat - node2.lat);
        double diffLon = Math.abs(node1.point.lon - node2.lon);
        return Math.sqrt(Math.pow(diffLats, 2) + Math.pow(diffLon, 2));
    }

}
