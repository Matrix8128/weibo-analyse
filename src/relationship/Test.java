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

		String s="D:\\apache-tomcat-7.0.37\\webapps\\weibo-analyse\\luceneDir\\1796533527";

		File dir=new File(s);
		System.out.println(dir.exists());
		if(dir.exists()){
			System.out.println(dir.delete());
		}
		

	}

	public static void main(String[] args) throws Exception {
		String s = "1胡新chen3";

	//	System.out.println(s.length());
		new Test().temp();
	}

}
