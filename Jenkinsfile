pipeline {
    agent { label 'linux-docker' }

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    stages {
        stage('Checkout') {
            steps { checkout scm }
        }

        stage('Test and Package') {
            steps {
                sh 'mvn -B clean verify'
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh 'mvn -B sonar:sonar -Dsonar.projectKey=aws-cicd-demo'
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Build and Push Image') {
            steps {
                sh '''
                    set -eu
                    test -n "$AWS_REGION"
                    test -n "$AWS_ACCOUNT_ID"
                    test -n "$ECR_REPOSITORY"
                    aws sts get-caller-identity
                    aws ecr get-login-password --region "$AWS_REGION" \
                      | docker login --username AWS --password-stdin \
                        "$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com"
                    IMAGE_URI="$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPOSITORY"
                    docker build -t "$IMAGE_URI:$BUILD_NUMBER" .
                    docker push "$IMAGE_URI:$BUILD_NUMBER"
                '''
            }
        }

        stage('Deploy to EKS') {
            steps {
                sh '''
                    set -eu
                    K8S_NAMESPACE="${K8S_NAMESPACE:-default}"
                    IMAGE_URI="$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPOSITORY"
                    aws eks update-kubeconfig --region "$AWS_REGION" --name "$EKS_CLUSTER_NAME"
                    kubectl get namespace "$K8S_NAMESPACE"
                    touch .deploy-attempted
                    kubectl apply -n "$K8S_NAMESPACE" -f k8s/service.yaml
                    kubectl apply -n "$K8S_NAMESPACE" -f k8s/deployment.yaml
                    kubectl set image -n "$K8S_NAMESPACE" deployment/aws-cicd-demo \
                      app="$IMAGE_URI:$BUILD_NUMBER"
                    kubectl rollout status -n "$K8S_NAMESPACE" deployment/aws-cicd-demo --timeout=180s
                '''
            }
        }
    }

    post {
        failure {
            script {
                sh '''
                    if [ -f .deploy-attempted ]; then
                      K8S_NAMESPACE="${K8S_NAMESPACE:-default}"
                      if aws eks update-kubeconfig --region "$AWS_REGION" --name "$EKS_CLUSTER_NAME" >/dev/null 2>&1; then
                        kubectl rollout undo -n "$K8S_NAMESPACE" deployment/aws-cicd-demo || true
                      fi
                    fi
                '''
            }
        }
        success {
            echo 'CI/CD pipeline completed successfully.'
        }
        always {
            sh 'docker logout "$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com" || true'
            cleanWs()
        }
    }
}
