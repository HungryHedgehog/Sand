import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.Arrays;
import java.util.HashMap;
import java.awt.Graphics;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class GamePanel extends JPanel implements Runnable{


    int height;
    int width;
    int gridWidth;
    int gridHeight;
    int moleculeScale = 4;

    byte[][] grid;

    BufferedImage image;
    HashMap<Byte, Molecule> Molecules = Molecule.InitializeMolecules();

    Thread gameThread;
    MouseHandler mouseHandler;
    Point mouseOnScreenLocation;

    public GamePanel(int width, int height){
        this.setPreferredSize(new Dimension(width, height));
        this.setBackground(Color.BLACK);
        this.setDoubleBuffered(true);
        this.width = width;
        this.height = height;
        this.gridWidth = width / moleculeScale;
        this.gridHeight =height / moleculeScale;
        grid = new byte[gridWidth][gridHeight];
        for (byte[] row: grid)
            Arrays.fill(row, (byte)1);
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    }

    public void start() {
        if(gameThread == null)
            gameThread = new Thread(this);
        mouseHandler = new MouseHandler(Molecules.keySet());
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
        addMouseWheelListener(mouseHandler);
        gameThread.start();
    }

    @Override
    public void run() {
        float interval = 1000;
        float delta = 0;
        long lastTime = System.nanoTime();
        long currentTime;
        while(gameThread != null){
            try{

                currentTime = System.nanoTime();
                delta += (currentTime - lastTime) / interval;
                lastTime = currentTime;

                if(delta >= 1){
                    updateMouseLocation();
                    drawMolecules();
                    updateImage();
                    updatePhysics();
                    repaint();
                    delta = 0;
                }

            } catch (Exception e){
                System.out.println(e);
            }
        }
    }

    private void updateMouseLocation(){
        //update onscreen pointer location unless new location would be null
        Point p = MouseInfo.getPointerInfo().getLocation();
        if(p != null){
            SwingUtilities.convertPointFromScreen(p, this);
            mouseOnScreenLocation = p;
        }
    }

    private void drawMolecules(){
        if(mouseHandler.LeftIsPressed){
            Point gridPoint = screenToGrid(mouseOnScreenLocation);
            if(isValidPointOnGrid(gridPoint.x, gridPoint.y)){
                int gridRadius = mouseHandler.DrawRadius / moleculeScale;

                for (int xOffset = -gridRadius; xOffset <= gridRadius; xOffset++){
                    for (int yOffset = -gridRadius; yOffset <= gridRadius; yOffset++){
                        int x = gridPoint.x - xOffset;
                        int y = gridPoint.y - yOffset;
                        if(isValidPointOnGrid(x, y) && xOffset * xOffset + yOffset * yOffset <= gridRadius * gridRadius){
                            grid[x][y] = mouseHandler.SelectedMolecule;                            
                        }
                    }
                }
            }
        }
    }

    private void updateImage(){
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                byte cell = grid[x / moleculeScale] [y / moleculeScale];
                Molecule mol = Molecules.get(cell);
                //Draw on image
                image.setRGB(x, y, mol.Color.getRGB());
            }
        }
    }

    private void updatePhysics() {
        byte[][] nextGrid = Arrays.stream(grid).map(byte[]::clone).toArray(byte[][]::new);

        for (int x = 0; x < gridWidth; x++) {   
            for (int y = 0; y < gridHeight; y++) {
                byte cell = grid[x][y];                
                Molecule mol = Molecules.get(cell);

                //only check for gravity above the lowest row
                if(y < gridHeight - 1){
                    Molecule moleculeBelow = Molecules.get(grid[x] [y + 1]);                    
                    Molecule moleculeBelowLeft = isValidPointOnGrid(x - 1, y + 1) ? Molecules.get(grid[x - 1] [y + 1]) : null;
                    Molecule moleculeBelowRight = isValidPointOnGrid(x + 1, y + 1) ? Molecules.get(grid[x + 1] [y + 1]) : null;
                    //gravity
                    if(mol.Density > moleculeBelow.Density){
                        nextGrid[x] [y] = moleculeBelow.ID;
                        nextGrid[x] [y + 1] = mol.ID;
                    } else if(moleculeBelowLeft != null && mol.Density > moleculeBelowLeft.Density){
                        nextGrid[x] [y] = moleculeBelowLeft.ID;
                        nextGrid[x - 1] [y + 1] = mol.ID;
                    } else if(moleculeBelowRight != null && mol.Density > moleculeBelowRight.Density){
                        nextGrid[x] [y] = moleculeBelowRight.ID;
                        nextGrid[x + 1] [y + 1] = mol.ID;
                    }
                }               

            }
        }

        grid = nextGrid;
    }
    
    private Point screenToGrid(Point screenPoint){
        return new Point(screenPoint.x / moleculeScale, screenPoint.y / moleculeScale);
    }
    
    private boolean isValidPointOnGrid(int x, int y){
        return x >= 0 && y >= 0 && x < gridWidth && y < gridWidth;
    }

    @Override
    public void paintComponent(Graphics g){
        super.paintComponent(g);
        g.drawImage(image, 0, 0, this);
        if(mouseOnScreenLocation != null){
            g.setColor(Color.WHITE);
            int offset = mouseHandler.DrawRadius / 2;
            g.drawOval(mouseOnScreenLocation.x - offset, mouseOnScreenLocation.y - offset, mouseHandler.DrawRadius, mouseHandler.DrawRadius);
        }
        g.setFont(new Font("SansSerif", Font.BOLD, 12)); 

        g.drawString(Molecules.get(mouseHandler.SelectedMolecule).Name, 10, 20);    
    }



}
