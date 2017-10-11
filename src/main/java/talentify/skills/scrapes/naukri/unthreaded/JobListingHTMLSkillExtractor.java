/**
 * 
 */
package main.java.talentify.skills.scrapes.naukri.unthreaded;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.flexible.standard.parser.ParseException;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * @author ab
 *
 */
public class JobListingHTMLSkillExtractor {

	/**
	 * @param args
	 * @throws SQLException
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public static void main(String[] args) throws SQLException, FileNotFoundException, IOException {
		DB db = new DB();
		String selectAllListingSQL = "SELECT id, url, is_downloaded FROM public.job_listings;";
		ResultSet rs1 = db.runSql(selectAllListingSQL);
		while (rs1.next()) {
			int listing_id = rs1.getInt(1);
			String url = rs1.getString(2);
			Boolean is_downloaded = rs1.getBoolean(3);
			if (is_downloaded) {
				File listingFile = new File("/home/ab/Documents/job_listings/listing" + listing_id + ".html");
				String skillKeywords = null;
				skillKeywords = getSkillKeywords(listingFile);
				saveSkillKeysInDB(skillKeywords, listing_id);
				System.out.println(listing_id + ">>>>" + skillKeywords);
			}
		}

	}

	private static String getSkillKeywords(File file) throws FileNotFoundException, IOException {
		String subSequence = "";
		if (!file.isDirectory()) {
			String text = new Scanner(file).useDelimiter("\\Z").next();
			Document document = Jsoup.parse(text);
			/*
			 * String innerHTML = document.text(); int startindexOf =
			 * innerHTML.indexOf("Keyskills"); if (startindexOf == -1) {
			 * startindexOf = innerHTML.indexOf("Keywords"); if (startindexOf ==
			 * -1) { startindexOf = innerHTML.indexOf("Key Skills"); } } int
			 * endIndexof = startindexOf +
			 * innerHTML.substring(startindexOf).indexOf("Contact"); if
			 * (endIndexof == -1) endIndexof = startindexOf +
			 * innerHTML.substring(startindexOf).indexOf("Desired Candidate");
			 * try { subSequence = innerHTML.substring(startindexOf,
			 * endIndexof); System.out.println(subSequence); } catch (Exception
			 * e) { // TODO: handle exception System.err.println("ERROR>>" +
			 * file.getAbsolutePath()); }
			 */
			Elements skillDivs = document.getElementsByClass("ksTags");
			for (Element skillDiv : skillDivs) {
				for (Element skilltag : skillDiv.children()) {
					subSequence += skilltag.text() + ",";
				}
			}
			if (subSequence.contains(","))
				subSequence = subSequence.substring(0, subSequence.length() - 1);
		}
		return subSequence;
	}

	private static void saveSkillKeysInDB(String skillKeywords, int listing_id) throws SQLException {
		// TODO Save the skills in the db and use the filename to attach a
		// foreign key to the job_listing entry table
		DB db = new DB();
		for (String skillWord : skillKeywords.split(",")) {
			// check if skill exists
			skillWord = skillWord.toLowerCase().trim();
			String checkSkillSQL = "SELECT id, title FROM public.skills where title = '" + skillWord + "';";
			ResultSet rs1 = db.runSql(checkSkillSQL);
			if (rs1.next()) {
				int skill_id = rs1.getInt(1);
				String skillMappingInsert = "INSERT INTO public.skills_listing (skill_id, listing_id) VALUES("
						+ skill_id + ", " + listing_id + ");";
				db.runSql2(skillMappingInsert);
				System.out.println(skillWord + " ALREADY PRESENT");
			} else {
				String skillInsertSQL = "INSERT INTO public.skills (title) VALUES('" + skillWord + "');";
				PreparedStatement stmt = db.conn.prepareStatement(skillInsertSQL, Statement.RETURN_GENERATED_KEYS);
				int affectedRows = stmt.executeUpdate();
				if (affectedRows > 0) {
					ResultSet generatedKeys = stmt.getGeneratedKeys();
					if (generatedKeys.next()) {
						int skill_id = generatedKeys.getInt(1);
						String skillMappingInsertSQL = "INSERT INTO public.skills_listing (skill_id, listing_id) VALUES("
								+ skill_id + ", " + listing_id + ");";
						db.runSql2(skillMappingInsertSQL);
					}
				} else {
					System.err.println("Insertion into skill table failed, so skipping mapping entry too!");
				}
				stmt.close();
			}
		}
	}

	private static void createLuceneIndex(String skillKeywords, File file) throws IOException, ParseException {
		Analyzer analyzer = new StandardAnalyzer();
		Directory directory = new RAMDirectory();
		// Directory directory = FSDirectory.open("/tmp/testindex");
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		IndexWriter iwriter = new IndexWriter(directory, config);
		org.apache.lucene.document.Document doc = new org.apache.lucene.document.Document();
		doc.add(new Field("skills", skillKeywords, TextField.TYPE_STORED));
		iwriter.addDocument(doc);
		iwriter.close();
		// Now search the index:
		/*
		 * DirectoryReader ireader = DirectoryReader.open(directory);
		 * IndexSearcher isearcher = new IndexSearcher(ireader); // Parse a
		 * simple query that searches for "text": QueryParser parser = new
		 * QueryParser("skills", analyzer); Query query = parser.parse("Key");
		 * ScoreDoc[] hits = isearcher.search(query, 1000).scoreDocs;
		 * Assert.assertEquals(1, hits.length); // Iterate through the results:
		 * for (int i = 0; i < hits.length; i++) {
		 * org.apache.lucene.document.Document hitDoc =
		 * isearcher.doc(hits[i].doc); Assert.assertEquals(skillKeywords,
		 * hitDoc.get("skills")); } ireader.close(); directory.close();
		 */
	}

}
