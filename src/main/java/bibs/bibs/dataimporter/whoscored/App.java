package bibs.bibs.dataimporter.whoscored;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
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


/**
 * Clase Inicial de Prueba para la prueba de la lectura del Datos del WEB y posteriormente su transformacion a JSON
 *
 */
public class App 
{
	private final static String pathFirefox = "C:\\Instalaciones\\Mozilla_27\\Mozilla Firefox\\firefox.exe";
	private static List<String> urls_matches = new ArrayList<String>();
	private static WebDriver driver;
	
	/*
	 * Instancia el Driver, Busca todos los urls especificos de cada juego a procesar, y los llama uno a uno
	 * */
	public static void main( String[] args )  throws Exception
    {
    	App app = new App();       
    	JSONArray arreglo = new JSONArray();
    	JSONObject data;  
    	
    	FirefoxProfile profile = new FirefoxProfile();
    	driver = new FirefoxDriver(new FirefoxBinary(new File(
    			pathFirefox)), profile);
        System.out.println( "Driver Instanciado!" );
    
        System.out.println( app.sendPost("https://www.whoscored.com/Matches/1076193/MatchReport").toString() );
        
        //driver.close();
        
        /*
    	getIdMatches();
    	
    	int i = 0;
    	for(String url : urls_matches) {
    		data  = app.sendPost(url);
    		arreglo.put(data);
    		
    		i++;
    		
    		if(i == 1) break; // Por razones de Pruebas, con solo procesar tres juegos queremos salir
    	}
        
    	driver.close();
    	
    	System.out.println(arreglo.toString());
    	
    	*/
    }
    
	/*
	 * Con un Url Base, se encarga de buscar todos los urls de los matchreport.
	 * Devuelve todos los urls con la siguiente estructura
	 * 
	 * 
	 https://www.whoscored.com/Matches/1076193/MatchReport/France-Ligue-1-2016-2017-Lille-Saint-Etienne
	 
	 */
	public static void getIdMatches() {
    	String url = "https://www.whoscored.com/Regions/74/Tournaments/22/Seasons/6318/Stages/13768/Fixtures/France-Ligue-1-2016-2017";
    	
    	driver.get(url);
    	
    	List<WebElement> wes = driver.findElements(By.className("match-report"));
    	
    	for(WebElement we : wes) {
    		System.out.println(we.getAttribute("href"));
    		
    		urls_matches.add(we.getAttribute("href"));
    	}
    	
    	
    }
    
	private JSONObject sendPost(final String url) throws Exception {
		//--------------------------------->>>>
    	// Obtenemos el idMath desde el URL
        Pattern intsOnly = Pattern.compile("\\d+");
        Matcher makeMatch = intsOnly.matcher(url);
        makeMatch.find();
        
        Long idMatch = Long.parseLong(makeMatch.group());
        
        //--------------------------------->>>>
    	// Vamos al URL
        driver.get(url);
        
        String newUrl = null;
        List<WebElement> weAs = driver.findElement(By.id("sub-navigation")).findElements(By.tagName("a"));
        for(WebElement a : weAs) {
        	if(a.getText().equals("Match Centre")) {
        		newUrl = a.getAttribute("href");
        	}
        }
        
        if(newUrl == null) {
        	throw new Exception("No fue encontrado el url buscado");
        }
        
        driver.get(newUrl);
        
        final String html = driver.getPageSource();
        
        JSONObject data = new JSONObject();
        
        Document document = Jsoup.parse(html);
        Element matchcentrestats = document.getElementById("match-centre-stats");
        Element shots = matchcentrestats.getElementsByAttributeValue("data-detail-for", "shotsTotal").first();
        
        Elements lis = shots.getElementsByClass("match-centre-sub-stat");
        for(Element li : lis) {
        	System.out.println("li html :: " + li.html());
    		
    		String key = li.getElementsByTag("h4").text();
    		
    		System.out.println("key " + key);
    		
    		key = key.replaceAll(" ", "");
    		key = key.replaceAll("%", "");
    		
    		System.out.println("key afeter " + key);
    		
    		Elements weValues = li.getElementsByClass("match-centre-stat-value");
    		
    		System.out.println("weValues.size " + weValues.size());
    		
    		if(weValues == null || weValues.size() != 2) {
    			throw new Exception("Estructura no esperada");
    		}
    
    		data.put(key+"L", weValues.get(0).text());
    		data.put(key+"V", weValues.get(1).text());
        }
        
        System.out.println(shots.html());
        
        
        data.put("idMatch", idMatch);
        
        return data;
	}
	
