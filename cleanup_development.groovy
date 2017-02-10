def props = null
def cf_template_name = null
def cf_template_name_prefix = null

def email_list = null
def slack_channel = null

def bucket = 'proteus-apcapdevelopment-us-east-1-objects'

try {
    
   stage('cleanup') {    
       node('unixaws') {
           email_list = env.PROTEUS_EMAIL_LIST
           slack_channel = env.PROTEUS_SLACK_CHANNEL
       }
       
           node('unixaws') {
        	   dir(env.UNIX_WORKSPACE + '/proteus/jobs/source control/app code dev') {
                   props = readProperties file: 'Config/Dev/proteus-dev-us-east-1.properties'
                   cf_template_name = props.SystemName + '-' + props.EnvironmentType + '-' + props.AccountName + '-' + props.Region + '-' + props.IndexName + '-v01'
                   cf_template_name_prefix = props.SystemName + '-' + props.EnvironmentType + '-' + props.AccountName + '-' + props.Region + '-' + props.IndexName + '-v'
               }
               
               build job: 'shared/jobs/build/cleanup previous', parameters: [string(name: 'PublicBucket', value: bucket), string(name: 'CF_TEMPLATE_NAME', value: cf_template_name), string(name: 'CF_TEMPLATE_NAME_PREFIX', value: cf_template_name_prefix), string(name: 'slack_channel', value: slack_channel)]
           }
       
           node('unixaws') {
        	   dir(env.UNIX_WORKSPACE + '/proteus/jobs/source control/app code dev') {
        		   props = readProperties file: 'Config/Dev/proteus-ops-us-east-1.properties'
                   cf_template_name = props.SystemName + '-' + props.EnvironmentType + '-' + props.AccountName + '-' + props.Region + '-' + props.IndexName + '-v01'
                   cf_template_name_prefix = props.SystemName + '-' + props.EnvironmentType + '-' + props.AccountName + '-' + props.Region + '-' + props.IndexName + '-v'
               }
               
               build job: 'shared/jobs/build/cleanup previous', parameters: [string(name: 'PublicBucket', value: bucket), string(name: 'CF_TEMPLATE_NAME', value: cf_template_name), string(name: 'CF_TEMPLATE_NAME_PREFIX', value: cf_template_name_prefix), string(name: 'slack_channel', value: slack_channel)]
           }   

           node('unixaws') {
        	   dir(env.UNIX_WORKSPACE + '/proteus/jobs/source control/app code dev') {
        		   props = readProperties file: 'Config/Dev/proteus-dvx-us-east-1.properties'
                   cf_template_name = props.SystemName + '-' + props.EnvironmentType + '-' + props.AccountName + '-' + props.Region + '-' + props.IndexName + '-v01'
                   cf_template_name_prefix = props.SystemName + '-' + props.EnvironmentType + '-' + props.AccountName + '-' + props.Region + '-' + props.IndexName + '-v'
               }
               
               build job: 'shared/jobs/build/cleanup previous', parameters: [string(name: 'PublicBucket', value: bucket), string(name: 'CF_TEMPLATE_NAME', value: cf_template_name), string(name: 'CF_TEMPLATE_NAME_PREFIX', value: cf_template_name_prefix), string(name: 'slack_channel', value: slack_channel)]
           }    
    }
    slackSend channel: slack_channel, message: 'Build Complete- ' + env.JOB_NAME + " " + env.BUILD_NUMBER
    
} catch (exc) {
    
    mail subject: env.JOB_NAME + " " + env.BUILD_NUMBER + " failure.",
            body: "Build url " + env.BUILD_URL + "\n\n" + exc,
              to: email_list
              
    slackSend channel: slack_channel, message: 'Build Error- ' + env.JOB_NAME + " " + env.BUILD_NUMBER + "\n\nBuild url " + env.BUILD_URL + "\n\n" + exc     

    throw exc        
}