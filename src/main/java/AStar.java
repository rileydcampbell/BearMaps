import java.util.HashSet;
import java.util.LinkedList;
import java.util.PriorityQueue;

/**
 * Created by rileycampbell on 4/13/16.
 */
public class AStar {
    HashSet<Vertex> marked;
    Vertex s;
    Vertex e;
    public AStar(Vertex start, Vertex end) {
        s = start;
        e = end;

    }
    public LinkedList<Long> calcAStar() {
        marked = new HashSet<>();
        LinkedList<Long> routeIDs = new LinkedList<>();
        PriorityQueue<SearchNode> aStar = new PriorityQueue<>();
        Vertex start = s;
        Vertex end = e;
        SearchNode root = new SearchNode(null, start, 0);
        aStar.add(root);
        SearchNode currentNode = aStar.remove();
        marked.add(currentNode.point);
//        double currentDistance = 0;
        while (currentNode.point != end) {
            for (Vertex neighbors : currentNode.point.connects) {
                if (!marked.contains(neighbors)) {
                    SearchNode newNode = new SearchNode(currentNode, neighbors, currentNode.dist
                            + currentNode.calculateDistance(currentNode, neighbors));
                    aStar.add(newNode);
                }
            }
            currentNode = aStar.remove();
            marked.add(currentNode.point);
//            currentDistance += currentNode.calculateDistance(currentNode, start);
        }

        SearchNode a = currentNode;
        while (a != null) {
            routeIDs.addFirst(a.point.id);
            a = a.prev;
        }
        return routeIDs;
    }
}
