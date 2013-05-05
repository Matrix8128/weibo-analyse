package relationship;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import weibo4j.*;
import weibo4j.model.*;

import weka.clusterers.*;
import weka.core.*;
import weka.core.converters.*;

public class Test {

	/**
	 * @param args
	 * @throws Exception 
	 */
	public void temp() throws Exception {

		//String access_token = "2.00N8EaxB08LsGa4ebcc4e9ac0YYqxw";
		//Tags tm = new Tags();
		//tm.client.setToken(access_token);
		//String uids ="" ;
		//String s="1796533527,1700780034";
		

		
		File inputFile=new File("C:\\Users\\Edward\\Desktop\\interest.arff");
		ArffLoader arf=new ArffLoader();
		arf.setFile(inputFile);
		Instances originIns=arf.getDataSet();
		
		/*System.out.println(insTest);
		System.exit(0);*/
		//Map<String,String> wordMap=new HashMap<String,String>();
		Instances insTest=new Instances(originIns,0,1);
		insTest.deleteStringAttributes();
		insTest.delete();
		ArrayList<String> wordList=new ArrayList<String>();
		for(int i=0;i<originIns.numInstances();i++){
			Instance tempIns=new Instance(originIns.instance(i));
			String word=tempIns.stringValue(originIns.attribute("text"));
			tempIns.deleteAttributeAt(0);
			wordList.add(word);
			insTest.add(tempIns);
		}
	
		
		SimpleKMeans sm=new SimpleKMeans();
		
		sm.setNumClusters(10);
		sm.setPreserveInstancesOrder(true);//very important
		sm.buildClusterer(insTest);
		
		int totalNum=insTest.numInstances();
		System.out.println("totalNum:"+insTest.numInstances());
		System.out.println("============================");
		Instances centreIns=sm.getClusterCentroids();
		Map<Integer,ArrayList<String>> result=new HashMap<Integer,ArrayList<String>>();
		for(int i=0;i<centreIns.numInstances();i++){
			Instance ins=centreIns.instance(i);
			result.put(i, new ArrayList<String>());
			System.out.println(i+":\t\n"+ins);
		}
		int[] assign=sm.getAssignments();
		for(int i=0;i<assign.length;i++){
			Instance ins=insTest.instance(i);
			//System.out.println(wordMap.containsKey(ins.toString()));
			result.get(assign[i]).add(wordList.get(i));
		}
		System.out.println(sm.toString());
		

	/*	DistanceFunction df=sm.getDistanceFunction();
		for(int i=0;i<originIns.numInstances();i++){
			Instance oriIns=originIns.instance(i);
			String word=oriIns.stringValue(0);
			Instance ins=new Instance(oriIns);
			ins.deleteAttributeAt(0);
			double distance=df.distance(ins, centreIns.instance(0));
			int cluster=0;
			for(int j=1;j<centreIns.numInstances();j++){
				Instance cenIns=centreIns.instance(j);
				double temp=df.distance(ins, cenIns);
				if(temp<distance){
					cluster=j;
				}
			}
			result.get(cluster).add(word);		
		}*/
		
		
		ArrayList<String> words=new ArrayList<String>();
		for(int k:result.keySet()){
			words=result.get(k);
			System.out.println("cluster"+k+":"+words.size()+":\t"+words.size()*1.0/totalNum*100);
			if(result.get(k).size()<100){
				System.out.println(result.get(k));
			}
		}
	}
	
	public static void main(String[] args) throws Exception {
		String s="1胡新chen3";

		System.out.println(s.length());
		//new Test().temp();
	}
	
}