	/*
	 * Se encarga de procesar cada juego en Particular y de convertirlo en JSON.
	 * */
    private JSONObject sendPost_old(final String url) throws Exception {
    	//--------------------------------->>>>
    	// Obtenemos el idMath desde el URL
        Pattern intsOnly = Pattern.compile("\\d+");
        Matcher makeMatch = intsOnly.matcher(url);
        makeMatch.find();
        
        Long idMatch = Long.parseLong(makeMatch.group());
        
        //--------------------------------->>>>
    	// Vamos al URL
        driver.get(url);
        
        String newUrl = null;
        List<WebElement> weAs = driver.findElement(By.id("sub-navigation")).findElements(By.tagName("a"));
        for(WebElement a : weAs) {
        	if(a.getText().equals("Match Centre")) {
        		newUrl = a.getAttribute("href");
        	}
        }
        
        if(newUrl == null) {
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
        
        //--------------------------------->>>>
    	// Instanciamos el JSON
        JSONObject data = new JSONObject();
        data.put("idMatch", idMatch);
        
        
        //--------------------------------->>>>
    	// Procesamos el header, leemos desde el resultado final a los nombre de los equipos entre otros
        WebElement weHeader = driver.findElement(By.id("match-header"));
        
        int i = 1;
        for(WebElement row : weHeader.findElements(By.tagName("tr"))) {
        	//data.put("row"+i, row.getText());
        	
        	if(i == 1) {
        		String array[] = row.getText().split(":");
        		data.put("golesL", 
        				(array[0].substring(array[0].length()-3, array[0].length()-1)).trim()
        				);
        		data.put("equipoL", 
        				(array[0].substring(0, array[0].length()-4)).trim()
        				);
        		data.put("golesV", 
        				(array[1].substring(0, 2)).trim()
        				);
        		data.put("equipoV", 
        				(array[1].substring(3, array[1].length()-1)).trim()
        				);
        	} 
        	
        	if(i == 2) {
        		String array[] = row.getText().split("\n");
        		int j = 1;
        		
        		String key = "null", value = "null";
        		for(String s : array) {
        			
        			
        			if(j%2!=0) { // Es impar
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
        
        //--------------------------------->>>>
    	// Nos encargamos de leer los detalles de la pantalla negra del WhoScored
        final String xPathBase = ".//*[@id='match-centre-stats']/ul/li[?]/div[1]/ul"; //  //*[@id="match-centre-stats"]/ul/li[4]/div[1]/ul
        final String xPathBaseButton = ".//*[@id='match-centre-stats']/ul/li[?]/div[2]/span";
        String xPath;
        String xPathButton;
        for(i = 4; i <= 4; i = i + 2) {  // Con observacion notamos que cambiando de dos en dos, entramos al detalle de las estadisticas
        	xPath = xPathBase.replace("?", Integer.toString(i));
        	xPathButton = xPathBaseButton.replace("?", Integer.toString(i+1));
        	
        	System.out.println("xPath :: " + xPath);
        	System.out.println("xPathButton :: " + xPathButton);
        	
        	driver.findElement(By.xpath(xPathButton)).click();
        	System.out.println("click");
        	
        	Thread.sleep(3000);
        	
        	WebElement weDetails = driver.findElement(By.xpath(xPath));
        	
        	System.out.println("weDetails :: " + weDetails.getText() + " isDisplayed " + weDetails.isDisplayed() 
        	+ "  isEnabled() " + weDetails.isEnabled() + "   tagName " + weDetails.getTagName() + "   Attri " + weDetails.getAttribute("data-mode"));
        	
        	
        	
        	// Internamente podemos tener varios cuadritos
        	List<WebElement> lis = weDetails.findElements(By.className("match-centre-sub-stat"));
        	
        	System.out.println("lis.size :: " + lis.size());
        	
        	// Procesamos cada cuadrito
        	for(WebElement li : lis) {
        		
        		System.out.println("li " + li.getText());
        		
        		String key = li.findElement(By.tagName("h4")).getText();
        		
        		System.out.println("key " + key);
        		
        		key = key.replaceAll(" ", "");
        		key = key.replaceAll("%", "");
        		
        		System.out.println("key afeter " + key);
        		
        		List<WebElement> weValues = li.findElement(By.className("match-centre-stat-values")).
        										findElements(By.className("match-centre-stat-value"));
        		
        		System.out.println("weValues.size " + weValues.size());
        		
        		if(weValues == null || weValues.size() != 2) {
        			throw new Exception("Estructura no esperada" + li.getText());
        		}
        
        		data.put(key+"L", weValues.get(0).getText());
        		data.put(key+"V", weValues.get(1).getText());
        		
        	}
        	
        }
        
        
        return data;
    }
    
    
    	
    }
    

