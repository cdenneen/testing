def props = null
def staticprops = null
def cf_template_name = null
def cf_static_template_name = null
def cf_template_name_prefix = null

def stackprops = null

def email_list = null
def slack_channel = null

def bucket = 'proteus-apcapdevelopment-us-east-1-objects'
def index_bucket = 'proteus-ops-apcapdevelopment-us-east-1-data'
def static_environment = null

try {
    
   node('unixaws') {
        email_list = env.PROTEUS_EMAIL_LIST
        slack_channel = env.PROTEUS_SLACK_CHANNEL
   }
   
   slackSend channel: slack_channel, message: 'Build Started - ' + env.JOB_NAME + " " + env.BUILD_NUMBER + " " + env.BUILD_URL
   stage('fetch code') {
        parallel(infracode: {
            node('unixaws') {
                
                dir(env.UNIX_WORKSPACE + '/proteus/jobs/source control/infrastructure code dev') {
                    stash name: "infra-stash"
                }
                print "DEBUG: parameter build number = " +  env.BUILD_NUMBER
            }
        }, appcode: {
            node('unixaws') {
            	
                build 'proteus/jobs/source control/app code master'

                dir(env.UNIX_WORKSPACE + '/proteus/jobs/source control/app code master') {
                    stash name: "app-stash"
                    props = readProperties file: 'Config/Dev/proteus-ops-us-east-1.properties'
                    cf_template_name = props.SystemName + '-' + props.EnvironmentType + '-' + props.AccountName + '-' + props.Region + '-' + props.IndexName + '-v' + env.BUILD_NUMBER
            		cf_static_template_name = props.SystemName + '-' + props.EnvironmentType + '-' + props.AccountName + '-' + props.Region + '-' + props.IndexName + '-static-artifacts'
                    
                    cf_template_name_prefix = props.SystemName + '-' + props.EnvironmentType + '-' + props.AccountName + '-' + props.Region + '-' + props.IndexName + '-v'
                }
                
                build job: 'shared/jobs/deploy/unix s3 upload folder', parameters: [string(name: 'bucket', value: bucket), string(name: 'local_folder', value: env.UNIX_WORKSPACE + '/proteus/jobs/source control/infrastructure code dev/cloudformation'), string(name: 'key', value: cf_static_template_name + '/infrastructure/cloudformation'), string(name: 'environment', value: props.EnvironmentType), string(name: 'slack_channel', value: slack_channel)]
            }
        }, searchapicode: {
            node('unixaws') {  
                build 'proteus/jobs/source control/search api code dev'
                
                dir(env.UNIX_WORKSPACE + '/proteus/jobs/source control/search api code dev') {
                    stash name: "search-api-stash"
                }
            }
        })
    } 
    
    stage('build') {
        node('unixaws') {
            build job: 'shared/jobs/build/create app bucket', parameters: [string(name: 'PublicBucket', value: bucket), string(name: 'CF_TEMPLATE_NAME', value: 'dev'), string(name: 'Region', value: 'us-east-1'), string(name: 'SlackChannel', value: slack_channel)]
    		build job: 'shared/jobs/build/create app bucket', parameters: [string(name: 'PublicBucket', value: index_bucket), string(name: 'CF_TEMPLATE_NAME', value: 'dev'), string(name: 'Region', value: 'us-east-1'), string(name: 'SlackChannel', value: slack_channel)]
			build job: 'shared/jobs/deploy/bucket expiration', parameters: [string(name: 'bucket', value: index_bucket), string(name: 'environment', value: 'dev'), string(name: 'prefix', value: 'Bulk/30Days'), string(name: 'days', value: '31'), string(name: 'slack_channel', value: slack_channel)]
        }
        parallel(srs: {
            node('vs2015') {
                dir(env.WIN_WORKSPACE + '/proteus/jobs/build/search services') {
                    deleteDir()
                    unstash "app-stash"
                }
                build job: 'proteus/jobs/build/search services', parameters: [string(name: 'bucket', value: bucket), string(name: 'name', value: cf_template_name)]
            }
        }, searchapi: {
            node('vs2015') {
                dir(env.WIN_WORKSPACE + '/proteus/jobs/build/search api') {
                    deleteDir()
                    unstash "search-api-stash"
                }
                build job: 'proteus/jobs/build/search api', parameters: [string(name: 'bucket', value: bucket), string(name: 'name', value: cf_template_name)]
            }   
        }, autosuggest: {
            node('unixaws') {
                dir(env.UNIX_WORKSPACE + '/proteus/jobs/build/auto suggest') {
                    deleteDir()
                    unstash "app-stash"
                }
                build job: 'proteus/jobs/build/auto suggest', parameters: [string(name: 'bucket', value: bucket), string(name: 'name', value: cf_template_name)]
            }  
        }, indexer: {
            node('unixaws') {
                dir(env.UNIX_WORKSPACE + '/proteus/jobs/build/es workflow') {
                    deleteDir()
                    unstash "app-stash"
                }
                build job: 'proteus/jobs/build/es workflow', parameters: [string(name: 'bucket', value: bucket), string(name: 'name', value: cf_template_name)]
            }
        }, bulkindexer: {
            node('unixaws') {
                dir(env.UNIX_WORKSPACE + '/proteus/jobs/build/bulk indexer') {
                    deleteDir()
                    unstash "app-stash"
                }
                build job: 'proteus/jobs/build/bulk indexer', parameters: [string(name: 'bucket', value: bucket), string(name: 'name', value: cf_template_name)]
            }
        }, newsalerter: {
            node('unixaws') {
                dir(env.UNIX_WORKSPACE + '/proteus/jobs/build/news alerter') {
                    deleteDir()
                    unstash "app-stash"
                }
                build job: 'proteus/jobs/build/news alerter', parameters: [string(name: 'bucket', value: bucket), string(name: 'name', value: cf_template_name)]
            }    
        }, newsmessenger: {
            node('unixaws') {
                dir(env.UNIX_WORKSPACE + '/proteus/jobs/build/news messenger') {
                    deleteDir()
                    unstash "app-stash"
                }
                build job: 'proteus/jobs/build/news messenger', parameters: [string(name: 'bucket', value: bucket), string(name: 'name', value: cf_template_name)]
            }
        }, querysuggest: {
            node('unixaws') {
                dir(env.UNIX_WORKSPACE + '/proteus/jobs/build/query suggest') {
                    deleteDir()
                    unstash "app-stash"
                }
                build job: 'proteus/jobs/build/query suggest', parameters: [string(name: 'bucket', value: bucket), string(name: 'name', value: cf_template_name)]
            }
        }, infrastructure: {
            node('unixaws') {
                dir(env.UNIX_WORKSPACE + '/proteus/jobs/build/infrastructure upload') {
                    deleteDir()
                    unstash "infra-stash"
                    zip dir: '', glob: '**', zipFile: 'puppet.zip'
                }
                build job: 'proteus/jobs/build/infrastructure upload', parameters: [string(name: 'bucket', value: bucket), string(name: 'name', value: cf_template_name), string(name: 'environment', value: 'dev'),]
            }
        }, searchapitests: {
            node('qualtest') {
                dir(env.WIN_WORKSPACE + '/proteus/jobs/build/search api tests') {
                    deleteDir()
                    unstash "app-stash"
                }
                build job: 'proteus/jobs/build/search api tests', parameters: [string(name: 'bucket', value: bucket), string(name: 'name', value: cf_template_name)]
            } 
        }, twitteralerter: {
            node('unixaws') {
                dir(env.UNIX_WORKSPACE + '/proteus/jobs/build/twitter alerter') {
                    deleteDir()
                    unstash "app-stash"
                }
                build job: 'proteus/jobs/build/twitter alerter', parameters: [string(name: 'bucket', value: bucket), string(name: 'name', value: cf_template_name)]
            }    
        }, cloudwatchmetrics: {
            node('unixaws') {
                dir(env.UNIX_WORKSPACE + '/proteus/jobs/build/cloud watch metrics') {
                    deleteDir()
                    unstash "app-stash"
                }
                build job: 'proteus/jobs/build/cloud watch metrics', parameters: [string(name: 'bucket', value: bucket), string(name: 'name', value: cf_template_name)]
            }     
        }, cleanupprevious: {
            node('unixaws') {
                build job: 'proteus/jobs/build/cleanup previous', parameters: [string(name: 'PublicBucket', value: bucket), string(name: 'CF_TEMPLATE_NAME', value: cf_template_name), string(name: 'CF_TEMPLATE_NAME_PREFIX', value: cf_template_name_prefix), string(name: 'slack_channel', value: slack_channel)]
            } 
    	}, appfacter: {
    		node('unixaws') {
    	        unstash "app-stash"
    	        build job: 'shared/jobs/build/create app facter', parameters: [string(name: 'environment', value: 'dev'), string(name: 'PublicBucket', value: bucket), string(name: 'CF_TEMPLATE_NAME', value: cf_template_name), string(name: 'PropertiesFile',  value: env.WORKSPACE + '/Config/Dev/proteus-ops-us-east-1.properties'), string(name: 'FileName', value: 'app.txt'), string(name: 'SlackChannel', value: slack_channel)]
    		}    		
        })
    } 
    
    stage('deploy') {
        node('unixaws') {
        	                                                             		  
            
            def static_parameters = 'EnvironmentType==' + props.EnvironmentType + ',,SystemName==' + 
									props.SystemName + ',,Bucket==' + bucket   + ',,IndexName==' + props.IndexName  +  
				                    ',,Tags==' + props.Tags + ',,VpcId==' + props.VpcId  + 
				                    ',,MessengerELBConfig==' + props.MessengerELBConfig + ',,MessengerELBSubnets==' + props.MessengerELBSubnets  + 
				                    ',,SearchELBConfig==' + props.SearchELBConfig + ',,SearchELBSubnets==' + props.SearchELBSubnets  + 
				                    ',,ElasticSearchClientELBConfig==' + props.ElasticSearchClientELBConfig + ',,ElasticSearchClientELBSubnets==' + props.ElasticSearchClientELBSubnets  + 
				                    ',,ElasticSearchDataELBConfig==' + props.ElasticSearchDataELBConfig + ',,ElasticSearchDataELBSubnets==' + props.ElasticSearchDataELBSubnets  + 
				                    ',,MarvelELBConfig==' + props.MarvelELBConfig + ',,MarvelELBSubnets==' + props.MarvelELBSubnets  + 
				                    ',,RedisClusterConfig==' + props.RedisClusterConfig + ',,RedisClusterSubnets==' + props.RedisClusterSubnets  + 
				                    ',,HostedZoneAndId==' + props.HostedZoneAndId
                             
            print static_parameters 

        
            def tags_static_parameters = 'environment==' + props.EnvironmentType + ',,system==' + 
		     props.SystemName + ',,indexname==' + props.IndexName

		    print tags_static_parameters 


			build job: 'shared/jobs/deploy/create stack tags', parameters: [string(name: 'environment', value: props.EnvironmentType), 
							                                               string(name: 'name', value: cf_static_template_name), 
							                                               string(name: 'bucket', value: bucket), 
							                                               string(name: 'key', value: cf_static_template_name + '/infrastructure/cloudformation/proteus_static.template'),
							                                               string(name: 'parameters', value: static_parameters),
							                                               string(name: 'slack_channel', value: slack_channel),
							                                               string(name: 'tags', value: tags_static_parameters)]
                                                                       
            build job: 'shared/jobs/deploy/unix write properties', parameters: [string(name: 'environment', value: props.EnvironmentType), 
                                                                                string(name: 'slack_channel', value: slack_channel), 
                                                                                string(name: 'filename', value: 'proteusopsstaticstack.properties'), 
                                                                                string(name: 'name', value: cf_static_template_name)]
                                                                                
            dir(env.UNIX_WORKSPACE + '/shared/jobs/deploy/unix write properties') {
                staticprops = readProperties file: 'proteusopsstaticstack.properties'
            } 
            def dataamidev = null
                    
            build job: 'shared/jobs/deploy/get ami', parameters: [string(name: 'environment', value: 'dev'), 
                                                                  string(name: 'name', value: props.UnixAMIReference), 
                                                                  string(name: 'filename', value: 'dataamiops.properties'), 
                                                                  string(name: 'slack_channel', value: slack_channel)]
        
            dir(env.UNIX_WORKSPACE + '/shared/jobs/deploy/get ami') {
            	dataamidev = readProperties file: 'dataamiops.properties'
            }        
                
            print dataamidev.UnixAMI
            
            
            def stack_parameters = 'EnvironmentType==' + props.EnvironmentType + ',,SystemName==' + 
								   props.SystemName + ',,Bucket==' + bucket   + ',,IndexName==' + props.IndexName  +  
								   ',,Tags==' + props.Tags + ',,VpcId==' + props.VpcId  + 
								   ',,InformationTopicARN==' + staticprops.InformationTopicARN + ',,ErrorTopicARN==' + staticprops.ErrorTopicARN  + 
								   ',,AlarmTopicARN==' + staticprops.AlarmTopicARN + ',,AutoScaleNotifications==' + props.AutoScaleNotifications  + 
								   ',,Tags==' + props.Tags + ',,HostedZoneAndId==' + props.HostedZoneAndId + 
								   ',,IndexBucket==' + index_bucket + ',,ElasticSearchELBSubnets==' + props.ElasticSearchELBSubnets + 
								   ',,ElasticSearchELBSecurityGroupId==' + staticprops.ElasticSearchELBSecurityGroupId + ',,ElasticSearchEc2SecurityGroupIds==' + staticprops.ProteusEc2SecurityGroup + ',' + staticprops.ElasticSearchEc2SecurityGroupId +
								   ',,ElasticSearchDataELBConfig==' + props.ElasticSearchDataELBConfig + ',,ElasticSearchAZs==' + props.ElasticSearchAZs + 
								   ',,ElasticSearchSubnets==' + props.ElasticSearchSubnets + ',,UnixAMI==' + dataamidev.UnixAMI + 
								   ',,ElasticSearchDataMasterInstance==' + props.ElasticSearchDataMasterInstance + ',,ElasticSearchDataInstance==' + props.ElasticSearchDataInstance +
								   ',,ElasticSearchClientELBConfig==' + props.ElasticSearchClientELBConfig + ',,ElasticSearchClientInstance==' + props.ElasticSearchClientInstance +
								   ',,ElasticSearchMarvelELBConfig==' + props.ElasticSearchMarvelELBConfig + ',,ElasticSearchMarvelInstance==' + props.ElasticSearchMarvelInstance +
								   ',,EnvironmentVersion==' + env.BUILD_NUMBER + ',,KeyName==' + props.KeyName 
								   
            print stack_parameters                     
        
            def tags_stack_parameters = 'environment==' + props.EnvironmentType + ',,system==' + 
		     props.SystemName + ',,indexname==' + props.IndexName + ',,version==' + props.IndexName

		    print tags_stack_parameters 
        
            build job: 'shared/jobs/deploy/create stack tags', parameters: [string(name: 'environment', value: 'dev'), 
                                                                            string(name: 'name', value: cf_template_name), 
                                                                            string(name: 'bucket', value: bucket), 
                                                                            string(name: 'key', value: cf_template_name + '/infrastructure/cloudformation/proteus.template'),
                                                                            string(name: 'parameters', value: stack_parameters),
                                                                            string(name: 'slack_channel', value: slack_channel),
                                                                            string(name: 'tags', value: tags_stack_parameters)]
                                                                       
            build job: 'shared/jobs/deploy/unix write properties', parameters: [string(name: 'environment', value: 'dev'), 
                                                                                string(name: 'slack_channel', value: slack_channel), 
                                                                                string(name: 'filename', value: 'proteusopsstack.properties'), 
                                                                                string(name: 'name', value: cf_template_name)]
                                                                                
            dir(env.UNIX_WORKSPACE + '/shared/jobs/deploy/unix write properties') {
                stackprops = readProperties file: 'proteusopsstack.properties'
            }
            
            unstash "app-stash"
            build job: 'proteus/jobs/elastic search/cluster status green', parameters: [string(name: 'name', value: cf_template_name), string(name: 'timeout', value: props.WaitForEnvironment)]
            		
    		build job: 'proteus/jobs/elastic search/create index', parameters: [string(name: 'name', value: cf_template_name), string(name: 'mapping_file',  value: env.WORKSPACE + '/ElasticSearch/Uno/mapping.json'), string(name: 'index_name', value: props.UNO_INDEX), string(name: 'shards', value: props.Shards), string(name: 'replicas', value: props.Replicas)]
			build job: 'proteus/jobs/elastic search/create index', parameters: [string(name: 'name', value: cf_template_name), string(name: 'mapping_file',  value: env.WORKSPACE + '/ElasticSearch/Uno/autosuggest.json'), string(name: 'index_name', value: 'autosuggest'), string(name: 'shards', value: props.Shards), string(name: 'replicas', value: props.Replicas)]
			build job: 'proteus/jobs/elastic search/create index', parameters: [string(name: 'name', value: cf_template_name), string(name: 'mapping_file',  value: env.WORKSPACE + '/ElasticSearch/Apx/mapping.json'), string(name: 'index_name', value: props.APX_INDEX), string(name: 'shards', value: props.Shards), string(name: 'replicas', value: props.Replicas)]
			build job: 'proteus/jobs/elastic search/create index', parameters: [string(name: 'name', value: cf_template_name), string(name: 'mapping_file',  value: env.WORKSPACE + '/ElasticSearch/Uno/topics.json'), string(name: 'index_name', value: 'followedtopics'), string(name: 'shards', value: props.Shards), string(name: 'replicas', value: props.Replicas)]
			build job: 'proteus/jobs/elastic search/create index', parameters: [string(name: 'name', value: cf_template_name), string(name: 'mapping_file',  value: env.WORKSPACE + '/ElasticSearch/Uno/channels.json'), string(name: 'index_name', value: 'channels'), string(name: 'shards', value: props.Shards), string(name: 'replicas', value: props.Replicas)]
            
            build job: 'shared/jobs/deploy/wait healthcheck', parameters: [string(name: 'name', value: cf_template_name), 
                                                                           string(name: 'timeout', value: props.WaitForEnvironment), 
                                                                           string(name: 'slack_channel', value: slack_channel)] 
        }                                                           
    }                                                                  

    stage('release') {
        node('unixaws') {
            parallel(deletestack: {
                build job: 'proteus/jobs/release/delete stack', parameters: [string(name: 'name', value: cf_template_name), 
                                                                            string(name: 'timeout', value: '30'), 
                                                                            string(name: 'bucket', value: bucket), 
                                                                            string(name: 'slack_channel', value: slack_channel)]
            }, mergeinfracode: {   
                build 'proteus/jobs/source control/merge infrastructure code'
            })    
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