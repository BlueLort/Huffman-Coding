package model;

import javafx.scene.Parent;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;

public class HuffmanCompressor {
    //the Compress Algorithm goes here it returns a table that contains Character -> binary value
    public static HashMap<Character,Integer> Compress(ArrayList<Pair<Character,Long>> FrequencyArray){
        //Convert the frequency array to a MinHeap to use it in the algorithm.
        PriorityQueue<Node> minHeap=ConvertToHeap(FrequencyArray);
        Node root=new Node(null,null,null);

        int len=FrequencyArray.size()-1;
        //notice i will only run n-1 times because in the end i will have the root
        for(int i=0;i<len;i++){
            Node P1=minHeap.poll();
            Node P2=minHeap.poll();
            Node Parent=new Node(new Pair<>(null,P1.val.getValue()+P2.val.getValue()),P1,P2);
            root=Parent;
            minHeap.add(Parent);
        }
        System.out.println(GetBinaryTable(root));
        return GetBinaryTable(root);
    }
    private static PriorityQueue<Node> ConvertToHeap(ArrayList<Pair<Character,Long>> FrequencyArray){
        PriorityQueue<Node> minHeap=new PriorityQueue<>(new HuffmanComparator());
        for(Pair ite:FrequencyArray){
            minHeap.add(new Node(ite,null,null));
        }
        return minHeap;
    }
    private static class HuffmanComparator implements Comparator<Node> {
        public int compare(Node p1, Node p2)
        {
            return p1.val.getValue().intValue()-p2.val.getValue().intValue();
        }
    }
    private static HashMap<Character,Integer> GetBinaryTable(Node HuffmanTree){
        HashMap<Character,Integer> table=new HashMap<>();
        TraverseTree(HuffmanTree.right,table,1);
        TraverseTree(HuffmanTree.left,table,0);
        return table;
    }
    private static void TraverseTree(Node node,HashMap<Character,Integer> table,Integer binaryVal){
            if(node==null)return;
            if(node.val.getKey()!=null){
                table.put(node.val.getKey(),binaryVal);
            }
            TraverseTree(node.right,table,binaryVal<<1 | 1);
            TraverseTree(node.left,table,binaryVal<<1);
    }
}

