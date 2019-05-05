import org.xml.sax.SAXException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Wraps the parsing functionality of the MapDBHandler as an example.
 * You may choose to add to the functionality of this class if you wish.
 * @author Alan Yao
 */
public class GraphDB {
    /**
     * Example constructor shows how to create and start an XML parser.
     * @param db_path Path to the XML file to be parsed.
     */

    MapDBHandler maphandler;
    HashSet<Vertex> marked;
    public GraphDB(String dbPath) {
        try {
            File inputFile = new File(dbPath);
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            maphandler = new MapDBHandler(this);
            saxParser.parse(inputFile, maphandler);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        clean();
    }

    /**
     * Helper to process strings into their "cleaned" form, ignoring punctuation and capitalization.
     * @param s Input string.
     * @return Cleaned string.
     */
    static String cleanString(String s) {
        return s.replaceAll("[^a-zA-Z ]", "").toLowerCase();
    }

    /**
     *  Remove nodes with no connections from the graph.
     *  While this does not guarantee that any two nodes in the remaining graph are connected,
     *  we can reasonably assume this since typically roads are connected.
     */
    private void clean() {
        List<Long> vertices = new ArrayList<Long>(maphandler.getMap().keySet());
//        System.out.println("size before: " + maphandler.getMap().size());
        for (Long v : vertices) {
            if (maphandler.getMap().get(v).connects.size() == 0) {
                maphandler.getMap().remove(v);
            }
        }
//        System.out.println("size after: " + maphandler.getMap().size());
    }

    public LinkedList<Long> getRoute(Vertex s, Vertex e) {
//        marked = new HashSet<>();
//        LinkedList<Long> routeIDs = new LinkedList<>();
//        PriorityQueue<SearchNode> aStar = new PriorityQueue<>();
//        Vertex start = s;
//        Vertex end = e;
//        SearchNode root = new SearchNode(null, start, 0);
//        aStar.add(root);
//        SearchNode currentNode = aStar.remove();
//        marked.add(currentNode.point);
////        double currentDistance = 0;
//        while (currentNode.point != end) {
//            for (Vertex neighbors : currentNode.point.connects) {
//                if (!marked.contains(neighbors)) {
//                    SearchNode newNode = new SearchNode(currentNode, neighbors, currentNode.dist +
//                    currentNode.calculateDistance(currentNode, neighbors));
//                    aStar.add(newNode);
//                }
//            }
//            currentNode = aStar.remove();
//            marked.add(currentNode.point);
////            currentDistance += currentNode.calculateDistance(currentNode, start);
//        }
//
//        SearchNode a = currentNode;
//        while (a != null) {
//            routeIDs.addFirst(a.point.id);
//            a = a.prev;
//        }
//        return routeIDs;
        AStar calcRoute = new AStar(s, e);
        return calcRoute.calcAStar();

    }
}
