package org.example.catanboardgameapp;

public class Harbor {

    public enum HarborType {
        GENERIC(null, 3),            // 3 : 1
        BRICK  (Resource.ResourceType.BRICK, 2),
        WOOD(Resource.ResourceType.WOOD,  2),
        WOOL   (Resource.ResourceType.WOOL,  2),
        GRAIN  (Resource.ResourceType.GRAIN, 2),
        ORE    (Resource.ResourceType.ORE,   2);

        public final Resource.ResourceType specific;
        public final int ratio;

        HarborType(Resource.ResourceType specific, int ratio) {
            this.specific = specific;
            this.ratio    = ratio;
        }
    }

    private final HarborType type;
    private final Edge edge;                 // two coastal vertices

    public Harbor(HarborType type, Edge edge) {
        this.type = type;
        this.edge = edge;
    }

    public HarborType getType() { return type; }
    public Edge getEdge()       { return edge; }

    /** A player can use the harbor if they own a settlement/city on either vertex. */
    public boolean usableBy(Player player) {
        return edge.getVertex1().getOwner() == player || edge.getVertex2().getOwner() == player;
    }


}
