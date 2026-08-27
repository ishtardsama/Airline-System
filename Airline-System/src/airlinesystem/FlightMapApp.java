package airlinesystem;

import javafx.animation.*;
import javafx.application.Application;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.Circle;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.*;

public class FlightMapApp extends Application {

    private static final double CW = 960, CH = 640, R = 19;

    private static final Color BG       = Color.web("#1e3248");
    private static final Color GRID     = Color.web("#27405a");
    private static final Color P_COL    = Color.web("#5DADE2");
    private static final Color S_COL    = Color.web("#F39C12");
    private static final Color W_COL    = Color.web("#52BE80");
    private static final Color NEW_COL  = Color.web("#F06292");
    private static final Color SEL_COL  = Color.web("#FF5252");
    private static final Color CUR_COL  = Color.web("#FF9800");
    private static final Color VIS_COL  = Color.web("#CE93D8");
    private static final Color PATH_COL = Color.web("#FFE000");
    private static final Color EDGE_C   = Color.web("#4a86b8");
    private static final Color TEXT_PRI = Color.web("#e8f4fc");
    private static final Color TEXT_SEC = Color.web("#90b4ce");
    private static final Color ACC_BLUE = Color.web("#5ab3f0");

    private FlightGraph graph;
    private Canvas canvas;
    private GraphicsContext gc;

    private final Map<String, double[]> pos         = new LinkedHashMap<>();
    private final Set<String>           newAirports = new HashSet<>();

    private String           selectedCode = null;
    private String           currentNode  = null;
    private final Set<String> visitedNodes = new LinkedHashSet<>();
    private final Set<String> pathNodeSet  = new HashSet<>();
    private final Set<String> pathEdgeSet  = new HashSet<>();
    private Timeline         animTimeline;

    private boolean placingMode    = false;
    private Airport pendingAirport = null;

    private Label    lblCode, lblName, lblCity, lblRegion, lblRoutes;
    private TextArea logArea;
    private ComboBox<String> cboFrom, cboDest;
    private TextField        tfNewCode, tfNewName, tfNewCity;
    private ComboBox<String> cboNewRegion;
    private Label            lblPlaceHint;

    @Override
    public void start(Stage stage) {
        graph = new FlightGraph();
        loadSampleData();
        definePositions();

        canvas = new Canvas(CW, CH);
        gc     = canvas.getGraphicsContext2D();
        canvas.setOnMouseClicked(e -> onCanvasClick(e.getX(), e.getY()));

        HBox root = new HBox();
        root.setStyle("-fx-background-color: #16293f;");

        ScrollPane sideScroll = new ScrollPane(buildSidePanel());
        sideScroll.setFitToWidth(true);
        sideScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sideScroll.setStyle("-fx-background: #16293f; -fx-background-color: #16293f; -fx-border-color: #2a4060;");
        sideScroll.setPrefWidth(295);

        root.getChildren().addAll(canvas, sideScroll);

        stage.setTitle("✈  Malaysia Airline Flight Network  –  Graph Visualizer");
        stage.setScene(new Scene(root, CW + 295, CH));
        stage.setResizable(false);
        stage.show();

        redraw();
    }

    private void redraw() {
        drawBackground();
        drawEdges();
        drawNodes();
        if (placingMode) drawPlacingHint();
    }

