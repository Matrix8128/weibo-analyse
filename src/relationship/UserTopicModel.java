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

class UserTopicAssignment implements Serializable {
	public UserTopicAssignment(double[] topicProbabilities, String name,String label) {
		super();
		TopicProbabilities = topicProbabilities;
		this.name = name;
		this.label=label;
	}
	

	int topicNum;
	double[] TopicProbabilities;
	String name;
	String label;
}

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
		this.infer = this.model.getInferencer();
	}

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

		EM em = new EM();
		em.setNumClusters(clusterNum);
		MakeDensityBasedClusterer sm = new MakeDensityBasedClusterer();
		sm.setClusterer(em);
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
		public KeyWord(String text, double freq, double normalizedFreq) {
			super();
			this.text = text;
			this.freq = freq;
			this.normalizedFreq = normalizedFreq;
		}

		public KeyWord(String text, double weight) {
			this.text = text;
			this.weight = weight;
		}

		String text;
		double weight;
		double freq;
		double normalizedFreq;
		double entropy;
		double normalizedEntropy;
		Map<Integer, Integer> assign;

		public double computeWeight() {
			double ratio = 0.7;
			this.weight = this.normalizedFreq * ratio + (1 - ratio)
					* this.normalizedEntropy;
			return this.weight;
		}
	}

	Map<String, KeyWord> keyWordMap = new HashMap<String, KeyWord>();

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

	public ArrayList<String> getHighEntropyWord(int topNum, boolean print)
			throws Exception {
		ArrayList<String> result = new ArrayList<String>();
		this.sortedKeyWords.clear();
		for (String word : this.keyWordMap.keySet()) {
			double entropy = this.keyWordMap.get(word).entropy;
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
				Map<Integer, Integer> map = this.keyWordMap.get(kw.text).assign;
				for (int topic : map.keySet()) {
					System.out.print(topic + ":" + map.get(topic) + "||");
				}
				System.out.println("");
			}
			result.add(kw.text);
		}
		return result;
	}

	public ArrayList<String> getRemoveWords(int removeNum, boolean print) {
		ArrayList<String> result = new ArrayList<String>();
		this.sortedKeyWords.clear();
		this.sortedKeyWords.addAll(this.keyWordMap.values());
		int containNum = this.sortedKeyWords.size() - removeNum;
		if (containNum < 0) {
			containNum = 0;
		}
		while (!this.sortedKeyWords.isEmpty() && containNum > 0) {
			containNum--;
			this.sortedKeyWords.poll();
		}
		while (!this.sortedKeyWords.isEmpty()) {
			KeyWord keyword = this.sortedKeyWords.poll();
			System.out.println("(" + keyword.text + "," + keyword.freq + ","
					+ keyword.entropy + "," + keyword.weight + ")");
			result.add(keyword.text);
		}
		System.out.println("");
		return result;
	}

	public JSONObject getKeyWord(File segmentedFile) throws Exception {

		System.out.println("getting keywords...");
		InstanceList instances = this.getInstanceList(segmentedFile);

		// get hight freq words
		MyRankedFeature mrf = new MyRankedFeature();
		Map<String, Double> topK = mrf.quickGetRemovedLowFreq(instances, 3,
				false);
		this.keyWordMap.clear();
		double totalFreq = 0;
		for (double freq : topK.values()) {
			totalFreq += freq;
		}
		for (String word : topK.keySet()) {
			double freq = topK.get(word);
			this.keyWordMap
					.put(word, new KeyWord(word, freq, freq / totalFreq));
		}

		// get word Assignment as feature for cluster
		ArrayList<String> wordList = new ArrayList<String>();
		wordList.addAll(topK.keySet());
		Map<String, Map<Integer, Integer>> assign = this
				.getWordAssignment(wordList);

		// get word entropy given the wordAssignment
		for (String word : assign.keySet()) {
			this.keyWordMap.get(word).assign = assign.get(word);
			double entropy = this.getWordsEntropy(assign.get(word),
					this.topicNum);
			this.keyWordMap.get(word).entropy = entropy;
		}

		// normalize word entropy and compute the word weight
		double totalEntropy = 0;
		for (KeyWord kw : this.keyWordMap.values()) {
			totalEntropy += kw.entropy;
		}
		for (String word : this.keyWordMap.keySet()) {
			double entropy = this.keyWordMap.get(word).entropy;
			this.keyWordMap.get(word).normalizedEntropy = -1
					* (entropy / totalEntropy);
			this.keyWordMap.get(word).computeWeight();
		}

		// remove words with low weight
		int maxNum=500;
		System.out.println("before:" + assign.size());
		int removeNum = assign.size() > maxNum ? assign.size() - maxNum : assign
				.size() / 20;
		ArrayList<String> removeWords = this.getRemoveWords(removeNum, true);
		for (String word : removeWords) {
			this.keyWordMap.remove(word);
			assign.remove(word);
		}

		/**/
		System.out.println("after:" + assign.size());

		// cluster words with weka
		JSONArray keyWordsList = new JSONArray();
		double maxWeight = 0;
		int clusterNum = 7;
		int maxKeyWordNum = 70;
		ArrayList<ArrayList<String>> clusterResult = this.wordCluster(assign,
				this.topicNum, clusterNum);
		// get the result ,every cluster provide certain proportion of words
		int totalNum = assign.size() / 2 > maxKeyWordNum ? maxKeyWordNum
				: assign.size() / 2;
		double percent = totalNum * 1.0 / assign.size();
		for (int i = 0; i < clusterResult.size(); i++) {
			ArrayList<String> list = clusterResult.get(i);
			int getNum = (int) (list.size() * percent);
			this.sortedKeyWords.clear();
			for (String word : list) {
				this.sortedKeyWords.add(this.keyWordMap.get(word));
			}

			JSONArray cluster = new JSONArray();
			System.out.println("cluster" + i + ":");
			int count = 0;
			while (!this.sortedKeyWords.isEmpty() && getNum > 0) {
				JSONObject mem = new JSONObject();
				getNum--;
				KeyWord keyword = this.sortedKeyWords.poll();
				mem.put("text", keyword.text);
				mem.put("weight", keyword.weight);
				if (keyword.weight > maxWeight) {
					maxWeight = keyword.weight;
				}
				System.out.print("(" + keyword.text + "," + keyword.freq + ","
						+ keyword.entropy + "," + keyword.weight + ")");
				cluster.put(mem);
			}
			System.out.println("");
			keyWordsList.put(cluster);
		}
		JSONObject keyWords = new JSONObject();
		keyWords.put("maxFreq", maxWeight);
		keyWords.put("totalNum", totalNum);
		keyWords.put("WordList", keyWordsList);
		return keyWords;
	}

	public void SaveHotUserDistribution(File outFile) throws Exception,
			FileNotFoundException {

		ArrayList<TopicAssignment> assigns = model.getData();
		System.out.println("instanceNum:" + assigns.size());
		ArrayList<UserTopicAssignment> utaList = new ArrayList<UserTopicAssignment>();
		for (int i = 0; i < assigns.size(); i++) {
			TopicAssignment ta = assigns.get(i);
			cc.mallet.types.Instance ins = ta.instance;
			System.out.println(i + "============");
			String source = ins.getSource().toString();
			
			String name = source.substring(source.lastIndexOf("\\") + 1);
			name = name.replaceAll(".txt$|.TXT$", "");
			utaList.add(new UserTopicAssignment(model.getTopicProbabilities(i),
					name,ins.getTarget().toString()));
			// name=name.split(".txt||")
			System.out.println("name:" + name + "\tsource:" + ins.getSource()
					+ "\ttarget:" + ins.getTarget());
			System.out.println("labeling:" + ta.topicDistribution);
			for (double p : model.getTopicProbabilities(i)) {
				System.out.print(p + ",");
			}
			System.out.println("");

		}
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(
				outFile));
		oos.writeObject(utaList);
		oos.close();

	}

	public ArrayList<UserTopicAssignment> loadUserTopicDis(File utdFile)
			throws Exception {

		ObjectInputStream in = new ObjectInputStream(new FileInputStream(
				utdFile));
		ArrayList<UserTopicAssignment> utaList = (ArrayList<UserTopicAssignment>) in
				.readObject();

		/*
		 * for (UserTopicAssignment uta : utaList) {
		 * System.out.println("=====================");
		 * System.out.println(uta.name);
		 * System.out.println(uta.TopicProbabilities.length); for (double p :
		 * uta.TopicProbabilities) { System.out.print(p + ","); }
		 * System.out.println(""); }
		 */
		return utaList;
	}

	public double[] getTopicDistribution(cc.mallet.types.Instance instance) {
		// use the default setting in InferTopics.java
		int numIterations = 100;
		int thinning = 10;
		int burnIn = 10;
		int threshold = 0;
		int max = -1;

		double[] topicDistribution = this.infer.getSampledDistribution(
				instance, numIterations, thinning, burnIn);

		return topicDistribution;
	}

	public ArrayList<ArrayList<String>> UserCluster(
			Map<String, Map<Integer, Double>> userAssignment, int topicNum,
			int clusterNum) throws Exception {

		ArrayList<ArrayList<String>> result = new ArrayList<ArrayList<String>>();
		String text = "@relation userCluster\n";
		text += "@attribute user String\n";
		for (int i = 0; i < topicNum; i++) {
			text += "@attribute topic" + i + "\t real\n";
		}
		text += "@data\n";

		for (String user : userAssignment.keySet()) {

			Map<Integer, Double> map = userAssignment.get(user);
			if (map == null) {
				System.out.println("sorry the user " + user + " does't exist");
				continue;
			}
			double[] assign = new double[topicNum];
			for (int index : map.keySet()) {
				assign[index] = map.get(index);
			}
			String line = "";
			line += user;
			for (int i = 0; i < assign.length; i++) {
				line += "," + assign[i];
			}
			line += "\n";
			text += line;
		}
		File wekaFile = new File("userClusterWeka.arff");

		PrintWriter Pout = new PrintWriter(new FileWriter(wekaFile));
		Pout.println(text);
		Pout.close();

		ArffLoader arf = new ArffLoader();
		arf.setFile(wekaFile);
		Instances originIns = arf.getDataSet();
		Instances insTest = new Instances(originIns);
		insTest.deleteStringAttributes();
		int totalNum = insTest.numInstances();

		EM em = new EM();
		em.setNumClusters(clusterNum);
		MakeDensityBasedClusterer sm = new MakeDensityBasedClusterer();
		sm.setClusterer(em);
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
				System.out.print("(" + word+"),");
			}
			System.out.print("\n");
		}
		wekaFile.delete();
		return result;
	}

	public double getEucliDistance(double[] x, double[] y) throws Exception {
		double distance = 0;

		if (x.length != y.length) {
			System.out.println("x:" + x.length + ",y:" + y.length);
			throw new Exception("x and y don't have the same  length");
		}
		for (int i = 0; i < x.length; i++) {
			distance += Math.pow(x[i] - y[i], 2);
		}
		distance = Math.sqrt(distance);
		return distance;
	}

	class RecomUser {
		String name;
		double distance;

		public RecomUser(String name, double distance) {
			super();
			this.name = name;
			this.distance = distance;
		}

		double score;
	}

	PriorityQueue<RecomUser> RecomUserQueue = new PriorityQueue<RecomUser>(1,
			new Comparator<RecomUser>() {
				@Override
				public int compare(RecomUser o1, RecomUser o2) {
					if (o1.distance > o2.distance) {
						return 1;
					} else if (o1.distance < o2.distance) {
						return -1;
					} else {
						return 0;
					}
				}
			});

	public void getRecomUser(File segmentedFile) throws Exception {
		System.out.println("getting Recom users");
		InstanceList instances = this.getInstanceList(segmentedFile);

		if (instances.size() != 1) {
			throw new Exception("size of instance is not 1");
		}
		double[] userTopicDis = this.getTopicDistribution(instances.get(0));
		ArrayList<UserTopicAssignment> utaList = this
				.loadUserTopicDis(new File(
						"C:\\Users\\Edward\\Desktop\\userAssignment"));

		RecomUserQueue.clear();
		for (int i = 0; i < utaList.size(); i++) {
			UserTopicAssignment uta = utaList.get(i);
			double distance = this.getEucliDistance(userTopicDis,
					uta.TopicProbabilities);
			String name = uta.name;

			RecomUserQueue.add(new RecomUser(name, distance));
		}

		int rank = 50;
		while (!RecomUserQueue.isEmpty() && rank > 0) {
			rank--;
			RecomUser ru = RecomUserQueue.poll();
			System.out.println(ru.name + ":\t" + ru.distance);
		}
	}

	public void findUserCommunity(int commNum) throws Exception {
		ArrayList<UserTopicAssignment> utaList = this
				.loadUserTopicDis(new File(
						"C:\\Users\\Edward\\Desktop\\userAssignment"));
		Map<String, Map<Integer, Double>> userAssignment = new HashMap<String, Map<Integer, Double>>();
		for (int i = 0; i < utaList.size(); i++) {
			UserTopicAssignment uta = utaList.get(i);
			String name = uta.name;
			String label=uta.label;
			Map<Integer, Double> assign = new HashMap<Integer, Double>();
			for (int j = 0; j < uta.TopicProbabilities.length; j++) {
				assign.put(j, uta.TopicProbabilities[j]);
			}
			userAssignment.put(name+"["+label+"]", assign);
		}
		this.UserCluster(userAssignment, this.topicNum, commNum);
	}

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		long start = System.currentTimeMillis();
		UserTopicModel utm = new UserTopicModel();
		File modelFile = new File(
				"D:\\mallet-2.0.7\\13weibo-XNV\\weibo-XNV-removedLowFreq3-500.model");
		utm.loadModel(modelFile);
		// utm.SaveHotUserDistribution(new File("C:\\Users\\Edward\\Desktop\\userAssignment"));
		// utm.loadInfer(new
		// File("D:\\apache-tomcat-7.0.37\\webapps\\weibo-analyse\\resource\\weibo-XNV-removedLowFreq3-2000.infer"));
		/*utm.getRecomUser(new File(
				"D:\\mallet-2.0.7\\14AllCorpus\\train-segmented-data\\art\\JASON贾川.txt"));*/
		utm.findUserCommunity(50);
		/*
		 * utm.loadModel(new File(
		 * "D:\\apache-tomcat-7.0.37\\webapps\\weibo-analyse\\resource\\weibo-XNV-removedLowFreq3-2000.model"
		 * ));
		 */
		/*
		 * utm.loadInfer(new File(
		 * "D:\\apache-tomcat-7.0.37\\webapps\\weibo-analyse\\infer"));
		 */
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
		// utm.infer=utm.model.getInferencer();
		// File inferFile=new
		// File("D:\\apache-tomcat-7.0.37\\webapps\\weibo-analyse\\infer-2000");
		/*
		 * try {
		 * 
		 * ObjectOutputStream oos = new ObjectOutputStream( new
		 * FileOutputStream(inferFile)); oos.writeObject(utm.infer);
		 * oos.close();
		 * 
		 * } catch (Exception e) { e.printStackTrace(); }
		 */

		System.out.println((System.currentTimeMillis() - start) / 1000);
	}

}
