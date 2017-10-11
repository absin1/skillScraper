/**
 * 
 */
package main.java.talentify.skills.scrapes.naukri.threaded;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * @author ab
 *
 */
public class Ripper {
	ExecutorService executor = Executors.newFixedThreadPool(1);

	/**
	 * @param args
	 * @throws SQLException
	 * @throws IOException
	 */
	public static void main(String[] args) throws SQLException, IOException {
		(new Ripper()).browseClusters();

	}

	private void browseClusters() throws SQLException, IOException {
		DB db = new DB();
		String sql = "SELECT id, url, cluster_type, cluster_name FROM public.job_listing_clusters order by id asc; ";
		ResultSet rs = db.runSql(sql);
		while (rs.next()) {
			try {
				String sql2 = "SELECT id, url, cluster_id, is_scraped FROM public.clusters_pagination where cluster_id = "
						+ rs.getInt(1) + ";";
				ResultSet rs2 = db.runSql(sql2);
				while (rs2.next()) {
					String URL = rs2.getString(2);
					if (rs2.getBoolean(4)) {
						System.out.println("Skipping : " + URL);
					} else {
						System.out.println("Doing...:" + URL);
						executor.execute(new ClusterQuantumAction(rs2));
						String sql3 = "UPDATE public.clusters_pagination SET is_scraped='true' WHERE id="
								+ rs2.getInt(1) + "; ";
						db.runSql(sql3);
						System.out.println("Done!..:" + URL);
					}
				}
				rs2.close();
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
		rs.close();
	}

}
