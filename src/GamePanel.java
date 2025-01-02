import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.awt.Graphics;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class GamePanel extends JPanel implements Runnable {

    int height;
    int width;
    int gridWidth;
    int gridHeight;
    int moleculeScale = 4;

    Byte[][] grid;

    BufferedImage image;
    HashMap<Byte, Molecule> Molecules = Molecule.InitializeMolecules();
    Random rand;
    Thread gameThread;
    MouseHandler mouseHandler;
    Point mouseOnScreenLocation;

    public GamePanel(int width, int height) {
        this.setPreferredSize(new Dimension(width, height));
        this.setBackground(Color.BLACK);
        this.setDoubleBuffered(true);
        this.width = width;
        this.height = height;
        this.gridWidth = width / moleculeScale;
        this.gridHeight = height / moleculeScale;
        grid = new Byte[gridWidth][gridHeight];
        for (Byte[] row : grid)
            Arrays.fill(row, (byte)0);
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    }

    public void start() {
        if (gameThread == null)
            gameThread = new Thread(this);
        mouseHandler = new MouseHandler(Molecules.keySet());
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
        addMouseWheelListener(mouseHandler);
        rand = new Random();
        gameThread.start();
    }

    @Override
    public void run() {
        float interval = 1000;
        float delta = 0;
        long lastTime = System.nanoTime();
        long currentTime;
        while (gameThread != null) {
            try {

                currentTime = System.nanoTime();
                delta += (currentTime - lastTime) / interval;
                lastTime = currentTime;

                if (delta >= 1) {
                    updateMouseLocation();
                    drawMolecules();
                    updateImage();
                    updatePhysics();
                    repaint();
                    delta = 0;
                }

            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

    private void updateMouseLocation() {
        // update onscreen pointer location unless new location would be null
        Point p = MouseInfo.getPointerInfo().getLocation();
        if (p != null) {
            SwingUtilities.convertPointFromScreen(p, this);
            mouseOnScreenLocation = p;
        }
    }

    private void drawMolecules() {
        if (mouseHandler.LeftIsPressed) {
            Point gridPoint = screenToGrid(mouseOnScreenLocation);
            if (isValidPointOnGrid(gridPoint.x, gridPoint.y)) {
                int gridRadius = mouseHandler.DrawRadius / moleculeScale;

                for (int xOffset = -gridRadius; xOffset <= gridRadius; xOffset++) {
                    for (int yOffset = -gridRadius; yOffset <= gridRadius; yOffset++) {
                        int x = gridPoint.x - xOffset;
                        int y = gridPoint.y - yOffset;
                        int r = rand.nextInt(3);
                        if (r > 0 && isValidPointOnGrid(x, y)
                                && xOffset * xOffset + yOffset * yOffset <= gridRadius * gridRadius) {
                            grid[x][y] = mouseHandler.SelectedMolecule;
                        }
                    }
                }
            }
        }
    }

    private void updateImage() {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Byte cell = grid[x / moleculeScale][y / moleculeScale];
                if(cell != null){
                    Molecule mol = Molecules.get(cell);
                    // Draw on image
                    image.setRGB(x, y, mol.Color.getRGB());
                } else {
                    image.setRGB(x, y, Color.PINK.getRGB());
                }

            }
        }
    }
    /*
     * Updates the physics by creating a new empty grid and filling it. To avoid overwriting we only write data when the field hasnt been touched in the new grid yet.
     * However when looking to the left we must make an exception otherwise things would never move left (because we traverse the grid left to right)
     */
    private void updatePhysics() {
        Byte[][] nextGrid = new Byte[gridWidth][gridHeight];

        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) {
                Byte cell = grid[x][y];
                Molecule mol = Molecules.get(cell);

                Molecule moleculeBelow = isValidPointOnGrid(x, y + 1) ? Molecules.get(grid[x][y + 1]) : null;
                //here we use the new value if there is one to not overwrite it otherwise
                Molecule moleculeBelowLeft = isValidPointOnGrid(x - 1, y + 1) ? (nextGrid[x - 1][y + 1] == null ? Molecules.get(grid[x - 1][y + 1]) : Molecules.get(nextGrid[x - 1][y + 1]))
                        : null;
                Molecule moleculeBelowRight = isValidPointOnGrid(x + 1, y + 1) ? Molecules.get(grid[x + 1][y + 1])
                        : null;
                //looking left we want to use the next grid value so we don't overwrite it later
                Molecule moleculeLeft = x - 1 >= 0 ? Molecules.get(nextGrid[x - 1][y]) : null;

                Molecule moleculeRight = x + 1 < gridWidth ? Molecules.get(grid[x + 1][y]) : null;

            

                //gravity
                if (moleculeBelow != null && mol.Density > moleculeBelow.Density && nextGrid[x][y+1] == null ) {
                    nextGrid[x][y] = moleculeBelow.ID;
                    nextGrid[x][y + 1] = mol.ID;
                } else if (moleculeBelowLeft != null && mol.Density > moleculeBelowLeft.Density) {
                    nextGrid[x][y] = moleculeBelowLeft.ID;
                    nextGrid[x - 1][y + 1] = mol.ID;
                } else if (moleculeBelowRight != null && mol.Density > moleculeBelowRight.Density && nextGrid[x + 1][y+1] == null) {
                    nextGrid[x][y] = moleculeBelowRight.ID;
                    nextGrid[x + 1][y + 1] = mol.ID;
                } //flow
                else if ((moleculeBelow != null || y == gridHeight - 1) && mol.flows) {
                    if (moleculeLeft != null && mol.Density > moleculeLeft.Density) {
                        nextGrid[x - 1][y] = mol.ID;
                        nextGrid[x][y] = moleculeLeft.ID;
                    } else if (moleculeRight != null && mol.Density > moleculeRight.Density && nextGrid[x + 1][y] == null) {
                        nextGrid[x + 1][y] = mol.ID;
                        nextGrid[x][y] = moleculeRight.ID;
                    }
                }
                
                
                if (nextGrid[x][y] == null) {
                    nextGrid[x][y] = grid[x][y];
                }

            }
        }

        grid = nextGrid;
    }

    private Point screenToGrid(Point screenPoint) {
        return new Point(screenPoint.x / moleculeScale, screenPoint.y / moleculeScale);
    }

    private boolean isValidPointOnGrid(int x, int y) {
        return x >= 0 && y >= 0 && x < gridWidth && y < gridHeight;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(image, 0, 0, this);
        if (mouseOnScreenLocation != null) {
            g.setColor(Color.WHITE);
            int offset = mouseHandler.DrawRadius / 2;
            g.drawOval(mouseOnScreenLocation.x - offset, mouseOnScreenLocation.y - offset, mouseHandler.DrawRadius,
                    mouseHandler.DrawRadius);
        }
        g.setFont(new Font("SansSerif", Font.BOLD, 12));

        g.drawString(Molecules.get(mouseHandler.SelectedMolecule).Name, 10, 20);
    }

}
