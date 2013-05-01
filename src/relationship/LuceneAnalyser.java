package relationship;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.wltea.analyzer.lucene.IKAnalyzer;

import java.io.*;
import java.util.Comparator;
import java.util.PriorityQueue;

import weibo4j.model.Status;
import weibo4j.model.StatusWapper;
import weibo4j.org.json.JSONArray;
import weibo4j.org.json.JSONException;
import weibo4j.org.json.JSONObject;
import weibo4j.org.json.JSONTokener;

public class LuceneAnalyser {

	String indexDir = "luceneDir";
	IndexWriter indexWriter = null;

	public void buildIndex(JSONObject indexData) {

		try {
			Directory dir = FSDirectory.open(new File(indexDir));
			IKAnalyzer analyzer = new IKAnalyzer();
			analyzer.setUseSmart(true);
			IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_35,
					analyzer);
			indexWriter = new IndexWriter(dir, iwc);
			indexWriter.deleteAll();

			JSONArray statusData=indexData.getJSONArray("statusData");
			for(int i=0;i<statusData.length();i++){
				String text=statusData.getString(i);
				Document doc = new Document();
				doc.add(new Field("text", text, Field.Store.YES,
						Field.Index.ANALYZED,
						Field.TermVector.WITH_POSITIONS_OFFSETS));
				indexWriter.addDocument(doc);
			}
			
			JSONArray userData=indexData.getJSONArray("userData");
			for(int i=0;i<userData.length();i++){
				String text=userData.getString(i);
				Document doc=new Document();
				doc.add(new Field("text", text, Field.Store.YES,
						Field.Index.ANALYZED,
						Field.TermVector.WITH_POSITIONS_OFFSETS));
				indexWriter.addDocument(doc);
			}
			
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

	public void getIndexInfo() {
		this.getIndexInfo(this.indexDir);
	}

	class MyTerm {
		Term originTrem = null;
		int totalFreq = 0;

		public MyTerm(Term originTrem, int totalFreq) {
			super();
			this.originTrem = originTrem;
			this.totalFreq = totalFreq;
		}

		@Override
		public String toString() {
			return originTrem + ", totalFreq=" + totalFreq;
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

	public void getIndexInfo(String indexdir) {
		IndexReader reader = null;
		try {
			Directory dir = FSDirectory.open(new File(indexdir));
			System.out.println(dir);
			reader = IndexReader.open(dir);

			System.out.println("document num:" + reader.numDocs());
			System.out.println("======================");

			TermEnum terms = reader.terms();
			sortedTermQueue.clear();
			while (terms.next()) {
				// System.out.print(terms.term() + "\tDocFreq:" +
				// terms.docFreq());
				int freq = 0;
				TermDocs termDocs = reader.termDocs(terms.term());
				while (termDocs.next()) {
					freq += termDocs.freq();
				}
				sortedTermQueue.add(new MyTerm(terms.term(), freq));
				// System.out.println("\ttotalFreq:" + freq);
			}
			System.out.println("total Size:" + sortedTermQueue.size());
			int num = 0;
			while (!sortedTermQueue.isEmpty()) {
				num++;
				System.out.println(num+":\t"+sortedTermQueue.poll());
			}

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

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SingleUserAnalyse sua = new SingleUserAnalyse("子不语12138");
		LuceneAnalyser ts = new LuceneAnalyser();
		try {
			File input=new File("C:\\Users\\Edward\\Desktop\\semi.txt");
			JSONObject js=new JSONObject(new JSONTokener(new FileReader(input)));
			js=sua.weightAdjust(js);
			 ts.buildIndex(js);
			ts.getIndexInfo();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// TODO Auto-generated method stub

	}

}
