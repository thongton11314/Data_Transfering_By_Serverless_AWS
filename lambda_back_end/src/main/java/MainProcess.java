package css436lab4;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.DeleteItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import org.json.simple.JSONArray;

/**
 * Main process class which build table, load, delete, query
 * @author Thong Ton
 * @apiNote Major code's implement by AWS services
 * @version November, 29, 2021
 */
public class MainProcess {

    // Main build
    private static MainProcess BUILD;

    // AWS DynamoDB table
    private static AmazonDynamoDB CLIENT;

    // Use to work with table in AWS DynamoDB
    private Table DBTable;    
    private final int ATTRIBUTE = 0;
    private final int VALUE = 1;
        
    /**
     * Build main process. This function will be invoked at static it self
     * @param logger
     */
    private MainProcess(LambdaLogger logger) {
    	try {
	        CLIENT = AmazonDynamoDBClientBuilder
	                    .standard()
	                    .withCredentials(new DefaultAWSCredentialsProviderChain())
	                    .build();
			logger.log("Build credential successful\n");
    	} catch (Exception e) {
			logger.log("Build credential Fail\n");
    	}
    }

    /**
     * Build and return the main process, must be called at initialization phase
     * @param lambdaLogger display current working state
     * @return MainProcess
     */
    public static MainProcess build(LambdaLogger lambdaLogger) {
        if (BUILD == null) {
            BUILD = new MainProcess(lambdaLogger);
        }
        return BUILD;
    }

    /**
     * Set up AWS DynamoDB table
     * @param nameTable Table's name
     * @param logger display current working state
     */
    public void setUpTable(String nameTable, LambdaLogger logger) {
        try {
        	DBTable = new Table(CLIENT, nameTable);
			logger.log("Set up table successful\n");
		} catch (Exception e) {
			logger.log("Set up table fail\n");
		}
    }
    
    /**
     * Load the data from provided URL link into AWS DynamoDB table
     * @param dataURL link to get data
     * @param pKey primary key 
     * @param sKey secondary key
     * @param logger display current working state
     * @return True if load all data successful, otherwise false
     */
    public boolean loadData(String dataURL, String pKey, String sKey, LambdaLogger logger) {
        try {
            logger.log("Loading process...\n");
            
            // Access to the lambda by URL link
            URL dataInPage = new URL(dataURL);
            
            // Get all the data in the link
            InputStream stream = dataInPage.openStream();
            Scanner scanner = new Scanner(stream);
            logger.log("Achieving data from URL:" + dataInPage.toString() + "\n");
            logger.log("Table Name: " + DBTable.getTableName() + "\n");
            int count = 1;
            while (scanner.hasNextLine()) {
            	String curLine = scanner.nextLine();

                // Check if more data to read
                if (curLine.isEmpty()) {
                    break;
                }
                
                // AWS DynamoDB Item
                Item itemToPut = doFormatData(curLine, pKey, sKey);
                
                // Insert item into table
                DBTable.putItem(itemToPut);
                logger.log("Put item: " + itemToPut.toJSON() + "\n");
            	count++;
            }
            scanner.close();
            logger.log("Put " + count + " item(s) into AWS DynamoDB sucessfull\n");
            return true;
        } catch (Exception e) {
            logger.log(e.getMessage());
            return false;
        }
    }
    
