package tp_sig;

import geoexplorer.gui.GeoMainFrame;
import geoexplorer.gui.LineString;
import geoexplorer.gui.MapPanel;
import org.postgis.Geometry;
import org.postgis.PGgeometry;
import org.postgis.Point;

import java.awt.*;
import java.sql.*;

public class Request {


    private static PreparedStatement stmt;
    private static Statement st;
    private static ResultSet res;

    /**
     * Question 8
     * Recuperer les resultats d'une requete simple
     * @param tags
     * @param value
     * @throws SQLException
     */
    public static void question8(Connection connection, String tags, String value) throws SQLException {
        stmt = connection.prepareStatement("->'" + tags + "' like '%" +value + "%';");
        res = stmt.executeQuery();

        while (res.next()) {
            System.out.println("colonne 1 = " + res.getString(1));
        }
    }

    /**
     * Qestion 9
     * Affichant tous les noms et coordonnées géographiques des points dont le nom ressemble à (au sens du LIKE SQL) l'argument
     * @param connection
     * @param name
     */
    public static void question9(Connection connection, String name) throws SQLException {
        getAllNamesAndCoord(connection, name);
    }

    /**
     * Qestion 10a
     * L'ensemble des routes autour de Grenoble
     * @param connection
     */
    public static void question10a(Connection connection) throws SQLException{
        getGrenobleRoutes(connection);
    }

    /**
     * Qestion 10b
     * @param connection
     * @throws SQLException
     */
    public static void question10b(Connection connection) throws SQLException{
        getGrenobleBuilding(connection);
    }

