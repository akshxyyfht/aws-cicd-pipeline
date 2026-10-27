
pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        skipDefaultCheckout(true)
    }

    environment {
        AWS_REGION = 'ap-south-1'
        AWS_ACCOUNT_ID = '194154437244'
        ECR_REPOSITORY = 'aws-cicd-pipeline'
        SONAR_PROJECT_KEY = 'aws-cicd-pipeline'
        SONAR_SERVER = 'SonarQube'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build and Test') {
            steps {
                sh 'mvn -B clean verify'
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv("${SONAR_SERVER}") {
                    sh '''
                        mvn -B sonar:sonar \
                          -Dsonar.projectKey="$SONAR_PROJECT_KEY" \
                          -Dsonar.host.url="$SONAR_HOST_URL" \
                          -Dsonar.token="$SONAR_AUTH_TOKEN"
                    '''
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                sh '''
                    set -eu

                    IMAGE_URI="$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPOSITORY"

                    docker build -t "$IMAGE_URI:$BUILD_NUMBER" .
                    docker tag "$IMAGE_URI:$BUILD_NUMBER" "$IMAGE_URI:latest"
                '''
            }
        }

        stage('Push to Amazon ECR') {
            steps {
                sh '''
                    set -eu

                    ECR_REGISTRY="$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com"
                    IMAGE_URI="$ECR_REGISTRY/$ECR_REPOSITORY"

                    aws sts get-caller-identity

                    aws ecr get-login-password --region "$AWS_REGION" \
                      | docker login --username AWS --password-stdin "$ECR_REGISTRY"

                    docker push "$IMAGE_URI:$BUILD_NUMBER"
                    docker push "$IMAGE_URI:latest"
                '''
            }
        }
    }

    post {
        success {
            echo 'Build, tests, SonarQube analysis, and ECR push completed successfully.'
        }

        failure {
            echo 'Pipeline failed. Review the stage logs above to identify the cause.'
        }

        always {
            sh '''
                docker logout "$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com" || true
            '''
        }
    }
}
