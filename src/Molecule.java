import java.util.HashMap;
import java.awt.Color;

public class Molecule {
    public final byte ID;
    public final String Name;
    public final Color Color;
    public final float Density;
    public final boolean flammable;
    public final boolean flows; // -1 = infinite

    public Molecule(byte id, String name, Color color, float density, boolean flammable, boolean flows){
        this.ID = id;
        this.Name = name;
        this.Color = color;
        this.Density = density;
        this.flammable = flammable;
        this.flows = flows;
    }

    public static HashMap<Byte, Molecule> InitializeMolecules(){
        HashMap<Byte, Molecule> Molecules = new HashMap<Byte,Molecule>();
        Molecules.put((byte)0, new Molecule((byte)0, "Air", new Color(2,2,2) , 1.29f, false, true));
        Molecules.put((byte)1, new Molecule((byte)1, "Water", new Color(60,60,254) , 997f, false, true));
        Molecules.put((byte)2, new Molecule((byte)2, "Sand", new Color(194,178,128) , 1602f, false, false));
        return Molecules;
    }
}
