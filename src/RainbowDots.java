import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class RainbowDots extends JFrame implements Runnable, MouseListener {

    private final int w = 500;
    private final int h = 500;

    private final int offsetW = 16;
    private final int offsetH = 38;

    private boolean redraw = false;

    private final BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    private final BufferedImage pimg = new BufferedImage(w / 8, h / 8, BufferedImage.TYPE_INT_RGB);

    private final NeuralNetwork nn;

    public List<Point> points = new ArrayList<>();

    private static final Set<Color> BLACK_BORDERS = Set.of(
            Color.WHITE,
            Color.YELLOW,
            Color.CYAN,
            Color.GRAY
    );

    private static final Map<Integer, Color> TYPE_TO_COLOR = Map.of(
            0, Color.BLACK,
            1, Color.RED,
            2, Color.GREEN,
            3, Color.BLUE,
            4, Color.YELLOW,
            5, Color.CYAN,
            6, Color.MAGENTA,
            7, Color.GRAY,
            8, Color.WHITE
    );

    private static final Map<Color, Integer> COLOR_TO_TYPE = TYPE_TO_COLOR.entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

    public RainbowDots() {
        UnaryOperator<Double> sigmoid = x -> 1 / (1 + Math.exp(-x));
        UnaryOperator<Double> dsigmoid = y -> y * (1 - y);
//        nn = new NeuralNetwork(0.01, sigmoid, dsigmoid, 2, 28, 28, 28, 9);
        nn = new NeuralNetwork(0.01, sigmoid, dsigmoid, 2, 57, 57, 57, 9);

        this.setSize(w + offsetW, h + offsetH);
        this.setVisible(true);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocation(50, 50);
        this.add(new JLabel(new ImageIcon(img)));
        addMouseListener(this);
    }

    @Override
    public void run() {
        while (true) {
            this.repaint();
//            try { Thread.sleep(17); } catch (InterruptedException e) {}
        }
    }

    private static double norma1(double value) {
        return (value > 1) ? 1 : value;
    }

    private static int getRgbColor(double[] outputs) {
        double black = outputs[0];
        double red = Math.max(0, outputs[1] - black);
        double green = Math.max(0, outputs[2] - black);
        double blue = Math.max(0, outputs[3] - black);
        double yellow = Math.max(0, outputs[4] - black);
        double cian = Math.max(0, outputs[5] - black);
        double magenta = Math.max(0, outputs[6] - black);
        double gray = Math.max(0, outputs[7] - black) * 0.5;
        double white = Math.max(0, outputs[8] - black);

        double commonRed = norma1(white + gray + red + yellow + magenta);
        double commonGreen = norma1(white + gray + green + yellow + cian);
        double commonBlue = norma1(white + gray + blue + magenta + cian);

        return ((int)(commonRed * 255) << 16) | ((int)(commonGreen * 255) << 8) | (int)(commonBlue * 255);
    }

    @Override
    public void paint(Graphics g) {
//        System.out.println("Paint");

        if (redraw && points.size() > 0) {
            for (int k = 0; k < 100; k++) {
                Point p = points.get((int) (Math.random() * points.size()));
                double nx = (double) p.x / w - 0.5;
                double ny = (double) p.y / h - 0.5;
                nn.feedForward(new double[]{nx, ny});
                double[] targets = new double[9];
                for (int i = 0; i < 9; i++) {
                    targets[i] = 0;
                }
                targets[p.type] = 1;
                nn.backpropagation(targets);
            }
        }

        if (redraw) {
            for (int i = 0; i < w / 8; i++) {
                for (int j = 0; j < h / 8; j++) {
                    double nx = (double) i / w * 8 - 0.5;
                    double ny = (double) j / h * 8 - 0.5;
                    double[] outputs = nn.feedForward(new double[]{nx, ny});
                    int color = getRgbColor(outputs);
                    pimg.setRGB(i, j, color);
                }
            }
        }

        Graphics ig = img.getGraphics();
        ig.drawImage(pimg, 0, 0, w, h, this);
        for (Point p : points) {
            Color mainColor = TYPE_TO_COLOR.getOrDefault(p.type, Color.WHITE);
            Color nearColor = BLACK_BORDERS.contains(mainColor) ? Color.BLACK : Color.WHITE;

            ig.setColor(nearColor);
            ig.fillOval(p.x - 2, p.y - 2, 14, 14);
            ig.setColor(mainColor);
            ig.fillOval(p.x, p.y, 10, 10);
        }
        g.drawImage(img, 8, 30, w, h, this);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        int type = getClickType(e);
//        System.out.println(type);
        points.add(new Point(e.getX() - offsetW, e.getY() - offsetH, type));
    }

    private static int getClickType(MouseEvent e) {
        return COLOR_TO_TYPE.getOrDefault(getClickColor(e), 8);
    }

    private static Color getClickColor(MouseEvent e) {
        boolean isLeft = ((e.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) != 0);
        boolean isRight = ((e.getModifiersEx() & InputEvent.BUTTON3_DOWN_MASK) != 0);

        System.out.println("Shift: " + e.isShiftDown() + " Alt: " + e.isAltDown() + " Ctrl: " + e.isControlDown());
        System.out.println("Left: " + isLeft +
                " 2?: " + (e.getModifiersEx() & InputEvent.BUTTON2_DOWN_MASK) +
                " Right: " + isRight);

        if (e.isShiftDown() && !e.isControlDown() && !e.isAltDown() && isLeft) {
            return Color.GREEN;
        } else if (e.isShiftDown() && !e.isControlDown() && !e.isAltDown() && isRight) {
            return Color.BLACK;
        } else if (e.isAltDown() && !e.isShiftDown() && !e.isControlDown() && isLeft) {
            return Color.YELLOW;
        } else if (e.isAltDown() && !e.isShiftDown() && !e.isControlDown() && isRight) {
            return Color.CYAN;
        } else if (e.isControlDown() && !e.isShiftDown() && !e.isAltDown() && isLeft) {
            return Color.MAGENTA;
        } else if (e.isControlDown() && !e.isShiftDown() && !e.isAltDown() && isRight) {
            return Color.GRAY;
        } else if (isLeft) {
            return Color.BLUE;
        } else if (isRight) {
            return Color.RED;
        }
        System.out.println("Incorrect input");
        return Color.WHITE;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        redraw = false;
        System.out.println("Redraw: off");
    }

    @Override
    public void mouseExited(MouseEvent e) {
        redraw = true;
        System.out.println("Redraw: on2");
    }
}