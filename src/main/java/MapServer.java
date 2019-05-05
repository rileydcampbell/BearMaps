
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.io.OutputStream;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.util.Map;
import java.awt.Color;
/* Maven is used to pull in these dependencies. */
import com.google.gson.Gson;
import java.util.List;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Set;
import static spark.Spark.*;
import java.util.Base64;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * This MapServer class is the entry point for running the JavaSpark web server for the BearMaps
 * application project, receiving API calls, handling the API call processing, and generating
 * requested images and routes.
 * @author Alan Yao
 */
public class MapServer {
    /**
     * The root upper left/lower right longitudes and latitudes represent the bounding box of
     * the root tile, as the images in the img/ folder are scraped.
     * Longitude == x-axis; latitude == y-axis.
     */
    public static final double ROOT_ULLAT = 37.892195547244356, ROOT_ULLON = -122.2998046875,
            ROOT_LRLAT = 37.82280243352756, ROOT_LRLON = -122.2119140625;
    /** Each tile is 256x256 pixels. */
    public static final int TILE_SIZE = 256;
    /** HTTP failed response. */
    private static final int HALT_RESPONSE = 403;
    /** Route stroke information: typically roads are not more than 5px wide. */
    public static final float ROUTE_STROKE_WIDTH_PX = 5.0f;
    /** Route stroke information: Cyan with half transparency. */
    public static final Color ROUTE_STROKE_COLOR = new Color(108, 181, 230, 200);

    /** The tile images are in the IMG_ROOT folder. */
    private static final String IMG_ROOT = "img/";
    private static Vertex start = null;
    private static Vertex end = null;
    private static LinkedList<Long> route;
    private static int prevLevel = 0;

    /**
     * The OSM XML file path. Downloaded from <a href="http://download.bbbike.org/osm/">here</a>
     * using custom region selection.
     **/
    private static final String OSM_DB_PATH = "berkeley.osm";
    /**
     * Each raster request to the server will have the following parameters
     * as keys in the params map accessible by,
     * i.e., params.get("ullat") inside getMapRaster(). <br>
     * ullat -> upper left corner latitude,<br> ullon -> upper left corner longitude, <br>
     * lrlat -> lower right corner latitude,<br> lrlon -> lower right corner longitude <br>
     * w -> user viewport window width in pixels,<br> h -> user viewport height in pixels.
     **/
    private static final String[] REQUIRED_RASTER_REQUEST_PARAMS = {"ullat", "ullon", "lrlat",
        "lrlon", "w", "h"};
    /**
     * Each route request to the server will have the following parameters
     * as keys in the params map.<br>
     * start_lat -> start point latitude,<br> start_lon -> start point longitude,<br>
     * end_lat -> end point latitude, <br>end_lon -> end point longitude.
     **/
    private static final String[] REQUIRED_ROUTE_REQUEST_PARAMS = {"start_lat", "start_lon",
        "end_lat", "end_lon"};
    /* Define any static variables here. Do not define any instance variables of MapServer. */
    private static GraphDB g;

    /**
     * Place any initialization statements that will be run before the server main loop here.
     * Do not place it in the main function. Do not place initialization code anywhere else.
     * This is for testing purposes, and you may fail tests otherwise.
     **/
    public static void initialize() {
        g = new GraphDB(OSM_DB_PATH);
    }

