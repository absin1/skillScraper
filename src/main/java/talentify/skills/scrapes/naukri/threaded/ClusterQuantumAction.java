/**
 * 
 */
package main.java.talentify.skills.scrapes.naukri.threaded;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
public class ClusterQuantumAction implements Runnable {
	ExecutorService executor = Executors.newFixedThreadPool(1);

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	ResultSet rs;
	DB db = new DB();

	public ClusterQuantumAction(ResultSet rs) {
		this.rs = rs;
	}

	@Override
	public void run() {
		try {
			String URL = rs.getString(2);
			int cluster_id = rs.getInt(3);
			;
			Document doc = Jsoup.connect(URL).get();
			Elements links = doc.getElementsByTag("a");
			for (Element link : links) {
				String linkURL = link.attr("abs:href");
				if (link.parent().hasClass("pagination")) {
					String insertSql = "INSERT INTO public.clusters_pagination (url, cluster_id, is_scraped) VALUES('"
							+ linkURL + "', " + cluster_id + ", false); ";
					db.runSql2(insertSql);
					getListings(linkURL, cluster_id);
				} else {
					if (linkURL.contains("job-listings")) {
						try {
							linkURL = linkURL.substring(0, linkURL.indexOf("?"));
						} catch (Exception e) {
							// TODO: handle exception
						}
						// System.out.println(linkURL);
						getListings(linkURL, cluster_id);
					}
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	public void getListings(String URL, int cluster_id) throws IOException, SQLException {
		String sql = "select * from job_listings where job_listings.url = '" + URL + "'";
		ResultSet rs = db.runSql(sql);
		if (rs.next()) {
		} else {
			String sql1 = "INSERT INTO job_listings (url) values " + "(?) returning id;";
			PreparedStatement stmt = db.conn.prepareStatement(sql1, Statement.RETURN_GENERATED_KEYS);
			stmt.setString(1, URL);
			int affectedRows = stmt.executeUpdate();
			if (affectedRows > 0) {
				ResultSet generatedKeys = stmt.getGeneratedKeys();
				if (generatedKeys.next()) {
					int listing_id = generatedKeys.getInt(1);
					String sql2 = "INSERT INTO public.listing_cluster (listing_id, cluster_id) VALUES(" + listing_id
							+ ", " + cluster_id + "); ";
					PreparedStatement stmt2 = db.conn.prepareStatement(sql2, Statement.RETURN_GENERATED_KEYS);
					stmt2.execute();
					stmt2.close();
				}
			} else {
				System.err.println("Insertion into lsiting table failed so skipping mapping entry too!");
			}
			stmt.close();
		}
		rs.close();
	}

}
