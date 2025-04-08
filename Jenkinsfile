pipeline {
    agent any 
    tools {
        jdk 'JDK 11'
        maven 'Maven 3.9.6'
    }

    environment {
        PROJECT_TARGET_JDK = '11'
        ARTIFACTS_BACKUP_DIR = "${env.JENKINS_SERVER_ARTIFACT_BACKUP_DIR}"
    }


    stages {

        stage('Verify Environment') {
            steps {
                script {
                    def mvnVersionInfo = sh(script: "mvn -version", returnStdout: true).trim()
                    echo "${mvnVersionInfo}"
                    if (!mvnVersionInfo.contains("Java version: ${PROJECT_TARGET_JDK}")) {
                        error "JDK version should be ${PROJECT_TARGET_JDK} for this build. Check your environment"
                    }
                }
                sh 'echo $JAVA_HOME'
            }
        }

        stage('Build') {
            steps {
                sh 'mvn dependency:resolve'
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Test') {
            steps {
                sh 'mvn test'
            }
        } 

        stage('Archive Artifacts') {
            steps {
                script {
                    def artifactName = sh(script: 'basename $(ls target/*.jar)', returnStdout: true).trim()
                    archiveArtifacts artifacts: "target/${artifactName}", fingerprint: true, allowEmptyArchive: true
                    sh "cp target/${artifactName} ${ARTIFACTS_BACKUP_DIR}"
                }
            }
        }
    }
    
    post {
        success {
            echo 'Build completed successfully!'
            // sh '''find target -type f ! -name '*.jar' -exec rm -f {} +'''
            sh 'rm -rf target/*'

        }
        failure {
            echo 'Build failed. Check the logs for details.'
        }        
    }

}