    private void drawBackground() {
        gc.setFill(BG);
        gc.fillRect(0, 0, CW, CH);

        gc.setStroke(GRID);
        gc.setLineWidth(0.6);
        for (double x = 0; x <= CW; x += 50) gc.strokeLine(x, 0, x, CH);
        for (double y = 0; y <= CH; y += 50) gc.strokeLine(0, y, CW, y);

        gc.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        gc.setFill(Color.web("#3a6a9a", 0.7));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("PENINSULAR MALAYSIA", 140, 560);
        gc.fillText("SARAWAK", 610, 600);
        gc.fillText("SABAH", 820, 50);

        gc.setFill(Color.web("#12233a", 0.85));
        gc.fillRect(0, 0, CW, 32);
        gc.setFill(ACC_BLUE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        gc.setTextAlign(TextAlignment.LEFT);
        gc.fillText("✈  Malaysia Domestic Flight Network", 14, 21);
        gc.setFill(TEXT_SEC);
        gc.setFont(Font.font("Arial", 11));
        gc.setTextAlign(TextAlignment.RIGHT);
        gc.fillText(graph.totalAirports() + " Airports  |  " + graph.totalFlights() + " Routes", CW - 14, 21);
    }

    private void drawEdges() {
        for (Map.Entry<String, LinkedList<Flight>> entry : graph.getAdjList().entrySet()) {
            String   src = entry.getKey();
            double[] sp  = pos.get(src);
            if (sp == null) continue;

            for (Flight f : entry.getValue()) {
                String   dst = f.getDestination().getCode();
                double[] dp  = pos.get(dst);
                if (dp == null) continue;

                boolean isPath = pathEdgeSet.contains(src + "_" + dst);
                Color   col    = isPath ? PATH_COL : EDGE_C;
                double  lw     = isPath ? 3.2 : 1.3;
                double  alpha  = isPath ? 1.0 : 0.5;

                gc.setStroke(col.deriveColor(0, 1, 1, alpha));
                gc.setLineWidth(lw);
                gc.strokeLine(sp[0], sp[1], dp[0], dp[1]);
                drawArrow(sp[0], sp[1], dp[0], dp[1], col.deriveColor(0, 1, 1, alpha));
            }
        }
    }

    private void drawArrow(double x1, double y1, double x2, double y2, Color c) {
        double angle = Math.atan2(y2 - y1, x2 - x1);
        double al    = 10, aa = Math.PI / 7;
        double ex    = x2 - (R + 3) * Math.cos(angle);
        double ey    = y2 - (R + 3) * Math.sin(angle);
        gc.setStroke(c);
        gc.setLineWidth(1.5);
        gc.strokeLine(ex, ey, ex - al * Math.cos(angle - aa), ey - al * Math.sin(angle - aa));
        gc.strokeLine(ex, ey, ex - al * Math.cos(angle + aa), ey - al * Math.sin(angle + aa));
    }

    private void drawNodes() {
        for (Map.Entry<String, double[]> entry : pos.entrySet()) {
            String  code = entry.getKey();
            double  x    = entry.getValue()[0];
            double  y    = entry.getValue()[1];
            Airport a    = graph.getAirport(code);
            if (a == null) continue;

            Color fill;
            if      (code.equals(selectedCode))  fill = SEL_COL;
            else if (code.equals(currentNode))    fill = CUR_COL;
            else if (pathNodeSet.contains(code))  fill = PATH_COL;
            else if (visitedNodes.contains(code)) fill = VIS_COL;
            else if (newAirports.contains(code))  fill = NEW_COL;
            else                                  fill = regionColor(a.getRegion());

            gc.setFill(fill.deriveColor(0, 1, 1, 0.22));
            gc.fillOval(x - R - 7, y - R - 7, (R + 7) * 2, (R + 7) * 2);

            gc.setFill(fill);
            gc.fillOval(x - R, y - R, R * 2, R * 2);

            gc.setStroke(Color.WHITE.deriveColor(0, 1, 1, 0.7));
            gc.setLineWidth(1.8);
            gc.strokeOval(x - R, y - R, R * 2, R * 2);

            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 11));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText(code, x, y + 4);

            gc.setFont(Font.font("Arial", 9));
            gc.setFill(TEXT_PRI.deriveColor(0, 1, 1, 0.85));
            gc.fillText(a.getCity().split(",")[0], x, y + R + 13);
        }
    }

    private void drawPlacingHint() {
        gc.setFill(Color.web("#FFE000", 0.15));
        gc.fillRect(0, 0, CW, CH);
        gc.setFill(Color.web("#FFE000"));
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("📍  Click anywhere on the map to place  ["
                + (pendingAirport != null ? pendingAirport.getCode() : "") + "]", CW / 2, CH / 2);
    }

    private Color regionColor(String region) {
        if (region != null && region.contains("Sabah"))   return S_COL;
        if (region != null && region.contains("Sarawak")) return W_COL;
        return P_COL;
    }

    private void onCanvasClick(double mx, double my) {
        if (placingMode && pendingAirport != null) {
            String code = pendingAirport.getCode();
            pos.put(code, new double[]{mx, my});
            graph.addAirport(pendingAirport);
            newAirports.add(code);
            cboFrom.getItems().add(code);
            cboDest.getItems().add(code);
            placingMode = false;
            canvas.setCursor(Cursor.DEFAULT);
            lblPlaceHint.setText("");
            log("[OK] Airport [" + code + "] placed on map!");
            selectedCode = code;
            updateInfoPanel(code);
            redraw();
            return;
        }

        String hit  = null;
        double best = Double.MAX_VALUE;
        for (Map.Entry<String, double[]> e : pos.entrySet()) {
            double dx = mx - e.getValue()[0], dy = my - e.getValue()[1];
            double d  = Math.sqrt(dx * dx + dy * dy);
            if (d <= R + 6 && d < best) { best = d; hit = e.getKey(); }
        }
        if (hit != null) {
            selectedCode = hit;
            updateInfoPanel(hit);
            redraw();
        }
    }

    private void updateInfoPanel(String code) {
        Airport a = graph.getAirport(code);
        if (a == null) return;
        lblCode.setText(code);
        lblName.setText(a.getName().isEmpty() ? "—" : a.getName());
        lblCity.setText(a.getCity());
        lblRegion.setText(a.getRegion());
        int out = graph.getNeighbors(code).size();
        lblRoutes.setText(out + " outgoing route" + (out == 1 ? "" : "s"));
        cboFrom.setValue(code);
    }

    private void runBFS() {
        String start = cboFrom.getValue();
        if (start == null) { log("[!] Select a start airport (From)."); return; }
        clearHighlights();
        log("── BFS from [" + start + "] ──");

        List<String>  order = new ArrayList<>();
        Set<String>   seen  = new LinkedHashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.offer(start);
        seen.add(start);
        while (!queue.isEmpty()) {
            String cur = queue.poll();
            order.add(cur);
            for (Flight f : graph.getNeighbors(cur)) {
                String nb = f.getDestination().getCode();
                if (!seen.contains(nb)) { seen.add(nb); queue.offer(nb); }
            }
        }
        animateTraversal(order, "BFS");
    }

    private void runDFS() {
        String start = cboFrom.getValue();
        if (start == null) { log("[!] Select a start airport (From)."); return; }
        clearHighlights();
        log("── DFS from [" + start + "] ──");

        List<String>  order = new ArrayList<>();
        Set<String>   seen  = new LinkedHashSet<>();
        Stack<String> stack = new Stack<>();
        stack.push(start);
        while (!stack.isEmpty()) {
            String cur = stack.pop();
            if (seen.contains(cur)) continue;
            seen.add(cur);
            order.add(cur);
            List<Flight> nb = graph.getNeighbors(cur);
            for (int i = nb.size() - 1; i >= 0; i--) {
                String n = nb.get(i).getDestination().getCode();
                if (!seen.contains(n)) stack.push(n);
            }
        }
        animateTraversal(order, "DFS");
    }

    private void animateTraversal(List<String> order, String label) {
        if (animTimeline != null) animTimeline.stop();
        visitedNodes.clear();
        currentNode = null;
        pathEdgeSet.clear();
        pathNodeSet.clear();

        final int[] step = {0};
        animTimeline = new Timeline(new KeyFrame(Duration.millis(480), e -> {
            if (step[0] < order.size()) {
                if (currentNode != null) visitedNodes.add(currentNode);
                currentNode = order.get(step[0]);
                Airport a = graph.getAirport(currentNode);
                log("Step " + (step[0] + 1) + "  [" + currentNode + "]  " + (a != null ? a.getCity() : ""));
                step[0]++;
            } else {
                if (currentNode != null) visitedNodes.add(currentNode);
                currentNode = null;
                log(label + " complete – " + order.size() + " airports visited.");
                animTimeline.stop();
            }
            redraw();
        }));
        animTimeline.setCycleCount(order.size() + 1);
        animTimeline.play();
    }

    private void runDijkstra(String mode) {
        String src  = cboFrom.getValue();
        String dest = cboDest.getValue();
        if (src == null || dest == null) { log("[!] Select From and To airports."); return; }
        if (src.equals(dest))            { log("[!] From and To are the same."); return; }
        clearHighlights();

        Map<String, Double> cost    = new HashMap<>();
        Map<String, String> prev    = new HashMap<>();
        Set<String>         settled = new HashSet<>();
        for (String c : graph.getAirports().keySet()) cost.put(c, Double.MAX_VALUE);
        cost.put(src, 0.0);

        PriorityQueue<String> pq = new PriorityQueue<>(
                Comparator.comparingDouble(c -> cost.getOrDefault(c, Double.MAX_VALUE)));
        pq.offer(src);

        while (!pq.isEmpty()) {
            String cur = pq.poll();
            if (settled.contains(cur)) continue;
            settled.add(cur);
            if (cur.equals(dest)) break;
            for (Flight f : graph.getNeighbors(cur)) {
                String nb = f.getDestination().getCode();
                if (settled.contains(nb)) continue;
                double w  = mode.equals("dist") ? f.getDistance() : f.getPrice();
                double nc = cost.get(cur) + w;
                if (nc < cost.get(nb)) { cost.put(nb, nc); prev.put(nb, cur); pq.offer(nb); }
            }
        }

        if (cost.get(dest) == Double.MAX_VALUE) {
            log("[!] No path from [" + src + "] to [" + dest + "]."); return;
        }

        LinkedList<String> path = new LinkedList<>();
        for (String c = dest; c != null; c = prev.get(c)) path.addFirst(c);
        for (String n : path) pathNodeSet.add(n);
        for (int i = 0; i < path.size() - 1; i++)
            pathEdgeSet.add(path.get(i) + "_" + path.get(i + 1));

        String unit  = mode.equals("dist") ? "km" : "RM";
        String label = mode.equals("dist") ? "Shortest Distance" : "Cheapest Price";
        log("── " + label + " ──");
        log("Path : " + String.join(" -> ", path));
        log("Total: " + String.format("%.0f %s", cost.get(dest), unit));
        redraw();
    }

    private VBox buildSidePanel() {
        VBox panel = new VBox(9);
        panel.setPadding(new Insets(14));
        panel.setStyle("-fx-background-color: #16293f;");
        panel.setPrefWidth(280);

        Label title = lbl("✈  Flight Network", 15, FontWeight.BOLD, "#5ab3f0");
        Label sub   = lbl("Malaysia Domestic  |  DSA Assignment", 10, FontWeight.NORMAL, "#5a8ab0");

        lblCode   = lbl("—", 22, FontWeight.BOLD, "#5DADE2");
        lblName   = lbl("Click any airport on the map", 10, FontWeight.NORMAL, "#90b4ce");
        lblCity   = lbl("", 10, FontWeight.NORMAL, "#90b4ce");
        lblRegion = lbl("", 10, FontWeight.NORMAL, "#90b4ce");
        lblRoutes = lbl("", 10, FontWeight.NORMAL, "#90b4ce");
        lblName.setWrapText(true);
        VBox infoCard = card(lblCode, lblName, lblCity, lblRegion, lblRoutes);

        cboFrom = combo("From / Start Airport");
        cboDest = combo("To / Destination Airport");
        for (String code : graph.getAirports().keySet()) {
            cboFrom.getItems().add(code);
            cboDest.getItems().add(code);
        }
        cboFrom.valueProperty().addListener((obs, old, nv) -> {
            if (nv != null) { selectedCode = nv; updateInfoPanel(nv); redraw(); }
        });

        Button btnBFS   = btn("▶  BFS Traversal",           "#1565C0");
        Button btnDFS   = btn("▶  DFS Traversal",           "#1B5E20");
        Button btnDist  = btn("▶  Shortest Distance (km)",  "#BF360C");
        Button btnPrice = btn("▶  Cheapest Price (RM)",     "#4A148C");
        Button btnClear = btn("✕  Clear Highlights",        "#37474F");
        btnBFS.setOnAction(e   -> runBFS());
        btnDFS.setOnAction(e   -> runDFS());
        btnDist.setOnAction(e  -> runDijkstra("dist"));
        btnPrice.setOnAction(e -> runDijkstra("price"));
        btnClear.setOnAction(e -> { clearHighlights(); log("Cleared."); redraw(); });

        tfNewCode    = field("IATA Code  (e.g. KLG)");
        tfNewName    = field("Airport Name");
        tfNewCity    = field("City, State");
        cboNewRegion = combo("Region");
        cboNewRegion.getItems().addAll("Peninsular Malaysia", "Sabah", "Sarawak");
        lblPlaceHint = lbl("", 10, FontWeight.NORMAL, "#FFE000");

        Button btnAddAirport = btn("📍  Place New Airport on Map", "#00695C");
        btnAddAirport.setOnAction(e -> {
            String code = tfNewCode.getText().trim().toUpperCase();
            if (code.isEmpty())          { log("[!] Enter IATA code first."); return; }
            if (code.length() > 4)       { log("[!] IATA code max 4 characters."); return; }
            if (graph.airportExists(code)) { log("[!] Airport [" + code + "] already exists."); return; }

            String name   = tfNewName.getText().trim();
            String city   = tfNewCity.getText().trim();
            String region = cboNewRegion.getValue() != null ? cboNewRegion.getValue() : "Peninsular Malaysia";
            if (city.isEmpty()) city = code + " City";

            pendingAirport = new Airport(code, name.isEmpty() ? code + " Airport" : name, city, region);
            placingMode    = true;
            canvas.setCursor(Cursor.CROSSHAIR);
            lblPlaceHint.setText("👆 Click on the map to place [" + code + "]");
            log("Click map to place [" + code + "]...");
            redraw();
        });

        VBox addCard = card(
                lbl("IATA Code & Details", 9, FontWeight.BOLD, "#5a8ab0"),
                tfNewCode, tfNewName, tfNewCity, cboNewRegion,
                btnAddAirport, lblPlaceHint
        );

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setPrefHeight(110);
        logArea.setStyle("-fx-control-inner-background: #0f1e2e; -fx-text-fill: #7ecfb0;"
                + " -fx-font-size: 10.5; -fx-font-family: monospace; -fx-border-color: #2a4060;");
        log("System ready. " + graph.totalAirports() + " airports loaded.");

        VBox legend = new VBox(5,
                legendRow(P_COL,    "Peninsular Malaysia"),
                legendRow(S_COL,    "Sabah"),
                legendRow(W_COL,    "Sarawak"),
                legendRow(NEW_COL,  "Newly Added Airport"),
                legendRow(VIS_COL,  "BFS / DFS Visited"),
                legendRow(CUR_COL,  "Currently Visiting"),
                legendRow(PATH_COL, "Shortest Path"),
                legendRow(SEL_COL,  "Selected Airport")
        );

        panel.getChildren().addAll(
                title, sub, sep(),
                secLabel("AIRPORT INFO"),    infoCard, sep(),
                secLabel("SELECT AIRPORTS"), cboFrom, cboDest, sep(),
                secLabel("ALGORITHMS"),      btnBFS, btnDFS, btnDist, btnPrice, btnClear, sep(),
                secLabel("ADD NEW AIRPORT"), addCard, sep(),
                secLabel("LOG"),             logArea, sep(),
                secLabel("LEGEND"),          legend
        );
        return panel;
    }

    private Label lbl(String text, double size, FontWeight w, String hex) {
        Label l = new Label(text);
        l.setFont(Font.font("Arial", w, size));
        l.setTextFill(Color.web(hex));
        return l;
    }

    private Label secLabel(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("Arial", FontWeight.BOLD, 9));
        l.setTextFill(Color.web("#4a7a9b"));
        return l;
    }

    private VBox card(javafx.scene.Node... nodes) {
        VBox v = new VBox(5, nodes);
        v.setPadding(new Insets(9));
        v.setStyle("-fx-background-color: #1e3d5a; -fx-background-radius: 6;");
        return v;
    }

    private Button btn(String text, String hex) {
        Button b = new Button(text);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        b.setStyle("-fx-background-color:" + hex + "; -fx-text-fill:white;"
                + " -fx-background-radius:5; -fx-cursor:hand; -fx-padding:6 10;");
        b.setOnMouseEntered(e -> b.setOpacity(0.82));
        b.setOnMouseExited(e  -> b.setOpacity(1.0));
        return b;
    }

    private ComboBox<String> combo(String prompt) {
        ComboBox<String> cb = new ComboBox<>();
        cb.setPromptText(prompt);
        cb.setMaxWidth(Double.MAX_VALUE);
        cb.setStyle("-fx-background-color:#1e3d5a; -fx-text-fill:#ddeeff; -fx-prompt-text-fill:#5a8ab0;");
        return cb;
    }

    private TextField field(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setStyle("-fx-background-color:#0f2030; -fx-text-fill:#ddeeff;"
                + " -fx-prompt-text-fill:#4a7a9b; -fx-border-color:#2a4060;"
                + " -fx-border-radius:4; -fx-background-radius:4;");
        return tf;
    }

    private Separator sep() {
        Separator s = new Separator();
        s.setStyle("-fx-background-color:#253d54;");
        return s;
    }

    private HBox legendRow(Color color, String text) {
        Circle dot = new Circle(6, color);
        Label  l   = lbl(text, 10, FontWeight.NORMAL, "#90b4ce");
        HBox   row = new HBox(8, dot, l);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void log(String msg) { logArea.appendText(msg + "\n"); }

    private void clearHighlights() {
        if (animTimeline != null) animTimeline.stop();
        visitedNodes.clear();
        currentNode = null;
        pathEdgeSet.clear();
        pathNodeSet.clear();
    }

    private void definePositions() {
        pos.put("KUL", new double[]{122, 457});
        pos.put("PEN", new double[]{ 57, 222});
        pos.put("JHB", new double[]{214, 548});
        pos.put("LGK", new double[]{ 34, 138});
        pos.put("KBR", new double[]{155, 147});
        pos.put("TGG", new double[]{193, 221});
        pos.put("AOR", new double[]{ 63, 147});
        pos.put("IPH", new double[]{ 92, 288});
        pos.put("KCH", new double[]{509, 572});
        pos.put("SBW", new double[]{587, 495});
        pos.put("BTU", new double[]{634, 410});
        pos.put("MYY", new double[]{680, 308});
        pos.put("LMN", new double[]{727, 265});
        pos.put("BKI", new double[]{774, 163});
        pos.put("KUD", new double[]{810,  74});
        pos.put("SDK", new double[]{868, 167});
        pos.put("LDU", new double[]{879, 248});
        pos.put("TWU", new double[]{870, 311});
    }

    private void loadSampleData() {
        addA("KUL", "Kuala Lumpur International Airport", "Sepang, Selangor",           "Peninsular Malaysia");
        addA("PEN", "Penang International Airport",       "Georgetown, Penang",          "Peninsular Malaysia");
        addA("JHB", "Senai International Airport",        "Johor Bahru, Johor",          "Peninsular Malaysia");
        addA("LGK", "Langkawi International Airport",     "Langkawi, Kedah",             "Peninsular Malaysia");
        addA("KBR", "Sultan Ismail Petra Airport",        "Kota Bharu, Kelantan",        "Peninsular Malaysia");
        addA("TGG", "Sultan Mahmud Airport",              "Kuala Terengganu, Terengganu","Peninsular Malaysia");
        addA("AOR", "Sultan Abdul Halim Airport",         "Alor Setar, Kedah",           "Peninsular Malaysia");
        addA("IPH", "Sultan Azlan Shah Airport",          "Ipoh, Perak",                 "Peninsular Malaysia");
        addA("BKI", "Kota Kinabalu International Airport","Kota Kinabalu, Sabah",        "Sabah");
        addA("TWU", "Tawau Airport",                      "Tawau, Sabah",                "Sabah");
        addA("SDK", "Sandakan Airport",                   "Sandakan, Sabah",             "Sabah");
        addA("LDU", "Lahad Datu Airport",                 "Lahad Datu, Sabah",           "Sabah");
        addA("KUD", "Kudat Airport",                      "Kudat, Sabah",                "Sabah");
        addA("KCH", "Kuching International Airport",      "Kuching, Sarawak",            "Sarawak");
        addA("MYY", "Miri Airport",                       "Miri, Sarawak",               "Sarawak");
        addA("BTU", "Bintulu Airport",                    "Bintulu, Sarawak",            "Sarawak");
        addA("SBW", "Sibu Airport",                       "Sibu, Sarawak",               "Sarawak");
        addA("LMN", "Limbang Airport",                    "Limbang, Sarawak",            "Sarawak");

        addF("MH1202","KUL","PEN",322,55,180);   addF("MH1180","KUL","JHB",330,55,160);
        addF("MH1168","KUL","LGK",452,65,200);   addF("MH1134","KUL","KBR",477,70,220);
        addF("MH1152","KUL","TGG",443,65,210);   addF("MH1114","KUL","AOR",450,65,195);
        addF("MH1124","KUL","IPH",197,45,145);   addF("MH2618","KUL","BKI",1597,155,450);
        addF("MH2506","KUL","KCH",1393,140,400); addF("MH2676","KUL","MYY",1741,165,480);
        addF("MH2526","KUL","BTU",1551,150,430); addF("MH2516","KUL","SBW",1452,145,410);
        addF("MH1203","PEN","KUL",322,55,185);   addF("MH1181","JHB","KUL",330,55,165);
        addF("MH1169","LGK","KUL",452,65,205);   addF("MH1135","KBR","KUL",477,70,225);
        addF("MH1153","TGG","KUL",443,65,215);   addF("MH1115","AOR","KUL",450,65,200);
        addF("MH1125","IPH","KUL",197,45,150);   addF("AK6350","PEN","BKI",1856,180,310);
        addF("AK6382","JHB","BKI",1766,175,300); addF("AK6322","JHB","KCH",1578,155,290);
        addF("MH2619","BKI","KUL",1597,155,455); addF("MH3264","BKI","TWU",345,60,180);
        addF("MH3254","BKI","SDK",277,55,160);   addF("MH3256","BKI","LDU",283,55,165);
        addF("MH3246","BKI","KUD",157,40,120);   addF("MH2862","BKI","KCH",1290,135,380);
        addF("MH2840","BKI","MYY",651,90,250);   addF("MH3265","TWU","BKI",345,60,185);
        addF("MH3285","TWU","SDK",185,45,140);   addF("MH3255","SDK","BKI",277,55,165);
        addF("MH3284","SDK","TWU",185,45,140);   addF("MH3257","LDU","BKI",283,55,170);
        addF("MH3247","KUD","BKI",157,40,125);   addF("MH2507","KCH","KUL",1393,140,405);
        addF("MH2863","KCH","BKI",1290,135,385); addF("MH3216","KCH","SBW",247,50,160);
        addF("MH3238","KCH","BTU",389,60,190);   addF("MH3244","KCH","MYY",646,90,255);
        addF("MH2677","MYY","KUL",1741,165,485); addF("MH2841","MYY","BKI",651,90,255);
        addF("MH3242","MYY","BTU",265,55,165);   addF("MH3245","MYY","KCH",646,90,255);
        addF("MH3268","MYY","LMN",190,45,140);   addF("MH3252","MYY","SBW",480,75,220);
        addF("MH2527","BTU","KUL",1551,150,435); addF("MH3243","BTU","MYY",265,55,165);
        addF("MH3233","BTU","SBW",180,45,140);   addF("MH3239","BTU","KCH",389,60,195);
        addF("MH2517","SBW","KUL",1452,145,415); addF("MH3217","SBW","KCH",247,50,165);
        addF("MH3232","SBW","BTU",180,45,140);   addF("MH3253","SBW","MYY",480,75,225);
        addF("MH3269","LMN","MYY",190,45,140);
    }

    private void addA(String c, String n, String ci, String r) {
        graph.addAirport(new Airport(c, n, ci, r));
    }

    private void addF(String code, String s, String d, double km, double min, double rm) {
        Airport sa = graph.getAirport(s), da = graph.getAirport(d);
        if (sa != null && da != null)
            graph.addFlight(new Flight(code, sa, da, km, min, rm));
    }

    public static void main(String[] args) { launch(args); }
}
