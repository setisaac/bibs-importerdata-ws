package bibs.bibs.dataimporter.whoscored;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Clase Inicial de Prueba para la prueba de la lectura del Datos del WEB y
 * posteriormente su transformacion a JSON
 *
 */
public class AppReports {
	private final static String pathFirefox = "C:\\Instalaciones\\Mozilla_27\\Mozilla Firefox\\firefox.exe";
	private final String WHOSCORED = "https://www.whoscored.com";
	private static List<String> urls_matches = new ArrayList<String>();
	private static WebDriver driver;
	private String incialMonth;
	private int cantMonth;
	
	public AppReports(String incialMonth, int cantMonth) {
		FirefoxProfile profile = new FirefoxProfile(); 
		driver = new FirefoxDriver(new FirefoxBinary(new File( pathFirefox)), profile);
		System.out.println( "Driver Instanciado en constructor!" );
		
		this.incialMonth = incialMonth;
		this.cantMonth = cantMonth;
		
	}

	public JSONArray getJsonArray() throws Exception {
		JSONArray arreglo = new JSONArray();
		JSONObject data;

		
		
		
		getIdMatches();
		
		//data = app.procesarMath("https://www.whoscored.com/Matches/1076193/MatchReport");
		//System.out.println(data.toString());
		
		/*
		 * FirefoxProfile profile = new FirefoxProfile(); driver = new
		 * FirefoxDriver(new FirefoxBinary(new File( pathFirefox)), profile);
		 * System.out.println( "Driver Instanciado!" );
		 * 
		 * System.out.println(
		 * app.sendPost("https://www.whoscored.com/Matches/1076193/MatchReport")
		 * .toString() );
		 */
		// driver.close();

		int i = 0; 
		  
		for(String url : urls_matches) { 
			  data = procesarMath(url);
			  arreglo.put(data);
		  
			  i++;
		  
			  if(i == 2) break; // Por razones de Pruebas, con solo procesar tres juegos queremos salir 
		}
		
		
		driver.close();
		
		return arreglo;
		  
	}
	
	/*
	 * Instancia el Driver, Busca todos los urls especificos de cada juego a
	 * procesar, y los llama uno a uno
	 */
	public static void main(String[] args) throws Exception {
		System.out.println("main begins");
		
		AppReports app = new AppReports("Aug 2016", 1);
		
		  
		  System.out.println(app.getJsonArray().toString());
		  
		  System.out.println("mains ends");
		 
	}

	/*
	 * Con un Url Base, se encarga de buscar todos los urls de los matchreport.
	 * Devuelve todos los urls con la siguiente estructura
	 * 
	 * 
	 * https://www.whoscored.com/Matches/1076193/MatchReport/France-Ligue-1-2016
	 * -2017-Lille-Saint-Etienne
	 * 
	 */
	public void getIdMatches() throws Exception {
		String url = "https://www.whoscored.com/Regions/74/Tournaments/22/Seasons/6318/Stages/13768/Fixtures/France-Ligue-1-2016-2017";

		driver.get(url);
		
		// Buscamos el mes Inicial
		WebElement div = driver.findElement(By.id("date-controller"));
		WebElement buttonPrevius = driver.findElement(By.className("previous"));
		WebElement buttonNext = driver.findElement(By.className("next"));
		String actualMonth = div.findElement(By.id("date-config-toggle-button")).getText();
		
		int i = 0;
		while(!actualMonth.equals(incialMonth)) {
			buttonPrevius.click();
			
			Thread.sleep(2000);
			
			actualMonth = div.findElement(By.id("date-config-toggle-button")).getText().trim();
			
			i++;
	
			System.out.println("actualMonth = -" + actualMonth + "-   i = " + i + "   inicialMonth -" + incialMonth + "-");
			
			if(i >= 12) {
				System.out.println("Ocurrio un evento inesperado");
				return;
			}
		}
		
		for(i = 1; i <= cantMonth; i++) {
			List<WebElement> wes = driver.findElements(By.className("match-report"));
		
			
			Document html = Jsoup.parse(driver.getPageSource());

			
			//List<WebElement> wes = driver.findElements(By.className("match-report"));
			Iterator<Element> it = html.getElementsByClass("match-report").iterator();

			while(it.hasNext()) {
				Element we = it.next();
				System.out.println(we.attr("href"));

				urls_matches.add(WHOSCORED + we.attr("href"));
			}
			
			buttonNext.click();
			
			Thread.sleep(2000);
		}
		
		/*
		
		*/
	}

