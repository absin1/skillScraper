package main.java.talentify.skills.scrapes.naukri.unthreaded;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Test {
	public static void main(String[] args) throws SQLException {
		// clusterPaginationEntry();
		jobListingURLCleanUp();
	}

	private static void jobListingURLCleanUp() throws SQLException {
		String selectAllListingSQL = "SELECT id, url,is_downloaded FROM public.job_listings order by id asc;";
		DB db = new DB();
		ResultSet rs1 = db.runSql(selectAllListingSQL);
		while (rs1.next()) {
			int listing_id = rs1.getInt(1);
			String listingURL = rs1.getString(2);
			System.err.print(listingURL+"----->");
			try{
				listingURL = listingURL.substring(0, listingURL.indexOf("?"));
			} catch (Exception e) {
				System.out.print("~~~~No ? present~~~~");
			}
			System.out.println(listingURL);
			String updateListingSQL = "UPDATE public.job_listings SET url='" + listingURL + "' WHERE id=" + listing_id
					+ ";";
			db.runSql2(updateListingSQL);
		}
	}

	private static void clusterPaginationEntry() throws SQLException {
		String sql = "SELECT id, url, cluster_type, cluster_name FROM public.job_listing_clusters; ";
		DB db = new DB();
		ResultSet runSql = db.runSql(sql);
		while (runSql.next()) {
			int cluster_id = runSql.getInt(1);
			String url = runSql.getString(2);
			String insertSql = "INSERT INTO public.clusters_pagination (url, cluster_id, is_scraped) VALUES('" + url
					+ "', " + cluster_id + ", false); ";
			db.runSql2(insertSql);
		}
	}
}
