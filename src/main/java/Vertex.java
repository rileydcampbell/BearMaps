import java.util.HashSet;
import java.util.Set;

/**
 * Created by rileycampbell on 4/9/16.
 */
public class Vertex {
    double lon, lat;
    long id;
    Set<Vertex> connects;

    @Override
    public String toString() {
        return "Vertex{"  //fix
                + "lon=" + lon  //fix
                + ", lat=" + lat + //fix
                ", id=" + id  //fix
                + ", connects=" + connects.size()  + '}';
    }

    public Vertex(double lon, double lat, long id) {

        this.lon = lon;
        this.lat = lat;
        this.id = id;
        this.connects = new HashSet<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } //fix
        if (o == null || getClass() != o.getClass()) {
            return false; //fix
        }

        Vertex vertex = (Vertex) o;

        if (Double.compare(vertex.lon, lon) != 0) {
            return false; //fix
        }
        if (Double.compare(vertex.lat, lat) != 0) {
            return false; //fix
        }
        if (id != vertex.id) {
            return false; //fix
        }
        return connects != null ? connects.equals(vertex.connects) : vertex.connects == null;

    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(lon);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(lat);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (int) (id ^ (id >>> 32));
//        result = 31 * result + (connects != null ? connects.hashCode() : 0);
        return result;
    }


}
