package main.java.talentify.skills.scrapes.naukri.threaded;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Test {
	public static void main(String[] args) throws SQLException {
		String sql = "SELECT id, url, cluster_type, cluster_name FROM public.job_listing_clusters; ";
		DB db = new DB();
		ResultSet runSql = db.runSql(sql);
		while (runSql.next()) {
			int cluster_id = runSql.getInt(1);
			String url = runSql.getString(2);
			String insertSql = "INSERT INTO public.clusters_pagination (url, cluster_id, is_scraped) VALUES('" + url
					+ "', "+cluster_id+", false); ";
			db.runSql2(insertSql);
		}
	}
}
