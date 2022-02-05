import json
import boto3
from boto3.dynamodb.conditions import Key, Attr

def lambda_handler(event, context):
    
    try:
        dynamodb = boto3.resource('dynamodb')
        table = dynamodb.Table('Program4')
        response = table.scan()
        data = response['Items']
        
        while 'LastEvaluatedKey' in response:
            response = table.scan(ExclusiveStartKey=response['LastEvaluatedKey'])
            data.extend(response['Items'])
    except:
        return json.dumps('Something wrong with table')
    
    if (len(data) <= 0):
        return {
                'statusCode': 200,
                'headers': {'Content-Type': 'application/json'},
                'body' : json.dumps('There is no data')
                }
    return {
            'statusCode': 200,
            'headers': {'Content-Type': 'application/json'},
            'body': json.dumps(str(data))
        }