pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        skipDefaultCheckout(true)
    }

    environment {
        AWS_REGION        = 'ap-south-1'
        AWS_ACCOUNT_ID    = '194154437244'
        ECR_REPOSITORY    = 'aws-cicd-pipeline'
        SONAR_PROJECT_KEY = 'aws-cicd-pipeline'
        SONAR_SERVER      = 'SonarQube'
        EKS_CLUSTER       = 'aws-cicd-cluster'
        K8S_DEPLOYMENT    = 'aws-cicd-app'
        K8S_CONTAINER     = 'aws-cicd-app'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build and Test') {
            steps {
                echo 'Building the application and running tests...'
                sh 'mvn -B clean verify'
            }
        }

        stage('SonarQube Analysis') {
            steps {
                echo 'Running SonarQube analysis...'
                withSonarQubeEnv("${SONAR_SERVER}") {
                    sh '''
                        mvn -B \
                          org.sonarsource.scanner.maven:sonar-maven-plugin:5.0.0.4389:sonar \
                          -Dsonar.projectKey="$SONAR_PROJECT_KEY" \
                          -Dsonar.host.url="$SONAR_HOST_URL" \
                          -Dsonar.token="$SONAR_AUTH_TOKEN" \
                          -Dsonar.scanner.skipJreProvisioning=true
                    '''
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    script {
                        def qg = waitForQualityGate()
                        if (qg.status != 'OK') {
                            error "Quality Gate failed: ${qg.status}"
                        }
                        echo "Quality Gate passed: ${qg.status}"
                    }
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    env.IMAGE_URI =
                        "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${ECR_REPOSITORY}"

                    sh '''
                        set -e
                        docker build \
                          -t "$IMAGE_URI:$BUILD_NUMBER" \
                          -t "$IMAGE_URI:latest" \
                          .
                    '''
                }
            }
        }

        stage('Push to Amazon ECR') {
            steps {
                sh '''
                    set -e
                    aws sts get-caller-identity

                    aws ecr get-login-password --region "$AWS_REGION" \
                      | docker login \
                          --username AWS \
                          --password-stdin \
                          "$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com"

                    docker push "$IMAGE_URI:$BUILD_NUMBER"
                    docker push "$IMAGE_URI:latest"
                '''
            }
        }

        stage('Deploy to EKS') {
            steps {
                sh '''
                    set -e

                    echo "Updating kubeconfig for EKS..."
                    aws eks update-kubeconfig \
                      --name "$EKS_CLUSTER" \
                      --region "$AWS_REGION"

                    echo "Deploying image: $IMAGE_URI:$BUILD_NUMBER"
                    kubectl set image \
                      "deployment/$K8S_DEPLOYMENT" \
                      "$K8S_CONTAINER=$IMAGE_URI:$BUILD_NUMBER"

                    echo "Waiting for rollout..."
                    kubectl rollout status \
                      "deployment/$K8S_DEPLOYMENT" \
                      --timeout=180s

                    echo "Checking deployment status..."
                    kubectl get deployment "$K8S_DEPLOYMENT"
                    kubectl get pods -l app=aws-cicd-app
                '''
            }
        }
    }

    post {
        always {
            sh '''
                docker logout \
                  "$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com" \
                  >/dev/null 2>&1 || true
            '''
            echo "Pipeline finished. Build: ${BUILD_NUMBER}"
        }

        success {
            echo 'CI/CD pipeline completed successfully!'
        }

        failure {
            echo 'Pipeline failed. Check the stage logs above.'
        }
    }
}