    public static void main(String[] args) {
        initialize();
        staticFileLocation("/page");
        /* Allow for all origin requests (since this is not an authenticated server, we do not
         * care about CSRF).  */
        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Request-Method", "*");
            response.header("Access-Control-Allow-Headers", "*");
        });

        /* Define the raster endpoint for HTTP GET requests. I use anonymous functions to define
         * the request handlers. */
        get("/raster", (req, res) -> {
            HashMap<String, Double> params =
                    getRequestParams(req, REQUIRED_RASTER_REQUEST_PARAMS);
            /* The png image is written to the ByteArrayOutputStream */
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            /* getMapRaster() does almost all the work for this API call */
            Map<String, Object> rasteredImgParams = getMapRaster(params, os);
            /* On an image query success, add the image data to the response */
            if (rasteredImgParams.containsKey("query_success")
                    && (Boolean) rasteredImgParams.get("query_success")) {
                String encodedImage = Base64.getEncoder().encodeToString(os.toByteArray());
                rasteredImgParams.put("b64_encoded_image_data", encodedImage);
            }
            /* Encode response to Json */
            Gson gson = new Gson();
            return gson.toJson(rasteredImgParams);
        });

        /* Define the routing endpoint for HTTP GET requests. */
        get("/route", (req, res) -> {
            HashMap<String, Double> params =
                    getRequestParams(req, REQUIRED_ROUTE_REQUEST_PARAMS);
            route = findAndSetRoute(params);
//            routes = route;
            return !route.isEmpty();
        });

        /* Define the API endpoint for clearing the current route. */
        get("/clear_route", (req, res) -> {
            clearRoute();
            return true;
        });

        /* Define the API endpoint for search */
        get("/search", (req, res) -> {
            Set<String> reqParams = req.queryParams();
            String term = req.queryParams("term");
            Gson gson = new Gson();
            /* Search for actual location data. */
            if (reqParams.contains("full")) {
                List<Map<String, Object>> data = getLocations(term);
                return gson.toJson(data);
            } else {
                /* Search for prefix matching strings. */
                List<String> matches = getLocationsByPrefix(term);
                return gson.toJson(matches);
            }
        });

        /* Define map application redirect */
        get("/", (request, response) -> {
            response.redirect("/map.html", 301);
            return true;
        });
    }

    /**
     * Validate & return a parameter map of the required request parameters.
     * Requires that all input parameters are doubles.
     * @param req HTTP Request
     * @param requiredParams TestParams to validate
     * @return A populated map of input parameter to it's numerical value.
     */
    private static HashMap<String, Double> getRequestParams(
            spark.Request req, String[] requiredParams) {
        Set<String> reqParams = req.queryParams();
        HashMap<String, Double> params = new HashMap<>();
        for (String param : requiredParams) {
            if (!reqParams.contains(param)) {
                halt(HALT_RESPONSE, "Request failed - parameters missing.");
            } else {
                try {
                    params.put(param, Double.parseDouble(req.queryParams(param)));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    halt(HALT_RESPONSE, "Incorrect parameters - provide numbers.");
                }
            }
        }
        return params;
    }


    /**
     * Handles raster API calls, queries for tiles and rasters the full image. <br>
     * <p>
     *     The rastered photo must have the following properties:
     *     <ul>
     *         <li>Has dimensions of at least w by h, where w and h are the user viewport width
     *         and height.</li>
     *         <li>The tiles collected must cover the most longitudinal distance per pixel
     *         possible, while still covering less than or equal to the amount of
     *         longitudinal distance per pixel in the query box for the user viewport size. </li>
     *         <li>Contains all tiles that intersect the query bounding box that fulfill the
     *         above condition.</li>
     *         <li>The tiles must be arranged in-order to reconstruct the full image.</li>
     *         <li>If a current route exists, lines of width ROUTE_STROKE_WIDTH_PX and of color
     *         ROUTE_STROKE_COLOR are drawn between all nodes on the route in the rastered photo.
     *         </li>
     *     </ul>
     *     Additional image about the raster is returned and is to be included in the Json response.
     * </p>
     * @param params Map of the HTTP GET request's query parameters - the query bounding box and
     *               the user viewport width and height.
     * @param os     An OutputStream that the resulting png image should be written to.
     * @return A map of parameters for the Json response as specified:
     * "raster_ul_lon" -> Double, the bounding upper left longitude of the rastered image <br>
     * "raster_ul_lat" -> Double, the bounding upper left latitude of the rastered image <br>
     * "raster_lr_lon" -> Double, the bounding lower right longitude of the rastered image <br>
     * "raster_lr_lat" -> Double, the bounding lower right latitude of the rastered image <br>
     * "raster_width"  -> Integer, the width of the rastered image <br>
     * "raster_height" -> Integer, the height of the rastered image <br>
     * "depth"         -> Integer, the 1-indexed quadtree depth of the nodes of the rastered image.
     * Can also be interpreted as the length of the numbers in the image string. <br>
     * "query_success" -> Boolean, whether an image was successfully rastered. <br>
     * @see #REQUIRED_RASTER_REQUEST_PARAMS
     */
    public static Map<String, Object> getMapRaster(Map<String, Double> params, OutputStream os) {
        HashMap<String, Object> rasteredImageParams = new HashMap<>();
        QuadTree tree = new QuadTree();
        int level = findDepth(params, tree);
        if (level > 7) {
            level = 7;
        }
        if (level < 0) {
            level = 0;
        }
        int x = 0;
        int y = 0;
        boolean first  = true;
        double prevLat = 69696969;
        LinkedList<Node> list = tree.getTilesOnLevel(level, params);
        Collections.sort(list);
        double w = findWidth(list);
        double h = findHeight(list);
        BufferedImage image = new BufferedImage((int) w, (int) h, BufferedImage.TYPE_INT_RGB);
        Graphics graphic = image.getGraphics();
        try {
            for (Node tile : list) {
                if (first) {
                    String imageName = "img/" + tile.fileName + ".png";
                    BufferedImage bi = ImageIO.read(new File(imageName));
                    graphic.drawImage(bi, 0, 0, null);
                    x += bi.getWidth();
                    prevLat = tile.getRootLrlat();
                    first = false;
                } else {
                    String imageName = "img/" + tile.fileName + ".png";
                    BufferedImage bi = ImageIO.read(new File(imageName));
                    if (prevLat != 69696969 && prevLat != tile.getRootLrlat()) {
                        x = 0;
                        y += bi.getHeight();
                    }
                    graphic.drawImage(bi, x, y, null);
                    x += bi.getWidth();
                    prevLat = tile.getRootLrlat();
                }
            }
            if (prevLevel != level) { //fix
                Graphics2D gr = image.createGraphics();
                gr.setStroke(new BasicStroke(MapServer.ROUTE_STROKE_WIDTH_PX,
                        BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); //fix
                gr.setColor(ROUTE_STROKE_COLOR);
                if (route != null && route.size() != 0) {
                    Long firstID = route.getFirst();
                    int lineSeg = 0;
                    for (Long id : route) {
                        if (id != firstID) {
                            int firstX = (int) Math.floor(calculateX(firstID, image,
                                    list.getFirst().getRootUllon(),
                                    list.getLast().getRootLrlon())); //fix
                            int firstY = (int) Math.floor(calculateY(firstID, image,
                                    list.getFirst().getRootUllat(),
                                    list.getLast().getRootLrlat())); //fix
                            int secondX = (int) Math.floor(calculateX(id, image,
                                    list.getFirst().getRootUllon(),
                                    list.getLast().getRootLrlon())); //fix
                            int secondY = (int) Math.floor(calculateY(id, image,
                                    list.getFirst().getRootUllat(),
                                    list.getLast().getRootLrlat())); //fix
                            gr.drawLine(firstX, firstY, secondX, secondY);
                            lineSeg++;
                            firstID = id;
                        }
                    }
                }
            }
            ImageIO.write(image, "png", os);
        } catch (IOException e) { //fix
            System.out.println("Not a valid File!");
        }
        rasterImage(list, rasteredImageParams, image, level);
        return rasteredImageParams;
    }

    public static void rasterImage(LinkedList<Node> list,
                                   HashMap<String, Object> rasteredImageParams,
                                   BufferedImage image, int level) {
        rasteredImageParams.put("raster_ul_lon", list.getFirst().getRootUllon());
        rasteredImageParams.put("raster_ul_lat", list.getFirst().getRootUllat());
        rasteredImageParams.put("raster_lr_lon", list.getLast().getRootLrlon());
        rasteredImageParams.put("raster_lr_lat", list.getLast().getRootLrlat());
        rasteredImageParams.put("raster_width", image.getWidth());
        rasteredImageParams.put("raster_height", image.getHeight());
        rasteredImageParams.put("depth", level);
        rasteredImageParams.put("query_success", true);
    }


    /**
     * Searches for the shortest route satisfying the input request parameters, sets it to be the
     * current route, and returns a <code>LinkedList</code> of the route's node ids for testing
     * purposes. <br>
     * The route should start from the closest node to the start point and end at the closest node
     * to the endpoint. Distance is defined as the euclidean between two points (lon1, lat1) and
     * (lon2, lat2).
     * @param params from the API call described in REQUIRED_ROUTE_REQUEST_PARAMS
     * @return A LinkedList of node ids from the start of the route to the end.
     */
    public static LinkedList<Long> findAndSetRoute(Map<String, Double> params) {
        LinkedList<Long> list;
        start = closestVertex(g.maphandler.getMap(), Double.valueOf(params.get("start_lat")),
                Double.valueOf(params.get("start_lon"))); //fix
        end = closestVertex(g.maphandler.getMap(), params.get("end_lat"), params.get("end_lon"));
        list = g.getRoute(start, end);
        route = list;
        return list;
    }

    public static Vertex getStart() {
        return start;
    }

    public static Vertex getEnd() {
        return end;
    }

    public static Vertex closestVertex(HashMap<Long, Vertex> map, double lat, double lon) {
        Vertex mouseClick = new Vertex(lon, lat, 0000000L); //fix
        List<Long> vertices = new ArrayList<Long>(map.keySet());
        Long closestId = vertices.get(0);
        for (Long id : vertices) {
            if (calculateDistance(map.get(id), mouseClick)
                    < calculateDistance(map.get(closestId), mouseClick)) { //fix
                closestId = id;
            }
        }
        return map.get(closestId);
    }

    public static double calculateDistance(Vertex v1, Vertex v2) {
        double diffLats = Math.abs(v1.lat - v2.lat);
        double diffLon = Math.abs(v1.lon - v2.lon);
        return Math.sqrt(Math.pow(diffLats, 2) + Math.pow(diffLon, 2));
    }

    public static int findDepth(Map<String, Double> params, QuadTree tree) {
        double dist = (params.get("lrlon") - params.get("ullon")) / params.get("w");
        Node tempNode = tree.getRoot();
        boolean deeper = true;
        int level = 0;
        while (deeper && tempNode != null) {
            double tileDist = (tempNode.getRootLrlon() - tempNode.getRootUllon()) / 256;
            if (!(tileDist > dist)) {
                deeper = false;
            } else {
                level++;
                tempNode = tempNode.getChildren()[0];
            }
        }
        return level;
    }

    public static double findWidth(LinkedList<Node> list) {
        double w = Math.round(Math.abs(list.get(0).getRootUllon()
                - list.get(list.size() - 1).getRootLrlon())
                / Math.abs(list.get(0).getRootUllon() - list.get(0).getRootLrlon())) * 256; //fix
        return w;
    }

    public static double findHeight(LinkedList<Node> list) {
        double h = Math.round(Math.abs(list.get(0).getRootUllat()
                - list.get(list.size() - 1).getRootLrlat())
                / Math.abs(list.get(0).getRootUllat() - list.get(0).getRootLrlat())) * 256; //fix
        return h;
    }

    /**
     * Clear the current found route, if it exists.
     */
    public static void clearRoute() {
        if (route != null) {
            route = new LinkedList<>();
            start = null;
            end = null;
        }
    }

    public static double calculateX(Long id, BufferedImage image, double u1, double u2) { //fix
        double overallLon = Math.abs(u1 - u2);
        double lonDifference = -(u1 - g.maphandler.getMap().get(id).lon); //fix
        double ratioLon = lonDifference / overallLon;
        return ratioLon * image.getWidth();
    }

    public static double calculateY(Long id, BufferedImage image, double u1, double u2) { //fix
        double overallLat = Math.abs(u1 - u2);
        double latDifference = -(g.maphandler.getMap().get(id).lat - u1); //fix
        double ratioLat = latDifference / overallLat;
        return ratioLat * image.getHeight();
    }



    /**
     * In linear time, collect all the names of OSM locations that prefix-match the query string.
     * @param prefix Prefix string to be searched for. Could be any case, with our without
     *               punctuation.
     * @return A <code>List</code> of the full names of locations whose cleaned name matches the
     * cleaned <code>prefix</code>.
     */
    public static List<String> getLocationsByPrefix(String prefix) {
        return new LinkedList<>();
    }

    /**
     * Collect all locations that match a cleaned <code>locationName</code>, and return
     * information about each node that matches.
     * @param locationName A full name of a location searched for.
     * @return A list of locations whose cleaned name matches the
     * cleaned <code>locationName</code>, and each location is a map of parameters for the Json
     * response as specified: <br>
     * "lat" -> Number, The latitude of the node. <br>
     * "lon" -> Number, The longitude of the node. <br>
     * "name" -> String, The actual name of the node. <br>
     * "id" -> Number, The id of the node. <br>
     */
    public static List<Map<String, Object>> getLocations(String locationName) {
        return new LinkedList<>();
    }
}