	private JSONObject procesarMath(final String url) throws Exception {
		// --------------------------------->>>>
		// Obtenemos el idMath desde el URL
		Pattern intsOnly = Pattern.compile("\\d+");
		Matcher makeMatch = intsOnly.matcher(url);
		makeMatch.find();

		Long idMatch = Long.parseLong(makeMatch.group());

		Document html = getHtml(url, false, true);

		String newUrl = null;
		Elements weAs = html.getElementById("sub-navigation").getElementsByTag("a");
		Iterator<Element> it = weAs.iterator();
		Element a;
		while (it.hasNext()) {
			a = it.next();

			if (a.text().equals("Match Centre")) {
				newUrl = WHOSCORED + a.attr("href");
				break;
			}
		}

		System.out.println("href = " + (newUrl));

		Document document = getHtml(newUrl, false, false);

		JSONObject data = new JSONObject();

		data.put("idMatch", idMatch);
		
		// --------------------------------->>>>
		// Buscamos el country y la Temporada
		Element div = document.getElementById("breadcrumb-nav");
		data.put("country", div.getElementsByClass("iconize").text());
		
		String aLigaTemp = div.getElementsByTag("a").text();
		data.put("Liga", aLigaTemp.split("-")[0].trim());
		
		String temp = aLigaTemp.split("-")[1].trim().replace("/", "");
		data.put("Temporada", temp);
		
		// --------------------------------->>>>
		// Procesamos el header, leemos desde el resultado final a los nombre de
		// los equipos entre otros
		Element weHeader = document.getElementById("match-header");

		System.out.println("--------Header-------------");
		System.out.println(weHeader.html());
		System.out.println("--------FinHeader-------------");
		
		int i = 1;
		Iterator<Element> ite = weHeader.getElementsByTag("tr").iterator();
		while(ite.hasNext()) {
			Element row = ite.next();
			// data.put("row"+i, row.getText());

			if (i == 1) {
				String array[] = row.text().split(":");
				data.put("golesL", (array[0].substring(array[0].length() - 3, array[0].length() - 1)).trim());
				data.put("equipoL", (array[0].substring(0, array[0].length() - 4)).trim());
				data.put("golesV", (array[1].substring(0, 2)).trim());
				data.put("equipoV", (array[1].substring(3, array[1].length() - 1)).trim());
			}

			if (i == 2) {
				String array[] = row.text().split("\n");
				int j = 1;

				String key = "null", value = "null";
				for (String s : array) {

					if (j % 2 != 0) { // Es impar
						s = s.replace(":", "");
						key = s;
					} else {
						value = s;

						data.put(key, value);
					}

					j++;
				}
			}
			
			

			i++;
		}
		
		// Buscar el info-block que contenga "Kick off"
		
		it = document.getElementsByClass("info-block").iterator();
		while(it.hasNext()) {
			Element infoBlockE = it.next();
			if(infoBlockE.text().contains("Kick off")) {
				Elements horaFecha = infoBlockE.getElementsByTag("dd");
				data.put("fechaOriginal", horaFecha.get(1).text());
				
				String f = horaFecha.get(1).text();
				SimpleDateFormat df = new SimpleDateFormat("EEE, dd-MMM-yy", new Locale("en"));
				SimpleDateFormat df2 = new SimpleDateFormat("yyyyMMdd");
				Date d = df.parse(f);
				
				data.put("fecha", df2.format(d));
				
				data.put("hora", horaFecha.get(0).text());
			}
		}
		
		
		
		
		
		Element matchcentrestats = document.getElementById("match-centre-stats");
		
		String array[] = {"shotsTotal", "possession", "passSuccess", "dribblesWon", "aerialsWon", "tackleSuccessful", "cornersTotal", "dispossessed"};
		
		for(String s : array) {
			Element shots = matchcentrestats.getElementsByAttributeValue("data-detail-for", s).first();

			Elements lis = shots.getElementsByClass("match-centre-sub-stat");
			for (Element li : lis) {
				System.out.println("li html :: " + li.html());

				String key = li.getElementsByTag("h4").text();

				System.out.println("key " + key);

				key = key.replaceAll(" ", "");
				key = key.replaceAll("%", "");

				System.out.println("key afeter " + key);

				Elements weValues = li.getElementsByClass("match-centre-stat-value");

				System.out.println("weValues.size " + weValues.size());

				if (weValues == null || weValues.size() != 2) {
					throw new Exception("Estructura no esperada");
				}

				data.put(key + "L", weValues.get(0).text());
				data.put(key + "V", weValues.get(1).text());
			}	
		}
		
		

		//System.out.println(shots.html());

		return data;

	}

