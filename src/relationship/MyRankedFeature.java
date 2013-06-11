package relationship;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import cc.mallet.types.*;

public class MyRankedFeature {

	/**
	 * @param args
	 */
	RankedFeatureVector RankVector = null;

	public RankedFeatureVector getFreqRankVector(InstanceList instances) {
		Alphabet dataAlphabet = instances.getAlphabet();
		FeatureCounter counter = new FeatureCounter(dataAlphabet);

		for (cc.mallet.types.Instance ins : instances) {
			if (ins.getData() instanceof FeatureSequence) {

				FeatureSequence features = (FeatureSequence) ins.getData();

				for (int position = 0; position < features.size(); position++) {
					counter.increment(features.getIndexAtPosition(position));
				}

			} else {
				throw new IllegalArgumentException(
						"Looking for a FeatureSequence, found a "
								+ ins.getData().getClass());
			}
		}
		this.RankVector = counter.toRankedFeatureVector();
		return this.RankVector;
	}
	
	public Map<String,Integer> quickGetLowFreq(InstanceList instances,FeatureCounter counter,int miniCount ){
		Alphabet dataAlphabet = instances.getAlphabet();
		Map<String,Integer> result=new HashMap<String,Integer>();
		if(counter==null){
			counter = new FeatureCounter(dataAlphabet);
			for (cc.mallet.types.Instance ins : instances) {
				if (ins.getData() instanceof FeatureSequence) {

					FeatureSequence features = (FeatureSequence) ins.getData();

					for (int position = 0; position < features.size(); position++) {
						counter.increment(features.getIndexAtPosition(position));
					}

				} else {
					throw new IllegalArgumentException(
							"Looking for a FeatureSequence, found a "
									+ ins.getData().getClass());
				}
			}
		}
		//System.out.println("total unique:"+counter.featureCounts.size());
		for(int feature=0;feature<dataAlphabet.size();feature++){
			if(counter.get(feature)<=miniCount){
				String word=(String) dataAlphabet.lookupObject(feature);
				if(result.containsKey(word)){
					continue;
				}else{
					int freq=counter.get(feature);
					result.put(word, freq);
				}
				
			}
		}
		
		return result;
	}

	public Map<String, Double>quickGetRemovedLowFreq(InstanceList instances,int threshod,boolean print){
		Map<String, Double> removedLowFreq=new HashMap<String,Double>();
		Alphabet dataAlphabet = instances.getAlphabet();
		FeatureCounter counter = new FeatureCounter(dataAlphabet);
		for (cc.mallet.types.Instance ins : instances) {
			if (ins.getData() instanceof FeatureSequence) {

				FeatureSequence features = (FeatureSequence) ins.getData();

				for (int position = 0; position < features.size(); position++) {
					counter.increment(features.getIndexAtPosition(position));
				}

			} else {
				throw new IllegalArgumentException(
						"Looking for a FeatureSequence, found a "
								+ ins.getData().getClass());
			}
		}
		//System.out.println("total unique:"+counter.featureCounts.size());
		for(int feature=0;feature<dataAlphabet.size();feature++){
			if(counter.get(feature)>threshod){
				String word=(String) dataAlphabet.lookupObject(feature);
				if(removedLowFreq.containsKey(word)){
					continue;
				}else{
					int freq=counter.get(feature);
					removedLowFreq.put(word, (double)freq);
					if(print){
						System.out.println(word+":"+freq);
					}
				}
				
			}
		}
		System.out.println("num of word with freq >"+threshod+" is "+removedLowFreq.size());
		
		return removedLowFreq;
	}
	public void setRankVector(RankedFeatureVector RankVector) {
		this.RankVector = RankVector;
	}

