import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.PaintScale;
import org.jfree.chart.renderer.xy.XYBlockRenderer;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.data.xy.DefaultXYZDataset;
import org.jfree.data.xy.XYZDataset;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicReference;

public class Main {
    private static void createFrame(boolean RUS) {
        EventQueue.invokeLater(() -> {
            JFrame frame = new JFrame(RUS ? "Справка" : "Help");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            frame.add(new JLabel(new ImageIcon(System.getProperty("user.dir")  +  (RUS ? "/77.jpg" : "/66.jpg"))));
            frame.pack();
            frame.setLocationByPlatform(true);
            frame.setVisible(true);
            frame.setResizable(false);
        });
    }
    private double A = 100.0, B = 100.0, time = 100.0, tH = 50.0, tC = 30.0, tFS = 10.0, lambda = 1.0, c = 1.0, ro = 1.0;
    private int Nx = 200, Ny = 200;
    private double hx = A / Nx, hy = B / Ny;
    private boolean RUS = true;
    private DefaultXYZDataset DATA;
    private double [][]f1;
    private double []range;
    private static void setVal(double [][]f, final double val) {
        for (double[] doubles : f) {
            java.util.Arrays.fill(doubles, val);
        }
    }
    private void runThrough1(double [][]f, final int idY, final double tau) {
        double[] alpha = new double[f[idY].length];
        double[] beta = new double[f[idY].length];
        alpha[0] = 0;
        beta[0] = tH;
        final double ai = lambda / (hx * hx), bi = 2 * ai + 2 * ro * c / tau;
        for (int i = 1; i < f[idY].length; i++) {
            final double v = bi - ai * alpha[i - 1], fi = -2 * ro * c * f[idY][i] / tau;
            alpha[i] = ai / v;
            beta[i] = (ai * beta[i - 1] - fi) / v;
        }
        f[idY][f[idY].length - 1] = tC;
        for (int i = f[idY].length - 2; i > 0; i--) {
            f[idY][i] = alpha[i] * f[idY][i + 1] + beta[i];
        }
        f[idY][0] = tH;
    }
    private void runThrough2(double [][]f, final int idX, final double tau) {
        double[] alpha = new double[f.length];
        double[] beta = new double[f.length];
        final double ai = lambda / (hy * hy), bi = 2 * ai + 2 * ro * c / tau, a = lambda / (ro * c);
        alpha[0] = a * tau / (a * tau + hy * hy);
        beta[0] = hy * hy * f[0][idX] / (a * tau + hy * hy);
        for (int i = 1; i < f.length; i++) {
            final double v = bi - ai * alpha[i - 1], fi = -2 * ro * c * f[i][idX]/ tau;
            alpha[i] = ai / v;
            beta[i] = (ai * beta[i - 1] - fi) / v;
        }
        f[f.length - 1][idX] = (a * tau * beta[f.length - 2] + hy * hy * f[f.length - 1][idX]) / (a * tau * (1.0 - alpha[f.length - 2]) + hy * hy);
        for (int i = f.length - 2; i >= 0; i--) {
            f[i][idX] = alpha[i] * f[i + 1] [idX] + beta[i];
        }
    }
    private void getRes(double [][]f,  final double tau) {
        for (double cur_time = 0; cur_time < time; cur_time += tau) {
            for (int j = 0; j < f.length; j++) {
                runThrough1(f, j, tau);
            }
            for (int j = 1; j < f[0].length - 1; j++) {
                runThrough2(f, j, tau);
            }
        }
    }
    private XYZDataset createDataset(final double [][]f) {
        DefaultXYZDataset dataset = new DefaultXYZDataset();
        hx = A / Nx;
        hy = B / Ny;
        final double tau = time / 1000;
        setVal(f, tFS);
        getRes(f, tau);
        double cur1 = hx / 2;
        for (int i = 0; i < Nx; i++) {
            double[][] data = new double[3][Ny];
            double cur2 = hy / 2;
            for (int j = 0; j < Ny; j++) {
                data[0][j] = cur1;
                data[1][j] = cur2;
                data[2][j] = f[j][i];
                cur2 += hy;
            }
            dataset.addSeries("S" + i, data);
            cur1 += hx;
        }
        return dataset;
    }
    private static double[] getRange(final double [][]f) {
        double first = f[0][0], second = f[0][0];
        for (final double[] doubles : f) {
            for (final double cur : doubles) {
                if (cur < first) {
                    first = cur;
                }
                if (cur > second) {
                    second = cur;
                }
            }
        }
        return new double[]{first, second};
    }
    public Main(String title) {
        JFrame f = new JFrame(title);
        f.setLayout(new GridBagLayout());
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f1 = new double[Ny][Nx];
        DATA = (DefaultXYZDataset) createDataset(f1);
        NumberAxis xAxis = new NumberAxis(RUS ? "Ось абсцисс" : "x Axis");
        NumberAxis yAxis = new NumberAxis(RUS ? "Ось ординат" : "y Axis");
        AtomicReference<XYPlot> plot = new AtomicReference<>(new XYPlot(DATA, xAxis, yAxis, null));
        XYBlockRenderer r = new XYBlockRenderer();
        range = getRange(f1);
        AtomicReference<SpectrumPaintScale> ps = new AtomicReference<>(new SpectrumPaintScale(range[0], range[1]));
        r.setPaintScale(ps.get());
        r.setBlockHeight(hy);
        r.setBlockWidth(hx);
        plot.get().setRenderer(r);
        AtomicReference<JFreeChart> chart = new AtomicReference<>(new JFreeChart(RUS ? "Распределение температур" : "Heat distribution", JFreeChart.DEFAULT_TITLE_FONT, plot.get(), false));
        NumberAxis scaleAxis = new NumberAxis(RUS ? "Диапазон" : "Range");
        scaleAxis.setAxisLinePaint(Color.WHITE);
        scaleAxis.setTickMarkPaint(Color.WHITE);
        AtomicReference<PaintScaleLegend> legend = new AtomicReference<>(new PaintScaleLegend(ps.get(), scaleAxis));
        legend.get().setSubdivisionCount(128);
        legend.get().setAxisLocation(AxisLocation.TOP_OR_RIGHT);
        legend.get().setPadding(new RectangleInsets(0, 0.0, 0.0, 0.0));
        legend.get().setStripWidth(20);
        legend.get().setPosition(RectangleEdge.RIGHT);
        legend.get().setBackgroundPaint(Color.WHITE);
        chart.get().addSubtitle(legend.get());
        chart.get().setBackgroundPaint(Color.WHITE);
        ChartPanel chartPanel = new ChartPanel(chart.get()) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(620, 350);
            }
        };
        chartPanel.setMouseZoomable(true, false);

        JButton button3 = new JButton(RUS ? "Справка" : "Help");
        button3.addActionListener(e -> createFrame(RUS));
        button3.setPreferredSize(new Dimension(95, 25));

        JTextField t1 = new JTextField(), t2 = new JTextField(), t3 = new JTextField(), t4 = new JTextField(), t5 = new JTextField(), t6 = new JTextField(),
                   t7 = new JTextField(), t8 = new JTextField(), t9 = new JTextField(), t10 = new JTextField(), t11 = new JTextField();

        JButton button1 = new JButton(RUS ? "Выч." : "Calc.");
        button1.addActionListener(e -> {
            A = Double.parseDouble(t1.getText());
            if (A <= 0) {
                A = 1;
                t1.setText(String.valueOf(A));
            }
            B = Double.parseDouble(t2.getText());
            if (B <= 0) {
                B = 1;
                t2.setText(String.valueOf(B));
            }
            Nx = Integer.parseInt(t3.getText());
            if (Nx < 3) {
                Nx = 3;
                t3.setText(String.valueOf(Nx));
            }
            Ny = Integer.parseInt(t4.getText());
            if (Ny < 2) {
                Ny = 2;
                t4.setText(String.valueOf(Ny));
            }
            tH = Double.parseDouble(t5.getText());
            tC = Double.parseDouble(t6.getText());
            tFS = Double.parseDouble(t7.getText());
            c = Double.parseDouble(t8.getText());
            if (c <= 0) {
                c = 1;
                t8.setText(String.valueOf(c));
            }
            lambda = Double.parseDouble(t9.getText());
            if (lambda <= 0) {
                lambda = 1;
                t9.setText(String.valueOf(lambda));
            }
            ro = Double.parseDouble(t10.getText());
            if (ro <= 0) {
                ro = 1;
                t10.setText(String.valueOf(ro));
            }
            time = Double.parseDouble(t11.getText());
            if (time <= 0) {
                time = 100;
                t11.setText(String.valueOf(time));
            }
            f1 = new double[Ny][Nx];
            DATA = (DefaultXYZDataset) createDataset(f1);
            plot.set(new XYPlot(DATA, xAxis, yAxis, null));
            range = getRange(f1);
            ps.set(new SpectrumPaintScale(range[0], range[1]));
            r.setPaintScale(ps.get());
            r.setBlockHeight(hy);
            r.setBlockWidth(hx);
            plot.get().setRenderer(r);
            chart.set(new JFreeChart(RUS ? "Распределение температур" : "Heat distribution", JFreeChart.DEFAULT_TITLE_FONT, plot.get(), false));
            legend.set(new PaintScaleLegend(ps.get(), scaleAxis));
            legend.get().setSubdivisionCount(128);
            legend.get().setAxisLocation(AxisLocation.TOP_OR_RIGHT);
            legend.get().setPadding(new RectangleInsets(0, 0.0, 0.0, 0.0));
            legend.get().setStripWidth(20);
            legend.get().setPosition(RectangleEdge.RIGHT);
            legend.get().setBackgroundPaint(Color.WHITE);
            chart.get().addSubtitle(legend.get());
            chart.get().setBackgroundPaint(Color.WHITE);
            chartPanel.setChart(chart.get());
        });
        button1.setPreferredSize(new Dimension(70, 25));

        JButton button4 = new JButton(RUS ? "Очистить" : "Clear");
        button4.addActionListener(e -> {
            final int val = DATA.getSeriesCount();
            int i = 0;
            while (i < val)  {
                DATA.removeSeries("S" + i);
                i++;
            }
        });
        button4.setPreferredSize(new Dimension(100, 25));

        JButton button2 = new JButton(RUS ? "Eng." : "Рус.");
        button2.addActionListener(e -> {
            RUS = !RUS;
            button2.setText(RUS ? "Eng." : "Рус.");
            button3.setText(RUS ? "Справка" : "Help");
            f.setTitle(RUS ? "Климент Лагранжевич КР по НММ" : "Kliment Lagrangiewicz CW on UMM");
            chart.get().setTitle(RUS ? "Распределение температур" : "Heat distribution");
            scaleAxis.setLabel(RUS ? "Диапазон" : "Range");
            xAxis.setLabel(RUS ? "Ось абсцисс" : "x Axis");
            yAxis.setLabel(RUS ? "Ось ординат" : "y Axis");
            button1.setText(RUS ? "Выч." : "Calc.");
            button4.setText(RUS ? "Очистить" : "Clear");
        });
        button2.setPreferredSize(new Dimension(65, 25));

        GridBagConstraints constraints;
        constraints = new GridBagConstraints();
        /* Panel1 */
        JPanel customPanel = new JPanel();
        customPanel.setLayout(new GridBagLayout());
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(5, 5, 5, 5);
        customPanel.add(chartPanel, constraints);

        /* Panel 2 */
        JPanel customPanel2 = new JPanel();
        customPanel2.setLayout(new GridBagLayout());
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(5, 5, 0, 5);
        customPanel2.add(button2, constraints);
        
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(5, 0, 0, 5);
        customPanel2.add(button3, constraints);

        constraints.gridx = 2;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(5, 0, 0, 5);
        customPanel2.add(button4, constraints);

        constraints.gridx = 3;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(5, 0, 0, 5);
        customPanel2.add(button1, constraints);

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(10, 5, 0, 5);
        customPanel2.add(new JLabel("A = "), constraints);

        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(10, 5, 0, 5);
        customPanel2.add(new JLabel("B = "), constraints);

        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(10, 5, 0, 5);
        customPanel2.add(new JLabel("Nx = "), constraints);

        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(10, 5, 0, 5);
        customPanel2.add(new JLabel("Ny = "), constraints);

        constraints.gridx = 0;
        constraints.gridy = 5;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(10, 5, 0, 5);
        customPanel2.add(new JLabel("Φh = "), constraints);

        constraints.gridx = 0;
        constraints.gridy = 6;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(10, 5, 0, 5);
        customPanel2.add(new JLabel("Φc = "), constraints);

        constraints.gridx = 0;
        constraints.gridy = 7;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(10, 5, 0, 5);
        customPanel2.add(new JLabel("Φ0 = "), constraints);

        constraints.gridx = 0;
        constraints.gridy = 8;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(10, 5, 0, 5);
        customPanel2.add(new JLabel("c = "), constraints);

        constraints.gridx = 0;
        constraints.gridy = 9;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(10, 5, 0, 5);
        customPanel2.add(new JLabel("λ = "), constraints);

        constraints.gridx = 0;
        constraints.gridy = 10;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(10, 5, 0, 5);
        customPanel2.add(new JLabel("ρ = "), constraints);

        constraints.gridx = 0;
        constraints.gridy = 11;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(10, 5, 0, 5);
        customPanel2.add(new JLabel("t = "), constraints);

        t1.setText(String.valueOf(A));
        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(10, 5, 0, 5);
        t1.setPreferredSize(new Dimension(90, 20));
        customPanel2.add(t1, constraints);

        t2.setText(String.valueOf(B));
        constraints.gridx = 1;
        constraints.gridy = 2;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(10, 5, 0, 5);
        t2.setPreferredSize(new Dimension(90, 20));
        customPanel2.add(t2, constraints);

        t3.setText(String.valueOf(Nx));
        constraints.gridx = 1;
        constraints.gridy = 3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(10, 5, 0, 5);
        t3.setPreferredSize(new Dimension(90, 20));
        customPanel2.add(t3, constraints);

        t4.setText(String.valueOf(Ny));
        constraints.gridx = 1;
        constraints.gridy = 4;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(10, 5, 0, 5);
        t4.setPreferredSize(new Dimension(90, 20));
        customPanel2.add(t4, constraints);

        t5.setText(String.valueOf(tH));
        constraints.gridx = 1;
        constraints.gridy = 5;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(10, 5, 0, 5);
        t5.setPreferredSize(new Dimension(90, 20));
        customPanel2.add(t5, constraints);

        t6.setText(String.valueOf(tC));
        constraints.gridx = 1;
        constraints.gridy = 6;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(10, 5, 0, 5);
        t6.setPreferredSize(new Dimension(90, 20));
        customPanel2.add(t6, constraints);

        t7.setText(String.valueOf(tFS));
        constraints.gridx = 1;
        constraints.gridy = 7;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(10, 5, 0, 5);
        t7.setPreferredSize(new Dimension(90, 20));
        customPanel2.add(t7, constraints);

        t8.setText(String.valueOf(c));
        constraints.gridx = 1;
        constraints.gridy = 8;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(10, 5, 0, 5);
        t8.setPreferredSize(new Dimension(90, 20));
        customPanel2.add(t8, constraints);

        t9.setText(String.valueOf(lambda));
        constraints.gridx = 1;
        constraints.gridy = 9;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(10, 5, 0, 5);
        t9.setPreferredSize(new Dimension(90, 20));
        customPanel2.add(t9, constraints);

        t10.setText(String.valueOf(ro));
        constraints.gridx = 1;
        constraints.gridy = 10;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(10, 5, 0, 5);
        t10.setPreferredSize(new Dimension(90, 20));
        customPanel2.add(t10, constraints);

        t11.setText(String.valueOf(time));
        constraints.gridx = 1;
        constraints.gridy = 11;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(10, 5, 0, 5);
        t11.setPreferredSize(new Dimension(90, 20));
        customPanel2.add(t11, constraints);


        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(0, 0, 0, 0);
        f.add(customPanel, constraints);
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(0, 0, 0, 0);
        f.add(customPanel2, constraints);
        f.pack();
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }
    public static void main(String[] args) {
        try {
            EventQueue.invokeLater(() -> new Main("Климент Лагранжевич КР по НММ"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private record SpectrumPaintScale (double lowerBound, double upperBound) implements PaintScale {
        @Override
        public double getLowerBound() {
            return lowerBound;
        }
        @Override
        public double getUpperBound() {
            return upperBound;
        }
        @Override
        public Paint getPaint(double value) {
            float scaledValue = (float) (value / (upperBound - lowerBound));
            float h1 = 0f;
            float h2 = 0.42f;
            float scaledH = h1 + scaledValue * (h2 - h1);
            return Color.getHSBColor(scaledH, 1f, 1f);
        }
    }
}