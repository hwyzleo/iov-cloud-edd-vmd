pipeline {
    agent any

    environment {
        REPO_URL = "http://${env.MAVEN_URL}/repository/maven-snapshots/"
        REPO_ID = "snapshots"
        PROJECT_NAME = "${env.JOB_NAME}"
        DIR_API = "${env.DIR_KEY}-api"
        DIR_SERVICE = "${env.DIR_KEY}-service"
        IMAGE_NAME = "${env.REGISTRY_URL}/${PROJECT_NAME}:${env.BUILD_NUMBER}"
    }

    parameters {
        choice(name: 'DEPLOY_API', choices: [false, true], description: '是否发布API')
        choice(name: 'DOCKER_NO_CACHE', choices: [false, true], description: '构建镜像时是否使用 --no-cache')
    }

    tools {
        maven 'M3'
    }

    stages {
        stage('构建并发布') {
            when { expression { params.DEPLOY_API == "true" } }
            steps {
                script {
                    dir(DIR_API) {
                        sh '''
                            echo '============================== 构建并发布 =============================='
                            mvn clean deploy -DaltDeploymentRepository=${REPO_ID}::default::${REPO_URL}
                        '''
                    }
                }
            }
        }
        stage('构建镜像') {
            steps {
                script {
                    def noCacheArg = params.DOCKER_NO_CACHE == "true" ? "--no-cache" : ""
                    sh """
                        echo '============================== 构建镜像 =============================='
                        cp /var/jenkins_home/settings.xml ./${DIR_SERVICE}/settings.xml
                        docker build ${noCacheArg} -t ${IMAGE_NAME} -f ../Dockerfile ./${DIR_SERVICE}/
                    """
                }
            }
        }
        stage('上传镜像') {
            steps {
                sh '''
                    echo '============================== 上传镜像 =============================='
                    docker push ${IMAGE_NAME}
                '''
            }
        }
        stage('运行镜像') {
            steps {
                sh '''
                    echo '============================== 运行镜像 =============================='
                    if [ -n \"\$(docker ps -q -f name=${PROJECT_NAME})" ]; then
                        docker stop ${PROJECT_NAME}
                    fi
                    if [ -n \"\$(docker ps -aq -f name=${PROJECT_NAME})" ]; then
                        docker rm ${PROJECT_NAME}
                    fi
                    docker pull ${IMAGE_NAME}
                    docker run -d --name ${PROJECT_NAME} ${IMAGE_NAME}
                    sleep 10
                    docker logs ${PROJECT_NAME}
                '''
            }
        }
    }
}