package relationship;
import java.util.*;
import java.io.*;

public class GetAccessToken {
	public String ReadToken(String URL) {
		String temp = null;
		try {
			BufferedReader br = new BufferedReader(
					new FileReader(new File(URL)));
			temp = br.readLine();
			br.close();
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return temp;
	}

	public  void ResetToken(String URL) {
		String temp = null;
		String listtemp = null;
		List list = new ArrayList();
		try {
			BufferedReader br = new BufferedReader(
					new FileReader(new File(URL)));
			temp = br.readLine();
			while ((listtemp = br.readLine()) != null) {
				list.add(listtemp);
			}
			list.add(temp);
			//System.out.println(list.size());
			br.close();
			BufferedWriter wr = new BufferedWriter(
					new FileWriter(new File(URL)));
			for (int i = 0; i < list.size(); i++) {
				wr.write(list.get(i) + "\n");
			}
			wr.close();
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
