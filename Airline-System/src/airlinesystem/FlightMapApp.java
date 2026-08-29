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
    private Slider           speedSlider;
    private Stage            primaryStage;

    // ── Remove Airport ───────────────────────────────────────────
    private ComboBox<String> cboRemoveAirport;

    // ── Manage Routes (edges) ────────────────────────────────────
    private ComboBox<String> cboEdgeFrom, cboEdgeTo;
    private TextField        tfEdgeCode, tfEdgeDist, tfEdgeDur, tfEdgePrice;

    @Override
    public void start(Stage stage) {
        graph = new FlightGraph();
        loadSampleData();
        definePositions();

        canvas = new Canvas(CW, CH);
        gc     = canvas.getGraphicsContext2D();
        canvas.setOnMouseClicked(e -> onCanvasClick(e.getX(), e.getY()));

        primaryStage = stage;

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
        if (start == null) { showError("No Airport Selected", "Please select a starting airport from the \"From\" dropdown before running the scan."); return; }
        clearHighlights();
        log("── Airport Connectivity Scan from [" + start + "] ──");

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
        animateTraversal(order, "Connectivity Scan");
    }

    private void runDFS() {
        String start = cboFrom.getValue();
        if (start == null) { showError("No Airport Selected", "Please select a starting airport from the \"From\" dropdown before running the route discovery."); return; }
        clearHighlights();
        log("── Deep Route Discovery from [" + start + "] ──");

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
        animateTraversal(order, "Route Discovery");
    }

    private void animateTraversal(List<String> order, String label) {
        if (animTimeline != null) animTimeline.stop();
        visitedNodes.clear();
        currentNode = null;
        pathEdgeSet.clear();
        pathNodeSet.clear();

        final int[] step = {0};
        double ms = speedSlider != null ? speedSlider.getValue() : 900;
        animTimeline = new Timeline(new KeyFrame(Duration.millis(ms), e -> {
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
                // Show result card
                Airport startA = graph.getAirport(order.isEmpty() ? "" : order.get(0));
                String startCity = (startA != null) ? startA.getCity() : "";
                showResult(
                    label,
                    order.get(0) + "  " + startCity,
                    String.join(" → ", order),
                    order.size() + " / " + graph.totalAirports() + " airports visited"
                );
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
        if (src == null || dest == null) { showError("Airports Not Selected", "Please select both a \"From\" and a \"To\" airport before calculating the route."); return; }
        if (src.equals(dest))            { showError("Same Airport Selected", "The \"From\" and \"To\" airports are the same.\nPlease choose two different airports."); return; }
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
        log("Total: " + (mode.equals("dist")
                ? String.format("%.0f km", cost.get(dest))
                : String.format("RM %.0f",  cost.get(dest))));

        // Show result card
        Airport srcA  = graph.getAirport(src);
        Airport dstA  = graph.getAirport(dest);
        String  route = String.join(" → ", path);
        String  total = mode.equals("dist")
                ? String.format("%.0f km", cost.get(dest))
                : String.format("RM %.0f",  cost.get(dest));
        showResult(
            label,
            (srcA != null ? srcA.getCity() : src) + "  ➜  " + (dstA != null ? dstA.getCity() : dest),
            route,
            "Total: " + total
        );
        redraw();
    }

    private VBox buildSidePanel() {
        VBox panel = new VBox(0);
        panel.setPadding(new Insets(14));
        panel.setStyle("-fx-background-color: #16293f;");
        panel.setPrefWidth(280);

        Label title = lbl("✈  Flight Network", 15, FontWeight.BOLD, "#5ab3f0");
        Label sub   = lbl("Malaysia Domestic  |  DSA Assignment", 10, FontWeight.NORMAL, "#5a8ab0");

        // ── Airport Info labels ──────────────────────────────────────────
        lblCode   = lbl("—", 22, FontWeight.BOLD, "#5DADE2");
        lblName   = lbl("Click any airport on the map", 10, FontWeight.NORMAL, "#90b4ce");
        lblCity   = lbl("", 10, FontWeight.NORMAL, "#90b4ce");
        lblRegion = lbl("", 10, FontWeight.NORMAL, "#90b4ce");
        lblRoutes = lbl("", 10, FontWeight.NORMAL, "#90b4ce");
        lblName.setWrapText(true);
        VBox infoCard = card(lblCode, lblName, lblCity, lblRegion, lblRoutes);

        // ── Airport selectors ────────────────────────────────────────────
        cboFrom = combo("From / Start Airport");
        cboDest = combo("To / Destination Airport");
        for (String code : graph.getAirports().keySet()) {
            cboFrom.getItems().add(code);
            cboDest.getItems().add(code);
        }
        cboFrom.valueProperty().addListener((obs, old, nv) -> {
            if (nv != null) { selectedCode = nv; updateInfoPanel(nv); redraw(); }
        });

        // ── Algorithm buttons ────────────────────────────────────────────
        Button btnBFS   = btn("▶  Airport Connectivity Scan", "#1565C0");
        Button btnDFS   = btn("▶  Deep Route Discovery",      "#1B5E20");
        Button btnDist  = btn("▶  Shortest Distance (km)",  "#BF360C");
        Button btnPrice = btn("▶  Cheapest Price (RM)",     "#4A148C");
        Button btnClear = btn("✕  Clear Highlights",        "#37474F");
        btnBFS.setOnAction(e   -> runBFS());
        btnDFS.setOnAction(e   -> runDFS());
        btnDist.setOnAction(e  -> runDijkstra("dist"));
        btnPrice.setOnAction(e -> runDijkstra("price"));
        btnClear.setOnAction(e -> { clearHighlights(); log("Cleared."); redraw(); });

        Label speedLabel = lbl("Traversal Speed", 9, FontWeight.BOLD, "#5a8ab0");
        speedSlider = new Slider(200, 2000, 900);
        speedSlider.setShowTickMarks(true);
        speedSlider.setShowTickLabels(false);
        speedSlider.setMajorTickUnit(400);
        speedSlider.setStyle("-fx-control-inner-background: #1e3248;");
        Label speedHint = lbl("◀ Fast                 Slow ▶", 9, FontWeight.NORMAL, "#7a9bb5");

        // ── Add airport fields ───────────────────────────────────────────
        tfNewCode    = field("IATA Code  (e.g. KLG)");
        tfNewName    = field("Airport Name");
        tfNewCity    = field("City, State");
        cboNewRegion = combo("Region");
        cboNewRegion.getItems().addAll("Peninsular Malaysia", "Sabah", "Sarawak");
        lblPlaceHint = lbl("", 10, FontWeight.NORMAL, "#FFE000");
        Button btnAddAirport = btn("📍  Place New Airport on Map", "#00695C");
        btnAddAirport.setOnAction(e -> {
            String code = tfNewCode.getText().trim().toUpperCase();
            if (code.isEmpty())            { log("[!] Enter IATA code first."); return; }
            if (code.length() > 4)         { log("[!] IATA code max 4 characters."); return; }
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

        // ── Compact legend grid ──────────────────────────────────────────
        GridPane legendGrid = new GridPane();
        legendGrid.setHgap(8);
        legendGrid.setVgap(5);
        legendGrid.setPadding(new Insets(8));
        legendGrid.setStyle("-fx-background-color: #1a2e44; -fx-background-radius: 6;");
        Object[][] legendItems = {
            { P_COL,    "Peninsular" }, { S_COL,    "Sabah"       },
            { W_COL,    "Sarawak"    }, { NEW_COL,  "New Airport" },
            { VIS_COL,  "Scanned"    }, { CUR_COL,  "Visiting"    },
            { PATH_COL, "Shortest"   }, { SEL_COL,  "Selected"    }
        };
        for (int i = 0; i < legendItems.length; i++) {
            Color  c   = (Color)  legendItems[i][0];
            String txt = (String) legendItems[i][1];
            javafx.scene.shape.Rectangle sq = new javafx.scene.shape.Rectangle(11, 11, c);
            sq.setArcWidth(3); sq.setArcHeight(3);
            Label  ll = lbl(txt, 9.5, FontWeight.NORMAL, "#a8cce6");
            HBox   hr = new HBox(5, sq, ll);
            hr.setAlignment(Pos.CENTER_LEFT);
            legendGrid.add(hr, i % 2, i / 2);
        }
        ColumnConstraints col1 = new ColumnConstraints(); col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints(); col2.setPercentWidth(50);
        legendGrid.getColumnConstraints().addAll(col1, col2);

        // ── Operation log ────────────────────────────────────────────────
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setPrefHeight(180);
        logArea.setStyle(
            "-fx-control-inner-background: #0a1828;" +
            "-fx-text-fill: #00e5ff;" +
            "-fx-font-size: 12;" +
            "-fx-font-family: 'Courier New', monospace;" +
            "-fx-border-color: #1e5080; -fx-border-width: 1.5;" +
            "-fx-background-radius: 4; -fx-border-radius: 4;"
        );
        log("SYSTEM READY  |  " + graph.totalAirports() + " airports loaded");

        // ── Remove Airport section ───────────────────────────────────────
        cboRemoveAirport = combo("Select Airport to Remove");
        for (String code : graph.getAirports().keySet()) cboRemoveAirport.getItems().add(code);

        Button btnRemoveAirport = btn("🗑  Remove Selected Airport", "#B71C1C");
        btnRemoveAirport.setOnAction(e -> removeSelectedAirport());

        Label removeHint = lbl("⚠ Also removes all routes to/from this airport.",
                9, FontWeight.NORMAL, "#FF8A80");
        removeHint.setWrapText(true);

        VBox removeAirportCard = card(
                lbl("Airport to Remove", 9, FontWeight.BOLD, "#5a8ab0"),
                cboRemoveAirport, removeHint, btnRemoveAirport
        );

        // ── Manage Routes (edges) section ────────────────────────────────
        cboEdgeFrom = combo("From Airport");
        cboEdgeTo   = combo("To Airport");
        for (String code : graph.getAirports().keySet()) {
            cboEdgeFrom.getItems().add(code);
            cboEdgeTo.getItems().add(code);
        }
        tfEdgeCode  = field("Flight Code  (e.g. MH9999)");
        tfEdgeDist  = field("Distance (km)");
        tfEdgeDur   = field("Duration (min)");
        tfEdgePrice = field("Price (RM)");

        Button btnAddRoute    = btn("➕  Add Route",    "#1B5E20");
        Button btnRemoveRoute = btn("➖  Remove Route", "#B71C1C");
        btnAddRoute.setOnAction(e    -> addRoute());
        btnRemoveRoute.setOnAction(e -> removeRoute());

        Label routeHint = lbl("Pick From & To, fill details then Add. To remove, just pick From & To.",
                9, FontWeight.NORMAL, "#7a9bb5");
        routeHint.setWrapText(true);

        HBox routeBtns = new HBox(6, btnAddRoute, btnRemoveRoute);
        routeBtns.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(btnAddRoute,    javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(btnRemoveRoute, javafx.scene.layout.Priority.ALWAYS);
        btnAddRoute.setMaxWidth(Double.MAX_VALUE);
        btnRemoveRoute.setMaxWidth(Double.MAX_VALUE);

        VBox manageRoutesCard = card(
                lbl("Route Endpoints", 9, FontWeight.BOLD, "#5a8ab0"),
                cboEdgeFrom, cboEdgeTo,
                lbl("Route Details (required for Add)", 9, FontWeight.BOLD, "#5a8ab0"),
                tfEdgeCode, tfEdgeDist, tfEdgeDur, tfEdgePrice,
                routeHint, routeBtns
        );

        // ── Assemble accordion sections ──────────────────────────────────
        panel.getChildren().addAll(
                title, sub,
                accordionSection("MAP LEGEND",      true,  legendGrid),
                accordionSection("AIRPORT INFO",    true,  infoCard),
                accordionSection("SELECT AIRPORTS", true,  cboFrom, cboDest),
                accordionSection("ALGORITHMS",      true,  btnBFS, btnDFS, btnDist, btnPrice, btnClear,
                                                           speedLabel, speedSlider, speedHint),
                accordionSection("ADD NEW AIRPORT", false, addCard),
                accordionSection("REMOVE AIRPORT",  false, removeAirportCard),
                accordionSection("MANAGE ROUTES",   false, manageRoutesCard),
                accordionSection("OPERATION LOG",   true,  logArea)
        );
        return panel;
    }

    /**
     * Creates a collapsible accordion section for the sidebar.
     * @param title     Section heading text
     * @param expanded  Whether the section starts expanded
     * @param content   Child nodes to show/hide
     */
    private VBox accordionSection(String title, boolean expanded, javafx.scene.Node... content) {
        // Content wrapper
        VBox body = new VBox(6, content);
        body.setPadding(new Insets(0, 0, 8, 0));
        body.setVisible(expanded);
        body.setManaged(expanded);

        // Toggle arrow label
        Label arrow = new Label(expanded ? "▼" : "▶");
        arrow.setFont(Font.font("Arial", FontWeight.BOLD, 9));
        arrow.setTextFill(Color.web("#5ab3f0"));
        arrow.setMinWidth(14);

        // Section title label
        Label titleLbl = new Label(title);
        titleLbl.setFont(Font.font("Arial", FontWeight.BOLD, 9));
        titleLbl.setTextFill(Color.web("#7ab8d8"));
        HBox.setHgrow(titleLbl, javafx.scene.layout.Priority.ALWAYS);

        // Thin divider line
        Separator divider = new Separator();
        divider.setStyle("-fx-background-color: #253d54;");

        // Clickable header row
        HBox header = new HBox(6, arrow, titleLbl);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(7, 0, 5, 0));
        header.setCursor(Cursor.HAND);
        header.setOnMouseEntered(e -> titleLbl.setTextFill(Color.web("#a8d8f0")));
        header.setOnMouseExited(e  -> titleLbl.setTextFill(Color.web("#7ab8d8")));
        header.setOnMouseClicked(e -> {
            boolean nowVisible = !body.isVisible();
            body.setVisible(nowVisible);
            body.setManaged(nowVisible);
            arrow.setText(nowVisible ? "▼" : "▶");
        });

        VBox section = new VBox(0, divider, header, body);
        section.setPadding(new Insets(0, 0, 2, 0));
        return section;
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
        cb.setStyle("-fx-background-color: #1e3d5a; -fx-border-color: #3b6288; -fx-border-radius: 4; -fx-background-radius: 4; -fx-mark-color: #5ab3f0;");
        cb.setButtonCell(new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(prompt);
                    setTextFill(Color.web("#8bb4d4"));
                    setFont(Font.font("Arial", FontWeight.NORMAL, 12));
                } else {
                    setText(item);
                    setTextFill(Color.web("#FFFFFF"));
                    setFont(Font.font("Arial", FontWeight.BOLD, 12));
                }
                setStyle("-fx-background-color: transparent;");
            }
        });
        cb.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setTextFill(Color.web("#FFFFFF"));
                    setFont(Font.font("Arial", 12));
                }
                setStyle("-fx-background-color: #16293f; -fx-padding: 5 8;");
            }
        });
        return cb;
    }

    private TextField field(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setStyle("-fx-background-color: #16293f; -fx-text-fill: #FFFFFF;"
                + " -fx-prompt-text-fill: #8bb4d4; -fx-border-color: #3b6288;"
                + " -fx-border-radius: 4; -fx-background-radius: 4; -fx-font-size: 11.5px;");
        return tf;
    }

    private Separator sep() {
        Separator s = new Separator();
        s.setStyle("-fx-background-color:#253d54;");
        return s;
    }

    /** Lightweight thin separator used inside result cards. */
    private Separator sep2() {
        Separator s = new Separator();
        s.setStyle("-fx-background-color:#2a4060; -fx-padding: 0;");
        return s;
    }

    /**
     * Populates the RESULT accordion panel and auto-expands it.
     * @param algorithm  e.g. "Connectivity Scan", "Shortest Distance"
     * @param stat       subtitle line (start airport or route cities)
     * @param path       main content — visit order or flight hops
     * @param summary    bottom summary line (count or total cost)
     */
    private void showResult(String algorithm, String stat, String path, String summary) {
        Stage popup = new Stage();
        popup.initOwner(primaryStage);
        popup.initStyle(javafx.stage.StageStyle.UNDECORATED);
        popup.setTitle("Result");

        // ── Header bar ──────────────────────────────────────────────────
        Label icon   = lbl("📋", 18, FontWeight.BOLD,   "#FFD600");
        Label algLbl = lbl(algorithm,  13, FontWeight.BOLD,   "#FFD600");
        Label statLbl= lbl(stat,       10, FontWeight.NORMAL, "#a8cce6");
        statLbl.setWrapText(true);
        HBox.setHgrow(algLbl, javafx.scene.layout.Priority.ALWAYS);

        // Close button
        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #7ab8d8;"
                + " -fx-font-size: 14; -fx-cursor: hand; -fx-padding: 0 4;");
        closeBtn.setOnAction(e -> popup.close());
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle("-fx-background-color: #FF5252;"
                + " -fx-text-fill: white; -fx-font-size: 14; -fx-cursor: hand;"
                + " -fx-background-radius: 4; -fx-padding: 0 4;"));
        closeBtn.setOnMouseExited(e  -> closeBtn.setStyle("-fx-background-color: transparent;"
                + " -fx-text-fill: #7ab8d8; -fx-font-size: 14; -fx-cursor: hand; -fx-padding: 0 4;"));

        HBox headerRow = new HBox(8, icon, algLbl, closeBtn);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.setPadding(new Insets(12, 12, 8, 14));
        headerRow.setStyle("-fx-background-color: #1a2e44;");
        HBox.setHgrow(algLbl, javafx.scene.layout.Priority.ALWAYS);

        // ── Route / path content ─────────────────────────────────────────
        Label statLabel = lbl(stat,  10, FontWeight.NORMAL, "#a8cce6");
        statLabel.setWrapText(true);

        Label pathLabel = new Label(path);
        pathLabel.setFont(Font.font("Courier New", FontWeight.NORMAL, 11));
        pathLabel.setTextFill(Color.web("#a0e0b0"));
        pathLabel.setWrapText(true);
        pathLabel.setMaxWidth(360);
        pathLabel.setStyle("-fx-background-color: #0d1e30; -fx-padding: 10;"
                + " -fx-background-radius: 6;");

        Label summaryLabel = lbl(summary, 12, FontWeight.BOLD, "#00e5ff");

        // ── OK button ────────────────────────────────────────────────────
        Button okBtn = new Button("  OK  ");
        okBtn.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        okBtn.setStyle("-fx-background-color: #1565C0; -fx-text-fill: white;"
                + " -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 6 24;");
        okBtn.setOnAction(e -> popup.close());
        okBtn.setOnMouseEntered(e -> okBtn.setStyle("-fx-background-color: #1976D2;"
                + " -fx-text-fill: white; -fx-background-radius: 6;"
                + " -fx-cursor: hand; -fx-padding: 6 24;"));
        okBtn.setOnMouseExited(e  -> okBtn.setStyle("-fx-background-color: #1565C0;"
                + " -fx-text-fill: white; -fx-background-radius: 6;"
                + " -fx-cursor: hand; -fx-padding: 6 24;"));
        HBox btnRow = new HBox(okBtn);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.setPadding(new Insets(4, 0, 0, 0));

        // ── Separator ────────────────────────────────────────────────────
        Separator div = new Separator();
        div.setStyle("-fx-background-color: #253d54;");

        VBox body = new VBox(10, statLabel, pathLabel, div, summaryLabel, btnRow);
        body.setPadding(new Insets(12, 14, 14, 14));
        body.setStyle("-fx-background-color: #1e3248;");
        body.setMaxWidth(400);

        // ── Outer border ─────────────────────────────────────────────────
        VBox root = new VBox(headerRow, body);
        root.setStyle("-fx-border-color: #1565C0; -fx-border-width: 2;"
                + " -fx-background-color: #1e3248; -fx-effect:"
                + " dropshadow(gaussian, rgba(0,0,0,0.6), 20, 0, 0, 6);");

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        popup.setScene(scene);
        popup.centerOnScreen();
        popup.show();
    }

    private HBox legendRow(Color color, String text) {
        Circle dot = new Circle(6, color);
        Label  l   = lbl(text, 10, FontWeight.NORMAL, "#90b4ce");
        HBox   row = new HBox(8, dot, l);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void log(String msg) {
        java.time.LocalTime now = java.time.LocalTime.now();
        String ts = String.format("%02d:%02d:%02d", now.getHour(), now.getMinute(), now.getSecond());
        logArea.appendText("[" + ts + "]  " + msg + "\n");
    }

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

    // ── Helper: sync all airport combo-boxes ─────────────────────────────
    private void syncAllCombos() {
        List<String> codes = new ArrayList<>(graph.getAirports().keySet());
        for (ComboBox<String> cb : new ComboBox[]{cboFrom, cboDest, cboRemoveAirport, cboEdgeFrom, cboEdgeTo}) {
            String cur = cb.getValue();
            cb.getItems().setAll(codes);
            if (codes.contains(cur)) cb.setValue(cur);
        }
    }

    // ── Remove Airport (vertex) ──────────────────────────────────────────
    private void removeSelectedAirport() {
        String code = cboRemoveAirport.getValue();
        if (code == null) {
            showError("No Airport Selected", "Please choose an airport to remove from the dropdown.");
            return;
        }
        // Confirm with user
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Remove Airport");
        confirm.setHeaderText(null);

        Label headerLbl = new Label("⚠  Remove Airport [" + code + "]?");
        headerLbl.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        headerLbl.setTextFill(Color.web("#FFD600"));
        headerLbl.setPadding(new Insets(10, 14, 10, 14));
        headerLbl.setStyle("-fx-background-color: #1a2e44;");
        confirm.getDialogPane().setHeader(headerLbl);
        confirm.getDialogPane().setStyle("-fx-background-color: #1e3248; -fx-border-color: #FF5252; -fx-border-width: 2;");
        confirm.getDialogPane().setContentText(
                "This will permanently remove [" + code + "] and ALL routes to/from it.");
        confirm.getDialogPane().lookup(".content.label")
               .setStyle("-fx-text-fill: #e0f0ff; -fx-font-size: 13;");

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == javafx.scene.control.ButtonType.OK) {
                boolean ok = graph.removeAirport(code);
                if (ok) {
                    pos.remove(code);
                    newAirports.remove(code);
                    pathNodeSet.remove(code);
                    visitedNodes.remove(code);
                    pathEdgeSet.removeIf(s -> s.startsWith(code + "_") || s.endsWith("_" + code));
                    if (code.equals(selectedCode)) { selectedCode = null; lblCode.setText("—"); lblName.setText("Click any airport on the map"); lblCity.setText(""); lblRegion.setText(""); lblRoutes.setText(""); }
                    if (code.equals(currentNode))  { currentNode = null; }
                    syncAllCombos();
                    log("[OK] Airport [" + code + "] removed from the network.");
                } else {
                    log("[!] Could not remove airport [" + code + "].");
                }
                redraw();
            }
        });
    }

    // ── Add Route (directed edge) ────────────────────────────────────────
    private void addRoute() {
        String from  = cboEdgeFrom.getValue();
        String to    = cboEdgeTo.getValue();
        if (from == null || to == null) {
            showError("Missing Airports", "Please select both a From and a To airport.");
            return;
        }
        if (from.equals(to)) {
            showError("Same Airport", "From and To airports must be different.");
            return;
        }

        String fCode = tfEdgeCode.getText().trim().toUpperCase();
        if (fCode.isEmpty()) fCode = "FL" + from + to;

        double dist, dur, price;
        try {
            dist  = Double.parseDouble(tfEdgeDist.getText().trim());
            dur   = Double.parseDouble(tfEdgeDur.getText().trim());
            price = Double.parseDouble(tfEdgePrice.getText().trim());
        } catch (NumberFormatException ex) {
            showError("Invalid Input", "Distance, Duration and Price must be valid numbers.");
            return;
        }
        if (dist <= 0 || dur <= 0 || price <= 0) {
            showError("Invalid Values", "Distance, Duration and Price must be positive numbers.");
            return;
        }

        Airport src  = graph.getAirport(from);
        Airport dest = graph.getAirport(to);
        if (src == null || dest == null) {
            showError("Airport Not Found", "One or both airports were not found in the graph.");
            return;
        }

        // Check for duplicate edge
        if (graph.getDirectFlight(from, to) != null) {
            showError("Route Exists", "A direct route from [" + from + "] to [" + to + "] already exists.");
            return;
        }

        boolean ok = graph.addFlight(new Flight(fCode, src, dest, dist, dur, price));
        if (ok) {
            log("[OK] Route added: [" + from + "] → [" + to + "]  " + fCode
                    + "  " + (int)dist + "km  RM" + (int)price);
            tfEdgeCode.clear(); tfEdgeDist.clear(); tfEdgeDur.clear(); tfEdgePrice.clear();
            redraw();
        } else {
            log("[!] Failed to add route.");
        }
    }

    // ── Remove Route (directed edge) ─────────────────────────────────────
    private void removeRoute() {
        String from = cboEdgeFrom.getValue();
        String to   = cboEdgeTo.getValue();
        if (from == null || to == null) {
            showError("Missing Airports", "Please select both a From and a To airport.");
            return;
        }
        if (from.equals(to)) {
            showError("Same Airport", "From and To airports must be different.");
            return;
        }

        if (graph.getDirectFlight(from, to) == null) {
            showError("No Direct Route", "There is no direct route from [" + from + "] to [" + to + "].");
            return;
        }

        boolean ok = graph.removeFlight(from, to);
        if (ok) {
            pathEdgeSet.remove(from + "_" + to);
            log("[OK] Route removed: [" + from + "] → [" + to + "]");
            redraw();
        } else {
            log("[!] Failed to remove route.");
        }
    }

    /** Shows a styled error alert dialog to the user. */
    private void showError(String title, String message) {
        log("[!] " + message);
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);   // suppress default header so our custom one shows

        // Custom bright header label — guaranteed to render without CSS lookup
        Label headerLbl = new Label("\u26a0  " + title);
        headerLbl.setFont(Font.font("Arial", FontWeight.BOLD, 15));
        headerLbl.setTextFill(Color.web("#FFD600"));
        headerLbl.setWrapText(true);
        headerLbl.setPadding(new Insets(10, 14, 10, 14));
        headerLbl.setMaxWidth(Double.MAX_VALUE);
        headerLbl.setStyle("-fx-background-color: #1a2e44;");

        alert.getDialogPane().setHeader(headerLbl);
        alert.getDialogPane().setStyle(
            "-fx-background-color: #1e3248;" +
            "-fx-border-color: #FF5252; -fx-border-width: 2;"
        );
        alert.getDialogPane().setContentText(message);
        alert.getDialogPane().lookup(".content.label").setStyle(
            "-fx-text-fill: #e0f0ff; -fx-font-size: 13;"
        );
        alert.showAndWait();
    }

    public static void main(String[] args) { launch(args); }
}
