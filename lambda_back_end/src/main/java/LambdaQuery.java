package css436lab4;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import java.util.Map;

/**
 * This class will be invoke from AWS Lambda. Achieving the data from front-end then retrieve data and return to user
 * @author Thong Ton
 * @apiNote Major code's implement by AWS services
 * @version November, 29, 2021
 */
public class LambdaQuery implements RequestHandler<Map<String, String>, String> {
    
    // Table name in AWS DynamoDB
    public static final String TABLE_NAME = "Program4";
    public static final String PRIMARY_KEY = "lastName";
    public static final String SECONDARY_KEY = "firstName";
    
    @Override
    public String handleRequest(Map<String, String> request, Context context) {
    	context.getLogger().log("Lambda calls query \n");
        MainProcess process = MainProcess.build(context.getLogger());
        if (process != null) {
            process.setUpTable(TABLE_NAME, context.getLogger());       
            return process.queryData(request, 
            		PRIMARY_KEY, 
            		SECONDARY_KEY, 
					context.getLogger());
        }
        return "Fail to query the data in table";
    }
}