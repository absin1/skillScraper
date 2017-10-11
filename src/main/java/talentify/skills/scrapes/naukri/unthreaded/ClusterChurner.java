package main.java.talentify.skills.scrapes.naukri.unthreaded;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ClusterChurner {
	public static void main(String[] args) throws SQLException, IOException {
		(new ClusterChurner()).browseClusters();
	}

	private void browseClusters() throws SQLException, IOException {
		DB db = new DB();
		String sql = "SELECT id, url, cluster_type, cluster_name FROM public.job_listing_clusters order by id asc; ";
		ResultSet rs = db.runSql(sql);
		while (rs.next()) {
			try {
				String sql2 = "SELECT id, url, cluster_id, is_scraped FROM public.clusters_pagination where cluster_id = "
						+ rs.getInt(1) + " order by id asc; ";
				ResultSet rs2 = db.runSql(sql2);
				while (rs2.next()) {
					String URL = rs2.getString(2);
					if (rs2.getBoolean(4)) {
						System.out.println("Skipping : " + URL);
					} else {
						System.out.println("Doing...:" + URL);
						int cluster_id = rs2.getInt(3);
						Document doc = Jsoup.connect(URL).get();
						Elements links = doc.getElementsByTag("a");
						for (Element link : links) {
							String linkURL = link.attr("abs:href");
							if (link.parent().hasClass("pagination")) {
								String selectSQL = "SELECT id, url, cluster_id, is_scraped FROM public.clusters_pagination WHERE url="
										+ linkURL + ";";
								ResultSet rs3 = db.runSql(selectSQL);
								if (rs3.next()) {

								} else {
									String insertSql = "INSERT INTO public.clusters_pagination (url, cluster_id, is_scraped) VALUES('"
											+ linkURL + "', " + cluster_id + ", false); ";
									db.runSql2(insertSql);
									addListings(linkURL, cluster_id);
								}
								rs3.close();
							} else {
								if (linkURL.contains("job-listings")) {
									try {
										linkURL = linkURL.substring(0, linkURL.indexOf("?"));
									} catch (Exception e) {
										// TODO: handle exception
									}
									addListings(linkURL, cluster_id);
								}
							}
						}
						String sql3 = "UPDATE public.clusters_pagination SET is_scraped='true' WHERE id="
								+ rs2.getInt(1) + "; ";
						db.runSql(sql3);
						System.err.println("Done!..:" + URL);
					}
				}
				rs2.close();
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
		rs.close();
	}

	public void addListings(String URL, int cluster_id) throws IOException, SQLException {
		DB db = new DB();
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