    /**
     * Question 11a : Les quartiers de grenoble
     * @param connection
     */
    public static void question11a(Connection connection){
        try {
            getGrenobleSchool(connection);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    /**
     * Recuperer les noms et coordonnées d'un nom particulier - Méthode 1
     * @param name
     * @throws SQLException
     */
    public void getAllNamesAndCoordBis(Connection connection, String name) throws SQLException{
        stmt = connection.prepareStatement("SELECT ST_X(geom), ST_Y(geom) FROM nodes where tags->'name' like '"+ name + "';");
        res = stmt.executeQuery();

        while (res.next()) {
            System.out.print("Longitude = " + res.getString(1));
            System.out.println("Latitude = " + res.getString(2));
        }
    }

    /**
     * Recuperer les noms et coordonnées d'un nom particulier - Méthode 2
     * @param name
     * @throws SQLException
     */
    public static void getAllNamesAndCoord(Connection connection, String name) throws SQLException {
        stmt = connection.prepareStatement("SELECT geom,tags->'name' as name FROM nodes where tags->'name' like '"+ name + "';");
        res = stmt.executeQuery();

        Geometry g;
        Point p;

        while (res.next()) {
            g = ((PGgeometry) res.getObject(1)).getGeometry();
            p = g.getPoint(0);

            System.out.print("Nom = " + res.getString("name"));
            System.out.print("  Longitude = " + p.getX());
            System.out.println("    Latitude = " + p.getY());
        }
    }

    /**
     * Recuperer les routes de Grenoble
     * @param connection
     * @return
     * @throws SQLException
     */
    public static void getGrenobleRoutes(Connection connection) throws SQLException {

        MapPanel  map = new MapPanel(4.75,44.01,0.1);
        GeoMainFrame geo = new GeoMainFrame("Map", map);
        st = connection.createStatement();
        //prepareStatement("SELECT linestring, tags->'highway' as highway FROM ways WHERE tags?'highway' LIMIT 1;");
        //result limited to 3 right now
        res = st.executeQuery("SELECT linestring, tags->'highway' as highway FROM ways WHERE tags?'highway' AND ST_Intersects(ways.bbox,ST_SetSRID(ST_MakeBox2D(ST_Point(5.7,45.1),ST_Point(5.8,45.2)),4326));");

        while (res.next()) {
            Geometry g = ((PGgeometry) res.getObject(1)).getGeometry();
            Point p = null;
            geoexplorer.gui.Point drawedPoint = null;
            LineString drawedLineString = new LineString();

            for (int i = 0; i < g.numPoints(); i++){
                p=g.getPoint(i);
                drawedPoint = new geoexplorer.gui.Point(p.getX(),p.getY(), Color.blue);
                drawedLineString.addPoint(drawedPoint);
            }
            map.addPrimitive(drawedLineString);
        }
        map.autoAdjust();
    }

    /**
     * Recuperer les batiments de Grenoble
     * @param connection
     * @throws SQLException
     */
    public static void getGrenobleBuilding(Connection connection) throws SQLException {
        MapPanel map = new MapPanel(4.75,44.01,0.1);
        GeoMainFrame geo = new GeoMainFrame("Map", map);

        st = connection.createStatement();
        //prepareStatement("SELECT linestring, tags->'highway' as highway FROM ways WHERE tags?'highway' LIMIT 1;");
        //result limited to 3 right now
        res = st.executeQuery("SELECT linestring, tags?'building' as building FROM ways WHERE tags?'building' AND ST_Intersects(ways.bbox,ST_SetSRID(ST_MakeBox2D(ST_Point(5.7,45.1),ST_Point(5.8,45.2)),4326));");

        while (res.next()) {
            Geometry g = ((PGgeometry) res.getObject(1)).getGeometry();
            Point p = null;
            geoexplorer.gui.Point drawedPoint = null;
            geoexplorer.gui.Polygon drawedPolygon = new geoexplorer.gui.Polygon();

            for (int i = 0; i < g.numPoints(); i++){
                p=g.getPoint(i);
                drawedPoint = new geoexplorer.gui.Point(p.getX(),p.getY(), Color.blue);
                drawedPolygon.addPoint(drawedPoint);
            }
            map.addPrimitive(drawedPolygon);
        }
        map.autoAdjust();
    }

    /**
     * Permet de faire la variation de couleur
     * @param i
     * @return
     */
    public static Color getColorAlea(int i){
        int red = (i*50)%255;
        int green =( i*10) %255;
        int blue = ( i*100) %255;
        Color color = new Color(red,green,blue);
       return color;
    }
    public static void getGrenobleSchool(Connection connection) throws SQLException {

        MapPanel  map = new MapPanel(4.75,44.01,0.1);
        GeoMainFrame geo = new GeoMainFrame("Map", map);
        int color=1;
        st = connection.createStatement();
       String requete = " SELECT   quartier.the_geom as geom, count(ways.tags->'name') as NbrPharma, quartier.quartier as quartier  " +
                "FROM ways, quartier WHERE ways.tags->'amenity' like '%school%'" +
                " and ST_Intersects(ST_Transform(quartier.the_geom,4326)::geometry, ways.linestring) GROUP BY quartier,quartier.the_geom " ;


        res = st.executeQuery(requete);

        while (res.next()) {
            Geometry g = ((PGgeometry) res.getObject(1)).getGeometry();
            Point p = null;
            geoexplorer.gui.Point drawedPoint = null;
            geoexplorer.gui.Polygon drawedPolygon = new geoexplorer.gui.Polygon(getColorAlea(color),getColorAlea(color));
            color++;
            for (int i = 0; i < g.numPoints(); i++){
                p=g.getPoint(i);
                drawedPoint = new geoexplorer.gui.Point(p.getX(),p.getY(), Color.blue);
                drawedPolygon.addPoint(drawedPoint);
            }
            map.addPrimitive(drawedPolygon);
        }
        map.autoAdjust();

       /* String requetebis = "  SELECT  linestring  " +
                "        FROM ways WHERE  ways.tags->'amenity' like '%school%' ;";
        ResultSet result  ;
        PreparedStatement state = connection.prepareStatement(requetebis);
        result = state.executeQuery();

        while (result.next() ) {
            Geometry g = ((PGgeometry) result.getObject(1)).getGeometry();
            Point p = null;
            geoexplorer.gui.Point drawedPoint = null;
            geoexplorer.gui.LineString drawedLine = new geoexplorer.gui.LineString();
            for (int i = 0; i < g.numPoints(); i++) {
                p = g.getPoint(i);
                drawedPoint = new geoexplorer.gui.Point(p.getX(), p.getY(), Color.blue);
                drawedLine.addPoint(drawedPoint);
                //Print all the points
                System.out.println("\t\tLongitude = " + p.getX());
                System.out.println("\t\tLatitude = " + p.getY());

            }
            map.addPrimitive(drawedLine);

        }
        map.autoAdjust();*/
    }

}