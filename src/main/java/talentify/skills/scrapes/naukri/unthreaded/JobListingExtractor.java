/**
 * 
 */
package main.java.talentify.skills.scrapes.naukri.unthreaded;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * @author ab
 *
 */
public class JobListingExtractor {

	/**
	 * @param args
	 * @throws SQLException
	 * @throws IOException
	 */
	public static void main(String[] args) throws SQLException, IOException {
		String selectAllSQL = "SELECT id, url, cluster_id, is_scraped FROM public.clusters_pagination order by id asc;";
		DB db = new DB();
		ResultSet rs1 = db.runSql(selectAllSQL);
		while (rs1.next()) {
			int pagination_id = rs1.getInt(1);
			String URL = rs1.getString(2);
			int cluster_id = rs1.getInt(3);
			boolean is_scraped = rs1.getBoolean(4);
			if (is_scraped) {
				System.err.println("Skipping opening pagination : " + URL);
			} else {
				try {
					Document doc = Jsoup.connect(URL).get();
					Elements links = doc.getElementsByTag("a");
					System.out.println("Getting Listing Links for pagination!..:" + URL);
					for (Element link : links) {
						String linkURL = link.attr("abs:href");
						linkURL = linkURL.substring(0, linkURL.indexOf("?"));
						if (linkURL.contains("job-listings")) {
							String checkListingSQL = "select * from job_listings where job_listings.url = '" + linkURL
									+ "'";
							ResultSet rs2 = db.runSql(checkListingSQL);
							if (rs2.next()) {
								System.err.println("Skipping listing URL " + linkURL + " for cluster: " + URL);
								int listing_id = rs2.getInt(1);
								String insertListingClusterPagination = "insert into listing_cluster (listing_id,cluster_id,pagination_id) values ("
										+ listing_id + "," + cluster_id + "," + pagination_id + ")";
								db.runSql2(insertListingClusterPagination);
							} else {
								String insertListingSQL = "INSERT INTO job_listings (url) values "
										+ "(?) returning id;";
								PreparedStatement stmt = db.conn.prepareStatement(insertListingSQL,
										Statement.RETURN_GENERATED_KEYS);
								stmt.setString(1, linkURL);
								int affectedRows = stmt.executeUpdate();
								if (affectedRows > 0) {
									ResultSet generatedKeys = stmt.getGeneratedKeys();
									if (generatedKeys.next()) {
										int listing_id = generatedKeys.getInt(1);
										String insertListingClusterPagination = "insert into listing_cluster (listing_id,cluster_id,pagination_id) values ("
												+ listing_id + "," + cluster_id + "," + pagination_id + ")";
										db.runSql2(insertListingClusterPagination);
									}
								} else {
									System.err.println(
											"Insertion into listing table failed So skipping mapping entry too!");
								}
								stmt.close();
							}
							rs2.close();
						}
					}
					String updatePaginationScrapeStatus = "UPDATE public.clusters_pagination SET is_scraped=true WHERE id="
							+ pagination_id + ";";
					db.runSql2(updatePaginationScrapeStatus);
				} catch (SocketTimeoutException e) {
					e.printStackTrace();
				}
			}
		}

	}

}
