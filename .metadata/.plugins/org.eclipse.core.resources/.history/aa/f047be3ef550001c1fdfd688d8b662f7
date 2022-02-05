package css436lab4;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

/**
 * This class will be invoke from AWS Lambda. Load the data from URL into AWS DynamoDB
 * @author Thong Ton
 * @apiNote Major code's implement by AWS services
 * @version November, 29, 2021
 */
public class LambdaLoad implements RequestHandler<Object, String> {
	
    // Link of initial data use to query to AWS DynamoDB
    public static final String DATA_URL = "https://s3-us-west-2.amazonaws.com/css490/input.txt";
    
    // Table name in AWS DynamoDB
    public static final String TABLE_NAME = "Program4";
    public static final String PRIMARY_KEY = "lastName";
    public static final String SECONDARY_KEY = "firstName";
    
    @Override
    public String handleRequest(Object nullObject, Context context) {
        context.getLogger().log("Lambda calls load \n");
        
        MainProcess process = MainProcess.build(context.getLogger());

        if (process != null) {
            process.setUpTable(TABLE_NAME, context.getLogger());
            
            boolean isLoad = process.loadData(DATA_URL, 
            								PRIMARY_KEY, 
            								SECONDARY_KEY, 
            								context.getLogger());

            // Invoke AWS lambda query function in here
            if (isLoad) {
                return "Load Successful";
            }
        }
        return "Load Fail";
    }
}