	private JSONObject sendPost(final String url) throws Exception {
		// --------------------------------->>>>
		// Obtenemos el idMath desde el URL
		Pattern intsOnly = Pattern.compile("\\d+");
		Matcher makeMatch = intsOnly.matcher(url);
		makeMatch.find();

		Long idMatch = Long.parseLong(makeMatch.group());

		// --------------------------------->>>>
		// Vamos al URL
		driver.get(url);

		String newUrl = null;
		List<WebElement> weAs = driver.findElement(By.id("sub-navigation")).findElements(By.tagName("a"));
		for (WebElement a : weAs) {
			if (a.getText().equals("Match Centre")) {
				newUrl = a.getAttribute("href");
			}
		}

		if (newUrl == null) {
			throw new Exception("No fue encontrado el url buscado");
		}

		driver.get(newUrl);

		final String html = driver.getPageSource();

		JSONObject data = new JSONObject();

		Document document = Jsoup.parse(html);
		Element matchcentrestats = document.getElementById("match-centre-stats");
		Element shots = matchcentrestats.getElementsByAttributeValue("data-detail-for", "shotsTotal").first();

		Elements lis = shots.getElementsByClass("match-centre-sub-stat");
		for (Element li : lis) {
			System.out.println("li html :: " + li.html());

			String key = li.getElementsByTag("h4").text();

			System.out.println("key " + key);

			key = key.replaceAll(" ", "");
			key = key.replaceAll("%", "");

			System.out.println("key afeter " + key);

			Elements weValues = li.getElementsByClass("match-centre-stat-value");

			System.out.println("weValues.size " + weValues.size());

			if (weValues == null || weValues.size() != 2) {
				throw new Exception("Estructura no esperada");
			}

			data.put(key + "L", weValues.get(0).text());
			data.put(key + "V", weValues.get(1).text());
		}

		System.out.println(shots.html());

		data.put("idMatch", idMatch);

		return data;
	}

	private final String USER_AGENT = "Mozilla/5.0";

	private Document getHtml(String url, Boolean writeHtml, Boolean useJsoup) throws Exception {
		Document document;

		if (useJsoup) {

			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();

			// add reuqest header
			con.setRequestMethod("POST");
			con.setRequestProperty("User-Agent", USER_AGENT);
			con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

			// Send post request
			con.setDoOutput(true);
			DataOutputStream wr = new DataOutputStream(con.getOutputStream());
			wr.flush();
			wr.close();

			int responseCode = con.getResponseCode();
			System.out.println("\nSending 'POST' request to URL : " + url);
			System.out.println("Response Code : " + responseCode);

			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();

			// print result
			if (writeHtml) {
				String sFichero = "C:\\DEVTOOLS\\fichero.txt";
				File fichero = new File(sFichero);

				if (fichero.exists()) {
					fichero.delete();
				}

				BufferedWriter bw = new BufferedWriter(new FileWriter(sFichero));

				// String linea;
				// while((linea = br.readLine()) != null)
				bw.write(response.toString());
				
				bw.close();
			}

			document = Jsoup.parse(response.toString());
		} else {
			
			//FirefoxProfile profile = new FirefoxProfile(); 
			//driver = new FirefoxDriver(new FirefoxBinary(new File( pathFirefox)), profile);
			//System.out.println( "Driver Instanciado!" );
			
			driver.get(url);
			document = Jsoup.parse(driver.getPageSource());
			
			//driver.close();
			
		}
		return document;
	}

