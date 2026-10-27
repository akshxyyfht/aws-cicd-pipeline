
pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    environment {
        AWS_REGION = 'ap-south-1'
        AWS_ACCOUNT_ID = '194154437244'
        ECR_REPOSITORY = 'aws-cicd-pipeline'
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

        stage('Build Docker Image') {
            steps {
                sh '''
                    set -eu
                    IMAGE_URI="$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPOSITORY"
                    docker build -t "$IMAGE_URI:$BUILD_NUMBER" .
                '''
            }
        }

        stage('Push to Amazon ECR') {
            steps {
                sh '''
                    set -eu
                    IMAGE_URI="$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPOSITORY"

                    aws sts get-caller-identity

                    aws ecr get-login-password --region "$AWS_REGION" \
                      | docker login --username AWS --password-stdin \
                        "$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com"

                    docker push "$IMAGE_URI:$BUILD_NUMBER"
                    docker tag "$IMAGE_URI:$BUILD_NUMBER" "$IMAGE_URI:latest"
                    docker push "$IMAGE_URI:latest"
                '''
            }
        }
    }

    post {
        success {
            echo 'CI/CD pipeline completed successfully!'
        }
        always {
            sh '''
                docker logout "$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com" || true
            '''
        }
    }
}
