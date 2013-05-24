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

import weibo4j.model.WeiboException;
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

	public LuceneAnalyser(String indexDir, String wekaFile) {
		super();
		this.indexDir = indexDir;
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

	Comparator MyTermCompare = new Comparator<MyTerm>() {

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

	};

	PriorityQueue<MyTerm> sortedTermQueue = new PriorityQueue<MyTerm>(1,
			MyTermCompare);

	int maxFreq=0;
	public void getIndexInfo(String indexdir, int freqThreshold) {
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
				}/*
				 * if(temp.originTrem.text().length()==1){ continue; }
				 */
				linkMap.put(temp.originTrem.text(), temp);
				sortedTermQueue.add(temp);
				termList.add(temp);
			}
			System.out.println("total Size:" + sortedTermQueue.size());
			System.out.println("mapsize:" + linkMap.keySet().size());
			// System.exit(0);
			int num = 0;
			this.maxFreq=sortedTermQueue.peek().totalFreq;
			while (!sortedTermQueue.isEmpty()) {
				num++;
				System.out.println(num + ":" + sortedTermQueue.poll());
			}
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
	
	public JSONArray Cluster(String wekaFilePath, int clusterNum)
			throws Exception {
		File inputFile = new File(wekaFilePath);
		ArffLoader arf = new ArffLoader();
		arf.setFile(inputFile);
		Instances originIns = arf.getDataSet();
		Instances insTest = new Instances(originIns);
		insTest.deleteStringAttributes();
		int totalNum = insTest.numInstances();

		// SimpleKMeans sm = new SimpleKMeans();
		EM em = new EM();
		em.setNumClusters(clusterNum);
		MakeDensityBasedClusterer sm = new MakeDensityBasedClusterer();
		sm.setClusterer(em);
		sm.buildClusterer(insTest);

		System.out.println("totalNum:" + insTest.numInstances());
		System.out.println("============================");
		System.out.println(sm.toString());
		Map<Integer, ArrayList<String>> result = new HashMap<Integer, ArrayList<String>>();
		for (int i = 0; i < clusterNum; i++) {
			result.put(i, new ArrayList<String>());
		}

		for (int i = 0; i < totalNum; i++) {
			Instance ins = originIns.instance(i);
			String word = ins.stringValue(0);
			Instance tempIns = new Instance(ins);
			tempIns.deleteAttributeAt(0);
			int cluster = sm.clusterInstance(tempIns);
			result.get(cluster).add(word);

		}

		// print the result
		ArrayList<String> words = new ArrayList<String>();
		JSONArray keyWords = new JSONArray();
		for (int k : result.keySet()) {
			words = result.get(k);
			PriorityQueue<MyTerm> clusterQueue = new PriorityQueue<MyTerm>(1,
					MyTermCompare);
			for (int i = 0; i < words.size(); i++) {
				String s = words.get(i);
				assert linkMap.containsKey(s);
				int freq = linkMap.get(s).totalFreq;
				clusterQueue.add(linkMap.get(s));
				words.set(i, "(" + s + ":" + freq + ")");
			}

			JSONArray clusterArray = new JSONArray();
			int num = clusterQueue.size() / 10 + 1;// 5%
			int totalFreq = 0;
			int totalLength = 0;
			for (int i = 0; i < num && !clusterQueue.isEmpty();) {
				JSONObject mem = new JSONObject();
				MyTerm myTerm = clusterQueue.poll();
				String word = myTerm.originTrem.text();
				if (word.length() == 1) {
					continue;
				}
				mem.put("text", word);
				mem.put("freq", myTerm.totalFreq);
				clusterArray.put(mem);
				i++;
				totalFreq += myTerm.totalFreq;
				totalLength += word.length();
			}

			double averFreq = totalFreq * 1.0 / num;
			double averLength = totalLength * 1.0 / num;
			int count = 0;
			while (!clusterQueue.isEmpty() && count < num) {
				MyTerm myTerm = clusterQueue.poll();
				String word = myTerm.originTrem.text();
				int freq = myTerm.totalFreq;
				int times = (int) (word.length() / averFreq) + 1;
				if (freq > averFreq / times) {
					JSONObject mem = new JSONObject();
					mem.put("text", word);
					mem.put("freq", freq);
					mem.put("extra", true);
					clusterArray.put(mem);
				}
			}

			keyWords.put(clusterArray);
			System.out.println("cluster" + k + ":" + words.size() + ":\t"
					+ (int) (words.size() * 1.0 / totalNum * 100));
			if (result.get(k).size() < 100) {
				System.out.println(result.get(k));
			}
		}
		// System.out.println("errorNum:"+errorNum);
		return keyWords;
	}

	private void myDelete(String path) throws Exception {
		File f = new File(path);
		if (!f.exists()) {
			return ;
		}
		if (f.isDirectory()) {
			if (f.listFiles().length == 0) {
				f.delete();
			} else {
				File delFile[] = f.listFiles();
				int i = delFile.length;
				for (int j = 0; j < i; j++) {
					if (delFile[j].isDirectory()) {
						myDelete(delFile[j].getAbsolutePath());
					} else {
						delFile[j].delete();
					}
				}
				f.delete();
			}
		} else {
			f.delete();
		}

	}
	@SuppressWarnings("finally")
	public JSONObject getKeyWords(JSONObject semiData) {
		JSONObject keyWords = new JSONObject();
		
		try {
			this.buildIndex(semiData);
			this.getIndexInfo(this.indexDir, 4);
			this.generateWekaFile(this.termList, this.maxDocNum, this.wekaFile);
			JSONArray array = this.Cluster(this.wekaFile, 7);
			int totalNum = 0;
			for (int i = 0; i < array.length(); i++) {
				totalNum += array.getJSONArray(i).length();
			}
			keyWords.put("maxFreq",this.maxFreq);
			keyWords.put("totalNum", totalNum);
			keyWords.put("WordList", array);
		} catch (WeiboException e) {
			System.out.print("getKeyWords++++++weibo\n");
			System.out.print("error:" + e.getError() + "toString:"
					+ e.toString());
			keyWords.put("error", e.getError());
			e.printStackTrace();
		} catch (Exception e) {
			System.out.print("getKeyWords++++++Exception");
			keyWords.put("error", e.toString());
			e.printStackTrace();
		} finally {
			try {
				this.myDelete(this.indexDir);
				this.myDelete(this.wekaFile);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return keyWords;
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SingleUserAnalyse sua = new SingleUserAnalyse("胡新辰点点点","usr.txt");
		LuceneAnalyser ts = new LuceneAnalyser();
		try {
			String semiFile = "C:\\Users\\Edward\\Desktop\\semi.txt";
			String resultFile = "C:\\Users\\Edward\\Desktop\\result.txt";
			// PrintWriter Pout = new PrintWriter(new FileWriter(semiFile));
			// JSONObject semiData = sua.getIndexData();
			// Pout.println(semiData.toString());
			// Pout.close();

			File input = new File(semiFile);
			JSONObject js = new JSONObject(new JSONTokener(
					new FileReader(input)));

			// js=sua.weightAdjust(js);
			JSONObject result = ts.getKeyWords(js);
			PrintWriter resultOut = new PrintWriter(new FileWriter(resultFile));
			resultOut.println(result.toString());
			resultOut.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// TODO Auto-generated method stub

	}

}