	/*
	 * Se encarga de procesar cada juego en Particular y de convertirlo en JSON.
	 */
	private JSONObject sendPost_old(final String url) throws Exception {
		// --------------------------------->>>>
		// Obtenemos el idMath desde el URL
		Pattern intsOnly = Pattern.compile("\\d+");
		Matcher makeMatch = intsOnly.matcher(url);
		makeMatch.find();

		Long idMatch = Long.parseLong(makeMatch.group());

		// --------------------------------->>>>
		// Vamos al URL
		driver.get(url);

		String newUrl = null;
		List<WebElement> weAs = driver.findElement(By.id("sub-navigation")).findElements(By.tagName("a"));
		for (WebElement a : weAs) {
			if (a.getText().equals("Match Centre")) {
				newUrl = a.getAttribute("href");
			}
		}

		if (newUrl == null) {
			throw new Exception("No fue encontrado el url buscado");
		}

		driver.get(newUrl);

		String html = driver.getPageSource();

		System.out.println("  --------------   ");
		System.out.println(html);

		String sFichero = "C:\\DEVTOOLS\\fichero.txt";
		File fichero = new File(sFichero);

		if (fichero.exists()) {
			fichero.delete();
		}

		BufferedWriter bw = new BufferedWriter(new FileWriter(sFichero));
		bw.write(html);

		System.out.println("  --------------   ");

		System.out.println("I am waiting");
		Thread.sleep(20000);

		// --------------------------------->>>>
		// Instanciamos el JSON
		JSONObject data = new JSONObject();
		data.put("idMatch", idMatch);

		// --------------------------------->>>>
		// Procesamos el header, leemos desde el resultado final a los nombre de
		// los equipos entre otros
		WebElement weHeader = driver.findElement(By.id("match-header"));

		int i = 1;
		for (WebElement row : weHeader.findElements(By.tagName("tr"))) {
			// data.put("row"+i, row.getText());

			if (i == 1) {
				String array[] = row.getText().split(":");
				data.put("golesL", (array[0].substring(array[0].length() - 3, array[0].length() - 1)).trim());
				data.put("equipoL", (array[0].substring(0, array[0].length() - 4)).trim());
				data.put("golesV", (array[1].substring(0, 2)).trim());
				data.put("equipoV", (array[1].substring(3, array[1].length() - 1)).trim());
			}

			if (i == 2) {
				String array[] = row.getText().split("\n");
				int j = 1;

				String key = "null", value = "null";
				for (String s : array) {

					if (j % 2 != 0) { // Es impar
						s = s.replace(":", "");
						key = s;
					} else {
						value = s;

						data.put(key, value);
					}

					j++;
				}
			}

			i++;
		}

		// --------------------------------->>>>
		// Nos encargamos de leer los detalles de la pantalla negra del
		// WhoScored
		final String xPathBase = ".//*[@id='match-centre-stats']/ul/li[?]/div[1]/ul"; // //*[@id="match-centre-stats"]/ul/li[4]/div[1]/ul
		final String xPathBaseButton = ".//*[@id='match-centre-stats']/ul/li[?]/div[2]/span";
		String xPath;
		String xPathButton;
		for (i = 4; i <= 4; i = i + 2) { // Con observacion notamos que
											// cambiando de dos en dos, entramos
											// al detalle de las estadisticas
			xPath = xPathBase.replace("?", Integer.toString(i));
			xPathButton = xPathBaseButton.replace("?", Integer.toString(i + 1));

			System.out.println("xPath :: " + xPath);
			System.out.println("xPathButton :: " + xPathButton);

			driver.findElement(By.xpath(xPathButton)).click();
			System.out.println("click");

			Thread.sleep(3000);

			WebElement weDetails = driver.findElement(By.xpath(xPath));

			System.out.println("weDetails :: " + weDetails.getText() + " isDisplayed " + weDetails.isDisplayed()
					+ "  isEnabled() " + weDetails.isEnabled() + "   tagName " + weDetails.getTagName() + "   Attri "
					+ weDetails.getAttribute("data-mode"));

			// Internamente podemos tener varios cuadritos
			List<WebElement> lis = weDetails.findElements(By.className("match-centre-sub-stat"));

			System.out.println("lis.size :: " + lis.size());

			// Procesamos cada cuadrito
			for (WebElement li : lis) {

				System.out.println("li " + li.getText());

				String key = li.findElement(By.tagName("h4")).getText();

				System.out.println("key " + key);

				key = key.replaceAll(" ", "");
				key = key.replaceAll("%", "");

				System.out.println("key afeter " + key);

				List<WebElement> weValues = li.findElement(By.className("match-centre-stat-values"))
						.findElements(By.className("match-centre-stat-value"));

				System.out.println("weValues.size " + weValues.size());

				if (weValues == null || weValues.size() != 2) {
					throw new Exception("Estructura no esperada" + li.getText());
				}

				data.put(key + "L", weValues.get(0).getText());
				data.put(key + "V", weValues.get(1).getText());

			}

		}

		return data;
	}

}
