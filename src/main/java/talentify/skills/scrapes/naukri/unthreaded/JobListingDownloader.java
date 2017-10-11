/**
 * 
 */
package main.java.talentify.skills.scrapes.naukri.unthreaded;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 * @author ab
 *
 */
public class JobListingDownloader {

	/**
	 * @param args
	 * @throws SQLException
	 * @throws IOException
	 */
	public static void main(String[] args) throws SQLException, IOException {
		String selectAllListingSQL = "SELECT id, url,is_downloaded FROM public.job_listings order by id asc;";
		DB db = new DB();
		ResultSet rs1 = db.runSql(selectAllListingSQL);
		while (rs1.next()) {
			int listing_id = rs1.getInt(1);
			String listingURL = rs1.getString(2);
			Boolean is_downlaoded = rs1.getBoolean(3);
			if (is_downlaoded) {
				System.err.println("Skipping downloading " + listingURL);
			} else {
				System.out.println("Downloading " + listingURL);
				try {
					Response execute = Jsoup.connect(listingURL).execute();
					String html = execute.body();
					String pathname = "/home/ab/Documents/job_listings/listing" + listing_id + ".html";
					File f = new File(pathname);
					f.getParentFile().mkdirs();
					f.createNewFile();
					BufferedWriter out = new BufferedWriter(
							new FileWriter("/home/ab/Documents/job_listings/listing" + listing_id + ".html"));
					out.write(html);
					if (out != null)
						out.close();
					String updateListingSQL = "UPDATE public.job_listings SET is_downloaded=true WHERE id=" + listing_id
							+ ";";
					db.runSql2(updateListingSQL);
				} catch (SocketTimeoutException e) {
					e.printStackTrace();
				}
			}
		}

	}

}
