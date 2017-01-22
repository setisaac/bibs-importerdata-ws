package bibs.bibs.dataimporter.whoscored;

import java.io.File;
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

import com.google.gson.JsonObject;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;


/**
 * Hello world!
 *
 */
public class App 
{
	private final static String pathFirefox = "C:\\Instalaciones\\Mozilla_27\\Mozilla Firefox\\firefox.exe";
	private static List<String> urls_matches = new ArrayList<String>();
	
	
    public static void main( String[] args )  throws Exception
    {
    	App app = new App();       
    	JSONArray arreglo = new JSONArray();
    	JSONObject data;  
    	
    	getIdMatches();
    	
    	for(String url : urls_matches) {
    		data  = app.sendPost(url);
    		arreglo.put(data);
    	}
        
    	System.out.println(arreglo.toString());
    }
    
    public static void getIdMatches() {
    	String url = "https://www.whoscored.com/Regions/74/Tournaments/22/Seasons/6318/Stages/13768/Fixtures/France-Ligue-1-2016-2017";
    	
    	FirefoxProfile profile = new FirefoxProfile();
    	WebDriver driver = new FirefoxDriver(new FirefoxBinary(new File(
    			pathFirefox)), profile);
        
    	driver.get(url);
    	
    	List<WebElement> wes = driver.findElements(By.className("match-report"));
    	
    	for(WebElement we : wes) {
    		System.out.println(we.getAttribute("href"));
    		
    		urls_matches.add(we.getAttribute("href"));
    	}
    	
    	driver.close();
    	
    }
    
    private JSONObject sendPost(final String url) throws Exception {
    	System.out.println( "Hello World!" );
        FirefoxProfile profile = new FirefoxProfile();
    	WebDriver driver = new FirefoxDriver(new FirefoxBinary(new File(
    			pathFirefox)), profile);
        System.out.println( "Instanciado!" );
        
        Pattern intsOnly = Pattern.compile("\\d+");
        Matcher makeMatch = intsOnly.matcher(url);
        makeMatch.find();
        
        Long idMatch = Long.parseLong(makeMatch.group());
        
        driver.get(url);
        
        WebElement weStatics = driver.findElement(By.id("match-report-team-statistics"));
        WebElement weHeader = driver.findElement(By.id("match-header"));
        
        String result = weHeader.getText() + " :: " + weStatics.getText();
        
        System.out.println(result);
        
        JSONObject data = new JSONObject();
        
        data.put("idMatch", idMatch);
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
        
    
        for(WebElement row : weStatics.findElements(By.className("stat"))) {
        	
        	System.out.println("---------     "  +  row.getText() );
        	
        	
        	
        	List<WebElement> valuesWE = row.findElements(By.className("stat-value"));
        	String value = valuesWE.get(0).getText() + " : " + valuesWE.get(1).getText();
        	String key = "";
        	try {
        		WebElement weKey = row.findElement(By.className("stat-label"));
        		key = weKey.getText();
        		data.put(key, value);
        	} catch( org.openqa.selenium.NoSuchElementException ex) {
        		key = "Possession";
        		data.put(key, value);
        		break;
        	}        	
        }
        
        
        driver.close();
        
        return data;
    }
    
    
    	
    }
    