    /**
     * Delete all the items in the AWS DynamoDB table
     * @param pKey primary key
     * @param sKey secondary key
     * @param logger display current working state
     * @return True if successful delete all data, otherwise fail
     * @see https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/GettingStarted.Java.03.html
     * @see https://codedestine.com/dynamodb-deleteitem-java/
     * @see https://stackoverflow.com/questions/30962186/how-to-delete-an-item-in-dynamodb-using-java
     */
    public boolean deleteData(String pKey, String sKey, LambdaLogger logger) {
    	try {
    		ScanRequest scanRequest = new ScanRequest().withTableName(DBTable.getTableName());
	    	ScanResult allItems = CLIENT.scan(scanRequest);
	    	logger.log("Retrieve all items to delete \n");        
	    	for (Map<String, AttributeValue> item : allItems.getItems()) {
	    		PrimaryKey key = new PrimaryKey(pKey, item.get(pKey).getS(),
												sKey, item.get(sKey).getS());
	    		DeleteItemSpec deleteItemSpec = new DeleteItemSpec().withPrimaryKey(key);
	    		DeleteItemOutcome outcome = DBTable.deleteItem(deleteItemSpec);
	    		logger.log(outcome + "\n");
	    	}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
    	return true;
    }

    /**
     * This is a helper function of loadData(). Format the raw data before adding into table
     * @param curLine current read Item
     * @param pKey primary key
     * @param sKey secondary key
     * @return an AWS DynamoDB item
     */
    private Item doFormatData(String curLine, String pKey, String sKey) {

        // Tokenize the data by space
        curLine = curLine.replaceAll("\\s+", " ");
        List<String> listOfAttribute = Arrays.asList(curLine.trim().split("\\s+"));
        List<String> rawData = new ArrayList<String>(listOfAttribute);

        // Use to achieve the formatted data
        Item formattedData = new Item();

        // Get last name and first name
        // String.withString(Key, Value)
        formattedData.withString(pKey, rawData.get(0));
        formattedData.withString(sKey, rawData.get(1));

        // Get the rest of attribute
        for (int i = 2; i < rawData.size(); i++) {

            // Tokenize key and value
            String[] attributeAndValue = rawData.get(i).trim().split("=");
            formattedData.withString(attributeAndValue[ATTRIBUTE], attributeAndValue[VALUE]);
        }
        return formattedData;
    }

    /**
     * Retrieve data by user input last name or first name, there are four options.
     * Empty names, both names, one of the names; names include last name and first name
     * @param request user request include names
     * @param pKey primary key
     * @param sKey secondary key
     * @param logger display current working state
     * @return JSON file if retrieve success, or a message when false
     */
    public String queryData(Map<String, String> request, String pKey, String sKey, LambdaLogger logger) {
        try {
            String firstName = request.get(sKey);
            String lastName = request.get(pKey);
            
	        // Both empty 
	        if ((firstName.isEmpty() || firstName.isBlank() || firstName == null || firstName.trim().equals(""))
	        	&& (lastName.isEmpty() || lastName.isBlank() || lastName == null || lastName.trim().equals(""))) {
	        	logger.log("Received empty names \n");
	        	return queryEmpty(pKey, sKey, logger);
	        }
	        
	        // First name empty, check last name
	        if (firstName.isEmpty() || firstName.isBlank() || firstName == null || firstName.trim() == "") {
	        	logger.log("Received ony last name \n");
	        	return queryLastName(pKey, sKey, lastName, logger);
	        }
	        
	        // Last name empty, check first name
	        if (lastName.isEmpty() || lastName.isBlank() || lastName == null || lastName.trim() == "") {
	        	logger.log("Received ony first name \n");
	        	return queryFirstName(pKey, sKey, firstName, logger);
	        }
	        
	        // Both names fill
	    	logger.log("Received " + lastName  + " " + firstName + "\n");
	        return queryBoth(pKey, lastName, sKey, firstName, logger);
        } catch (Exception e) {
        	return e.getMessage();
        }
    }
    
    /**
     * Retrieve data from AWS DynamoDB, user doesn't provide last name and last name. So, get All the data
     * @param pKey
     * @param sKey
     * @param logger
     * @see https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/ScanJavaDocumentAPI.html
     * @return
     */
    private String queryEmpty(String pKey, String sKey, LambdaLogger logger) {
    	try {
    		List<Object> result = new ArrayList();
	    	ScanRequest scanRequest = new ScanRequest()
	    		    .withTableName(DBTable.getTableName());
	    	ScanResult allItems = CLIENT.scan(scanRequest);
	    	logger.log("Retrieve all items \n");
	    	for (Map<String, AttributeValue> item : allItems.getItems()){
	    		result.add(mapToJson(item));
	    	}
	    	return JSONArray.toJSONString(result);
	    }
    	catch (Exception e) {
    		logger.log(e.getMessage() +"\n");
    		return "No record found";
    	}
    }
    
    /**
     * This is helper function for queryEmpty(). Map from AWS DynamoDB key to JSON object
     * @param keyValueMap
     * @return JSON object
     */
    private static Map<String, Object> mapToJson(Map<String,AttributeValue> keyValueMap){
        Map<String,Object> key = new HashMap();
        for(Map.Entry<String, AttributeValue> entry : keyValueMap.entrySet()) 
        {
            if(entry.getValue().getM() == null) {
            	key.put(entry.getKey(),entry.getValue().getS());
            }
            else {
            	key.put(entry.getKey(),mapToJson(entry.getValue().getM()));
            }
        }
        return key;
    }
    
    /**
     * Retrieve data from AWS DynamoDB by last name and first name
     * @param pKey primary key
     * @param lName last name
     * @param sKey secondary key
     * @param fName first name
     * @param logger display current state
     * @return
     */
    private String queryBoth(String pKey, String lName, String sKey, String fName, LambdaLogger logger) {
    	try {
	        Item result = DBTable.getItem(pKey, lName, sKey, fName);
	        if (result == null) {
	            logger.log("No record of " + lName + " " + fName + "\n");
	            return "No record of  " + lName + " " + fName;
	        }
	        return result.toJSON();
    	} catch (Exception e) {
    		return e.getMessage();
    	}
    }
    
    /**
     * Retrieve data from AWS DynamoDB by only last name
     * @param pKey primary key
     * @param sKey secondary key
     * @param lName last name
     * @param logger display state message
     * @return
     */
    private String queryLastName(String pKey, String sKey, String lName, LambdaLogger logger) {
    	String attribute = pKey;
        String value = ":" + lName;
        JsonArray result = new JsonArray();
        PrimaryKey currentEvaluatedKey = null;
        
        // Query all object that match the last name
        do {
        	
        	// Create object to query
	        Map<String, Object> attributeAndValues = new HashMap<>();
	        attributeAndValues.put(value, lName);
	        
	        // Create object to query in DynamoDB
	        QuerySpec query = new QuerySpec().withKeyConditionExpression(attribute + " = " + value)
	                						.withValueMap(attributeAndValues)
	                						.withExclusiveStartKey(currentEvaluatedKey);
        	
	        // Get object match in table
	        ItemCollection<QueryOutcome> queryItem;
            query = query.withConsistentRead(true);
            queryItem = DBTable.query(query);
            
            // Format Item
            JsonArray anObjectMatch = formatQueryItem(queryItem);
            
            // Add item into list of json
            result.addAll(anObjectMatch);

            // using the same key for the next search
        	// Prepare for next search
            Map<String, AttributeValue> lastEvaluatedKey = queryItem
            										.getLastLowLevelResult()
								                    .getQueryResult()
								                    .getLastEvaluatedKey();
            if (lastEvaluatedKey == null) {
            	currentEvaluatedKey = null;
            }
            else {
            currentEvaluatedKey = new PrimaryKey(pKey, lastEvaluatedKey.get(pKey).getS(),
                    							sKey, lastEvaluatedKey.get(sKey).getS());
            }
        }
        while (currentEvaluatedKey != null);
        
        // Return a list of object match with last name
    	return result.toString();
    }
    
    /**
     * Retrieve data from AWS DynamoDB by only first name
     * @param pKey primary key
     * @param sKey secondary key
     * @param fName first name
     * @param logger display state message
     * @return string of list of all people has same first name if exist, otherwise empty string
     */
    private String queryFirstName(String pKey, String sKey, String fName, LambdaLogger logger) {
    	String attribute = sKey;
        String value = ":" + fName;
        JsonArray result = new JsonArray();
        PrimaryKey currentEvaluatedKey = null;
        
        // Query all object that match the last name
        do {
        	
        	// Create object to query
	        Map<String, Object> attributeAndValues = new HashMap<>();
	        attributeAndValues.put(value, fName);
	        
	        // Create object to query in DynamoDB
	        QuerySpec query = new QuerySpec().withKeyConditionExpression(attribute + " = " + value)
	                						.withValueMap(attributeAndValues)
	                						.withExclusiveStartKey(currentEvaluatedKey);

	        // Get object match in table
	        ItemCollection<QueryOutcome> anQueryItem;
            Index onlyFirstName = DBTable.getIndex(sKey + "-" + pKey + "-index");
            anQueryItem = onlyFirstName.query(query);

            // Format Item
            JsonArray anObjectMatch = formatQueryItem(anQueryItem);
            
            // Add item into list of json
            result.addAll(anObjectMatch);

            // using the same key for the next search
        	// Prepare for next search
            Map<String, AttributeValue> lastEvaluatedKey = anQueryItem
            											.getLastLowLevelResult()
            											.getQueryResult()
            											.getLastEvaluatedKey();
            if (lastEvaluatedKey == null) {
            	currentEvaluatedKey = null;
            }
            else {
	            currentEvaluatedKey = new PrimaryKey(sKey, lastEvaluatedKey.get(sKey).getS(),
	                    							pKey, lastEvaluatedKey.get(pKey).getS());
            }
        }
        while (currentEvaluatedKey != null);
        
        // Return a list of object match with last name
    	return result.toString();
    }
    
    /**
     * This function will format item into JSON
     * @param anQueryItem
     * @return JsonArray
     */
	private JsonArray formatQueryItem(ItemCollection<QueryOutcome> queryItem) {
	    JsonParser parser = new JsonParser();
	    JsonArray result = new JsonArray();
	    for (Item anItem : queryItem) {
	    	result.add(parser.parse(anItem.toJSONPretty()).getAsJsonObject());
	    }
	    return result;
	}
}