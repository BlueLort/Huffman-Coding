package model;


import javafx.util.Pair;

import java.util.*;

public class FrequencyChecker {
    //get the frequency of each character and store it in array and return that.
    public static ArrayList<Pair<Character,Long>> GetFrequency(byte[] inputData){
        HashMap<Character,Long> frequencies=new HashMap<>();
        for(int i=0;i<inputData.length;i++){
            if(!frequencies.containsKey((char)(inputData[i]&0xff))){
                frequencies.put((char)(inputData[i]&0xff),1L);
            }else{
                long lastVal=frequencies.get((char)(inputData[i]&0xff));
                frequencies.replace((char)(inputData[i]&0xff),lastVal+1L);
            }

        }
        ArrayList<Pair<Character,Long>> result=new ArrayList<>();
        for (Map.Entry<Character,Long> ite:frequencies.entrySet()) {
            result.add(new Pair<>(ite.getKey(),ite.getValue()));
        }
        return result;
    }



}
