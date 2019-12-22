package model;

import javafx.concurrent.Task;
import javafx.scene.Parent;
import javafx.util.Pair;
import model.Decompression.DecompressedFileInfo;
import model.Decompression.DecompressedFolderInfo;
import model.Decompression.DecompressionHandler;

import java.util.*;

public class HuffmanCompressor {
    //the Compress Algorithm goes here it returns a table that contains Character -> binary value
    public static CompressionInfo Compress(ArrayList<Pair<Character,Long>> frequencyArray,String fileName){
        //Convert the frequency array to a MinHeap to use it in the algorithm.
        PriorityQueue<Node> minHeap=ConvertToHeap(frequencyArray);
        Node root=new Node(null,null,null);
        int len=frequencyArray.size()-1;
        //notice i will only run n-1 times because in the end i will have the root
        for(int i=0;i<len;i++){
            Node P1=minHeap.poll();
            Node P2=minHeap.poll();
            Node Parent=new Node(new Pair<>(null,P1.val.getValue()+P2.val.getValue()),P1,P2);
            root=Parent;
            minHeap.add(Parent);
        }
        HashMap<Character,String> dictionary=GetBinaryTable(root);

        CompressionInfo CI = new CompressionInfo(frequencyArray,dictionary,fileName);

        instantiateDiplay(CI);//Call to sync print function;

        //next line for debugging
        //DecompressionHandler.debug=dictionary;
        return CI;
    }
    private static Thread instantiateDiplay(CompressionInfo CI){
        Task task = new Task<Void>() {
            @Override public Void call() {
                InfoHandler.DisplayInfo(CI);
                return null;
            }
        };
        Thread t= new Thread(task);
        t.start();
        return t;
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
    private static HashMap<Character,String> GetBinaryTable(Node HuffmanTree){
        HashMap<Character,String> table=new HashMap<>();
        TraverseTree(HuffmanTree.right,table,"1");
        TraverseTree(HuffmanTree.left,table,"0");
        return table;
    }
    private static void TraverseTree(Node node,HashMap<Character,String> table,String binaryVal){
            if(node==null)return;
            if(node.val.getKey()!=null){
                table.put(node.val.getKey(),binaryVal);
            }
            TraverseTree(node.right,table,binaryVal+"1");
            TraverseTree(node.left,table,binaryVal+"0");
    }

}

