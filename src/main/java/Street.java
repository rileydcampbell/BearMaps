import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by rileycampbell on 4/11/16.
 */
public class Street {
    ArrayList<Vertex> connections;
    HashMap<String, String> map;
    String streetType;

    public Street() {
        connections = new ArrayList<>();
        map = new HashMap<>();
        streetType = "";
    }



}
