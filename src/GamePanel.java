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

    Molecule[][] grid;

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
        Molecule defaultMolecule = Molecules.get((byte)0);  
        grid = new Molecule[gridWidth][gridHeight];      
        for (Molecule[] row : grid)
            Arrays.fill(row, defaultMolecule);
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
                        int r = rand.nextInt(gridRadius);
                        int r2 = rand.nextInt(100);
                        if (r < gridRadius / 2 && isValidPointOnGrid(x, y)
                                && xOffset * xOffset + yOffset * yOffset <= gridRadius * gridRadius) {
                            Molecule mol = Molecules.get(mouseHandler.SelectedMolecule).Clone();
                            mol.Random = r2;
                            if(mol.Random < 10){
                                mol.Color = mol.Color.darker();
                            } else if (mol.Random > 90){
                                mol.Color = mol.Color.brighter();
                            }
                            grid[x][y] = mol;
                            System.out.println(mol.Color);
                        }
                    }
                }
            }
        }
    }

    private void updateImage() {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Molecule mol = grid[x / moleculeScale][y / moleculeScale];
                if (mol != null) {
                    // Draw on image
                    image.setRGB(x, y, mol.Color.getRGB());
                } else {
                    image.setRGB(x, y, Color.PINK.getRGB());
                }

            }
        }
    }

    /*
     * Updates the physics by creating a new empty grid and filling it. To avoid
     * overwriting we only write data when the field hasnt been touched in the new
     * grid yet.
     * However when looking to the left we must make an exception otherwise things
     * would never move left (because we traverse the grid left to right)
     */
    private void updatePhysics() {
        updateGravity();
    }

    private void updateGravity(){
        Molecule[][] nextGrid = new Molecule[gridWidth][gridHeight];

        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) {

                if (nextGrid[x][y] == null) {

                    Molecule mol = grid[x][y];
                    Molecule moleculeBelow = isValidPointOnGrid(x, y + 1) ? grid[x][y + 1] : null;

                    Molecule moleculeBelowLeft = isValidPointOnGrid(x - 1, y + 1) ? grid[x - 1][y + 1]
                            : null;
                    Molecule moleculeBelowRight = isValidPointOnGrid(x + 1, y + 1) ? grid[x + 1][y + 1]
                            : null;

                    Molecule moleculeLeft = x - 1 >= 0 ? grid[x - 1][y] : null;
                    Molecule moleculeRight = x + 1 < gridWidth ? grid[x + 1][y] : null;

                    // gravity
                    if (moleculeBelow != null && moleculeBelow.Flows && mol.Density > moleculeBelow.Density) {
                        nextGrid[x][y] = moleculeBelow;
                        nextGrid[x][y + 1] = mol;
                    } else if (moleculeBelowLeft != null && moleculeBelowLeft.Flows && mol.Density > moleculeBelowLeft.Density) {
                        nextGrid[x][y] = moleculeBelowLeft;
                        nextGrid[x - 1][y + 1] = mol;
                    }
                    else if (moleculeBelowRight != null && moleculeBelowRight.Flows
                            && mol.Density > moleculeBelowRight.Density) {
                        nextGrid[x][y] = moleculeBelowRight;
                        nextGrid[x + 1][y + 1] = mol;
                    } else if (mol.Flows) {
                        short dir = 0;
                        //int r = rand.nextInt(3);
                        /*
                         * If there is space on both sides we want it to not go in one direction without bias
                         */
                        if(moleculeLeft != null && moleculeLeft.Flows && mol.Density > moleculeLeft.Density && nextGrid[x-1][y] == null){
                            dir -= 1;
                        }
                        if(moleculeRight != null && moleculeRight.Flows && mol.Density > moleculeRight.Density && nextGrid[x+1][y] == null){
                            dir +=1;
                        }

                        switch(dir){
                            case -1:
                                nextGrid[x][y] = moleculeLeft;
                                nextGrid[x - 1][y] = mol;
                                break;
                            case 1:
                                nextGrid[x][y] = moleculeRight;
                                nextGrid[x + 1][y] = mol;
                                break;
                                
                        }
                    }
                }
            }
        }

        //Update the ones not moved so they dont get in the way
        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) { 
                if(nextGrid[x][y] == null){
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
