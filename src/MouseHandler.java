import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.Iterator;
import java.util.Set;

public class MouseHandler extends MouseAdapter {
    
    private final byte MIN_DRAW_RADIUS = 1;
    private final byte MAX_DRAW_RADIUS = 50;

    public byte SelectedMolecule = 0; 
    public byte DrawRadius = 10;
    private Set<Byte> keys;
    private Iterator<Byte> moleculeIterator;
    public boolean LeftIsPressed;    
    public boolean RightIsPressed;

    public MouseHandler (Set<Byte> keys){
        this.keys = keys;
        moleculeIterator = keys.iterator();
        if(moleculeIterator.hasNext())
            SelectedMolecule = moleculeIterator.next();
        else
            SelectedMolecule = 0;
    }

    @Override
    public void mousePressed(MouseEvent e){
        if(e.getButton() == MouseEvent.BUTTON1)
            LeftIsPressed = true;
        else if (e.getButton() == MouseEvent.BUTTON3)
            RightIsPressed = true;
    }

    @Override
    public void mouseReleased(MouseEvent e){
        if(e.getButton() == MouseEvent.BUTTON1)
            LeftIsPressed = false;
        else if (e.getButton() == MouseEvent.BUTTON3)
            RightIsPressed = false;
    }

    @Override
    public void mouseClicked(MouseEvent e){
        if(e.getButton() == MouseEvent.BUTTON3){
            if(moleculeIterator.hasNext())
                SelectedMolecule = moleculeIterator.next();
            else {
                moleculeIterator = keys.iterator();
                if(moleculeIterator.hasNext())
                    SelectedMolecule = moleculeIterator.next();
            }
        }
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e){
        DrawRadius = (byte) Math.min(MAX_DRAW_RADIUS, Math.max(MIN_DRAW_RADIUS, DrawRadius + e.getScrollAmount() * e.getWheelRotation()));
    }
}
