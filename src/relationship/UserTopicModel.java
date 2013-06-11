package relationship;

import cc.mallet.util.*;
import cc.mallet.types.*;
import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.*;
import cc.mallet.topics.*;

import java.util.*;
import java.util.regex.*;
import java.io.*;

import weibo4j.org.json.*;

import segment.Segment;

import weibo4j.org.json.JSONObject;
import weka.clusterers.*;
import weka.core.*;
import weka.core.converters.*;

public class UserTopicModel {

	/**
	 * @param args
	 */
	String userData = null;
	Segment mySegmentor = null;
	ParallelTopicModel model = null;
	Alphabet dataAlphabet = null;
	TopicInferencer infer = null;
	int topicNum = 0;
	int topicMask;
	int topicBits;
	public int[][] typeTopicCounts;

	public UserTopicModel() {

	}

	public void loadInfer(File inferFile) throws Exception {
		System.out.println("loading infer....");
		this.infer = TopicInferencer.read(inferFile);
		this.dataAlphabet = this.infer.alphabet;
		this.topicNum = this.infer.numTopics;
		this.topicMask = this.infer.topicMask;
		this.topicBits = this.infer.topicBits;
		this.typeTopicCounts = this.infer.typeTopicCounts;

	}

	public void loadModel(File modleFile) throws Exception {
		System.out.println("loading model....");
		this.model = ParallelTopicModel.read(modleFile);
		this.topicNum = this.model.numTopics;
		this.dataAlphabet = this.model.getAlphabet();
	}

	/*
	 * public UserTopicModel(File modleFile) throws Exception {
	 * 
	 * System.out.println("loading model...."); this.model =
	 * ParallelTopicModel.read(modleFile); this.topicNum = this.model.numTopics;
	 * this.dataAlphabet = this.model.getAlphabet();
	 * 
	 * // File inferFile=new File(modleFile.getParentFile(),"inferFile"); //
	 * this.infer=this.model.getInferencer(); //
	 * this.infer=TopicInferencer.read(modleFile);
	 * 
	 * try {
	 * 
	 * ObjectOutputStream oos = new ObjectOutputStream(new
	 * FileOutputStream(inferFile)); oos.writeObject(infer); oos.close();
	 * 
	 * } catch (Exception e) { e.printStackTrace(); }
	 * 
	 * // this.topicNum=this.infer.numTopics; //
	 * this.dataAlphabet=this.infer.alphabet; // this.model =
	 * ParallelTopicModel.read(modleFile); }
	 */
	public String setUserData(JSONObject semiData) throws Exception {
		this.userData = "";
		JSONArray statusData = semiData.getJSONArray("statusData");
		for (int i = 0; i < statusData.length(); i++) {
			String text = statusData.getString(i);
			this.userData += text;
		}
		JSONArray userData = semiData.getJSONArray("userData");
		for (int i = 0; i < userData.length(); i++) {
			String text = userData.getString(i);
			this.userData += text;
		}
		return this.userData;
	}

	public double getWordsEntropy(Map<Integer, Integer> assign, int numTopics)
			throws Exception {

		double totalEntropy = 0;
		int totalWeight = 0;
		for (int topics : assign.keySet()) {
			totalWeight += assign.get(topics);
		}
		if (totalWeight == 0) {
			// System.out.println(x)
			// throw new Exception("zero is divied");
			totalWeight = 1;
		}
		for (int i = 0; i < numTopics; i++) {
			int weight = 0;
			if (assign.containsKey(i)) {
				weight = assign.get(i);
			}
			double prob = weight * 1.0 / totalWeight;
			double entropy = 0;
			if (prob != 0) {
				entropy = -prob * (Math.log(prob));
			}
			// System.out.println(i+":\tprob:"+prob+"\tentropy:"+entropy);
			totalEntropy += entropy;
		}
		// System.out.println("totalEntropy:"+totalEntropy);
		return totalEntropy;

	}

