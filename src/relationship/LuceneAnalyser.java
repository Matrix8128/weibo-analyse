package relationship;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.wltea.analyzer.lucene.IKAnalyzer;

import java.io.*;
import java.util.*;
import java.util.Queue;

import weibo4j.model.Status;
import weibo4j.model.StatusWapper;
import weibo4j.org.json.JSONArray;
import weibo4j.org.json.JSONException;
import weibo4j.org.json.JSONObject;
import weibo4j.org.json.JSONTokener;

import weka.clusterers.*;
import weka.core.*;
import weka.core.converters.*;

public class LuceneAnalyser {

	String indexDir = "luceneDir";
	String wekaFile = "keywords.arff";
	IndexWriter indexWriter = null;
	ArrayList<MyTerm> termList = new ArrayList<MyTerm>();
	int maxDocNum = 0;
	Map<String, MyTerm> linkMap = new HashMap<String, MyTerm>();

	public LuceneAnalyser() {
		super();
		// TODO Auto-generated constructor stub
	}

	public LuceneAnalyser(String wekaFile) {
		super();
		this.wekaFile = wekaFile;
	}

	public void buildIndex(JSONObject indexData) {

		try {
			Directory dir = FSDirectory.open(new File(indexDir));
			IKAnalyzer analyzer = new IKAnalyzer();
			analyzer.setUseSmart(true);
			IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_35,
					analyzer);
			indexWriter = new IndexWriter(dir, iwc);
			indexWriter.deleteAll();

			JSONArray statusData = indexData.getJSONArray("statusData");
			for (int i = 0; i < statusData.length(); i++) {
				String text = statusData.getString(i);
				Document doc = new Document();
				doc.add(new Field("text", text, Field.Store.YES,
						Field.Index.ANALYZED,
						Field.TermVector.WITH_POSITIONS_OFFSETS));
				indexWriter.addDocument(doc);
			}

			JSONArray userData = indexData.getJSONArray("userData");
			for (int i = 0; i < userData.length(); i++) {
				String text = userData.getString(i);
				Document doc = new Document();
				doc.add(new Field("text", text, Field.Store.YES,
						Field.Index.ANALYZED,
						Field.TermVector.WITH_POSITIONS_OFFSETS));
				indexWriter.addDocument(doc);
			}
			// indexWriter.commit();
			System.out.println("Index is done");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				indexWriter.close();
			} catch (CorruptIndexException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	

	class MyTerm {
		Term originTrem = null;
		int totalFreq = 0;
		TermDocs termDocs = null;
		int vector[] = null;
		Map<Integer, Integer> termMap = new HashMap<Integer, Integer>();

		public MyTerm(Term originTrem, TermDocs termDocs, int maxDocNum)
				throws IOException {
			super();

			this.originTrem = originTrem;
			this.termDocs = termDocs;
			this.totalFreq = 0;
			while (this.termDocs.next()) {
				int docNum = termDocs.doc();
				int freq = termDocs.freq();
				this.termMap.put(docNum, freq);
				this.totalFreq += freq;
			}
			this.vector = new int[maxDocNum];
			for (int i = 0; i < maxDocNum; i++) {
				this.vector[i] = 0;
			}
			for (int k : this.termMap.keySet()) {
				this.vector[k] = (int) this.termMap.get(k);
			}

		}

		@Override
		public String toString() {
			String result = originTrem + ", totalFreq=" + totalFreq + "\t";
			for (int k : this.termMap.keySet()) {
				result += "(" + k + "," + this.termMap.get(k) + ")";
			}
			return result;

		}

	}

	PriorityQueue<MyTerm> sortedTermQueue = new PriorityQueue<MyTerm>(1,

	new Comparator<MyTerm>() {

		@Override
		public int compare(MyTerm o1, MyTerm o2) {
			int freq1 = o1.totalFreq;
			int freq2 = o2.totalFreq;
			if (freq1 < freq2) {
				return 1;
			} else if (freq1 > freq2) {
				return -1;
			} else {
				return 0;
			}
		}

	});

	public void getIndexInfo(String indexdir,int freqThreshold) {
		IndexReader reader = null;
		try {
			Directory dir = FSDirectory.open(new File(indexdir));
			System.out.println(dir);
			reader = IndexReader.open(dir);

			System.out.println("document num:" + reader.numDocs());
			System.out.println("======================");

			TermEnum terms = reader.terms();
			sortedTermQueue.clear();
			maxDocNum = reader.maxDoc();
			linkMap.clear();
			termList.clear();
			while (terms.next()) {
				// System.out.print(terms.term() + "\tDocFreq:" +
				TermDocs termDocs = reader.termDocs(terms.term());
				MyTerm temp = new MyTerm(terms.term(), termDocs, maxDocNum);
				if (temp.totalFreq < freqThreshold) {
					continue;
				}
				linkMap.put(temp.originTrem.text(), temp);
				sortedTermQueue.add(temp);
				termList.add(temp);
			}
			System.out.println("total Size:" + sortedTermQueue.size());
			System.out.println("mapsize:" + linkMap.keySet().size());
			// System.exit(0);
			int num = 0;

			/*while (!sortedTermQueue.isEmpty()) {
				num++;
				System.out.println(num + ":" + sortedTermQueue.poll());
			}*/
			System.out.println("read index info done");
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void generateWekaFile(ArrayList<MyTerm> myTerms, int maxDocNum,
			String wekaFilePath) throws IOException {

		String text = "@relation interest\n";
		text += "@attribute text string\n";
		for (int i = 0; i < maxDocNum; i++) {
			text += "@attribute doc" + i + "\treal\n";
		}
		text += "@data\n";
		for (int j = 0; j < myTerms.size(); j++) {
			MyTerm term = myTerms.get(j);
			String line = "";
			line += term.originTrem.text();
			for (int i = 0; i < term.vector.length; i++) {
				line += "," + term.vector[i];
			}
			line += "\n";
			text += line;
		}
		// System.out.println(text);
		PrintWriter Pout = new PrintWriter(new FileWriter(wekaFilePath));
		Pout.println(text);
		Pout.close();
	}

	public Map<Integer, ArrayList<String>> Cluster(String wekaFilePath,int clusterNum)
			throws Exception {
		File inputFile = new File(wekaFilePath);
		ArffLoader arf = new ArffLoader();
		arf.setFile(inputFile);
		Instances originIns = arf.getDataSet();

		Instances insTest = new Instances(originIns, 0, 1);
		insTest.deleteStringAttributes();
		insTest.delete();
		ArrayList<String> wordList = new ArrayList<String>();
		for (int i = 0; i < originIns.numInstances(); i++) {
			Instance tempIns = new Instance(originIns.instance(i));
			String word = tempIns.stringValue(originIns.attribute("text"));
			tempIns.deleteAttributeAt(0);
			wordList.add(word);
			insTest.add(tempIns);
		}

		SimpleKMeans sm = new SimpleKMeans();
		// EM sm=new EM();
		sm.setNumClusters(clusterNum);
		sm.setPreserveInstancesOrder(true);// very important
		sm.buildClusterer(insTest);

		int totalNum = insTest.numInstances();
		System.out.println("totalNum:" + insTest.numInstances());
		System.out.println("============================");
		Instances centreIns = sm.getClusterCentroids();
		Map<Integer, ArrayList<String>> result = new HashMap<Integer, ArrayList<String>>();
		for (int i = 0; i < centreIns.numInstances(); i++) {
			Instance ins = centreIns.instance(i);
			result.put(i, new ArrayList<String>());
			// System.out.println(i+":\t\n"+ins);
		}
		int[] assign = sm.getAssignments();
		for (int i = 0; i < assign.length; i++) {
			Instance ins = insTest.instance(i);
			// System.out.println(wordMap.containsKey(ins.toString()));
			result.get(assign[i]).add(wordList.get(i));
		}
		//System.out.println(sm.toString());

		ArrayList<String> words = new ArrayList<String>();
		// int errorNum=0;
		for (int k : result.keySet()) {
			words = result.get(k);
			System.out.println("cluster" + k + ":" + words.size() + ":\t"
					+ words.size() * 1.0 / totalNum * 100);
			for (int i = 0; i < words.size(); i++) {
				String s = words.get(i);
				assert linkMap.containsKey(s);
				/*
				 * if(!linkMap.containsKey(s)){
				 * System.out.println("XXXXXXXXXXXXXXXXXX");
				 * System.out.println(s); System.out.println(linkMap.get(s));
				 * errorNum++; continue; }
				 */
				int freq = linkMap.get(s).totalFreq;
				words.set(i, "(" + s + ":" + freq + ")");
			}
			if (result.get(k).size() < 100) {
				System.out.println(result.get(k));
			}
		}
		// System.out.println("errorNum:"+errorNum);
		return result;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SingleUserAnalyse sua = new SingleUserAnalyse("胡新辰点点点");
		LuceneAnalyser ts = new LuceneAnalyser();
		try {
			String semiFile = "C:\\Users\\Edward\\Desktop\\semi.txt";
			// PrintWriter Pout = new PrintWriter(new FileWriter(semiFile));
			// JSONObject result = sua.getIndexData();
			// Pout.println(result.toString());
			// Pout.close();

			File input = new File(semiFile);
			JSONObject js = new JSONObject(new JSONTokener(
					new FileReader(input)));

			// js=sua.weightAdjust(js);
			ts.buildIndex(js);
			ts.getIndexInfo(ts.indexDir,6);
			ts.generateWekaFile(ts.termList, ts.maxDocNum, ts.wekaFile);
			ts.Cluster(ts.wekaFile,8);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// TODO Auto-generated method stub

	}

}
