

/**
 * Created by rileycampbell on 4/13/16.
 */
public class Node implements Comparable<Node> {
    int fileName;
    double ullon;
    double ULLAT;
    double LRLON;
    double LRLAT;
    int level;
    Node[] childs;

    public Node(int name, double un, double uLLAT, double lRLON, double lRLAT, int l) {
        this.fileName = name;
        this.ullon = un;
        this.ULLAT = uLLAT;
        this.LRLON = lRLON;
        this.LRLAT = lRLAT;
        this.level = l;
        this.childs = new Node[4];
        if (level < 7) {
            create();
        }
    }

    public void create() {
        if (fileName == 0) {
            childs[0] = new Node(1, ullon, ULLAT, midpoint(ullon, LRLON),
                    midpoint(LRLAT, ULLAT), level + 1);
            childs[1] = new Node(2, midpoint(ullon, LRLON), ULLAT, LRLON,
                    midpoint(LRLAT, ULLAT), level + 1);
            childs[2] = new Node(3, ullon, midpoint(LRLAT, ULLAT),
                    midpoint(ullon, LRLON), LRLAT, level + 1);
            childs[3] = new Node(4, midpoint(ullon, LRLON),
                    midpoint(LRLAT, ULLAT), LRLON, LRLAT, level + 1);

        } else {
            recurse();
        }
    }

    public void recurse() {
        childs[0] = new Node(fileName * 10 + 1, ullon, ULLAT, midpoint(ullon, LRLON),
                midpoint(LRLAT, ULLAT), level + 1);
        childs[1] = new Node(fileName * 10 + 2, midpoint(ullon, LRLON), ULLAT, LRLON,
                midpoint(LRLAT, ULLAT), level + 1);
        childs[2] = new Node(fileName * 10 + 3, ullon, midpoint(LRLAT, ULLAT),
                midpoint(ullon, LRLON), LRLAT, level + 1);
        childs[3] = new Node(fileName * 10 + 4, midpoint(ullon, LRLON),
                midpoint(LRLAT, ULLAT), LRLON, LRLAT, level + 1);
    }

    public double midpoint(double one, double two) {
        return (one + two) / 2;
    }

    public Node[] getChildren() {
        return childs;
    }

    public Node getChild(int index) {
        return childs[(index % 10) - 1];
    }
    @Override
    public int compareTo(Node comapareNode) {
        if (Double.compare(this.ULLAT, comapareNode.ULLAT) != 0) {
            return Double.compare(this.ULLAT, comapareNode.ULLAT) * -1;
        } else {
            return Double.compare(this.ullon, comapareNode.ullon);
        }
    }


    public double getRootUllon() { //fix
        return ullon;
    }

    public double getRootUllat() {
        return ULLAT;
    } //fix

    public double getRootLrlon() {
        return LRLON;
    } //fix

    public double getRootLrlat() {
        return LRLAT;
    } //fix
}
