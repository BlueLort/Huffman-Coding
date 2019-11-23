package model;

import javafx.util.Pair;

class Node{
    public Pair<Character,Long> val;
    public Node right;
    public Node left;
    Node( Pair<Character,Long> val,Node left,Node right){
        this.val=val;
        this.right=right;
        this.left=left;
    }

    @Override
    public String toString() {
            String out = "";
            out += val.toString() + "\n";
            if (right != null) {
                out += val.toString() + " Right:";
                out += right.toString();
            }
            if (right != null) {
                out += val.toString() + " Left:";
                out += left.toString();
            }
            return out;

    }

}