	private Pipe buildPipe() {
		ArrayList<Pipe> pipeList = new ArrayList<Pipe>();

		// Convert the "target" object into a numeric index
		// into a LabelAlphabet.
		pipeList.add(new Target2Label());

		// The "data" field is currently a filename. Save it as "source".
		pipeList.add(new SaveDataInSource());
		// Set "data" to the file's contents. "data" is now a String.
		pipeList.add(new Input2CharSequence("utf8"));

		Pattern tokenPattern = null;
		String pat = "[\u4E00-\u9FFFa-zA-Z-]+";
		// String pat="[^\\x00-\\xff]+";
		try {
			tokenPattern = Pattern.compile(pat);
		} catch (PatternSyntaxException pse) {
			throw new IllegalArgumentException("The token regular expression ("
					+ pat + ") was invalid: " + pse.getMessage());
		}
		// Add the tokenizer
		pipeList.add(new CharSequence2TokenSequence(tokenPattern));
		pipeList.add(new TokenSequenceLowercase());
		// So far we have a sequence of Token objects that contain
		// String values. Look these up in an alphabet and store integer IDs
		// ("features") instead of Strings.

		pipeList.add(new TokenSequence2FeatureSequence());

		return new SerialPipes(pipeList);
	}

	public InstanceList getInstanceList(File srcDir) {
		File srcFile = null;
		File tempDir = null;
		if (srcDir.isFile()) {
			String fileName = srcDir.getName();
			assert srcDir.isFile();
			tempDir = new File(srcDir.getParentFile(), "tempDir");
			tempDir.mkdir();
			srcFile = new File(tempDir, fileName);
			this.fileCopy(srcDir, srcFile);
			srcDir = tempDir;
		}

		InstanceList instances = new InstanceList(this.buildPipe());
		boolean removeCommonPrefix = true;
		instances.addThruPipe(new FileIterator(srcDir,
				FileIterator.STARTING_DIRECTORIES, removeCommonPrefix));

		if (tempDir != null) {
			tempDir.delete();
		}
		if (srcFile != null) {
			srcFile.delete();
		}
		return instances;

	}