	public int getTotalNum() {
		if (this.RankVector != null) {
			return this.RankVector.numLocations();
		} else {
			return -1;
		}
	}

	
	public ArrayList<String> getTopK(int num, boolean print) {
		ArrayList<String> topK = new ArrayList<String>();
		int len = this.RankVector.numLocations();
		if (num > len) {
			num = len;
		}
		for (int rank = 0; rank < num; rank++) {
			Object obj = this.RankVector.getObjectAtRank(rank);
			if (print) {
				double val = this.RankVector.getValueAtRank(rank);
				System.out.println(obj + ":" + val + " ");
			}
			topK.add(obj.toString());
		}
		return topK;
	}
	public Map<String,Double> getTopKMap(int num, boolean print) {
		Map<String,Double> topK = new HashMap<String,Double>();
		int len = this.RankVector.numLocations();
		if (num > len) {
			num = len;
		}
		for (int rank = 0; rank < num; rank++) {
			Object obj = this.RankVector.getObjectAtRank(rank);
			double val = this.RankVector.getValueAtRank(rank);
			if (print) {
				System.out.println(obj + ":" + val + " ");
			}
			topK.put(obj.toString(), val);
		}
		return topK;
	}
	
	public ArrayList<String> getLowerK(int num, boolean print) {
		ArrayList<String> lowerK = new ArrayList<String>();
		int len = this.RankVector.numLocations();
		assert (num < len);
		for (int rank = len - num; rank < len; rank++) {
			Object obj = this.RankVector.getObjectAtRank(rank);
			if (print) {
				double val = this.RankVector.getValueAtRank(rank);
				System.out.println(obj + ":" + val + " ");
			}
			lowerK.add(obj.toString());
		}
		return lowerK;
	}

	public ArrayList<String> getLowerTokenByThreshod(double threshod,
			boolean print) {
		ArrayList<String> result = new ArrayList<String>();
		int len = this.RankVector.numLocations();
		for (int rank = len - 1; rank > 0; rank--) {
			Object obj = this.RankVector.getObjectAtRank(rank);
			double val = this.RankVector.getValueAtRank(rank);
			if (val < threshod) {
				result.add(obj.toString());
				if (print) {
					System.out.println(obj + ":" + val + " ");
				}
			}
		}
		return result;
	}

	public static void main(String[] args) throws Exception {
	//	TopicModel tm = new TopicModel();

	//	InstanceList instances =InstanceList.load(new File("D:\\mallet-2.0.7\\11weibo-mutilNV\\weib-mutilNV-trained.mallet"));
		InstanceList instances =InstanceList.load(new File("D:\\mallet-2.0.7\\11weibo-mutilNV\\weibo-mutilNV-removeLowFreq.mallet"));
	//	File rankV=new File("D:\\mallet-2.0.7\\11weibo-mutilNV\\rankV");
		MyRankedFeature mrf = new MyRankedFeature();
	//	mrf.getFreqRankVector(instances);
		Map<String ,Integer> resultMap=mrf.quickGetLowFreq(instances, null, 3); 
		/*try {
			ObjectOutputStream ois;
			ois = new ObjectOutputStream (new FileOutputStream (rankV));
			ois.writeObject(mrf.RankVector);
			ois.close();
		} catch (Exception e) {
			e.printStackTrace();
			throw new IllegalArgumentException ("Couldn't save InstanceList to file "+rankV.getAbsolutePath());
		}*/
		System.out.println("totalNum:" + resultMap.size());
	/*	System.out.println("topK====================");
		mrf.getTopK(10, true);
		System.out.println("lowK=====================");
		mrf.getLowerK(10, true);*/
		System.out.println("lowByThreshod=====================");
		PrintWriter pout=new PrintWriter(new FileWriter("D:\\mallet-2.0.7\\11weibo-mutilNV\\weibo-freqLe3.dict"));
		for(String word:resultMap.keySet()){
			System.out.println(word+":"+resultMap.get(word));
			pout.println(word);
		}
		pout.close();
		System.out.println("totalNum:" + resultMap.size());
		//mrf.getLowerTokenByThreshod(2, true);
	}

}
