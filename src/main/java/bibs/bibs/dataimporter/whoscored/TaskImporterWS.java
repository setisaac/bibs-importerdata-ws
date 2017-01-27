package bibs.bibs.dataimporter.whoscored;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.axiom.util.UIDGenerator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.SynapseException;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.MediatorFaultHandler;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.apache.synapse.startup.Task;


public class TaskImporterWS implements Task, ManagedLifecycle { 
	private static final Log log = LogFactory.getLog(TaskImporterWS.class);
	
	private static WebDriver driver;
	
	private final String USER_AGENT = "Mozilla/5.0";
		
		private SynapseEnvironment synapseEnvironment;
		private String userAgent;
		private String injectTo;
		private String sequenceName;
		
		private final static String pathFirefox = "C:\\Instalaciones\\Mozilla_27\\Mozilla Firefox\\firefox.exe";
		private static List<String> urls_matches = new ArrayList<String>();
		
		public void execute() {
			try {
				JSONArray jsonArrayData = this.convertHtmlToJson();
				
				System.out.println(jsonArrayData);
				
				if (log.isDebugEnabled()) {
					log.debug(jsonArrayData.toString());
				}
				
				org.apache.synapse.MessageContext mc = this.synapseEnvironment.createMessageContext();
				mc.setMessageID(UIDGenerator.generateURNString());
				mc.pushFaultHandler(new MediatorFaultHandler(mc.getFaultSequence()));
				
	            //Obtengo axis2 message content
	        	org.apache.axis2.context.MessageContext axis2MessageContext = ((Axis2MessageContext) mc).getAxis2MessageContext();
	            // Getting the json payload to string
	            String jsonPayloadToString = JsonUtil.jsonPayloadToString(axis2MessageContext);
	            // Make a json object
	            JSONObject jsonBody = new JSONObject(jsonPayloadToString);
	            //agrego objecto json generado
	            jsonBody.put("report-matches", jsonArrayData);
	           	//actualizo el Payload.
	            JsonUtil.getNewJsonPayload(axis2MessageContext, jsonBody.toString(), true, true);
				
				if ("sequence".equalsIgnoreCase(this.injectTo)) {
					if ((this.sequenceName == null) || (this.sequenceName.equals(""))) {
						handleError("Sequence name not specified");
					}
					SequenceMediator seq = (SequenceMediator) this.synapseEnvironment.getSynapseConfiguration()
							.getSequence(this.sequenceName);

					if (seq != null) {
						if (log.isDebugEnabled()) {
							log.debug("injecting message to sequence : " + this.sequenceName);
						}
						mc.pushFaultHandler(new MediatorFaultHandler(mc.getFaultSequence()));
						this.synapseEnvironment.injectAsync(mc, seq);
					} else {
						handleError("Sequence: " + this.sequenceName + " not found");
					}
				}
				
			} catch (Exception e) {
				if (log.isErrorEnabled()) {
					log.error(e);
				}
			}
			
		}
		
		
	    private JSONArray convertHtmlToJson() throws Exception {
	    	JSONArray arreglo = new JSONArray();
	    	JSONObject data;  
	    	
	    	FirefoxProfile profile = new FirefoxProfile();
	    	driver = new FirefoxDriver(new FirefoxBinary(new File(
	    			pathFirefox)), profile);
	    	
	    	getIdMatches();
	    	
	    	for(String url : urls_matches) {
	    		data  = TaskImporterWS.sendPost(url);
	    		arreglo.put(data);
	    	}
	    	
	    	driver.close();
	    	
			return arreglo;
		}
	    
	    public static void getIdMatches() {
	    	String url = "https://www.whoscored.com/Regions/74/Tournaments/22/Seasons/6318/Stages/13768/Fixtures/France-Ligue-1-2016-2017";
	    	
	    	
	        
	    	driver.get(url);
	    	
	    	List<WebElement> wes = driver.findElements(By.className("match-report"));
	    	
	    	for(WebElement we : wes) {
	    		System.out.println(we.getAttribute("href"));
	    		
	    		urls_matches.add(we.getAttribute("href"));
	    	}
	    	
	    	//driver.close();
	    	
	    }


		private static JSONObject sendPost(final String url) throws Exception {
	    	/*System.out.println( "Hello World!" );
	        FirefoxProfile profile = new FirefoxProfile();
	    	WebDriver driver = new FirefoxDriver(new FirefoxBinary(new File(
	    			pathFirefox)), profile);
	        System.out.println( "Instanciado!" );*/
	        
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
	        
	        
	        //driver.close();
	        
	        return data;
	    }
		
		private void handleError(String msg) {
			log.error(msg);
			throw new SynapseException(msg);
		}
		

		public void destroy() {
			// TODO Auto-generated method stub
			
		}

		public String getUserAgent() {
			return userAgent;
		}

		public void setUserAgent(String userAgent) {
			this.userAgent = userAgent;
		}
		
		public SynapseEnvironment getSynapseEnvironment() {
			return synapseEnvironment;
		}

		public void setSynapseEnvironment(SynapseEnvironment synapseEnvironment) {
			this.synapseEnvironment = synapseEnvironment;
		}

		public void init(SynapseEnvironment arg0) {
			this.synapseEnvironment = arg0;
		}

		public void setInjectTo(String injectTo) {
			this.injectTo = injectTo;
		}

		public void setSequenceName(String sequenceName) {
			this.sequenceName = sequenceName;
		}

}
