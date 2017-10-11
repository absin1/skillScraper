/**
 * 
 */
package main.java.talentify.skills.scrapes.naukri.unthreaded;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * @author ab
 *
 */
public class Paginator {
	static DB db = new DB();

	/**
	 * @param args
	 * @throws SQLException
	 * @throws IOException
	 */
	public static void main(String[] args) throws SQLException, IOException {
		String getAllSQL = "SELECT id, url, cluster_type, cluster_name, is_paginated FROM public.job_listing_clusters order by id asc;";
		DB db = new DB();
		ResultSet rs1 = db.runSql(getAllSQL);
		while (rs1.next()) {
			if (rs1.getBoolean(5)) {
				System.out.println("Skipping!..:" + rs1.getString(2));
			} else {
				String URL = rs1.getString(2);
				int cluster_id = rs1.getInt(1);
				String checkPaginationStatusSQL = "select * from clusters_pagination where cluster_id = " + cluster_id
						+ " order by id desc";
				ResultSet rs2 = db.runSql(checkPaginationStatusSQL);
				if (rs2.next()) {
					URL = rs2.getString(2);
					System.out.println("Resuming from!..:" + URL);
					rs2.close();
				} else {
					System.out.println("Starting!..:" + URL);
				}
				extracted(URL, cluster_id);
				String updateSQL = "UPDATE public.job_listing_clusters SET is_paginated=true WHERE id=" + cluster_id
						+ ";";
				System.err.println(updateSQL);
				db.runSql2(updateSQL);
				System.out.println("Done!..:" + URL);
			}
		}
		rs1.close();
	}

	private static void extracted(String URL, int cluster_id) throws IOException, SQLException {
		try {
			Document doc = Jsoup.connect(URL).get();
			Elements links = doc.getElementsByTag("a");
			System.out.println("Adding!..:" + URL);
			for (Element link : links) {
				String linkURL = link.attr("abs:href");
				if (link.parent().hasClass("pagination")) {
					String selectSQL = "SELECT id, url, cluster_id, is_scraped FROM public.clusters_pagination WHERE url='"
							+ linkURL + "';";
					ResultSet rs2 = db.runSql(selectSQL);
					if (rs2.next()) {

					} else {
						String insertSql = "INSERT INTO public.clusters_pagination (url, cluster_id, is_scraped) VALUES('"
								+ linkURL + "', " + cluster_id + ", false); ";
						db.runSql2(insertSql);
						extracted(linkURL, cluster_id);
					}
					rs2.close();
				}
			}
		} catch (SocketTimeoutException e) {
			e.printStackTrace();
		}
	}
}
