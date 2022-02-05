package css436lab4;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

/**
 * This class will be invoke from AWS Lambda. Delete all the data in AWS DynamoDB
 * @author Thong Ton
 * @apiNote Major code's implement by AWS services
 * @version November, 29, 2021
 */
public class LambdaDelete implements RequestHandler<Object, String> {
    
    // Table name in AWS DynamoDB
    public static final String TABLE_NAME = "Program4";
    public static final String PRIMARY_KEY = "lastName";
    public static final String SECONDARY_KEY = "firstName";
    
    @Override
    public String handleRequest(Object thisNull, Context context) {
    	context.getLogger().log("Lambda calls delete \n");
        MainProcess process = MainProcess.build(context.getLogger());
        if (process != null) {
            process.setUpTable(TABLE_NAME, context.getLogger());       
            boolean isDelete = process.deleteData(PRIMARY_KEY, SECONDARY_KEY, context.getLogger());
            if (isDelete) {
            	return "Delete all data successfull";
            }
            else {
            	return "Delete all data fail";
            }
        }
        return "Fail to delete the data in table";
    }
}