	public void fileCopy(File srcFile, File destFile) {
		FileInputStream in = null;
		FileOutputStream out = null;
		try {
			in = new FileInputStream(srcFile);
			out = new FileOutputStream(destFile);
			byte[] buffer = new byte[1024];
			int num = 0;
			while ((num = in.read(buffer)) != -1) {
				out.write(buffer, 0, num);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				if (in != null) {
					in.close();
				}
				if (out != null) {
					out.close();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	// look up ParallelTopicModel.getSortedWords() and printTypeTopicCounts
	// model.printTypeTopicCounts(new File("D:\\typeTopicCount.txt"));
	// model.printTopWords(new File("D:\\topic-keys"), 20, false);
	public Map<String, Map<Integer, Integer>> getWordAssignment(
			ArrayList<String> wordList) throws Exception {
		// assert model != null : "don't have a model yet,please load one";

		Map<String, Map<Integer, Integer>> wordAssignment = new HashMap<String, Map<Integer, Integer>>();
		for (String word : wordList) {
			// System.out.println("the word:" + word);
			Map<Integer, Integer> assignment = new HashMap<Integer, Integer>();
			// must set the addIfNotPresent false(in lookupIndex(Object entry,
			// boolean addIfNotPresent))
			if (!this.dataAlphabet.contains(word)) {
				System.out.println("no " + word + " in alphabet");
				// wordAssignment.put(word, null);
				continue;
			}
			int wordId = this.dataAlphabet.lookupIndex(word, false);
			// System.out.println("the wordId:"+wordId);
			assert wordId != -1 : "the word:" + word + " does't exists";
			int[] topicCounts = this.typeTopicCounts[wordId];
			for (int i = 0; i < topicCounts.length && topicCounts[i] > 0; i++) {
				int topic = topicCounts[i] & this.topicMask;
				int count = topicCounts[i] >> this.topicBits;
				assignment.put(topic, count);
				// System.out.println(topic+":"+count);
			}
			wordAssignment.put(word, assignment);
		}

		return wordAssignment;
	}

	public ArrayList<ArrayList<String>> wordCluster(
			Map<String, Map<Integer, Integer>> wordAssignment, int topicNum,
			int clusterNum) throws Exception {

		ArrayList<ArrayList<String>> result = new ArrayList<ArrayList<String>>();
		String text = "@relation wordCluster\n";
		text += "@attribute text String\n";
		for (int i = 0; i < topicNum; i++) {
			text += "@attribute topic" + i + "\t real\n";
		}
		text += "@data\n";

		for (String word : wordAssignment.keySet()) {

			Map<Integer, Integer> map = wordAssignment.get(word);
			if (map == null) {
				System.out.println("sorry the word " + word + " does't exist");
				continue;
			}
			int[] assign = new int[topicNum];
			for (int index : map.keySet()) {
				assign[index] = map.get(index);
			}
			String line = "";
			line += word;
			for (int i = 0; i < assign.length; i++) {
				line += "," + assign[i];
			}
			line += "\n";
			text += line;
		}
		File wekaFile = new File("wordClusterWeka.arff");

		PrintWriter Pout = new PrintWriter(new FileWriter(wekaFile));
		Pout.println(text);
		Pout.close();

		ArffLoader arf = new ArffLoader();
		arf.setFile(wekaFile);
		Instances originIns = arf.getDataSet();
		Instances insTest = new Instances(originIns);
		insTest.deleteStringAttributes();
		int totalNum = insTest.numInstances();

		EM sm = new EM();
		sm.setNumClusters(clusterNum);

		// sm.setClusterer(em);
		sm.buildClusterer(insTest);

		System.out.println("totalNum:" + insTest.numInstances());
		System.out.println("============================");
		// System.out.println(sm.toString());
		for (int i = 0; i < clusterNum; i++) {
			result.add(new ArrayList<String>());
		}
		for (int i = 0; i < totalNum; i++) {
			weka.core.Instance ins = originIns.instance(i);
			String word = ins.stringValue(0);
			weka.core.Instance tempIns = new weka.core.Instance(ins);
			tempIns.deleteAttributeAt(0);
			int cluster = sm.clusterInstance(tempIns);
			result.get(cluster).add(word);
		}
		// show the result
		for (int i = 0; i < clusterNum; i++) {
			ArrayList<String> list = result.get(i);
			System.out.print("cluster " + i + ":");
			for (String word : list) {
				System.out.print(" " + word);
			}
			System.out.print("\n");
		}
		wekaFile.delete();
		return result;
	}

	class KeyWord {
		public KeyWord(String text, double weight) {
			super();
			this.text = text;
			this.weight = weight;
		}

		String text;
		double weight;
		double entropy;
		int cluster;
	}

	PriorityQueue<KeyWord> sortedKeyWords = new PriorityQueue<KeyWord>(1,
			new Comparator<KeyWord>() {
				@Override
				public int compare(KeyWord o1, KeyWord o2) {
					if (o1.weight < o2.weight) {
						return 1;
					} else if (o1.weight > o2.weight) {
						return -1;
					} else {
						return 0;
					}
				}
			});

	public ArrayList<String> getHighEntropyWord(
			Map<String, Map<Integer, Integer>> wordAssign, int topNum,
			boolean print) throws Exception {
		ArrayList<String> result = new ArrayList<String>();
		this.sortedKeyWords.clear();
		for (String word : wordAssign.keySet()) {

			double entropy = this.getWordsEntropy(wordAssign.get(word),
					this.topicNum);
			// System.out.println(entropy);
			// wordEntropyMap.put(word, entropy);
			this.sortedKeyWords.add(new KeyWord(word, entropy));
		}
		if (topNum < 0) {
			topNum = 0;
		}
		while (!this.sortedKeyWords.isEmpty() && topNum > 0) {
			topNum--;
			KeyWord kw = this.sortedKeyWords.poll();
			if (print) {
				System.out.println(kw.text + ":" + kw.weight);
				Map<Integer, Integer> map = wordAssign.get(kw.text);
				for (int topic : map.keySet()) {
					System.out.print(topic + ":" + map.get(topic) + "||");
				}
				System.out.println("");
			}
			result.add(kw.text);
		}
		return result;
	}

	public JSONObject getKeyWord(File segmentedFile) throws Exception {

		System.out.println("getting keywords...");
		InstanceList instances = this.getInstanceList(segmentedFile);

		// get hight freq words
		MyRankedFeature mrf = new MyRankedFeature();
		// mrf.getFreqRankVector(instances);
		// int maxWordNum=500;
		// int num = mrf.getTotalNum() > maxWordNum ? maxWordNum :
		// mrf.getTotalNum();
		// Map<String, Double> topK = mrf.getTopKMap(num, true);
		Map<String, Double> topK = mrf.quickGetRemovedLowFreq(instances, 3,
				false);
		// get word Assignment as feature for cluster
		ArrayList<String> wordList = new ArrayList<String>();
		wordList.addAll(topK.keySet());
		Map<String, Map<Integer, Integer>> assign = this
				.getWordAssignment(wordList);

		// remove words with high Entropy
		System.out.println("before:" + assign.size());
		int removeNum = wordList.size() / 5;
		ArrayList<String> highEntropyWords = this.getHighEntropyWord(assign,
				removeNum, true);
		for (String word : highEntropyWords) {
			assign.remove(word);
		}
		
		System.out.println("after:" + assign.size());

		// cluster words with weka
		JSONArray keyWordsList = new JSONArray();
		double maxFreq = 0;
		int clusterNum = 10;
		ArrayList<ArrayList<String>> clusterResult = this.wordCluster(assign,
				this.topicNum, clusterNum);
		// get the result ,every cluster provide certain protation of words
		int totalNum = assign.size()/2>100?100:assign.size()/2;
		double percent=totalNum*1.0/assign.size();
		for (int i = 0; i < clusterResult.size(); i++) {
			ArrayList<String> list = clusterResult.get(i);
			int getNum=(int) (list.size()*percent);
			this.sortedKeyWords.clear();
			for (String word : list) {
				this.sortedKeyWords.add(new KeyWord(word, topK.get(word)));
			}
			
			JSONArray cluster = new JSONArray();
			System.out.println("cluster" + i + ":");
			int count = 0;
			while (!this.sortedKeyWords.isEmpty() && getNum> 0) {
				JSONObject mem = new JSONObject();
				getNum--;
				KeyWord keyword = this.sortedKeyWords.poll();
				mem.put("text", keyword.text);
				mem.put("weight", keyword.weight);
				if (keyword.weight > maxFreq) {
					maxFreq = keyword.weight;
				}
				System.out.print("(" + keyword.text + "," + keyword.weight
						+ ")");
				cluster.put(mem);
			}
			System.out.println("");
			keyWordsList.put(cluster);
		}
		JSONObject keyWords = new JSONObject();
		keyWords.put("maxFreq", maxFreq);
		keyWords.put("totalNum", totalNum);
		keyWords.put("WordList", keyWordsList);
		return keyWords;
	}

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		long start = System.currentTimeMillis();
		UserTopicModel utm = new UserTopicModel();
		utm.loadModel(new File(
				"D:\\apache-tomcat-7.0.37\\webapps\\weibo-analyse\\resource\\weibo-XNV-removedLowFreq3-2000.model"));
		/*utm.loadInfer(new File(
				"D:\\apache-tomcat-7.0.37\\webapps\\weibo-analyse\\infer"));*/
		/*
		 * File srcFile = new File("C:\\Users\\Edward\\Desktop\\work\\周鸿祎.txt");
		 * 
		 * System.out.println("loading time:"+(System.currentTimeMillis()-start)/
		 * 1000); Segment test = new Segment(
		 * "D:\\MyEclipse\\MyEclipse 10\\Workspaces\\MALLET", "UTF8"); //
		 * 导入停用词表，第二个参数是停用词表文件编码，包中自带的是utf8编码。 test.importStopWords(
		 * "D:\\MyEclipse\\MyEclipse 10\\Workspaces\\MALLET\\chinese_stopword.dic"
		 * , "utf8"); test.importStopWords(
		 * "D:\\MyEclipse\\MyEclipse 10\\Workspaces\\MALLET\\english_stopword.dic"
		 * , "utf8"); Set<String> filter = new HashSet<String>(); //
		 * filter.add("url"); filter.add("x"); filter.add("n"); filter.add("v");
		 * test.setMinTermlength(2); test.setTypeFilter(filter, true);
		 * 
		 * File segmentedFile = new File(srcFile.getParentFile(),
		 * srcFile.getName() + "-segmented");
		 * test.segmentFile(srcFile.getAbsolutePath(),
		 * "utf8",segmentedFile.getAbsolutePath(), false);
		 */
	//	utm.infer=utm.model.getInferencer();
		//File inferFile=new File("D:\\apache-tomcat-7.0.37\\webapps\\weibo-analyse\\infer-2000");
	/*	try {

			ObjectOutputStream oos = new ObjectOutputStream(
					new FileOutputStream(inferFile));
			oos.writeObject(utm.infer);
			oos.close();

		} catch (Exception e) {
			e.printStackTrace();
		}*/
		System.out.println("segmente time:"
				+ (System.currentTimeMillis() - start) / 1000);
		// utm.getKeyWord(segmentedFile);
		utm.getKeyWord(new File(
				"D:\\apache-tomcat-7.0.37\\webapps\\weibo-analyse\\1796533527-segmentdFile"));
		System.out.println((System.currentTimeMillis() - start) / 1000);
	}

}
